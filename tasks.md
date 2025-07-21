## Completed Tasks
- [x] Set up the basic structure for the Appero Android library module.
- [x] Implement a simple 'Hello World' function in the Appero library.
- [x] Publish the Appero library locally (Maven Local or as a module) for testing.
- [x] Create an external sample Android app project for integration testing.
- [x] Integrate the Appero library into the sample app (as a dependency or module).
- [x] Invoke the 'Hello World' function from the Appero library in the sample app and verify output.

## Core SDK Features (Based on iOS Version)

### 1. SDK Initialization & Configuration
- [x] **Implement SDK initialization with API key and client ID**
  - Create `Appero.instance.start(apiKey: String, clientId: String)` method
  - Set up singleton pattern for global access (similar to iOS shared instance model)
  - Initialize core SDK components and networking
  - Store credentials securely for API calls to https://app.appero.co.uk/api/feedback
  - **Android-specific**: Initialize in Application.onCreate() or MainActivity.onCreate()

### 2. User Experience Tracking System
- [x] **Implement experience points tracking with Likert scale**
  - Create `Experience` enum: `VERY_POSITIVE(+2), POSITIVE(+1), NEUTRAL(0), NEGATIVE(-1), VERY_NEGATIVE(-2)`
  - Create `log(experience: Experience)` method with predefined scoring (same as iOS)
  - Create `log(points: Int)` method for custom scoring (developer-defined scale)
  - Maintain running total of user experience points
  - Store experience data persistently (SharedPreferences)
  - **Key insight**: Track positive/negative interactions to build experience score

- [x] **Implement smart prompting threshold system**
  - Add `ratingThreshold` property (configurable, default: 5 points based on iOS pattern)
  - Add `shouldShowAppero` property to check if prompt should be displayed
  - Logic: only prompt when experience score crosses threshold AND user hasn't already given feedback
  - **Android-specific**: Consider Activity lifecycle for prompt timing

### 3. User Session Management
- [ ] **Implement user ID management with persistence**
  - Auto-generate unique user ID on first use (UUID, stored in SharedPreferences)
  - Add `setUser(userId: String)` method for account-based systems
  - Add `resetUser()` method to clear user data on logout
  - Track feedback submission status per user ID
  - **Important**: Experience score and feedback status are tied to user ID

### 4. Feedback Collection API
- [ ] **Implement feedback submission with networking (Retrofit)**
  - Create `postFeedback(rating: Int, feedback: String)` suspend function
  - API endpoint: POST https://app.appero.co.uk/api/feedback
  - Form data: api_key, client_id, rating, feedback, sent_at (ISO 8601)
  - Use Retrofit with OkHttp for HTTP requests
  - Implement proper error handling and retry logic
  - Mark user as having submitted feedback to prevent re-prompting
  - **Flutter consideration**: Design API to be easily wrapped for Flutter plugin

### 5. UI Components (Android Native - Jetpack Compose Priority)
- [ ] **Create feedback UI using Jetpack Compose (Primary)**
  - Build star rating component (1-5 stars, Material Design 3)
  - Build feedback text input component (multiline EditText equivalent)
  - Create modal/bottom sheet presentation (equivalent to iOS .sheet)
  - Handle UI state management with Compose state
  - **Android-specific**: Use Material Design 3 components, support dark/light themes

- [ ] **Create legacy View system support (Secondary)**
  - Build XML-based feedback UI for non-Compose apps
  - Create DialogFragment-based implementation
  - Provide easy integration methods for existing apps
  - **Android-specific**: Support API level compatibility back to minSdk

### 6. Theming System
- [ ] **Implement customizable theming**
  - Create `ApperoTheme` data class/interface
  - Provide default system theme (follows system light/dark mode)
  - Provide fixed light and dark themes (like iOS version)
  - Allow custom color schemes for branding
  - Apply theme to all UI components
  - Add `Appero.instance.theme = customTheme` property
  - **Android-specific**: Integrate with Material Design 3 theming

### 7. Analytics Integration
- [ ] **Implement analytics delegate system**
  - Create `ApperoAnalyticsDelegate` interface
  - Add `logApperoFeedback(rating: Int, feedback: String)` callback
  - Add `logRatingSelected(rating: Int)` callback
  - Allow third-party analytics integration (Firebase, Mixpanel, etc.)
  - Set via `Appero.instance.analyticsDelegate = delegate`
  - **Same events as iOS**: Maintain consistency across platforms

