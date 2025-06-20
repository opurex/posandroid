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


import com.opurex.client.drivers.PowaDeviceManager;
import com.opurex.client.utils.BitmapManipulation;
import com.opurex.client.utils.StringUtils;

public class PowaPrinter implements Printer {

    private PowaDeviceManager.PowaPrinterCommand command;

    public PowaPrinter(PowaDeviceManager.PowaPrinterCommand command) {
        super();
        this.command = command;
    }

    public String getAddress() {
        return "";
    }

    @Override
    public boolean isConnected() {
        return command.isConnected();
    }

    @Override
    public void connect() {
        command.connect();
    }

    @Override
    public void disconnect() {
        command.disconnect();
    }

    @Override
    public void forceDisconnect() {
        command.disconnect();
    }

    @Override
    public void printLine(String data) {
        String ascii = StringUtils.formatAscii(data);
        while (ascii.length() > 32) {
            //Get the last word that fit
            //If no such word exist just cut the 32th character
            int index = (ascii.substring(0, 32)).lastIndexOf(" ");
            if (index == -1 || ascii.charAt(32) == ' ') {
                index = 32;
            }
            String sub = ascii.substring(0, index);
            command.printText("        " + sub + "        ");
            //Remove the useless space at the beginning if exists
            if (ascii.charAt(index) == ' ')
                index++;
            ascii = ascii.substring(index);
        }
        command.printText("        " + ascii + "        ");
    }

    @Override
    public void printLine() {
        command.printText(" ");
    }

    @Override
    public void flush()
    {
        command.printText("");
        command.printText("");
        command.printText("");
        command.printReceipt();
    }

    @Override
    public void printBitmap(Bitmap bitmap) {
        command.printImage(BitmapManipulation.centeredBitmap(bitmap, 572));
    }

    @Override
    public void cut() {
    }

    @Override
    public void initPrint() {
        command.startReceipt();
    }

    protected void printDone() {
        // Handled in PowaCallback
    }
}
