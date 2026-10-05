package com.cherish.app.settings.ui

import com.cherish.app.settings.model.AppContentScale
import io.github.jinlinahida.shirokowear.ui.ShirokoWearContentScale

/**
 * Adapter converting domain [AppContentScale] to [ShirokoWearContentScale].
 * Keeps domain logic clean and decoupled from the UI library.
 */
fun AppContentScale.toShirokoWear(): ShirokoWearContentScale = when (this) {
    AppContentScale.SMALL -> ShirokoWearContentScale.SMALL
    AppContentScale.STANDARD -> ShirokoWearContentScale.STANDARD
    AppContentScale.LARGE -> ShirokoWearContentScale.LARGE
}
