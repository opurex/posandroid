package com.opurex.client.drivers;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import com.opurex.client.OpurexPOS;
import com.opurex.client.drivers.printer.Printer;
import com.opurex.client.drivers.printer.EmptyPrinter;
import com.opurex.client.drivers.printer.EpsonPrinter;
import com.opurex.client.drivers.printer.LKPXXPrinter;
import com.opurex.client.drivers.printer.WoosimPrinter;
import com.opurex.client.drivers.printer.documents.PrintableDocument;
import com.opurex.client.drivers.utils.DeviceManagerEvent;
import com.opurex.client.drivers.utils.DeviceManagerEventListener;
import com.opurex.client.utils.OpurexConfiguration;

/**
 * Manager for a single printer.
 * Created by svirch_n on 23/12/15.
 */
public class DefaultDeviceManager implements POSDeviceManager, DeviceManagerEventListener
{
    protected String name;
    protected Printer printer;
    protected DeviceManagerEventListener listener;

    DefaultDeviceManager(DeviceManagerEventListener listener, int deviceIndex) {
        OpurexConfiguration conf = OpurexPOS.getConf();
        String prDriver = conf.getPrinterDriver(deviceIndex);
        switch (prDriver) {
        case "LK-PXX":
            this.name = "LK-PXX " + conf.getPrinterAddress(deviceIndex);
            this.printer = new LKPXXPrinter(conf.getPrinterAddress(deviceIndex), this);
            break;
        case "Woosim":
            this.name = "Woosim " + conf.getPrinterAddress(deviceIndex);
            this.printer = new WoosimPrinter(conf.getPrinterAddress(deviceIndex), this);
            break;
        case "EPSON ePOS IP":
            this.name = "EPSON " + conf.getPrinterModel(deviceIndex) + " " + conf.getPrinterAddress(deviceIndex);
            this.printer = new EpsonPrinter(EpsonPrinter.CTX_ETH, conf.getPrinterAddress(deviceIndex), conf.getPrinterModel(deviceIndex), this);
            break;
        case "None":
        default:
            this.name = "Empty";
            this.printer = new EmptyPrinter(this);
        }
        this.listener = listener;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public void print(PrintableDocument doc) {
        if (this.printer.isConnected()) {
            Context context = OpurexPOS.getAppContext();
            doc.print(this.printer, context);
            this.listener.onDeviceManagerEvent(this, new DeviceManagerEvent(DeviceManagerEvent.PrintDone, doc));
        }
    }

    @Override
    public void connect() {
        if (!this.printer.isConnected()) {
            this.printer.connect();
        } else {
            this.listener.onDeviceManagerEvent(this, new DeviceManagerEvent(DeviceManagerEvent.PrinterConnected));
        }
    }

    @Override
    public void disconnect() {
        if (this.printer.isConnected()) {
            this.printer.disconnect();
        } else {
            this.listener.onDeviceManagerEvent(this, new DeviceManagerEvent(DeviceManagerEvent.PrinterDisconnected));
        }
    }

    public void wasDisconnected() {
        this.printer.forceDisconnect();
    }

    @Override
    public void openCashDrawer() { }
    @Override
    public boolean hasCashDrawer() { return false; }

    @Override
    public void onDeviceManagerEvent(POSDeviceManager manager, DeviceManagerEvent event) {
        this.listener.onDeviceManagerEvent(this, event);
    }

    @Override
    public boolean isManaging(Object o) {
        if (o instanceof Printer) {
            Printer p = (Printer) o;
            return p.getAddress().toLowerCase().equals(this.printer.getAddress().toLowerCase());
        }
        if (o instanceof BluetoothDevice) {
            BluetoothDevice d = (BluetoothDevice) o;
            if (this.printer.getAddress().toLowerCase().equals(d.getAddress().toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean equals(Object o) {
        return (o instanceof DefaultDeviceManager) && ((DefaultDeviceManager)o).getName().equals(this.getName());
    }
}
