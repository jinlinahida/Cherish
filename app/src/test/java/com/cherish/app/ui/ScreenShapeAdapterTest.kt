package com.cherish.app.ui

import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.cherish.app.settings.model.AppContentScale
import com.cherish.app.settings.ui.toShirokoWear
import io.github.jinlinahida.shirokowear.ui.ShirokoWearContentScale
import io.github.jinlinahida.shirokowear.ui.ShirokoWearScreenShape
import io.github.jinlinahida.shirokowear.ui.shirokoWearDimens
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests verifying Wear OS screen shape and content scale resolution & combination behaviors:
 * - Pure resolver from platform [isScreenRound] indicator
 * - Combination matrix (ROUND/SQUARE x SMALL/STANDARD/LARGE)
 * - Insets and title text alignment invariants between Round and Square bezels
 * - Content scale domain adapter mapping
 */
class ScreenShapeAdapterTest {

    @Test
    fun `resolveScreenShape resolves round and square shapes correctly`() {
        assertEquals(ShirokoWearScreenShape.ROUND, resolveScreenShape(isScreenRound = true))
        assertEquals(ShirokoWearScreenShape.SQUARE, resolveScreenShape(isScreenRound = false))
    }

    @Test
    fun `round standard combination produces centered titles and 20dp base horizontal insets`() {
        val dimens = shirokoWearDimens(
            contentScale = ShirokoWearContentScale.STANDARD,
            screenShape = ShirokoWearScreenShape.ROUND,
        )

        assertTrue(dimens.isRound)
        assertEquals(ShirokoWearScreenShape.ROUND, dimens.screenShape)
        assertEquals(TextAlign.Center, dimens.titleTextAlign)
        assertEquals(20.dp, dimens.horizontalPadding)
        assertEquals(24.dp, dimens.verticalPadding)
        assertEquals(8.dp, dimens.itemSpacing)
    }

    @Test
    fun `round large combination scales insets by 112 percent while preserving round title alignment`() {
        val dimens = shirokoWearDimens(
            contentScale = ShirokoWearContentScale.LARGE,
            screenShape = ShirokoWearScreenShape.ROUND,
        )

        assertTrue(dimens.isRound)
        assertEquals(TextAlign.Center, dimens.titleTextAlign)
        assertEquals(20.dp * 1.12f, dimens.horizontalPadding)
        assertEquals(24.dp * 1.12f, dimens.verticalPadding)
        assertEquals(8.dp * 1.12f, dimens.itemSpacing)
    }

    @Test
    fun `round small combination scales insets down by 88 percent while preserving round title alignment`() {
        val dimens = shirokoWearDimens(
            contentScale = ShirokoWearContentScale.SMALL,
            screenShape = ShirokoWearScreenShape.ROUND,
        )

        assertTrue(dimens.isRound)
        assertEquals(TextAlign.Center, dimens.titleTextAlign)
        assertEquals(20.dp * 0.88f, dimens.horizontalPadding)
        assertEquals(24.dp * 0.88f, dimens.verticalPadding)
        assertEquals(8.dp * 0.88f, dimens.itemSpacing)
    }

    @Test
    fun `square standard combination produces start-aligned titles and 16dp base horizontal insets`() {
        val dimens = shirokoWearDimens(
            contentScale = ShirokoWearContentScale.STANDARD,
            screenShape = ShirokoWearScreenShape.SQUARE,
        )

        assertFalse(dimens.isRound)
        assertEquals(ShirokoWearScreenShape.SQUARE, dimens.screenShape)
        assertEquals(TextAlign.Start, dimens.titleTextAlign)
        assertEquals(16.dp, dimens.horizontalPadding)
        assertEquals(24.dp, dimens.verticalPadding)
        assertEquals(8.dp, dimens.itemSpacing)
    }

    @Test
    fun `square large combination scales insets by 112 percent while preserving start-aligned titles`() {
        val dimens = shirokoWearDimens(
            contentScale = ShirokoWearContentScale.LARGE,
            screenShape = ShirokoWearScreenShape.SQUARE,
        )

        assertFalse(dimens.isRound)
        assertEquals(TextAlign.Start, dimens.titleTextAlign)
        assertEquals(16.dp * 1.12f, dimens.horizontalPadding)
        assertEquals(24.dp * 1.12f, dimens.verticalPadding)
        assertEquals(8.dp * 1.12f, dimens.itemSpacing)
    }

    @Test
    fun `square small combination scales insets down by 88 percent while preserving start-aligned titles`() {
        val dimens = shirokoWearDimens(
            contentScale = ShirokoWearContentScale.SMALL,
            screenShape = ShirokoWearScreenShape.SQUARE,
        )

        assertFalse(dimens.isRound)
        assertEquals(TextAlign.Start, dimens.titleTextAlign)
        assertEquals(16.dp * 0.88f, dimens.horizontalPadding)
        assertEquals(24.dp * 0.88f, dimens.verticalPadding)
        assertEquals(8.dp * 0.88f, dimens.itemSpacing)
    }

    @Test
    fun `square bezels provide strictly more horizontal usable width across all content scales`() {
        val scales = listOf(
            ShirokoWearContentScale.SMALL,
            ShirokoWearContentScale.STANDARD,
            ShirokoWearContentScale.LARGE,
        )

        for (scale in scales) {
            val roundDimens = shirokoWearDimens(contentScale = scale, screenShape = ShirokoWearScreenShape.ROUND)
            val squareDimens = shirokoWearDimens(contentScale = scale, screenShape = ShirokoWearScreenShape.SQUARE)

            assertTrue(
                "Square horizontal padding must be strictly less than round horizontal padding for scale $scale",
                squareDimens.horizontalPadding < roundDimens.horizontalPadding,
            )
            // Vertical padding remains identical between screen shapes for vertical balance
            assertEquals(roundDimens.verticalPadding, squareDimens.verticalPadding)
        }
    }

    @Test
    fun `app content scale adapter maps domain scales to shirokowear content scales correctly`() {
        assertEquals(ShirokoWearContentScale.SMALL, AppContentScale.SMALL.toShirokoWear())
        assertEquals(ShirokoWearContentScale.STANDARD, AppContentScale.STANDARD.toShirokoWear())
        assertEquals(ShirokoWearContentScale.LARGE, AppContentScale.LARGE.toShirokoWear())
    }
}
