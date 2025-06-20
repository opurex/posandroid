
package com.opurex.client.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.opurex.client.BiometricAuthManager;
import com.opurex.client.R;

public class BiometricSetupActivity extends POSConnectedTrackedActivity {
    
    private BiometricAuthManager biometricManager;
    private Switch biometricSwitch;
    private TextView statusText;
    private Button testButton;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.biometric_setup);
        
        biometricManager = new BiometricAuthManager(this);
        
        initializeViews();
        updateUI();
    }
    
    private void initializeViews() {
        biometricSwitch = findViewById(R.id.biometric_switch);
        statusText = findViewById(R.id.biometric_status);
        testButton = findViewById(R.id.test_biometric_button);
        
        biometricSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked && !biometricManager.isBiometricAvailable()) {
                biometricSwitch.setChecked(false);
                Toast.makeText(this, R.string.biometric_not_available, Toast.LENGTH_LONG).show();
                return;
            }
            
            biometricManager.setBiometricEnabled(isChecked);
            updateUI();
        });
        
        testButton.setOnClickListener(v -> testBiometricAuthentication());
    }
    
    private void updateUI() {
        boolean isAvailable = biometricManager.isBiometricAvailable();
        boolean isEnabled = biometricManager.isBiometricEnabled();
        
        biometricSwitch.setEnabled(isAvailable);
        biometricSwitch.setChecked(isEnabled);
        testButton.setEnabled(isEnabled);
        
        statusText.setText(biometricManager.getBiometricStatusMessage());
    }
    
    private void testBiometricAuthentication() {
        biometricManager.authenticateForManagerFunctions(new BiometricAuthManager.AuthenticationCallback() {
            @Override
            public void onAuthenticationSucceeded() {
                Toast.makeText(BiometricSetupActivity.this, 
                    R.string.biometric_test_success, Toast.LENGTH_SHORT).show();
            }
            
            @Override
            public void onAuthenticationFailed() {
                Toast.makeText(BiometricSetupActivity.this, 
                    R.string.biometric_test_failed, Toast.LENGTH_SHORT).show();
            }
            
            @Override
            public void onAuthenticationError(String error) {
                Toast.makeText(BiometricSetupActivity.this, 
                    getString(R.string.biometric_error, error), Toast.LENGTH_LONG).show();
            }
        });
    }
}
