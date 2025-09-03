package com.appero.sdk.ui.legacy

import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import com.example.appero_sdk_android.R
import com.appero.sdk.analytics.ApperoAnalyticsListener
import com.appero.sdk.ui.config.FeedbackPromptConfig
import com.appero.sdk.ui.components.FeedbackStep
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import androidx.compose.ui.graphics.toArgb

/**
 * Legacy BottomSheetDialogFragment for XML-based projects
 * Provides Appero feedback collection as a modal bottom sheet using traditional Android View system
 */
class FeedbackDialogFragment : BottomSheetDialogFragment() {

    companion object {
        fun newInstance(): FeedbackDialogFragment {
            return FeedbackDialogFragment()
        }
    }

    private var config: FeedbackPromptConfig? = null
    private var analyticsListener: ApperoAnalyticsListener? = null
    private var onSubmitCallback: ((rating: Int, feedback: String) -> Unit)? = null
    private var onDismissCallback: (() -> Unit)? = null
    private var onFeedbackSubmissionCallback: ((success: Boolean, message: String?) -> Unit)? = null
    
    // Rating state
    private var selectedRating: Int = 0
    private val ratingButtons = mutableListOf<ImageButton>()
    
    // Flow state
    private var initialStep: FeedbackStep = FeedbackStep.Rating
    private var currentStep: FeedbackStep = FeedbackStep.Rating
    private var isSubmitting: Boolean = false
    private var flowConfig: com.appero.sdk.ui.config.FeedbackFlowConfig? = null
    private var reviewPromptThreshold: Int = 4
    private var onRequestReview: (() -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_feedback_legacy, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUI(view)
    }

