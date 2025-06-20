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

import com.opurex.client.data.Data;
import com.opurex.client.utils.exception.loadArchiveException;
import com.opurex.client.models.Ticket;
import com.opurex.client.utils.Error;
import com.opurex.client.R;
import com.opurex.client.data.CashArchive;
import com.opurex.client.models.Customer;
import com.opurex.client.models.Receipt;
import com.opurex.client.models.ZTicket;
import com.opurex.client.activities.TrackedActivity;
import com.opurex.client.utils.exception.SaveArchiveException;
import com.opurex.client.widgets.ProgressPopup;

import android.content.Context;
import android.util.Log;
import android.os.Handler;
import android.os.Message;

import java.io.IOError;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/** Process to send an archive and handle feedback. */
public class SendProcess implements Handler.Callback {

    private static final String LOG_TAG = "Opurex/SendProcess";

    private static SendProcess instance;

    private Context ctx;
    private boolean errorOccured;
    private ProgressPopup feedback;
    private TrackedActivity caller;
    private Handler listener;
    /** Archives progress */
    private int progress;
    private int progressMax;
    /** Content progress */
    private int subprogress;
    private int subprogressMax;
    /** [0] is ZTicket, [1] is List<Receipt> */
    private Object[] currentArchive;
    /** True when sync is requested to be interrupted */
    private boolean stop;
    private boolean sendCustomer;

    private SendProcess(Context ctx) throws IOException {
        this.ctx = ctx;
        this.errorOccured = false;
        this.progressMax = CashArchive.getArchiveCount(ctx);
        this.sendCustomer = Data.Customer.createdCustomers.size() > 0;
    }

