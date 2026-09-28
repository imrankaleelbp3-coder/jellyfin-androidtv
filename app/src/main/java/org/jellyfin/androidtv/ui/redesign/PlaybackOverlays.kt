package org.jellyfin.androidtv.ui.redesign

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

/** Consumes Back and calls [onBack]. */
private fun Modifier.onBackKey(onBack: () -> Unit) = onPreviewKeyEvent {
	if (it.key == Key.Back && it.type == KeyEventType.KeyUp) { onBack(); true } else it.key == Key.Back
}

// ---------------------------------------------------------------- Skip intro

/**
 * Restyled "Ask to skip" prompt. The app's existing media-segment logic decides when
 * [visible] is true and what [label] says ("Skip intro", "Skip recap", ...).
 * Matches the app's current behaviour: disappears after 8 s or on Back.
 */
@Composable
fun SkipSegmentButton(
	visible: Boolean,
	label: String,
	accent: Color,
	onSkip: () -> Unit,
	onDismiss: () -> Unit,
	modifier: Modifier = Modifier,
) {
	val motion = LocalMotionLevel.current
	AnimatedVisibility(
		visible = visible,
		enter = fadeIn(tween(motion.ms(300))) + slideInVertically(tween(motion.ms(500))) { it / 2 },
		exit = fadeOut(tween(motion.ms(200))),
		modifier = modifier,
	) {
		val focus = remember { FocusRequester() }
		val remaining = remember { Animatable(1f) }
		LaunchedEffect(Unit) {
			focus.requestFocus()
			remaining.animateTo(0f, tween(RedesignMotion.SkipPromptMs.toInt(), easing = LinearEasing))
			onDismiss()
		}
		Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(6.dp)) {
			RText("Press OK to skip · Back to dismiss", color = RedesignColors.TextSecondary, fontSize = 12.sp)
			Box(
				Modifier
					.focusRequester(focus)
					.onBackKey(onDismiss)
					.border(2.dp, RedesignColors.FocusRing, RoundedCornerShape(10.dp))
					.padding(3.dp)
					.clip(RoundedCornerShape(8.dp))
					.background(RedesignColors.Text)
					.clickable(onClick = onSkip),
			) {
				RText(
					label,
					color = RedesignColors.OnLight,
					fontSize = 17.sp,
					fontWeight = FontWeight.Bold,
					modifier = Modifier.padding(horizontal = 22.dp, vertical = 12.dp),
				)
				Box(Modifier.align(Alignment.BottomStart).fillMaxWidth(remaining.value).height(3.dp).background(accent))
			}
		}
	}
}

// ---------------------------------------------------------------- Up next

data class UpNextInfo(val title: String, val subtitle: String)

/**
 * Card shown over the credits. Start it when the outro media segment begins
 * (fallback: the existing "next up" timing). Counts down, then calls [onPlayNow].
 */
