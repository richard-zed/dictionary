package rip.failed.iris

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

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
    MaterialTheme(colorScheme = colorScheme, content = content)
}
