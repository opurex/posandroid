
package com.opurex.client.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Build;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.opurex.client.R;
import com.opurex.client.Transaction;
import com.opurex.client.data.Data;
import com.opurex.client.models.Product;

import java.util.ArrayList;
import java.util.List;

public class InventoryManager {
    private static final String CHANNEL_ID = "stock_alerts";
    private static final String PREFS_NAME = "inventory_prefs";
    private static final String KEY_LOW_STOCK_THRESHOLD = "low_stock_threshold";
    private static final int DEFAULT_LOW_STOCK_THRESHOLD = 10;
    
    private Context context;
    private SharedPreferences prefs;
    private NotificationManagerCompat notificationManager;
    
    public InventoryManager(Context context) {
        this.context = context;
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        this.notificationManager = NotificationManagerCompat.from(context);
        createNotificationChannel();
    }
    
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = context.getString(R.string.stock_alert_channel_name);
            String description = context.getString(R.string.stock_alert_channel_description);
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            
            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }
    
    public void checkLowStock() {
        List<Product> lowStockProducts = getLowStockProducts();
        if (!lowStockProducts.isEmpty()) {
            showLowStockNotification(lowStockProducts);
        }
    }
    
    private List<Product> getLowStockProducts() {
        List<Product> lowStockProducts = new ArrayList<>();
        int threshold = getLowStockThreshold();
        
        try {
            List<Product> allProducts = Data.Product.list(context);
            for (Product product : allProducts) {
                if (product.getStock() != null && product.getStock() <= threshold && product.getStock() > 0) {
                    lowStockProducts.add(product);
                }
            }
        } catch (Exception e) {
            ErrorHandler.handleError(context, e, "Failed to check inventory levels");
        }
        
        return lowStockProducts;
    }
    
    private void showLowStockNotification(List<Product> lowStockProducts) {
        String title = context.getString(R.string.low_stock_alert_title);
        String message = context.getResources().getQuantityString(
            R.plurals.low_stock_alert_message, 
            lowStockProducts.size(), 
            lowStockProducts.size()
        );
        
        Intent intent = new Intent(context, Transaction.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
            context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_menu_update)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true);
        
        // Add action for detailed view
        if (lowStockProducts.size() <= 3) {
            StringBuilder detailText = new StringBuilder();
            for (Product product : lowStockProducts) {
                detailText.append(product.getLabel())
                    .append(" (").append(product.getStock()).append(" left)\n");
            }
            builder.setStyle(new NotificationCompat.BigTextStyle()
                .bigText(detailText.toString().trim()));
        }
        
        notificationManager.notify(1001, builder.build());
    }
    
    public int getLowStockThreshold() {
        return prefs.getInt(KEY_LOW_STOCK_THRESHOLD, DEFAULT_LOW_STOCK_THRESHOLD);
    }
    
    public void setLowStockThreshold(int threshold) {
        prefs.edit().putInt(KEY_LOW_STOCK_THRESHOLD, threshold).apply();
    }
    
    public boolean isProductLowStock(Product product) {
        return product.getStock() != null && 
               product.getStock() <= getLowStockThreshold() && 
               product.getStock() > 0;
    }
}
