package com.tomady.nutrition.worker

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * A domain event representing a newly generated daily suggestion for a specific user.
 *
 * The worker posts instances of this event after successfully generating a
 * personalised dish suggestion. The bridge layer observes the event bus and
 * forwards the event to the React Native host via [DeviceEventEmitter].
 *
 * @property userId    The target user identifier.
 * @property dishId    The ID of the generated [Dish] in the local database.
 * @property dishName  Human-readable name of the suggested dish.
 * @property date      The date for which the suggestion was generated (yyyy-MM-dd).
 * @property isCompatible Whether the suggestion is compatible with the user's health profile.
 * @property warnings  List of health warnings associated with the suggestion.
 */
data class SuggestionEvent(
    val userId: String,
    val dishId: String,
    val dishName: String,
    val date: String,
    val isCompatible: Boolean,
    val warnings: List<String>
)

/**
 * Singleton event bus for propagating [SuggestionEvent]s from background
 * [DailySuggestionWorker] execution to the React Native bridge layer.
 *
 * ## Usage
 * In the worker:
 * ```kotlin
 * SuggestionEventBus.post(event)
 * ```
 *
 * In the bridge module (e.g., [DietModule]):
 * ```kotlin
 * launch {
 *     SuggestionEventBus.events.collect { event ->
 *         // emit via DeviceEventEmitter
 *     }
 * }
 * ```
 *
 * The underlying [MutableSharedFlow] is configured with:
 * - `replay = 0` — new subscribers only receive future events, not past ones.
 * - `extraBufferCapacity = 10` — accommodates bursts of suggestions without
 *    suspending the producer (the worker).
 */
object SuggestionEventBus {

    private val _events = MutableSharedFlow<SuggestionEvent>(
        replay = 0,
        extraBufferCapacity = 10
    )

    /**
     * A cold [SharedFlow] that emits [SuggestionEvent]s as they are produced.
     *
     * Collectors should use [kotlinx.coroutines.flow.catch] to handle any
     * unexpected errors during collection.
     */
    val events: SharedFlow<SuggestionEvent> = _events.asSharedFlow()

    /**
     * Posts a new [SuggestionEvent] to all active collectors.
     *
     * This is safe to call from any thread (the worker runs on [Dispatchers.IO]).
     *
     * @param event The domain event to propagate.
     */
    fun post(event: SuggestionEvent) {
        _events.tryEmit(event)
    }
}
