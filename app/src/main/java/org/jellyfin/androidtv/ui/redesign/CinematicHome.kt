package org.jellyfin.androidtv.ui.redesign

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

/** Plain data so these components don't depend on SDK types; map BaseItemDto -> this in the ViewModel. */
data class HeroItem(
	val id: String,
	val title: String,
	val eyebrow: String,         // "New episodes", "Movie", ...
	val badges: List<String>,    // "2021", "3 seasons", "TV-MA", "4K HDR"
	val overview: String,
	val playLabel: String,       // "Resume S2 · E5" or "Play"
	val cardSubtitle: String,    // "S2 · E5 · 24 min left"
	val progress: Float?,        // 0..1, null when unplayed
)

/** Advances every [intervalMs] unless [paused]. Restarts the timer whenever the index changes. */
@Composable
fun rememberAutoRotation(count: Int, paused: Boolean, intervalMs: Long = RedesignMotion.RotateIntervalMs) =
	remember { mutableIntStateOf(0) }.also { index ->
		LaunchedEffect(count, paused, index.intValue) {
			if (!paused && count > 1) {
				delay(intervalMs)
				index.intValue = (index.intValue + 1) % count
			}
		}
	}

@Composable
fun HeroInfo(item: HeroItem, accent: Color, modifier: Modifier = Modifier, buttons: @Composable () -> Unit) {
	val motion = LocalMotionLevel.current
	AnimatedContent(
		targetState = item,
		contentKey = { it.id },
		transitionSpec = {
			(fadeIn(tween(motion.ms(600), motion.ms(RedesignMotion.HeroInDelayMs))) +
				slideInVertically(tween(motion.ms(RedesignMotion.HeroInMs), motion.ms(RedesignMotion.HeroInDelayMs))) { it / 8 })
				.togetherWith(fadeOut(tween(motion.ms(200))))
		},
		label = "hero",
		modifier = modifier,
	) { current ->
		Column(verticalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.widthIn(max = 640.dp)) {
			Text(current.eyebrow.uppercase(), color = accent, fontSize = 13.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
			Text(
				current.title,
				color = RedesignColors.Text,
				fontFamily = RedesignFonts.Display,
				fontSize = 60.sp,
				fontWeight = FontWeight.ExtraBold,
				lineHeight = 60.sp,
				maxLines = 2,
				overflow = TextOverflow.Ellipsis,
			)
			Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
				current.badges.forEach { badge ->
					Text(
						badge,
						color = RedesignColors.TextSecondary,
						fontSize = 14.sp,
						modifier = Modifier
							.clip(RoundedCornerShape(6.dp))
							.background(RedesignColors.Glass)
							.padding(horizontal = 8.dp, vertical = 2.dp),
					)
				}
			}
			Text(current.overview, color = RedesignColors.TextSecondary, fontSize = 17.sp, lineHeight = 25.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
			Spacer(Modifier.height(4.dp))
			buttons()
		}
	}
}

@Composable
fun RotationDots(count: Int, current: Int, paused: Boolean, intervalMs: Long = RedesignMotion.RotateIntervalMs) {
	val motion = LocalMotionLevel.current
	Row(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalAlignment = Alignment.CenterVertically) {
		repeat(count) { i ->
			val active = i == current
			val width by animateDpAsState(if (active) 38.dp else 9.dp, tween(motion.ms(400)), label = "dot")
			Box(Modifier.width(width).height(4.dp).clip(RoundedCornerShape(2.dp)).background(RedesignColors.Text.copy(alpha = 0.3f))) {
				if (active) {
					val fill = remember(current) { Animatable(if (paused || motion == MotionLevel.Off) 1f else 0f) }
					LaunchedEffect(current, paused) {
						if (paused) fill.snapTo(1f) else fill.animateTo(1f, tween(intervalMs.toInt(), easing = LinearEasing))
					}
					Box(Modifier.fillMaxHeight().fillMaxWidth(fill.value).background(RedesignColors.Text))
				}
			}
		}
	}
}

/**
 * The assembled Home from the canvas (layout in dp for a 960x540dp TV window).
 * [backdrop] and [cardImage] should use the app's image loader with BlurHash placeholders.
 * [heroButtons] receives the currently shown item so Play/More info always match it.
 */
@Composable
fun CinematicHome(
	items: List<HeroItem>,
	accent: Color,
	onOpen: (HeroItem) -> Unit,
	onQuickActions: (HeroItem) -> Unit,
	backdrop: @Composable (HeroItem, Modifier) -> Unit,
	cardImage: @Composable (HeroItem, Modifier) -> Unit,
	heroButtons: @Composable (HeroItem) -> Unit,
	performanceMode: Boolean = false,
	topBar: @Composable () -> Unit = {},
) {
	if (items.isEmpty()) return
	val motion = rememberMotionLevel(performanceMode)
	var rowFocused by remember { mutableStateOf(false) }
	val active = rememberAutoRotation(items.size, paused = rowFocused)
	val current = items[active.intValue.coerceIn(items.indices)]

	androidx.compose.runtime.CompositionLocalProvider(LocalMotionLevel provides motion) {
		DpadDirectionTracker(Modifier.fillMaxSize()) {
			CinematicBackdrop(item = current, image = backdrop)

			Column(Modifier.fillMaxSize().padding(start = 48.dp, top = 22.dp)) {
				topBar()
				Spacer(Modifier.weight(1f))
				HeroInfo(current, accent) { heroButtons(current) }
				Spacer(Modifier.height(16.dp))
				RotationDots(items.size, active.intValue, paused = rowFocused)
				Spacer(Modifier.height(22.dp))
				Text("Continue watching", color = RedesignColors.Text, fontFamily = RedesignFonts.Display, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
				Spacer(Modifier.height(12.dp))
				LazyRow(
					horizontalArrangement = Arrangement.spacedBy(14.dp),
					contentPadding = PaddingValues(end = 48.dp, top = 10.dp, bottom = 34.dp),
					modifier = Modifier.onFocusChanged { rowFocused = it.hasFocus },
				) {
					itemsIndexed(items, key = { _, it -> it.id }) { index, item ->
						Column {
							FocusLiftCard(
								onClick = { onOpen(item) },
								onLongClick = { onQuickActions(item) },
								onFocused = { active.intValue = index },
								width = 170.dp,
								height = 96.dp,
							) {
								Box(Modifier.fillMaxSize()) {
									cardImage(item, Modifier.fillMaxSize())
									item.progress?.let { p ->
										Box(Modifier.align(Alignment.BottomStart).fillMaxWidth().height(3.dp).background(Color.White.copy(alpha = 0.22f))) {
											Box(Modifier.fillMaxHeight().fillMaxWidth(p).background(accent))
										}
									}
								}
							}
							if (index == active.intValue && rowFocused) {
								Spacer(Modifier.height(14.dp))
								Text(item.cardSubtitle, color = RedesignColors.Text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
							}
						}
					}
				}
			}
		}
	}
}
