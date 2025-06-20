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
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.List;

import com.opurex.client.activities.POSConnectedTrackedActivity;
import com.opurex.client.data.CashArchive;
import com.opurex.client.data.Data;
import com.opurex.client.drivers.POSDeviceManager;
import com.opurex.client.drivers.utils.DeviceManagerEvent;
import com.opurex.client.models.ZTicket;
import com.opurex.client.utils.Error;
import com.opurex.client.utils.exception.loadArchiveException;
import com.opurex.client.widgets.ZTicketAdapter;

public class ZSelect extends POSConnectedTrackedActivity
        implements AdapterView.OnItemClickListener {

    private static final String LOG_TAG = "Opurex/ZSelect";

    private ListView list;
    private ProgressDialog printing;
    private AlertDialog alertDialog;
    private ZTicket selectedZ;
    private List<ZTicket> zs;

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);
        this.zs = new ArrayList<ZTicket>();
        int archiveCount = CashArchive.getArchiveCount(this);
        for (int i = 0; i < archiveCount; i++) {
            try {
                ZTicket z = (ZTicket) CashArchive.loadArchive(i)[0];
                this.zs.add(z);
            } catch (loadArchiveException e) {

            }
        }
        // Set views
        setContentView(R.layout.z_select);
        this.list = (ListView) this.findViewById(R.id.z_list);
        this.list.setAdapter(new ZTicketAdapter(this.zs));
        this.list.setOnItemClickListener(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onItemClick(AdapterView parent, View v,
                            int position, long id) {
        final ZTicket z = this.zs.get(position);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        String label = this.getString(R.string.z_sequence)
                 + " " + z.getCash().getSequence();
        builder.setTitle(label);
        String[] items = new String[]{this.getString(R.string.print)};
        builder.setItems(items, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case 0:
                        selectedZ = z;
                        print(z);
                        break;
                }
            }
        });
        builder.show();
    }

    private void print(final ZTicket z) {
        showProgressDialog();
        if (!this.printZTicket(z, Data.CashRegister.current(this))) {
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
                selectedZ = null;
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
                selectedZ = null;
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