    /** Start update process with the given context (should be application
     * context). If already started nothing happens.
     * @return True if started, false if already started.
     */
    public static boolean start(Context ctx) {
        if (instance == null) {
            // Create new process and run
            try {
                instance = new SendProcess(ctx);
                instance.sendCustomer();
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
            return true;
        } else {
            // Already started
            return false;
        }
    }
    public static boolean isStarted() {
        return instance != null;
    }
    /** Request sync to stop. This is not immediate. */
    public static boolean stop() {
        if (isStarted()) {
            instance.stop = true;
            instance.refreshFeedback();
            unbind();
            instance = null;
            return true;
        }
        return false;
    }
    private void finish() {
        SyncUtils.notifyListener(this.listener, SyncSend.SYNC_DONE);
        unbind();
        instance = null;
    }
    /** Bind a feedback popup to the process. Must be started before binding
     * otherwise nothing happens.
     * This will show the popup with the current state.
     */
    public static boolean bind(ProgressPopup feedback, TrackedActivity caller,
            Handler listener) {
        if (instance == null) {
            return false;
        }
        instance.caller = caller;
        instance.feedback = feedback;
        instance.listener = listener;
        // Update from current state
        feedback.setTitle(instance.ctx.getString(R.string.sync_title));
        instance.refreshFeedback();
        feedback.show();
        return true;
    }
    /** Unbind feedback for when the popup is destroyed during the process. */
    public static void unbind() {
        if (instance == null) {
            return;
        }
        instance.feedback.dismiss();
        instance.feedback = null;
        instance.listener = null;
        instance.caller = null;
    }

    private void refreshFeedback() {
        if (this.feedback == null) {
            return;
        }
        if (!this.stop) {
            if (this.sendCustomer) {
                this.feedback.setMessage(instance.ctx.getString(R.string.sync_send_customers));
                this.feedback.setMax(1);
                this.feedback.setProgress(instance.subprogress);
            } else {
                this.feedback.setMessage(instance.ctx.getString(R.string.sync_send_message,
                        instance.progress, instance.progressMax));
                this.feedback.setMax(instance.subprogressMax);
                this.feedback.setProgress(instance.subprogress);
            }
        } else {
            this.feedback.setMessage(this.ctx.getString(R.string.sync_send_stopping));
            this.feedback.setIndeterminate(true);
        }
    }

    /**
     * Sends new customer to server.
     * @return true if customers were send, false otherwise
     */
    private boolean sendCustomer() {
        if (Data.Customer.resolvedIds.size() > 0) {
            Log.i(LOG_TAG, "Customer Sync: There are saved local customer ids");
        }
        if (!this.sendCustomer) {
            SyncUtils.notifyListener(this.listener, SyncSend.CUSTOMER_SYNC_DONE);
            instance.nextArchive();
            return false;
        }
        JSONArray cstJArray = new JSONArray();
        for (Customer c : Data.Customer.createdCustomers) {
            try {
                // Balance is updated locally but ignored by server.
                JSONObject o = c.toJSON();
                cstJArray.put(o);
            } catch (JSONException e) {
                Log.d(LOG_TAG, c.toString(), e);
                SyncUtils.notifyListener(this.listener,
                        SyncSend.CUSTOMER_SYNC_FAILED);
                return false;
            }
        }
        this.subprogress++;
        this.refreshFeedback();
        ServerLoader loader = new ServerLoader(this.ctx);
        loader.asyncWrite(new CustHandler(this, this.listener),
                "api/customer", "customers", cstJArray.toString());
        return true;
    }

    private boolean parseCustomer(JSONObject resp) {
        try {
            // Were customers properly send ?
            int createdCustomerSize = Data.Customer.createdCustomers.size();
            JSONArray ids = null;
            if (createdCustomerSize == 1) {
                ids = new JSONArray();
                ids.put(String.valueOf(resp.getInt("content")));
            } else {
                ids = resp.getJSONArray("content");
            }
            for (int i = 0; i < createdCustomerSize; i++) {
                String tmpId = Data.Customer.createdCustomers.get(i).getId();
                String serverId = String.valueOf(ids.getInt(i));
                if (tmpId == null) continue; // Should never happen.
                // Updating local info
                for (Customer c : Data.Customer.customers) {
                    if (c.getId().equals(tmpId)) {
                        c.setId(serverId);
                        break;
                    }
                }
                for (Ticket t : Data.Session.currentSession(this.ctx).getTickets()) {
                    Customer c = t.getCustomer();
                    if (c != null && c.getId().equals(tmpId)) {
                        c.setId(serverId);
                    }
                }
                for (Receipt r : Data.Receipt.getReceipts(this.ctx)) {
                    Customer c = r.getTicket().getCustomer();
                    if (c != null && c.getId().equals(tmpId)) {
                        c.setId(serverId);
                    }
                }
                Data.Customer.resolvedIds.put(tmpId, serverId);
            }
            // Sending Customer completed
            Data.Customer.createdCustomers.clear();
            Data.Customer.save();
            Data.Session.save();
            Data.Receipt.save();
            Log.i(LOG_TAG, "Customer Sync: Saved new local customer ids");
            this.sendCustomer = false;
            this.subprogress = 0;
            SyncUtils.notifyListener(this.listener, SyncSend.CUSTOMER_SYNC_DONE);
            instance.nextArchive();
            return true;
        } catch (JSONException e) {
            // Customer not send properly.
            Log.i(LOG_TAG, "Error while parsing customer result", e);
            SyncUtils.notifyListener(this.listener, SyncSend.CUSTOMER_SYNC_FAILED);
        } catch (IOError e) {
            Log.i(LOG_TAG, "Could not save customer data in parse customer", e);
            SyncUtils.notifyListener(this.listener, SyncSend.CUSTOMER_SYNC_FAILED);
        }
        return false;
    }

    private boolean nextArchive() {
        try {
            this.currentArchive = CashArchive.loadAnArchive();
        } catch (loadArchiveException e) {
            e.printStackTrace();
            return false;
        }
        if (this.currentArchive[0] == null) {
            // No more
            return false;
        }
        // Copy list to break pointer reference
        List<Receipt> receipts = new ArrayList<Receipt>();
        //noinspection unchecked [1] is a List<Receipt>
        receipts.addAll((List<Receipt>) this.currentArchive[1]);
        // Count chunks
        int chunks = receipts.size() / SyncSend.TICKETS_BUFFER;
        if (receipts.size() % SyncSend.TICKETS_BUFFER > 0) {
            // Add final partial chunk
            chunks++;
        }
        ZTicket z = (ZTicket) this.currentArchive[0];
        // Add 2 for cash (open and close)
        this.subprogressMax = chunks + 2;
        // Reinit subprogress
        this.subprogress = 0;
        this.progress++;
        this.refreshFeedback();
        // Sync archive
        SyncSend syncSend = new SyncSend(this.ctx,
                new Handler(this), receipts, z);
        syncSend.synchronize();
        return true;
    }

    /** Things to do after an archive is sent and start the next one if any. */
    private void postSync() {
        // Delete current archive
        CashArchive.deleteArchive(this.ctx, (ZTicket) this.currentArchive[0]);
        // Move to next or finish
        if (!this.nextArchive() || this.stop) {
            // Finished
            this.finish();
        }
    }

    @Override
	public boolean handleMessage(Message m) {
        switch (m.what) {
        case SyncSend.CONNECTION_FAILED:
            if (m.obj instanceof Exception) {
                Log.i(LOG_TAG, "Connection error", ((Exception)m.obj));
                Error.showError(R.string.err_connection_error, this.caller);
            } else {
                String error = null;
                if (m.obj instanceof String) {
                    error = (String) m.obj;
                }
                Log.i(LOG_TAG, "Server error " + m.obj);
                Error.showError(R.string.err_server_error, error, this.caller);
            }
            this.finish();
            break;
        case SyncSend.OPENCASH_SYNC_FAILED:
        case SyncSend.CLOSECASH_SYNC_FAILED:
            Log.w(LOG_TAG, "Cash sync failed "
                    + m.obj.toString());
            String error = null;
            if (m.obj instanceof String) {
                error = (String) m.obj;
            }
            Error.showError(R.string.err_sync, error, this.caller);
            this.finish();
            break;
        case SyncSend.SYNC_ERROR:
            Log.w(LOG_TAG, "Sync error: " +  m.obj);
            error = null;
            if (m.obj instanceof String) {
                error = (String) m.obj;
            }
            Error.showError(R.string.err_sync, error, this.caller);
            this.finish();
            break;

        case SyncSend.OPENCASH_SYNC_DONE:
            this.subprogress++;
            this.refreshFeedback();
            break;
        case SyncSend.RECEIPTS_SYNC_FAILED:
            Log.w(LOG_TAG, "Receipts sync failed: " + m.obj);
            error = null;
            if (m.obj instanceof String) {
                error = (String) m.obj;
            }
            Error.showError(R.string.err_sync, error, this.caller);
            this.finish();
            break;
        case SyncSend.RECEIPTS_SYNC_DONE:
            this.subprogress++;
            this.refreshFeedback();
            break;
        case SyncSend.RECEIPTS_SYNC_PROGRESSED:
            this.subprogress++;
            this.refreshFeedback();
            // Delete the first receipts to not send them twice.
            // Use tickets or this.currentArchive[1] indifferently
            // (shared pointer)
            List tickets = (List) this.currentArchive[1];
            if (tickets.size() > SyncSend.TICKETS_BUFFER) {
                // subList shares reference, clear portion of the original list
                tickets.subList(0, SyncSend.TICKETS_BUFFER).clear();
            } else {
                tickets.clear();
            }
            // Update archive
            try {
                //noinspection unchecked
                CashArchive.updateArchive(
                        (ZTicket) this.currentArchive[0],
                        (List<Receipt>) this.currentArchive[1]);
            } catch (IOException e) {
                e.printStackTrace();
                // There's nothing we can do about it, it's too late
            } catch (SaveArchiveException e) {
                e.printStackTrace();
            }
            break;
        case SyncSend.CLOSECASH_SYNC_DONE:
            this.postSync();
            break;
        case SyncSend.CUSTOMER_SYNC_DONE:
            break;
        case SyncSend.CUSTOMER_SYNC_FAILED:
            Log.w(LOG_TAG, "New customers sync failed: " + m.obj);
            error = null;
            if (m.obj instanceof String) {
                error = (String) m.obj;
            }
            Error.showError(R.string.err_save_customers, error, this.caller);
            this.finish();
            break;
        case SyncSend.SYNC_DONE:
            // This does nothing as the message is sent at the same time as
            // an other *_DONE and may mess up post treatment order.
            break;
        }
        return true;
    }

    static private class CustHandler extends Handler {
        private final WeakReference<SendProcess> activityRef;
        private final WeakReference<Handler> listenerRef;

        public CustHandler(SendProcess activity, Handler listener) {
            this.activityRef = new WeakReference<>(activity);
            this.listenerRef = new WeakReference<>(listener);
        }

        private String getError(String response) {
            try {
                JSONObject o = new JSONObject(response);
                if (o.has("error")) {
                    return o.getString("error");
                }
            } catch (JSONException ignored) {
            }
            return null;
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case ServerLoader.OK:
                    // Parse content
                    try {
                        ServerLoader.Response r = (ServerLoader.Response) msg.obj;
                        SendProcess activity = this.activityRef.get();
                        if (activity != null) {
                            JSONObject result = r.getResponse();
                            String status = result.getString("status");
                            if (!status.equals("ok")) {
                                JSONObject err = result.getJSONObject("content");
                                String error = err.getString("code");
                                SyncUtils.notifyListener(this.listenerRef.get(),
                                        SyncSend.SYNC_ERROR, error);
                            } else {
                                activity.parseCustomer(result);
                            }
                        }
                    } catch (JSONException e) {
                        SyncUtils.notifyListener(this.listenerRef.get(),
                                SyncSend.SYNC_ERROR, msg.obj);
                    }
                    break;
                case ServerLoader.ERR:
                    Log.e(LOG_TAG, "Server error", (Exception) msg.obj);
                    SyncUtils.notifyListener(this.listenerRef.get(),
                            SyncSend.CONNECTION_FAILED, msg.obj);
            }
        }
    }
}
