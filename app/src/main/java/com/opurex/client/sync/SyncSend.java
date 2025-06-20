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

import android.content.Context;
import android.os.Message;
import android.os.Handler;
import android.util.Log;

import java.util.List;

import com.opurex.client.data.Data;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.opurex.client.models.Receipt;
import com.opurex.client.models.ZTicket;

public class SyncSend {

    private static final String LOG_TAG = "Opurex/SyncSend";

    public static final int TICKETS_BUFFER = 10;
    // Note: SyncUpdate uses positive values, SyncSend negative ones
    public static final int SYNC_DONE = -1;
    public static final int CONNECTION_FAILED = -2;
    public static final int RECEIPTS_SYNC_DONE = -3;
    public static final int RECEIPTS_SYNC_FAILED = -4;
    public static final int OPENCASH_SYNC_DONE = -5;
    public static final int OPENCASH_SYNC_FAILED = -6;
    public static final int EPIC_FAIL = -7;
    public static final int SYNC_ERROR = -8;
    public static final int RECEIPTS_SYNC_PROGRESSED = -9;
    public static final int CLOSECASH_SYNC_DONE = -10;
    public static final int CLOSECASH_SYNC_FAILED = -11;
    public static final int CUSTOMER_SYNC_DONE = -12;
    public static final int CUSTOMER_SYNC_FAILED = -13;

    private Context ctx;
    private Handler listener;

    /** The tickets to send */
    private List<Receipt> receipts;
    /** Index of first ticket to send in a call */
    private int ticketOffset;
    private int currentChunkSize;
    private ZTicket z;
    private boolean receiptsDone;
    private boolean openCashDone;
    private boolean closeCashDone;
    private boolean killed;

    public SyncSend(Context ctx, Handler listener,
                    List<Receipt> receipts, ZTicket z) {
        this.listener = listener;
        this.ctx = ctx;
        this.receipts = receipts;
        this.z = z;
    }

    public void synchronize() {
        runOpenCashSync();
     }

    private void fail(String message, int operation) {
        SyncUtils.notifyListener(this.listener, operation, message);
    }

    private void runReceiptsSync() {
        if (this.receipts.size() == 0) {
            // No receipts, skip and notify
            this.receiptsDone = true;
            SyncUtils.notifyListener(this.listener, RECEIPTS_SYNC_DONE, true);
            this.runCloseCashSync();
        } else {
            try {
                if (!this.nextTicketRush()) {
                    this.runCloseCashSync();
                }
            } catch (Exception e) {
                if (e instanceof JSONException) {
                    // it is already handled.
                } else {
                    Log.e(LOG_TAG, "Error while sending tickets.", e);
                    this.fail("Error while sending ticket " + e.toString(),
                            RECEIPTS_SYNC_FAILED);
                }
            }
        }
    }

    /** First send the cash in opened state to be able to register the tickets
     * in it. */
    private void runOpenCashSync() {
        try {
            JSONObject jsCash = this.z.getCash().toOpenJSON();
            ServerLoader loader = new ServerLoader(this.ctx);
            loader.asyncWrite(new DataHandler(DataHandler.TYPE_OPENCASH),
                    "api/cash", "session", jsCash.toString());
        } catch (JSONException e) {
            Log.e(LOG_TAG, this.z.toString(), e);
            this.fail("Unable to format local data (open cash) " + e.toString(),
                    OPENCASH_SYNC_FAILED);
            return;
        }
    }

    /** Once all tickets are sent, update the cash in closed state. */
    private void runCloseCashSync() {
        try {
            ServerLoader loader = new ServerLoader(this.ctx);
            loader.asyncWrite(new DataHandler(DataHandler.TYPE_CLOSECASH),
                    "api/cash", "session", this.z.toJSON().toString());
        } catch (JSONException e) {
            Log.e(LOG_TAG, this.z.toString(), e);
            this.fail("Unable to format local data (close cash) " + e.toString(),
                    CLOSECASH_SYNC_FAILED);
            return;
        }
    }

    /** Send a chunk of tickets.
     * @return True if there was tickets to send, false otherwise.
     * @throws Exception if something goes wrong. */
    private boolean nextTicketRush() throws Exception {
        if (this.ticketOffset >= this.receipts.size()) {
            return false;
        }
        JSONArray rcptsJSON = new JSONArray();
        for (int i = this.ticketOffset; i < this.receipts.size()
                && i < this.ticketOffset + TICKETS_BUFFER; i++) {
            Receipt r = this.receipts.get(i);
            if (Data.Customer.resolvedIds.size() > 0
                    && r.getTicket() != null && r.getTicket().getCustomer() != null) {
                String sId = Data.Customer.resolvedIds.get(r.getTicket().getCustomer().getId());
                if (sId != null) r.getTicket().getCustomer().setId(sId);
            }
            try {
                JSONObject o = r.toJSON();
                rcptsJSON.put(o);
            } catch (JSONException e) {
                Log.e(LOG_TAG, r.toString(), e);
                this.fail("Unable to format local ticket " + e.toString(),
                        RECEIPTS_SYNC_FAILED);
                throw e;
            }
        }
        this.currentChunkSize = rcptsJSON.length();
        ServerLoader loader = new ServerLoader(this.ctx);
        loader.asyncWrite(new DataHandler(DataHandler.TYPE_RECEIPTS),
                "api/ticket", "tickets", rcptsJSON.toString());
        return true;
    }

