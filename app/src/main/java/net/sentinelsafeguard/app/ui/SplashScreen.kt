package net.sentinelsafeguard.app.ui

import android.provider.Settings
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.sentinelsafeguard.app.R
import net.sentinelsafeguard.app.ui.theme.SentinelBlack
import net.sentinelsafeguard.app.ui.theme.SentinelGreen
import net.sentinelsafeguard.app.ui.theme.SentinelOutline
import net.sentinelsafeguard.app.util.SplashKind
import net.sentinelsafeguard.app.util.SplashSeason
import java.time.LocalDate

/**
 * The app launch splash IS Sentinel Command's entry splash
 * (owner order 2026-11-02: "the same splash screen as Command").
 *
 * The default WELCOME splash is a faithful native recreation of
 * the web build's CSS/DOM animation (Command shell, SPLASH-2026-
 * 10-26): the same official logo artwork in its black card, the
 * two clipped slice passes, the WELCOME letters landing staggered
 * with their ghost echoes, the single scan sweep and emerald
 * trace, on the same radial emerald field — with Command's own
 * timing: fade at 3.84s, hard release at 4.7s, 280ms fade, tap or
 * any press dismisses at once, and a short static hold (0.52s /
 * 1.2s) under reduced motion.
 *
 * Seasonal parity rides the same windows Command uses
 * ([SplashSeason]): Oct 30–Nov 1 the Halloween "Night Watch"
 * (Command plays a 2.88 MB video there; the app carries the
 * video's own approved poster frame — 98 KB — with a slow push-in
 * instead, an explicit size tradeoff: the poster already contains
 * the composited "HAPPY HALLOWEEN" greeting, so what the owner
 * sees is the same final frame), and Wed–Fri of Thanksgiving week
 * the V4 feast scene (the approved feast poster still with push-in,
 * the Sentinel logo flash front and center at 4.5s, "HAPPY
 * THANKSGIVING" letter-built at bottom center; fade 9.8s / hard
 * 10.6s / watchdog 12s). Reduced motion gets the stills, promptly.
 *
 * It plays on cold start, in front of sign-in, and never replays
 * on tab switches — launch only.
 */
@Composable
fun SplashScreen(onFinished: () -> Unit) {
    val context = LocalContext.current
    val kind = remember { SplashSeason.kind(LocalDate.now()) }
    val reduced = remember {
        try {
            Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1f,
            ) == 0f
        } catch (e: Exception) {
            false
        }
    }

    val timing = when (kind) {
        SplashKind.WELCOME -> if (reduced) 520L to 1200L else 3840L to 4700L
        SplashKind.HALLOWEEN -> if (reduced) 1400L to 2100L else 6400L to 9000L
        SplashKind.THANKSGIVING -> if (reduced) 1400L to 2100L else 9800L to 12000L
    }
    val (fadeAtMs, hardMs) = timing

    var finished by remember { mutableStateOf(false) }
    val rootAlpha = remember { Animatable(1f) }
    fun finish() {
        if (!finished) {
            finished = true
            onFinished()
        }
    }

    LaunchedEffect(kind) {
        launch {
            delay(hardMs)
            finish()
        }
        launch {
            delay(fadeAtMs)
            rootAlpha.animateTo(0f, tween(280))
            finish()
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .graphicsLayer { alpha = rootAlpha.value }
            .background(SentinelBlack)
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
            ) { finish() },
    ) {
        when (kind) {
            SplashKind.WELCOME -> WelcomeStage(reduced)
            SplashKind.HALLOWEEN -> HalloweenStage(reduced)
            SplashKind.THANKSGIVING -> ThanksgivingStage(reduced)
        }
    }
}

/** The layered field behind the welcome stage: near-black base,
 *  the emerald radial glow at 38% height, faint scanlines, and the
 *  edge vignette — the web splash's background stack. */
@Composable
private fun SplashField(content: @Composable () -> Unit) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val density = LocalDensity.current
        val glowRadius = with(density) { (maxWidth * 0.9f).toPx() }
        val glowCenter = Offset(
            x = with(density) { (maxWidth * 0.5f).toPx() },
            y = with(density) { (maxHeight * 0.38f).toPx() },
        )
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFF060806), Color(0xFF040604)),
                    ),
                ),
        )
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            SentinelGreen.copy(alpha = 0.13f),
                            Color.Transparent,
                        ),
                        center = glowCenter,
                        radius = glowRadius,
                    ),
                ),
        )
        Canvas(Modifier.fillMaxSize()) {
            val step = 4.dp.toPx()
            var y = 0f
            while (y < size.height) {
                drawLine(
                    color = Color.White.copy(alpha = 0.025f),
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = 1f,
                )
                y += step
            }
        }
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.62f)),
                        radius = with(density) { maxHeight.toPx() } * 0.75f,
                    ),
                ),
        )
        content()
    }
}

