package com.genoma.mines.auth

import android.app.Activity
import android.content.Context
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.tasks.await

sealed class GoogleSignInResult {
    data class Success(val profile: UserProfile) : GoogleSignInResult()
    data class Failure(val message: String) : GoogleSignInResult()
    object Cancelled : GoogleSignInResult()
}

class GoogleAuthManager(
    private val context: Context,
    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()
) {

    private val credentialManager by lazy {
        CredentialManager.create(context)
    }

    suspend fun signIn(
        webClientId: String,
        activity: Activity
    ): GoogleSignInResult {

        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(webClientId)
            .setAutoSelectEnabled(false)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        return try {

            val response = credentialManager.getCredential(
                context = activity,
                request = request
            )

            val credential = response.credential

            if (
                credential is CustomCredential &&
                credential.type ==
                GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            ) {

                val googleIdTokenCredential =
                    GoogleIdTokenCredential.createFrom(credential.data)

                val firebaseCredential =
                    GoogleAuthProvider.getCredential(googleIdTokenCredential.idToken, null)

                val authResult = firebaseAuth.signInWithCredential(firebaseCredential).await()
                val firebaseUser = authResult.user

                if (firebaseUser == null) {
                    return GoogleSignInResult.Failure("Firebase sign-in returned no user")
                }

                GoogleSignInResult.Success(
                    UserProfile(
                        id = firebaseUser.uid,
                        displayName = googleIdTokenCredential.displayName
                            ?: firebaseUser.displayName,
                        email = firebaseUser.email,
                        photoUrl =
                            googleIdTokenCredential.profilePictureUri?.toString()
                                ?: firebaseUser.photoUrl?.toString()
                    )
                )

            } else {
                GoogleSignInResult.Failure(
                    "Unexpected credential type"
                )
            }

        } catch (e: GoogleIdTokenParsingException) {

            GoogleSignInResult.Failure(
                "Could not parse Google ID token"
            )

        } catch (e: GetCredentialException) {

            GoogleSignInResult.Failure(
                e.message ?: "Sign-in failed"
            )

        } catch (e: Exception) {

            GoogleSignInResult.Failure(
                e.message ?: "Firebase sign-in failed"
            )
        }
    }

    suspend fun signOut() {

        try {
            firebaseAuth.signOut()

            credentialManager.clearCredentialState(
                ClearCredentialStateRequest()
            )

        } catch (e: Exception) {
            // Nothing to clear.
        }
    }
}