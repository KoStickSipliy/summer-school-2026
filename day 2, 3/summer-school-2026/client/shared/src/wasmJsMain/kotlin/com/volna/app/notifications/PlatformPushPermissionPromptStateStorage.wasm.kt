package com.volna.app.notifications

import kotlinx.browser.localStorage

actual object PlatformPushPermissionPromptStateStorage : PushPermissionPromptStateStorage {
    actual override suspend fun wasPromptRequested(): Boolean =
        localStorage.getItem(KEY_PROMPT_REQUESTED) == "true"

    actual override suspend fun markPromptRequested() {
        localStorage.setItem(KEY_PROMPT_REQUESTED, "true")
    }

    private const val KEY_PROMPT_REQUESTED = "volna_push_prompt_requested"
}
