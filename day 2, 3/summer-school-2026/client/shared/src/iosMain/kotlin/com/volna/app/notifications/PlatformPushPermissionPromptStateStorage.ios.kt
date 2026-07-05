package com.volna.app.notifications

import platform.Foundation.NSUserDefaults

actual object PlatformPushPermissionPromptStateStorage : PushPermissionPromptStateStorage {
    private val defaults: NSUserDefaults
        get() = NSUserDefaults.standardUserDefaults

    actual override suspend fun wasPromptRequested(): Boolean =
        defaults.boolForKey(KEY_PROMPT_REQUESTED)

    actual override suspend fun markPromptRequested() {
        defaults.setBool(true, forKey = KEY_PROMPT_REQUESTED)
    }

    private const val KEY_PROMPT_REQUESTED = "volna_push_prompt_requested"
}
