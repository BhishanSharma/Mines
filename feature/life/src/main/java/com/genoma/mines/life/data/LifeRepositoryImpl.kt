package com.genoma.mines.life.data
import com.genoma.mines.life.data.remote.FirestoreLifeRepository
import com.genoma.mines.life.domain.LifeSnapshot
import com.genoma.mines.life.domain.LifeStatus
import com.genoma.mines.session.SessionManager
import com.genoma.mines.session.UserSession
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest

@OptIn(ExperimentalCoroutinesApi::class)
class LifeRepositoryImpl(
    private val sessionManager: SessionManager,
    private val guestLife: LifeDataStore,
    private val firestoreLife: FirestoreLifeRepository
) : LifeRepository {

    override val lifeSnapshot: Flow<LifeSnapshot> =
        sessionManager.sessionFlow.flatMapLatest { session ->
            when (session) {
                is UserSession.Authenticated -> firestoreLife.observeLife(session.firebaseUid)
                UserSession.Guest -> guestLife.lifeSnapshot
            }
        }

    override suspend fun getStatus(): LifeStatus {
        return when (val session = sessionManager.currentSession) {
            is UserSession.Authenticated -> firestoreLife.getStatus(session.firebaseUid)
            UserSession.Guest -> guestLife.getStatus()
        }
    }

    override suspend fun consumeHeart(): Boolean {
        return when (val session = sessionManager.currentSession) {
            is UserSession.Authenticated -> firestoreLife.consumeHeart(session.firebaseUid)
            UserSession.Guest -> guestLife.consumeHeart()
        }
    }

    override suspend fun refundHeart() {
        when (val session = sessionManager.currentSession) {
            is UserSession.Authenticated -> firestoreLife.refundHeart(session.firebaseUid)
            UserSession.Guest -> guestLife.refundHeart()
        }
    }

    override suspend fun refillHearts() {
        when (val session = sessionManager.currentSession) {
            is UserSession.Authenticated -> firestoreLife.refillHearts(session.firebaseUid)
            UserSession.Guest -> guestLife.refillHearts()
        }
    }
}
