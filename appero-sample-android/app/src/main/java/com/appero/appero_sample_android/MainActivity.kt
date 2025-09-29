package com.appero.appero_sample_android

import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ripple.rememberRipple
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
import androidx.fragment.app.FragmentActivity
import com.appero.sdk.Appero
import com.appero.sdk.debug.ApperoDebugMode
import com.appero.sdk.ui.config.FeedbackFlowConfig
import com.appero.sdk.ui.config.FeedbackPromptConfig
import com.appero.sdk.domain.model.Experience
import com.appero.sdk.ui.theme.CustomTheme
import com.appero.sdk.ui.theme.DarkTheme
import com.appero.sdk.ui.theme.DefaultTheme
import com.appero.sdk.ui.theme.LightTheme

class MainActivity : FragmentActivity() {
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
    
    // Theme state management
    var selectedTheme by remember { mutableStateOf(0) }
    val isDarkTheme = selectedTheme == 2
    
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
            .background(
                color = when (selectedTheme) {
                    0 -> Color.Transparent // System - let MaterialTheme handle it
                    1 -> Color.White // Light
                    else -> Color.Black // Dark
                }
            )
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
            textAlign = TextAlign.Center,
            color = if (isDarkTheme) Color.White else Color.Black
        )
        
        // Theme selector (tab style)
        Text(
            text = "Appero UI theme:",
            fontSize = 18.sp,
            textAlign = TextAlign.Center,
            color = if (isDarkTheme) Color.White else Color.Black
        )
        val tabs = listOf("System", "Light", "Dark")
        TabRow(
            selectedTabIndex = selectedTheme,
            containerColor = if (isDarkTheme) Color(0xFF1C1C1E) else Color.White,
            contentColor = if (isDarkTheme) Color.White else Color.Black
        ) {
            tabs.forEachIndexed { index, label ->
                Tab(
                    selected = selectedTheme == index,
                    onClick = {
                        selectedTheme = index
                        Appero.theme = when (index) {
                            0 -> DefaultTheme()
                            1 -> LightTheme()
                            else -> DarkTheme()
                        }
                    },
                    text = { 
                        Text(
                            label,
                            color = if (isDarkTheme) Color.White else Color.Black
                        ) 
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Experience logging buttons
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = rememberRipple(color = Color(0xFF4CAF50))
                ) {
                    Appero.log(Experience.VERY_POSITIVE)
                    experienceState = Appero.getExperienceState()
                },
            color = if (isDarkTheme) Color(0xFF2D4A2D) else Color(0xFFE8F5E8),
            shape = RoundedCornerShape(8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("👍", fontSize = 18.sp)
                Text("Very Positive", color = if (isDarkTheme) Color(0xFF64B5F6) else Color(0xFF007AFF))
            }
        }
        
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = rememberRipple(color = Color(0xFF4CAF50))
                ) {
                    Appero.log(Experience.POSITIVE)
                    experienceState = Appero.getExperienceState()
                },
            color = if (isDarkTheme) Color(0xFF2D4A2D) else Color(0xFFE8F5E8),
            shape = RoundedCornerShape(8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("👍", fontSize = 18.sp, color = Color(0xFF4CAF50))
                Text("Positive", color = if (isDarkTheme) Color(0xFF64B5F6) else Color(0xFF007AFF))
            }
        }
        
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = rememberRipple(color = Color(0xFFFF9800))
                ) {
                    Appero.log(Experience.NEUTRAL)
                    experienceState = Appero.getExperienceState()
                },
            color = if (isDarkTheme) Color(0xFF4A3D2D) else Color(0xFFFFF3E0),
            shape = RoundedCornerShape(8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("⚪", fontSize = 18.sp, color = Color(0xFFFF9800))
                Text("Neutral", color = if (isDarkTheme) Color(0xFF64B5F6) else Color(0xFF007AFF))
            }
        }
        
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = rememberRipple(color = Color(0xFFF44336))
                ) {
                    Appero.log(Experience.NEGATIVE)
                    experienceState = Appero.getExperienceState()
                },
            color = if (isDarkTheme) Color(0xFF4A2D2D) else Color(0xFFFFEBEE),
            shape = RoundedCornerShape(8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("👎", fontSize = 18.sp, color = Color(0xFFF44336))
                Text("Negative", color = if (isDarkTheme) Color(0xFF64B5F6) else Color(0xFF007AFF))
            }
        }
        
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = rememberRipple(color = Color(0xFFF44336))
                ) {
                    Appero.log(Experience.VERY_NEGATIVE)
                    experienceState = Appero.getExperienceState()
                },
            color = if (isDarkTheme) Color(0xFF4A2D2D) else Color(0xFFFFEBEE),
            shape = RoundedCornerShape(8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("👎", fontSize = 18.sp)
                Text("Very Negative", color = if (isDarkTheme) Color(0xFF64B5F6) else Color(0xFF007AFF))
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Manual feedback buttons
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = rememberRipple(color = if (isDarkTheme) Color(0xFF64B5F6) else Color(0xFF007AFF))
                ) {
                    Appero.showFeedbackPrompt(
                        config = feedbackConfig,
                        onResult = { _, _ ->
                            experienceState = Appero.getExperienceState()
                        }
                    )
                },
            color = if (isDarkTheme) Color(0xFF2C2C2E) else Color(0xFFF2F2F7),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(
                modifier = Modifier.padding(16.dp),
                text = "Manually Trigger Feedback (Compose)",
                color = if (isDarkTheme) Color(0xFF64B5F6) else Color(0xFF007AFF)
            )
        }

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = rememberRipple(color = if (isDarkTheme) Color(0xFF64B5F6) else Color(0xFF007AFF))
                ) {
                    (context as? FragmentActivity)?.let { activity ->
                        Appero.showFeedbackDialog(
                            activity = activity,
                            config = feedbackConfig,
                            onResult = { _, _ ->
                                experienceState = Appero.getExperienceState()
                            }
                        )
                    }
                },
            color = if (isDarkTheme) Color(0xFF2C2C2E) else Color(0xFFF2F2F7),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(
                modifier = Modifier.padding(16.dp),
                text = "Manually Trigger Feedback (XML)", 
                color = if (isDarkTheme) Color(0xFF64B5F6) else Color(0xFF007AFF)
            )
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