@Composable
private fun WelcomeStage(reduced: Boolean) {
    SplashField {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            if (reduced) {
                WelcomeStill()
            } else {
                WelcomeAnimated()
            }
        }
    }
}

@Composable
private fun WelcomeStill() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        LogoCard(Modifier)
        Spacer(Modifier.height(24.dp))
        SplashWord(static = true)
        Spacer(Modifier.height(17.dp))
        SplashTrace(static = true)
    }
}

@Composable
private fun WelcomeAnimated() {
    val densityObj = LocalDensity.current

    // Logo resolve (1.42s) + its two clipped slice passes.
    val logoAlpha = remember { Animatable(0f) }
    val logoScale = remember { Animatable(0.958f) }
    val logoTy = remember { Animatable(10f) }
    val sliceAAlpha = remember { Animatable(0f) }
    val sliceAX = remember { Animatable(0f) }
    val sliceBAlpha = remember { Animatable(0f) }
    val sliceBX = remember { Animatable(0f) }
    // Word ghosts (1.9s) + scan (0.62s @ 1.02s) + trace (0.55s @ 1.08s).
    val ghostAAlpha = remember { Animatable(0f) }
    val ghostAX = remember { Animatable(0f) }
    val ghostBAlpha = remember { Animatable(0f) }
    val ghostBX = remember { Animatable(0f) }
    val scanX = remember { Animatable(0f) }
    val scanAlpha = remember { Animatable(0f) }
    val traceScale = remember { Animatable(0f) }
    val traceAlpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        launch {
            logoAlpha.animateTo(1f, keyframes {
                durationMillis = 1420
                0f at 0
                1f at 256
                1f at 1420
            })
        }
        launch {
            logoScale.animateTo(1f, keyframes {
                durationMillis = 1420
                0.958f at 0
                0.978f at 256
                0.986f at 440
                0.994f at 653
                1.008f at 909
                1f at 1164
                1f at 1420
            })
        }
        launch {
            logoTy.animateTo(0f, keyframes {
                durationMillis = 1420
                10f at 0
                3f at 256
                1f at 440
                0f at 653
                (-1f) at 909
                0f at 1164
                0f at 1420
            })
        }
        launch {
            sliceAAlpha.animateTo(0f, keyframes {
                durationMillis = 1420
                0f at 142
                0.68f at 185
                0f at 256
                0.52f at 426
                0f at 511
                0f at 1420
            })
        }
        launch {
            sliceAX.animateTo(0f, keyframes {
                durationMillis = 1420
                0f at 142
                (-9f) at 185
                0f at 256
                6f at 426
                0f at 511
                0f at 1420
            })
        }
        launch {
            sliceBAlpha.animateTo(0f, keyframes {
                durationMillis = 1420
                0f at 227
                0.58f at 284
                0f at 355
                0.46f at 540
                0f at 625
                0f at 1420
            })
        }
        launch {
            sliceBX.animateTo(0f, keyframes {
                durationMillis = 1420
                0f at 227
                8f at 284
                0f at 355
                (-6f) at 540
                0f at 625
                0f at 1420
            })
        }
        launch {
            ghostAAlpha.animateTo(0f, keyframes {
                durationMillis = 1900
                0f at 1368
                0.75f at 1444
                0.5f at 1520
                0f at 1596
                0f at 1900
            })
        }
        launch {
            ghostAX.animateTo(0f, keyframes {
                durationMillis = 1900
                0f at 1368
                (-6f) at 1444
                4f at 1520
                0f at 1596
                0f at 1900
            })
        }
        launch {
            ghostBAlpha.animateTo(0f, keyframes {
                durationMillis = 1900
                0f at 1387
                0.62f at 1463
                0.42f at 1539
                0f at 1615
                0f at 1900
            })
        }
        launch {
            ghostBX.animateTo(0f, keyframes {
                durationMillis = 1900
                0f at 1387
                5f at 1463
                (-4f) at 1539
                0f at 1615
                0f at 1900
            })
        }
        launch {
            delay(1020)
            scanAlpha.animateTo(0f, keyframes {
                durationMillis = 620
                0f at 0
                0.9f at 112
                0f at 620
            })
        }
        launch {
            delay(1020)
            scanX.animateTo(1f, tween(620))
        }
        launch {
            delay(1080)
            traceAlpha.animateTo(1f, tween(300))
        }
        launch {
            delay(1080)
            traceScale.animateTo(1f, tween(550))
        }
    }

    BoxWithConstraints {
        val stageWidthPx = with(densityObj) { maxWidth.toPx() }
        val scanWidthPx = stageWidthPx * 0.46f
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            LogoCard(
                Modifier.graphicsLayer {
                    alpha = logoAlpha.value
                    scaleX = logoScale.value
                    scaleY = logoScale.value
                    translationY = logoTy.value * density
                },
                sliceA = {
                    SplashSlice(
                        bandTop = 0.17f,
                        bandBottom = 0.39f,
                        modifier = Modifier.graphicsLayer {
                            alpha = sliceAAlpha.value
                            translationX = sliceAX.value * density
                        },
                    )
                },
                sliceB = {
                    SplashSlice(
                        bandTop = 0.59f,
                        bandBottom = 0.82f,
                        modifier = Modifier.graphicsLayer {
                            alpha = sliceBAlpha.value
                            translationX = sliceBX.value * density
                        },
                    )
                },
            )
            Spacer(Modifier.height(24.dp))
            SplashWord(
                static = false,
                ghostA = Modifier.graphicsLayer {
                    alpha = ghostAAlpha.value
                    translationX = ghostAX.value * density
                },
                ghostB = Modifier.graphicsLayer {
                    alpha = ghostBAlpha.value
                    translationX = ghostBX.value * density
                },
            )
            Spacer(Modifier.height(17.dp))
            SplashTrace(
                static = false,
                modifier = Modifier.graphicsLayer {
                    alpha = traceAlpha.value
                    scaleX = traceScale.value
                    transformOrigin = TransformOrigin(0.5f, 0.5f)
                },
            )
        }
        // The single scan sweep across the stage at 56% height.
        Box(
            Modifier
                .align(Alignment.TopStart)
                .offset(
                    x = with(densityObj) { (scanX.value * (stageWidthPx + scanWidthPx) - scanWidthPx).toDp() },
                    y = maxHeight * 0.56f,
                )
                .width(with(densityObj) { scanWidthPx.toDp() })
                .height(1.dp)
                .graphicsLayer { alpha = scanAlpha.value }
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            Color.Transparent,
                            SentinelGreen.copy(alpha = 0.78f),
                            Color.Transparent,
                        ),
                    ),
                ),
        )
    }
}

