package com.opurex.client.utils;

import android.Manifest;
import android.app.Activity;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import com.opurex.client.R;
import com.opurex.client.Transaction;
import com.opurex.client.data.Data;
import com.opurex.client.models.Catalog;
import com.opurex.client.models.Product;
import com.opurex.client.models.Stock;

import org.json.JSONException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class InventoryManager {
    private static final String TAG = "InventoryManager";
    private static final String CHANNEL_ID = "stock_alerts";
    private static final String PREFS_NAME = "inventory_prefs";
    private static final String KEY_LOW_STOCK_THRESHOLD = "low_stock_threshold";
    private static final int DEFAULT_LOW_STOCK_THRESHOLD = 10;

    private final Context context;
    private final SharedPreferences prefs;
    private final NotificationManagerCompat notificationManager;

    public InventoryManager(Context context) {
        this.context = context;
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        this.notificationManager = NotificationManagerCompat.from(context);
        createNotificationChannel();
        requestNotificationPermission();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = context.getString(R.string.stock_alert_channel_name);
            String description = context.getString(R.string.stock_alert_channel_description);
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            NotificationManager manager = context.getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                if (context instanceof Activity) {
                    ActivityCompat.requestPermissions(
                            (Activity) context,
                            new String[]{Manifest.permission.POST_NOTIFICATIONS},
                            1000
                    );
                }
            }
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
            Catalog catalog = Data.Catalog.catalog(context);
            for (Product product : catalog.getAllProducts()) {
                Stock stock = Data.Stock.stocks.get(product.getId());
                if (stock != null && stock.getQuantity() != null) {
                    double quantity = stock.getQuantity(); // Keep as double
                    if (quantity <= threshold && quantity > 0) {
                        lowStockProducts.add(product);
                    }
                }
            }
        } catch (Exception e) {
            ErrorHandler.handleError(context, ErrorHandler.ErrorType.DATABASE_ERROR, "Failed to check inventory levels");
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
                context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_menu_update)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        if (lowStockProducts.size() <= 3) {
            StringBuilder detailText = new StringBuilder();
            for (Product product : lowStockProducts) {
                Stock stock = Data.Stock.stocks.get(product.getId());
                double quantity = (stock != null && stock.getQuantity() != null) ? stock.getQuantity() : 0.0;
                int displayQuantity = (int) quantity; // convert for display as int
                detailText.append(product.getLabel())
                        .append(" (").append(displayQuantity).append(" left)\n");
            }
            builder.setStyle(new NotificationCompat.BigTextStyle()
                    .bigText(detailText.toString().trim()));
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                        == PackageManager.PERMISSION_GRANTED) {
            notificationManager.notify(1001, builder.build());
        } else {
            Log.w(TAG, "Notification permission not granted.");
        }
    }

    public int getLowStockThreshold() {
        return prefs.getInt(KEY_LOW_STOCK_THRESHOLD, DEFAULT_LOW_STOCK_THRESHOLD);
    }

    public void setLowStockThreshold(int threshold) {
        prefs.edit().putInt(KEY_LOW_STOCK_THRESHOLD, threshold).apply();
    }

    public boolean isProductLowStock(Product product) {
        Stock stock = Data.Stock.stocks.get(product.getId());
        double quantity = (stock != null && stock.getQuantity() != null) ? stock.getQuantity() : 0.0;
        return quantity <= getLowStockThreshold() && quantity > 0;
    }
}




