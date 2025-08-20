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
    
    // Rating state
    private var selectedRating: Int = 0
    private val ratingButtons = mutableListOf<ImageButton>()

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

        // Setup submit button with theming
        val btnSubmit = view.findViewById<Button>(R.id.btnSubmit)
        btnSubmit?.text = config.submitText
        btnSubmit?.setOnClickListener {
            if (selectedRating > 0) {
                val feedbackText = etFeedback?.text?.toString() ?: ""
                analyticsListener?.onRatingSelected(selectedRating)
                onSubmitCallback?.invoke(selectedRating, feedbackText)
                dismiss()
            }
        }
        
        // Apply Appero theme to submit button
        applyTheme(view)


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
        val btnSubmit = view.findViewById<Button>(R.id.btnSubmit)
        btnSubmit?.let { button ->
            val theme = com.appero.sdk.Appero.theme
            if (theme.accentColor != androidx.compose.ui.graphics.Color.Unspecified) {
                button.backgroundTintList = android.content.res.ColorStateList.valueOf(theme.accentColor.toArgb())
            }
            if (theme.buttonTextColor != androidx.compose.ui.graphics.Color.Unspecified) {
                button.setTextColor(theme.buttonTextColor.toArgb())
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

    internal fun setOnSubmitCallback(callback: (rating: Int, feedback: String) -> Unit) {
        this.onSubmitCallback = callback
    }

    internal fun setOnDismissCallback(callback: () -> Unit) {
        this.onDismissCallback = callback
    }
} 