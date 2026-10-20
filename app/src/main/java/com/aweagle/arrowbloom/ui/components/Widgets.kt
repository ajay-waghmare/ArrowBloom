package com.aweagle.arrowbloom.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aweagle.arrowbloom.R
import com.aweagle.arrowbloom.engine.Scoring
import com.aweagle.arrowbloom.ui.Button
import com.aweagle.arrowbloom.ui.IconButton
import com.aweagle.arrowbloom.ui.pressable
import com.aweagle.arrowbloom.ui.theme.OnAccent
import com.aweagle.arrowbloom.ui.theme.StarEmpty
import com.aweagle.arrowbloom.ui.theme.StarGold
import com.aweagle.arrowbloom.ui.theme.SurfaceBorder
import com.aweagle.arrowbloom.ui.theme.SurfaceCard
import com.aweagle.arrowbloom.ui.theme.SurfaceSoft
import com.aweagle.arrowbloom.ui.theme.TextMuted
import com.aweagle.arrowbloom.ui.theme.TextPrimary
import com.aweagle.arrowbloom.ui.theme.TextSecondary

/** A white card with a hairline border and a soft shadow. */
@Composable
fun SectionCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = SurfaceCard,
        modifier = modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(22.dp), ambientColor = Color(0x331D2A4D), spotColor = Color(0x331D2A4D))
            .border(1.dp, SurfaceBorder, RoundedCornerShape(22.dp)),
    ) {
        Column(Modifier.padding(18.dp), content = content)
    }
}

/** Screen title row with a back button. */
@Composable
fun ScreenHeader(title: String, onBack: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back), tint = TextPrimary)
        }
        Text(title, color = TextPrimary, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
    }
}

/** Five stars, [stars] of them lit, read by TalkBack as one phrase. */
@Composable
fun StarsRow(stars: Int, modifier: Modifier = Modifier, size: Dp = 20.dp, dimUnlit: Color = StarEmpty) {
    val desc = stringResource(R.string.a11y_stars, stars)
    Row(modifier.clearAndSetSemantics { contentDescription = desc }, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        repeat(Scoring.MAX_STARS) { i ->
            Icon(Icons.Filled.Star, contentDescription = null, tint = if (i < stars) StarGold else dimUnlit, modifier = Modifier.size(size))
        }
    }
}

/** A big number with a small label. */
@Composable
fun StatTile(value: String, label: String, accent: Color, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = SurfaceCard,
        modifier = modifier.border(1.dp, SurfaceBorder, RoundedCornerShape(16.dp)),
    ) {
        Column(Modifier.padding(vertical = 12.dp, horizontal = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, color = accent, fontSize = 22.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(2.dp))
            Text(label, color = TextSecondary, fontSize = 11.sp, maxLines = 1)
        }
    }
}

/** The one loud button per screen. */
@Composable
fun PrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, container: Color = MaterialTheme.colorScheme.primary, leading: ImageVector? = null) {
    Button(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().height(54.dp),
        shape = RoundedCornerShape(18.dp),
        colors = ButtonDefaults.buttonColors(containerColor = container, contentColor = OnAccent),
    ) {
        if (leading != null) {
            Icon(leading, contentDescription = null, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black, letterSpacing = 0.5.sp)
    }
}

/** A quiet secondary button. */
@Composable
fun SoftButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Button(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().height(50.dp),
        shape = RoundedCornerShape(18.dp),
        colors = ButtonDefaults.buttonColors(containerColor = SurfaceSoft, contentColor = TextPrimary),
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun ToggleRow(icon: ImageVector, label: String, checked: Boolean, onToggle: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = SurfaceCard,
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp).border(1.dp, SurfaceBorder, RoundedCornerShape(16.dp)).pressable(onClick = onToggle),
    ) {
        Row(Modifier.padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(14.dp))
            Text(label, color = TextPrimary, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            Switch(
                checked = checked,
                onCheckedChange = { onToggle() },
                colors = SwitchDefaults.colors(checkedTrackColor = MaterialTheme.colorScheme.primary, checkedThumbColor = OnAccent),
            )
        }
    }
}

@Composable
fun ActionRow(icon: ImageVector, label: String, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = SurfaceCard,
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp).border(1.dp, SurfaceBorder, RoundedCornerShape(16.dp)).pressable(onClick = onClick),
    ) {
        Row(Modifier.padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(14.dp))
            Text(label, color = TextPrimary, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = TextMuted)
        }
    }
}
