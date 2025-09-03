package com.appero.appero_sample_android

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.appero.appero_sample_android.ui.theme.ApperoSampleAndroidTheme
import android.app.Activity
import com.appero.sdk.Appero
import com.appero.sdk.debug.ApperoDebugMode
import com.appero.sdk.domain.model.Experience
import com.appero.sdk.ui.config.FeedbackFlowConfig
import com.appero.sdk.ui.config.FeedbackPromptConfig
import com.appero.sdk.ui.theme.CustomTheme
import com.appero.sdk.ui.theme.DarkTheme
import com.appero.sdk.ui.theme.DefaultTheme
import com.appero.sdk.ui.theme.LightTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Initialize the Appero SDK with DEBUG mode for development
        Appero.start(
            context = this,
            apiKey = "Cu8i7jOIm1cN2IhDO3iqV2cLSzcdI9/zUaws7+d19Rs", // Updated API key to match curl
            clientId = "beeec9b8-3908-4605-9b45-faded129d41e", // Sample client ID
            debugMode = ApperoDebugMode.DEBUG // Enable debug logging for development
        )
        
        // Set up analytics listener for tracking Appero events
        Appero.setAnalyticsListener(ExampleAnalyticsListener())
        
        // Set default custom theme for the sample app
        Appero.theme = CustomTheme(
            primaryColor = Color(0xFF4CAF50),
            accentColor = Color(0xFF4CAF50),
            buttonBackgroundColor = Color(0xFF4CAF50),
            veryNegativeColor = Color(0xFFFF6B6B),
            negativeColor = Color(0xFFFF9F43),
            neutralColor = Color(0xFFFECA57),
            positiveColor = Color(0xFF48CAE4),
            veryPositiveColor = Color(0xFF4CAF50)
        )
        
        setContent {
            ApperoSampleAndroidTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    ApperoSampleApp()
                }
            }
        }
    }
}

