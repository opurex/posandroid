package com.opurex.client.drivers.mpop;

import android.content.Context;
import com.starmicronics.starioextension.starioextmanager.StarIoExtManager;
import com.starmicronics.starioextension.starioextmanager.*;
import com.opurex.client.OpurexPOS;
import com.opurex.client.drivers.POSDeviceManager;
import com.opurex.client.drivers.printer.documents.PrintableDocument;
import com.opurex.client.drivers.utils.DeviceManagerEvent;
import com.opurex.client.drivers.utils.DeviceManagerEventListener;

/**
 * Created by svirch_n on 23/12/15.
 */
public class MPopDeviceManager implements POSDeviceManager {

    public static final int TIMEOUT = 10000;
    MPopPrinter mPopPrinter;
    StarIoExtManager manager;
    private DeviceManagerEventListener listener;
    private final MPopPrinterCommand printerCommand = new PrinterCommandWithManager();

    public MPopDeviceManager(DeviceManagerEventListener listener) {
        super();
        manager = new StarIoExtManager(StarIoExtManager.Type.WithBarcodeReader, OpurexPOS.getConfiguration().getPrinterModel(), "", TIMEOUT, OpurexPOS.getAppContext());
        mPopPrinter = new MPopPrinter(printerCommand);
        this.manager.setListener(new MPopInnerListener(listener));
        this.listener = listener;
    }

    @Override
    public String getName() {
        return "Star MPop";
    }

    @Override
    public void connect() {
        this.manager.connect();
    }

    @Override
    public void disconnect() {
        this.manager.disconnect();
    }

    public void wasDisconnected() {
        this.manager.disconnect();
    }

    @Override
    public void print(PrintableDocument doc) {
        if (this.mPopPrinter.isConnected()) {
            Context context = OpurexPOS.getAppContext();
            doc.print(this.mPopPrinter, context);
            this.listener.onDeviceManagerEvent(this, new DeviceManagerEvent(DeviceManagerEvent.PrintDone, doc));
        }
    }

    @Override
    public void openCashDrawer() {
        MPopManager.openDrawer();
    }

    public boolean isManaging(Object o) {
        return false;
    }

    public class PrinterCommandWithManager implements MPopPrinterCommand {
        public MPopCommunication.Result sendCommand(byte[] data) {
            return MPopCommunication.sendCommands(data, manager.getPort());
        }

        @Override
        public boolean isConnected() {
            return manager.getPrinterOnlineStatus().equals(StarIoExtManager.Status.PrinterOnline);
        }
    }

    public class PrinterCommandClassical implements MPopPrinterCommand {
        public MPopCommunication.Result sendCommand(byte[] data) {
            return MPopCommunication.sendCommands(data, OpurexPOS.getConfiguration().getPrinterModel(), "", TIMEOUT);
        }

        @Override
        public boolean isConnected() {
            return true;
        }
    }

    @Override
    public boolean hasCashDrawer() {
        return true;
    }

    private class MPopInnerListener extends StarIoExtManagerListener {

        protected DeviceManagerEventListener listener;

        public MPopInnerListener(DeviceManagerEventListener listener) {
            this.listener = listener;
        }

        private void notifyEvent(int event) { this.notifyEvent(event, null); }
        private void notifyEvent(int event, Object data) {
            if (this.listener != null) {
                this.listener.onDeviceManagerEvent(MPopDeviceManager.this, new DeviceManagerEvent(event, data));
            }
        }

        @Override
        public void didCashDrawerOpen() {
            super.didCashDrawerOpen();
            this.notifyEvent(DeviceManagerEvent.CashDrawerOpened);
        }

        @Override
        public void didCashDrawerClose() {
            super.didCashDrawerClose();
            this.notifyEvent(DeviceManagerEvent.CashDrawerClosed);
        }

        @Override
        public void didBarcodeReaderConnect() {
            super.didBarcodeReaderConnect();
            this.notifyEvent(DeviceManagerEvent.ScannerConnected);
        }

        @Override
        public void didBarcodeReaderDisconnect() {
            super.didBarcodeReaderDisconnect();
            this.notifyEvent(DeviceManagerEvent.ScannerDisconnected);
        }

        @Override
        public void didPrinterOnline() {
            super.didPrinterOnline();
            this.notifyEvent(DeviceManagerEvent.PrinterConnected);
        }

        @Override
        public void didPrinterOffline() {
            super.didPrinterOffline();
            this.notifyEvent(DeviceManagerEvent.PrinterDisconnected);
        }

        @Override
        public void didBarcodeReaderImpossible() {
            super.didBarcodeReaderImpossible();
            this.notifyEvent(DeviceManagerEvent.ScannerFailure);
        }

        @Override
        public void didBarcodeDataReceive(byte[] bytes) {
            super.didBarcodeDataReceive(bytes);
            String grossString = new String(bytes);
            String formatedString = grossString.replaceAll("[\r\n]+$", "");
            this.notifyEvent(DeviceManagerEvent.ScannerReader, formatedString);
        }
    }
}
