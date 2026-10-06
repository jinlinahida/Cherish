package com.cherish.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import io.github.jinlinahida.shirokowear.ui.ShirokoWearScreenShape

/**
 * Detects the runtime [ShirokoWearScreenShape] from the current Compose [LocalConfiguration].
 *
 * Keyed on [Configuration.isScreenRound] to ensure the shape is resolved once per configuration lifecycle
 * and never re-allocated per recomposition frame.
 */
@Composable
fun rememberScreenShape(): ShirokoWearScreenShape {
    val configuration = LocalConfiguration.current
    return remember(configuration.isScreenRound) {
        resolveScreenShape(configuration.isScreenRound)
    }
}
