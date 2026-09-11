package com.genoma.mines.store


enum class StoreCategory(val displayName: String) {
    BOARD_THEME("Board Themes"),
    CELL_SKIN("Cell Skins"),
    AVATAR("Avatars")
}

sealed interface StoreItem {
    val id: String
    val name: String
    val description: String

    /** Cost in in-game currency (coins). 0 means it's free/default. */
    val price: Int

    val category: StoreCategory
}


data class BoardThemeItem(
    override val id: String,
    override val name: String,
    override val description: String,
    override val price: Int,
    val previewColorHex: List<String>,
    val style: BoardThemeStyle
) : StoreItem {
    override val category = StoreCategory.BOARD_THEME
}


data class CellSkinItem(
    override val id: String,
    override val name: String,
    override val description: String,
    override val price: Int,
    val previewDrawableRes: Int? = null
) : StoreItem {
    override val category = StoreCategory.CELL_SKIN
}


data class AvatarStoreItem(
    override val id: String,
    override val name: String,
    override val description: String,
    override val price: Int,
    val previewDrawableRes: Int? = null
) : StoreItem {
    override val category = StoreCategory.AVATAR
}