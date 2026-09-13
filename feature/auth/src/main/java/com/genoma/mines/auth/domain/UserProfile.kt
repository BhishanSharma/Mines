package com.genoma.mines.auth.domain
data class UserProfile(
    val id: String,
    val displayName: String?,
    val email: String?,
    val photoUrl: String?
)