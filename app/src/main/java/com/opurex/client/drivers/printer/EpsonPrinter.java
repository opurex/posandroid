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
package com.opurex.client.drivers.printer;

import android.graphics.Bitmap;
import com.epson.epos2.Epos2Exception;
import com.epson.epos2.printer.Printer;
import com.epson.epos2.printer.PrinterStatusInfo;
import com.epson.epos2.printer.ReceiveListener;
import com.opurex.client.drivers.utils.DeviceManagerEvent;
import com.opurex.client.drivers.utils.DeviceManagerEventListener;

/** Epson ethernet printer. */
public class EpsonPrinter implements com.opurex.client.drivers.printer.Printer, ReceiveListener
{
    public static final int CTX_ETH = 0;
    public static final int CTX_BLUETOOTH = 1;
    public static final int CTX_USB = 2;

    private String address;
    /** Exception thrown while building the printer. Reported in later uses. */
    private Epos2Exception createError;
    /** Exception thrown while printing. Reported on flush. */
    private Epos2Exception printError;
    private com.epson.epos2.printer.Printer printer;
    private DeviceManagerEventListener listener;
    private boolean forceDisconnected;

    public EpsonPrinter(int ctx, String address, String model, DeviceManagerEventListener listener) {
        switch (ctx) {
            case CTX_BLUETOOTH:
                this.address = "BT:" + address;
                break;
            case CTX_ETH:
            default:
                this.address = "TCP:" + address;
                break;
        }
        this.listener = listener;
        try {
            int modelConst = 0;
            // Values must match those in values.xml
            switch (model) {
                case "TM-T70":
                    modelConst = Printer.TM_T70;
                    break;
                case "TM-T88V":
                    modelConst = Printer.TM_T88;
                    break;
                case "TM-P60":
                    modelConst = Printer.TM_P60;
                    break;
                case "TM-U220":
                    modelConst = Printer.TM_U220;
                    break;
                case "TM-T20":
                default:
                    modelConst = Printer.TM_T20;
            }
            this.printer = new Printer(modelConst, Printer.MODEL_ANK, null);
            this.printer.setReceiveEventListener(this);
        } catch (Epos2Exception e) {
            this.createError = e;
            this.printer = null;
        }
    }

    public String getAddress() {
        return this.address;
    }

    private void notifyListener(int event) { this.notifyListener(event, null); }
    private void notifyListener(int event, Object data) {
        if (this.listener != null) {
            this.listener.onDeviceManagerEvent(null, new DeviceManagerEvent(event, data));
        }
    }

    @Override // From Printer
    public void connect() {
        if (this.printer == null) {
            this.notifyListener(DeviceManagerEvent.PrinterConnectFailure, this.createError);
            return;
        }
        try {
            this.printer.connect(this.address, Printer.PARAM_DEFAULT);
            this.forceDisconnected = false;
            this.notifyListener(DeviceManagerEvent.PrinterConnected);
        } catch (Epos2Exception e) {
            this.notifyListener(DeviceManagerEvent.PrinterConnectFailure);
        }
    }

    @Override // From Printer
    public void disconnect() {
        if (this.printer == null) {
            this.notifyListener(DeviceManagerEvent.PrinterDisconnected);
            return;
        }
        try {
            this.printer.disconnect();
            this.notifyListener(DeviceManagerEvent.PrinterDisconnected);
        } catch (Epos2Exception e) {
            this.notifyListener(DeviceManagerEvent.PrinterDisconnectFailure);
        }
    }

    @Override // From Printer
    public void forceDisconnect() {
        try {
            this.printer.disconnect();
        } catch (Epos2Exception e) {
        }
        this.forceDisconnected = true;
        this.notifyListener(DeviceManagerEvent.PrinterDisconnected);
    }

    @Override // From Printer
    public boolean isConnected() {
        if (this.printer == null) {
            return false;
        }
        if (this.forceDisconnected) {
            return false;
        }
        PrinterStatusInfo status = this.printer.getStatus();
        return status.getConnection() == Printer.TRUE && status.getOnline() == Printer.TRUE;
    }

    @Override // From Printer
    public void initPrint() {
        if (this.printer == null) {
            return;
        }
        try {
            this.printer.beginTransaction();
            this.printer.clearCommandBuffer();
        } catch (Epos2Exception e) {
            this.printError = e;
        }
    }

    @Override // From Printer
    public void printLine() {
        if (this.printer == null || this.printError != null) {
            return;
        }
        try {
            this.printer.addFeedLine(1);
        } catch (Epos2Exception e) {
            this.printError = e;
        }
    }

    @Override // From Printer
    public void printLine(String data) {
        if (this.printer == null || this.printError != null) {
            return;
        }
        try {
            this.printer.addText(data.replace("\\", "\\\\"));
            this.printer.addFeedLine(1);
        } catch (Epos2Exception e) {
            this.printError = e;
        }
    }

    @Override // From Printer
    public void printBitmap(Bitmap bitmap) {
        if (this.printer == null || this.printError != null) {
            return;
        }
        try {
            this.printer.addImage(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(),
                    Printer.PARAM_DEFAULT, Printer.MODE_GRAY16,
                    Printer.PARAM_DEFAULT, Printer.PARAM_DEFAULT,
                    Printer.PARAM_DEFAULT);
        } catch (Epos2Exception e) {
            this.printError = e;
        }
    }

    @Override // From Printer
    public void cut() {
        if (this.printer == null || this.printError != null) {
            return;
        }
        try {
            this.printer.addCut(Printer.PARAM_DEFAULT);
        } catch (Epos2Exception e) {
            this.printError = e;
        }
    }

    @Override // From Printer
    public void flush() {
        if (this.printer == null) {
            return;
        }
        if (this.printError != null) {
            this.notifyListener(DeviceManagerEvent.PrintError, this.printError);
            this.printError = null;
        }
        try {
            this.printer.sendData(Printer.PARAM_DEFAULT);
            this.printer.endTransaction();
        } catch (Epos2Exception e) {
            this.notifyListener(DeviceManagerEvent.PrintError, e);
        }
   }

    @Override // From ReceiveListener
    public void onPtrReceive(final Printer printer, final int code, final PrinterStatusInfo status, final String printJobId) {

    }
}
