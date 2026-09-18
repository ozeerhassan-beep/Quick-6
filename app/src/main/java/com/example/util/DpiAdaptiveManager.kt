package com.example.util

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density

/**
 * Scaling modes for adapting Kwic-Kart to different phone DPIs and screen densities.
 */
enum class DpiScaleMode(val displayName: String, val description: String, val defaultMultiplier: Float) {
    AUTO(
        displayName = "Auto (Selon DPI)",
        description = "S'adapte dynamiquement selon la densité DPI et la largeur de votre écran",
        defaultMultiplier = 1.0f
    ),
    COMPACT(
        displayName = "Compact (0.88x)",
        description = "Haute densité DPI • Affiche plus de produits et contenus",
        defaultMultiplier = 0.88f
    ),
    STANDARD(
        displayName = "Standard (1.00x)",
        description = "Densité standard Android par défaut (100%)",
        defaultMultiplier = 1.00f
    ),
    CONFORT(
        displayName = "Confort (1.15x)",
        description = "Basse densité DPI • Textes et boutons agrandis pour confort visuel",
        defaultMultiplier = 1.15f
    ),
    EXTRA_LARGE(
        displayName = "Grand (1.25x)",
        description = "Très agrandi • Accessibilité et lisibilité maximale",
        defaultMultiplier = 1.25f
    ),
    CUSTOM(
        displayName = "Personnalisé",
        description = "Ajustement manuel au pourcentage près",
        defaultMultiplier = 1.0f
    )
}

/**
 * Comprehensive specifications of the user's phone display and DPI metrics.
 */
data class DpiDeviceInfo(
    val densityDpi: Int,
    val densityScale: Float,
    val densityBucket: String,
    val screenWidthDp: Int,
    val screenHeightDp: Int,
    val smallestScreenWidthDp: Int,
    val deviceCategory: String,
    val calculatedAutoMultiplier: Float,
    val effectiveScale: Float,
    val mode: DpiScaleMode
)

object DpiAdaptiveManager {

    /**
     * Resolves human-readable Android density bucket name based on DPI.
     */
    fun getDensityBucketName(densityDpi: Int): String {
        return when {
            densityDpi <= 120 -> "ldpi (~120 DPI)"
            densityDpi <= 160 -> "mdpi (~160 DPI • Baseline)"
            densityDpi <= 240 -> "hdpi (~240 DPI)"
            densityDpi <= 320 -> "xhdpi (~320 DPI)"
            densityDpi <= 440 -> "xxhdpi (~400-440 DPI • Standard)"
            densityDpi <= 480 -> "xxhdpi (~480 DPI • Haute densité)"
            densityDpi <= 560 -> "xxxhdpi (~560 DPI)"
            else -> "xxxhdpi (~${densityDpi} DPI • Ultra Haute densité)"
        }
    }

    /**
     * Resolves device category label based on screen width in dp.
     */
    fun getDeviceCategory(screenWidthDp: Int): String {
        return when {
            screenWidthDp < 350 -> "Smartphone Compact (< 350dp)"
            screenWidthDp <= 420 -> "Smartphone Standard (350–420dp)"
            screenWidthDp < 600 -> "Grand Smartphone / Phablet"
            screenWidthDp < 840 -> "Tablette Moyenne / Pliable Ouvert"
            else -> "Grande Tablette / Bureau"
        }
    }