    private fun setupUI(view: View) {
        val config = this.config ?: return

        // Setup title and subtitle
        view.findViewById<TextView>(R.id.tvTitle)?.text = config.title
        view.findViewById<TextView>(R.id.tvSubtitle)?.text = config.subtitle

        // Setup rating buttons
        setupRatingButtons(view)

        // Setup follow-up question
        val tvFollowUp = view.findViewById<TextView>(R.id.tvFollowUp)
        tvFollowUp?.text = config.followUpQuestion

        // Setup feedback input
        val etFeedback = view.findViewById<EditText>(R.id.etFeedback)
        etFeedback?.hint = config.placeholder
        
        // Setup character counter
        val tvCharacterCounter = view.findViewById<TextView>(R.id.tvCharacterCounter)
        tvCharacterCounter?.text = "0/${config.maxCharacters}"
        
        // Add text change listener to update character counter and enforce limit
        etFeedback?.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                val text = s?.toString() ?: ""
                tvCharacterCounter?.text = "${text.length}/${config.maxCharacters}"
                
                // Enforce character limit
                if (text.length > config.maxCharacters) {
                    val limitedText = text.substring(0, config.maxCharacters)
                    etFeedback?.setText(limitedText)
                    etFeedback?.setSelection(limitedText.length)
                }
            }
        })
        
        // Add focus listeners to hide/show elements when keyboard appears (like Compose)
        etFeedback?.setOnFocusChangeListener { _, hasFocus ->
            // Don't change UI if we're currently submitting
            if (isSubmitting) {
                return@setOnFocusChangeListener
            }
            
            val tvFollowUp = view.findViewById<TextView>(R.id.tvFollowUp)
            
            if (hasFocus) {
                // Hide title and rating when text input is focused
                view.findViewById<TextView>(R.id.tvTitle)?.visibility = View.GONE
                view.findViewById<View>(R.id.ratingContainer)?.visibility = View.GONE
                view.findViewById<TextView>(R.id.tvSubtitle)?.visibility = View.GONE
                
                // Hide "Not now" button when keyboard is visible (frustration flow)
                view.findViewById<Button>(R.id.btnNotNow)?.visibility = View.GONE
                
                // Add top margin to follow-up question when title is hidden
                tvFollowUp?.let { followUp ->
                    val layoutParams = followUp.layoutParams as? android.view.ViewGroup.MarginLayoutParams
                    layoutParams?.topMargin = (32 * resources.displayMetrics.density).toInt() // 32dp
                    followUp.layoutParams = layoutParams
                }
            } else {
                // Show title and rating when text input loses focus
                if (currentStep == FeedbackStep.Rating) {
                    view.findViewById<TextView>(R.id.tvTitle)?.visibility = View.VISIBLE
                    view.findViewById<View>(R.id.ratingContainer)?.visibility = View.VISIBLE
                    view.findViewById<TextView>(R.id.tvSubtitle)?.visibility = View.VISIBLE
                }
                
                // Show "Not now" button when keyboard is hidden (frustration flow)
                if (initialStep == FeedbackStep.Frustration) {
                    view.findViewById<Button>(R.id.btnNotNow)?.visibility = View.VISIBLE
                }
                
                // Reset top margin to original when title is visible
                tvFollowUp?.let { followUp ->
                    val layoutParams = followUp.layoutParams as? android.view.ViewGroup.MarginLayoutParams
                    layoutParams?.topMargin = 0 // Original margin
                    followUp.layoutParams = layoutParams
                }
            }
        }

        // Setup submit button with theming
        val btnSubmit = view.findViewById<Button>(R.id.btnSubmit)
        btnSubmit?.text = config.submitText
        btnSubmit?.setOnClickListener {
            val feedbackText = etFeedback?.text?.toString() ?: ""
            
            // Validate input before submission
            if (feedbackText.isBlank()) {
                return@setOnClickListener
            }
            
            // Prevent double submission
            if (isSubmitting) {
                return@setOnClickListener
            }
            
            // Show loading state FIRST (like Compose isSubmitting = true)
            showLoadingState(view)
            
            // Submit feedback through callback
            if (initialStep == FeedbackStep.Frustration) {
                // For frustration flow, always submit with rating 0
                onSubmitCallback?.invoke(0, feedbackText)
            } else {
                // For rating flow, require a rating
                if (selectedRating > 0) {
                    analyticsListener?.onRatingSelected(selectedRating)
                    onSubmitCallback?.invoke(selectedRating, feedbackText)
                }
            }
            
            // Note: Thank you step will be shown after API response
            // The SDK should call handleFeedbackSubmissionResult() after API completion
        }
        
        // Setup thank you title and subtitle
        val tvThankYouTitle = view.findViewById<TextView>(R.id.tvThankYouTitle)
        tvThankYouTitle?.text = flowConfig?.thankYouTitle ?: "Thank you for your feedback!"
        
        val tvThankYouSubtitle = view.findViewById<TextView>(R.id.tvThankYouSubtitle)
        tvThankYouSubtitle?.text = flowConfig?.thankYouSubtitle ?: "We appreciate your input"
        
        // Setup close button
        val btnClose = view.findViewById<Button>(R.id.btnClose)
        btnClose?.text = flowConfig?.thankYouCtaText ?: "Close"
        btnClose?.setOnClickListener {
            // Trigger Play Store review if eligible before dismissing
            if (selectedRating >= reviewPromptThreshold) {
                onRequestReview?.invoke()
            }
            dismiss()
        }
        
        // Apply Appero theme to submit button
        applyTheme(view)

        // Handle initial step (frustration vs rating flow)
        handleInitialStep(view)
    }
    
    private fun showLoadingState(view: View) {
        // Set submitting state to true (like Compose isSubmitting = true)
        isSubmitting = true
        
        // Hide ALL interactive elements to make room for the loader
        view.findViewById<View>(R.id.ratingContainer)?.visibility = View.GONE
        view.findViewById<View>(R.id.feedbackSection)?.visibility = View.GONE
        view.findViewById<Button>(R.id.btnSubmit)?.visibility = View.GONE
        view.findViewById<Button>(R.id.btnNotNow)?.visibility = View.GONE
        
        // Show the loading container - this should now be visible
        val loadingContainer = view.findViewById<View>(R.id.loadingContainer)
        loadingContainer?.visibility = View.VISIBLE
        
        // Debug: Log the visibility state
        android.util.Log.d("ApperoSDK", "showLoadingState: loadingContainer visibility = ${loadingContainer?.visibility}")
        android.util.Log.d("ApperoSDK", "showLoadingState: loadingContainer found = ${loadingContainer != null}")
    }
    
    private fun hideLoadingState(view: View) {
        // Reset submitting state to false (like Compose isSubmitting = false)
        isSubmitting = false
        
        // Hide loading progress bar
        val loadingContainer = view.findViewById<View>(R.id.loadingContainer)
        loadingContainer?.visibility = View.GONE
        
        // Restore the UI to the previous state based on current step
        if (currentStep == FeedbackStep.ThankYou || isSubmitting) {
            // If we're already in thank you step or still submitting, don't restore anything
            return
        }
        
        // Restore the appropriate UI state
        when (initialStep) {
            FeedbackStep.Frustration -> {
                // For frustration flow, show feedback section and submit button
                view.findViewById<View>(R.id.feedbackSection)?.visibility = View.VISIBLE
                view.findViewById<Button>(R.id.btnSubmit)?.visibility = View.VISIBLE
                view.findViewById<Button>(R.id.btnNotNow)?.visibility = View.VISIBLE
            }
            FeedbackStep.Rating -> {
                // For rating flow, show rating and feedback sections
                view.findViewById<View>(R.id.ratingContainer)?.visibility = View.VISIBLE
                view.findViewById<View>(R.id.feedbackSection)?.visibility = View.VISIBLE
                view.findViewById<Button>(R.id.btnSubmit)?.visibility = View.VISIBLE
            }
            else -> {
                // Default to rating flow for other steps
                view.findViewById<View>(R.id.ratingContainer)?.visibility = View.VISIBLE
                view.findViewById<View>(R.id.feedbackSection)?.visibility = View.VISIBLE
                view.findViewById<Button>(R.id.btnSubmit)?.visibility = View.VISIBLE
            }
        }
    }
    
    private fun onFeedbackSubmitted(success: Boolean, message: String? = null) {
        view?.let { view ->
            if (success) {
                // Show thank you step on success
                showThankYouStep(view)
            } else {
                // Show error and reset to input state
                hideLoadingState(view)
                // You could show a toast or error message here
            }
        }
        
        // Call the external callback if set
        onFeedbackSubmissionCallback?.invoke(success, message)
    }
    
    private fun showThankYouStep(view: View, apiMessage: String? = null) {
        // Reset submitting state (like Compose transitionToThankYou)
        isSubmitting = false
        
        // Hide all other sections
        view.findViewById<TextView>(R.id.tvTitle)?.visibility = View.GONE
        view.findViewById<TextView>(R.id.tvSubtitle)?.visibility = View.GONE
        view.findViewById<View>(R.id.ratingContainer)?.visibility = View.GONE
        view.findViewById<View>(R.id.feedbackSection)?.visibility = View.GONE
        view.findViewById<Button>(R.id.btnSubmit)?.visibility = View.GONE
        view.findViewById<Button>(R.id.btnNotNow)?.visibility = View.GONE
        view.findViewById<View>(R.id.loadingContainer)?.visibility = View.GONE
        
        // Show thank you section
        view.findViewById<View>(R.id.thankYouSection)?.visibility = View.VISIBLE
        
        // Update thank you message with API response if available
        if (apiMessage != null) {
            val tvThankYouSubtitle = view.findViewById<TextView>(R.id.tvThankYouSubtitle)
            tvThankYouSubtitle?.text = apiMessage
        }
        
        // Update current step
        currentStep = FeedbackStep.ThankYou
    }
    
    private fun handleInitialStep(view: View) {
        when (initialStep) {
            FeedbackStep.Frustration -> {
                // Hide rating buttons for frustration flow
                val ratingContainer = view.findViewById<View>(R.id.ratingContainer)
                ratingContainer?.visibility = View.GONE
                
                // Hide subtitle for frustration flow
                val tvSubtitle = view.findViewById<TextView>(R.id.tvSubtitle)
                tvSubtitle?.visibility = View.GONE
                
                // Show feedback section immediately
                val feedbackSection = view.findViewById<View>(R.id.feedbackSection)
                feedbackSection?.visibility = View.VISIBLE
                
                // Update follow-up question for frustration flow
                val tvFollowUp = view.findViewById<TextView>(R.id.tvFollowUp)
                tvFollowUp?.text = "Would you mind telling us what went wrong?"
                
                // Update feedback input placeholder for frustration flow
                val etFeedback = view.findViewById<EditText>(R.id.etFeedback)
                etFeedback?.hint = "Would you mind telling us what went wrong?"
                
                // Update submit button to work without rating
                val btnSubmit = view.findViewById<Button>(R.id.btnSubmit)
                btnSubmit?.isEnabled = true
                btnSubmit?.visibility = View.VISIBLE
                
                // Show "Not now" button for frustration flow
                val btnNotNow = view.findViewById<Button>(R.id.btnNotNow)
                btnNotNow?.visibility = View.VISIBLE
                btnNotNow?.setOnClickListener {
                    dismiss()
                }
            }
            FeedbackStep.Rating -> {
                // Show rating buttons (default behavior)
                val ratingContainer = view.findViewById<View>(R.id.ratingContainer)
                ratingContainer?.visibility = View.VISIBLE
                
                // Show subtitle for rating flow
                val tvSubtitle = view.findViewById<TextView>(R.id.tvSubtitle)
                tvSubtitle?.visibility = View.VISIBLE
                
                // Hide feedback section initially
                val feedbackSection = view.findViewById<View>(R.id.feedbackSection)
                feedbackSection?.visibility = View.GONE
                
                // Submit button starts disabled until rating is selected
                val btnSubmit = view.findViewById<Button>(R.id.btnSubmit)
                btnSubmit?.isEnabled = false
                
                // Hide "Not now" button for rating flow
                val btnNotNow = view.findViewById<Button>(R.id.btnNotNow)
                btnNotNow?.visibility = View.GONE
            }
            else -> {
                // Default to rating flow for other steps
                val ratingContainer = view.findViewById<View>(R.id.ratingContainer)
                ratingContainer?.visibility = View.VISIBLE
                
                val tvSubtitle = view.findViewById<TextView>(R.id.tvSubtitle)
                tvSubtitle?.visibility = View.VISIBLE
                
                val feedbackSection = view.findViewById<View>(R.id.feedbackSection)
                feedbackSection?.visibility = View.GONE
                
                val btnSubmit = view.findViewById<Button>(R.id.btnSubmit)
                btnSubmit?.isEnabled = false
                
                val btnNotNow = view.findViewById<Button>(R.id.btnNotNow)
                btnNotNow?.visibility = View.GONE
            }
        }
    }

    private fun setupRatingButtons(view: View) {
        val ratingIds = listOf(
            R.id.btnRating1,
            R.id.btnRating2,
            R.id.btnRating3,
            R.id.btnRating4,
            R.id.btnRating5
        )

        ratingButtons.clear()
        
        ratingIds.forEachIndexed { index: Int, id: Int ->
            val button: ImageButton? = view.findViewById<ImageButton>(id)
            button?.let { btn ->
                ratingButtons.add(btn)
                btn.setOnClickListener {
                    selectRating(index + 1)
                }
            }
        }
        
        // Initialize all buttons to full visibility
        resetRatingButtons()
    }

    private fun selectRating(rating: Int) {
        selectedRating = rating
        
        // Update button states with visual feedback
        ratingButtons.forEachIndexed { index, button ->
            val ratingValue = index + 1
            val isSelected = ratingValue == rating
            
            if (isSelected) {
                // Selected rating: fully visible
                button.alpha = 1.0f
                button.clearColorFilter()
            } else {
                // Unselected ratings: apply semi-transparent white overlay
                button.alpha = 0.3f // This creates the grayed-out effect
            }
        }

        // Show feedback input after rating selection
        view?.findViewById<View>(R.id.feedbackSection)?.visibility = View.VISIBLE
        
        // Enable submit button when rating is selected
        view?.findViewById<Button>(R.id.btnSubmit)?.isEnabled = true
    }
    
    private fun resetRatingButtons() {
        // Reset all buttons to full visibility (initial state)
        ratingButtons.forEach { button ->
            button.alpha = 1.0f
            button.clearColorFilter()
        }
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        onDismissCallback?.invoke()
    }
    
    override fun onResume() {
        super.onResume()
        // Refresh theme when dialog becomes visible (handles theme changes)
        view?.let { applyTheme(it) }
    }
    
    private fun applyTheme(view: View) {
        val theme = com.appero.sdk.Appero.theme
        
        // Apply theme to submit button
        val btnSubmit = view.findViewById<Button>(R.id.btnSubmit)
        btnSubmit?.let { button ->
            if (theme.accentColor != androidx.compose.ui.graphics.Color.Unspecified) {
                button.backgroundTintList = android.content.res.ColorStateList.valueOf(theme.accentColor.toArgb())
            }
            if (theme.buttonTextColor != androidx.compose.ui.graphics.Color.Unspecified) {
                button.setTextColor(theme.buttonTextColor.toArgb())
            }
        }
        
        // Apply theme to close button (same as submit button)
        val btnClose = view.findViewById<Button>(R.id.btnClose)
        btnClose?.let { button ->
            if (theme.accentColor != androidx.compose.ui.graphics.Color.Unspecified) {
                button.backgroundTintList = android.content.res.ColorStateList.valueOf(theme.accentColor.toArgb())
            }
            if (theme.buttonTextColor != androidx.compose.ui.graphics.Color.Unspecified) {
                button.setTextColor(theme.buttonTextColor.toArgb())
            }
        }
        
        // Apply theme to loading progress bar (same as CTA button)
        val loadingProgressBar = view.findViewById<com.google.android.material.progressindicator.CircularProgressIndicator>(R.id.loadingContainer)
        loadingProgressBar?.let { progressBar ->
            if (theme.accentColor != androidx.compose.ui.graphics.Color.Unspecified) {
                progressBar.setIndicatorColor(theme.accentColor.toArgb())
            }
        }
    }

    // Internal methods for SDK integration
    internal fun setAnalyticsListener(listener: ApperoAnalyticsListener?) {
        this.analyticsListener = listener
    }

    internal fun setConfig(config: FeedbackPromptConfig) {
        this.config = config
    }
    
    internal fun setInitialStep(step: FeedbackStep) {
        this.initialStep = step
    }

    internal fun setOnSubmitCallback(callback: (rating: Int, feedback: String) -> Unit) {
        this.onSubmitCallback = callback
    }
    
    internal fun setFeedbackSubmissionCallback(callback: (success: Boolean, message: String?) -> Unit) {
        // This callback will be called by the SDK after API submission
        // It should be set by the SDK to handle the response
        this.onFeedbackSubmissionCallback = callback
    }
    
    internal fun showThankYouStep(apiMessage: String? = null) {
        // Public method to show thank you step from SDK
        view?.let { showThankYouStep(it, apiMessage) }
    }
    
    internal fun handleFeedbackSubmissionResult(success: Boolean, message: String?) {
        view?.let { view ->
            if (success) {
                // Show thank you step on success with API message
                showThankYouStep(view, message)
            } else {
                // Show error and reset to input state
                hideLoadingState(view)
                // You could show a toast or error message here
                // For now, we'll just reset the button state
            }
        }
        
        // Call the external callback if set
        onFeedbackSubmissionCallback?.invoke(success, message)
    }

    internal fun setOnDismissCallback(callback: () -> Unit) {
        this.onDismissCallback = callback
    }

    internal fun setFlowConfig(flowConfig: com.appero.sdk.ui.config.FeedbackFlowConfig) {
        this.flowConfig = flowConfig
    }

    internal fun setReviewPromptThreshold(threshold: Int) {
        this.reviewPromptThreshold = threshold
    }

    internal fun setOnRequestReview(callback: () -> Unit) {
        this.onRequestReview = callback
    }
} 