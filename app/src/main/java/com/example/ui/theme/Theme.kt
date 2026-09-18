package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.example.util.DpiAdaptiveProvider
import com.example.util.DpiScaleMode

@Composable
fun CatalogManagerTheme(
    darkTheme: Boolean = false,
    primaryColor: Color = BluePrimary,
    primaryContainerColor: Color = BlueLight,
    fontFamily: FontFamily = FontFamily.SansSerif,
    fontScaleMultiplier: Float = 1.0f,
    cornerRadiusDp: Int = 16,
    dpiScaleMode: DpiScaleMode = DpiScaleMode.AUTO,
    dpiCustomScale: Float = 1.0f,
    dynamicColor: Boolean = false, // Set to false to maintain crisp brand identity
    content: @Composable () -> Unit,
) {
    val lightColorScheme = lightColorScheme(
        primary = primaryColor,
        primaryContainer = primaryContainerColor,
        secondary = primaryColor,
        tertiary = EmeraldSuccess,
        background = SlateBackground,
        surface = SlateSurface,
        onPrimary = Color.White,
        onBackground = SlateTextPrimary,
        onSurface = SlateTextPrimary
    )

    val darkColorScheme = darkColorScheme(
        primary = primaryContainerColor,
        primaryContainer = primaryColor,
        secondary = primaryColor,
        tertiary = EmeraldSuccess,
        background = Color(0xFF14181F),
        surface = Color(0xFF1F242D),
        onPrimary = Color(0xFF0F172A),
        onBackground = Color(0xFFF7F9FC),
        onSurface = Color(0xFFF7F9FC)
    )

    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> darkColorScheme
        else -> lightColorScheme
    }

    val customTypography = remember(fontFamily, fontScaleMultiplier) {
        getCustomTypography(fontFamily, fontScaleMultiplier)
    }

    val customShapes = remember(cornerRadiusDp) {
        Shapes(
            small = RoundedCornerShape((cornerRadiusDp / 2).coerceAtLeast(2).dp),
            medium = RoundedCornerShape(cornerRadiusDp.dp),
            large = RoundedCornerShape((cornerRadiusDp * 1.25f).toInt().dp)
        )
    }

    DpiAdaptiveProvider(
        scaleMode = dpiScaleMode,
        customScale = dpiCustomScale
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = customTypography,
            shapes = customShapes,
            content = content
        )
    }
}

