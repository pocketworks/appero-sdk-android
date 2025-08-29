# Flutter Integration Guide

This guide explains how to integrate the Appero Android SDK into Flutter projects.

## 📦 Installation

### 1. Add GitHub Packages Repository

Add the GitHub Packages repository to your `android/build.gradle`:

```gradle
allprojects {
    repositories {
        google()
        mavenCentral()  // Required for other dependencies
        maven {
            url 'https://maven.pkg.github.com/pocketworks/appero-sdk-android'
            credentials {
                username = project.findProperty('gpr.user') ?: System.getenv('GITHUB_USERNAME')
                password = project.findProperty('gpr.key') ?: System.getenv('GITHUB_TOKEN')
            }
        }
    }
}
```

### 2. Add Dependency

Add the SDK dependency to your `android/app/build.gradle`:

```gradle
dependencies {
    implementation 'com.pocketworks:appero-sdk-android:1.0.0'
}
```

### 3. Set Up GitHub Token

Create a `~/.gradle/gradle.properties` file with your GitHub credentials:

```properties
gpr.user=your_github_username
gpr.key=your_github_personal_access_token
```

Or set environment variables:

```bash
export GITHUB_USERNAME=your_github_username
export GITHUB_TOKEN=your_github_personal_access_token
```

## 🚀 Usage

### 1. Initialize SDK

In your Flutter app's Android code (e.g., `MainActivity.kt`):

```kotlin
import com.appero.sdk.Appero
import com.appero.sdk.debug.ApperoDebugMode

class MainActivity: FlutterActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize Appero SDK
        Appero.start(
            context = this,
            apiKey = "your_api_key_here",
            clientId = "your_client_id_here",
            debugMode = ApperoDebugMode.DEBUG
        )
    }
}
```

### 2. Create Flutter Platform Channel

Create a Flutter platform channel to communicate with the Android SDK:

```dart
import 'package:flutter/services.dart';

class ApperoSDK {
  static const MethodChannel _channel = MethodChannel('appero_sdk');

  // Initialize SDK
  static Future<void> initialize({
    required String apiKey,
    required String clientId,
    bool debugMode = false,
  }) async {
    await _channel.invokeMethod('initialize', {
      'apiKey': apiKey,
      'clientId': clientId,
      'debugMode': debugMode,
    });
  }

  // Log experience
  static Future<void> logExperience(String name, int rating) async {
    await _channel.invokeMethod('logExperience', {
      'name': name,
      'rating': rating,
    });
  }

  // Log custom points
  static Future<void> logPoints(int points) async {
    await _channel.invokeMethod('logPoints', {
      'points': points,
    });
  }

  // Show feedback prompt
  static Future<void> showFeedbackPrompt() async {
    await _channel.invokeMethod('showFeedbackPrompt');
  }

  // Set user ID
  static Future<void> setUser(String userId) async {
    await _channel.invokeMethod('setUser', {
      'userId': userId,
    });
  }

  // Reset user
  static Future<void> resetUser() async {
    await _channel.invokeMethod('resetUser');
  }

  // Reset experience
  static Future<void> resetExperience() async {
    await _channel.invokeMethod('resetExperience');
  }
}
```

### 3. Implement Platform Channel in Android

In your `MainActivity.kt`:

```kotlin
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel
import com.appero.sdk.Appero
import com.appero.sdk.debug.ApperoDebugMode
import com.appero.sdk.domain.model.Experience

class MainActivity: FlutterActivity() {
    private val CHANNEL = "appero_sdk"

    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)
        
        MethodChannel(flutterEngine.dartExecutor.binaryMessenger, CHANNEL).setMethodCallHandler { call, result ->
            when (call.method) {
                "initialize" -> {
                    val apiKey = call.argument<String>("apiKey") ?: ""
                    val clientId = call.argument<String>("clientId") ?: ""
                    val debugMode = call.argument<Boolean>("debugMode") ?: false
                    
                    Appero.start(
                        context = this,
                        apiKey = apiKey,
                        clientId = clientId,
                        debugMode = if (debugMode) ApperoDebugMode.DEBUG else ApperoDebugMode.PRODUCTION
                    )
                    result.success(null)
                }
                
                "logExperience" -> {
                    val name = call.argument<String>("name") ?: ""
                    val rating = call.argument<Int>("rating") ?: 0
                    
                    Appero.log(Experience.valueOf(name), rating)
                    result.success(null)
                }
                
                "logPoints" -> {
                    val points = call.argument<Int>("points") ?: 0
                    Appero.log(points)
                    result.success(null)
                }
                
                "showFeedbackPrompt" -> {
                    Appero.showFeedbackPrompt()
                    result.success(null)
                }
                
                "setUser" -> {
                    val userId = call.argument<String>("userId") ?: ""
                    Appero.setUser(userId)
                    result.success(null)
                }
                
                "resetUser" -> {
                    Appero.resetUser()
                    result.success(null)
                }
                
                "resetExperience" -> {
                    Appero.resetExperienceAndPrompt()
                    result.success(null)
                }
                
                else -> {
                    result.notImplemented()
                }
            }
        }
    }
}
```

## 📱 Example Flutter Usage

```dart
import 'package:flutter/material.dart';
import 'appero_sdk.dart';

void main() async {
  WidgetsFlutterBinding.ensureInitialized();
  
  // Initialize Appero SDK
  await ApperoSDK.initialize(
    apiKey: 'your_api_key_here',
    clientId: 'your_client_id_here',
    debugMode: true,
  );
  
  runApp(MyApp());
}

class MyApp extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      home: Scaffold(
        appBar: AppBar(title: Text('Appero Flutter Demo')),
        body: Center(
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              ElevatedButton(
                onPressed: () => ApperoSDK.logExperience('button_click', 5),
                child: Text('Log Positive Experience'),
              ),
              ElevatedButton(
                onPressed: () => ApperoSDK.logPoints(3),
                child: Text('Add 3 Points'),
              ),
              ElevatedButton(
                onPressed: () => ApperoSDK.showFeedbackPrompt(),
                child: Text('Show Feedback'),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
```

## 🔧 Configuration

### Permissions

Add required permissions to `android/app/src/main/AndroidManifest.xml`:

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
```

### ProGuard Rules

If you're using ProGuard, add these rules to `android/app/proguard-rules.pro`:

```proguard
# Appero SDK
-keep class com.appero.sdk.** { *; }
-keepclassmembers class com.appero.sdk.** { *; }
```

## 🚀 Next Steps

1. **Publish to GitHub Packages**: Run the publishing script
2. **Test Integration**: Verify the SDK works in your Flutter app
3. **Create Flutter Plugin**: Consider creating a proper Flutter plugin package
4. **Documentation**: Add comprehensive documentation for Flutter developers

## 📚 Resources

- [GitHub Packages Documentation](https://docs.github.com/en/packages/working-with-a-github-packages-registry/working-with-the-gradle-registry)
- [Flutter Platform Channels](https://docs.flutter.dev/development/platform-integration/platform-channels)
- [Appero Android SDK Documentation](../README.md) 