package com.genoma.mines.store.data.catalog
import com.genoma.mines.core.ui.R as CoreUiR
import com.genoma.mines.store.domain.AvatarStoreItem

/** Premium avatar; once owned it can be equipped from the Store or Profile. */
val roboAvatar = AvatarStoreItem(
    id = "avatar_robo",
    name = "Robo",
    description = "A robot avatar for your profile.",
    price = 15,
    previewDrawableRes = CoreUiR.drawable.avatar_robo
)