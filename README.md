# Appero SDK for Android

The intelligent in-app feedback widget that drives organic growth through smart user experience tracking and automated Play Store review prompts.

## 📋 Requirements

### Android API Levels
- **Minimum Android API:** 24 (Android 7.0)
- **Target/Compile API:** 34+
- **UI Framework:** Jetpack Compose + XML Views (full support for both)
- **Networking:** Retrofit + OkHttp (bundled)
- **Storage:** SharedPreferences (automatic)

---

## 🚀 Installation & Setup

### 1. Add GitHub Packages Repository

Add the GitHub Packages repository to your `settings.gradle.kts`:

```kotlin
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()  // Required for other dependencies
        maven {
            url = uri("https://maven.pkg.github.com/pocketworks/appero-sdk-android")
            credentials {
                username = System.getenv("GITHUB_USERNAME")
                password = System.getenv("GITHUB_TOKEN")
            }
        }
    }
}
```

**Note:** GitHub Packages requires authentication even for public packages. You'll need to set up GitHub credentials as described in step 2.

### 2. Set Up GitHub Token

GitHub Packages requires authentication even for public packages. Create a GitHub Personal Access Token:

#### **Option A: Classic Personal Access Token (Recommended)**

1. Go to **GitHub.com** → Click your profile picture → **Settings**
2. Scroll down to **Developer settings** (bottom left sidebar)
3. Click **Personal access tokens** → **Tokens (classic)**
4. Click **"Generate new token"** → **"Generate new token (classic)"**
5. Give it a name like "Appero SDK Access"
6. Set expiration (90 days recommended)
7. Select scope: ✅ `read:packages`
8. Click **"Generate token"**
9. **Copy the token immediately** (you won't see it again!)

#### **Option B: Fine-grained Personal Access Token**

1. Go to **GitHub.com** → Click your profile picture → **Settings**
2. Scroll down to **Developer settings** → **Personal access tokens** → **Fine-grained tokens**
3. Click **"Generate new token"**
4. Give it a name like "Appero SDK Access"
5. Set expiration and repository access
6. Under **Repository permissions**:
   - **Contents**: Read
   - **Metadata**: Read
   - **Packages**: Read (if available)
7. Click **"Generate token"**

#### **Set Environment Variables**

Set the credentials as environment variables:

```bash
export GITHUB_USERNAME=your_github_username
export GITHUB_TOKEN=your_github_personal_access_token
```

**For CI/CD or permanent setup**, add to your shell profile (`~/.zshrc`, `~/.bashrc`, etc.):
```bash
echo 'export GITHUB_USERNAME=your_github_username' >> ~/.zshrc
echo 'export GITHUB_TOKEN=your_github_personal_access_token' >> ~/.zshrc
source ~/.zshrc
```

**For Android Studio**, you can set environment variables in the IDE settings or run configurations.

### 3. Add Dependencies

Add these to your `app/build.gradle.kts`:

```kotlin
dependencies {
    // Appero SDK from GitHub Packages
    implementation("com.pocketworks:appero-sdk-android:1.0.0")
    
    // REQUIRED: Google Play In-App Review (for Play Store review feature)
    implementation("com.google.android.play:review:2.0.1")
    
    // REQUIRED: Jetpack Compose (if using Compose UI)
    implementation(platform("androidx.compose:compose-bom:2024.04.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.activity:activity-compose:1.8.0")
    
    // REQUIRED: Fragment support (if using XML layouts)
    implementation("androidx.fragment:fragment-ktx:1.6.2")
    implementation("com.google.android.material:material:1.10.0")
    
    // REQUIRED: Coroutines (if not already using)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
}
```

### 4. Add Permissions & Activity Configuration

Add to your `AndroidManifest.xml`:

```xml
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    
    <!-- REQUIRED: For API calls to submit feedback -->
    <uses-permission android:name="android.permission.INTERNET" />
    
    <!-- REQUIRED: For network connectivity detection and offline queue -->
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
    
</manifest>
```

**Important**: For proper keyboard handling with the feedback bottom sheet, add this to your activity:

```xml
<activity
    android:name=".YourActivity"
    android:windowSoftInputMode="adjustResize">
    <!-- ... other activity attributes ... -->
</activity>
```

This ensures the text input remains visible when the keyboard appears.

### 5. Initialize the SDK

In your `Application` class or `MainActivity.onCreate()`:

```kotlin
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // REQUIRED: Initialize Appero SDK
        Appero.start(
            context = this,
            apiKey = "your_api_key_here",        // Get from Appero dashboard
            clientId = null,                     // null = auto-generate user ID
            debugMode = ApperoDebugMode.DEBUG    // Use PRODUCTION for release
        )
        
        // OPTIONAL: Set up analytics listener
        Appero.setAnalyticsListener(YourAnalyticsListener())
        
        setContent {
            YourAppTheme {
                // Your app content with Appero integration
                Appero.FeedbackPromptUI(
                    config = FeedbackPromptConfig(
                        title = "How was your experience?",
                        subtitle = "We'd love to hear your thoughts"
                    ),
                    onRequestReview = {
                        // Automatic Play Store review handling
                        Appero.requestPlayStoreReview(this@MainActivity)
                    }
                )
            }
        }
    }
}
```

---

## 🎯 Core Features

### Experience Tracking

Track user experiences to build an intelligent feedback system:

```kotlin
// Semantic tracking (recommended)
Appero.log(Experience.VERY_POSITIVE)  // +2 points
Appero.log(Experience.POSITIVE)       // +1 point
Appero.log(Experience.NEUTRAL)        // 0 points
Appero.log(Experience.NEGATIVE)       // -1 point
Appero.log(Experience.VERY_NEGATIVE)  // -2 points

// Custom point tracking
Appero.log(3)   // Add 3 points
Appero.log(-2)  // Subtract 2 points

// Check if user should see feedback prompt
if (Appero.shouldShowAppero()) {
    // User has positive experience, show feedback prompt
}
```

### Smart Feedback Collection

The SDK provides **three UI approaches** to fit your project's needs:

#### 🎛️ **Jetpack Compose UI (Recommended)**
Modern bottom sheet modal for Compose projects:

```kotlin
// Configure thresholds
Appero.ratingThreshold = 5  // Show feedback when experience >= 5 points

// Manual feedback prompt (Compose)
Appero.showFeedbackPrompt(
    config = FeedbackPromptConfig(
        title = "We value your feedback!",
        subtitle = "How was your experience?",
        followUpQuestion = "Tell us more...",
        placeholder = "Type your feedback here",
        submitText = "Send",
        maxCharacters = 120
    )
) { success, message ->
    // Handle submission result
}
```

#### 🏛️ **XML Bottom Sheet (Legacy Support)**
Modal bottom sheet using traditional Android Views for XML-based projects:

```kotlin
class YourActivity : FragmentActivity() {
    
    private fun showFeedback() {
        // XML bottom sheet modal (same UX as Compose version)
        Appero.showFeedbackDialog(
            activity = this,
            config = FeedbackPromptConfig(
                title = "How was your experience?",
                subtitle = "We'd love to hear your thoughts",
                followUpQuestion = "Tell us more...",
                placeholder = "Share your feedback...",
                submitText = "Send Feedback",
                maxCharacters = 120
            )
        ) { success, message ->
            if (success) {
                Toast.makeText(this, "Feedback submitted!", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
```

#### 🔄 **ComposeView Hybrid (Migration Path)**
Embed Compose UI within XML layouts for gradual migration:

```xml
<!-- activity_main.xml -->
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:orientation="vertical">
    
    <!-- Your existing XML views -->
    <TextView
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:text="Your XML-based UI" />
    
    <!-- Embed Appero Compose UI -->
    <androidx.compose.ui.platform.ComposeView
        android:id="@+id/appero_compose_view"
        android:layout_width="match_parent"
        android:layout_height="wrap_content" />
        
</LinearLayout>
```

```kotlin
class YourActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        // Set up ComposeView with Appero UI
        findViewById<ComposeView>(R.id.appero_compose_view).setContent {
            YourAppTheme {
                Appero.FeedbackPromptUI(
                    config = FeedbackPromptConfig(
                        title = "How was your experience?",
                        subtitle = "Modern Compose UI in XML layout"
                    ),
                    onRequestReview = {
                        Appero.requestPlayStoreReview(this@YourActivity)
                    }
                )
            }
        }
    }
}
```

---

## 🏛️ XML Layout Integration

### For Existing XML-Based Projects

The Appero SDK provides **full support for traditional XML layouts** with the same modern UX as Compose:

#### **XML Bottom Sheet Dialog**

```kotlin
class MainActivity : FragmentActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        // Initialize Appero SDK
        Appero.start(this, "your_api_key", null)
        
        // Show feedback when needed
        findViewById<Button>(R.id.feedback_button).setOnClickListener {
            showApperoFeedback()
        }
    }
    
    private fun showApperoFeedback() {
        Appero.showFeedbackDialog(
            activity = this,
            config = FeedbackPromptConfig(
                title = "How was your experience?",
                subtitle = "We'd love to hear your thoughts",
                followUpQuestion = "What can we improve?",
                placeholder = "Tell us more...",
                submitText = "Send Feedback",
                maxCharacters = 200
            )
        ) { success, message ->
            if (success) {
                Toast.makeText(this, "Thank you for your feedback!", Toast.LENGTH_LONG).show()
                // Automatically handles Play Store review if rating >= threshold
            } else {
                Toast.makeText(this, "Failed to submit: $message", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    // Track experiences in your XML-based app
    private fun onUserCompletedTask() {
        Appero.log(Experience.POSITIVE)
        
        // Check if user should see feedback prompt
        if (Appero.shouldShowAppero()) {
            showApperoFeedback()
        }
    }
}
```

#### **Advanced XML Integration**

```kotlin
class YourFragment : Fragment() {
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Create DialogFragment directly for more control
        val dialogFragment = Appero.createFeedbackDialogFragment(
            config = FeedbackPromptConfig(
                title = "Fragment Feedback",
                subtitle = "How's this feature working for you?"
            )
        ) { success, message ->
            // Handle result
        }
        
        // Show when needed
        view.findViewById<Button>(R.id.show_feedback).setOnClickListener {
            dialogFragment.show(parentFragmentManager, "appero_feedback")
        }
    }
}
```

### **Migration Strategies**

#### **Strategy 1: Gradual Migration with ComposeView**
Perfect for teams wanting to adopt Compose incrementally:

```xml
<!-- Keep your existing XML layout -->
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent">
    
    <!-- Your existing XML views -->
    <include layout="@layout/your_existing_layout" />
    
    <!-- Add Compose components gradually -->
    <androidx.compose.ui.platform.ComposeView
        android:id="@+id/modern_components"
        android:layout_width="match_parent"
        android:layout_height="wrap_content" />
        
</LinearLayout>
```

#### **Strategy 2: XML-First Approach**
Use XML bottom sheets throughout your app:

```kotlin
class BaseActivity : FragmentActivity() {
    
    protected fun showStandardFeedback() {
        Appero.showFeedbackDialog(
            activity = this,
            config = getStandardFeedbackConfig()
        ) { success, message ->
            handleFeedbackResult(success, message)
        }
    }
    
    private fun getStandardFeedbackConfig() = FeedbackPromptConfig(
        title = getString(R.string.feedback_title),
        subtitle = getString(R.string.feedback_subtitle),
        followUpQuestion = getString(R.string.feedback_follow_up),
        placeholder = getString(R.string.feedback_placeholder),
        submitText = getString(R.string.feedback_submit)
    )
}
```

### **XML Layout Best Practices**

**✅ Recommended:**
- Use `FragmentActivity` or `AppCompatActivity`
- Initialize SDK in `onCreate()` before setting content view
- Use `showFeedbackDialog()` for consistent bottom sheet UX
- Handle results in callbacks for user feedback

**⚠️ Important Notes:**
- XML bottom sheets provide the **same UX** as Compose version
- All analytics callbacks work identically
- Play Store review integration is automatic
- Offline queuing works the same way

---

## 🏪 Play Store Review Integration

### Automatic Review Flow

The SDK automatically handles Play Store reviews after successful feedback submission:

```kotlin
// Configure Play Store review threshold
Appero.playStoreReviewThreshold = 4  // Default: 4 stars

// The flow works automatically:
// 1. User submits feedback with 4+ star rating
// 2. SDK automatically triggers Play Store review
// 3. Falls back to external Play Store if in-app review fails
```

### Manual Review Requests

```kotlin
// Basic Play Store review request
Appero.requestPlayStoreReview(activity) { result ->
    when (result) {
        is Appero.PlayStoreReviewResult.InAppReviewShown -> {
            // Google Play in-app review dialog was shown
        }
        is Appero.PlayStoreReviewResult.InAppReviewCompleted -> {
            // In-app review flow completed
        }
        is Appero.PlayStoreReviewResult.FallbackTriggered -> {
            // User was redirected to Play Store app/website
        }
        is Appero.PlayStoreReviewResult.Failed -> {
            // Review request failed: ${result.reason}
        }
    }
}

// Rating-based review request
Appero.requestPlayStoreReviewIfRating(
    activity = this,
    rating = userRating,     // 1-5 stars
    threshold = 4,           // Only show if rating >= 4
    fallbackToExternalStore = true  // Redirect to Play Store if in-app fails
)

// Check if in-app review is available
val isAvailable = Appero.isInAppReviewAvailable(this)
```

### Compose Integration with Review Flow

```kotlin
@Composable
fun YourScreen() {
    Appero.FeedbackPromptUI(
        config = FeedbackPromptConfig(
            title = "How was your experience?",
            subtitle = "We'd love to hear your thoughts",
            followUpQuestion = "What made it great?",
            placeholder = "Tell us more...",
            submitText = "Send Feedback"
        ),
        flowConfig = FeedbackFlowConfig(
            rateUsTitle = "Enjoying our app?",
            rateUsSubtitle = "Please rate us on the Play Store!",
            rateUsCtaText = "Rate Now",
            thankYouMessage = "Thank you for your feedback!"
        ),
        reviewPromptThreshold = 4,  // Show Play Store review for 4+ stars
        onRequestReview = {
            // This handles the complete Play Store review flow
            Appero.requestPlayStoreReview(context as Activity)
        }
    )
}
```

### Play Store Review Best Practices

**✅ What the SDK Handles Automatically:**
- Package name detection for correct app redirect
- Google Play In-App Review API integration
- Fallback to external Play Store when in-app review fails
- Analytics tracking for review events
- Error handling and graceful degradation

**🎯 Review Flow:**
1. User submits feedback with rating
2. If rating ≥ threshold (default: 4), trigger review
3. Try Google Play In-App Review API first
4. If fails, fallback to Play Store app (`market://` URL)
5. If Play Store app unavailable, open in web browser
6. Analytics callbacks throughout the process

---

## 📊 Analytics Integration

Track all Appero events in your analytics platform:

```kotlin
class YourAnalyticsListener : ApperoAnalyticsListener {
    override fun onApperoFeedbackSubmitted(rating: Int, feedback: String) {
        // Track feedback submission
        FirebaseAnalytics.getInstance().logEvent("appero_feedback_submitted") {
            param("rating", rating.toLong())
            param("feedback_length", feedback.length.toLong())
        }
    }
    
    override fun onRatingSelected(rating: Int) {
        // Track rating selection (before submission)
        FirebaseAnalytics.getInstance().logEvent("appero_rating_selected") {
            param("rating", rating.toLong())
        }
    }
    
    override fun onPlayStoreReviewRequested() {
        // Track when Play Store review is requested
        FirebaseAnalytics.getInstance().logEvent("appero_play_store_review_requested")
    }
    
    override fun onPlayStoreReviewCompleted(successful: Boolean) {
        // Track Play Store review completion
        FirebaseAnalytics.getInstance().logEvent("appero_play_store_review_completed") {
            param("successful", successful)
        }
    }
    
    override fun onPlayStoreFallbackTriggered() {
        // Track fallback to external Play Store
        FirebaseAnalytics.getInstance().logEvent("appero_play_store_fallback_triggered")
    }
}

// Set the analytics listener
Appero.setAnalyticsListener(YourAnalyticsListener())
```

---

## 👤 User Session Management

Handle user sessions for account-based apps:

```kotlin
// Auto-generated user ID (default behavior)
// SDK automatically creates and persists a UUID

// Set custom user ID (when user logs in)
Appero.setUser("user_123")

// Get current user ID
val userId = Appero.getCurrentUserId()

// Reset user session (when user logs out)
Appero.resetUser()

// Each user has separate:
// - Experience tracking
// - Feedback submission status
// - Offline queues
```

---

## 📶 Offline Support

Robust offline functionality with automatic sync:

### Feedback Queue
```kotlin
// Check queued feedback count
val feedbackCount = Appero.getQueuedFeedbackCount()

// Manually process queue (automatic when network returns)
Appero.processQueuedFeedback()

// Clear queue (testing only)
Appero.clearQueuedFeedback()
```

### Experience Queue
```kotlin
// Check queued experiences count
val experienceCount = Appero.getQueuedExperiencesCount()

// Manually process queue
Appero.processQueuedExperiences()

// Clear queue (testing only)
Appero.clearQueuedExperiences()
```

**Features:**
- ✅ Automatic network detection
- ✅ Queue processing when connectivity returns
- ✅ Retry mechanism with exponential backoff
- ✅ Duplicate request prevention
- ✅ Persistent storage across app restarts

---

## 🎨 Theming & Customization

### Theme Customization
```kotlin
// Custom theme
Appero.theme = CustomTheme(
    primaryColor = Color(0xFF6200EE),
    accentColor = Color(0xFF03DAC6),
    backgroundColor = Color.White,
    textColor = Color.Black,
    cornerRadius = 12.dp
)

// Default theme (adapts to system light/dark mode)
Appero.theme = DefaultTheme()

// Light theme (fixed)
Appero.theme = LightTheme()

// Dark theme (fixed)
Appero.theme = DarkTheme()
```

### Configuration Options
```kotlin
// Experience tracking threshold
Appero.ratingThreshold = 5

// Play Store review threshold
Appero.playStoreReviewThreshold = 4

// Reset experience and feedback status
Appero.resetExperienceAndPrompt()
```

---

## 🐛 Debug Mode & Logging

Comprehensive debugging system for development:

```kotlin
// Enable debug mode during development
Appero.start(
    context = this,
    apiKey = "your_api_key",
    clientId = null,
    debugMode = ApperoDebugMode.DEBUG  // or PRODUCTION
)
```

### Debug Mode Features
- **API Request/Response Logging** - Complete HTTP request details
- **Network Error Tracking** - Connectivity issues and retries
- **SDK Operation Logging** - Initialization, user management, queue processing
- **Play Store Review Events** - Review request attempts and results

### Log Filtering
```bash
# View only Appero SDK logs
adb logcat | grep "ApperoSDK"

# Example logs
D/ApperoSDK: SDK Initialization: Completed successfully
D/ApperoSDK: --> POST https://app.appero.co.uk/api/feedback
D/ApperoSDK: API Success: POST /api/feedback (Status: 200)
D/ApperoSDK: Play Store Review: In-app review completed successfully
E/ApperoSDK: Network Error (Play Store Review): Failed to request review flow
```

---

## 📚 API Reference

### Core Methods
```kotlin
// SDK Initialization
Appero.start(context, apiKey, clientId)
Appero.start(context, apiKey, clientId, debugMode)
Appero.isInitialized(): Boolean

// Experience Tracking
Appero.log(Experience)
Appero.log(points: Int)
Appero.shouldShowAppero(): Boolean
Appero.getExperienceState(): ExperienceState?
Appero.ratingThreshold: Int

// Feedback UI (Compose)
Appero.showFeedbackPrompt(config)
Appero.showFeedbackPrompt(config, initialStep)
Appero.FeedbackPromptUI(...)

// Feedback UI (XML Layouts)
Appero.showFeedbackDialog(activity, config)
Appero.showFeedbackDialog(activity, config, initialStep)
Appero.createFeedbackDialogFragment(config)

// User Management
Appero.setUser(userId: String)
Appero.resetUser()
Appero.getCurrentUserId(): String?

// Play Store Review
Appero.requestPlayStoreReview(activity, fallbackToExternalStore, onComplete)
Appero.requestPlayStoreReviewIfRating(activity, rating, threshold, fallbackToExternalStore, onComplete)
Appero.isInAppReviewAvailable(activity): Boolean
Appero.playStoreReviewThreshold: Int

// Offline Queues
Appero.getQueuedFeedbackCount(): Int
Appero.processQueuedFeedback()
Appero.clearQueuedFeedback()
Appero.getQueuedExperiencesCount(): Int
Appero.processQueuedExperiences()
Appero.clearQueuedExperiences()

// Analytics & Configuration
Appero.setAnalyticsListener(listener)
Appero.theme: ApperoTheme
Appero.resetExperienceAndPrompt()
```

---

## ✅ Setup Checklist

Before integrating Appero SDK, ensure:

- [ ] **Minimum API 24** in `build.gradle.kts`
- [ ] **Google Play Review dependency** added
- [ ] **UI Framework dependencies** (Compose OR Fragment + Material Design)
- [ ] **INTERNET permission** in `AndroidManifest.xml`
- [ ] **ACCESS_NETWORK_STATE permission** in `AndroidManifest.xml`
- [ ] **Appero.start()** called in `onCreate()`
- [ ] **API key** obtained from Appero dashboard
- [ ] **Analytics listener** configured (optional)
- [ ] **Activity extends FragmentActivity** (if using XML layouts)

---

## ⚠️ Common Pitfalls

### GitHub Packages Issues

**Problem:** `401 Unauthorized` or `403 Forbidden` when downloading the SDK

**Solutions:**
1. **Check token permissions** - Ensure your token has `read:packages` scope
2. **Verify environment variables** - Make sure `GITHUB_USERNAME` and `GITHUB_TOKEN` are set
3. **Token expiration** - Classic tokens expire; regenerate if needed
4. **Organization access** - If using organization token, ensure it has repository access
5. **Fine-grained token approval** - Organization tokens may need admin approval

**Test your setup:**
```bash
# Verify environment variables
echo "Username: $GITHUB_USERNAME"
echo "Token: ${GITHUB_TOKEN:0:10}..."

# Test API access
curl -H "Authorization: token $GITHUB_TOKEN" \
  "https://api.github.com/repos/pocketworks/appero-sdk-android"
```

### ❌ Don't Do This
```kotlin
// Multiple initialization
Appero.start(this, apiKey, clientId)
Appero.start(this, apiKey, clientId) // Don't repeat

// Using non-Activity context for Play Store review
fun someFunction(context: Context) {
    Appero.requestPlayStoreReview(context) // Will fail
}

// Missing required permissions
// Will cause network failures and offline queue issues
```

### ✅ Do This Instead
```kotlin
// Initialize once in Application or MainActivity
class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        Appero.start(this, apiKey, clientId)
    }
}

// Use Activity context for Play Store reviews
fun requestReview(activity: Activity) {
    Appero.requestPlayStoreReview(activity)
}

// Include required permissions in AndroidManifest.xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
```

---

## 🎯 Complete Integration Example

```kotlin
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Initialize Appero SDK
        Appero.start(
            context = this,
            apiKey = "your_api_key_here",
            clientId = null,
            debugMode = ApperoDebugMode.DEBUG
        )
        
        // Configure thresholds
        Appero.ratingThreshold = 5              // Experience threshold
        Appero.playStoreReviewThreshold = 4     // Review threshold
        
        // Set up analytics
        Appero.setAnalyticsListener(object : ApperoAnalyticsListener {
            override fun onApperoFeedbackSubmitted(rating: Int, feedback: String) {
                FirebaseAnalytics.getInstance().logEvent("appero_feedback_submitted") {
                    param("rating", rating.toLong())
                }
            }
            
            override fun onRatingSelected(rating: Int) {
                FirebaseAnalytics.getInstance().logEvent("appero_rating_selected") {
                    param("rating", rating.toLong())
                }
            }
            
            override fun onPlayStoreReviewRequested() {
                FirebaseAnalytics.getInstance().logEvent("appero_play_store_review_requested")
            }
            
            override fun onPlayStoreReviewCompleted(successful: Boolean) {
                FirebaseAnalytics.getInstance().logEvent("appero_play_store_review_completed") {
                    param("successful", successful)
                }
            }
            
            override fun onPlayStoreFallbackTriggered() {
                FirebaseAnalytics.getInstance().logEvent("appero_play_store_fallback_triggered")
            }
        })
        
        setContent {
            YourAppTheme {
                // Your app content
                YourMainScreen()
                
                // Appero feedback UI (handles visibility automatically)
                Appero.FeedbackPromptUI(
                    config = FeedbackPromptConfig(
                        title = "How was your experience?",
                        subtitle = "We'd love to hear your thoughts",
                        followUpQuestion = "What made it great?",
                        placeholder = "Share your feedback...",
                        submitText = "Send Feedback"
                    ),
                    flowConfig = FeedbackFlowConfig(
                        rateUsTitle = "Enjoying our app?",
                        rateUsSubtitle = "Please rate us on the Play Store!",
                        rateUsCtaText = "Rate Now",
                        thankYouMessage = "Thank you for your feedback!"
                    ),
                    reviewPromptThreshold = 4,
                    onRequestReview = {
                        Appero.requestPlayStoreReview(this@MainActivity)
                    }
                ) { success, message ->
                    // Handle feedback submission result
                    if (success) {
                        // Feedback submitted successfully
                        // Play Store review may be triggered automatically
                    }
                }
            }
        }
    }
    
    // Track user experiences throughout your app
    private fun onPositiveUserAction() {
        Appero.log(Experience.POSITIVE)
    }
    
    private fun onNegativeUserAction() {
        Appero.log(Experience.NEGATIVE)
    }
}
```

---

## 🚀 Flutter Integration

For Flutter projects, use the official Appero Flutter plugin instead of integrating this Android SDK directly:

- **Flutter Plugin**: [appero-sdk-flutter](https://github.com/pocketworks/appero-sdk-flutter)
- **Installation**: Add `appero_flutter` to your `pubspec.yaml`
- **Features**: Auto-trigger callbacks, native UI integration, unified iOS/Android API

The Flutter plugin wraps this Android SDK and provides additional features like auto-trigger callbacks for seamless Flutter integration.

---

## 📄 License

MIT 