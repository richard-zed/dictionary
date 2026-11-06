package rip.failed.iris.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import rip.failed.iris.R

/**
 * Quicksand, bundled rather than downloaded.
 *
 * Four static weights rather than the variable font: androidx.compose.ui.text
 * .font.Font takes one face per weight, and a variable axis would need
 * FontVariation.Settings threaded through every style.
 *
 * The files are shipped byte-identical. Quicksand's OFL carries a Reserved Font
 * Name, so they must not be altered or rebranded. The licence sits at
 * QUICKSAND-OFL.txt in the repo root, NOT in res/font, where aapt treats every
 * file as a resource and a .txt fails the build.
 */
val Quicksand = FontFamily(
    Font(R.font.quicksand_regular, FontWeight.Normal),
    Font(R.font.quicksand_medium, FontWeight.Medium),
    Font(R.font.quicksand_semibold, FontWeight.SemiBold),
    Font(R.font.quicksand_bold, FontWeight.Bold),
)

/**
 * The M3 ramp with only the family swapped.
 *
 * Copied field by field from Typography() rather than written out: the default
 * ramp carries size, line height and tracking that were tuned together, and
 * retyping them is how a body line ends up with a display's letter spacing.
 * Weights and sizes stay M3's.
 */
private fun quicksand(base: TextStyle) = base.copy(fontFamily = Quicksand)

val IrisTypography = Typography().let { t ->
    Typography(
        displayLarge = quicksand(t.displayLarge),
        displayMedium = quicksand(t.displayMedium),
        displaySmall = quicksand(t.displaySmall),
        headlineLarge = quicksand(t.headlineLarge),
        headlineMedium = quicksand(t.headlineMedium),
        headlineSmall = quicksand(t.headlineSmall),
        titleLarge = quicksand(t.titleLarge),
        titleMedium = quicksand(t.titleMedium),
        titleSmall = quicksand(t.titleSmall),
        bodyLarge = quicksand(t.bodyLarge),
        bodyMedium = quicksand(t.bodyMedium),
        bodySmall = quicksand(t.bodySmall),
        labelLarge = quicksand(t.labelLarge),
        labelMedium = quicksand(t.labelMedium),
        labelSmall = quicksand(t.labelSmall),
    )
}
