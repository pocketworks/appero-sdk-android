package com.appero.appero_sample_android

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.appero_sdk_android.Appero
import com.example.appero_sdk_android.Experience
import com.example.appero_sdk_android.HelloWorld

class MainActivity : AppCompatActivity() {
    
    private lateinit var statusTextView: TextView
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        
        // Initialize the Appero SDK
        Appero.start(
            context = this,
            apiKey = "a1b2c3d4-e5f6-7890-abcd-ef1234567890", // Sample API key
            clientId = "beeec9b8-3908-4605-9b45-faded129d41e" // Sample client ID
        )
        
        statusTextView = findViewById(R.id.textView)
        
        // Set up experience tracking demonstration
        setupExperienceDemo()
        
        // Update display
        updateStatusDisplay()
    }
    
    private fun setupExperienceDemo() {
        // Create buttons for different experience levels (you'll need to add these to your layout)
        // For now, we'll simulate some experience tracking
        
        // Simulate some positive experiences after a delay
        statusTextView.postDelayed({
            // Log some positive experiences
            Appero.log(Experience.POSITIVE)
            Appero.log(Experience.VERY_POSITIVE)
            Appero.log(Experience.POSITIVE)
            updateStatusDisplay()
        }, 1000)
    }
    
    private fun updateStatusDisplay() {
        val greeting = HelloWorld.greet()
        val isInitialized = Appero.isInitialized()
        val experienceState = Appero.getExperienceState()
        
        val statusText = buildString {
            appendLine(greeting)
            appendLine()
            appendLine("SDK Initialized: $isInitialized")
            
            if (experienceState != null) {
                appendLine()
                appendLine("Experience Tracking:")
                appendLine("• Points: ${experienceState.experiencePoints}")
                appendLine("• Threshold: ${experienceState.ratingThreshold}")
                appendLine("• Should Show Prompt: ${experienceState.shouldShowPrompt}")
                appendLine("• Has Submitted Feedback: ${experienceState.hasSubmittedFeedback}")
                
                if (experienceState.shouldShowPrompt) {
                    appendLine()
                    appendLine("🎉 Ready to ask for feedback!")
                }
            }
        }
        
        statusTextView.text = statusText
    }
}