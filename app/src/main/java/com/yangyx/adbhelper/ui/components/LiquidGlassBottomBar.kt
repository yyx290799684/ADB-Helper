package com.yangyx.adbhelper.ui.components

import android.graphics.RenderEffect as AndroidRenderEffect
import android.graphics.RuntimeShader
import android.os.Build
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yangyx.adbhelper.ui.models.LiquidGlassConfig
import com.yangyx.adbhelper.ui.models.NavItem
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val DISPERSION_SHADER_SRC = """
    uniform shader composable;
    uniform float4 dropletRect;
    uniform float cornerRadius;
    uniform float progress;

    float sdRoundRect(float2 p, float2 b, float r) {
        float2 q = abs(p) - b + float2(r, r);
        return min(max(q.x, q.y), 0.0) + length(max(q, 0.0)) - r;
    }

    half4 main(float2 coord) {
        half4 baseColor = composable.eval(coord);
        if (progress <= 0.005) {
            return baseColor;
        }

        float2 center = float2(dropletRect.x + dropletRect.z * 0.5, dropletRect.y + dropletRect.w * 0.5);
        float2 halfSize = float2(dropletRect.z * 0.5, dropletRect.w * 0.5);
        float dist = sdRoundRect(coord - center, halfSize, cornerRadius);

        // 仅在水滴凸透镜边界极窄的微距范围(~1.6px)内产生微色散，绝不夸张扩大
        float edgeBand = smoothstep(-1.8, -0.2, dist) * (1.0 - smoothstep(0.2, 1.8, dist));
        
        if (edgeBand > 0.01) {
            float eps = 1.0;
            float dX = sdRoundRect(coord + float2(eps, 0.0) - center, halfSize, cornerRadius) -
                       sdRoundRect(coord - float2(eps, 0.0) - center, halfSize, cornerRadius);
            float dY = sdRoundRect(coord + float2(0.0, eps) - center, halfSize, cornerRadius) -
                       sdRoundRect(coord - float2(0.0, eps) - center, halfSize, cornerRadius);
            float2 normal = normalize(float2(dX, dY) + float2(0.0001, 0.0001));

            // 色散物理位移极小(1.3像素)，完全基于水滴下方像素自身的RGB颜色进行折射分光
            float disp = 1.3 * progress * edgeBand;
            
            half4 sR = composable.eval(coord - normal * (disp * 1.1));
            half4 sG = composable.eval(coord);
            half4 sB = composable.eval(coord + normal * (disp * 1.1));

            half3 dispersedRgb = half3(sR.r, sG.g, sB.b);
            float alpha = max(baseColor.a, max(sR.a, sB.a));
            
            return half4(mix(baseColor.rgb, dispersedRgb, edgeBand * progress * 0.85), alpha);
        }

        return baseColor;
    }
"""

/**
 * 拟态透明流体玻璃底栏 (Coolapk 酷安风格液态毛玻璃与色散滑块透镜)：
 *
 * 1. 【常规状态 (Normal State)】：
 *    - 悬浮深灰黑磨砂半透胶囊底壳 (Smoked Obsidian Frosted Glass)，微透出后方内容与文字；
 *    - 选中项为内嵌的柔和暗色磨砂小胶囊，微弱反光勾边；
 *    - 选中项图标与文字为高辨识度的活力薄荷绿 (#00C48C)，未选中项为通透纯白。
 *
 * 2. 【按下/拖拽状态 (Pressed / Dragging State)】：
 *    - 激活凸透镜色散物理质感：滑块膨胀变大 (Press Expansion)，微微拉伸形变；
 *    - 边缘呈现标志性的彩虹棱镜色散光圈 (Prismatic Chromatic Aberration Rim)，右侧呈现强青蓝折射光晕 (Cyan Flare)，左上呈现暖金琥珀高光；
 *    - 内部通透感提升，凸面透镜反光强烈；
 *    - 释放后平滑回弹收缩，吸附并完成页面切换。
 */
