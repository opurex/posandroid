
package com.opurex.client.activities;

import android.os.Bundle;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.SeekBar;
import android.widget.Button;
import android.widget.Toast;

import com.opurex.client.R;
import com.opurex.client.utils.ThemeManager;
import com.opurex.client.utils.InventoryManager;
import com.opurex.client.utils.ImageCache;

import java.text.DecimalFormat;

public class SettingsActivity extends POSConnectedTrackedActivity {
    
    private ThemeManager themeManager;
    private InventoryManager inventoryManager;
    private ImageCache imageCache;
    
    private Switch darkModeSwitch;
    private Switch largeTextSwitch;
    private SeekBar stockThresholdSeekBar;
    private TextView stockThresholdValue;
    private TextView cacheSize;
    private Button clearCacheButton;
    private Button checkInventoryButton;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_activity);
        
        // Initialize managers
        themeManager = ThemeManager.getInstance(this);
        inventoryManager = new InventoryManager(this);
        imageCache = ImageCache.getInstance(this);
        
        initializeViews();
        setupListeners();
        updateUI();
    }
    
    private void initializeViews() {
        darkModeSwitch = findViewById(R.id.dark_mode_switch);
        largeTextSwitch = findViewById(R.id.large_text_switch);
        stockThresholdSeekBar = findViewById(R.id.stock_threshold_seekbar);
        stockThresholdValue = findViewById(R.id.stock_threshold_value);
        cacheSize = findViewById(R.id.cache_size_text);
        clearCacheButton = findViewById(R.id.clear_cache_button);
        checkInventoryButton = findViewById(R.id.check_inventory_button);
    }
    
    private void setupListeners() {
        darkModeSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            themeManager.setDarkMode(isChecked);
            recreate(); // Restart activity to apply theme
        });
        
        largeTextSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            themeManager.setLargeText(isChecked);
        });
        
        stockThresholdSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int threshold = progress + 1; // Min value of 1
                stockThresholdValue.setText(String.valueOf(threshold));
                if (fromUser) {
                    inventoryManager.setLowStockThreshold(threshold);
                }
            }
            
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        
        clearCacheButton.setOnClickListener(v -> {
            imageCache.clearCache();
            updateCacheSize();
            Toast.makeText(this, R.string.image_cache_cleared, Toast.LENGTH_SHORT).show();
        });
        
        checkInventoryButton.setOnClickListener(v -> {
            inventoryManager.checkLowStock();
            Toast.makeText(this, "Inventory check completed", Toast.LENGTH_SHORT).show();
        });
    }
    
    private void updateUI() {
        darkModeSwitch.setChecked(themeManager.isDarkModeEnabled());
        largeTextSwitch.setChecked(themeManager.isLargeTextEnabled());
        
        int threshold = inventoryManager.getLowStockThreshold();
        stockThresholdSeekBar.setProgress(threshold - 1);
        stockThresholdValue.setText(String.valueOf(threshold));
        
        updateCacheSize();
    }
    
    private void updateCacheSize() {
        long size = imageCache.getCacheSize();
        String sizeText = formatFileSize(size);
        cacheSize.setText(getString(R.string.cache_size, sizeText));
    }
    
    private String formatFileSize(long size) {
        DecimalFormat df = new DecimalFormat("#.##");
        if (size < 1024) {
            return size + " B";
        } else if (size < 1024 * 1024) {
            return df.format(size / 1024.0) + " KB";
        } else {
            return df.format(size / (1024.0 * 1024.0)) + " MB";
        }
    }
    
    @Override
    public void onDeviceManagerEvent(POSDeviceManager manager, DeviceManagerEvent event) {
        // Handle device manager events if needed
    }
}
