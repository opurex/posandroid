
package com.opurex.client.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.LruCache;
import android.os.AsyncTask;
import android.widget.ImageView;
import android.util.Log;

import com.opurex.client.R;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.lang.ref.WeakReference;
import java.security.MessageDigest;

public class ImageCache {
    private static final String TAG = "ImageCache";
    private static final String CACHE_DIR = "image_cache";
    private static final int MAX_MEMORY_CACHE_SIZE = 1024 * 1024 * 8; // 8MB
    
    private static ImageCache instance;
    private LruCache<String, Bitmap> memoryCache;
    private File diskCacheDir;
    private Context context;
    
    private ImageCache(Context context) {
        this.context = context.getApplicationContext();
        
        // Initialize memory cache
        memoryCache = new LruCache<String, Bitmap>(MAX_MEMORY_CACHE_SIZE) {
            @Override
            protected int sizeOf(String key, Bitmap bitmap) {
                return bitmap.getByteCount();
            }
        };
        
        // Initialize disk cache directory
        diskCacheDir = new File(context.getCacheDir(), CACHE_DIR);
        if (!diskCacheDir.exists()) {
            diskCacheDir.mkdirs();
        }
    }
    
    public static synchronized ImageCache getInstance(Context context) {
        if (instance == null) {
            instance = new ImageCache(context);
        }
        return instance;
    }
    
    public void loadImage(String imageUrl, ImageView imageView) {
        if (imageUrl == null || imageUrl.isEmpty()) {
            imageView.setImageResource(R.drawable.prd_default);
            return;
        }
        
        String key = generateKey(imageUrl);
        
        // Check memory cache first
        Bitmap bitmap = memoryCache.get(key);
        if (bitmap != null) {
            imageView.setImageBitmap(bitmap);
            return;
        }
        
        // Check disk cache
        bitmap = loadFromDiskCache(key);
        if (bitmap != null) {
            memoryCache.put(key, bitmap);
            imageView.setImageBitmap(bitmap);
            return;
        }
        
        // Load from network
        imageView.setImageResource(R.drawable.prd_default); // Placeholder
        new ImageDownloadTask(imageView, key).execute(imageUrl);
    }
    
    private Bitmap loadFromDiskCache(String key) {
        try {
            File file = new File(diskCacheDir, key);
            if (file.exists()) {
                return BitmapFactory.decodeFile(file.getAbsolutePath());
            }
        } catch (Exception e) {
            Log.w(TAG, "Failed to load from disk cache: " + e.getMessage());
        }
        return null;
    }
    
    private void saveToDiskCache(String key, Bitmap bitmap) {
        try {
            File file = new File(diskCacheDir, key);
            FileOutputStream out = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.PNG, 90, out);
            out.close();
        } catch (Exception e) {
            Log.w(TAG, "Failed to save to disk cache: " + e.getMessage());
        }
    }
    
    private String generateKey(String url) {
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            digest.update(url.getBytes());
            byte[] hash = digest.digest();
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            return String.valueOf(url.hashCode());
        }
    }
    
    public void clearCache() {
        memoryCache.evictAll();
        
        // Clear disk cache
        File[] files = diskCacheDir.listFiles();
        if (files != null) {
            for (File file : files) {
                file.delete();
            }
        }
    }
    
    public long getCacheSize() {
        long size = 0;
        File[] files = diskCacheDir.listFiles();
        if (files != null) {
            for (File file : files) {
                size += file.length();
            }
        }
        return size;
    }
    
    private class ImageDownloadTask extends AsyncTask<String, Void, Bitmap> {
        private final WeakReference<ImageView> imageViewReference;
        private final String key;
        private String imageUrl;
        
        public ImageDownloadTask(ImageView imageView, String key) {
            imageViewReference = new WeakReference<>(imageView);
            this.key = key;
        }
        
        @Override
        protected Bitmap doInBackground(String... urls) {
            imageUrl = urls[0];
            return downloadBitmap(imageUrl);
        }
        
        @Override
        protected void onPostExecute(Bitmap bitmap) {
            if (bitmap != null && imageViewReference != null) {
                ImageView imageView = imageViewReference.get();
                if (imageView != null) {
                    // Cache the image
                    memoryCache.put(key, bitmap);
                    saveToDiskCache(key, bitmap);
                    
                    // Set the image
                    imageView.setImageBitmap(bitmap);
                }
            }
        }
        
        private Bitmap downloadBitmap(String url) {
            try {
                URL imageUrl = new URL(url);
                HttpURLConnection connection = (HttpURLConnection) imageUrl.openConnection();
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(10000);
                connection.setInstanceFollowRedirects(true);
                
                InputStream inputStream = connection.getInputStream();
                Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                inputStream.close();
                connection.disconnect();
                
                return bitmap;
            } catch (Exception e) {
                Log.w(TAG, "Failed to download image: " + url, e);
                return null;
            }
        }
    }
}