    /**
     * Dynamically computes the optimal adaptive scale multiplier based on actual phone DPI
     * and screen width in dp.
     */
    fun computeAutoMultiplier(densityDpi: Int, screenWidthDp: Int): Float {
        // Base width calculation
        val widthMultiplier = when {
            screenWidthDp < 340 -> 0.86f // Very compact or huge system display scaling
            screenWidthDp < 370 -> 0.92f // Narrow phone
            screenWidthDp <= 430 -> 1.00f // Standard modern smartphone baseline
            screenWidthDp < 500 -> 1.04f // Wider phone
            screenWidthDp < 600 -> 1.06f // Extra wide phablet
            else -> 1.10f               // Tablet / Foldable
        }

        // DPI adjustment factor
        val dpiFactor = when {
            densityDpi >= 560 && screenWidthDp <= 380 -> -0.04f // Very dense narrow display
            densityDpi <= 200 && screenWidthDp >= 400 -> 0.04f  // Low density large display
            else -> 0.0f
        }

        return (widthMultiplier + dpiFactor).coerceIn(0.80f, 1.30f)
    }

    /**
     * Builds full device info and resolves the effective scale factor.
     */
    fun calculateDeviceInfo(
        configuration: Configuration,
        densityScale: Float,
        mode: DpiScaleMode,
        customScale: Float
    ): DpiDeviceInfo {
        val densityDpi = configuration.densityDpi
        val screenWidthDp = configuration.screenWidthDp
        val screenHeightDp = configuration.screenHeightDp
        val smallestScreenWidthDp = configuration.smallestScreenWidthDp

        val autoMultiplier = computeAutoMultiplier(densityDpi, screenWidthDp)

        val effectiveScale = when (mode) {
            DpiScaleMode.AUTO -> autoMultiplier
            DpiScaleMode.CUSTOM -> customScale.coerceIn(0.75f, 1.35f)
            else -> mode.defaultMultiplier
        }

        return DpiDeviceInfo(
            densityDpi = densityDpi,
            densityScale = densityScale,
            densityBucket = getDensityBucketName(densityDpi),
            screenWidthDp = screenWidthDp,
            screenHeightDp = screenHeightDp,
            smallestScreenWidthDp = smallestScreenWidthDp,
            deviceCategory = getDeviceCategory(screenWidthDp),
            calculatedAutoMultiplier = autoMultiplier,
            effectiveScale = effectiveScale,
            mode = mode
        )
    }
}

val LocalDpiAdaptiveScale = compositionLocalOf { 1.0f }
val LocalDpiDeviceInfo = compositionLocalOf<DpiDeviceInfo> {
    DpiDeviceInfo(
        densityDpi = 420,
        densityScale = 2.625f,
        densityBucket = "xxhdpi (420 DPI)",
        screenWidthDp = 411,
        screenHeightDp = 891,
        smallestScreenWidthDp = 411,
        deviceCategory = "Smartphone Standard",
        calculatedAutoMultiplier = 1.0f,
        effectiveScale = 1.0f,
        mode = DpiScaleMode.AUTO
    )
}

/**
 * Composition provider that automatically resizes and scales all dp and sp elements
 * throughout the entire Jetpack Compose tree based on the phone's DPI and user preference.
 */
@Composable
fun DpiAdaptiveProvider(
    scaleMode: DpiScaleMode = DpiScaleMode.AUTO,
    customScale: Float = 1.0f,
    content: @Composable () -> Unit
) {
    val configuration = LocalConfiguration.current
    val baseDensity = LocalDensity.current

    val deviceInfo = remember(configuration, baseDensity.density, scaleMode, customScale) {
        DpiAdaptiveManager.calculateDeviceInfo(
            configuration = configuration,
            densityScale = baseDensity.density,
            mode = scaleMode,
            customScale = customScale
        )
    }

    val effectiveScale = deviceInfo.effectiveScale

    // Modify the active Compose Density: dp and sp will scale proportionally
    val adaptiveDensity = remember(baseDensity, effectiveScale) {
        Density(
            density = baseDensity.density * effectiveScale,
            fontScale = baseDensity.fontScale * effectiveScale
        )
    }

    CompositionLocalProvider(
        LocalDensity provides adaptiveDensity,
        LocalDpiAdaptiveScale provides effectiveScale,
        LocalDpiDeviceInfo provides deviceInfo
    ) {
        content()
    }
}
