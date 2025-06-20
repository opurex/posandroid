package com.opurex.client.models.interfaces;

import android.content.Context;
import android.graphics.Bitmap;

/**
 * Created by nsvir on 10/08/15.
 * n.svirchevsky@gmail.com
 */
public interface Item {

    public enum Type {
        Product,
        Category;
    }

    Type getType();
    String getLabel();
    String getId();
    boolean hasImage();
    Bitmap getImage(Context ctx);
}
