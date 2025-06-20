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
package com.opurex.client.models;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Calendar;
import java.util.Date;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * A validated ticket
 */
public class Receipt implements Serializable {

    private Ticket ticket;
    private Collection<Payment> payments;
    private long paymentTime;
    private User cashier;
    private Discount discount;

    public Receipt(Ticket t, Collection<Payment> p, User u) {
        this.ticket = t;
        this.payments = p;
        this.cashier = u;
        Calendar c = Calendar.getInstance();
        Date now = c.getTime();
        this.paymentTime = now.getTime() / 1000;
    }

    private Receipt() {}

    /** Create a new Receipt with only Products instead of Compositions
     * for gson to handle it correctly. */
    public static Receipt convertFlatCompositions(Receipt r) {
        Ticket tktCopy = r.getTicket().getTmpTicketCopy();
        List<TicketLine> lines = tktCopy.getLines();
        while (tktCopy.getLines().size() > 0) {
            tktCopy.removeTicketLine(tktCopy.getLines().get(0));
        }
        for (TicketLine l : r.getTicket().getLines()) {
            List<TicketLine> flatten = TicketLine.flattenCompositionLine(l);
            for (TicketLine flat : flatten) {
                tktCopy.addTicketLine(flat);
            }
        }
        Receipt copy = new Receipt();
        copy.ticket = tktCopy;
        copy.payments = r.payments;
        copy.cashier = r.cashier;
        copy.paymentTime = r.paymentTime;
        copy.discount = r.discount;
        return copy;
    }

    public long getPaymentTime() {
        return this.paymentTime;
    }

    public Ticket getTicket() {
        return this.ticket;
    }

    public User getUser() {
        return this.cashier;
    }

    public Collection<Payment> getPayments() {
        return this.payments;
    }

    public Discount getDiscount() {
        return discount;
    }

    public void setDiscount(Discount discount) {
        this.discount = discount;
    }

    public boolean hasDiscount() {
        return this.discount != null && !this.discount.getBarcode().getCode().isEmpty();
    }

    public JSONObject toJSON() throws JSONException {
        JSONObject o = this.ticket.toJSON(false);
        o.put("date", this.paymentTime);
        double custBalance = 0.0;
        if (this.ticket.getCustomer() == null) {
            for (TicketLine l : this.ticket.getLines()) {
                if (l.getProduct().isPrepaid()) {
                    custBalance += l.getTotalExcTax();
                }
            }
            for (Payment p : this.payments) {
                if (p.getMode().isDebt() || p.getMode().isPrepaid()) {
                    custBalance -= p.getAmount();
                }
            }
        }
        o.put("custBalance", custBalance);
        o.put("user", this.cashier.getId());
        JSONArray pays = new JSONArray();
        int i = 0;
        for (Payment p : this.payments) {
            JSONObject jsP = p.toJSON();
            JSONObject jsB = null;
            jsP.put("dispOrder", i);
            if (!jsP.isNull("back")) {
                jsB = jsP.getJSONObject("back");
                jsP.remove("back");
            }
            pays.put(jsP);
            i++;
            if (jsB != null) {
                jsB.put("dispOrder", i);
                pays.put(jsB);
                i++;
            }
        }
        o.put("payments", pays);
        return o;
    }

    @Override
    public String toString() {
        return this.ticket.toString() + " by " + this.cashier.toString()
                + " at " + paymentTime;
    }

    public String getTicketNumber() {
        return this.ticket.getTicketId();
    }
}
