package com.genoma.mines.store.domain

import com.genoma.mines.store.data.StoreCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StoreItemStatesTest {

    private fun resolve(
        price: Int = 20,
        owned: Boolean = false,
        equipped: Boolean = false,
        isDefault: Boolean = false,
        canApply: Boolean = true,
        gems: Int = 0
    ) = StoreItemStates.resolve(price, owned, equipped, isDefault, canApply, gems)

    @Test
    fun `affordable unowned item shows Buy`() {
        assertEquals(StoreItemAction.BUY, resolve(gems = 20).action)
    }

    @Test
    fun `unaffordable item shows the shortfall`() {
        val state = resolve(gems = 15)
        assertEquals(StoreItemAction.NEED_MORE_GEMS, state.action)
        assertEquals(5, state.gemsShort)
    }

    @Test
    fun `owned item shows Apply regardless of balance`() {
        assertEquals(StoreItemAction.APPLY, resolve(owned = true, gems = 0).action)
    }

    @Test
    fun `equipped premium item shows Applied with Unequip`() {
        val state = resolve(owned = true, equipped = true)
        assertEquals(StoreItemAction.APPLIED, state.action)
        assertTrue(state.canUnequip)
    }

    @Test
    fun `equipped default can't be unequipped`() {
        val state = resolve(price = 0, equipped = true, isDefault = true)
        assertEquals(StoreItemAction.APPLIED, state.action)
        assertFalse(state.canUnequip)
    }

    @Test
    fun `free items count as owned`() {
        assertEquals(StoreItemAction.APPLY, resolve(price = 0, isDefault = true).action)
    }

    @Test
    fun `owned items that can't be applied show Owned`() {
        assertEquals(StoreItemAction.OWNED, resolve(owned = true, canApply = false).action)
    }

    @Test
    fun `unequipping keeps ownership so the item returns to Apply`() {
        val applied = resolve(owned = true, equipped = true)
        val afterUnequip = resolve(owned = true, equipped = false)
        assertEquals(StoreItemAction.APPLIED, applied.action)
        assertEquals(StoreItemAction.APPLY, afterUnequip.action)
    }

    @Test
    fun `catalog default theme is listed and free`() {
        val default = StoreCatalog.boardThemeById(StoreCatalog.DEFAULT_BOARD_THEME_ID)
        assertEquals(0, default?.price)
    }

    @Test
    fun `featured item must name a listed item`() {
        val visible = StoreCatalog.visibleItems(newThemeEnabled = false, ownedItemIds = emptySet())
        assertEquals("cellskin_frost", StoreCatalog.featuredItemId(" cellskin_frost ", visible))
        assertNull(StoreCatalog.featuredItemId("does_not_exist", visible))
        assertNull(StoreCatalog.featuredItemId("", visible))
    }

    @Test
    fun `every catalog item is visible when no item is flag gated`() {
        assertEquals(
            StoreCatalog.allItems,
            StoreCatalog.visibleItems(newThemeEnabled = false, ownedItemIds = emptySet())
        )
    }
}
