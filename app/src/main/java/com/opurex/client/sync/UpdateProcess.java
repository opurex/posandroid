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

import com.opurex.client.Configure;
import com.opurex.client.R;
import com.opurex.client.data.*;
import com.opurex.client.data.Data;
import com.opurex.client.models.*;
import com.opurex.client.utils.Error;
import com.opurex.client.utils.URLTextGetter;
import com.opurex.client.activities.TrackedActivity;
import com.opurex.client.widgets.ProgressPopup;

import android.content.Context;
import android.os.Message;
import android.os.Handler;
import android.util.Log;

import java.io.IOError;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Manager for update processus and UI feedback
 */
public class UpdateProcess implements Handler.Callback {

    private static final String LOG_TAG = "Opurex/UpdateProcess";

    private static final int PHASE_DATA = 1;
    private static final int PHASE_IMG = 2;
    private static final int CTX_POOL_SIZE = 10;

    private static UpdateProcess instance;

    private Context ctx;
    private ProgressPopup feedback;
    private TrackedActivity caller;
    private Handler listener;
    private int progress;
    private boolean failed;
    private int phase;
    // States for img phase
    private int openCtxCount;
    private List<Product> productsToLoad;
    private List<Category> categoriesToLoad;
    private List<PaymentMode> paymentModesToLoad;
    private int nextCtxIdx;
    private ImgUpdate imgUpdate;

    private UpdateProcess(Context ctx) {
        this.ctx = ctx;
    }

    /**
     * Start update process with the given context (should be application
     * context). If already started nothing happens.
     *
     * @return True if started, false if already started.
     */
    public static boolean start(Context ctx) {
        if (instance == null) {
            // Create new process and run
            instance = new UpdateProcess(ctx);
            instance.failed = false;
            SyncUpdate syncUpdate = new SyncUpdate(instance.ctx,
                    new Handler(instance));
            syncUpdate.startSyncUpdate();
            return true;
        } else {
            // Already started
            return false;
        }
    }

    private void storeAccount() {
        Data.Login.setLogin(new Login(
                Configure.getUser(this.ctx),
                Configure.getPassword(this.ctx),
                Configure.getMachineName(this.ctx)));
        try {
            Data.Login.save();
        } catch (IOError e) {
            Log.e(LOG_TAG, "Could not save Login", e);
        }
    }

    private void invalidateAccount() {
        Data.Login.setLogin(new Login());
        try {
            Data.Login.save();
        } catch (IOError e) {
            Log.e(LOG_TAG, "Could not save Login", e);
        }
    }

    private void runImgPhase() {
        this.progress = 0;
        this.productsToLoad = new ArrayList<Product>();
        this.categoriesToLoad = new ArrayList<Category>();
        this.paymentModesToLoad = new ArrayList<PaymentMode>();
        // Look for categories and products with an image.
        Catalog c = Data.Catalog.catalog(this.ctx);
        for (Category cat : c.getAllCategories()) {
            if (cat.hasImage()) {
                this.categoriesToLoad.add(cat);
            }
            for (Product p : c.getProducts(cat)) {
                if (p.hasImage()) {
                    this.productsToLoad.add(p);
                }
            }
        }
        // Look for payment modes with an image.
        List<PaymentMode> modes = Data.PaymentMode.paymentModes(this.ctx);
        for (PaymentMode mode : modes) {
            if (mode.hasImage()) {
                this.paymentModesToLoad.add(mode);
            }
        }
        // Set the feedback progress bar.
        this.nextCtxIdx = 0;
        this.imgUpdate = new ImgUpdate(this.ctx, new Handler(this));
        if (this.feedback != null) {
            this.feedback.setProgress(0);
            this.feedback.setMax(this.productsToLoad.size()
                    + this.categoriesToLoad.size());
            this.feedback.setTitle(instance.ctx.getString(R.string.sync_img_title));
            this.feedback.setMessage(instance.ctx.getString(R.string.sync_img_message));
        }
        // Run.
        this.pool();
    }

    public static boolean isStarted() {
        return instance != null;
    }

    private void finish() {
        if (!this.failed) {
            this.storeAccount();
        } else {
            this.invalidateAccount();
        }
        Log.i(LOG_TAG, "Update sync finished.");
        SyncUtils.notifyListener(this.listener, SyncUpdate.SYNC_DONE);
        unbind();
        instance = null;
    }

