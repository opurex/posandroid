
package com.opurex.client.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import androidx.appcompat.app.AppCompatDelegate;
import com.opurex.client.R;

public class ThemeManager {
    private static final String PREFS_NAME = "opurex_theme_prefs";
    private static final String KEY_DARK_MODE = "dark_mode_enabled";
    private static final String KEY_ACCESSIBILITY_TEXT = "large_text_enabled";
    
    private static ThemeManager instance;
    private SharedPreferences prefs;
    private Context context;
    
    private ThemeManager(Context context) {
        this.context = context.getApplicationContext();
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }
    
    public static ThemeManager getInstance(Context context) {
        if (instance == null) {
            instance = new ThemeManager(context);
        }
        return instance;
    }
    
    public boolean isDarkModeEnabled() {
        return prefs.getBoolean(KEY_DARK_MODE, isSystemDarkMode());
    }
    
    public void setDarkMode(boolean enabled) {
        prefs.edit().putBoolean(KEY_DARK_MODE, enabled).apply();
        applyTheme();
    }
    
    public boolean isLargeTextEnabled() {
        return prefs.getBoolean(KEY_ACCESSIBILITY_TEXT, false);
    }
    
    public void setLargeText(boolean enabled) {
        prefs.edit().putBoolean(KEY_ACCESSIBILITY_TEXT, enabled).apply();
    }
    
    public void applyTheme() {
        if (isDarkModeEnabled()) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }
    }
    
    private boolean isSystemDarkMode() {
        int nightModeFlags = context.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        return nightModeFlags == Configuration.UI_MODE_NIGHT_YES;
    }
    
    public int getCurrentTheme() {
        return isDarkModeEnabled() ? R.style.POSTechTheme_Dark : R.style.POSTechTheme;
    }
}