/** The official logo lockup in its black card, exactly as the web
 *  splash frames it (218px wide, 390/475, 18px radius, hairline). */
@Composable
private fun LogoCard(
    modifier: Modifier = Modifier,
    sliceA: (@Composable () -> Unit)? = null,
    sliceB: (@Composable () -> Unit)? = null,
) {
    Box(
        modifier
            .width(218.dp)
            .aspectRatio(390f / 475f)
            .shadow(18.dp, RoundedCornerShape(18.dp))
            .background(Color.Black, RoundedCornerShape(18.dp))
            .border(1.dp, SentinelOutline, RoundedCornerShape(18.dp))
            .clip(RoundedCornerShape(18.dp)),
    ) {
        Image(
            painter = painterResource(R.drawable.sentinel_logo),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize(),
        )
        sliceA?.invoke()
        sliceB?.invoke()
    }
}

/** One clipped horizontal band of the logo — a slice pass layer. */
@Composable
private fun SplashSlice(bandTop: Float, bandBottom: Float, modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxSize()
            .clip(
                androidx.compose.foundation.shape.GenericShape { size, _ ->
                    addRect(Rect(0f, size.height * bandTop, size.width, size.height * bandBottom))
                },
            ),
    ) {
        Image(
            painter = painterResource(R.drawable.sentinel_logo),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

/** WELCOME, letter by letter, with the two clipped ghost echoes
 *  behind it (emerald top band, white lower band). */
@Composable
private fun SplashWord(
    static: Boolean,
    ghostA: Modifier = Modifier,
    ghostB: Modifier = Modifier,
) {
    val word = "WELCOME"
    Box {
        if (!static) {
            SplashWordRow(
                word,
                Color.White,
                animated = true,
                modifier = ghostB.clip(
                    androidx.compose.foundation.shape.GenericShape { size, _ ->
                        addRect(Rect(0f, size.height * 0.61f, size.width, size.height * 0.91f))
                    },
                ),
            )
            SplashWordRow(
                word,
                SentinelGreen,
                animated = true,
                modifier = ghostA.clip(
                    androidx.compose.foundation.shape.GenericShape { size, _ ->
                        addRect(Rect(0f, size.height * 0.08f, size.width, size.height * 0.37f))
                    },
                ),
            )
        }
        SplashWordRow(word, Color.White, animated = !static)
    }
}

@Composable
private fun SplashWordRow(
    word: String,
    color: Color,
    animated: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(modifier) {
        word.forEachIndexed { index, ch ->
            if (index > 0) Spacer(Modifier.width(11.dp))
            if (animated) {
                val alpha = remember { Animatable(0f) }
                val ty = remember { Animatable(12f) }
                LaunchedEffect(Unit) {
                    delay(500L + index * 45L)
                    launch { alpha.animateTo(1f, tween(580)) }
                    launch { ty.animateTo(0f, tween(580)) }
                }
                Text(
                    ch.toString(),
                    color = color,
                    fontSize = 42.sp,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.graphicsLayer {
                        this.alpha = alpha.value
                        translationY = ty.value * density
                    },
                )
            } else {
                Text(
                    ch.toString(),
                    color = color,
                    fontSize = 42.sp,
                    fontWeight = FontWeight.ExtraBold,
                )
            }
        }
    }
}

/** The emerald hairline that grows under the word. */
@Composable
private fun SplashTrace(static: Boolean, modifier: Modifier = Modifier) {
    Box(
        modifier
            .width(252.dp)
            .height(2.dp)
            .background(
                Brush.horizontalGradient(
                    listOf(Color.Transparent, SentinelGreen, Color.Transparent),
                ),
            ),
    )
}

/**
 * Halloween window: the Night Watch poster frame — the video's own
 * approved poster, greeting already composited into the art —
 * under a slow push-in. Reduced motion: the still, plainly.
 */
@Composable
private fun HalloweenStage(reduced: Boolean) {
    val scale = remember { Animatable(1f) }
    if (!reduced) {
        LaunchedEffect(Unit) {
            scale.animateTo(1.05f, tween(7300, easing = LinearEasing))
        }
    }
    Image(
        painter = painterResource(R.drawable.splash_halloween_poster),
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                scaleX = scale.value
                scaleY = scale.value
            },
    )
}

/**
 * Thanksgiving window: Command's V4 feast scene, still edition —
 * the approved feast poster under a slow push-in, the Sentinel
 * logo flash front and center at 4.5s (screen-blended like the web
 * build so its black ground keys out), and HAPPY THANKSGIVING
 * letter-built into the bottom-center band. Reduced motion: the
 * poster and the greeting, no flash.
 */
@Composable
private fun ThanksgivingStage(reduced: Boolean) {
    val pushIn = remember { Animatable(1f) }
    val flashAlpha = remember { Animatable(0f) }
    val flashScale = remember { Animatable(1.12f) }
    if (!reduced) {
        LaunchedEffect(Unit) {
            launch { pushIn.animateTo(1.06f, tween(10600, easing = LinearEasing)) }
            launch {
                delay(4500)
                flashAlpha.animateTo(1f, tween(260))
                flashScale.animateTo(1f, tween(340))
                delay(2100)
                flashAlpha.animateTo(0f, tween(900))
            }
        }
    }
    Box(Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(R.drawable.splash_thanksgiving_poster),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = pushIn.value
                    scaleY = pushIn.value
                },
        )
        // The flash: FRONT AND CENTER, ~70% of frame height,
        // screen-blended like the web build so the logo's black
        // card melts into the feast (drawn on a Canvas — the
        // DrawScope blend parameter is the supported route in
        // this Compose version).
        val logoBitmap = ImageBitmap.imageResource(
            LocalContext.current.resources, R.drawable.sentinel_logo,
        )
        Canvas(
            Modifier
                .align(Alignment.Center)
                .fillMaxHeight(0.7f)
                .aspectRatio(390f / 475f)
                .graphicsLayer {
                    scaleX = flashScale.value
                    scaleY = flashScale.value
                },
        ) {
            drawImage(
                image = logoBitmap,
                dstSize = IntSize(size.width.toInt(), size.height.toInt()),
                alpha = flashAlpha.value,
                blendMode = BlendMode.Screen,
            )
        }
        // The greeting: CENTER BOTTOM, letter-built.
        Row(
            Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 40.dp),
        ) {
            "HAPPY THANKSGIVING".forEachIndexed { index, ch ->
                if (index > 0) Spacer(Modifier.width(3.dp))
                if (ch == ' ') {
                    Spacer(Modifier.width(10.dp))
                } else if (reduced) {
                    ThanksgivingLetter(ch.toString(), static = true)
                } else {
                    ThanksgivingLetter(ch.toString(), static = false, index = index)
                }
            }
        }
    }
}

@Composable
private fun ThanksgivingLetter(text: String, static: Boolean, index: Int = 0) {
    if (static) {
        Text(
            text,
            color = Color(0xFFF3E9D2),
            fontSize = 21.sp,
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.Bold,
        )
        return
    }
    val alpha = remember { Animatable(0f) }
    val ty = remember { Animatable(8f) }
    LaunchedEffect(Unit) {
        delay(5300L + index * 45L)
        launch { alpha.animateTo(1f, tween(500)) }
        launch { ty.animateTo(0f, tween(500)) }
    }
    Text(
        text,
        color = Color(0xFFF3E9D2),
        fontSize = 21.sp,
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.graphicsLayer {
            this.alpha = alpha.value
            translationY = ty.value * density
        },
    )
}
