package com.volna.app.notifications

import com.volna.app.core.logging.AppLogger

actual object PlatformPushPermissionRequester : PushPermissionRequester {
    actual override suspend fun requestPermission(reminderHours: List<Int>) {
        AppLogger.d("Push permission request triggered (wasm), reminderHours=$reminderHours")
    }
}
