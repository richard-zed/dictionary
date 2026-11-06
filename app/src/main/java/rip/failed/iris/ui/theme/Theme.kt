package rip.failed.iris.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/** Used only below Android 12, where the wallpaper palette does not exist. */
val FallbackLight = lightColorScheme(
    primary = Color(0xFF4F5B92),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFDBE1FF),
    onPrimaryContainer = Color(0xFF071945),
    secondary = Color(0xFF5B5D72),
    onSecondary = Color(0xFFFFFFFF),
    background = Color(0xFFFCF8FD),
    surface = Color(0xFFFCF8FD),
    onSurface = Color(0xFF1B1B1F),
    onSurfaceVariant = Color(0xFF46464F),
)

val FallbackDark = darkColorScheme(
    primary = Color(0xFFB9C3FF),
    onPrimary = Color(0xFF212E60),
    primaryContainer = Color(0xFF374478),
    onPrimaryContainer = Color(0xFFDBE1FF),
    secondary = Color(0xFFC4C5DD),
    onSecondary = Color(0xFF2D2F42),
    background = Color(0xFF141218),
    surface = Color(0xFF141218),
    onSurface = Color(0xFFE4E1E9),
    onSurfaceVariant = Color(0xFFC7C5D0),
)
