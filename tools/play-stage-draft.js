/* Upload ONE signed AAB and stage it as a DRAFT release on one or more tracks, in one edit + one commit.
 * Usage: node tools/play-stage-draft.js <sa.json> <package> <aab> <releaseName> <track[,track...]> [--notes "en-US text"] [--dry-run]
 * Safety: reads every track before and after the change inside the edit; if a release that was live
 * (completed / inProgress / halted) before is missing afterwards, the edit is discarded and nothing changes.
 * A draft is never shown to testers; rolling it out (and any review) stays a console click.
 * Node 16 compatible: built-in https + crypto only. */
const fs = require('fs'), crypto = require('crypto'), https = require('https');
const args = process.argv.slice(2);
const flag = n => { const i = args.indexOf(n); if (i < 0) return null; const v = args[i + 1]; args.splice(i, 2); return v; };
const DRY = args.includes('--dry-run'); if (DRY) args.splice(args.indexOf('--dry-run'), 1);
const NOTES = flag('--notes');
const [KEY, PKG, AAB, NAME, TRACKS] = args;
if (!KEY || !PKG || !AAB || !NAME || !TRACKS) { console.error('usage: <sa.json> <package> <aab> <releaseName> <track[,track]> [--notes text] [--dry-run]'); process.exit(2); }
if (!fs.existsSync(AAB)) { console.error('AAB not found: ' + AAB); process.exit(2); }
const sa = JSON.parse(fs.readFileSync(KEY, 'utf8'));
const tracks = TRACKS.split(',').map(s => s.trim()).filter(Boolean);
const b64 = s => Buffer.from(s).toString('base64').replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '');
const req = (o, b) => new Promise((res, rej) => { const r = https.request(o, x => { const d = []; x.on('data', c => d.push(c)); x.on('end', () => res({ status: x.statusCode, body: Buffer.concat(d).toString('utf8') })); }); r.on('error', rej); r.setTimeout(300000, () => r.destroy(new Error('timeout'))); if (b) r.write(b); r.end(); });
async function token() {
  const now = Math.floor(Date.now() / 1000);
  const si = `${b64(JSON.stringify({ alg: 'RS256', typ: 'JWT' }))}.${b64(JSON.stringify({ iss: sa.client_email, scope: 'https://www.googleapis.com/auth/androidpublisher', aud: sa.token_uri, iat: now, exp: now + 3600 }))}`;
  const sg = crypto.createSign('RSA-SHA256'); sg.update(si);
  const form = `grant_type=${encodeURIComponent('urn:ietf:params:oauth:grant-type:jwt-bearer')}&assertion=${si}.${b64(sg.sign(sa.private_key))}`;
  const r = await req({ method: 'POST', hostname: 'oauth2.googleapis.com', path: '/token', headers: { 'Content-Type': 'application/x-www-form-urlencoded', 'Content-Length': Buffer.byteLength(form) } }, form);
  const j = JSON.parse(r.body); if (!j.access_token) throw new Error('auth: ' + r.body.slice(0, 200)); return j.access_token;
}
const H = 'androidpublisher.googleapis.com';
const LIVE = new Set(['completed', 'inProgress', 'halted']);
const liveKey = rel => `${rel.status}:${(rel.versionCodes || []).join('+')}`;
(async () => {
  const tk = await token(); const A = { Authorization: `Bearer ${tk}` };
  const base = `/androidpublisher/v3/applications/${PKG}/edits`;
  const mk = await req({ method: 'POST', hostname: H, path: base, headers: { ...A, 'Content-Length': 0 } });
  if (mk.status !== 200) throw new Error('edits.insert ' + mk.status + ' ' + mk.body.slice(0, 300));
  const id = JSON.parse(mk.body).id; console.log(`edit ${id} opened${DRY ? ' (DRY RUN — will be discarded)' : ''}`);
  const discard = async why => { const d = await req({ method: 'DELETE', hostname: H, path: `${base}/${id}`, headers: A }); console.log(`edit discarded (HTTP ${d.status}). Nothing changed.${why ? ' Reason: ' + why : ''}`); };
  const readTrack = async t => { const g = await req({ method: 'GET', hostname: H, path: `${base}/${id}/tracks/${t}`, headers: A }); return g.status === 200 ? (JSON.parse(g.body).releases || []) : []; };
  try {
    const before = {};
    for (const t of tracks) { before[t] = await readTrack(t); console.log(`before ${t.padEnd(9)}: ${before[t].map(r => `${r.name} [${r.status}] vc${(r.versionCodes || []).join(',')}`).join(' | ') || '(none)'}`); }
    const aab = fs.readFileSync(AAB);
    console.log(`uploading ${Math.round(aab.length / 1024)} KB…`);
    const up = await req({ method: 'POST', hostname: H, path: `/upload${base}/${id}/bundles?uploadType=media`, headers: { ...A, 'Content-Type': 'application/octet-stream', 'Content-Length': aab.length } }, aab);
    if (up.status !== 200) { await discard('bundle upload HTTP ' + up.status + ' ' + up.body.slice(0, 300)); process.exit(4); }
    const bj = JSON.parse(up.body); const vc = String(bj.versionCode);
    console.log(`bundle uploaded: versionCode ${vc} sha256 ${String(bj.sha256 || '').slice(0, 16)}…`);
    for (const t of tracks) {
      const release = { name: NAME, status: 'draft', versionCodes: [vc] };
      if (NOTES) release.releaseNotes = [{ language: 'en-US', text: NOTES }];
      // tracks.update takes the WHOLE release list: a body with only the draft removes the live
      // release from the track (seen 20 Oct 2026). Carry every live release over unchanged.
      const keep = before[t].filter(r => LIVE.has(r.status));
      const body = JSON.stringify({ track: t, releases: [...keep, release] });
      const put = await req({ method: 'PUT', hostname: H, path: `${base}/${id}/tracks/${t}`, headers: { ...A, 'Content-Type': 'application/json', 'Content-Length': Buffer.byteLength(body) } }, body);
      if (put.status !== 200) { await discard(`tracks.update ${t} HTTP ${put.status} ${put.body.slice(0, 300)}`); process.exit(5); }
      const after = await readTrack(t);
      console.log(`after  ${t.padEnd(9)}: ${after.map(r => `${r.name} [${r.status}] vc${(r.versionCodes || []).join(',')}`).join(' | ')}`);
      const lost = before[t].filter(r => LIVE.has(r.status)).map(liveKey).filter(k => !after.some(r => liveKey(r) === k));
      if (lost.length) { await discard(`track ${t} would lose its live release(s) ${lost.join(', ')}`); process.exit(6); }
      if (!after.some(r => r.status === 'draft' && (r.versionCodes || []).includes(vc))) { await discard(`draft vc${vc} not on ${t} after update`); process.exit(7); }
    }
    if (DRY) { await discard('dry run'); return; }
    const cm = await req({ method: 'POST', hostname: H, path: `${base}/${id}:commit`, headers: { ...A, 'Content-Length': 0 } });
    console.log(`edits.commit → HTTP ${cm.status}`);
    if (cm.status !== 200) { console.log(cm.body.slice(0, 600)); process.exit(8); }
    console.log(`\n✔ versionCode ${vc} staged as DRAFT "${NAME}" on: ${tracks.join(', ')}. Nothing rolled out.`);
  } catch (e) { await discard(e.message); process.exit(1); }
})().catch(e => { console.error('FATAL', e.message); process.exit(1); });
