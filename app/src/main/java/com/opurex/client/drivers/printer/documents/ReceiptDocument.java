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
import com.opurex.client.data.DataSavable.CashRegisterData;
import com.opurex.client.drivers.printer.Printer;
import com.opurex.client.drivers.printer.PrinterHelper;
import com.opurex.client.models.CashRegister;
import com.opurex.client.models.Customer;
import com.opurex.client.models.Payment;
import com.opurex.client.models.PaymentMode;
import com.opurex.client.models.Product;
import com.opurex.client.models.Receipt;
import com.opurex.client.models.Ticket;
import com.opurex.client.models.TicketLine;
import java.util.Date;
import java.util.List;
import java.text.DateFormat;
import java.text.DecimalFormat;

/** Proxy class to print receipts.
 * It is not directly set into Receipt for changes to the render
 * not break the serialization (and kill local receipts on update). */
public class ReceiptDocument implements PrintableDocument
{
    private Receipt r;

    public ReceiptDocument (Receipt r) {
        this.r = r;
    }

    public boolean print(Printer printer, Context ctx) {
        if (!printer.isConnected()) {
            return false;
        }
        DecimalFormat priceFormat = new DecimalFormat("#0.00");
        Customer c = this.r.getTicket().getCustomer();
        printer.initPrint();
        PrinterHelper.printLogo(printer, ctx);
        PrinterHelper.printHeader(printer, ctx);
        // Title
        CashRegisterData crData = new CashRegisterData();
        CashRegister cr = crData.current(ctx);
        DateFormat df = DateFormat.getDateTimeInstance();
        String date = df.format(new Date(this.r.getPaymentTime() * 1000));
        printer.printLine(PrinterHelper.padAfter(ctx.getString(R.string.tkt_date), 7)
                + PrinterHelper.padBefore(date, 25));
        if (c != null) {
            printer.printLine(PrinterHelper.padAfter(ctx.getString(R.string.tkt_cust), 9)
                    + PrinterHelper.padBefore(c.getName(), 23));
        }
        printer.printLine(PrinterHelper.padAfter(ctx.getString(R.string.tkt_number), 16) +
                PrinterHelper.padBefore(cr.getMachineName() + " - " + this.r.getTicketNumber(), 16));
        printer.printLine();
        // Content
        printer.printLine(PrinterHelper.padAfter(ctx.getString(R.string.tkt_line_article), 10)
                + PrinterHelper.padBefore(ctx.getString(R.string.tkt_line_price), 7)
                + PrinterHelper.padBefore("", 5)
                + PrinterHelper.padBefore(ctx.getString(R.string.tkt_line_total), 10));
        printer.printLine();
        printer.printLine("--------------------------------");
        String lineTxt;
        for (TicketLine line : this.r.getTicket().getLines()) {
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
        printer.printLine("--------------------------------");
        if (this.r.getTicket().getDiscountRate() > 0.0) {
            Ticket ticket = this.r.getTicket();
            String line = PrinterHelper.padAfter(ctx.getString(R.string.tkt_discount_label), 16);
            line += PrinterHelper.padBefore((ticket.getDiscountRate() * 100) + "%", 6);
            line += PrinterHelper.padBefore("-" + ticket.getFinalDiscount() + "€", 10);
            printer.printLine(line);
            printer.printLine("--------------------------------");
        }
        // Taxes
        printer.printLine();
        DecimalFormat rateFormat = new DecimalFormat("#0.#");
        List<Ticket.TaxLine> taxes = this.r.getTicket().getTaxLines();
        for (Ticket.TaxLine line : taxes) {
            double rate = line.getRate();
            double dispRate = rate * 100;
            printer.printLine(PrinterHelper.padAfter(ctx.getString(R.string.tkt_tax)
                    + rateFormat.format(dispRate) + "%", 20)
                    + PrinterHelper.padBefore(priceFormat.format(line.getAmount()) + "€", 12));
        }
        printer.printLine();
        // Total
        printer.printLine(PrinterHelper.padAfter(ctx.getString(R.string.tkt_subtotal), 15)
                + PrinterHelper.padBefore(priceFormat.format(this.r.getTicket().getTicketPriceExcTax()) + "€", 17));
        printer.printLine(PrinterHelper.padAfter(ctx.getString(R.string.tkt_total), 15)
                + PrinterHelper.padBefore(priceFormat.format(this.r.getTicket().getTicketPrice()) + "€", 17));
        printer.printLine(PrinterHelper.padAfter(ctx.getString(R.string.tkt_inc_vat), 15)
                + PrinterHelper.padBefore(priceFormat.format(this.r.getTicket().getTaxCost()) + "€", 17));
        // Payments
        printer.printLine();
        printer.printLine();
        for (Payment pmt : this.r.getPayments()) {
            Payment retPmt = pmt.getBackPayment();
            if (pmt.getMode().getLabel().length() > 20) {
                printer.printLine(PrinterHelper.padAfter(pmt.getMode().getLabel(), 32));
                printer.printLine(PrinterHelper.padBefore(priceFormat.format(pmt.getGiven()) + "€", 32));
            } else {
                printer.printLine(
                       PrinterHelper.padAfter(pmt.getMode().getLabel(), 20)
                        + PrinterHelper.padBefore(priceFormat.format(pmt.getGiven()) + "€", 12)
                );
            }
            if (retPmt != null) {
                PaymentMode retMode = retPmt.getMode();
                if (retMode.getBackLabel().length() > 18) {
                    printer.printLine(PrinterHelper.padAfter("  " + retMode.getBackLabel(), 32));
                    printer.printLine(PrinterHelper.padBefore(priceFormat.format((-retPmt.getGiven())) + "€", 32)
                    );
                } else {
                    printer.printLine(
                            PrinterHelper.padAfter("  " + retMode.getBackLabel(), 20)
                            + PrinterHelper.padBefore(priceFormat.format((-retPmt.getGiven())) + "€", 12)
                    );
                }
            }
        }
        if (c != null) {
            double refill = 0.0;
            for (TicketLine l : this.r.getTicket().getLines()) {
                Product p = l.getProduct();
                if (p.isPrepaid()) {
                    refill += l.getProductIncTax() * l.getQuantity();
                }
            }
            printer.printLine();
            if (refill > 0.0) {
                printer.printLine(PrinterHelper.padAfter(ctx.getString(R.string.tkt_refill), 16)
                        + PrinterHelper.padBefore(priceFormat.format(refill) + "€", 16));
            }
            printer.printLine(PrinterHelper.padAfter(ctx.getString(R.string.tkt_prepaid_amount), 32));
            printer.printLine(PrinterHelper.padBefore(priceFormat.format(c.getPrepaid()) + "€", 32));
        }
        printer.printLine();
        PrinterHelper.printFooter(printer, ctx);
        if (this.r.hasDiscount()) {
            printer.printLine();
            PrinterHelper.printDiscount(printer, ctx, this.r.getDiscount());
        }
        printer.cut();
        printer.flush();
        // End
        return true;
    }
}
