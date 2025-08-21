package com.appero.appero_sample_android

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.FragmentActivity
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.appero.appero_sample_android.ui.theme.ApperoSampleAndroidTheme
import com.appero.appero_sample_android.R
import com.appero.sdk.Appero
import com.appero.sdk.domain.model.Experience
import com.appero.sdk.ui.config.FeedbackFlowConfig
import com.appero.sdk.ui.config.FeedbackPromptConfig

/**
 * Demonstrates hybrid XML + Compose approach for legacy projects
 * Shows how XML-based apps can gradually adopt Compose components
 */
class XmlDemoActivity : FragmentActivity() {
    
    private lateinit var tvExperienceState: TextView
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_xml_demo)
        
        // Register for automatic XML dialog triggering
        Appero.registerLegacyActivity(this)
        
        // Initialize views
        setupXmlViews()
        setupComposeView()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        // Unregister to prevent memory leaks
        Appero.unregisterLegacyActivity()
    }
    
    private fun setupXmlViews() {
        tvExperienceState = findViewById(R.id.tvExperienceState)
        updateExperienceState()
        
        // XML Dialog buttons
        findViewById<Button>(R.id.btnXmlDialog).setOnClickListener {
            showXmlDialog()
        }
        
        // Experience tracking buttons
        findViewById<Button>(R.id.btnPositive).setOnClickListener {
            Appero.log(Experience.POSITIVE)
            updateExperienceState()
            Toast.makeText(this, "Positive experience logged (+1)", Toast.LENGTH_SHORT).show()
        }
        
        findViewById<Button>(R.id.btnNeutral).setOnClickListener {
            Appero.log(Experience.NEUTRAL)
            updateExperienceState()
            Toast.makeText(this, "Neutral experience logged (0)", Toast.LENGTH_SHORT).show()
        }
        
        findViewById<Button>(R.id.btnNegative).setOnClickListener {
            Appero.log(Experience.NEGATIVE)
            updateExperienceState()
            Toast.makeText(this, "Negative experience logged (-1)", Toast.LENGTH_SHORT).show()
        }
        
        // Test auto-trigger button
        findViewById<Button>(R.id.btnTestAutoTrigger).setOnClickListener {
            // Add enough positive experiences to cross the threshold (default is 5)
            repeat(5) {
                Appero.log(Experience.POSITIVE)
            }
            updateExperienceState()
            Toast.makeText(this, "Added +5 points to trigger threshold! XML dialog should appear automatically.", Toast.LENGTH_LONG).show()
        }
        
        // Back button
        findViewById<Button>(R.id.btnBackToCompose).setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }
    
    private fun setupComposeView() {
        val composeView = findViewById<ComposeView>(R.id.composeView)
        
        composeView.setContent {
            ApperoSampleAndroidTheme {
                ComposeViewContent()
            }
        }
    }
    
    @Composable
    private fun ComposeViewContent() {
        var experienceState by remember { mutableStateOf(Appero.getExperienceState()) }
        
        // Configuration for the feedback prompt
        val feedbackConfig = remember {
            FeedbackPromptConfig(
                title = "ComposeView Feedback 🚀",
                subtitle = "Modern Compose UI embedded in XML layout",
                followUpQuestion = "How does the hybrid approach feel?",
                placeholder = "Share your thoughts on XML + Compose...",
                submitText = "Send via ComposeView",
                maxCharacters = 120
            )
        }
        
        val feedbackFlowConfig = remember {
            FeedbackFlowConfig(
                thankYouTitle = "Thank you for trying the hybrid demo!",
                thankYouSubtitle = "You've experienced both XML and Compose",
                thankYouCtaText = "Close"
            )
        }
        
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = {
                    Appero.showFeedbackPrompt(
                        config = feedbackConfig,
                        onResult = { success, message ->
                            val toastMessage = if (success) {
                                "✅ ComposeView: Feedback submitted!"
                            } else {
                                "❌ ComposeView: Failed to submit: $message"
                            }
                            Toast.makeText(this@XmlDemoActivity, toastMessage, Toast.LENGTH_LONG).show()
                            experienceState = Appero.getExperienceState()
                            updateExperienceState() // Update XML TextView too
                        }
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("🎛️ Show Compose Bottom Sheet")
            }
            
            // Show current experience state in Compose
            experienceState?.let { state ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Compose State Display",
                            fontSize = 14.sp,
                            color = Color.Gray
                        )
                        Text(
                            text = "Points: ${state.experiencePoints} | Ready: ${state.shouldShowPrompt}",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                }
            }
        }
        
        // Include the Appero Feedback UI (will show when triggered)
        Appero.FeedbackPromptUI(
            config = feedbackConfig,
            flowConfig = feedbackFlowConfig,
            reviewPromptThreshold = 4,
            onRequestReview = {
                Appero.requestPlayStoreReview(this@XmlDemoActivity)
            },
            onResult = { success, message ->
                val toastMessage = if (success) {
                    "✅ ComposeView: Feedback submitted successfully!"
                } else {
                    "❌ ComposeView: Failed to submit feedback: $message"
                }
                Toast.makeText(this@XmlDemoActivity, toastMessage, Toast.LENGTH_LONG).show()
                experienceState = Appero.getExperienceState()
                updateExperienceState() // Update XML TextView too
            }
        )
    }
    
    private fun showXmlDialog() {
        val config = FeedbackPromptConfig(
            title = "XML Bottom Sheet 📋",
            subtitle = "Modal bottom sheet using traditional XML Views",
            followUpQuestion = "How does the XML bottom sheet feel?",
            placeholder = "Share your XML bottom sheet experience...",
            submitText = "Send via XML Bottom Sheet",
            maxCharacters = 120
        )
        
        Appero.showFeedbackDialog(
            activity = this,
            config = config,
            onResult = { success, message ->
                val toastMessage = if (success) {
                    "✅ XML Bottom Sheet: Feedback submitted!"
                } else {
                    "❌ XML Bottom Sheet: Failed to submit: $message"
                }
                Toast.makeText(this, toastMessage, Toast.LENGTH_LONG).show()
                updateExperienceState()
            }
        )
    }
    
    private fun updateExperienceState() {
        val state = Appero.getExperienceState()
        state?.let {
            tvExperienceState.text = "Experience Points: ${it.experiencePoints} | Ready: ${it.shouldShowPrompt}"
        } ?: run {
            tvExperienceState.text = "Experience state not available"
        }
    }
} 