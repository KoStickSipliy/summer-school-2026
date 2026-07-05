package com.volna.app.notifications

interface PushPermissionRequester {
    suspend fun requestPermission(reminderHours: List<Int>)
}

interface PushPermissionPromptStateStorage {
    suspend fun wasPromptRequested(): Boolean
    suspend fun markPromptRequested()
}

expect object PlatformPushPermissionRequester : PushPermissionRequester {
    override suspend fun requestPermission(reminderHours: List<Int>)
}

expect object PlatformPushPermissionPromptStateStorage : PushPermissionPromptStateStorage {
    override suspend fun wasPromptRequested(): Boolean
    override suspend fun markPromptRequested()
}
