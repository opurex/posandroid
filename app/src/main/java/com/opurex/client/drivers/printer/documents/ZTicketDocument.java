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
import com.opurex.client.data.Data;
import com.opurex.client.drivers.printer.Printer;
import com.opurex.client.drivers.printer.PrinterHelper;
import com.opurex.client.models.CashRegister;
import com.opurex.client.models.PaymentDetail;
import com.opurex.client.models.PaymentMode;
import com.opurex.client.models.ZTicket;
import java.util.Date;
import java.util.Map;
import java.text.DateFormat;
import java.text.DecimalFormat;

/** Proxy class to print ZTickets.
 * It is not directly set into ZTicket for changes to the render
 * not break the serialization (and kill local z tickets on update). */
public class ZTicketDocument implements PrintableDocument
{
    private ZTicket z;
    private CashRegister cr;

    public ZTicketDocument(ZTicket z, CashRegister cr) {
        this.z = z;
        this.cr = cr;
    }

    public boolean print(Printer printer, Context ctx) {
        if (!printer.isConnected()) {
            return false;
        }
        printer.initPrint();
        PrinterHelper.printLogo(printer, ctx);
        PrinterHelper.printHeader(printer, ctx);
        // Title
        DecimalFormat priceFormat = new DecimalFormat("#0.00");
        DateFormat df = DateFormat.getDateTimeInstance();
        printer.printLine(this.cr.getMachineName());
        String openDate = df.format(new Date(this.z.getCash().getOpenDate() * 1000));
        String closeDate = df.format(new Date(this.z.getCash().getCloseDate() * 1000));
        printer.printLine(PrinterHelper.padAfter(ctx.getString(R.string.tkt_z_open), 10) + PrinterHelper.padBefore(openDate, 22));
        printer.printLine(PrinterHelper.padAfter(ctx.getString(R.string.tkt_z_close), 10) + PrinterHelper.padBefore(closeDate, 22));
        printer.printLine(PrinterHelper.padAfter(ctx.getString(R.string.tkt_z_tickets), 10) + PrinterHelper.padBefore(String.valueOf(this.z.getTicketCount()), 22));
        printer.printLine(PrinterHelper.padAfter(ctx.getString(R.string.tkt_z_total), 10) + PrinterHelper.padBefore(priceFormat.format(this.z.getTotal()) + "€", 22));
        printer.printLine(PrinterHelper.padAfter(ctx.getString(R.string.tkt_z_subtotal), 10) + PrinterHelper.padBefore(priceFormat.format(this.z.getSubtotal()) + "€", 22));
        printer.printLine(PrinterHelper.padAfter(ctx.getString(R.string.tkt_z_taxes), 10) + PrinterHelper.padBefore(priceFormat.format(this.z.getTaxAmount()) + "€", 22));
        printer.printLine("--------------------------------");
        // Payments
        Map<Integer, PaymentDetail> pmt = this.z.getPayments();
        for (Integer pmId : pmt.keySet()) {
            PaymentMode mode = Data.PaymentMode.get(pmId);
            if (mode.getLabel().length() > 20) {
                printer.printLine(PrinterHelper.padAfter(mode.getLabel(), 32));
                printer.printLine(PrinterHelper.padBefore(priceFormat.format(pmt.get(pmId).getTotal()) + "€", 32));
            } else {
                printer.printLine(
                        PrinterHelper.padAfter(mode.getLabel(), 20)
                        + PrinterHelper.padBefore(priceFormat.format(pmt.get(pmId).getTotal()) + "€", 12)
                );
            }
        }
        printer.printLine("--------------------------------");
        // Taxes
        DecimalFormat rateFormat = new DecimalFormat("#0.#");
        for (Double rate : this.z.getTaxBases().keySet()) {
            printer.printLine(PrinterHelper.padAfter(rateFormat.format(rate * 100) + "%", 9) + PrinterHelper.padBefore(priceFormat.format(this.z.getTaxBases().get(rate)) + "€ / " + priceFormat.format(this.z.getTaxBases().get(rate) * rate) + "€", 23));
            }
        printer.printLine();
        PrinterHelper.printFooter(printer, ctx);
        // Cut
        printer.cut();
        printer.flush();
        return true;
    }
}
