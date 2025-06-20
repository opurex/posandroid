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
package com.opurex.client.drivers.printer.documents;

import android.content.Context;
import android.graphics.BitmapFactory;
import com.opurex.client.OpurexPOS;
import com.opurex.client.R;
import com.opurex.client.drivers.printer.Printer;
import com.opurex.client.drivers.printer.PrinterHelper;

public class TestDocument implements PrintableDocument
{
    public static final String BARCODE_VALUE = "4931036717968";

    public boolean print(Printer printer, Context ctx) {
        if (!printer.isConnected()) {
            return false;
        }
        printer.initPrint();
        PrinterHelper.printLogo(printer, ctx);
        PrinterHelper.printHeader(printer, ctx);
        printer.printBitmap(BitmapFactory.decodeResource(OpurexPOS.getAppContext().getResources(), R.drawable.barcode_test));
        printer.printLine("\nBarcode value is: " + TestDocument.BARCODE_VALUE);
        PrinterHelper.printFooter(printer, ctx);
        printer.cut();
        printer.flush();
        return true;
    }

}