    private void parseReceiptsResult(JSONObject resp) {
        try {
            JSONObject o = resp.getJSONObject("content");
            JSONArray jsFailures = o.getJSONArray("failures");
            JSONArray jsErrors = o.getJSONArray("errors");
            JSONArray jsSuccesses = o.getJSONArray("successes");
            // Handle server errors
            if (jsErrors.length() > 0) {
                /* Send a failure. It gives an opportunity to see the error.
                 * If it is a partial failure, all tickets that passed
                 * will be rejected next time. Better that nothing. */
                SyncUtils.notifyListener(this.listener, RECEIPTS_SYNC_FAILED,
                        resp.toString());
                return;
            }
            // Successes and failures are tracked server-side.
            // Consider it enough not to keep them locally.
            // Send next chunk or continue sync.
            this.ticketOffset += this.currentChunkSize;
            try {
                if (!this.nextTicketRush()) {
                    SyncUtils.notifyListener(this.listener, RECEIPTS_SYNC_DONE);
                    this.runCloseCashSync();
                } else {
                    SyncUtils.notifyListener(this.listener,
                            RECEIPTS_SYNC_PROGRESSED);
                }
            } catch (Exception e) {
                if (e instanceof JSONException) {
                    // it is already handled.
                } else {
                    Log.e(LOG_TAG, "Error while sending tickets.", e);
                    this.fail("Error while sending ticket " + e.toString(),
                            RECEIPTS_SYNC_FAILED);
                }
            }
        } catch (JSONException e) {
            // TODO: this sucks.
            // The tickets are probably registered server-side, but maybe not.
            // At worst, they will be rejected the next time.
            Log.e(LOG_TAG, "Error while parsing receipts result", e);
            SyncUtils.notifyListener(this.listener, RECEIPTS_SYNC_FAILED,
                    "Error while parsing receipts result " + resp.toString());
        }
    }

    private void parseOpenCashResult(JSONObject resp) {
        // Server returns the internal id. It is not used.
        SyncUtils.notifyListener(this.listener, OPENCASH_SYNC_DONE, this.z);
        // Continue with receipts
        this.runReceiptsSync();
    }

    private void parseCloseCashResult(JSONObject resp) {
        // Server returns the internal id. It is not used.
        SyncUtils.notifyListener(this.listener, CLOSECASH_SYNC_DONE, this.z);
        this.finish();
    }

    private void finish() {
        SyncUtils.notifyListener(this.listener, SYNC_DONE);
    }
    
    private class DataHandler extends Handler {
        
        private static final int TYPE_RECEIPTS = 1;
        private static final int TYPE_OPENCASH = 2;
        private static final int TYPE_CLOSECASH = 3;

        private int type;
        
        public DataHandler(int type) {
            this.type = type;
        }

        @Override
        public void handleMessage(Message msg) {
            switch (this.type) {
            case TYPE_RECEIPTS:
                SyncSend.this.receiptsDone = true;
                break;
            case TYPE_OPENCASH:
                SyncSend.this.openCashDone = true;
                break;
            case TYPE_CLOSECASH:
                SyncSend.this.closeCashDone = true;
                break;
            }
            switch (msg.what) {
            case ServerLoader.OK:
                // Parse content
                ServerLoader.Response resp = (ServerLoader.Response) msg.obj;
                String status = resp.getStatus();
                JSONObject result = resp.getResponse();
                if (!ServerLoader.Response.STATUS_OK.equals(status)) {
                    String error = resp.getErrorCode();
                    SyncUtils.notifyListener(listener, SYNC_ERROR, error);
                    finish();
                    break;
                } else {
                    switch (this.type) {
                    case TYPE_RECEIPTS:
                        parseReceiptsResult(result);
                        break;
                    case TYPE_OPENCASH:
                        parseOpenCashResult(result);
                        break;
                    case TYPE_CLOSECASH:
                        parseCloseCashResult(result);
                        break;
                    }
                }
                break;
            case ServerLoader.ERR:
                Log.e(LOG_TAG, "URLTextGetter error", (Exception)msg.obj);
                SyncUtils.notifyListener(listener, CONNECTION_FAILED, msg.obj);
                finish();
                return;
            }
        }
    }

}
