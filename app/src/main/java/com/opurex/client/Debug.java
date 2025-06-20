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
import android.os.Bundle;
import android.widget.TextView;

import java.io.IOError;
import java.io.StringWriter;
import java.io.PrintWriter;
import java.util.Calendar;
import java.util.Date;

import com.opurex.client.data.*;
import com.opurex.client.models.Cash;
import com.opurex.client.models.Receipt;
import com.opurex.client.models.Ticket;
import com.opurex.client.models.ZTicket;

public class Debug extends Activity {

    private static final String LOG_TAG = "Opurex/Debug";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.debug);
        this.refresh();
    }

    public void refresh() {
        TextView archives = (TextView) this.findViewById(R.id.dbg_archives);
        int archivesCount = CashArchive.getArchiveCount(this);
        String archivesTxt = archivesCount + " archives";
        for (int i = 0; i < archivesCount; i++) {
            try {
                Object[] rawArchive = CashArchive.loadArchive(i);
                ZTicket z = (ZTicket) rawArchive[0];
                archivesTxt += "\n" + z.getCash().getCashRegisterId()
                        + "-" + z.getCash().getSequence() + ": "
                        + z.toJSON().toString();
            } catch (Exception e) {
                archivesTxt += "\nError: " + e.getMessage();
            }
        }
        archives.setText(archivesTxt);
        TextView cash = (TextView) this.findViewById(R.id.dbg_current_cash);
        if (Data.Cash.currentCash(this) == null) {
            cash.setText("Null");
        } else {
            Cash c = Data.Cash.currentCash(this);
            String strCash = "Id: " + c.getId() + "\n";
            strCash += "CashRegId: " + c.getCashRegisterId() + "\n";
            strCash += "Sequence: " + c.getSequence() + "\n";
            strCash += "Open date: ";
            if (c.wasOpened()) {
                Date d = new Date(c.getOpenDate() * 1000);
                Calendar cal = Calendar.getInstance();
                cal.setTime(d);
                strCash += cal.get(Calendar.YEAR) + "-"
                    + (cal.get(Calendar.MONTH) + 1) + "-"
                    + cal.get(Calendar.DAY_OF_MONTH) + " "
                    + cal.get(Calendar.HOUR_OF_DAY) + ":"
                    + cal.get(Calendar.MINUTE) + "\n";
            } else {
                strCash += "not opened\n";
            }
            strCash += "Open cash: ";
            if (c.getOpenCash() == null) {
                strCash += "null\n";
            } else {
                strCash += c.getOpenCash() + "\n";
            }
            strCash += "Close date: ";
            if (c.isClosed()) {
                Date d = new Date(c.getCloseDate() * 1000);
                Calendar cal = Calendar.getInstance();
                cal.setTime(d);
                strCash += cal.get(Calendar.YEAR) + "-"
                    + (cal.get(Calendar.MONTH) + 1) + "-"
                    + cal.get(Calendar.DAY_OF_MONTH) + " "
                    + cal.get(Calendar.HOUR_OF_DAY) + ":"
                    + cal.get(Calendar.MINUTE) + "\n";
            } else {
                strCash += "not closed\n";
            }
            strCash += "Close/expected cash: ";
            if (c.getCloseCash() == null) {
                strCash += "null";
            } else {
                strCash += c.getCloseCash();
            }
            strCash += " / ";
            if (c.getExpectedCash() == null) {
                strCash += "null\n";
            } else {
                strCash += c.getExpectedCash() + "\n";
            }
            strCash += "Continuous: " + c.isContinuous() + "\n";
            strCash += "Dirty: " + Data.Cash.dirty;
            cash.setText(strCash);
        }

        TextView rcpts = (TextView) this.findViewById(R.id.dbg_receipts);
        String strrcpts = Data.Receipt.getReceipts(this).size() + " tickets\n";
        for (Receipt r : Data.Receipt.getReceipts(this)) {
            try {
                strrcpts += r.toJSON().toString(2) + "\n";
            } catch (Exception e) {
                e.printStackTrace();
                StringWriter w = new StringWriter();
                e.printStackTrace(new PrintWriter(w));
                strrcpts += w.toString();
            }
        }
        rcpts.setText(strrcpts);

        TextView session = (TextView) this.findViewById(R.id.dbg_current_session);
        String strSession = Data.Session.currentSession(this).getTickets().size()
            + " tickets\n";
        for (Ticket t : Data.Session.currentSession(this).getTickets()) {
            try {
                strSession += t.toJSON(true).toString(2) + "\n";
            } catch (Exception e) {
                e.printStackTrace();
                StringWriter w = new StringWriter();
                e.printStackTrace(new PrintWriter(w));
                strrcpts += w.toString();
            }
        }
        session.setText(strSession);

        TextView error = (TextView) this.findViewById(R.id.dbg_last_error);
        try {
        String lastError = Data.Crash.customLoad(this);
        error.setText(lastError);
        } catch (IOError e) {
            error.setText(e.getMessage());
        }
    }
}
