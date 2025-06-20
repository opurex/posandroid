package com.opurex.client.drivers.mpop;

import android.graphics.Bitmap;

import com.opurex.client.OpurexPOS;
import com.opurex.client.drivers.printer.Printer;
import com.opurex.client.utils.StringUtils;

/**
 * Created by svirch_n on 23/12/15.
 */
public class MPopPrinter implements Printer {

    protected MPopCommandDataList mPopCommand = new MPopCommandDataList();
    protected String textToPrint = "";
    protected MPopPrinterCommand printerCommand;

    public MPopPrinter(MPopPrinterCommand printerCommand) {
        super();
        this.printerCommand = printerCommand;
    }

    public String getAddress() {
        return "";
    }

    @Override
    public void connect() {
    }

    @Override
    public void disconnect() {
    }

    public void forceDisconnect() {
    }

    @Override
    public void initPrint() { }

    @Override
    public void printLine(String data) {
        data = StringUtils.formatAscii(data);
        this.textToPrint += data + "\r\n";
    }

    @Override
    public void printLine() {
        this.textToPrint += "\r\n";
    }

    @Override
    public void flush() {
        this.mPopCommand.add(MPopFunction.Printer.data(this.textToPrint));
        this.textToPrint = "";
    }

    @Override
    public void printBitmap(Bitmap bitmap) {
        flush();
        this.mPopCommand.add(MPopFunction.Printer.image(bitmap));
    }

    @Override
    public void cut() {
        this.mPopCommand.add(MPopFunction.Printer.cut());
        byte[] bytes = this.mPopCommand.getByteArray();
        this.mPopCommand.clear();
        MPopCommunication.Result result = printerCommand.sendCommand(bytes);
        OpurexPOS.Log.d(result.getAsText());
    }

    public boolean isConnected() {
        return printerCommand.isConnected();
    }
}
