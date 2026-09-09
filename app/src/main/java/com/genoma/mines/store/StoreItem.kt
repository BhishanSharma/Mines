package com.genoma.mines.store

/**
 * The kinds of thing the store can sell. Adding a new kind of purchasable
 * content (e.g. "sound packs" later) means adding a case here, a matching
 * StoreItem subtype below, and whatever UI that category needs — those
 * are the only three places category support has to be wired in.
 */
enum class StoreCategory(val displayName: String) {
    BOARD_THEME("Board Themes"),
    CELL_SKIN("Cell Skins"),
    AVATAR("Avatars")
}

/**
 * Common contract for anything sellable in the store. This file defines
 * the shape only — it never lists actual items. Concrete items live one
 * per file under store/components/, and get assembled into the catalog
 * in StoreCatalog.kt.
 */
sealed interface StoreItem {
    val id: String
    val name: String
    val description: String

    /** Cost in in-game currency (coins). 0 means it's free/default. */
    val price: Int

    val category: StoreCategory
}

/**
 * A purchasable color palette for the board and cells — replaces the
 * default teal theme on the game screen once equipped.
 */
data class BoardThemeItem(
    override val id: String,
    override val name: String,
    override val description: String,
    override val price: Int,
    val previewColorHex: List<String>
) : StoreItem {
    override val category = StoreCategory.BOARD_THEME
}

/**
 * A purchasable cell appearance — how individual mine/flag/number cells
 * are drawn, independent of the board's color theme. `previewDrawableRes`
 * is nullable so an item can be defined before its art exists yet; the UI
 * should fall back to a generic placeholder icon when it's null.
 */
data class CellSkinItem(
    override val id: String,
    override val name: String,
    override val description: String,
    override val price: Int,
    val previewDrawableRes: Int? = null
) : StoreItem {
    override val category = StoreCategory.CELL_SKIN
}

/**
 * A purchasable profile avatar. Deliberately separate from the free
 * AvatarOption set already used on Home/Profile, so the free set and the
 * store's paid set can be extended independently without colliding.
 */
data class AvatarStoreItem(
    override val id: String,
    override val name: String,
    override val description: String,
    override val price: Int,
    val previewDrawableRes: Int? = null
) : StoreItem {
    override val category = StoreCategory.AVATAR
}