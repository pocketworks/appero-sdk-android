# Appero SDK for Android

The in-app feedback widget that drives organic growth.

## Requirements

- **Minimum Android API:** 24 (Android 7.0)
- **Target/Compile API:** 34+
- **UI Framework:** Jetpack Compose (primary), View system (XML) support in progress
- **Networking:** Retrofit + OkHttp
- **Storage:** SharedPreferences

---

## Installation

Add the SDK to your project via Maven Local (for now):

```kotlin
// In your app/build.gradle.kts
implementation("com.example.appero:appero-sdk-android:1.0.0")
```

---

## Getting Started

Initialize Appero in your `Application` or `MainActivity`:

```kotlin
Appero.start(
    context = this,
    apiKey = "your_api_key",
    clientId = null // Pass null or blank to auto-generate a unique user ID
)
```

- If you pass `null` or blank for `clientId`, the SDK will auto-generate and persist a UUID for the user (iOS parity).

---

## Logging User Experience (Experience Tracker)

Track user experience using either predefined levels or custom points:

```kotlin
// Likert scale (recommended, semantic)
Appero.log(Experience.VERY_POSITIVE)
Appero.log(Experience.POSITIVE)
Appero.log(Experience.NEUTRAL)
Appero.log(Experience.NEGATIVE)
Appero.log(Experience.VERY_NEGATIVE)

// Custom points (flexible)
Appero.log(2)   // Add 2 points
Appero.log(-1)  // Subtract 1 point
```

- The two overloads (`log(Experience)` and `log(Int)`) let you choose between semantic events and custom scoring.
- Each log updates the local experience score and also submits the event to the backend.
- The backend may instruct the SDK to show the feedback flow immediately (server-triggered prompt), including:
  - `flowType` (e.g. "frustration") which can start from a specific step
  - `feedbackUI` (dynamic title/subtitle/prompt)

### Thresholds & Prompting

```kotlin
Appero.ratingThreshold = 5 // Default is 5

if (Appero.shouldShowAppero()) {
    // You can choose to call Appero.showFeedbackPrompt(config) here
}
```

- `shouldShowAppero()` is a local check: true if score >= threshold AND user hasn’t submitted feedback yet.
- Prompts can be shown either by your app logic using the local check, or automatically when the server response requests it (after a log).

### Starting from a Specific Step

```kotlin
// You can start the flow at a specific step (e.g., frustration)
Appero.showFeedbackPrompt(config, initialStep = FeedbackStep.Frustration)
```

### Inspecting Experience State (for debugging)

```kotlin
val state = Appero.getExperienceState()
// state.userId, state.experiencePoints, state.ratingThreshold,
// state.shouldShowPrompt, state.hasSubmittedFeedback
```

---

## Showing the Feedback UI

### Jetpack Compose

Add the feedback prompt to your Compose hierarchy:

```kotlin
Appero.FeedbackPromptUI(
    config = FeedbackPromptConfig(
        title = "We value your feedback!",
        subtitle = "How was your experience?",
        followUpQuestion = "Tell us more...",
        placeholder = "Type your feedback here",
        submitText = "Send",
        maxCharacters = 120
    ),
    flowConfig = FeedbackFlowConfig(
        rateUsTitle = "Enjoying our app?",
        rateUsSubtitle = "Please rate us on the Play Store!",
        rateUsCtaText = "Rate Now",
        thankYouMessage = "Thank you for your feedback!"
    ),
    reviewPromptThreshold = 4, // Show Play Store review prompt if rating >= 4
    onRequestReview = { activity ->
        Appero.requestPlayStoreReview(activity)
    }
)
```

To show the prompt programmatically:

```kotlin
Appero.showFeedbackPrompt(config)
```

### XML/View System (Work in Progress)

> **Note:** XML and traditional View system support is currently a work in progress. Future releases will provide DialogFragment-based and XML integration for legacy apps.

---

## Theming

Customize the feedback UI to match your brand:

```kotlin
Appero.theme = CustomTheme(
    accentColor = Color(0xFF6200EE),
    textColor = Color.Black,
    // ... other theme properties
)
```

If not set, the SDK uses a default theme that adapts to light/dark mode.

---

## Analytics Integration

Capture Appero events in your analytics:

```kotlin
class MyAnalyticsListener : ApperoAnalyticsListener {
    override fun onApperoFeedbackSubmitted(rating: Int, feedback: String) {
        // Log to Firebase, Mixpanel, etc.
    }
    override fun onRatingSelected(rating: Int) {
        // Log rating selection
    }
}

Appero.setAnalyticsListener(MyAnalyticsListener())
```

---

## User Session Management

- **Auto-generated user ID:** If you don’t provide a `clientId`, the SDK generates and persists a UUID.
- **Set a custom user ID:**  
  ```kotlin
  Appero.setUser("your_unique_user_id")
  ```
- **Reset user (e.g., on logout):**  
  ```kotlin
  Appero.resetUser()
  ```

---

## Offline Support

- Feedback submissions are queued if offline and sent automatically when connectivity returns.
- You can inspect and manage the queue:

```kotlin
val count = Appero.getQueuedFeedbackCount()
Appero.processQueuedFeedback()  // Triggers processing immediately
Appero.clearQueuedFeedback()    // Clears the queue (testing only)
```

- Network changes are detected automatically, and the queue is processed when the network is available.

---

## Play Store Review Prompt

- After feedback submission, if the rating is above a configurable threshold, the SDK can trigger the Play Store review dialog using the Play Core API.
- This is customizable via `FeedbackPromptUI` parameters `reviewPromptThreshold` and `onRequestReview`.

```kotlin
Appero.FeedbackPromptUI(
    config = ..., 
    reviewPromptThreshold = 4,
    onRequestReview = { Appero.requestPlayStoreReview(activity) }
)
```

---

## API Reference

- `Appero.start(context, apiKey, clientId)`
- `Appero.log(Experience)`
- `Appero.log(points: Int)`
- `Appero.ratingThreshold`
- `Appero.shouldShowAppero()`
- `Appero.showFeedbackPrompt(config)`
- `Appero.showFeedbackPrompt(config, initialStep: FeedbackStep)`
- `Appero.FeedbackPromptUI(...)`
- `Appero.setAnalyticsListener(listener)`
- `Appero.setUser(userId)`
- `Appero.resetUser()`
- `Appero.getCurrentUserId()`
- `Appero.getExperienceState()`
- `Appero.getQueuedFeedbackCount()`
- `Appero.processQueuedFeedback()`
- `Appero.clearQueuedFeedback()`
- `Appero.theme`
- `Appero.requestPlayStoreReview(activity)`

---

## Example

```kotlin
// In your MainActivity
override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    Appero.start(this, "your_api_key", null)
    Appero.setAnalyticsListener(MyAnalyticsListener())
    setContent {
        Appero.FeedbackPromptUI(
            config = FeedbackPromptConfig(
                title = "We value your feedback!",
                subtitle = "How was your experience?",
                followUpQuestion = "Tell us more...",
                placeholder = "Type your feedback here",
                submitText = "Send"
            )
        )
    }
}
```

---

## License

MIT 