### 8. Advanced Features
- [ ] **Implement experience reset functionality**
  - Add `resetExperienceAndPrompt()` method
  - Clear current experience points
  - Reset feedback submission status
  - **Caution**: Should be used carefully, recommend tracking last prompt date

- [ ] **Add configuration options**
  - Customizable prompt timing and cooldown periods
  - Minimum experience threshold settings
  - Debug logging options
  - **Android-specific**: Consider app lifecycle and background restrictions

### 9. UI Integration Patterns
- [ ] **Jetpack Compose Integration**
  - Provide `ApperoRatingView(productName: String)` composable
  - State management with `remember` and `mutableStateOf`
  - Integration with `LaunchedEffect` for triggering
  - **Example**: Easy integration with existing Compose apps

- [ ] **Traditional Android Integration**
  - Fragment-based implementation
  - Activity result contracts for feedback flow
  - Intent-based launching
  - **Android-specific**: Handle configuration changes and lifecycle

### 10. Testing & Documentation
- [ ] **Create comprehensive unit tests**
  - Test all core functionality
  - Mock network requests (MockWebServer)
  - Test user session management
  - Test experience tracking logic
  - **Android-specific**: Use JUnit, Mockito, and Android Test frameworks

- [ ] **Create sample implementations**
  - Jetpack Compose sample app (modern Android)
  - Traditional View system sample app (legacy support)
  - Integration examples for common scenarios
  - **Android-specific**: Show integration in different app architectures (MVVM, MVP, etc.)

- [ ] **Create documentation**
  - README with setup instructions
  - API documentation (KDoc)
  - Integration guide for different Android patterns
  - Theming guide with Material Design integration
  - **Flutter wrapper preparation**: Document APIs that will be exposed to Flutter

### 11. Flutter Wrapper Preparation
- [ ] **Design Flutter-friendly API surface**
  - Ensure all public methods can be easily exposed via platform channels
  - Use simple data types (String, Int, Boolean) for cross-platform compatibility
  - Design callback interfaces that can be bridged to Flutter
  - **Future task**: Create separate Flutter plugin that wraps this Android library

## What Appero Does (Context)
Appero is an intelligent in-app feedback widget that drives organic growth by:
1. **Tracking user experience** - Monitors positive/negative interactions in your app using a points system
2. **Smart prompting** - Only asks for feedback when users have had enough positive experiences (crosses threshold)
3. **Collecting feedback** - Provides a native UI for users to rate (1-5 stars) and provide written feedback
4. **Driving growth** - Converts happy users into app store reviews and referrals
5. **Preventing feedback fatigue** - Tracks who has already given feedback to avoid re-prompting

The key insight is that instead of randomly prompting users for feedback (which often gets negative responses), Appero waits until users have demonstrated they're having a good experience before asking for their opinion.

## Technical Requirements
- **Minimum Android API**: 24 (Android 7.0) - Jetpack Compose requirement
- **Target Android API**: 35 (Android 15) - Current Google Play requirement
- **Compile Android API**: 35 (Android 15) - Aligned with target
- **UI Framework**: Jetpack Compose (primary), Traditional Views (secondary)
- **Networking**: Retrofit + OkHttp for API calls
- **Storage**: SharedPreferences for user data and experience tracking
- **Architecture**: Singleton pattern with lifecycle-aware components
- **Distribution**: Maven Central (local for now, can migrate to private later)

## API Endpoint Details
- **URL**: https://app.appero.co.uk/api/feedback
- **Method**: POST
- **Content-Type**: multipart/form-data
- **Required Fields**:
  - api_key: String (UUID format)
  - client_id: String (UUID format)
  - rating: String (1-5)
  - feedback: String (user comment)
  - sent_at: String (ISO 8601 format, e.g., "2024-03-20T12:00:00Z")

## Implementation Progress

### ✅ Step 1 Complete: SDK Initialization & Configuration

**Files Created/Modified:**
- `app/src/main/java/com/example/appero_sdk_android/Appero.kt` - Main SDK singleton class
- `app/src/test/java/com/example/appero_sdk_android/ApperoTest.kt` - Unit tests for SDK initialization
- `appero-sample-android/app/src/main/java/com/appero/appero_sample_android/MainActivity.kt` - Updated with SDK integration
- `gradle/libs.versions.toml` - Added Mockito dependency
- `app/build.gradle.kts` - Added Mockito test dependency

