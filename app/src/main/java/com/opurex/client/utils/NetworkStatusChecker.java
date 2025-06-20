
package com.opurex.client.utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.Looper;

/**
 * Network status checker with user-friendly feedback
 */
public class NetworkStatusChecker {
    
    private Context context;
    private Handler mainHandler;
    
    public interface NetworkStatusListener {
        void onNetworkAvailable();
        void onNetworkUnavailable();
        void onNetworkWeak();
    }
    
    public NetworkStatusChecker(Context context) {
        this.context = context;
        this.mainHandler = new Handler(Looper.getMainLooper());
    }
    
    /**
     * Check if network is available
     */
    public boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = 
            (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        
        if (connectivityManager != null) {
            NetworkInfo activeNetwork = connectivityManager.getActiveNetworkInfo();
            return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
        }
        return false;
    }
    
    /**
     * Check WiFi signal strength
     */
    public boolean isWifiSignalStrong() {
        WifiManager wifiManager = (WifiManager) context.getApplicationContext()
            .getSystemService(Context.WIFI_SERVICE);
        
        if (wifiManager != null) {
            int rssi = wifiManager.getConnectionInfo().getRssi();
            int level = WifiManager.calculateSignalLevel(rssi, 5);
            return level >= 3; // Good signal (3/5 or better)
        }
        return false;
    }
    
    /**
     * Provide network feedback to user
     */
    public void checkNetworkAndNotify() {
        if (!isNetworkAvailable()) {
            ErrorHandler.handleError(context, ErrorHandler.ErrorType.NETWORK_CONNECTION, 
                "No network connection available");
        } else if (!isWifiSignalStrong()) {
            // Show warning about weak signal but don't treat as error
            mainHandler.post(() -> 
                ErrorHandler.showProgress(context, "Weak network signal detected"));
        }
    }
    
    /**
     * Monitor network status changes
     */
    public void startNetworkMonitoring(NetworkStatusListener listener) {
        // Simple periodic check - in production you'd use ConnectivityManager.NetworkCallback
        Handler handler = new Handler(Looper.getMainLooper());
        Runnable networkCheck = new Runnable() {
            @Override
            public void run() {
                if (isNetworkAvailable()) {
                    if (isWifiSignalStrong()) {
                        listener.onNetworkAvailable();
                    } else {
                        listener.onNetworkWeak();
                    }
                } else {
                    listener.onNetworkUnavailable();
                }
                handler.postDelayed(this, 10000); // Check every 10 seconds
            }
        };
        handler.post(networkCheck);
    }
}
