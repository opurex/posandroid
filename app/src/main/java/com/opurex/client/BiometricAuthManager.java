
package com.opurex.client;

import android.content.Context;
import android.content.SharedPreferences;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import java.util.concurrent.Executor;

public class BiometricAuthManager {
    
    private static final String PREFS_NAME = "BiometricPrefs";
    private static final String KEY_BIOMETRIC_ENABLED = "biometric_enabled";
    
    private final FragmentActivity activity;
    private final SharedPreferences prefs;
    private BiometricPrompt biometricPrompt;
    private BiometricPrompt.PromptInfo promptInfo;
    
    public interface AuthenticationCallback {
        void onAuthenticationSucceeded();
        void onAuthenticationFailed();
        void onAuthenticationError(String error);
    }
    
    public BiometricAuthManager(FragmentActivity activity) {
        this.activity = activity;
        this.prefs = activity.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        setupBiometricPrompt();
    }
    
    private void setupBiometricPrompt() {
        Executor executor = ContextCompat.getMainExecutor(activity);
        
        biometricPrompt = new BiometricPrompt((FragmentActivity) activity, executor, 
            new BiometricPrompt.AuthenticationCallback() {
                @Override
                public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                    super.onAuthenticationError(errorCode, errString);
                    if (currentCallback != null) {
                        currentCallback.onAuthenticationError(errString.toString());
                    }
                }
                
                @Override
                public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                    super.onAuthenticationSucceeded(result);
                    if (currentCallback != null) {
                        currentCallback.onAuthenticationSucceeded();
                    }
                }
                
                @Override
                public void onAuthenticationFailed() {
                    super.onAuthenticationFailed();
                    if (currentCallback != null) {
                        currentCallback.onAuthenticationFailed();
                    }
                }
            });
    }
    
    private AuthenticationCallback currentCallback;
    
    public boolean isBiometricAvailable() {
        BiometricManager biometricManager = BiometricManager.from(activity);
        switch (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK)) {
            case BiometricManager.BIOMETRIC_SUCCESS:
                return true;
            case BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE:
            case BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE:
            case BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED:
            default:
                return false;
        }
    }
    
    public boolean isBiometricEnabled() {
        return prefs.getBoolean(KEY_BIOMETRIC_ENABLED, false) && isBiometricAvailable();
    }
    
    public void setBiometricEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_BIOMETRIC_ENABLED, enabled).apply();
    }
    
    public void authenticateForManagerFunctions(AuthenticationCallback callback) {
        authenticate(
            activity.getString(R.string.biometric_title_manager),
            activity.getString(R.string.biometric_subtitle_manager),
            callback
        );
    }
    
    public void authenticateForSensitiveOperation(String operation, AuthenticationCallback callback) {
        authenticate(
            activity.getString(R.string.biometric_title_sensitive),
            activity.getString(R.string.biometric_subtitle_sensitive, operation),
            callback
        );
    }
    
    public void authenticateForAppAccess(AuthenticationCallback callback) {
        authenticate(
            activity.getString(R.string.biometric_title_access),
            activity.getString(R.string.biometric_subtitle_access),
            callback
        );
    }
    
    private void authenticate(String title, String subtitle, AuthenticationCallback callback) {
        if (!isBiometricEnabled()) {
            callback.onAuthenticationError(activity.getString(R.string.biometric_not_enabled));
            return;
        }
        
        this.currentCallback = callback;
        
        promptInfo = new BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setNegativeButtonText(activity.getString(R.string.cancel))
            .build();
        
        biometricPrompt.authenticate(promptInfo);
    }
    
    public String getBiometricStatusMessage() {
        BiometricManager biometricManager = BiometricManager.from(activity);
        switch (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK)) {
            case BiometricManager.BIOMETRIC_SUCCESS:
                return activity.getString(R.string.biometric_available);
            case BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE:
                return activity.getString(R.string.biometric_no_hardware);
            case BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE:
                return activity.getString(R.string.biometric_hw_unavailable);
            case BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED:
                return activity.getString(R.string.biometric_none_enrolled);
            default:
                return activity.getString(R.string.biometric_unknown_error);
        }
    }
    
    // Utility method to check if sensitive operation should require authentication
    public boolean shouldRequireAuthForOperation(String operation) {
        // Define which operations require biometric auth
        String[] sensitiveOperations = {
            "cash_drawer_open",
            "price_override",
            "discount_apply",
            "refund_process",
            "inventory_adjust",
            "reports_access",
            "settings_change"
        };
        
        for (String sensitiveOp : sensitiveOperations) {
            if (sensitiveOp.equals(operation)) {
                return isBiometricEnabled();
            }
        }
        return false;
    }
}
