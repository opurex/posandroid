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
import com.opurex.client.R;
import com.opurex.client.drivers.printer.Printer;
import com.opurex.client.drivers.printer.PrinterHelper;
import com.opurex.client.models.Customer;
import com.opurex.client.models.Ticket;
import com.opurex.client.models.TicketLine;

import java.text.DateFormat;
import java.text.DecimalFormat;

/** Proxy class to print orders (Ticket). */
public class OrderDocument implements PrintableDocument
{
    private Ticket t;

    public OrderDocument (Ticket t) {
        this.t = t;
    }

    public boolean print(Printer printer, Context ctx) {
        if (!printer.isConnected()) {
            return false;
        }
        DecimalFormat priceFormat = new DecimalFormat("#0.00");
        Customer c = this.t.getCustomer();
        printer.initPrint();
        // Header
        PrinterHelper.printHeader(printer, ctx);
        // Title
        DateFormat df = DateFormat.getDateTimeInstance();
        if (c != null) {
            printer.printLine(PrinterHelper.padAfter(ctx.getString(R.string.order_name), 16) +
                    PrinterHelper.padBefore(this.t.getTicketId(), 16));
            printer.printLine(PrinterHelper.padAfter(ctx.getString(R.string.tkt_cust), 9)
                    + PrinterHelper.padBefore(c.getName(), 23));
        } else {
            printer.printLine(PrinterHelper.padAfter(ctx.getString(R.string.order_name), 16) +
                    PrinterHelper.padBefore(this.t.getLabel(), 16));
        }
        printer.printLine();
        // Content
        printer.printLine(PrinterHelper.padAfter(ctx.getString(R.string.tkt_line_article), 10)
                + PrinterHelper.padBefore(ctx.getString(R.string.tkt_line_price), 7)
                + PrinterHelper.padBefore("", 5)
                + PrinterHelper.padBefore(ctx.getString(R.string.tkt_line_total), 10));
        printer.printLine();
        printer.printLine("--------------------------------");
        String lineTxt;
        for (TicketLine line : this.t.getLines()) {
            printer.printLine(PrinterHelper.padAfter(line.getProduct().getLabel(), 32));
            lineTxt = priceFormat.format(line.getProductIncTax());
            lineTxt = PrinterHelper.padBefore(lineTxt, 17);
            lineTxt += PrinterHelper.padBefore("x" + line.getQuantity(), 5);
            lineTxt += PrinterHelper.padBefore(priceFormat.format(line.getTotalDiscPIncTax()), 10);
            printer.printLine(lineTxt);
            if (line.getDiscountRate() != 0) {
                printer.printLine(PrinterHelper.padBefore(ctx.getString(R.string.include_discount) + Double.toString(line.getDiscountRate() * 100) + "%", 32));
            }
        }
        printer.printLine();
        // Footer
        PrinterHelper.printFooter(printer, ctx);
        printer.cut();
        printer.flush();
        // End
        return true;
    }
}
