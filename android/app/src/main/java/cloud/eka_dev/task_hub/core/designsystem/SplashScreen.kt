package cloud.eka_dev.task_hub.core.designsystem

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Premium Gojek/Grab-inspired animated Splash Screen for Task Hub on Android.
 * Features:
 * - Deep luxury dark canvas with ambient aurora glow spotlights
 * - Signature concentric triple radar wave pulse rings
 * - Elastic spring logo entrance and rotation pop
 * - Animated shimmer sheen across "Task Hub" brand typography
 * - Sleek tagline reveal ("Master Your Time, Own Your Day")
 * - 3 animated pulsing bottom indicator dots
 * - Fluid scale & crossfade exit morphing
 */
@Composable
fun SplashScreen(
    onComplete: () -> Unit = {}
) {
    var isExiting by remember { mutableStateOf(false) }

    val exitScale by animateFloatAsState(
        targetValue = if (isExiting) 1.08f else 1f,
        animationSpec = tween(durationMillis = 380, easing = FastOutSlowInEasing),
        label = "exitScale"
    )
    val exitAlpha by animateFloatAsState(
        targetValue = if (isExiting) 0f else 1f,
        animationSpec = tween(durationMillis = 380, easing = FastOutSlowInEasing),
        label = "exitAlpha"
    )

    // Infinite Transitions for Ambient Glow, Radar Rings, and Shimmer
    val infiniteTransition = rememberInfiniteTransition(label = "SplashInfiniteTransition")

    // Ambient Aurora Glow breathing
    val ambientBreath by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ambientBreath"
    )

    // Radar Wave 1
    val ring1Progress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ring1"
    )

    // Radar Wave 2 (delayed)
    val ring2Progress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, delayMillis = 350, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ring2"
    )

    // Radar Wave 3 (delayed)
    val ring3Progress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, delayMillis = 700, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ring3"
    )

    // Shimmer translate across brand title
    val shimmerTranslate by infiniteTransition.animateFloat(
        initialValue = -350f,
        targetValue = 650f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, delayMillis = 300, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerTranslate"
    )

    // One-shot Entrance Spring Animations
    val logoScale = remember { Animatable(0.1f) }
    val logoRotation = remember { Animatable(-18f) }
    val logoAlpha = remember { Animatable(0f) }

    val textAlpha = remember { Animatable(0f) }
    val textOffsetY = remember { Animatable(22f) }
    val taglineAlpha = remember { Animatable(0f) }
    val taglineOffsetY = remember { Animatable(14f) }

    LaunchedEffect(Unit) {
        // Logo elastic spring entrance
        launch {
            logoAlpha.animateTo(1f, tween(300))
        }
        launch {
            logoRotation.animateTo(
                targetValue = 0f,
                animationSpec = spring(
                    dampingRatio = 0.55f,
                    stiffness = Spring.StiffnessLow
                )
            )
        }
        launch {
            logoScale.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = 0.52f,
                    stiffness = Spring.StiffnessLow
                )
            )
        }

        // Title entrance
        delay(320)
        launch {
            textAlpha.animateTo(1f, tween(450))
        }
        launch {
            textOffsetY.animateTo(
                targetValue = 0f,
                animationSpec = spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessMediumLow)
            )
        }

        // Tagline entrance
        delay(200)
        launch {
            taglineAlpha.animateTo(1f, tween(450))
        }
        launch {
            taglineOffsetY.animateTo(
                targetValue = 0f,
                animationSpec = spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessMediumLow)
            )
        }

        // Minimum splash display duration (1850ms) before fluid exit
        delay(1850)
        isExiting = true
        delay(380)
        onComplete()
    }

    val titleShimmerBrush = Brush.linearGradient(
        colors = listOf(
            Color.White,
            BrandSky,
            Color.White
        ),
        start = Offset(shimmerTranslate, 0f),
        end = Offset(shimmerTranslate + 260f, 90f)
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkSurfaceBackground)
            .graphicsLayer {
                scaleX = exitScale
                scaleY = exitScale
                alpha = exitAlpha
            },
        contentAlignment = Alignment.Center
    ) {
        // 1. Ambient Background Aurora Spotlights
        Canvas(modifier = Modifier.fillMaxSize()) {
            // Spotlight 1: Indigo top-right
            val center1 = Offset(size.width * 0.82f, size.height * 0.24f)
            val radius1 = size.width * 0.65f * ambientBreath
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(BrandIndigo.copy(alpha = 0.35f), Color.Transparent),
                    center = center1,
                    radius = radius1
                ),
                radius = radius1,
                center = center1
            )

            // Spotlight 2: Sky cyan bottom-left
            val center2 = Offset(size.width * 0.18f, size.height * 0.72f)
            val radius2 = size.width * 0.58f * ambientBreath
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(BrandSky.copy(alpha = 0.28f), Color.Transparent),
                    center = center2,
                    radius = radius2
                ),
                radius = radius2,
                center = center2
            )
        }

        // 2. Concentric Radar Pulse Rings (Gojek / Grab Signature Radar Waves)
        Canvas(modifier = Modifier.size(320.dp)) {
            val centerPoint = Offset(size.width / 2f, size.height / 2f)
            val baseRadius = 55.dp.toPx()

            // Ring 1
            if (ring1Progress > 0f) {
                val r1 = baseRadius * (0.8f + ring1Progress * 2.1f)
                val alpha1 = ((1f - ring1Progress) * 0.55f).coerceIn(0f, 1f)
                drawCircle(
                    brush = Brush.linearGradient(
                        colors = listOf(BrandSky.copy(alpha = alpha1), BrandIndigo.copy(alpha = alpha1 * 0.4f)),
                        start = Offset(centerPoint.x - r1, centerPoint.y - r1),
                        end = Offset(centerPoint.x + r1, centerPoint.y + r1)
                    ),
                    radius = r1,
                    center = centerPoint,
                    style = Stroke(width = 2.dp.toPx())
                )
            }

            // Ring 2
            if (ring2Progress > 0f) {
                val r2 = baseRadius * (0.8f + ring2Progress * 2.3f)
                val alpha2 = ((1f - ring2Progress) * 0.45f).coerceIn(0f, 1f)
                drawCircle(
                    brush = Brush.linearGradient(
                        colors = listOf(BrandPrimary.copy(alpha = alpha2), BrandViolet.copy(alpha = alpha2 * 0.4f)),
                        start = Offset(centerPoint.x - r2, centerPoint.y - r2),
                        end = Offset(centerPoint.x + r2, centerPoint.y + r2)
                    ),
                    radius = r2,
                    center = centerPoint,
                    style = Stroke(width = 1.75.dp.toPx())
                )
            }

            // Ring 3
            if (ring3Progress > 0f) {
                val r3 = baseRadius * (0.8f + ring3Progress * 2.5f)
                val alpha3 = ((1f - ring3Progress) * 0.35f).coerceIn(0f, 1f)
                drawCircle(
                    brush = Brush.linearGradient(
                        colors = listOf(BrandSky.copy(alpha = alpha3), Color.Transparent),
                        start = Offset(centerPoint.x - r3, centerPoint.y - r3),
                        end = Offset(centerPoint.x + r3, centerPoint.y + r3)
                    ),
                    radius = r3,
                    center = centerPoint,
                    style = Stroke(width = 1.25.dp.toPx())
                )
            }
        }

        // 3. Central Brand Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.weight(1f))

            // Logo with spring bounce and rotation pop
            Box(
                modifier = Modifier
                    .graphicsLayer {
                        scaleX = logoScale.value
                        scaleY = logoScale.value
                        rotationZ = logoRotation.value
                        alpha = logoAlpha.value
                    },
                contentAlignment = Alignment.Center
            ) {
                // Soft glow halo behind logo
                Box(
                    modifier = Modifier
                        .size(130.dp)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(BrandPrimary.copy(alpha = 0.35f), Color.Transparent)
                            ),
                            shape = CircleShape
                        )
                )

                BrandLogo(size = 96.dp)
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Brand Title with Shimmer Mask
            Box(
                modifier = Modifier
                    .graphicsLayer {
                        alpha = textAlpha.value
                        translationY = textOffsetY.value
                    }
            ) {
                Text(
                    text = "Task Hub",
                    style = TextStyle(
                        brush = titleShimmerBrush,
                        fontSize = 34.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Default,
                        letterSpacing = 1.2.sp
                    )
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Tagline reveal
            Box(
                modifier = Modifier
                    .graphicsLayer {
                        alpha = taglineAlpha.value
                        translationY = taglineOffsetY.value
                    }
            ) {
                Text(
                    text = "Master Your Time, Own Your Day",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White.copy(alpha = 0.65f),
                    letterSpacing = 0.6.sp
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // 4. Sleek bottom animated pulse dots
            Row(
                modifier = Modifier
                    .alpha(taglineAlpha.value)
                    .padding(bottom = 36.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(3) { index ->
                    val dotAlpha by infiniteTransition.animateFloat(
                        initialValue = 0.25f,
                        targetValue = 1f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(
                                durationMillis = 600,
                                delayMillis = index * 180,
                                easing = FastOutSlowInEasing
                            ),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "dot$index"
                    )
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .alpha(dotAlpha)
                            .background(BrandSky, CircleShape)
                    )
                }
            }
        }
    }
}
