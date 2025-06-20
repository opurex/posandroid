
package com.opurex.client.utils;

import android.content.Context;
import android.widget.Toast;
import android.util.Log;
import com.opurex.client.R;

/**
 * Enhanced error handling utility for better user experience
 * Provides user-friendly error messages and consistent error reporting
 */
public class ErrorHandler {
    
    private static final String TAG = "ErrorHandler";
    
    public enum ErrorType {
        NETWORK_CONNECTION,
        AUTHENTICATION,
        PAYMENT_PROCESSING,
        PRINTER_CONNECTION,
        DATABASE_ERROR,
        VALIDATION_ERROR,
        UNKNOWN_ERROR
    }
    
    /**
     * Display user-friendly error message based on error type
     */
    public static void handleError(Context context, ErrorType errorType, String technicalMessage) {
        String userMessage = getUserFriendlyMessage(context, errorType);
        
        // Log technical details for debugging
        Log.e(TAG, "Error Type: " + errorType + ", Technical: " + technicalMessage);
        
        // Show user-friendly message
        Toast.makeText(context, userMessage, Toast.LENGTH_LONG).show();
    }
    
    /**
     * Handle network errors specifically
     */
    public static void handleNetworkError(Context context, Exception exception) {
        String message;
        if (exception.getMessage() != null && exception.getMessage().contains("timeout")) {
            message = context.getString(R.string.error_network_timeout);
        } else if (exception.getMessage() != null && exception.getMessage().contains("host")) {
            message = context.getString(R.string.error_network_unreachable);
        } else {
            message = context.getString(R.string.error_network_general);
        }
        
        Log.e(TAG, "Network Error: " + exception.getMessage(), exception);
        Toast.makeText(context, message, Toast.LENGTH_LONG).show();
    }
    
    /**
     * Handle validation errors with specific field feedback
     */
    public static void handleValidationError(Context context, String fieldName, String issue) {
        String message = context.getString(R.string.error_validation_field, fieldName, issue);
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
        Log.w(TAG, "Validation Error - Field: " + fieldName + ", Issue: " + issue);
    }
    
    /**
     * Show success feedback to user
     */
    public static void showSuccess(Context context, String action) {
        String message = context.getString(R.string.success_action_completed, action);
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
        Log.i(TAG, "Success: " + action);
    }
    
    /**
     * Show loading state feedback
     */
    public static void showProgress(Context context, String action) {
        String message = context.getString(R.string.progress_action_running, action);
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
        Log.i(TAG, "Progress: " + action);
    }
    
    private static String getUserFriendlyMessage(Context context, ErrorType errorType) {
        switch (errorType) {
            case NETWORK_CONNECTION:
                return context.getString(R.string.error_network_connection);
            case AUTHENTICATION:
                return context.getString(R.string.error_authentication);
            case PAYMENT_PROCESSING:
                return context.getString(R.string.error_payment_processing);
            case PRINTER_CONNECTION:
                return context.getString(R.string.error_printer_connection);
            case DATABASE_ERROR:
                return context.getString(R.string.error_database);
            case VALIDATION_ERROR:
                return context.getString(R.string.error_validation);
            default:
                return context.getString(R.string.error_unknown);
        }
    }
    
    /**
     * Handle specific POS device errors
     */
    public static void handleDeviceError(Context context, String deviceName, String errorMessage) {
        String message = context.getString(R.string.error_device_specific, deviceName, errorMessage);
        Toast.makeText(context, message, Toast.LENGTH_LONG).show();
        Log.e(TAG, "Device Error - " + deviceName + ": " + errorMessage);
    }
}
