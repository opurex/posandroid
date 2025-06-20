package com.opurex.client.drivers;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Looper;
import com.mpowa.android.sdk.powapos.PowaPOS;
import com.mpowa.android.sdk.powapos.common.base.PowaEnums;
import com.mpowa.android.sdk.powapos.common.dataobjects.PowaDeviceObject;
import com.mpowa.android.sdk.powapos.core.PowaPOSEnums;
import com.mpowa.android.sdk.powapos.core.abstracts.PowaScanner;
import com.mpowa.android.sdk.powapos.drivers.s10.PowaS10Scanner;
import com.mpowa.android.sdk.powapos.drivers.tseries.PowaTSeries;
import com.opurex.client.OpurexPOS;
import com.opurex.client.drivers.utils.DeviceManagerEvent;
import com.opurex.client.drivers.utils.DeviceManagerEventListener;
import com.opurex.client.drivers.printer.BasePowaPOSCallback;
import com.opurex.client.drivers.printer.PowaPrinter;
import com.opurex.client.drivers.printer.documents.PrintableDocument;
import com.opurex.client.utils.IntegerHolder;
import com.opurex.client.utils.exception.ScannerNotFoundException;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Created by svirch_n on 22/01/16.
 */
public class PowaDeviceManager implements POSDeviceManager {

    private static final String CALLBACK_TAG = "PowaDeviceManager/Callback";
    protected PowaPrinterCommand command = new PowaPrinterCommand();
    PowaPrinter printer = new PowaPrinter(command);
    PowaScanner scanner;
    PowaPOS powa;
    PowaTSeries printerDevice;
    private TransPowaCallback callback;
    private DeviceManagerEventListener listener;

    public PowaDeviceManager(DeviceManagerEventListener listener) {
        Context context = OpurexPOS.getAppContext();
        this.callback = new TransPowaCallback(listener);
        this.listener = listener;
        final PowaPOS powa = new PowaPOS(context, this.callback);
        this.powa = powa;
        this.printerDevice = new PowaTSeries(context, false);
        this.scanner = new PowaS10Scanner(context);
    }

    @Override
    public String getName() {
        return "PowaPos";
    }

    @Override
    public void connect() {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                powa.addPeripheral(printerDevice);
                powa.addPeripheral(scanner);
                connectBluetooth(powa);
                powa.requestMCURotationSensorStatus();
            }
        });
    }

    public void connectBluetooth() {
        if (powa != null) {
            connectBluetooth(powa);
        }
    }

    /**
     * Connect the first bluetooth paired POWA scan
     * only if no scans are currently connected
     * @param powa the POS
     */
    private void connectBluetooth(PowaPOS powa) {
        if (powa != null) {
            PowaScanner scanner = powa.getScanner();
            if (scanner != null && scanner.getSelectedScanner() == null) {
                //If no scanner is selected
                try {
                    scanner.selectScanner(getScanner(powa));
                    scanner.setScannerAutoScan(OpurexPOS.getConf().scannerIsAutoScan());
                } catch (ScannerNotFoundException ignore) {
                }
            }
        }
    }

    private PowaDeviceObject getScanner(final PowaPOS powa) throws ScannerNotFoundException {
        PowaDeviceObject scannerSelected = null;
        List<PowaDeviceObject> scanners = powa.getAvailableScanners();
        if (scanners.size() > 0) {
            scannerSelected = scanners.get(0);
        } else {
            throw new ScannerNotFoundException();
        }
        return scannerSelected;
    }

    @Override
    public void disconnect() {
        if (powa != null) {
            powa.dispose();
            //No handler disconnected with dispose
            this.callback.notifyEvent(DeviceManagerEvent.PrinterDisconnected);
            this.callback.notifyEvent(DeviceManagerEvent.ScannerDisconnected);
        }
    }

    public void wasDisconnected() {
        powa.dispose();
    }

    @Override
    public boolean hasCashDrawer() {
        return true;
    }

    @Override
    public void openCashDrawer() {
        powa.openCashDrawer();
    }

    @Override
    public void print(PrintableDocument doc) {
        if (this.printer.isConnected()) {
            Context context = OpurexPOS.getAppContext();
            doc.print(this.printer, context);
            this.listener.onDeviceManagerEvent(this, new DeviceManagerEvent(DeviceManagerEvent.PrintDone, doc));
        }
    }

    public boolean isManaging(Object o) {
        return false;
    }

    private class TransPowaCallback extends BasePowaPOSCallback {

        protected DeviceManagerEventListener listener;

        public TransPowaCallback(DeviceManagerEventListener listener) {
            this.listener = listener;
        }

        public void notifyEvent(int event) { this.notifyEvent(event, null); }
        public void notifyEvent(int event, Object data) {
            if (this.listener != null) {
                this.listener.onDeviceManagerEvent(PowaDeviceManager.this, new DeviceManagerEvent(event, data));
            }
        }

        @Override
        public void onScannerConnectionStateChanged(PowaEnums.ConnectionState newState) {
            switch (newState) {
                case CONNECTED:
                    this.notifyEvent(DeviceManagerEvent.ScannerConnected);
                    break;
                case DISCONNECTED:
                    this.notifyEvent(DeviceManagerEvent.ScannerDisconnected);
                    break;
            }
        }

        @Override
        public void onPrintJobResult(PowaPOSEnums.PrintJobResult result) {
            PowaDeviceManager.this.command.printingDone(new DeviceManagerEvent(DeviceManagerEvent.PrintDone, result));
        }

        @Override
        public void onRotationSensorStatus(PowaPOSEnums.RotationSensorStatus rotationSensorStatus) {
            this.notifyEvent(DeviceManagerEvent.BaseRotation, rotationSensorStatus);
        }

        @Override
        public void onScannerRead(String barcode) {
            this.notifyEvent(DeviceManagerEvent.ScannerReader, barcode);
        }

        @Override
        public void onMCUConnectionStateChanged(PowaEnums.ConnectionState state) {
            if (state.equals(PowaEnums.ConnectionState.CONNECTED)) {
                powa.requestMCUSystemConfiguration();
                this.notifyEvent(DeviceManagerEvent.PrinterConnected);
            } else if (state.equals(PowaEnums.ConnectionState.DISCONNECTED)) {
                this.notifyEvent(DeviceManagerEvent.PrinterDisconnected);
            }
        }

        @Override
        public void onMCUSystemConfiguration(Map<String, String> map) {
            super.onMCUSystemConfiguration(map);
            this.notifyEvent(100);
        }
    }

    public class PowaPrinterCommand {

        LinkedList<IntegerHolder> pendingPrints = new LinkedList<>();
        IntegerHolder current = new IntegerHolder();

        public void printingDone(DeviceManagerEvent printingDoneEvent) {
            if (pendingPrints.size() > 0) {
                IntegerHolder first = pendingPrints.getFirst();
                first.decrease();
                if (first.isEmpty()) {
                    callback.notifyEvent(DeviceManagerEvent.PrintDone);
                    pendingPrints.removeFirst();
                }
            }
        }

        public boolean isConnected() {
            return printerDevice.isDriverConnected();
        }

        public void printText(String string) {
            printerDevice.printText(string);
        }

        public void startReceipt() {
            current.increase();
            printerDevice.startReceipt();
        }

        public void printImage(Bitmap bitmap) {
            current.increase();
            printerDevice.printImage(bitmap);
        }

        public void printReceipt() {
            printerDevice.printReceipt();
            pendingPrints.add(current);
            current = new IntegerHolder();
        }

        public void connect() {
        }

        public void disconnect() {
        }
    }
}
