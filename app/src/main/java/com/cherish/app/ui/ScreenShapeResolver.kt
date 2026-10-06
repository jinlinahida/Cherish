package com.cherish.app.ui

import io.github.jinlinahida.shirokowear.ui.ShirokoWearScreenShape

/**
 * Resolves the appropriate [ShirokoWearScreenShape] based on whether the physical
 * display is reported as round by the system.
 *
 * Pure Kotlin function strictly decoupled from Android framework and Composable runtime
 * to ensure 100% JVM unit testability.
 */
fun resolveScreenShape(isScreenRound: Boolean): ShirokoWearScreenShape =
    if (isScreenRound) ShirokoWearScreenShape.ROUND else ShirokoWearScreenShape.SQUARE
