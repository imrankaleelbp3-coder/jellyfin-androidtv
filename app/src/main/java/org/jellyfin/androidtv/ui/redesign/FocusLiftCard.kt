package org.jellyfin.androidtv.ui.redesign

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * A TV remote has no pointer, so the canvas's "tilt toward the cursor" becomes
 * "tilt in from the direction you pressed, then settle". This remembers the last
 * horizontal D-pad press for the whole screen.
 */
val LocalLastDpadDirection = compositionLocalOf<MutableState<Int>> { mutableStateOf(0) }

/** Wrap a screen with this so cards know which way focus arrived from (-1 left, +1 right). */
@Composable
fun DpadDirectionTracker(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
	val direction = remember { mutableStateOf(0) }
	CompositionLocalProvider(LocalLastDpadDirection provides direction) {
		Box(
			modifier.onPreviewKeyEvent { event ->
				if (event.type == KeyEventType.KeyDown) {
					when (event.key) {
						Key.DirectionRight -> direction.value = 1
						Key.DirectionLeft -> direction.value = -1
						Key.DirectionUp, Key.DirectionDown -> direction.value = 0
					}
				}
				false // never consume; we only observe
			},
		) { content() }
	}
}

@Composable
fun FocusLiftCard(
	onClick: () -> Unit,
	width: Dp,
	height: Dp,
	modifier: Modifier = Modifier,
	onLongClick: (() -> Unit)? = null,
	onFocused: () -> Unit = {},
	shape: Shape = RoundedCornerShape(14.dp),
	content: @Composable () -> Unit,
) {
	val motion = LocalMotionLevel.current
	val lastDirection = LocalLastDpadDirection.current

	var focused by remember { mutableStateOf(false) }
	var longPressFired by remember { mutableStateOf(false) }
	val interaction = remember { MutableInteractionSource() }

	val scale by animateFloatAsState(
		targetValue = if (focused) RedesignMotion.FocusScale else 1f,
		animationSpec = tween(motion.ms(RedesignMotion.FocusMs)),
		label = "scale",
	)
	val lift by animateDpAsState(
		targetValue = if (focused) RedesignMotion.FocusLift else 0.dp,
		animationSpec = tween(motion.ms(RedesignMotion.FocusMs)),
		label = "lift",
	)
	val ringAlpha by animateFloatAsState(
		targetValue = if (focused) 1f else 0f,
		animationSpec = tween(motion.ms(200)),
		label = "ring",
	)
	val tilt = remember { Animatable(0f) }
	val shine = remember { Animatable(-0.5f) }

	LaunchedEffect(focused, motion) {
		if (!focused) {
			tilt.snapTo(0f)
			return@LaunchedEffect
		}
		onFocused()
		if (motion != MotionLevel.Full) return@LaunchedEffect
		// Tilt in from the pressed direction, then spring back flat.
		tilt.snapTo(lastDirection.value * RedesignMotion.EntryTiltDegrees)
		tilt.animateTo(0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow))
	}
	LaunchedEffect(focused, motion) {
		if (focused && motion == MotionLevel.Full) {
			shine.snapTo(-0.5f)
			shine.animateTo(1.5f, tween(700))
		}
	}

	Box(
		modifier
			.size(width, height)
			.graphicsLayer {
				scaleX = scale
				scaleY = scale
				translationY = -lift.toPx()
				rotationY = tilt.value
				cameraDistance = 12f * density
			}
			.shadow(if (focused) 28.dp else 6.dp, shape, clip = false)
			.border(3.dp, RedesignColors.FocusRing.copy(alpha = ringAlpha), shape)
			.clip(shape)
			.onFocusChanged { focused = it.isFocused }
			// Long-press OK opens quick actions. Verify on-device: some remotes send
			// repeat events differently, and clickable must not also fire on release.
			.onPreviewKeyEvent { event ->
				val isOk = event.key == Key.DirectionCenter || event.key == Key.Enter
				when {
					!isOk || onLongClick == null -> false
					event.type == KeyEventType.KeyDown && event.nativeKeyEvent.isLongPress -> {
						longPressFired = true
						onLongClick()
						true
					}
					event.type == KeyEventType.KeyUp && longPressFired -> {
						longPressFired = false
						true // swallow the release so onClick doesn't also run
					}
					else -> false
				}
			}
			.clickable(interactionSource = interaction, indication = null, onClick = onClick)
			.drawWithContent {
				drawContent()
				if (focused && motion == MotionLevel.Full) {
					val x = size.width * shine.value
					drawRect(
						Brush.linearGradient(
							0f to Color.Transparent,
							0.5f to Color.White.copy(alpha = 0.28f),
							1f to Color.Transparent,
							start = Offset(x - size.width * 0.35f, 0f),
							end = Offset(x + size.width * 0.35f, size.height),
						),
					)
				}
			},
	) { content() }
}