@Composable
fun UpNextCard(
	info: UpNextInfo,
	accent: Color,
	onPlayNow: () -> Unit,
	onCancel: () -> Unit,
	thumbnail: @Composable (Modifier) -> Unit,
	seconds: Int = RedesignMotion.UpNextSeconds,
	modifier: Modifier = Modifier,
) {
	val motion = LocalMotionLevel.current
	var left by remember { mutableIntStateOf(seconds) }
	val sweep by animateFloatAsState(left / seconds.toFloat(), tween(motion.ms(1000), easing = LinearEasing), label = "ring")
	val focus = remember { FocusRequester() }

	LaunchedEffect(Unit) {
		focus.requestFocus()
		while (left > 0) { delay(1000); left-- }
		onPlayNow()
	}

	val shown = remember { MutableTransitionState(false).apply { targetState = true } }
	AnimatedVisibility(
		visibleState = shown,
		enter = fadeIn(tween(motion.ms(400))) + slideInHorizontally(tween(motion.ms(600))) { it / 6 },
		modifier = modifier.onBackKey(onCancel),
	) {
		Column(Modifier.width(300.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
			RText("UP NEXT", color = accent, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
			Box(Modifier.fillMaxWidth().height(169.dp).clip(RoundedCornerShape(12.dp))) {
				thumbnail(Modifier.fillMaxSize())
				Box(Modifier.fillMaxSize().background(RedesignColors.Ink.copy(alpha = 0.35f)))
				Box(Modifier.align(Alignment.Center).size(60.dp), contentAlignment = Alignment.Center) {
					Canvas(Modifier.fillMaxSize()) {
						val stroke = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
						drawCircle(RedesignColors.Ink.copy(alpha = 0.55f))
						drawCircle(Color.White.copy(alpha = 0.25f), style = stroke)
						drawArc(Color.White, -90f, 360f * sweep, useCenter = false, style = stroke)
					}
					RText("$left", color = RedesignColors.Text, fontSize = 22.sp, fontWeight = FontWeight.Bold)
				}
			}
			RText(info.title, color = RedesignColors.Text, fontFamily = RedesignFonts.Display, fontSize = 17.sp, fontWeight = FontWeight.Bold)
			RText(info.subtitle, color = RedesignColors.TextSecondary, fontSize = 12.sp)
			Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
				PillButton("Play now", primary = true, onClick = onPlayNow, modifier = Modifier.focusRequester(focus))
				PillButton("Cancel", primary = false, onClick = onCancel)
			}
		}
	}
}

@Composable
private fun PillButton(text: String, primary: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
	var focused by remember { mutableStateOf(false) }
	Box(
		modifier
			.onFocusChanged { focused = it.isFocused }
			.border(2.dp, if (focused) RedesignColors.FocusRing else Color.Transparent, RoundedCornerShape(10.dp))
			.padding(3.dp)
			.clip(RoundedCornerShape(8.dp))
			.background(if (primary) RedesignColors.Text else RedesignColors.Glass)
			.clickable(onClick = onClick),
	) {
		RText(
			text,
			color = if (primary) RedesignColors.OnLight else RedesignColors.Text,
			fontSize = 14.sp,
			fontWeight = FontWeight.Bold,
			modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
		)
	}
}

// ---------------------------------------------------------------- Quick actions

data class QuickAction(val label: String, val icon: ImageVector, val onClick: () -> Unit)

/** Long-press menu. First action gets focus; Back closes it. */
@Composable
fun QuickActionsMenu(
	title: String,
	subtitle: String,
	actions: List<QuickAction>,
	onDismiss: () -> Unit,
	modifier: Modifier = Modifier,
) {
	val firstFocus = remember { FocusRequester() }
	LaunchedEffect(Unit) { firstFocus.requestFocus() }

	Column(
		modifier
			.width(250.dp)
			.onBackKey(onDismiss)
			.clip(RoundedCornerShape(14.dp))
			// Real backdrop blur isn't available on most TV boxes; a dense translucent fill reads the same.
			.background(Color(0xE6181D26))
			.border(1.dp, RedesignColors.Text.copy(alpha = 0.12f), RoundedCornerShape(14.dp))
			.padding(7.dp),
	) {
		Column(Modifier.padding(horizontal = 9.dp, vertical = 6.dp)) {
			RText(title, color = RedesignColors.Text, fontSize = 15.sp, fontWeight = FontWeight.Bold)
			RText(subtitle, color = RedesignColors.TextMuted, fontSize = 11.sp)
		}
		actions.forEachIndexed { i, action ->
			var focused by remember { mutableStateOf(false) }
			Row(
				verticalAlignment = Alignment.CenterVertically,
				horizontalArrangement = Arrangement.spacedBy(10.dp),
				modifier = Modifier
					.fillMaxWidth()
					.then(if (i == 0) Modifier.focusRequester(firstFocus) else Modifier)
					.onFocusChanged { focused = it.isFocused }
					.clip(RoundedCornerShape(8.dp))
					.background(if (focused) RedesignColors.Text else Color.Transparent)
					.clickable { action.onClick(); onDismiss() }
					.padding(horizontal = 10.dp, vertical = 9.dp),
			) {
				val tint = if (focused) RedesignColors.OnLight else RedesignColors.TextSecondary
				Image(imageVector = action.icon, contentDescription = null, colorFilter = ColorFilter.tint(tint), modifier = Modifier.size(16.dp))
				RText(action.label, color = tint, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
			}
		}
	}
}
