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

import android.content.Context;
import com.opurex.client.data.ResourceData;
import com.opurex.client.models.Discount;
import com.opurex.client.utils.BitmapManipulation;
import java.io.IOException;

/** Utility class to provide standard behaviour shortcuts to printers. */
public class PrinterHelper
{
    private PrinterHelper() {}

    public static void printLogo(Printer p, Context ctx) {
        try {
            String logoData = ResourceData.loadString(ctx,
                    "MobilePrinter.Logo");
            if (logoData != null) {
                p.printBitmap(BitmapManipulation.createBitmapFromResources(logoData));
                p.printLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void printHeader(Printer p, Context ctx) {
        try {
            String headerData = ResourceData.loadString(ctx,
                    "MobilePrinter.Header");
            printResourceData(p, headerData);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void printFooter(Printer p, Context ctx) {
        try {
            String footerData = ResourceData.loadString(ctx,
                    "MobilePrinter.Footer");
            printResourceData(p, footerData);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void printDiscount(Printer p, Context ctx, Discount discount) {
        p.flush(); // printDiscount must flush data before printing a bitmap
        p.printLine(discount.getTitle(ctx) + " " +
                discount.getDate(ctx));
        p.printBitmap(discount.getBarcode().toBitmap());
    }

    private static void printResourceData(Printer p, String headerData) {
        if (headerData != null) {
            String[] lines = headerData.split("\n");
            for (String line : lines) {
                p.printLine(line);
            }
            p.printLine();
        }
    }

    public static String padBefore(String text, int size) {
        String ret = "";
        for (int i = 0; i < size - text.length(); i++) {
            ret += " ";
        }
        ret += text;
        return ret;
    }

    public static String padAfter(String text, int size) {
        String ret = text;
        for (int i = 0; i < size - text.length(); i++) {
            ret += " ";
        }
        return ret;
    }

}
