package com.aweagle.arrowbloom.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.util.Log
import java.util.Random
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin
import kotlin.math.tanh

/**
 * Fully procedural, offline sound: no audio files at all. A worker thread synthesises
 * 16-bit PCM in real time and streams it through an [AudioTrack].
 *
 * Two layers share the track. The MUSIC layer is a slow ambient pad on a four-chord loop,
 * a soft bass pulse and a sparse pentatonic arpeggio whose density rises with the combo.
 * The SOUND layer is the board itself played as an instrument: every escaped arrow plays
 * a plucked note whose pitch climbs with the combo, a blocked tap plays a low thud, a
 * cleared board plays a rising run. Both layers are quantised to one pentatonic scale, so
 * nothing the player does can sound wrong. Either layer can be muted on its own.
 *
 * Every failure is swallowed: audio can never affect gameplay.
 */
class MusicEngine {

    private val sr = 44100

    @Volatile private var playing = false
    @Volatile private var released = false
    @Volatile private var musicOn = true
    @Volatile private var soundOn = true
    @Volatile private var targetIntensity = 0f

    private var intensity = 0f
    private var master = 0f
    private var musicGain = 1f

    private var track: AudioTrack? = null
    private var thread: Thread? = null
    private val rnd = Random()

    private var n = 0L
    private val bpm = 72.0
    private val beatDur = 60.0 / bpm
    private val stepDur = beatDur / 2.0
    private var lastStep = -1L

    // A minor, four chords, four seconds each; the scale is A minor pentatonic across two octaves.
    private val chords = arrayOf(intArrayOf(57, 60, 64), intArrayOf(53, 57, 60), intArrayOf(48, 55, 60), intArrayOf(55, 59, 62))
    private val scale = intArrayOf(57, 60, 62, 64, 67, 69, 72, 74)
    private val bright = 1.0

    private var arpFreq = 0.0
    private var arpStart = -1.0
    private var arpDur = 0.0
    private var arpAmp = 0.0

    private class Voice {
        @JvmField var active = false
        @JvmField var freq = 0.0
        @JvmField var start = 0L
        @JvmField var dur = 0L
        @JvmField var amp = 0.0
        @JvmField var soft = false
    }
    private class NoteReq(val freq: Double, val delay: Long, val dur: Long, val amp: Double, val soft: Boolean)
    private val voices = Array(VOICES) { Voice() }
    private val pending = ConcurrentLinkedQueue<NoteReq>()

    private fun midiToFreq(m: Int) = 440.0 * Math.pow(2.0, (m - 69) / 12.0)

    // ----- lifecycle ------------------------------------------------------------

    /** Audio runs while the game screen is in the foreground and at least one layer is on. */
    fun setPlaying(on: Boolean) {
        if (released) return
        if (on && (musicOn || soundOn)) {
            ensureStarted()
            playing = true
            try { track?.play() } catch (_: Throwable) {}
        } else {
            playing = false
            try { track?.pause() } catch (_: Throwable) {}
        }
    }

    fun setMusicOn(on: Boolean) { musicOn = on; if (!on && !soundOn) setPlaying(false) }
    fun setSoundOn(on: Boolean) { soundOn = on; if (!on && !musicOn) setPlaying(false) }

    fun setIntensity(v: Float) { targetIntensity = v.coerceIn(0f, 1f) }

    fun release() {
        released = true
        playing = false
        try { thread?.join(200) } catch (_: Throwable) {}
        try { track?.stop() } catch (_: Throwable) {}
        try { track?.release() } catch (_: Throwable) {}
        track = null
    }

    // ----- the board as an instrument --------------------------------------------

    /**
     * An arrow escaped: one pluck. Pitch follows the column (left low, right high) and
     * climbs two scale steps per combo, capped about an octave and a half up so a long
     * streak peaks pleasantly instead of piercing.
     */
    fun escapeNote(combo: Int, column: Int, cols: Int) {
        if (!canPlaySound()) return
        val base = ((column.coerceIn(0, cols - 1).toFloat() / cols.coerceAtLeast(1)) * 3).toInt()
        val shift = ((combo - 1).coerceAtLeast(0) * 2).coerceAtMost(12)
        enqueue(midiFor(base + shift), delaySamples = 0L, durSec = 0.42, amp = 0.15)
    }

    /** A blocked tap: two short low notes falling, a polite "not that one". */
    fun blockedThud() {
        if (!canPlaySound()) return
        enqueue(midiToFreq(scale[0] - 12), 0L, 0.16, 0.13, soft = true)
        enqueue(midiToFreq(scale[0] - 14), (sr * 0.07).toLong(), 0.2, 0.11, soft = true)
    }

    /** Board cleared: a five-note rising run. */
    fun bloomRun() {
        if (!canPlaySound()) return
        for (k in 0 until 5) enqueue(midiFor(k * 2), (k * sr * 0.07).toLong(), 0.5, 0.16)
        enqueue(midiFor(10), (5 * sr * 0.07).toLong(), 0.9, 0.14)
    }

    /** A hint lit up: one soft high note. */
    fun hintPing() {
        if (!canPlaySound()) return
        enqueue(midiToFreq(scale[scale.size - 1] + 12), 0L, 0.35, 0.08)
    }

    /** Undo: a short descending pair. */
    fun undoNote() {
        if (!canPlaySound()) return
        enqueue(midiFor(4), 0L, 0.18, 0.09, soft = true)
        enqueue(midiFor(2), (sr * 0.08).toLong(), 0.25, 0.09, soft = true)
    }

