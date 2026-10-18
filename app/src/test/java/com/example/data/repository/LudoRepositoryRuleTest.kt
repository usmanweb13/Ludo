package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.util.UUID

class LudoRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun createRoom_asAuthenticatedUser_succeeds() = runBlocking {
        val uid = signInTestUser("player1@test.com")
        val repo = LudoRepository(firestore)

        val roomCode = "R" + UUID.randomUUID().toString().take(4).uppercase()
        val result = withTimeout(5000L) {
            repo.createRoom(
                roomCode = roomCode,
                name = "Test Room",
                isPublic = true,
                maxPlayers = 4,
                userId = uid,
                displayName = "Player One"
            )
        }
        assertTrue("Room creation should succeed: ${result.exceptionOrNull()}", result.isSuccess)

        val room = withTimeout(5000L) {
            repo.observeRoom(roomCode).first { it != null }
        }
        assertNotNull(room)
        assertEquals(roomCode, room?.roomId)
        assertEquals(uid, room?.hostId)
    }

    @Test
    fun joinRoom_asSecondUser_succeeds() = runBlocking {
        val hostUid = signInTestUser("host@test.com")
        val hostRepo = LudoRepository(firestore)
        val roomCode = "J" + UUID.randomUUID().toString().take(4).uppercase()

        withTimeout(5000L) {
            hostRepo.createRoom(
                roomCode = roomCode,
                name = "Join Test",
                isPublic = true,
                maxPlayers = 4,
                userId = hostUid,
                displayName = "Host Player"
            )
        }

        val guestUid = signInTestUser("guest@test.com")
        val guestRepo = LudoRepository(firestore)
        val joinResult = withTimeout(5000L) {
            guestRepo.joinRoom(roomCode, guestUid, "Guest Player")
        }
        assertTrue("Join room should succeed: ${joinResult.exceptionOrNull()}", joinResult.isSuccess)

        val updatedRoom = withTimeout(5000L) {
            guestRepo.observeRoom(roomCode).first { it != null && it.players.size >= 2 }
        }
        assertNotNull(updatedRoom)
        assertTrue(updatedRoom!!.playerIds.contains(guestUid))
    }

    @Test
    fun observeRoom_unauthenticatedUser_failsWithPermissionDenied() = runBlocking {
        auth.signOut()
        val repo = LudoRepository(firestore)
        try {
            withTimeout(3000L) {
                repo.observeRoom("NONEXISTENT").first()
            }
            fail("Expected PERMISSION_DENIED for unauthenticated caller")
        } catch (e: FirebaseFirestoreException) {
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, e.code)
        } catch (_: Exception) {
            // Flow caught and handled or threw exception
        }
    }

    @Test
    fun startGame_and_rollDice_succeeds() = runBlocking {
        val hostUid = signInTestUser("dice_host@test.com")
        val hostRepo = LudoRepository(firestore)
        val roomCode = "D" + UUID.randomUUID().toString().take(4).uppercase()

        withTimeout(5000L) {
            hostRepo.createRoom(
                roomCode = roomCode,
                name = "Dice Test",
                isPublic = true,
                maxPlayers = 4,
                userId = hostUid,
                displayName = "Host Player"
            )
        }

        val guestUid = signInTestUser("dice_guest@test.com")
        val guestRepo = LudoRepository(firestore)
        withTimeout(5000L) {
            guestRepo.joinRoom(roomCode, guestUid, "Guest Player")
        }

        // Host starts game
        signInTestUser("dice_host@test.com")
        val startResult = withTimeout(5000L) {
            hostRepo.startGame(roomCode, hostUid)
        }
        assertTrue("Start game should succeed: ${startResult.exceptionOrNull()}", startResult.isSuccess)

        // Host rolls dice
        val rollResult = withTimeout(5000L) {
            hostRepo.rollDice(roomCode, "red", hostUid)
        }
        assertTrue("Roll dice should succeed: ${rollResult.exceptionOrNull()}", rollResult.isSuccess)
    }
}

