package rip.failed.iris

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

class MainActivity : ComponentActivity() {

    /**
     * Text arriving from the share sheet, if any.
     *
     * ACTION_PROCESS_TEXT reaches LookupActivity directly, but some hosts only
     * offer a share target, so the same selection can arrive here instead. The
     * activity then opens on the entry rather than on search, which keeps the
     * two routes feeling like one feature.
     */
    private var sharedText by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        sharedText = intent?.readSharedText()

        setContent {
            IrisTheme {
                val incoming = sharedText
                if (incoming == null) {
                    SearchScreen()
                } else {
                    // Back from a shared lookup returns to search, not out of
                    // the app: the user may well want to look something else
                    // up, and dropping them back into the source app on the
                    // first back press would be a dead end here.
                    BackHandler(enabled = true) { sharedText = null }
                    SharedEntryScreen(incoming)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        sharedText = intent.readSharedText()
    }
}

/**
 * EXTRA_TEXT, or the ACTION_SEND text subtype. Both are CharSequence, so the
 * coercion happens once here rather than at each call site.
 */
private fun Intent.readSharedText(): String? {
    if (action != Intent.ACTION_SEND) return null
    if (type?.startsWith("text/") != true) return null
    val raw = getCharSequenceExtra(Intent.EXTRA_TEXT)?.toString() ?: return null
    return raw.takeIf { it.isNotBlank() }
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
