package rip.failed.iris

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { IrisTheme { SearchScreen() } }
    }
}

/**
 * Material You when the device has it, our own palette when it does not.
 *
 * `dynamicLightColorScheme` reads the wallpaper-derived palette on Android 12+;
 * below that it throws, so the version check is load-bearing rather than
 * defensive.
 */
@Composable
fun IrisTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val colorScheme = when {
        android.os.Build.VERSION.SDK_INT >= 31 ->
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        darkTheme -> rip.failed.iris.ui.theme.FallbackDark
        else -> rip.failed.iris.ui.theme.FallbackLight
    }
    // System bars follow the Compose scheme rather than the platform theme.
    // enableEdgeToEdge() derives them from whichever theme the window was
    // created with, which here is a plain Material parent and knows nothing
    // about the wallpaper palette -- so a Material You build would draw tonal
    // surfaces under system bars tinted from somewhere else.
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = rip.failed.iris.ui.theme.IrisTypography,
        content = content,
    )
}