@Composable
fun ApperoSampleApp() {
    var experienceState by remember { mutableStateOf(Appero.getExperienceState()) }
    val context = LocalContext.current
    
    // Configuration for the feedback prompt
    val feedbackConfig = remember {
        FeedbackPromptConfig(
            title = "We're happy to see that you're using Appero Sample App 🎉",
            subtitle = "Let us know how we're doing",
            followUpQuestion = "What made your experience positive?",
            placeholder = "Share your thoughts here",
            submitText = "Send feedback",
            maxCharacters = 240
        )
    }
    
    // Demo config for the feedback flow
    val feedbackFlowConfig = remember {
        FeedbackFlowConfig(
            thankYouTitle = "Thank you for your valuable feedback!",
            thankYouSubtitle = "Your input helps us improve our app",
            thankYouCtaText = "Close"
        )
    }
    val reviewPromptThreshold = 4

    // Update experience state when it changes
    LaunchedEffect(Unit) {
        experienceState = Appero.getExperienceState()
    }
    
    val scrollState = rememberScrollState()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(modifier = Modifier.height(32.dp))
        
        // Title
        Text(
            text = "Appero SDK Demo",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        
        // Prominent CTA to XML Demo
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF4CAF50))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "🚀 Try the Hybrid Approach!",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "See how XML projects can adopt Compose gradually",
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.9f),
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Button(
                    onClick = { 
                        context.startActivity(Intent(context, XmlDemoActivity::class.java))
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White)
                ) {
                    Text(
                        text = "🔄 View XML + ComposeView Demo",
                        color = Color(0xFF4CAF50),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
        
        // API Comparison Info
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD))
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "🚀 Dual API Support",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "This sample demonstrates both UI approaches:",
                    fontSize = 14.sp,
                    color = Color.Gray
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "• 🎛️ Compose UI: Modern bottom sheet (recommended)\n• 🏛️ XML Dialog: Traditional DialogFragment (legacy support)\n• 🔄 Same backend: Both use identical analytics & API integration",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
                

            }
        }
        
        // Hello World from SDK
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Appero SDK Demo",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "SDK Initialized: ${Appero.isInitialized()}",
                    fontSize = 14.sp,
                    color = Color.Gray
                )
            }
        }
        
        // Debug Mode Selector
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Debug Mode",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "Current: DEBUG (check logcat for API errors)",
                    fontSize = 14.sp,
                    color = Color.Gray
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "Available modes: PRODUCTION, DEBUG",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "• PRODUCTION: No logging\n• DEBUG: API errors and critical operations",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
        }
        
        // User Session Info
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "User Session",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text("• User ID: ${Appero.getCurrentUserId()?.take(8)}...", fontSize = 14.sp)
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { 
                            Appero.setUser("test-user-123")
                            experienceState = Appero.getExperienceState()
                            Toast.makeText(context, "Set user to: test-user-123", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF9C27B0)
                        )
                    ) {
                        Text("Set Test User", fontSize = 12.sp)
                    }
                    
                    Button(
                        onClick = { 
                            Appero.resetUser()
                            experienceState = Appero.getExperienceState()
                            Toast.makeText(context, "Reset to new anonymous user", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF607D8B)
                        )
                    ) {
                        Text("Reset User", fontSize = 12.sp)
                    }
                }
            }
        }
        
        // Experience Tracking Status
        experienceState?.let { state ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Experience Tracking",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text("• Points: ${state.experiencePoints}")
                    Text("• Threshold: ${state.ratingThreshold}")
                    Text("• Should Show Prompt: ${state.shouldShowPrompt}")
                    Text("• Has Submitted Feedback: ${state.hasSubmittedFeedback}")
                Text("• Queued Feedback: ${Appero.getQueuedFeedbackCount()}")
                    
                    if (state.shouldShowPrompt) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "🎉 Ready to ask for feedback!",
                            color = Color(0xFF4CAF50),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
        
        // Experience Buttons
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Test Experience Logging",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { 
                            Appero.log(Experience.VERY_POSITIVE)
                            experienceState = Appero.getExperienceState()
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF4CAF50)
                        )
                    ) {
                        Text("😍 +2")
                    }
                    
                    Button(
                        onClick = { 
                            Appero.log(Experience.POSITIVE)
                            experienceState = Appero.getExperienceState()
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF8BC34A)
                        )
                    ) {
                        Text("😊 +1")
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { 
                            Appero.log(Experience.NEGATIVE)
                            experienceState = Appero.getExperienceState()
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFFF9800)
                        )
                    ) {
                        Text("😕 -1")
                    }
                    
                    Button(
                        onClick = { 
                            Appero.log(Experience.VERY_NEGATIVE)
                            experienceState = Appero.getExperienceState()
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFF44336)
                        )
                    ) {
                        Text("😢 -2")
                    }
                }
            }
        }
        
        // Theme Selection
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0))
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "🎨 Theme Selection (iOS-Style)",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            Appero.theme = DefaultTheme()
                            Toast.makeText(context, "🔄 System Theme Applied (Auto Light/Dark)", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF007AFF))
                    ) {
                        Text("System", fontSize = 10.sp)
                    }
                    
                    Button(
                        onClick = {
                            Appero.theme = LightTheme()
                            Toast.makeText(context, "☀️ Light Theme Applied", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFFFFF), contentColor = Color.Black)
                    ) {
                        Text("Light", fontSize = 10.sp)
                    }
                    
                    Button(
                        onClick = {
                            Appero.theme = DarkTheme()
                            Toast.makeText(context, "🌙 Dark Theme Applied", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1C1C1E))
                    ) {
                        Text("Dark", fontSize = 10.sp)
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Button(
                    onClick = {
                        // Custom brand theme with purple/pink colors
                        Appero.theme = CustomTheme(
                            primaryColor = Color(0xFF6B46C1),
                            accentColor = Color(0xFF8B5CF6),
                            buttonBackgroundColor = Color(0xFF9333EA),
                            veryPositiveColor = Color(0xFF10B981),
                            positiveColor = Color(0xFF3B82F6),
                            neutralColor = Color(0xFFF59E0B),
                            negativeColor = Color(0xFFEF4444),
                            veryNegativeColor = Color(0xFFDC2626)
                        )
                        Toast.makeText(context, "🎨 Custom Brand Theme Applied!", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF9333EA))
                ) {
                    Text("Custom Brand Theme")
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "• Themes are applied instantly\n• Same API as iOS: Appero.theme = CustomTheme()\n• Supports light/dark/custom branding",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
        }
        
        // Themed Feedback Prompt Demos
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E8))
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "🎨 Themed Feedback Demos",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Default System Theme Button
                Button(
                    onClick = { 
                        // Set default theme and show prompt
                        val originalTheme = Appero.theme
                        Appero.theme = DefaultTheme()
                        
                        Appero.showFeedbackPrompt(
                            config = feedbackConfig.copy(
                                title = "Default System Theme 🔄",
                                subtitle = "Using system colors (auto light/dark)"
                            ),
                            onResult = { success, message ->
                                experienceState = Appero.getExperienceState()
                                // Restore original theme
                                Appero.theme = originalTheme
                            }
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF007AFF) // iOS blue
                    )
                ) {
                    Text("📱 Show with Default System Theme")
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Original Green Theme Button  
                Button(
                    onClick = { 
                        // Set original green theme and show prompt
                        val originalTheme = Appero.theme
                        Appero.theme = CustomTheme(
                            primaryColor = Color(0xFF4CAF50),
                            accentColor = Color(0xFF4CAF50),
                            buttonBackgroundColor = Color(0xFF4CAF50),
                            veryNegativeColor = Color(0xFFFF6B6B),
                            negativeColor = Color(0xFFFF9F43),
                            neutralColor = Color(0xFFFECA57),
                            positiveColor = Color(0xFF48CAE4),
                            veryPositiveColor = Color(0xFF4CAF50)
                        )
                        
                        Appero.showFeedbackPrompt(
                            config = feedbackConfig.copy(
                                title = "Original Green Theme 🟢",
                                subtitle = "Using the classic green colors we had before"
                            ),
                            onResult = { success, message ->
                                experienceState = Appero.getExperienceState()
                                // Restore original theme
                                Appero.theme = originalTheme
                            }
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4CAF50) // Green
                    )
                ) {
                    Text("🟢 Show with Original Green Theme")
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "• Each button temporarily applies its theme\n• Themes are restored after feedback submission\n• Test keyboard handling with both themes",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
        }

        // Play Store Review Testing
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF8E1))
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "🧪 Play Store Review Testing",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "Test in-app reviews with current package: ${context.packageName}",
                    fontSize = 14.sp,
                    color = Color.Gray
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { 
                            // Test with current app's package (should work if using a published app's package)
                            Appero.requestPlayStoreReview(context as Activity) { result ->
                                val message = when (result) {
                                    is Appero.PlayStoreReviewResult.InAppReviewShown -> "✅ In-app review shown!"
                                    is Appero.PlayStoreReviewResult.InAppReviewCompleted -> "✅ In-app review completed!"
                                    is Appero.PlayStoreReviewResult.FallbackTriggered -> "✅ Fallback to Play Store"
                                    is Appero.PlayStoreReviewResult.Failed -> "❌ Failed: ${result.reason}"
                                }
                                Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF4CAF50)
                        )
                    ) {
                        Text("Test Current Package", fontSize = 10.sp)
                    }
                    
                    Button(
                        onClick = { 
                            // Test the feedback flow that triggers review
                            Appero.showFeedbackPrompt(
                                config = feedbackConfig.copy(
                                    title = "Test In-App Review 🧪",
                                    subtitle = "This should trigger a Play Store review"
                                ),
                                onResult = { success, message ->
                                    experienceState = Appero.getExperienceState()
                                }
                            )
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF2196F3)
                        )
                    ) {
                        Text("Test Full Flow", fontSize = 10.sp)
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "• Use build variants to test with different package names\n• In-app reviews only work with published app packages\n• If using a published package, you should see the real review dialog!",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
        }
        
        Spacer(modifier = Modifier.weight(1f))
    }
    
    // Appero Feedback Prompt UI with new flow
    Appero.FeedbackPromptUI(
        config = feedbackConfig,
        flowConfig = feedbackFlowConfig,
        reviewPromptThreshold = reviewPromptThreshold,
        onRequestReview = {
            // Trigger Play Store review prompt
            if (context is Activity) {
                Appero.requestPlayStoreReview(context as Activity)
            }
        },
        onResult = { success, message ->
            experienceState = Appero.getExperienceState()
        },
        activity = context as? Activity
    )
}

@Preview(showBackground = true)
@Composable
fun ApperoSampleAppPreview() {
    ApperoSampleAndroidTheme {
        ApperoSampleApp()
    }
}