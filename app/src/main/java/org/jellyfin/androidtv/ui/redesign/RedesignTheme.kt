package org.jellyfin.androidtv.ui.redesign

import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp

/** Colours from the approved canvas. */
object RedesignColors {
	val Ink = Color(0xFF07090D)          // page background
	val Surface = Color(0xFF151A22)
	val Text = Color(0xFFF2EEE8)         // warm off-white
	val TextSecondary = Color(0xFFD5D9E0)
	val TextMuted = Color(0xFFA7B0BE)
	val Glass = Color(0x33F2EEE8)        // translucent button fill (no real backdrop blur on TV)
	val OnLight = Color(0xFF07090D)      // text on the white Play button
	val FocusRing = Color(0xF2FFFFFF)
	val DefaultAccent = Color(0xFF00A4DC) // Jellyfin blue; user-selectable later
}

/**
 * Sora (headings) and DM Sans (body) are OFL-licensed Google Fonts.
 * Add the .ttf files to res/font and replace these with FontFamily(Font(R.font.sora_bold, ...)).
 */
object RedesignFonts {
	val Display: FontFamily = FontFamily.Default
	val Body: FontFamily = FontFamily.Default
}

/** Timings from the canvas, in one place so they're easy to tune on a real TV. */
object RedesignMotion {
	const val BackdropCrossfadeMs = 1100
	const val KenBurnsMs = 18_000
	const val KenBurnsEndScale = 1.14f
	const val FocusMs = 240
	const val HeroInMs = 750
	const val HeroInDelayMs = 150
	const val RotateIntervalMs = 8_000L
	const val SkipPromptMs = 8_000L
	const val UpNextSeconds = 10
	const val FocusScale = 1.12f
	val FocusLift = 12.dp
	const val EntryTiltDegrees = 7f
}

/**
 * Full    – everything on (crossfades, slow zoom, tilt, shine).
 * Reduced – "performance mode" for low-power boxes: fades only, no zoom/tilt/shine.
 * Off     – the system "Remove animations" / animator scale 0 is set: no motion at all.
 */
enum class MotionLevel { Full, Reduced, Off }

val LocalMotionLevel = staticCompositionLocalOf { MotionLevel.Full }

@Composable
fun rememberMotionLevel(performanceMode: Boolean): MotionLevel {
	val context = LocalContext.current
	return remember(performanceMode) {
		val animatorScale = Settings.Global.getFloat(
			context.contentResolver,
			Settings.Global.ANIMATOR_DURATION_SCALE,
			1f,
		)
		when {
			animatorScale == 0f -> MotionLevel.Off
			performanceMode -> MotionLevel.Reduced
			else -> MotionLevel.Full
		}
	}
}

/** Duration helper: returns 0 when motion is off so every animation snaps. */
fun MotionLevel.ms(duration: Int): Int = if (this == MotionLevel.Off) 0 else duration
