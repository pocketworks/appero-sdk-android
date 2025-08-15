# Keyboard Handling in Appero SDK

## Overview

The Appero SDK uses a ModalBottomSheet with text input fields for collecting user feedback. Proper keyboard handling is essential for a good user experience, especially on Android devices.

## Required Setup

### 1. AndroidManifest.xml Configuration

Your app's `AndroidManifest.xml` must include the proper `windowSoftInputMode` setting for the activity that hosts the Appero SDK:

```xml
<activity
    android:name=".YourActivity"
    android:windowSoftInputMode="adjustResize">
    <!-- ... other activity attributes ... -->
</activity>
```

**Important**: The `adjustResize` mode is required for proper keyboard handling with ModalBottomSheet.

### 2. Alternative: adjustPan Mode

If `adjustResize` doesn't work well in your specific app architecture, you can use `adjustPan`:

```xml
<activity
    android:name=".YourActivity"
    android:windowSoftInputMode="adjustPan">
    <!-- ... other activity attributes ... -->
</activity>
```

## How It Works

The Appero SDK implements several keyboard handling strategies specifically designed for ModalBottomSheet:

1. **WindowInsets Integration**: Uses `WindowInsets.ime` to properly handle keyboard appearance
2. **Dynamic Height Adjustment**: ModalBottomSheet height adjusts from 73% to 98% when keyboard is visible
3. **Conditional Bottom Padding**: Adds 32dp bottom padding when keyboard is visible
4. **Scrollable Content**: Content is scrollable to ensure text input remains visible
5. **IME State Detection**: Uses ViewTreeObserver to detect keyboard state changes

## ModalBottomSheet Specific Handling

ModalBottomSheet has unique keyboard handling requirements:

- **Height Expansion**: Sheet expands to 98% of screen height when keyboard appears
- **Bottom Padding**: Additional padding ensures text input is not hidden
- **Scroll Behavior**: Content remains scrollable to access all elements
- **Window Insets**: Proper integration with system window insets

## Common Issues and Solutions

### Issue: Text input hidden behind keyboard

**Solution**: Ensure your activity has `android:windowSoftInputMode="adjustResize"` in the manifest.

### Issue: ModalBottomSheet doesn't resize properly

**Solution**: The SDK automatically handles this with dynamic height adjustment (73% → 98%).

### Issue: Keyboard appears but content doesn't scroll

**Solution**: The SDK includes scrollable content. If issues persist, check for conflicting scroll containers in your app.

### Issue: Text input still not visible enough

**Solution**: The SDK adds 32dp bottom padding when keyboard is visible. If you need more space, ensure your app's theme doesn't override window insets.

## Testing

To test keyboard handling:

1. Launch your app with the Appero SDK
2. Trigger the feedback prompt
3. Tap on the text input field
4. Verify the keyboard appears and the text input remains visible
5. Test on different screen sizes and orientations
6. Verify the ModalBottomSheet expands properly

## Best Practices

1. **Always use `adjustResize`** in your manifest for activities hosting the SDK
2. **Test on multiple devices** with different screen sizes
3. **Test in different orientations** (portrait and landscape)
4. **Avoid custom window insets handling** that might conflict with the SDK
5. **Don't override ModalBottomSheet behavior** in your app

## Troubleshooting

If you're still experiencing issues:

1. Check that your activity's `windowSoftInputMode` is set correctly
2. Ensure no other UI components are interfering with window insets
3. Test on a clean activity without other complex layouts
4. Verify you're using the latest version of the Appero SDK
5. Check if your app's theme or custom components override window insets

## Technical Details

The SDK uses the following Compose components for keyboard handling:

- `WindowInsets.ime` for keyboard insets
- `ModalBottomSheet` with dynamic height adjustment (73% → 98%)
- `verticalScroll` for scrollable content
- `ViewTreeObserver.OnPreDrawListener` for IME state detection
- Conditional bottom padding (32dp when keyboard visible)

This approach is specifically designed for ModalBottomSheet and follows Android's recommended patterns for keyboard handling in modern apps.

## Alternative Approaches

If the standard approach doesn't work in your specific app:

1. **Try `adjustPan`** instead of `adjustResize` in your manifest
2. **Check for conflicting themes** that might override window insets
3. **Ensure no custom window insets handling** in your app
4. **Test with a minimal activity** to isolate the issue 