    private fun canPlaySound(): Boolean = !released && soundOn && playing && track != null

    private fun midiFor(scaleIndex: Int): Double {
        val octave = scaleIndex / scale.size
        return midiToFreq(scale[scaleIndex % scale.size] + 12 * octave)
    }

    private fun enqueue(freq: Double, delaySamples: Long, durSec: Double, amp: Double, soft: Boolean = false) {
        pending.offer(NoteReq(freq, delaySamples, (durSec * sr).toLong(), amp, soft))
    }

    // ----- synthesis --------------------------------------------------------------

    private fun ensureStarted() {
        if (track != null || released) return
        try {
            val minBuf = AudioTrack.getMinBufferSize(sr, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT)
            track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build(),
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sr)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build(),
                )
                .setTransferMode(AudioTrack.MODE_STREAM)
                .setBufferSizeInBytes(maxOf(minBuf, sr / 2))
                .build()
            thread = Thread { run() }.apply { isDaemon = true; name = "MusicEngine"; start() }
        } catch (t: Throwable) {
            Log.w(TAG, "init failed", t)
        }
    }

    private fun run() {
        val buf = ShortArray(1024)
        while (!released) {
            val t = track
            if (t == null || !playing) {
                try { Thread.sleep(40) } catch (_: InterruptedException) {}
                continue
            }
            drainNotes()
            val wantMusic = if (musicOn) 1f else 0f
            for (i in buf.indices) {
                intensity += (targetIntensity - intensity) * 0.00006f
                master += (1f - master) * 0.00004f
                musicGain += (wantMusic - musicGain) * 0.0002f
                val v = (sample() * master * 0.32f).coerceIn(-1f, 1f)
                buf[i] = (v * 32767f).toInt().toShort()
                n++
            }
            try { t.write(buf, 0, buf.size) } catch (th: Throwable) { Log.w(TAG, "write failed", th) }
        }
    }

    private fun drainNotes() {
        var req = pending.poll()
        while (req != null) {
            val slot = voices.firstOrNull { !it.active } ?: voices.minByOrNull { it.start }!!
            slot.freq = req.freq
            slot.start = n + req.delay
            slot.dur = req.dur
            slot.amp = req.amp
            slot.soft = req.soft
            slot.active = true
            req = pending.poll()
        }
    }

    private fun padFor(chord: IntArray, t: Double, trem: Double): Double {
        var s = 0.0
        for (m in chord) {
            val f = midiToFreq(m)
            s += 0.11 * trem * sin(2 * PI * f * t)
            s += 0.018 * bright * trem * sin(2 * PI * f * 2 * t)
        }
        return s
    }

    private fun sample(): Float {
        val t = n / sr.toDouble()
        var s = 0.0

        if (musicGain > 0.001f) {
            val pos = t / 4.0
            val idx = pos.toInt()
            val frac = pos - idx
            val cur = chords[idx % chords.size]
            val nxt = chords[(idx + 1) % chords.size]
            val xLen = 0.18
            val w = if (frac > 1.0 - xLen) (frac - (1.0 - xLen)) / xLen else 0.0
            val trem = 0.9 + 0.1 * sin(2 * PI * 0.1 * t)

            val step = (t / stepDur).toLong()
            if (step != lastStep) {
                lastStep = step
                if (rnd.nextDouble() < 0.14 + 0.40 * intensity) {
                    arpFreq = midiToFreq(scale[rnd.nextInt(scale.size)])
                    arpStart = t
                    arpDur = stepDur * (1.6 + rnd.nextDouble())
                    arpAmp = 0.09 + 0.06 * intensity
                }
            }

            var m = (1.0 - w) * padFor(cur, t, trem)
            if (w > 0.0) m += w * padFor(nxt, t, trem)

            val beat = (t / beatDur).toLong()
            if (beat % 2 == 0L) {
                val phase = (t % beatDur) / beatDur
                val atk = (phase / 0.06).coerceAtMost(1.0)
                m += 0.13 * atk * exp(-2.2 * phase) * sin(2 * PI * midiToFreq(cur[0] - 12) * t)
            }

            if (arpStart >= 0) {
                val lt = t - arpStart
                if (lt in 0.0..arpDur) {
                    val atk = (lt / 0.025).coerceAtMost(1.0)
                    val dec = exp(-2.6 * lt / arpDur)
                    m += arpAmp * atk * dec * (sin(2 * PI * arpFreq * t) + 0.07 * bright * sin(2 * PI * arpFreq * 2 * t))
                }
            }
            s += m * musicGain
        }

        for (v in voices) {
            if (!v.active) continue
            val lt = n - v.start
            if (lt < 0) continue
            if (lt >= v.dur) { v.active = false; continue }
            val ltSec = lt / sr.toDouble()
            val durSec = v.dur / sr.toDouble()
            val atk = (ltSec / (if (v.soft) 0.02 else 0.008)).coerceAtMost(1.0)
            val env = atk * exp(-3.2 * ltSec / durSec)
            s += v.amp * env * (sin(2 * PI * v.freq * t) + 0.08 * bright * sin(2 * PI * v.freq * 2 * t))
        }

        return tanh(s * 1.05).toFloat()
    }

    companion object {
        private const val TAG = "MusicEngine"
        private const val VOICES = 8
    }
}
