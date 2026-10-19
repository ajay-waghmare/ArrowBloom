package com.aweagle.arrowbloom.ui.screens

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings as SystemSettings
import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.StarRate
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.aweagle.arrowbloom.BuildConfig
import com.aweagle.arrowbloom.R
import com.aweagle.arrowbloom.ui.GameUiState
import com.aweagle.arrowbloom.ui.GameViewModel
import com.aweagle.arrowbloom.ui.components.ActionRow
import com.aweagle.arrowbloom.ui.components.ScreenHeader
import com.aweagle.arrowbloom.ui.components.ToggleRow
import com.aweagle.arrowbloom.ui.theme.TextSecondary
import java.util.Locale

@Composable
fun SettingsScreen(state: GameUiState, vm: GameViewModel) {
    val context = LocalContext.current
    var showHowTo by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp),
        ) {
            ScreenHeader(stringResource(R.string.settings), onBack = { vm.feedback.tap(); vm.closeSettings() })
            Spacer(Modifier.height(8.dp))
            Column(Modifier.verticalScroll(rememberScrollState())) {
                SectionLabel(stringResource(R.string.section_audio))
                ToggleRow(Icons.AutoMirrored.Filled.VolumeUp, stringResource(R.string.sound), state.soundOn) { vm.toggleSound() }
                ToggleRow(Icons.Filled.MusicNote, stringResource(R.string.music), state.musicOn) { vm.toggleMusic() }
                ToggleRow(Icons.Filled.Vibration, stringResource(R.string.haptics), state.hapticsOn) { vm.toggleHaptics() }

                Spacer(Modifier.height(16.dp))
                SectionLabel(stringResource(R.string.section_help))
                ActionRow(Icons.Filled.School, stringResource(R.string.how_to_play)) { vm.feedback.tap(); showHowTo = true }
                if (Build.VERSION.SDK_INT >= 33) {
                    ActionRow(Icons.Filled.Language, stringResource(R.string.language)) {
                        vm.feedback.tap()
                        runCatching {
                            context.startActivity(
                                Intent(SystemSettings.ACTION_APP_LOCALE_SETTINGS, Uri.parse("package:${context.packageName}"))
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                            )
                        }.onFailure { Toast.makeText(context, R.string.could_not_open_link, Toast.LENGTH_SHORT).show() }
                    }
                }
                ActionRow(Icons.Filled.StarRate, stringResource(R.string.rate_app)) {
                    vm.feedback.tap()
                    val opened = runCatching {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=${context.packageName.removeSuffix(".debug")}")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                        true
                    }.getOrDefault(false)
                    if (!opened) runCatching {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(BuildConfig.STORE_URL)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                    }.onFailure { Toast.makeText(context, R.string.could_not_open_link, Toast.LENGTH_SHORT).show() }
                }
                ActionRow(Icons.Filled.PrivacyTip, stringResource(R.string.privacy_policy)) {
                    vm.feedback.tap()
                    runCatching {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(BuildConfig.PRIVACY_POLICY_URL)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                    }.onFailure { Toast.makeText(context, R.string.could_not_open_link, Toast.LENGTH_SHORT).show() }
                }

                Spacer(Modifier.height(20.dp))
                Text(
                    stringResource(R.string.offline_note),
                    color = TextSecondary,
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    stringResource(R.string.version_footer, BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE),
                    color = TextSecondary,
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                )
            }
        }
        if (showHowTo) HowToPlayDialog(onDismiss = { showHowTo = false })
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text.uppercase(Locale.getDefault()),
        color = TextSecondary,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(start = 4.dp, bottom = 8.dp),
    )
}