**Core Implementation Details:**

1. **Singleton Pattern**: Implemented `Appero` object using Kotlin's `object` keyword for global access
2. **Secure Storage**: Credentials stored in SharedPreferences with dedicated keys:
   - `api_key`: Stores the API key securely
   - `client_id`: Stores the client ID securely  
   - `is_initialized`: Tracks initialization state
3. **Input Validation**: Added `require()` checks to prevent blank API keys or client IDs
4. **Context Management**: Stores application context for future SDK operations
5. **Internal API Access**: Provides internal methods for other SDK components to access stored credentials

**API Design:**
```kotlin
// Public API
Appero.start(context: Context, apiKey: String, clientId: String)
Appero.isInitialized(): Boolean

// Internal API (for other SDK components)
Appero.getApiKey(): String?
Appero.getClientId(): String?
Appero.getContext(): Context?
```

**Testing Coverage:**
- ✅ Successful initialization with valid credentials
- ✅ Exception handling for blank API key
- ✅ Exception handling for blank client ID
- ✅ SharedPreferences storage verification
- ✅ Mockito integration for isolated testing

**Sample App Integration:**
- SDK initialization in `MainActivity.onCreate()`
- Visual feedback showing initialization status
- Uses example credentials for demonstration

**Architecture Decisions:**
- Used `object` for singleton pattern (thread-safe by default in Kotlin)
- SharedPreferences for persistence (Android standard for key-value storage)
- Application context stored to prevent memory leaks
- Internal visibility for credential access methods
- Separate initialization state tracking for reliability

**Next Steps Prepared:**
- TODO comments in `initializeCoreComponents()` for future features
- Internal API ready for networking, user management, and experience tracking
- Foundation set for Flutter wrapper compatibility

### ✅ Step 2 Complete: User Experience Tracking System

**Files Created/Modified:**
- `app/src/main/java/com/example/appero_sdk_android/Experience.kt` - Experience enum with Likert scale values
- `app/src/main/java/com/example/appero_sdk_android/ExperienceTracker.kt` - Core experience tracking logic
- `app/src/test/java/com/example/appero_sdk_android/ExperienceTrackerTest.kt` - Comprehensive unit tests
- `app/src/main/java/com/example/appero_sdk_android/Appero.kt` - Updated with experience tracking API
- `appero-sample-android/app/src/main/java/com/appero/appero_sample_android/MainActivity.kt` - Demo implementation

**Core Implementation Details:**

1. **Experience Enum**: Five-point Likert scale with weighted scoring:
   - `VERY_POSITIVE(+2)` - Major successes, important completions
   - `POSITIVE(+1)` - Minor successes, smooth interactions  
   - `NEUTRAL(0)` - Standard interactions with no particular outcome
   - `NEGATIVE(-1)` - Minor issues, friction in user flow
   - `VERY_NEGATIVE(-2)` - Major issues, failed important actions

2. **ExperienceTracker Class**: Internal class managing experience state:
   - Persistent storage using SharedPreferences
   - Configurable rating threshold (default: 5 points)
   - Automatic prevention of negative experience points
   - Smart prompting logic: threshold met AND no previous feedback
   - State management for debugging and monitoring

3. **Public API Design**:
   ```kotlin
   // Experience logging
   Appero.log(Experience.VERY_POSITIVE)
   Appero.log(customPoints: Int)
   
   // Threshold management
   Appero.ratingThreshold = 10
   
   // Prompt checking
   val shouldShow = Appero.shouldShowAppero()
   
   // State inspection
   val state = Appero.getExperienceState()
   ```

4. **Key Features Implemented**:
   - **Negative value protection**: Experience points cannot go below 0
   - **Persistent storage**: All data survives app restarts
   - **Configurable thresholds**: Developers can adjust prompt timing
   - **Feedback prevention**: Users won't be re-prompted after submitting
   - **State inspection**: Debug-friendly state access for development

**Testing Coverage:**
- ✅ Experience enum logging (both positive and negative)
- ✅ Custom points logging with validation
- ✅ Negative value prevention logic
- ✅ Threshold-based prompt triggering
- ✅ Feedback submission state tracking
- ✅ Threshold configuration with validation
- ✅ State reset functionality
- ✅ Complete state inspection for debugging