    /**
     * Bind a feedback popup to the process. Must be started before binding
     * otherwise nothing happens. This will show the popup with the current
     * state.
     */
    public static boolean bind(ProgressPopup feedback, TrackedActivity caller,
            Handler listener) {
        if (instance == null) {
            return false;
        }
        instance.caller = caller;
        instance.feedback = feedback;
        instance.listener = listener;
        feedback.setMax(SyncUpdate.STEPS);
        feedback.setTitle(instance.ctx.getString(R.string.sync_title));
        feedback.setMessage(instance.ctx.getString(R.string.sync_message));
        feedback.setProgress(instance.progress);
        feedback.show();
        return true;
    }

    /**
     * Unbind feedback for when the popup is destroyed during the process.
     */
    public static void unbind() {
        if (instance == null) {
            return;
        }
        instance.feedback.dismiss();
        instance.feedback = null;
        instance.listener = null;
        instance.caller = null;
    }

    /**
     * Increment progress by steps and update feedback.
     */
    private void progress(int steps) {
        this.progress += steps;
        if (this.feedback != null) {
            for (int i = 0; i < steps; i++) {
                this.feedback.increment(false);
            }
        }
    }

    /**
     * Increment progress by 1 and update feedback.
     */
    private void progress() {
        this.progress(1);
    }

    /**
     * POOL!!! Let ctx fly and fill the ctx pool with img requests
     */
    private synchronized void pool() {
        int maxSize = this.productsToLoad.size() + this.categoriesToLoad.size()
                + this.paymentModesToLoad.size();
        int prdEnd = this.productsToLoad.size();
        int catEnd = this.productsToLoad.size() + this.categoriesToLoad.size();
        while (openCtxCount < CTX_POOL_SIZE
                && this.nextCtxIdx < maxSize) {
            if (this.nextCtxIdx < prdEnd) {
                this.imgUpdate.loadImage(this.productsToLoad.get(this.nextCtxIdx));
            } else if (this.nextCtxIdx < catEnd) {
                int idx = this.nextCtxIdx - prdEnd;
                this.imgUpdate.loadImage(this.categoriesToLoad.get(idx));
            } else {
                int idx = this.nextCtxIdx - catEnd;
                this.imgUpdate.loadImage(this.paymentModesToLoad.get(idx));
            }
            this.nextCtxIdx++;
            this.openCtxCount++;
        }
        if (this.nextCtxIdx == maxSize && this.openCtxCount == 0) {
            // This is the end
            this.finish();
        }
    }

    /**
     * A pool ctx has finished, release it and refill pool.
     */
    private synchronized void poolDown() {
        this.progress();
        this.openCtxCount--;
        this.pool();
    }

