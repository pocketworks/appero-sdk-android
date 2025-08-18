package com.appero.sdk

import android.content.SharedPreferences
import com.appero.sdk.data.remote.ApperoApiService
import com.appero.sdk.domain.repository.FeedbackRepository
import com.appero.sdk.data.local.queue.OfflineFeedbackQueue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class OfflineFeedbackQueueTest {

	private lateinit var feedbackRepository: FeedbackRepository
	private lateinit var offlineFeedbackQueue: OfflineFeedbackQueue
 
	@Before
	fun setUp() {
		// In-memory SharedPreferences to avoid Android framework/editor mocking
		sharedPreferences = InMemorySharedPreferences()
		// Create API service (not used in these tests as we don't call submitFeedback)
		val apiService = ApperoApiService.create("test-api-key")
		feedbackRepository = FeedbackRepository(sharedPreferences, apiService)
		offlineFeedbackQueue = OfflineFeedbackQueue(feedbackRepository, CoroutineScope(Dispatchers.IO))
		// Ensure offline so background processing never attempts network
		offlineFeedbackQueue.onNetworkStateChanged(false)
	}

    @Test
    fun queueFeedback_shouldIncreaseQueueSize() {
        val initial = offlineFeedbackQueue.getQueueSize()
        offlineFeedbackQueue.queueFeedback(5, "Great app!")
        val after = offlineFeedbackQueue.getQueueSize()
        assertEquals(initial + 1, after)
    }

	@Test
	fun clearQueue_shouldEmptyAllItems() {
		offlineFeedbackQueue.queueFeedback(4, "Nice")
		offlineFeedbackQueue.queueFeedback(3, "Ok")
		assertEquals(2, offlineFeedbackQueue.getQueueSize())
		offlineFeedbackQueue.clearQueue()
		assertEquals(0, offlineFeedbackQueue.getQueueSize())
	}

	@Test
	fun queueFeedback_shouldLimitQueueSizeToMax() {
		// Push more than the internal MAX_QUEUE_SIZE (100)
		repeat(150) { i ->
			offlineFeedbackQueue.queueFeedback((i % 5) + 1, "msg-$i")
		}
		assertEquals(100, offlineFeedbackQueue.getQueueSize())
	}
}

/**
 * Minimal in-memory SharedPreferences for testing
 */
private class InMemorySharedPreferences : SharedPreferences {
	private val data = mutableMapOf<String, String>()

	override fun getAll(): MutableMap<String, *> = data
	override fun getString(key: String?, defValue: String?): String? = data[key] ?: defValue
	override fun getStringSet(key: String?, defValues: MutableSet<String>?): MutableSet<String>? = null
	override fun getInt(key: String?, defValue: Int): Int = data[key]?.toIntOrNull() ?: defValue
	override fun getLong(key: String?, defValue: Long): Long = data[key]?.toLongOrNull() ?: defValue
	override fun getFloat(key: String?, defValue: Float): Float = data[key]?.toFloatOrNull() ?: defValue
	override fun getBoolean(key: String?, defValue: Boolean): Boolean = data[key]?.toBoolean() ?: defValue
	override fun contains(key: String?): Boolean = data.containsKey(key)

	override fun edit(): SharedPreferences.Editor = Editor(data)
	override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {}
	override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {}

	private class Editor(private val data: MutableMap<String, String>) : SharedPreferences.Editor {
		private val pending = mutableMapOf<String, String?>()
		override fun putString(key: String?, value: String?): SharedPreferences.Editor { if (key != null) pending[key] = value; return this }
		override fun putStringSet(key: String?, values: MutableSet<String>?): SharedPreferences.Editor = this
		override fun putInt(key: String?, value: Int): SharedPreferences.Editor { if (key != null) pending[key] = value.toString(); return this }
		override fun putLong(key: String?, value: Long): SharedPreferences.Editor { if (key != null) pending[key] = value.toString(); return this }
		override fun putFloat(key: String?, value: Float): SharedPreferences.Editor { if (key != null) pending[key] = value.toString(); return this }
		override fun putBoolean(key: String?, value: Boolean): SharedPreferences.Editor { if (key != null) pending[key] = value.toString(); return this }
		override fun remove(key: String?): SharedPreferences.Editor { if (key != null) pending[key] = null; return this }
		override fun clear(): SharedPreferences.Editor { pending.clear(); data.clear(); return this }
		override fun commit(): Boolean { apply(); return true }
		override fun apply() { for ((k, v) in pending) { if (v == null) data.remove(k) else data[k] = v }; pending.clear() }
	}
} 