**Sample App Integration:**
- Demonstrates automatic experience logging
- Shows real-time experience state updates
- Visual feedback when prompt threshold is reached
- Example of SDK integration in Activity lifecycle

**Architecture Decisions:**
- Internal `ExperienceTracker` class for separation of concerns
- SharedPreferences for Android-standard persistence
- Property-based API for easy configuration
- Comprehensive state object for debugging
- Validation at API boundaries to prevent misuse

**Next Steps Prepared:**
- Experience tracking foundation ready for user session management
- Internal API prepared for feedback submission integration
- State management ready for UI component integration
- Threshold system ready for smart prompt timing

### 🔄 **Step 3: Offline Support & Caching System** (Next Priority)

**Goal**: Implement comprehensive offline support for experience tracking and feedback submission, ensuring no data loss and seamless user experience regardless of network connectivity.

**Core Components to Implement:**

1. **Offline Experience Caching**
   - Cache all experience logs locally when offline
   - Implement queue system for pending experience submissions
   - Batch sync experiences when connectivity returns
   - Maintain experience point calculations offline
   - Preserve threshold crossing detection while offline

2. **Offline Feedback Submission Queue**
   - Implement local feedback storage for offline submissions
   - Queue feedback submissions with retry mechanism
   - Store complete feedback data (rating, text, metadata, timestamp)
   - Background sync when network becomes available
   - Handle submission failures gracefully with exponential backoff

3. **Smart Prompt State Caching**
   - Cache "ready to show feedback UI" decision locally
   - Store prompt eligibility flag independent of network state
   - Preserve developer's control over UI timing
   - Cache UI strings and localization data locally
   - Store flow type decisions (positive/neutral/negative paths)

4. **UI Content Caching**
   - Download and cache feedback UI strings from backend
   - Store multiple language support locally
   - Cache flow-specific content (positive vs negative feedback flows)
   - Implement content versioning and updates
   - Fallback to default strings if cache unavailable

5. **Network State Management**
   - Monitor network connectivity changes
   - Implement intelligent sync strategy
   - Handle partial sync scenarios
   - Provide network state callbacks to developers
   - Queue operations during connectivity gaps

6. **Data Persistence Architecture**
   - Implement robust local database (Room/SQLite)
   - Design schema for cached experiences, feedback, and UI data
   - Handle data migrations and versioning
   - Implement data cleanup policies
   - Ensure GDPR compliance for cached user data

**Technical Implementation Details:**

```kotlin
// Example API extensions for offline support
Appero.setOfflineModeEnabled(true)
Appero.getOfflineQueueStatus(): OfflineQueueStatus
Appero.forceSyncWhenOnline(): Boolean

// Network state callbacks
Appero.setNetworkStateListener { isOnline ->
    // Handle connectivity changes
}

// Offline-aware experience logging
Appero.log(Experience.POSITIVE) // Works offline, queues for sync

// Cached prompt availability
Appero.shouldShowAppero() // Returns cached decision offline
```

**Database Schema Design:**
- `cached_experiences` table: Experience logs waiting for sync
- `feedback_queue` table: Feedback submissions pending upload
- `ui_cache` table: Cached strings, flows, and configurations
- `sync_metadata` table: Last sync timestamps and versioning

**Key Features from iOS SDK Reference:**
- ✅ Experience logging continues offline (cached locally)
- ✅ Feedback submissions queued and retried automatically
- ✅ "Ready to show" state cached independently of network
- ✅ UI strings and flow types stored locally
- ✅ Developer maintains control over UI presentation timing
- ✅ Seamless sync when connectivity returns

**Flutter Wrapper Considerations:**
- Expose offline queue status to Flutter layer
- Provide network state events via platform channels
- Cache management accessible from Flutter
- Consistent offline behavior across platforms

**Testing Requirements:**
- Unit tests for offline queue management
- Integration tests for sync scenarios
- Network simulation testing (airplane mode, poor connectivity)
- Data persistence verification across app restarts
- Cache invalidation and cleanup testing

**Success Metrics:**
- Zero data loss during offline periods
- Smooth user experience regardless of connectivity
- Efficient background sync without battery drain
- Consistent behavior with iOS SDK offline capabilities

