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

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import com.opurex.client.activities.POSConnectedTrackedActivity;
import com.opurex.client.data.Data;
import com.opurex.client.drivers.POSDeviceManager;
import com.opurex.client.drivers.utils.DeviceManagerEvent;
import com.opurex.client.models.Receipt;
import com.opurex.client.utils.Error;
import com.opurex.client.widgets.ReceiptsAdapter;

public class ReceiptSelect extends POSConnectedTrackedActivity
        implements AdapterView.OnItemClickListener {

    private static final String LOG_TAG = "Opurex/ReceiptSelect";
    public static final String TICKET_ID_KEY = "ticketId";

    private ListView list;
    private ProgressDialog printing;
    private AlertDialog alertDialog;
    private Receipt selectedReceipt;

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);
        // Set views
        setContentView(R.layout.receipt_select);
        this.list = (ListView) this.findViewById(R.id.receipts_list);
        this.list.setAdapter(new ReceiptsAdapter(Data.Receipt.getReceipts(this)));
        this.list.setOnItemClickListener(this);
        // Init printer connection
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onItemClick(AdapterView parent, View v,
                            int position, long id) {
        final Receipt receipt = Data.Receipt.getReceipts(this).get(position);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        String label = this.getString(R.string.ticket_label,
                receipt.getTicket().getTicketId());
        builder.setTitle(label);
        String[] items = new String[]{this.getString(R.string.print), this.getString(R.string.refund)};
        builder.setItems(items, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case 0:
                        selectedReceipt = receipt;
                        print(receipt);
                        break;
                    case 1:
                        Intent intent = new Intent().putExtra(ReceiptSelect.TICKET_ID_KEY, receipt.getTicket().getId());
                        setResult(Transaction.PAST_TICKET_FOR_RESULT, intent);
                        finish();
                        break;
                }
            }
        });
        builder.show();
    }

    private void refreshList() {
        if (Data.Receipt.hasReceipts()) {
            ReceiptSelect.this.list.setAdapter(new ReceiptsAdapter(Data.Receipt.getReceipts(this)));
        } else {
            ReceiptSelect.this.finish();
        }
    }

    private void print(final Receipt receipt) {
        showProgressDialog();
        if (!this.printReceipt(receipt)) {
            this.askReprint();
        }
    }

    private void showProgressDialog() {
        dismissPrintingProgressDialog();
        dismissAlertDialog();
        this.printing = new ProgressDialog(this);
        this.printing.setIndeterminate(true);
        this.printing.setMessage(this.getString(R.string.print_printing));
        this.printing.show();
    }

    private void askReprint() {
        reconnect();
        dismissPrintingProgressDialog();
        dismissAlertDialog();
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(OpurexPOS.getStringResource(R.string.print_ask_retry));
        builder.setPositiveButton(R.string.retry, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
                askReprint();
            }
        });
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
                selectedReceipt = null;
            }
        });
        builder.show();
    }

    private void dismissAlertDialog() {
        if (alertDialog != null) {
            alertDialog.dismiss();
            alertDialog = null;
        }
    }

    @Override
    public void onDeviceManagerEvent(POSDeviceManager manager, DeviceManagerEvent event) {
        switch (event.what) {
            case DeviceManagerEvent.PrintError:
                OpurexPOS.Log.d("Unable to connect to printer");
                Error.showError(R.string.printer_has_failed, this);
                break;
            case DeviceManagerEvent.PrintQueued:
                askReprint();
                break;
            case DeviceManagerEvent.PrintDone:
                dismissPrintingProgressDialog();
                dismissAlertDialog();
                selectedReceipt = null;
                break;
            default:
                OpurexPOS.Log.d("Uncaught event n°" + event.what);
        }
    }

    private void dismissPrintingProgressDialog() {
        if (this.printing != null) {
            this.printing.dismiss();
            this.printing = null;
        }
    }
}