    @Override
    public boolean handleMessage(Message m) {
        switch (m.what) {
            case SyncUpdate.SYNC_ERROR:
                this.failed = true;
                if (m.obj instanceof Exception) {
                    Exception e = (Exception) m.obj;
                    if (e instanceof URLTextGetter.ServerException &&
                            "Unable to get auth token".equals(e.getMessage())) {
                        // Not logged error
                        SyncUtils.notifyListener(this.listener,
                                SyncUpdate.SYNC_ERROR_NOT_LOGGED);
                    } else {
                        // Response error (unexpected content)
                        Log.i(LOG_TAG, "Server error " + m.obj);
                        Error.showError(R.string.err_server_error,
                                e.toString(), this.caller);
                    }
                } else {
                    // String user error
                    String error = (String) m.obj;
                    if ("Not logged".equals(error)) {
                        Log.i(LOG_TAG, "Not logged");
                        SyncUtils.notifyListener(this.listener,
                                SyncUpdate.SYNC_ERROR_NOT_LOGGED);
                    } else {
                        Log.e(LOG_TAG, "Unknown server errror: " + error);
                        Error.showError(R.string.err_server_error, error,
                                this.caller);
                    }
                }
                this.finish();
                break;
            case SyncUpdate.CONNECTION_FAILED:
                this.failed = true;
                if (m.obj instanceof Exception) {
                    Exception e = (Exception) m.obj;
                    Log.i(LOG_TAG, "Connection error", e);
                    Error.showError(R.string.err_connection_error,
                            e.toString(), this.caller);
                } else {
                    String error = null;
                    if (m.obj instanceof String) {
                        error = (String) m.obj;
                    }
                    Log.i(LOG_TAG, "Server error " + m.obj);
                    Error.showError(R.string.err_server_error, error,
                            this.caller);
                }
                this.finish();
                break;

            case SyncUpdate.INCOMPATIBLE_VERSION:
                this.failed = true;
                Error.showError(R.string.err_version_error, instance.caller);
                this.finish();
                break;
            case SyncUpdate.VERSION_DONE:
                this.progress();
                break;

            case SyncUpdate.CURRENCIES_SYNC_DONE:
                this.progress();
                List<Currency> currencies = (List<Currency>) m.obj;
                Data.Currency.set(currencies);
                try {
                    Data.Currency.save();
                } catch (IOError e) {
                    Log.e(LOG_TAG, "Unable to save currencies", e);
                    Error.showError("Unable to save currencies.",
                            e.toString(), this.caller);
                }
                break;
            case SyncUpdate.CASHREG_SYNC_DONE:
                this.progress();
                // Get received cash register
                CashRegister cashReg = (CashRegister) m.obj;
                Data.CashRegister.set(cashReg);
                try {
                    Data.CashRegister.save();
                } catch (IOError e) {
                    Log.e(LOG_TAG, "Unable to save cash register", e);
                    Error.showError(R.string.err_save_cash_register,
                            e.toString(), this.caller);
                }
                break;
            case SyncUpdate.CASH_SYNC_DONE:
                this.progress();
                // Get received cash
                Cash cash = (Cash) m.obj;
                Cash current = Data.Cash.currentCash(this.ctx);
                boolean save = false;
                if (current == null) {
                    // No current cash, set it
                    cash.setContinuous(false);
                    Data.Cash.setCash(cash);
                    save = true;
                } else {
                    // If there are archives to be sent, the received cash
                    // is an old one and must be ignored.
                    int archiveCount = CashArchive.getArchiveCount(this.ctx);
                    if (archiveCount == 0) {
                        if (current.absorb(cash)) {
                            save = true;
                        } else {
                            // If the current cash was not opened, switch
                            // (no operation were done)
                            if (!current.isOpened()) {
                                cash.setContinuous(false);
                                Data.Cash.setCash(cash);
                                save = true;
                            } else {
                                // This is a conflict
                                Error.showError(R.string.err_cash_conflict,
                                        instance.caller);
                            }
                        }
                    }
                    // TODO: handle switching cash registers (now: delete data)
                    // Check for future send conflicts:
                    // - current has a lower sequence
                    // - current has an equal sequence with archives
                    if (current.getSequence() < cash.getSequence()
                            || (archiveCount > 0 && current.getSequence() == cash.getSequence())) {
                        Error.showError(R.string.err_cash_fuckedup,
                                "Cash session conflict: archive will conflict",
                                this.caller);
                    }
                }
                if (save) {
                    try {
                        Data.Cash.save();
                    } catch (IOError e) {
                        Log.e(LOG_TAG, "Unable to save cash", e);
                        Error.showError(R.string.err_save_cash, e.toString(),
                                this.caller);
                    }
                }
                break;

            case SyncUpdate.TAXES_SYNC_DONE:
                List<Tax> taxes = ((List<Tax>) m.obj);
                Data.Tax.setTaxes(taxes);
                Data.Tax.save();
                break;
            case SyncUpdate.CATEGORIES_SYNC_DONE:
            case SyncUpdate.PRODUCTS_SYNC_DONE:
                this.progress();
                break;
            case SyncUpdate.CATALOG_SYNC_DONE:
                this.progress();
                Catalog catalog = (Catalog) m.obj;
                Data.Catalog.setCatalog(catalog);
                try {
                    Data.Catalog.save();
                } catch (IOError e) {
                    Log.e(LOG_TAG, "Unable to save catalog", e);
                    Error.showError(R.string.err_save_catalog, e.toString(),
                            this.caller);
                }
                break;
            case SyncUpdate.COMPOSITIONS_SYNC_DONE:
                this.progress();
                //noinspection unchecked
                Data.Composition.compositions = (Map<String, Composition>) m.obj;
                try {
                    Data.Composition.save();
                } catch (IOError e) {
                    Log.e(LOG_TAG, "Unable to save compositions", e);
                    Error.showError(R.string.err_save_compositions,
                            e.toString(), this.caller);
                }
                break;

            case SyncUpdate.ROLES_SYNC_DONE:
                this.progress();
                break;
            case SyncUpdate.USERS_SYNC_DONE:
                this.progress();
                //noinspection unchecked
                List<User> users = (List<User>) m.obj;
                Data.User.setUsers(users);
                try {
                    Data.User.save();
                } catch (IOError e) {
                    Log.e(LOG_TAG, "Unable to save users", e);
                    Error.showError(R.string.err_save_users, e.toString(),
                            this.caller);
                }
                break;

            case SyncUpdate.CUSTOMERS_SYNC_DONE:
                this.progress();
                //noinspection unchecked
                List<Customer> customers = (List) m.obj;
                Data.Customer.customers = customers;
                try {
                    Data.Customer.save();
                } catch (IOError e) {
                    Log.e(LOG_TAG, "Unable to save customers", e);
                    Error.showError(R.string.err_save_customers, e.toString(),
                            this.caller);
                }
                break;

            case SyncUpdate.TARIFF_AREAS_SYNC_DONE:
                this.progress();
                //noinspection unchecked
                List<TariffArea> areas = (List<TariffArea>) m.obj;
                Data.TariffArea.areas = areas;
                try {
                    Data.TariffArea.save();
                } catch (IOError e) {
                    Log.e(LOG_TAG, "Unable to save tariff areas", e);
                    Error.showError(R.string.err_save_tariff_areas,
                            e.toString(), this.caller);
                }
                break;
            case SyncUpdate.PAYMENTMODE_SYNC_DONE:
                this.progress();
                //noinspection unchecked
                List<PaymentMode> modes = (List<PaymentMode>) m.obj;
                Data.PaymentMode.setPaymentModes(modes);
                try {
                    Data.PaymentMode.save();
                } catch (IOError e) {
                    Log.e(LOG_TAG, "Unable to save payment modes", e);
                    Error.showError(R.string.err_save_payment_modes,
                            e.toString(), this.caller);
                }
                break;

            case SyncUpdate.RESOURCE_SYNC_DONE:
                //this.progress();
                try {
                    if (m.obj != null) {
                        String[] resData = (String[]) m.obj;
                        ResourceData.save(this.ctx, resData[0], resData[1]);
                    } else {
                        // TODO: get name from result when API send back name even if null
                        //ResourceData.delete(this.ctx, resData[0]);
                    }
                } catch (IOException e) {
                    Log.e(LOG_TAG, "Unable to save resource", e);
                    Error.showError(R.string.err_save_resource,
                            e.toString(), this.caller);
                }
                break;
            case SyncUpdate.PLACES_SKIPPED:
                this.progress();
                break;
            case SyncUpdate.PLACES_SYNC_DONE:
                this.progress();
                //noinspection unchecked
                List<Floor> floors = (List<Floor>) m.obj;
                Data.Place.floors = floors;
                try {
                    Data.Place.save();
                } catch (IOError e) {
                    Log.e(LOG_TAG, "Unable to save places", e);
                    Error.showError(R.string.err_save_places,
                            e.toString(), this.caller);
                }
                break;
            case SyncUpdate.DISCOUNT_SYNC_DONE:
                this.progress();
                //noinspection unchecked
                ArrayList<Discount> discounts = (ArrayList< Discount>) m.obj;
                Data.Discount.setCollection(discounts);
                try {
                    Data.Discount.save();
                } catch (IOError e) {
                    Log.e(LOG_TAG, "Unable to save discount", e);
                    Error.showError(R.string.err_save_discount,
                            e.toString(), caller);
                }
                break;

            case SyncUpdate.CASHREG_SYNC_NOTFOUND:
                this.failed = true;
                SyncUtils.notifyListener(this.listener, SyncUpdate.CASHREG_SYNC_NOTFOUND);
                this.finish();
                break;

            case SyncUpdate.PAYMENTMODE_SYNC_ERROR:
            case SyncUpdate.DISCOUNT_SYNC_ERROR:
            case SyncUpdate.CATEGORIES_SYNC_ERROR:
            case SyncUpdate.TAXES_SYNC_ERROR:
            case SyncUpdate.CATALOG_SYNC_ERROR:
            case SyncUpdate.USERS_SYNC_ERROR:
            case SyncUpdate.CUSTOMERS_SYNC_ERROR:
            case SyncUpdate.CASH_SYNC_ERROR:
            case SyncUpdate.CASHREG_SYNC_ERROR:
            case SyncUpdate.PLACES_SYNC_ERROR:
            case SyncUpdate.COMPOSITIONS_SYNC_ERROR:
            case SyncUpdate.TARIFF_AREA_SYNC_ERROR:
                this.failed = true;
                if (m.obj instanceof Exception) {
                    Error.showError(((Exception) m.obj).getMessage(), this.caller);
                } else {
                    String error = null;
                    if (m.obj instanceof String) {
                        error = (String) m.obj;
                    }
                    Error.showError(R.string.err_sync, error, this.caller);
                }
                break;

            case SyncUpdate.SYNC_DONE:
                // Data phase finished, load images.
                if (!this.failed) {
                    this.runImgPhase();
                } else {
                    this.finish();
                }
                break;

            case ImgUpdate.LOAD_DONE:
                this.poolDown();
                break;
            case ImgUpdate.CONNECTION_FAILED:
                this.failed = true;
                if (instance != null) {
                    if (m.obj instanceof Exception) {
                        Error.showError(((Exception) m.obj).getMessage(),
                                this.caller);
                    } else if (m.obj instanceof Integer) {
                        Error.showError("Code " + m.obj, this.caller);
                    }
                    this.finish();
                }
                break;
            default:
        }
        return true;
    }
}
