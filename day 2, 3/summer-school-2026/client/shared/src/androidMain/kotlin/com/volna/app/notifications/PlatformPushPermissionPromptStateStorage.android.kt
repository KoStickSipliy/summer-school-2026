package com.volna.app.notifications

import android.content.Context
import android.content.SharedPreferences

actual object PlatformPushPermissionPromptStateStorage : PushPermissionPromptStateStorage {
    private var preferences: SharedPreferences? = null
    private var fallbackRequested: Boolean = false

    fun initialize(context: Context) {
        preferences = context.applicationContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
        if (fallbackRequested) {
            preferences?.edit()?.putBoolean(KEY_PROMPT_REQUESTED, true)?.apply()
        }
    }

    actual override suspend fun wasPromptRequested(): Boolean =
        preferences?.getBoolean(KEY_PROMPT_REQUESTED, false) ?: fallbackRequested

    actual override suspend fun markPromptRequested() {
        fallbackRequested = true
        preferences?.edit()?.putBoolean(KEY_PROMPT_REQUESTED, true)?.apply()
    }

    private const val PREFERENCES_NAME = "volna_notifications"
    private const val KEY_PROMPT_REQUESTED = "push_prompt_requested"
}
