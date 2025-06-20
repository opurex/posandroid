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
package com.opurex.client;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;

import java.io.IOError;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import com.opurex.client.activities.POSConnectedTrackedActivity;
import com.opurex.client.data.*;
import com.opurex.client.data.Data;
import com.opurex.client.drivers.POSDeviceManager;
import com.opurex.client.drivers.printer.documents.ZTicketDocument;
import com.opurex.client.drivers.utils.DeviceManagerEvent;
import com.opurex.client.models.*;
import com.opurex.client.activities.TrackedActivity;
import com.opurex.client.utils.Error;

public class CloseCash extends POSConnectedTrackedActivity {

    private static final String LOG_TAG = "Opurex/Cash";

    private static final int REQUEST_COUNT = 1;

    private ZTicket zTicket;
    private ListView stockList;
    private ProgressDialog progressDialog;
    private AlertDialog alertDialog;
    private Double cashAmount;
    private boolean printQueued;
    private Timer retryTimer;

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.close_cash);
        this.retryTimer = new Timer();
        // Set z ticket info
        this.zTicket = new ZTicket(this);
        String labelPayment, valuePayment, labelTaxes, valueTaxes;
        labelPayment = valuePayment = labelTaxes = valueTaxes = "";
        Map<Integer, PaymentDetail> payments = zTicket.getPayments();
        Map<Double, Double> taxBases = zTicket.getTaxBases();
        // Show z ticket data
        DecimalFormat currFormat = new DecimalFormat("#0.00");
        for (Integer m : payments.keySet()) {
            PaymentMode pm = Data.PaymentMode.get(m);
            labelPayment += pm.getLabel() + "\n";
            valuePayment += addValuePaymentMode(currFormat, payments.get(m));
        }
        ((TextView) this.findViewById(R.id.z_payment_total_value))
                .setText(currFormat.format(zTicket.getTotal()) + " €");
        DecimalFormat rateFormat = new DecimalFormat("##0.#");
        for (Double rate : taxBases.keySet()) {
            labelTaxes += (rateFormat.format(rate * 100)
                    + (rate < 10 ? " " : "") + "%  :  "
                    + currFormat.format(taxBases.get(rate)) + "\n");
            valueTaxes += currFormat.format(taxBases.get(rate) * rate) + " €\n";
        }

        // Set the warning message if any
        if (Data.Session.currentSession(this).hasRunningTickets()) {
            TextView warning = (TextView) this.findViewById(R.id.close_warning);
            warning.setText(R.string.close_running_ticket_message);
        }

        ((TextView) this.findViewById(R.id.z_label_payment_content))
                .setText(labelPayment);
        ((TextView) this.findViewById(R.id.z_value_payment_content))
                .setText(valuePayment);

        ((TextView) this.findViewById(R.id.z_label_taxes_content))
                .setText(labelTaxes);
        ((TextView) this.findViewById(R.id.z_value_taxes_content))
                .setText(valueTaxes);

        ((TextView) this.findViewById(R.id.z_subtotal_value))
                .setText(currFormat.format(zTicket.getSubtotal()) + " €");
        ((TextView) this.findViewById(R.id.z_taxes_taxes_values))
                .setText(currFormat.format(zTicket.getTaxAmount()) + " €");
        ((TextView) this.findViewById(R.id.z_taxes_total_values))
                .setText(currFormat.format(zTicket.getTotal()) + " €");

        this.findViewById(R.id.close).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                requestClose();
            }
        });
    }

    private String addValuePaymentMode(DecimalFormat format, PaymentDetail paymentDetail) {
        String result;
        result = "Income: " + format.format(paymentDetail.getIncome()) + " €\t";
        result += "Outcome: " + format.format(paymentDetail.getOutcome()) + " €\t";
        result += "Total: " + format.format(paymentDetail.getTotal()) + " €";
        return result + "\n";
    }

    /**
     * Undo temporary close operations on current cash.
     */
    private void undoClose() {
        this.cashAmount = null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        this.retryTimer.cancel();
        // Undo checks if closed nicely the new cash doesn't have these data
        // if closed by cancel the current cash may have these data set from
        // close activities
        this.undoClose();
    }

    // Close button callback
    private void requestClose() {
        if (this.cashAmount == null) {
            Intent countCash = new Intent(this, CloseCashCount.class);
            if (this.zTicket.getExpectedCash() != null) {
                countCash.putExtra(CloseCashCount.EXPECTED_AMOUNT_KEY,
                        this.zTicket.getExpectedCash());
            }
            this.startActivityForResult(countCash, REQUEST_COUNT);
            return;
        }
        this.askCloseType();
    }

    /** Show a popup to select the type of close and call closeCash. */
    private void askCloseType() {
        AlertDialog.Builder b = new AlertDialog.Builder(this);
        b.setTitle(R.string.close_type_title);
        b.setItems(new CharSequence[] {
                    this.getResources().getString(R.string.close_type_simple),
                    this.getResources().getString(R.string.close_type_period),
                    this.getResources().getString(R.string.close_type_fyear) },
            new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        int type = Cash.CLOSE_SIMPLE;
                        switch (which) {
                        case 0: closeCash(Cash.CLOSE_SIMPLE); break;
                        case 1: closeCash(Cash.CLOSE_PERIOD); break;
                        case 2: closeCash(Cash.CLOSE_FYEAR); break;
                        }
                    }
                });
        b.show();
    }
    /**
     * Do close checks and effectively close the cash
     */
    private void closeCash(int closeType) {
        try {
            this.closeCashAction(closeType);
        } catch (IOException e) {
            Log.e(LOG_TAG, "Unable to archive cash", e);
            Error.showError(R.string.err_save_cash_register, this);
            return;
        }
        //making final copy for thread use
        final ZTicket zticket = this.zTicket;
        final CashRegister cashRegister = Data.CashRegister.current(this);
        print(zticket, cashRegister);
    }

    private void print(final ZTicket zTicket, final CashRegister cashRegister) {
        if (!this.printZTicket(zTicket, cashRegister)) {
            this.askReprintOrLeave(R.string.print_no_connexion);
        }
    }

    private void showProgressDialog() {
        this.progressDialog = new ProgressDialog(this);
        this.progressDialog.setIndeterminate(true);
        this.progressDialog.setCancelable(false);
        this.progressDialog.setMessage(this.getString(R.string.print_printing));
        this.progressDialog.show();
        this.retryTimer.schedule(new TimerTask() {
            public void run() {
                askReprintOrLeave(R.string.print_ask_retry);
            }
        }, 5000);
    }

    private void closeCashAction(int closeType) throws IOException {
        Data.Cash.currentCash(this).closeNow(this.zTicket, closeType,
                this.cashAmount, this.zTicket.getExpectedCash());
        this.zTicket.getCash().closeNow(this.zTicket, closeType,
                this.cashAmount, this.zTicket.getExpectedCash()); // probably the same object
        Data.Cash.dirty = true;
        // Archive and create a new cash
        CashArchive.archiveCurrent(this.zTicket);
        Data.Cash.clear(this);
        Cash nextCash = this.zTicket.getCash().next();
        Data.Cash.setCash(nextCash);
        Data.Receipt.clear(this);
        try {
            Data.Cash.save();
        } catch (IOError e) {
            Log.e(LOG_TAG, "Unable to save cash", e);
            Error.showError(R.string.err_save_cash, this);
        }
        Data.Session.clear(this);
    }

    public static void close(TrackedActivity caller) {
        Intent i = new Intent(caller, CloseCash.class);
        caller.startActivity(i);
    }

    /**
     * On check result
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode,
                                    Intent data) {
        switch (resultCode) {
            case Activity.RESULT_CANCELED:
                // Check canceled, undo close
                this.undoClose();
                break;
            case Activity.RESULT_OK:
                if (requestCode == REQUEST_COUNT) {
                    double amount = data.getDoubleExtra(CloseCashCount.AMOUNT_KEY, -1.0);
                    if (amount == -1.0) {
                        // This is an error
                        this.undoClose();
                        return;
                    }
                    this.cashAmount = amount;
                }
                // Continue close process
                this.requestClose();
                break;
        }
    }

    @Override
    public void onDeviceManagerEvent(POSDeviceManager manager, DeviceManagerEvent event) {
        switch (event.what) {
            case DeviceManagerEvent.PrintError:
                OpurexPOS.Log.d("Unable to connect to printer");
                askReprintOrLeave(R.string.printer_failure);
                break;
            case DeviceManagerEvent.PrintQueued:
                this.printQueued = true;
                showProgressDialog();
                break;
            case DeviceManagerEvent.PrintDone:
                Object extra = event.getExtra();
                if (extra != null && (extra instanceof ZTicketDocument)) {
                    retryTimer.cancel();
                    retryTimer = new Timer();
                    dismissProgressDialog();
                    dismissAlertDialog();
                    Start.backToStart(this);
                }
                break;
            default:
                OpurexPOS.Log.d("Unhandled DeviceManagerEvent " + event);
        }
    }

    private void askReprintOrLeave(final int messageId) {
        reconnect();
        dismissProgressDialog();
        dismissAlertDialog();
        final ZTicket zTicket = this.zTicket;
        final CashRegister cashRegister = Data.CashRegister.current(this);
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(OpurexPOS.getStringResource(messageId));
        builder.setCancelable(false);
        builder.setPositiveButton(R.string.retry, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                if (printQueued) {
                    showProgressDialog();
                } else {
                    print(zTicket, cashRegister);
                }
            }
        });
        builder.setNegativeButton(R.string.close_anyway, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                CloseCash.this.backToStart();
            }
        });
        runOnUiThread(new Runnable() {
            public void run() {
                alertDialog = builder.create();
                alertDialog.show();
            }
        });
    }

    private void backToStart() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Start.backToStart(CloseCash.this);
            }
        });
    }

    private void dismissAlertDialog() {
        if (alertDialog != null){
            alertDialog.dismiss();
            alertDialog = null;
        }
    }

    private void dismissProgressDialog() {
        if (this.progressDialog != null) {
            this.progressDialog.dismiss();
            this.progressDialog = null;
        }
        this.retryTimer.cancel();
        this.retryTimer = new Timer();
    }
}
