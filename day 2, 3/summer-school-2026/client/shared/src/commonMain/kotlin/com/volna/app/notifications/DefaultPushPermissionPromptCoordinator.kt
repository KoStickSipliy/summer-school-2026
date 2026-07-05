package com.volna.app.notifications

import com.volna.app.core.logging.AppLogger
import com.volna.app.domain.model.Booking

interface PushPermissionPromptCoordinator {
    suspend fun requestIfNeeded(booking: Booking): List<Int>?
}

class DefaultPushPermissionPromptCoordinator(
    private val stateStorage: PushPermissionPromptStateStorage,
    private val permissionRequester: PushPermissionRequester,
) : PushPermissionPromptCoordinator {
    override suspend fun requestIfNeeded(booking: Booking): List<Int>? {
        if (booking.isFirstBooking != true) return null

        val alreadyRequested = runCatching {
            stateStorage.wasPromptRequested()
        }.getOrElse { failure ->
            AppLogger.e(failure, "Failed to read push prompt state; fallback to false")
            false
        }
        if (alreadyRequested) return null

        runCatching {
            stateStorage.markPromptRequested()
        }.onFailure { failure ->
            AppLogger.e(failure, "Failed to persist push prompt state")
        }

        val reminderHours = booking.reminderHours
            .orEmpty()
            .filter { it > 0 }
            .distinct()

        runCatching {
            permissionRequester.requestPermission(reminderHours)
        }.onFailure { failure ->
            AppLogger.e(failure, "Failed to request push permission")
        }

        return reminderHours.takeIf { it.isNotEmpty() }
    }
}
