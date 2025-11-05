package com.zybooks.focusflow.data

data class FocusSession(
    val id: Long,
    val startTimeMillis: Long,
    val durationMinutes: Int,
    val type: String,
    val completed: Boolean
)