@Composable
fun LiquidGlassBottomBar(
    navItems: List<NavItem>,
    selectedIndex: Int,
    onItemSelected: (Int) -> Unit,
    config: LiquidGlassConfig,
    modifier: Modifier = Modifier,
    isDocked: Boolean = false
) {
    val isDark = isSystemInDarkTheme()
    val density = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()

    // Smooth morph transition between floating capsule island and docked bottom bar
    val dockedTransition = updateTransition(targetState = isDocked, label = "docked_transition")

    val animRadiusTop by dockedTransition.animateDp(
        transitionSpec = { spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = 380f) },
        label = "anim_radius_top"
    ) { docked ->
        if (docked) 16.dp else 30.dp
    }

    val animRadiusBottom by dockedTransition.animateDp(
        transitionSpec = { spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = 380f) },
        label = "anim_radius_bottom"
    ) { docked ->
        if (docked) 0.dp else 30.dp
    }

    val animHorizontalPadding by dockedTransition.animateDp(
        transitionSpec = { spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = 380f) },
        label = "anim_horizontal_padding"
    ) { docked ->
        if (docked) 0.dp else 14.dp
    }

    val animVerticalPadding by dockedTransition.animateDp(
        transitionSpec = { spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = 380f) },
        label = "anim_vertical_padding"
    ) { docked ->
        if (docked) 0.dp else 6.dp
    }

    val animSliderRadius by dockedTransition.animateDp(
        transitionSpec = { spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = 380f) },
        label = "anim_slider_radius"
    ) { docked ->
        if (docked) 14.dp else 25.dp
    }

    val animElevation by dockedTransition.animateDp(
        transitionSpec = { spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = 380f) },
        label = "anim_elevation"
    ) { docked ->
        if (docked) 0.dp else if (isDark) 10.dp else 8.dp
    }

    val islandHeight = 56.dp
    val containerHeight = if (isDocked) 56.dp else 68.dp

    val islandShape = RoundedCornerShape(
        topStart = animRadiusTop.coerceAtLeast(0.dp),
        topEnd = animRadiusTop.coerceAtLeast(0.dp),
        bottomStart = animRadiusBottom.coerceAtLeast(0.dp),
        bottomEnd = animRadiusBottom.coerceAtLeast(0.dp)
    )
    val itemCount = navItems.size.coerceAtLeast(1)

    // Gesture interaction state
    var isDragging by remember { mutableStateOf(false) }
    var isPressed by remember { mutableStateOf(false) }
    var dragFingerXPx by remember { mutableFloatStateOf(0f) }

    // Physical spring slider offset in Dp (tracks the base tab start position)
    val sliderOffsetDp = remember { Animatable(0f) }

    // Subtle fluid horizontal stretch for organic jelly movement (no vertical lifting)
    val sliderScaleX = remember { Animatable(1f) }
    val sliderScaleY = remember { Animatable(1f) }

    // Chromatic dispersion intensity for active interaction
    val chromaticAlpha = remember { Animatable(0f) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (isDocked) Modifier.background(if (isDark) Color(0xFF161922) else Color(0xFFF1F5F9))
                else Modifier
            )
            .navigationBarsPadding()
            .padding(vertical = animVerticalPadding.coerceAtLeast(0.dp))
            .graphicsLayer { clip = false },
        contentAlignment = Alignment.Center
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(containerHeight)
                .graphicsLayer { clip = false },
            contentAlignment = Alignment.Center
        ) {
            val totalAvailableWidthDp = maxWidth
            val capsuleMarginDp = animHorizontalPadding.coerceAtLeast(0.dp)
            val capsuleWidthDp = (totalAvailableWidthDp - capsuleMarginDp * 2).coerceAtLeast(100.dp)
            val tabWidthDp = capsuleWidthDp / itemCount
            val tabWidthPx = with(density) { tabWidthDp.toPx() }
            val capsuleWidthPx = with(density) { capsuleWidthDp.toPx() }
            val totalAvailableWidthPx = with(density) { totalAvailableWidthDp.toPx() }

            // 交互状态：仅在手指按下或拖拽时膨胀为超出底栏的大圆角水滴凸透镜，松手后弹性平滑缩回底栏内部
            val isInteracting = isDragging || isPressed

            val interactionProgress by animateFloatAsState(
                targetValue = if (isInteracting) 1f else 0f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = 380f
                ),
                label = "interaction_progress"
            )

            // 高度：按压时膨胀为 68.dp（上下对称超出 56.dp 底栏各 6.dp），松手后缩回 52.dp（完全嵌套在 56.dp 底壳内）
            val currentDropletHeight by animateDpAsState(
                targetValue = if (isInteracting) (if (isDocked) 54.dp else 68.dp) else 52.dp,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = 380f
                ),
                label = "droplet_height"
            )

            // 宽度：按压时外扩至 tabWidthDp + 24.dp，松手后缩回至底栏内嵌胶囊 tabWidthDp - 4.dp
            val currentDropletWidthExtra by animateDpAsState(
                targetValue = if (isInteracting) 24.dp else (-4).dp,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = 380f
                ),
                label = "droplet_width_extra"
            )

            // 圆角：按压时大圆角 34.dp，松手后对应 52.dp 高度内嵌胶囊圆角 22.dp
            val currentDropletCornerRadius by animateDpAsState(
                targetValue = if (isInteracting) 34.dp else 26.dp,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = 380f
                ),
                label = "droplet_corner_radius"
            )

            // 阴影：按压凸起浮动时具有柔和阴影，松手缩回底栏内部后无外溢阴影 (0.dp)
            val currentDropletElevation by animateDpAsState(
                targetValue = if (isInteracting) (if (isDark) 10.dp else 6.dp) else 0.dp,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = 380f
                ),
                label = "droplet_elevation"
            )

            val dropletWidthDp = (tabWidthDp + currentDropletWidthExtra).coerceAtLeast(36.dp)
            val dropletWidthPx = with(density) { dropletWidthDp.toPx() }
            val dropletShape = RoundedCornerShape(currentDropletCornerRadius)

            // Synchronize idle selectedIndex to slider position when not dragging
            LaunchedEffect(selectedIndex, isDragging, tabWidthDp.value) {
                if (!isDragging) {
                    val targetX = (tabWidthDp * selectedIndex).value
                    sliderOffsetDp.animateTo(
                        targetValue = targetX,
                        animationSpec = spring(
                            dampingRatio = config.jellySpring.dampingRatio,
                            stiffness = config.jellySpring.stiffness
                        )
                    )
                }
            }

            // Calculate live candidate index while dragging or pressing
            val currentHoverIndex by remember(isDragging, isPressed, dragFingerXPx, selectedIndex, tabWidthPx) {
                derivedStateOf {
                    if (isDragging || isPressed) {
                        (dragFingerXPx / tabWidthPx).toInt().coerceIn(0, itemCount - 1)
                    } else {
                        selectedIndex
                    }
                }
            }

            // Outer capsule background: 半透明深烟熏黑磨砂玻璃（Dark Smoked Frosted Acrylic）
            val iosGlassContainerBg = remember(isDark, config.translucency) {
                if (isDark) {
                    val baseAlpha = config.translucency.darkAlpha.coerceIn(0.80f, 0.94f)
                    Brush.verticalGradient(
                        listOf(
                            Color(0xFF1E2129).copy(alpha = baseAlpha),
                            Color(0xFF15171F).copy(alpha = (baseAlpha + 0.04f).coerceAtMost(0.97f)),
                            Color(0xFF101217).copy(alpha = (baseAlpha + 0.05f).coerceAtMost(0.98f))
                        )
                    )
                } else {
                    val baseAlpha = config.translucency.lightAlpha.coerceIn(0.80f, 0.95f)
                    Brush.verticalGradient(
                        listOf(
                            Color(0xFFFFFFFF).copy(alpha = (baseAlpha + 0.05f).coerceAtMost(0.98f)),
                            Color(0xFFF3F6FA).copy(alpha = baseAlpha),
                            Color(0xFFE8EDF4).copy(alpha = (baseAlpha + 0.03f).coerceAtMost(0.98f))
                        )
                    )
                }
            }

            // Outer capsule fine translucent rim
            val iosGlassBorder = remember(isDark) {
                if (isDark) {
                    BorderStroke(
                        0.75.dp,
                        Brush.verticalGradient(
                            listOf(
                                Color.White.copy(alpha = 0.25f),
                                Color.White.copy(alpha = 0.07f),
                                Color.White.copy(alpha = 0.12f)
                            )
                        )
                    )
                } else {
                    BorderStroke(
                        0.75.dp,
                        Brush.verticalGradient(
                            listOf(
                                Color.White.copy(alpha = 0.85f),
                                Color(0xFFCBD5E1).copy(alpha = 0.40f),
                                Color.White.copy(alpha = 0.60f)
                            )
                        )
                    )
                }
            }

            // 物理光学微色散着色器：实时采样底层真实渲染像素进行微距色散折射分光
            val runtimeShader = remember {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    try {
                        RuntimeShader(DISPERSION_SHADER_SRC)
                    } catch (_: Throwable) {
                        null
                    }
                } else null
            }
            val rawDropletLeftDp = capsuleMarginDp + sliderOffsetDp.value.dp + (tabWidthDp - dropletWidthDp) / 2
            val dropletLeftDp = rawDropletLeftDp.coerceIn(
                2.dp,
                (totalAvailableWidthDp - dropletWidthDp - 2.dp).coerceAtLeast(2.dp)
            )
            val dropletLeftPx = with(density) { dropletLeftDp.toPx() }
            val dropletHeightPx = with(density) { currentDropletHeight.toPx() }
            val dropletCornerRadiusPx = with(density) { currentDropletCornerRadius.toPx() }
            val containerHeightPx = with(density) { containerHeight.toPx() }
            val dropletTopPx = (containerHeightPx - dropletHeightPx) / 2f

            // 物理色散渲染容器：仅在凸透镜边界极细微距(~1.6px)内对底层真实内容RGB通道进行物理折射分光
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(containerHeight)
                    .graphicsLayer {
                        clip = false
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && runtimeShader != null && interactionProgress > 0.005f) {
                            try {
                                runtimeShader.setFloatUniform(
                                    "dropletRect",
                                    dropletLeftPx,
                                    dropletTopPx,
                                    dropletWidthPx,
                                    dropletHeightPx
                                )
                                runtimeShader.setFloatUniform("cornerRadius", dropletCornerRadiusPx)
                                runtimeShader.setFloatUniform("progress", interactionProgress)
                                renderEffect = AndroidRenderEffect
                                    .createRuntimeShaderEffect(runtimeShader, "composable")
                                    .asComposeRenderEffect()
                            } catch (_: Throwable) {
                                renderEffect = null
                            }
                        } else {
                            renderEffect = null
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                // Layer 0: Floating Island Capsule (底栏深色磨砂胶囊底壳，高度 56.dp，垂直居中于 68.dp 容器内)
                Box(
                    modifier = Modifier
                        .offset(x = capsuleMarginDp)
                        .width(capsuleWidthDp)
                        .height(islandHeight)
                        .align(Alignment.CenterStart)
                        .graphicsLayer { clip = false }
                        .shadow(
                            elevation = animElevation.coerceAtLeast(0.dp),
                            shape = islandShape,
                            ambientColor = if (isDark) Color(0x60000000) else Color(0x12000000),
                            spotColor = if (isDark) Color(0x90000000) else Color(0x1A000000),
                            clip = false
                        )
                        .background(brush = iosGlassContainerBg, shape = islandShape)
                        .then(
                            if (!isDocked) Modifier.border(border = iosGlassBorder, shape = islandShape)
                            else Modifier
                        )
                        // Top Specular edge line (顶部纤细反射高光)
                        .drawBehind {
                            val canvasW = size.width
                            val topHighlightBrush = Brush.horizontalGradient(
                                0.05f to Color.Transparent,
                                0.30f to Color.White.copy(alpha = if (isDark) 0.22f else 0.45f),
                                0.70f to Color.White.copy(alpha = if (isDark) 0.22f else 0.45f),
                                0.95f to Color.Transparent
                            )
                            drawRoundRect(
                                brush = topHighlightBrush,
                                topLeft = Offset(16.dp.toPx(), 0.5.dp.toPx()),
                                size = Size(canvasW - 32.dp.toPx(), 1.dp.toPx()),
                                cornerRadius = CornerRadius(0.5.dp.toPx(), 0.5.dp.toPx())
                            )
                        }
                )

                // Layer 1: Oversized Liquid Glass Water Droplet Lens (按压时为大圆角水滴透镜，松手后缩回底栏内)
                val alpha = chromaticAlpha.value

                // 通透纯净的微弱反光边缘（交互膨胀时柔和显现，空闲缩回时保持极其低调的内嵌细线）
                val dropletBorderBrush = remember(isDark, interactionProgress) {
                    if (isDark) {
                        Brush.verticalGradient(
                            listOf(
                                Color.White.copy(alpha = 0.12f + 0.20f * interactionProgress),
                                Color.White.copy(alpha = 0.04f + 0.04f * interactionProgress),
                                Color.White.copy(alpha = 0.08f + 0.10f * interactionProgress)
                            )
                        )
                    } else {
                        Brush.verticalGradient(
                            listOf(
                                Color.White.copy(alpha = 0.30f + 0.20f * interactionProgress),
                                Color.White.copy(alpha = 0.12f + 0.08f * interactionProgress),
                                Color.White.copy(alpha = 0.20f + 0.15f * interactionProgress)
                            )
                        )
                    }
                }

            Box(
                modifier = Modifier
                    .offset(x = dropletLeftDp)
                    .width(dropletWidthDp)
                    .height(currentDropletHeight)
                    .align(Alignment.CenterStart)
                    .graphicsLayer {
                        scaleX = sliderScaleX.value
                        scaleY = sliderScaleY.value
                        clip = false
                    }
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .shadow(
                            elevation = currentDropletElevation,
                            shape = dropletShape,
                            ambientColor = if (isDark) Color(0x60000000) else Color(0x14000000),
                            spotColor = if (isDark) Color(0x90000000) else Color(0x1E000000),
                            clip = false
                        )
                        // 水滴本体通透流体玻璃材质 (按压膨胀时微亮，松手缩回时为内嵌半透胶囊)
                        .background(
                            brush = if (isDark) {
                                Brush.verticalGradient(
                                    listOf(
                                        Color.White.copy(alpha = 0.08f + 0.04f * interactionProgress),
                                        Color.White.copy(alpha = 0.04f + 0.01f * interactionProgress),
                                        Color.White.copy(alpha = 0.06f + 0.02f * interactionProgress)
                                    )
                                )
                            } else {
                                Brush.verticalGradient(
                                    listOf(
                                        Color.White.copy(alpha = 0.55f + 0.10f * interactionProgress),
                                        Color.White.copy(alpha = 0.35f + 0.05f * interactionProgress),
                                        Color.White.copy(alpha = 0.45f + 0.07f * interactionProgress)
                                    )
                                )
                            },
                            shape = dropletShape
                        )
                        // 大圆角矩形柔和边缘细线
                        .border(
                            border = BorderStroke(
                                1.dp,
                                dropletBorderBrush
                            ),
                            shape = dropletShape
                        )
                        // 水滴凸透镜自然光学微光（微距色散由着色器采样底层像素RGB分离生成，此处无任何外加固定颜色）
                        .drawBehind {
                            if (interactionProgress > 0.01f) {
                                val p = interactionProgress
                                val w = size.width
                                val h = size.height
                                val r = currentDropletCornerRadius.toPx()

                                // 仅保留 0.75dp 极弱的自然顶光（消除一切固定彩虹色和巨型光圈）
                                val subtleRimGlow = Brush.verticalGradient(
                                    0.0f to Color.White.copy(alpha = (if (isDark) 0.14f else 0.22f) * p),
                                    0.25f to Color.White.copy(alpha = (if (isDark) 0.02f else 0.04f) * p),
                                    1.0f to Color.Transparent
                                )
                                drawRoundRect(
                                    brush = subtleRimGlow,
                                    size = Size(w, h),
                                    cornerRadius = CornerRadius(r, r),
                                    style = Stroke(width = 0.75.dp.toPx())
                                )
                            }
                        }
                )
            }

            // Layer 2: Navigation Tab Icons & Typography (高度 56.dp 居中，与底栏完全对齐，恢复原本颜色层级)
            Row(
                modifier = Modifier
                    .offset(x = capsuleMarginDp)
                    .width(capsuleWidthDp)
                    .height(islandHeight)
                    .align(Alignment.CenterStart),
                verticalAlignment = Alignment.CenterVertically
            ) {
                navItems.forEachIndexed { index, item ->
                    val isCandidate = if (isDragging || isPressed) currentHoverIndex == index else selectedIndex == index
                    val isActualActive = selectedIndex == index

                    // Gentle scale feedback when hovered or active
                    val tabScale by animateFloatAsState(
                        targetValue = if (isCandidate) 1.08f else 1.0f,
                        animationSpec = spring(dampingRatio = 0.7f, stiffness = 400f),
                        label = "tab_scale"
                    )

                    // 恢复原本的文字与图标颜色层级（不使用任何外加颜色，保持原有中性高对比颜色）
                    val iconColor = when {
                        isCandidate -> if (isDark) Color.White else Color(0xFF0F172A)
                        isActualActive -> if (isDark) Color(0xFFF1F5F9) else Color(0xFF1E293B)
                        else -> if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                    }
                    val textColor = when {
                        isCandidate -> if (isDark) Color.White else Color(0xFF0F172A)
                        isActualActive -> if (isDark) Color(0xFFF1F5F9) else Color(0xFF1E293B)
                        else -> if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.graphicsLayer {
                                scaleX = tabScale
                                scaleY = tabScale
                            }
                        ) {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = item.title,
                                tint = iconColor,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.height(1.dp))
                            Text(
                                text = item.title,
                                fontSize = 11.sp,
                                maxLines = 1,
                                fontWeight = if (isCandidate || isActualActive) FontWeight.SemiBold else FontWeight.Normal,
                                color = textColor,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

        // Layer 3: Invisible Gesture Handling Overlay (覆盖全底栏胶囊区域及超出部分 68.dp，支持流畅拖动与点击)
        Box(
            modifier = Modifier
                .offset(x = capsuleMarginDp)
                .width(capsuleWidthDp)
                .fillMaxHeight()
                .pointerInput(itemCount, tabWidthPx, capsuleWidthPx) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            isDragging = true
                            isPressed = true
                            dragFingerXPx = offset.x
                            coroutineScope.launch {
                                launch { chromaticAlpha.animateTo(1f, tween(100)) }
                                launch { sliderScaleX.animateTo(1.08f, tween(100, easing = FastOutSlowInEasing)) }

                                val targetLeftPx = (offset.x - tabWidthPx / 2f).coerceIn(0f, capsuleWidthPx - tabWidthPx)
                                val targetLeftDp = with(density) { targetLeftPx.toDp() }
                                sliderOffsetDp.snapTo(targetLeftDp.value)
                            }
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            dragFingerXPx = change.position.x
                            val targetLeftPx = (change.position.x - tabWidthPx / 2f).coerceIn(0f, capsuleWidthPx - tabWidthPx)
                            val targetLeftDp = with(density) { targetLeftPx.toDp() }
                            coroutineScope.launch {
                                sliderOffsetDp.snapTo(targetLeftDp.value)
                            }
                        },
                        onDragEnd = {
                            val finalIdx = (dragFingerXPx / tabWidthPx).toInt().coerceIn(0, itemCount - 1)
                            isDragging = false
                            isPressed = false
                            coroutineScope.launch {
                                launch { chromaticAlpha.animateTo(0f, spring(0.75f, 380f)) }
                                launch { sliderScaleX.animateTo(1f, spring(0.65f, 450f)) }
                                val snapTargetDp = (tabWidthDp * finalIdx).value
                                sliderOffsetDp.animateTo(
                                    targetValue = snapTargetDp,
                                    animationSpec = spring(
                                        dampingRatio = config.jellySpring.dampingRatio,
                                        stiffness = config.jellySpring.stiffness
                                    )
                                )
                            }
                            onItemSelected(finalIdx)
                        },
                        onDragCancel = {
                            isDragging = false
                            isPressed = false
                            coroutineScope.launch {
                                launch { chromaticAlpha.animateTo(0f, spring(0.75f, 380f)) }
                                launch { sliderScaleX.animateTo(1f, spring(0.65f, 450f)) }
                                val snapTargetDp = (tabWidthDp * selectedIndex).value
                                sliderOffsetDp.animateTo(snapTargetDp, spring(0.7f, 600f))
                            }
                        }
                    )
                }
                .pointerInput(itemCount, tabWidthPx) {
                    detectTapGestures(
                        onPress = { offset ->
                            isPressed = true
                            dragFingerXPx = offset.x
                            val targetIdx = (offset.x / tabWidthPx).toInt().coerceIn(0, itemCount - 1)
                            coroutineScope.launch {
                                launch { chromaticAlpha.animateTo(1f, tween(80)) }
                                launch { sliderScaleX.animateTo(1.06f, tween(80)) }
                                sliderOffsetDp.animateTo(
                                    (tabWidthDp * targetIdx).value,
                                    spring(
                                        dampingRatio = config.jellySpring.dampingRatio,
                                        stiffness = config.jellySpring.stiffness
                                    )
                                )
                            }
                            val startTime = System.currentTimeMillis()
                            val released = tryAwaitRelease()
                            val elapsed = System.currentTimeMillis() - startTime
                            if (elapsed < 160) {
                                delay(160 - elapsed)
                            }
                            isPressed = false
                            coroutineScope.launch {
                                launch { chromaticAlpha.animateTo(0f, spring(0.75f, 380f)) }
                                launch { sliderScaleX.animateTo(1f, spring(0.65f, 450f)) }
                            }
                            if (released) {
                                onItemSelected(targetIdx)
                            }
                        }
                    )
                }
        )
        }
    }
}
