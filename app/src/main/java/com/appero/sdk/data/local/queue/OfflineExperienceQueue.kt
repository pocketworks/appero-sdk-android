package com.appero.sdk.data.local.queue

import android.util.Log
import com.appero.sdk.domain.repository.ExperienceRepository
import com.appero.sdk.domain.repository.ExperienceSubmissionResult
import com.appero.sdk.domain.repository.QueuedExperience
import com.appero.sdk.util.DateTimeUtils.getCurrentTimestamp
import com.appero.sdk.debug.ApperoLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.util.Timer
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.timer

internal class OfflineExperienceQueue(
    private val experienceRepository: ExperienceRepository,
    private val scope: CoroutineScope
) {
    companion object {
        private const val MAX_RETRY_ATTEMPTS = 5
        private const val MAX_QUEUE_SIZE = 200
        private const val RETRY_TIMER_INTERVAL_MS = 180_000L // 3 minutes
    }

    private var retryTimer: Timer? = null
    private var isNetworkAvailable = false
    private val isProcessing = AtomicBoolean(false)

    init { startRetryTimer() }

    private fun startRetryTimer() {
        stopRetryTimer()
        retryTimer = timer(
            name = "ApperoExpRetryTimer",
            daemon = true,
            initialDelay = RETRY_TIMER_INTERVAL_MS,
            period = RETRY_TIMER_INTERVAL_MS
        ) { scope.launch { processQueue() } }
    }

    private fun stopRetryTimer() { retryTimer?.cancel(); retryTimer = null }

    fun onNetworkStateChanged(isAvailable: Boolean) {
        isNetworkAvailable = isAvailable
        if (isAvailable && getQueueSize() > 0) {
            scope.launch { processQueue() }
        }
    }

    fun queueExperience(value: Int, context: String) {
        val item = QueuedExperience(
            id = UUID.randomUUID().toString(),
            value = value,
            context = context,
            sentAt = getCurrentTimestamp(),
            retryCount = 0
        )
        val current = experienceRepository.getQueuedExperiences().toMutableList()
        if (current.size >= MAX_QUEUE_SIZE) {
            ApperoLogger.warning("Queue full, removing oldest item")
            current.removeAt(0)
        }
        current.add(item)
        experienceRepository.saveQueuedExperiences(current)
    }

    fun processQueue() {
        if (!isNetworkAvailable) {
            return
        }
        if (!isProcessing.compareAndSet(false, true)) {
            return
        }

        // Take a snapshot and clear stored queue before processing to avoid duplicate batch triggers
        val snapshot = experienceRepository.getQueuedExperiences()
        if (snapshot.isEmpty()) {
            isProcessing.set(false)
            return
        }
        experienceRepository.saveQueuedExperiences(emptyList())

        scope.launch {
            try {
                val remaining = mutableListOf<QueuedExperience>()
                for (item in snapshot) {
                    try {
                        val clientId = com.appero.sdk.Appero.getClientId()
                        if (clientId == null) {
                            remaining.add(item)
                            continue
                        }
                        val result = experienceRepository.submitExperience(
                            clientId = clientId,
                            value = item.value,
                            context = item.context,
                            sentAt = item.sentAt,
                            allowRetry = true
                        )
                        when (result) {
                            is ExperienceSubmissionResult.Success -> {
                                // Successfully submitted
                            }
                            is ExperienceSubmissionResult.Error -> {
                                if (item.retryCount < MAX_RETRY_ATTEMPTS) {
                                    remaining.add(item.copy(retryCount = item.retryCount + 1))
                                } else {
                                    ApperoLogger.logApiError("/api/experience", "POST", "Max retries reached for item ${item.id}")
                                }
                            }
                        }
                    } catch (_: Exception) {
                        if (item.retryCount < MAX_RETRY_ATTEMPTS) {
                            remaining.add(item.copy(retryCount = item.retryCount + 1))
                        } else {
                            ApperoLogger.logNetworkError("Experience Queue Processing", "Max retries reached for item ${item.id}")
                        }
                    }
                }

                // Save remaining items back to queue
                experienceRepository.saveQueuedExperiences(remaining)
            } finally {
                isProcessing.set(false)
            }
        }
    }

    fun getQueueSize(): Int {
        return experienceRepository.getQueuedExperiences().size
    }

    fun clearQueue() {
        experienceRepository.saveQueuedExperiences(emptyList())
    }

    fun cleanup() {
        stopRetryTimer()
    }
} 