package com.genoma.mines.session

import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class SessionManager(
    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()
) {

    val currentSession: UserSession
        get() = firebaseAuth.currentUser?.uid
            ?.let { UserSession.Authenticated(it) }
            ?: UserSession.Guest

    val sessionFlow: Flow<UserSession> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            val session = auth.currentUser?.uid
                ?.let { UserSession.Authenticated(it) }
                ?: UserSession.Guest

            trySend(session)
        }

        firebaseAuth.addAuthStateListener(listener)

        awaitClose {
            firebaseAuth.removeAuthStateListener(listener)
        }
    }

    fun signOut() {
        firebaseAuth.signOut()
    }
}