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

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.graphics.Bitmap;

import com.sewoo.jpos.command.ESCPOS;
import com.sewoo.jpos.command.ESCPOSConst;
import com.sewoo.jpos.printer.ESCPOSPrinter;
import com.sewoo.port.android.BluetoothPort;
import com.sewoo.request.android.RequestHandler;
import com.opurex.client.drivers.utils.DeviceManagerEvent;
import com.opurex.client.drivers.utils.DeviceManagerEventListener;

import java.io.IOException;

public class LKPXXPrinter implements Printer {

    private static final char ESC = ESCPOS.ESC;
    private static final char LF = ESCPOS.LF;
    private static final String CUT = ESC + "|#fP";

    private ESCPOSPrinter printer;
    private BluetoothSocket sock;
    private BluetoothPort port;
    private Thread sewooHandlerThread;
    protected String address;
    private DeviceManagerEventListener listener;
    private boolean forceDisconnected;

    public LKPXXPrinter(String address, DeviceManagerEventListener listener) {
        super();
        this.address = address;
        this.port = BluetoothPort.getInstance();
        this.printer = new ESCPOSPrinter();
        this.listener = listener;
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
    /** Connect the printer.
     * It will send a message to the device manager listener:
     * - PrinterConnected
     * - PrinterConnectFailure with the exception thrown. */
    @Override
	public void connect() {
        try {
            BluetoothAdapter btAdapt = BluetoothAdapter.getDefaultAdapter();
            BluetoothDevice dev = btAdapt.getRemoteDevice(this.address.toUpperCase());
            port.connect(dev);
            if (port.isConnected()) {
                this.forceDisconnected = false;
                createHandler();
                this.notifyListener(DeviceManagerEvent.PrinterConnected);
            } else {
                this.notifyListener(DeviceManagerEvent.PrinterConnectFailure);
            }
        } catch (IllegalArgumentException | IOException e) {
            if (this.listener != null) {
                this.notifyListener(DeviceManagerEvent.PrinterConnectFailure, e);
            } else {
                e.printStackTrace();
            }
        }
    }

    private void createHandler() {
        RequestHandler sewooHandler = new RequestHandler();
        sewooHandlerThread = new Thread(sewooHandler);
        sewooHandlerThread.start();
    }


    private void removeHandler() {
        if ((sewooHandlerThread != null) && (sewooHandlerThread.isAlive())) {
            sewooHandlerThread.interrupt();
        }
    }

    @Override
    public void disconnect() {
        try {
            port.disconnect();
            removeHandler();
            this.notifyListener(DeviceManagerEvent.PrinterDisconnected);
        } catch (IOException | InterruptedException e) {
            this.notifyListener(DeviceManagerEvent.PrinterDisconnectFailure, e);
        }
    }

    @Override
    public void forceDisconnect() {
        try {
            port.disconnect();
        } catch (IOException | InterruptedException e) {
        }
        removeHandler();
        this.forceDisconnected = true;
        this.notifyListener(DeviceManagerEvent.PrinterDisconnected);
    }

    @Override
    public void initPrint() { }

    @Override
	public void printLine(String data) {
        String ascii = data.replace("é", "e");
        ascii = ascii.replace("è", "e");
        ascii = ascii.replace("ê", "e");
        ascii = ascii.replace("ë", "e");
        ascii = ascii.replace("à", "a");
        ascii = ascii.replace("ï", "i");
        ascii = ascii.replace("ô", "o");
        ascii = ascii.replace("ç", "c");
        ascii = ascii.replace("ù", "u");
        ascii = ascii.replace("É", "E");
        ascii = ascii.replace("È", "E");
        ascii = ascii.replace("Ê", "E");
        ascii = ascii.replace("Ë", "E");
        ascii = ascii.replace("À", "A");
        ascii = ascii.replace("Ï", "I");
        ascii = ascii.replace("Ô", "O");
        ascii = ascii.replace("Ç", "c");
        ascii = ascii.replace("Ù", "u");
        ascii = ascii.replace("€", "E");
        try {
            this.printer.printNormal(ascii + LF);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void printBitmap(Bitmap bitmap) {
        try {
            this.printer.printBitmap(bitmap, ESCPOSConst.LK_ALIGNMENT_CENTER, ESCPOSConst.LK_BITMAP_NORMAL);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
	public void printLine() {
        this.printer.lineFeed(1);
    }

    @Override
    public void cut() {

    }

    @Override
    public void flush() {
        printer.lineFeed(2);
    }

    @Override
    public boolean isConnected() {
        if (this.forceDisconnected) {
            return false;
        }
        return port.isConnected();
    }

}
