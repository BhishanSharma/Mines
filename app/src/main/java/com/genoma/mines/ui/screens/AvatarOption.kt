package com.genoma.mines.ui.screens

import com.genoma.mines.R

enum class AvatarOption(
    val id: String,
    val drawableRes: Int,
    val contentDescription: String
) {
    MALE_1("male_01", R.drawable.avatar_male_01, "Male avatar 1"),
    MALE_2("male_02", R.drawable.avatar_male_02, "Male avatar 2"),
    MALE_3("male_03", R.drawable.avatar_male_03, "Male avatar 3"),
    FEMALE_1("female_01", R.drawable.avatar_female_01, "Female avatar 1"),
    FEMALE_2("female_02", R.drawable.avatar_female_02, "Female avatar 2"),
    FEMALE_3("female_03", R.drawable.avatar_female_03, "Female avatar 3");

    companion object {
        val Default = MALE_1

        fun fromId(id: String?): AvatarOption {
            return entries.find { it.id == id } ?: Default
        }
    }
}