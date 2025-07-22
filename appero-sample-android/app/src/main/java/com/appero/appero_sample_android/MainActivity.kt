package com.appero.appero_sample_android

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
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
import com.example.appero_sdk_android.Appero
import com.example.appero_sdk_android.Experience
import com.example.appero_sdk_android.HelloWorld
import com.example.appero_sdk_android.ui.FeedbackPromptConfig

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Initialize the Appero SDK
        Appero.start(
            context = this,
            apiKey = "9d7d6790-7d9b-42ab-8f08-b5d07e8d9dc0", // Updated API key to match curl
            clientId = "beeec9b8-3908-4605-9b45-faded129d41e" // Sample client ID
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
            maxCharacters = 120
        )
    }
    
    // Update experience state when it changes
    LaunchedEffect(Unit) {
        experienceState = Appero.getExperienceState()
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
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
                    text = HelloWorld.greet(),
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
        
        // Feedback Prompt Controls
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Feedback Prompt",
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
                            Appero.showFeedbackPrompt(
                                config = feedbackConfig,
                                onResult = { success, message ->
                                    val toastMessage = if (success) {
                                        "✅ Feedback submitted: $message"
                                    } else {
                                        "❌ Failed to submit: $message"
                                    }
                                    Toast.makeText(context, toastMessage, Toast.LENGTH_LONG).show()
                                    experienceState = Appero.getExperienceState()
                                }
                            )
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF2196F3)
                        )
                    ) {
                        Text("Show Feedback")
                    }
                    
                    Button(
                        onClick = { 
                            Appero.resetExperienceAndPrompt()
                            experienceState = Appero.getExperienceState()
                            Toast.makeText(context, "Experience and feedback status reset", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF9E9E9E)
                        )
                    ) {
                        Text("Reset")
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.weight(1f))
    }
    
    // Appero Feedback Prompt UI with result callback
    Appero.FeedbackPromptUI(
        config = feedbackConfig,
        onResult = { success, message ->
            val toastMessage = if (success) {
                "✅ Feedback submitted successfully!"
            } else {
                "❌ Failed to submit feedback: $message"
            }
            Toast.makeText(context, toastMessage, Toast.LENGTH_LONG).show()
            experienceState = Appero.getExperienceState()
        }
    )
}

@Preview(showBackground = true)
@Composable
fun ApperoSampleAppPreview() {
    ApperoSampleAndroidTheme {
        ApperoSampleApp()
    }
}