package org.jellyfin.androidtv.ui.redesign

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer

/**
 * The cinematic background used on Home and Details.
 *
 * When [item] changes, the old artwork crossfades into the new one and the new one
 * starts a slow zoom ("Ken Burns"). [image] draws the artwork for a given item —
 * pass the app's existing image loader here (e.g. an AsyncImage with the item's
 * backdrop URL and its BlurHash as the placeholder).
 */
@Composable
fun <T> CinematicBackdrop(
	item: T,
	modifier: Modifier = Modifier,
	image: @Composable (item: T, modifier: Modifier) -> Unit,
) {
	val motion = LocalMotionLevel.current

	Box(modifier.fillMaxSize().background(RedesignColors.Ink)) {
		Crossfade(
			targetState = item,
			animationSpec = tween(motion.ms(RedesignMotion.BackdropCrossfadeMs)),
			label = "backdrop",
		) { current ->
			val zoom = remember(current) { Animatable(1.02f) }
			LaunchedEffect(current, motion) {
				if (motion == MotionLevel.Full) {
					zoom.animateTo(
						RedesignMotion.KenBurnsEndScale,
						tween(RedesignMotion.KenBurnsMs, easing = LinearOutSlowInEasing),
					)
				}
			}
			image(
				current,
				Modifier
					.fillMaxSize()
					.graphicsLayer {
						scaleX = zoom.value
						scaleY = zoom.value
						transformOrigin = TransformOrigin(0.7f, 0.4f)
					},
			)
		}

		// Left scrim keeps the title and synopsis readable over any artwork.
		Box(
			Modifier
				.fillMaxSize()
				.background(
					Brush.horizontalGradient(
						0f to RedesignColors.Ink.copy(alpha = 0.95f),
						0.30f to RedesignColors.Ink.copy(alpha = 0.75f),
						0.65f to RedesignColors.Ink.copy(alpha = 0.10f),
						1f to Color.Transparent,
					),
				),
		)
		// Bottom scrim so the card rows sit on solid ground.
		Box(
			Modifier
				.fillMaxSize()
				.background(
					Brush.verticalGradient(
						0.45f to Color.Transparent,
						0.80f to RedesignColors.Ink,
					),
				),
		)
	}
}
