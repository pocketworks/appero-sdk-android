package com.appero.appero_sample_android

import android.os.Bundle
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.appero_sdk_android.Appero
import com.example.appero_sdk_android.HelloWorld

class MainActivity : AppCompatActivity() {
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
        
        // Display SDK status and greeting
        val greeting = HelloWorld.greet()
        val isInitialized = Appero.isInitialized()
        val statusText = "$greeting\n\nSDK Initialized: $isInitialized"
        
        findViewById<TextView>(R.id.textView)?.text = statusText
    }
}