//package com.opurex.client.utils;
//
//import android.Manifest;
//import android.app.Activity;
//import android.app.NotificationChannel;
//import android.app.NotificationManager;
//import android.app.PendingIntent;
//import android.content.Context;
//import android.content.Intent;
//import android.content.SharedPreferences;
//import android.content.pm.PackageManager;
//import android.os.Build;
//import android.util.Log;
//
//import androidx.core.app.ActivityCompat;
//import androidx.core.app.NotificationCompat;
//import androidx.core.app.NotificationManagerCompat;
//import androidx.core.content.ContextCompat;
//
//import com.opurex.client.R;
//import com.opurex.client.Transaction;
//import com.opurex.client.data.Data;
//import com.opurex.client.models.Catalog;
//import com.opurex.client.models.Product;
//import com.opurex.client.models.Stock;
//import com.opurex.client.utils.exception.DataCorruptedException;
//
//import org.json.JSONException;
//
//import java.io.IOException;
//import java.util.ArrayList;
//import java.util.List;
//
//public class InventoryManager {
//    private static final String TAG = "InventoryManager";
//    private static final String CHANNEL_ID = "stock_alerts";
//    private static final String PREFS_NAME = "inventory_prefs";
//    private static final String KEY_LOW_STOCK_THRESHOLD = "low_stock_threshold";
//    private static final int DEFAULT_LOW_STOCK_THRESHOLD = 10;
//
//    private Context context;
//    private SharedPreferences prefs;
//    private NotificationManagerCompat notificationManager;
//
//    public InventoryManager(Context context) {
//        this.context = context;
//        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
//        this.notificationManager = NotificationManagerCompat.from(context);
//        createNotificationChannel();
//        requestNotificationPermission();
//    }
//
//    private void createNotificationChannel() {
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//            CharSequence name = context.getString(R.string.stock_alert_channel_name);
//            String description = context.getString(R.string.stock_alert_channel_description);
//            int importance = NotificationManager.IMPORTANCE_DEFAULT;
//            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
//            channel.setDescription(description);
//
//            NotificationManager manager = context.getSystemService(NotificationManager.class);
//            if (manager != null) {
//                manager.createNotificationChannel(channel);
//            }
//        }
//    }
//
//    private void requestNotificationPermission() {
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
//            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
//                    != PackageManager.PERMISSION_GRANTED) {
//                if (context instanceof Activity) {
//                    ActivityCompat.requestPermissions(
//                            (Activity) context,
//                            new String[]{Manifest.permission.POST_NOTIFICATIONS},
//                            1000
//                    );
//                }
//            }
//        }
//    }
//
//    public void checkLowStock() {
//        List<Product> lowStockProducts = getLowStockProducts();
//        if (!lowStockProducts.isEmpty()) {
//            showLowStockNotification(lowStockProducts);
//        }
//    }
//
//    private List<Product> getLowStockProducts() {
//        List<Product> lowStockProducts = new ArrayList<>();
//        int threshold = getLowStockThreshold();
//
//        try {
//            Catalog catalog = Data.Catalog.catalog(context);
//            for (Product product : catalog.getAllProducts()) {
//                Stock stock = Data.Stock.stocks.get(product.getId());
//                if (stock != null && stock.getQuantity() != null) {
//                    double quantity = stock.getQuantity();
//                    if (quantity <= threshold && quantity > 0) {
//                        lowStockProducts.add(product);
//                    }
//                }
//            }
//        } catch (Exception e) {
//            ErrorHandler.handleError(context, ErrorHandler.ErrorType.DATABASE_ERROR,
//                    "Failed to check inventory levels");
//        }
//
//        return lowStockProducts;
//    }
//
//    private void showLowStockNotification(List<Product> lowStockProducts) {
//        String title = context.getString(R.string.low_stock_alert_title);
//        String message = context.getResources().getQuantityString(
//                R.plurals.low_stock_alert_message,
//                lowStockProducts.size(),
//                lowStockProducts.size()
//        );
//
//        Intent intent = new Intent(context, Transaction.class);
//        PendingIntent pendingIntent = PendingIntent.getActivity(
//                context, 0, intent,
//                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
//        );
//
//        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
//                .setSmallIcon(R.drawable.ic_menu_update)
//                .setContentTitle(title)
//                .setContentText(message)
//                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
//                .setContentIntent(pendingIntent)
//                .setAutoCancel(true);
//
//        if (lowStockProducts.size() <= 3) {
//            StringBuilder detailText = new StringBuilder();
//            for (Product product : lowStockProducts) {
//                Stock stock = Data.Stock.stocks.get(product.getId());
//                int quantity = (stock != null && stock.getQuantity() != null) ? stock.getQuantity().intValue() : 0;
//                detailText.append(product.getLabel())
//                        .append(" (").append(quantity).append(" left)\n");
//            }
//            builder.setStyle(new NotificationCompat.BigTextStyle()
//                    .bigText(detailText.toString().trim()));
//        }
//
//        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
//                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
//                        == PackageManager.PERMISSION_GRANTED) {
//            notificationManager.notify(1001, builder.build());
//        } else {
//            Log.w(TAG, "Notification permission not granted.");
//        }
//    }
//
//    public int getLowStockThreshold() {
//        return prefs.getInt(KEY_LOW_STOCK_THRESHOLD, DEFAULT_LOW_STOCK_THRESHOLD);
//    }
//
//    public void setLowStockThreshold(int threshold) {
//        prefs.edit().putInt(KEY_LOW_STOCK_THRESHOLD, threshold).apply();
//    }
//
//    public boolean isProductLowStock(Product product) {
//        Stock stock = Data.Stock.stocks.get(product.getId());
//        double quantity = (stock != null && stock.getQuantity() != null) ? stock.getQuantity() : 0.0;
//        return quantity <= getLowStockThreshold() && quantity > 0;
//    }
//}
