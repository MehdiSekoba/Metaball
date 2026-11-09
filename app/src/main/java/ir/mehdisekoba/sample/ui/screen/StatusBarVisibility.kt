package ir.mehdisekoba.sample.ui.screen

import android.app.Activity
import android.view.WindowInsets
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalView

@Composable
internal fun StatusBarVisibility(hidden: Boolean) {
    val view = LocalView.current
    LaunchedEffect(hidden) {
        val controller = (view.context as? Activity)?.window?.insetsController ?: return@LaunchedEffect
        if (hidden) controller.hide(WindowInsets.Type.statusBars()) else controller.show(WindowInsets.Type.statusBars())
    }
}
