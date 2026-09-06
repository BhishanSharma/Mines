package com.genoma.mines.session

sealed class UserSession {
    data class Authenticated(val firebaseUid: String) : UserSession()
    object Guest : UserSession()
}