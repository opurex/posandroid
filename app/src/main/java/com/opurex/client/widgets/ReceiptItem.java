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
package com.opurex.client.widgets;

import com.opurex.client.R;
import com.opurex.client.models.Customer;
import com.opurex.client.models.Payment;
import com.opurex.client.models.PaymentMode;
import com.opurex.client.models.Receipt;
import com.opurex.client.models.TicketLine;
import com.opurex.client.utils.StringUtils;

import android.content.Context;
import android.view.LayoutInflater;
import android.widget.TextView;
import android.widget.RelativeLayout;

public class ReceiptItem extends RelativeLayout {

    private Receipt receipt;

    private TextView label;
    private TextView date;
    private TextView time;
    private TextView customer;
    private TextView paymentMode;
    private TextView amount;
    private TextView user;
    private TextView content;

    public ReceiptItem(Context context, Receipt r) {
        super(context);
        LayoutInflater.from(context).inflate(R.layout.receipt_item,
                this, true);
        this.label = (TextView) this.findViewById(R.id.receipt_label);
        this.date = (TextView) this.findViewById(R.id.receipt_date);
        this.time = (TextView) this.findViewById(R.id.receipt_time);
        this.customer = (TextView) this.findViewById(R.id.receipt_customer);
        this.paymentMode = (TextView) this.findViewById(R.id.receipt_payment_mode);
        this.amount = (TextView) this.findViewById(R.id.receipt_amount);
        this.user = (TextView) this.findViewById(R.id.receipt_user);
        this.content = (TextView) this.findViewById(R.id.receipt_content);
        this.reuse(r);
    }

    public void reuse(Receipt r) {
        this.receipt = r;
        // Ticket number and amount
        this.label.setText("[" + r.getTicket().getTicketId() + "]");
        this.amount.setText(this.getContext().getString(R.string.ticket_total,
            r.getTicket().getTicketPrice()));
        // Date and time
        this.date.setText(StringUtils.formatDateNumeric(this.getContext(), r.getPaymentTime() * 1000));
        this.time.setText(StringUtils.formatTimeNumeric(r.getPaymentTime() * 1000));
        // Customer
        Customer c = this.receipt.getTicket().getCustomer();
        if (c != null) {
            this.customer.setText(c.getName());
        } else {
            this.customer.setText("");
        }
        // Payment mode
        boolean multiplePm = false;
        PaymentMode pm = null;
        for (Payment p : r.getPayments()) {
            if (pm != null && !(p.getMode().getCode().equals(pm.getCode()))) {
                multiplePm = true;
                break;
            }
            pm = p.getMode();
        }
        if (multiplePm) {
            this.paymentMode.setText(R.string.ticket_pm_multiple);
        } else {
            if (pm != null) {
                this.paymentMode.setText(pm.getLabel());
            } else {
                this.paymentMode.setText("");
            }
        }
        // User
        this.user.setText(r.getUser().getName());
        // Content
        String content = "";
        for (TicketLine l : r.getTicket().getLines()) {
            content += l.getProduct().getLabel() + " x " + l.getQuantity() + ", ";
        }
        if (content != "") {
            content = content.substring(0, content.length() - 2);
        }
        this.content.setText(content);
    }

    public Receipt getReceipt() {
        return this.receipt;
    }

}
