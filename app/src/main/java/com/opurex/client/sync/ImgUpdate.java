/*
  Opurex Android client
  Copyright (C) Opurex contributors, see the COPYRIGHT file

  This program is free software: you can redistribute it and/or modify
  it under the terms of the GNU General Public License as published by
  the Free Software Foundation, either version 3 of the License, or
  (at your option) any later version.

  This program is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  GNU General Public License for more details.

  You should have received a copy of the GNU General Public License
  along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/
package com.opurex.client.sync;

import com.opurex.client.data.ImagesData;
import com.opurex.client.models.Category;
import com.opurex.client.models.PaymentMode;
import com.opurex.client.models.Product;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import java.io.IOException;

/** Updater for product and category images */
public class ImgUpdate {

    private static final String LOG_TAG = "Opurex/ImgUpdate";
    public static final int LOAD_DONE = 4701;
    public static final int CONNECTION_FAILED = 4702;
    private static final int TYPE_CAT = 1;
    private static final int TYPE_PRD = 2;

    private Context ctx;
    private ServerLoader loader;
    private Handler listener;

    public ImgUpdate(Context ctx, Handler listener) {
        this.ctx = ctx;
        this.loader = new ServerLoader(ctx);
        this.listener = listener;
    }

    /** Erase all category images. This is a synchronous call. */
    public void resetCategoryImages() throws IOException {
        ImagesData.clearCategories(this.ctx);
    }

    /** Erase all product images. This is a synchronous call. */
    public void resetProductImage() throws IOException {
        ImagesData.clearProducts(this.ctx);
    }

    /** Request and store the image of a category */
    public void loadImage(Category c) {
        int cId = Integer.parseInt(c.getId());
        this.loader.asyncReadImage(new DataHandler(DataHandler.TYPE_CAT, cId),
                "category", cId);
    }

    /** Request and store the image of a product */
    public void loadImage(Product p) {
        int pId = Integer.parseInt(p.getId());
        this.loader.asyncReadImage(new DataHandler(DataHandler.TYPE_PRD, pId),
                "product", pId);
    }

    public void loadImage(PaymentMode pm) {
        int pmId = pm.getId();
        this.loader.asyncReadImage(new DataHandler(DataHandler.TYPE_PM, pmId),
                "paymentmode", pmId);
    }

    private class DataHandler extends Handler {
        private static final int TYPE_CAT = 1;
        private static final int TYPE_PRD = 2;
        private static final int TYPE_PM = 3;

        private int type;
        private int id;

        public DataHandler(int type, int id) {
            this.type = type;
            this.id = id;
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case ServerLoader.OK:
                // Parse content
                byte[] img = (byte[]) msg.obj;
                switch (this.type) {
                case TYPE_CAT:
                    try {
                        ImagesData.storeCategoryImage(this.id, img);
                    } catch (IOException e) {
                        e.printStackTrace();
                        // TODO: handle IOException
                    }
                    break;
                case TYPE_PRD:
                    try {
                        ImagesData.storeProductImage(this.id, img);
                    } catch (IOException e) {
                        e.printStackTrace();
                        // TODO: handle IOException
                    }
                    break;
                case TYPE_PM:
                    try {
                        ImagesData.storePaymentModeImage(this.id, img);
                    } catch (IOException e) {
                        e.printStackTrace();
                        // TODO: handle IOException
                    }
                }
                SyncUtils.notifyListener(listener, LOAD_DONE,
                        msg.obj);
                break;
            case ServerLoader.ERR:
                ((Exception)msg.obj).printStackTrace();
                SyncUtils.notifyListener(listener, CONNECTION_FAILED,
                        msg.obj);
            }
        }
    }
}
