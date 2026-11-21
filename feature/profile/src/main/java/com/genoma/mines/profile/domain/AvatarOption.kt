package com.genoma.mines.profile.domain
import com.genoma.mines.profile.R
import com.genoma.mines.core.ui.R as CoreUiR

/**
 * Player avatars. Free avatars have no [storeItemId]; premium avatars are
 * sold in the Store and can only be equipped once owned.
 */
enum class AvatarOption(
    val id: String,
    val drawableRes: Int,
    val contentDescription: String,
    val storeItemId: String? = null
) {
    MALE_1("male_01", R.drawable.avatar_male_01, "Male avatar 1"),
    MALE_2("male_02", R.drawable.avatar_male_02, "Male avatar 2"),
    MALE_3("male_03", R.drawable.avatar_male_03, "Male avatar 3"),
    FEMALE_1("female_01", R.drawable.avatar_female_01, "Female avatar 1"),
    FEMALE_2("female_02", R.drawable.avatar_female_02, "Female avatar 2"),
    FEMALE_3("female_03", R.drawable.avatar_female_03, "Female avatar 3"),
    ROBO("robo", CoreUiR.drawable.avatar_robo, "Robot avatar", storeItemId = "avatar_robo");

    val isPremium: Boolean
        get() = storeItemId != null

    /** Free avatars are always usable; premium ones need their store item. */
    fun isUsableWith(ownedStoreItemIds: Set<String>): Boolean =
        storeItemId == null || storeItemId in ownedStoreItemIds

    companion object {
        val Default = MALE_1

        fun fromId(id: String?): AvatarOption {
            return entries.find { it.id == id } ?: Default
        }

        fun forStoreItem(storeItemId: String): AvatarOption? =
            entries.find { it.storeItemId == storeItemId }

        /** The saved avatar, falling back to the default when it isn't owned (e.g. after signing out). */
        fun resolve(id: String?, ownedStoreItemIds: Set<String>): AvatarOption =
            fromId(id).takeIf { it.isUsableWith(ownedStoreItemIds) } ?: Default
    }
}
