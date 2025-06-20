package com.opurex.client.drivers.printer;

import android.graphics.Bitmap;

import com.opurex.client.drivers.utils.DeviceManagerEvent;
import com.opurex.client.drivers.utils.DeviceManagerEventListener;
import com.opurex.client.models.Discount;

/**
 * Created by nanosvir on 04 Jan 16.
 */
public class EmptyPrinter implements Printer {

    private boolean connected;
    private DeviceManagerEventListener listener;

    public EmptyPrinter(DeviceManagerEventListener listener) {
        this.listener = listener;
    }

    public String getAddress() {
        return "";
    }

    private void notifyListener(int event) {
        this.notifyListener(event, null);
    }
    private void notifyListener(int event, Object data) {
        if (this.listener != null) {
            this.listener.onDeviceManagerEvent(null, new DeviceManagerEvent(event, data));
        }
    }

    @Override
    public boolean isConnected() {
        return this.connected;
    }

    @Override
    public void connect() {
        this.connected = true;
        this.notifyListener(DeviceManagerEvent.PrinterConnected);
    }

    @Override
    public void disconnect() {
        this.connected = false;
        this.notifyListener(DeviceManagerEvent.PrinterDisconnected);
    }

    public void forceDisconnect() {
        this.disconnect();
    }

    public void initPrint() {}
    public void printLogo() {}
    public void printHeader() {}
    public void printFooter() {}
    public void printLine() {}
    public void printLine(String data) {}
    public void printDiscount(Discount discount) {}
    public void printBitmap(Bitmap bitmap) {}
    public void cut() {}
    public void flush() {}

}
