
package com.opurex.client.utils;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import com.opurex.client.R;

/**
 * Progress feedback utility for better user experience during operations
 */
public class ProgressFeedback {
    
    private ProgressDialog progressDialog;
    private Context context;
    private Handler mainHandler;
    
    public ProgressFeedback(Context context) {
        this.context = context;
        this.mainHandler = new Handler(Looper.getMainLooper());
    }
    
    /**
     * Show progress dialog for long operations
     */
    public void showProgress(String title, String message) {
        mainHandler.post(() -> {
            if (progressDialog != null && progressDialog.isShowing()) {
                progressDialog.dismiss();
            }
            
            progressDialog = new ProgressDialog(context);
            progressDialog.setTitle(title);
            progressDialog.setMessage(message);
            progressDialog.setCancelable(false);
            progressDialog.setIndeterminate(true);
            progressDialog.show();
        });
    }
    
    /**
     * Update progress message
     */
    public void updateProgress(String message) {
        mainHandler.post(() -> {
            if (progressDialog != null && progressDialog.isShowing()) {
                progressDialog.setMessage(message);
            }
        });
    }
    
    /**
     * Hide progress dialog
     */
    public void hideProgress() {
        mainHandler.post(() -> {
            if (progressDialog != null && progressDialog.isShowing()) {
                progressDialog.dismiss();
                progressDialog = null;
            }
        });
    }
    
    /**
     * Show progress with timeout
     */
    public void showProgressWithTimeout(String title, String message, int timeoutMs) {
        showProgress(title, message);
        
        mainHandler.postDelayed(() -> {
            hideProgress();
            ErrorHandler.handleError(context, ErrorHandler.ErrorType.UNKNOWN_ERROR, 
                "Operation timed out");
        }, timeoutMs);
    }
    
    /**
     * Show inline progress indicator
     */
    public void showInlineProgress(View container, String message) {
        mainHandler.post(() -> {
            ProgressBar progressBar = container.findViewById(R.id.progress_bar);
            TextView progressText = container.findViewById(R.id.progress_text);
            
            if (progressBar != null) {
                progressBar.setVisibility(View.VISIBLE);
            }
            if (progressText != null) {
                progressText.setText(message);
                progressText.setVisibility(View.VISIBLE);
            }
        });
    }
    
    /**
     * Hide inline progress indicator
     */
    public void hideInlineProgress(View container) {
        mainHandler.post(() -> {
            ProgressBar progressBar = container.findViewById(R.id.progress_bar);
            TextView progressText = container.findViewById(R.id.progress_text);
            
            if (progressBar != null) {
                progressBar.setVisibility(View.GONE);
            }
            if (progressText != null) {
                progressText.setVisibility(View.GONE);
            }
        });
    }
}
