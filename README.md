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
// Basic initialization
Appero.start(
    context = this,
    apiKey = "your_api_key",
    clientId = null // Pass null or blank to auto-generate a unique user ID
)

// With debug mode (recommended for development)
Appero.start(
    context = this,
    apiKey = "your_api_key",
    clientId = null,
    debugMode = ApperoDebugMode.DEBUG // or ApperoDebugMode.PRODUCTION
)
```

- If you pass `null` or blank for `clientId`, the SDK will auto-generate and persist a UUID for the user (iOS parity).

---

## Debug Mode & Logging

The SDK provides a comprehensive debug mode system to help with development and troubleshooting:

### Debug Modes

```kotlin
enum class ApperoDebugMode {
    PRODUCTION,  // No debug logs (default)
    DEBUG        // Detailed logging for development
}
```

### What Gets Logged in DEBUG Mode

When `ApperoDebugMode.DEBUG` is enabled, the SDK logs:

- **API Request Errors** - Failed feedback/experience submissions
- **Critical SDK Operations** - Initialization and shutdown
- **Network Issues** - Connectivity problems and retry failures
- **HTTP Requests** - Detailed API call logs (request/response)

All logs use the **"ApperoSDK"** tag for easy filtering:

```
D/ApperoSDK: SDK Initialization: Starting with debug mode: DEBUG
D/ApperoSDK: --> POST https://app.appero.co.uk/api/v1/experiences
D/ApperoSDK: API Success: POST /api/experiences (Status: 200)
E/ApperoSDK: API Error: POST /api/feedback (Status: 500) - Server error
```

### Filtering Logs

To see only Appero SDK logs in Android Studio or logcat:

```bash
adb logcat | grep "ApperoSDK"
```

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
- **Offline Support:** Experience logs are queued if offline and sent automatically when connectivity returns.
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

- `shouldShowAppero()` is a local check: true if score >= threshold AND user hasn't submitted feedback yet.
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

- **Auto-generated user ID:** If you don't provide a `clientId`, the SDK generates and persists a UUID.
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

Both feedback and experience submissions are queued if offline and sent automatically when connectivity returns.

### Feedback Queue

```kotlin
val count = Appero.getQueuedFeedbackCount()
Appero.processQueuedFeedback()  // Triggers processing immediately
Appero.clearQueuedFeedback()    // Clears the queue (testing only)
```

### Experience Queue

```kotlin
val count = Appero.getQueuedExperiencesCount()
Appero.processQueuedExperiences()  // Triggers processing immediately
Appero.clearQueuedExperiences()    // Clears the queue (testing only)
```

- Network changes are detected automatically, and queues are processed when the network is available.
- **Concurrent Processing Protection:** The SDK prevents duplicate requests when connectivity returns.

---

## Play Store Review Prompt

The SDK provides seamless integration with Google Play In-App Review API to prompt users for app store reviews after positive feedback. The SDK handles the entire review flow internally.

### Basic Review Request

```kotlin
// Request Play Store review (shows review dialog)
Appero.requestPlayStoreReview(activity)
```

### Conditional Review Based on Rating

```kotlin
// Only show review prompt if rating is 4 or higher (default threshold)
Appero.requestPlayStoreReviewIfRating(
    activity = this,
    rating = userRating, // 1-5 rating from feedback
    threshold = 4 // Optional: custom threshold (default: 4)
)
```

### Integration with Feedback Flow

```kotlin
Appero.FeedbackPromptUI(
    config = FeedbackPromptConfig(...),
    reviewPromptThreshold = 4, // Show review if rating >= 4
    onRequestReview = { activity ->
        Appero.requestPlayStoreReview(activity)
    }
)
```

### Analytics Integration

The SDK automatically notifies your analytics listener when review flows are requested:

```kotlin
class MyAnalyticsListener : ApperoAnalyticsListener {
    override fun onApperoFeedbackSubmitted(rating: Int, feedback: String) {
        // Log feedback submission
    }
    
    override fun onRatingSelected(rating: Int) {
        // Log rating selection
    }
    
    override fun onPlayStoreReviewRequested() {
        // Log when Play Store review is requested
        FirebaseAnalytics.getInstance().logEvent("appero_review_requested")
    }
}
```

### Key Benefits

- **✅ SDK Managed** - SDK handles the entire review flow internally
- **✅ Google Recommended** - Uses official Google Play In-App Review API
- **✅ Non-Intrusive** - Shows native Play Store review dialog
- **✅ Smart Filtering** - Only prompts users with positive ratings
- **✅ Configurable Threshold** - Customize minimum rating for review prompts
- **✅ Fallback Handling** - Gracefully handles API failures
- **✅ Analytics Ready** - Automatic analytics events for review requests

### How It Works

1. **User submits feedback** with a rating (1-5 stars)
2. **SDK checks rating** against configurable threshold (default: 4)
3. **If rating ≥ threshold** - Shows Google Play review dialog
4. **If rating < threshold** - Skips review prompt, shows thank you message
5. **Review completion** - Optional callback for analytics or UI updates

### Best Practices

- **Set appropriate thresholds** - Don't prompt users with negative experiences
- **Respect user choice** - The API doesn't tell you if they reviewed, so don't ask again
- **Test thoroughly** - Review dialog behavior varies by device and Play Store version
- **Handle gracefully** - Always provide fallback behavior for API failures

---

## API Reference

### Core Methods
- `Appero.start(context, apiKey, clientId)`
- `Appero.start(context, apiKey, clientId, debugMode)`

### Experience Tracking
- `Appero.log(Experience)`
- `Appero.log(points: Int)`
- `Appero.ratingThreshold`
- `Appero.shouldShowAppero()`
- `Appero.getExperienceState()`

### Feedback UI
- `Appero.showFeedbackPrompt(config)`
- `Appero.showFeedbackPrompt(config, initialStep: FeedbackStep)`
- `Appero.FeedbackPromptUI(...)`

### User Management
- `Appero.setUser(userId)`
- `Appero.resetUser()`
- `Appero.getCurrentUserId()`

### Offline Queues
- `Appero.getQueuedFeedbackCount()`
- `Appero.processQueuedFeedback()`
- `Appero.clearQueuedFeedback()`
- `Appero.getQueuedExperiencesCount()`
- `Appero.processQueuedExperiences()`
- `Appero.clearQueuedExperiences()`

### Analytics & Theming
- `Appero.setAnalyticsListener(listener)`
- `Appero.theme`
- `Appero.requestPlayStoreReview(activity)`
- `Appero.requestPlayStoreReviewIfRating(activity, rating, threshold)`

---

## Example

```kotlin
// In your MainActivity
override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    
    // Initialize with debug mode for development
    Appero.start(
        context = this, 
        apiKey = "your_api_key", 
        clientId = null,
        debugMode = ApperoDebugMode.DEBUG
    )
    
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