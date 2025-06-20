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
import java.text.SimpleDateFormat;
import java.util.*;

import com.opurex.client.data.Data;
import com.opurex.client.utils.CalculPrice;
import com.opurex.client.utils.ReadList;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.util.Log;

/**
 * Class for Ticket and SharedTicket. Once validated, the Ticket is embedded
 * in a Receipt.
 * Ticket is Shareable
 * It means that any edits are updated if the app is in synchronized mode.
 * All edits are made in _method() which ends with the updateTicket() call.
 * make sure to keep this semantic and behaviors.
 * Note: updateTicket was deleted with the share of orders. It requested
 * the order from the server.
 */
public class Ticket implements Serializable {

    private static final String LOG_TAG = "Opurex/Ticket";

    private static final String DATE_FORMAT = "HH:mm:ss";
    private String id; //
    /** Cash register id. 0 when not set (for orders). */
    private int cashRegisterId;
    /** Cash session sequence. 0 when not set (for orders). */
    private int sequence;
    /** Ticket number. */
    private String ticketId;
    private int articles;
    private List<TicketLine> lines;
    private Customer customer;
    private TariffArea area;
    private User user;
    private Integer discountProfileId;
    private double discountRate;
    private Integer custCount;
    private long serverDate_seconds;

    private transient ArrayList<Payment> payments;

    // TicketId is only set on payment action
    // The equivalent of ticketId is the creationTime
    private double creationTime;

    private static final String LOGTAG = "Tickets";
    private static final String JSONERR_AREA = "Error while parsing Area JSON, setting Area to null";
    private static final String JSONERR_CUSTOMER = "Error while parsing Costumer JSON, setting Costumer to null";
    private static final String JSONERR_DATE = "Error parsing date in JSON, setting it to 0";
    private String label = null;

    private void _init(String id, String label) {
        this.id = id;
        this.ticketId = label;
        this.label = label;
        this.lines = new ArrayList<TicketLine>();
        this.creationTime = new Date().getTime();
        this.payments = new ArrayList<Payment>();
    }

    private void _addTicketLine(TicketLine ticketLine) {
        this.lines.add(ticketLine);
    }

    private void _removeTicketLine(TicketLine ticketLine) {
        this.lines.remove(ticketLine);
    }

    private void _adjustTicketLine(TicketLine ticketLine, int qtt) {
        ticketLine.adjustQuantity(qtt);
    }

    private void _setTicketLineQtt(TicketLine ticketLine, double qtt) {
        ticketLine.setQuantity(qtt);
    }

    private void _setCustomer(Customer c) { this.customer = c; }


    public Ticket() { _init(UUID.randomUUID().toString(), null); }
    public Ticket(String label) { _init(UUID.randomUUID().toString(), label); }
    public Ticket(String id, String label) { _init(id, label); }

    /** Get a temporary copy of the ticket. It is used to manipulate a copy
     * before applying the changes (like when splitting the ticket). */
    public Ticket getTmpTicketCopy() {
        Ticket t = new Ticket();
        if (this.id != null) {
            t.id = new String(this.id);
        }
        t.ticketId = this.ticketId;
        t.articles = this.articles;
        t.creationTime = this.creationTime;
        t.cashRegisterId = this.cashRegisterId;
        t.sequence = this.sequence;
        t.user = this.user;
        t.customer = this.customer;
        if (this.custCount != null) {
            t.custCount = this.custCount;
        }
        t.lines = new ArrayList<TicketLine>();
        for (TicketLine l : this.lines) {
            t.addTicketLine(new TicketLine(l));
        }
        t.payments = new ArrayList<Payment>();
        for (Payment p : payments) {
            t.payments.add(p.copyPayment());
        }
        if (this.area != null) {
            t.area = this.area;
        }
        if (this.discountProfileId != null) {
            t.discountProfileId = new Integer(this.discountProfileId);
        }
        t.discountRate = this.discountRate;
        // taxes are not copied, must be calculated again.
        return t;
    }

    public void assignToPlace(Place p) {
        this.id = p.getId();
        this.ticketId = p.getName();
        this.label = p.getName();
    }

    /*
        PAYMENTS
     */
    public Collection<Payment> getPaymentsCollection() {
        return Collections.unmodifiableCollection(this.payments);
    }

    public ReadList<Payment> getPaymentList() {
        return new ReadList<>(this.payments);
    }


    public void addPayment(Payment payment) { this.payments.add(payment); }
    public void removePayment(Payment payment) {
        this.payments.remove(payment);
    }

    /** Set cashRegisterId and sequence from cash session. */
    public void setCashSession(Cash session) {
        this.cashRegisterId = session.getCashRegisterId();
        this.sequence = session.getSequence();
    }

    /** Set ticket number. */
    public void setTicketId(String ticketId) { this.ticketId = ticketId; }
    public void setDiscountRate(double rate) { this.discountRate = rate; }
    public double getDiscountRate() { return this.discountRate; }
    public String getId() { return this.id; }

    public String getLabel() {
        if (this.customer != null) {
            return this.customer.getName();
        }
        if (this.label != null) {
            return label;
        }
        return new SimpleDateFormat(DATE_FORMAT).format(creationTime);
    }

    public TariffArea getTariffArea() { return this.area; }
    public User getUser() { return this.user; }
    public long getServerDateInSeconds() { return this.serverDate_seconds; }
    public void setTariffArea(TariffArea area) {
        this.area = area;
        for (TicketLine l : this.lines) {
            l.setTariffArea(area);
        }
    }
    public List<TicketLine> getLines() { return this.lines; }
    public TicketLine getLineAt(int index) { return this.lines.get(index); }

    public void addLine(Product p, int qty) {
        addTicketLine(new TicketLine(p, qty, getTariffArea()));
    }

    public void removeTicketLine(TicketLine l) {
        _removeTicketLine(l);
        // Removes only 1 article for a scaled product
        if (l.getProduct().isScaled()) {
            this.articles--;
        } else {
            this.articles -= l.getArticlesNumber();
        }
    }

    /**
     * Adds a line with a scaled product
     *
     * @param p     the product to add
     * @param scale the product's weight
     */
    public void addLineProductScaled(Product p, double scale) {
        addTicketLine(new TicketLine(p, scale, getTariffArea()));
    }

    public void addTicketLine(TicketLine ticketLine) {
        if (ticketLine.getProduct().isScaled()) {
            this.articles++;
        } else {
            this.articles += ticketLine.getArticlesNumber();
        }
        _addTicketLine(ticketLine);
    }
    
    /**
     * Add product to ticket
     *
     * @param p is the product to add
     * @return product's position
     */
    public int addProduct(Product p) {
        int position = 0;
        for (TicketLine l : this.lines) {
            if (l.getProduct().equals(p) && !l.isCustom()) {
                this.adjustProductQuantity(l, 1);
                return position;
            }
            position++;
        }
        this.addLine(p, 1);
        return position;
    }

    public int addProductReturn(Product p) {
        int position = 0;
        for (TicketLine l : this.lines) {
            if (l.getProduct().equals(p) && !l.isCustom()) {
                this.adjustProductQuantity(l, -1);
                return position;
            }
            position++;
        }
        this.addLine(p, -1);
        return position;
    }

    public void addScaledProductReturn(Product p, double scale) {
        this.addLineProductScaled(p, -scale);
    }

    /**
     * Adds scaled product to the ticket
     *
     * @param p     the product to add
     * @param scale the products weight
     */
    public void addScaledProduct(Product p, double scale) {
        this.addLineProductScaled(p, scale);
    }

    private boolean adjustProductQuantity(TicketLine l, int qty) {
        if (this.sameSign(l.getQuantity() + qty, l.getQuantity())) { //if the quantity's numeric sign didn't change
            _adjustTicketLine(l, qty);
            if (this.sameSign(l.getQuantity(), qty)) {
                this.articles += Math.abs(qty);
            } else {
                this.articles -= Math.abs(qty);
            }
            return true;
        }
        this.removeTicketLine(l);
        return false;
    }

    private boolean sameSign(double quantity, double qty) {
        return quantity * qty > 0;
    }

    /**
     * Add/subtract quantity to non-scaled product and removes it if final qty < 0
     *
     * @param l   is the ticket line to edit
     * @param qty is the quantity to add
     * @return false if line is remove, true otherwise
     */
    public boolean adjustQuantity(TicketLine l, int qty) {
        return this.adjustProductQuantity(l, qty);
    }

    /**
     * Adjusts the weight of a scaled product
     *
     * @param l     the ticket's line of the product to modify
     * @param scale the modify weight
     * @return false if line is removed, true otherwise
     */
    public boolean adjustScale(TicketLine l, double scale) {
        if (scale > 0) {
            _setTicketLineQtt(l, scale);
            return true;
        }
        this.removeTicketLine(l);
        return false;
    }

    /** Reset the ticket content. Used when validating changes
     * when splitting a ticket. */
    public void replaceLines(List<TicketLine> lines) {
        this.lines.clear();
        this.articles = 0;
        for (TicketLine l : lines) {
            this.addTicketLine(l);
        }
    }

    public int getArticlesCount() { return this.articles; }

    // Ticket prices without ticket discount (but with line discounts)
    //////////////////////////////////////////////////////////////////

    /** @deprecated
     * Get price without tax nor ticket discount.
     * Unreliable. Deprecated until resurected by a B2B feature. */
    @Deprecated
    public double getSubprice() {
        double total = 0.0;
        for (TicketLine l : this.lines) {
            total = CalculPrice.add(total, l.getTotalDiscPExcTax());
        }
        return total;
    }

    /** Get total amount without ticket discount. */
    public double getSubtotal() {
        double total = 0.0;
        for (TicketLine l : this.lines) {
            total = CalculPrice.add(total, l.getTotalDiscPIncTax());
        }
        return total;
    }

    // Ticket prices with ticket discount
    /////////////////////////////////////

    /** Get ticket total with taxes and ticket discount. */
    public double getTicketPrice() {
        return CalculPrice.discount(this.getSubtotal(), this.discountRate);
    }

    /** Get ticket price without tax with ticket discount. */
    public double getTicketPriceExcTax() {
        return CalculPrice.discount(this.getSubprice(), this.discountRate);
    }

    /** Get tax total with discount. */
    public double getTaxCost() {
        List<TaxLine> taxes = this.getTaxLines();
        double total = 0.0;
        for (TaxLine line : taxes) {
            total = CalculPrice.add(total, line.getAmount());
        }
        return total;
    }

    /** Get the total of taxes by rate with discount. */
    public List<TaxLine> getTaxLines() {
        List<TaxLine> taxes = new ArrayList<TaxLine>();
        List<Tax> allTaxes = Data.Tax.getTaxes();
        // Get tax total from lines
        for (TicketLine l : this.lines) {
            int taxId = l.getProduct().getTaxId();
            Tax tax = null;
            for (Tax t : allTaxes) {
                if (t.getId() == taxId) {
                    tax = t;
                    break;
                }
            }
            if (tax == null) {
                // TODO: This is an unlikely to happen error
                Log.e(LOG_TAG, "Null tax for id " + taxId);
                continue;
            }
            TaxLine taxLine = null;
            for (TaxLine tl : taxes) {
                if (tl.getTaxId() == taxId) {
                    taxLine = tl;
                    break;
                }
            }
            if (taxLine == null) {
                taxLine = new TaxLine(tax);
                taxes.add(taxLine);
            }
            taxLine.add(l.getTotalDiscPIncTax());
        }
        // Apply ticket discount
        for (TaxLine tl : taxes) {
            tl.finalize(true, this.discountRate);
        }
        return taxes;
    }

    public String getTicketId() { return this.ticketId; }
    public boolean isEmpty() { return this.lines.size() == 0; }
    public Customer getCustomer() { return this.customer; }

    public void setCustomer(Customer c) {
        _setCustomer(c);
        if (c != null && c.getTariffAreaId() != null) {
            List<TariffArea> tariffAreasList = new ArrayList<TariffArea>();
            tariffAreasList.addAll(Data.TariffArea.areas);
            for (TariffArea tariffArea : tariffAreasList) {
                if (tariffArea.getId().equals(c.getTariffAreaId())) {
                    this.setTariffArea(tariffArea);
                    break;
                }
            }
        }
    }

    // Boolean is here in order to manage ID which has to be serialized or not
    public JSONObject toJSON(boolean isShared) throws JSONException {
        JSONObject o = new JSONObject();
        if (isShared) {
            if (this.id != null) {
                o.put("id", id);
            } else {
                o.put("id", JSONObject.NULL);
            }
        } else {
            o.put("type", 0);
        }
        if (this.ticketId != null) {
            // Validated ticket with a number
            o.put("cashRegister", this.cashRegisterId);
            o.put("sequence", this.sequence);
            o.put("number", Integer.parseInt(ticketId));
        } else {
            // Non validated ticket
            o.put("cashRegister", 0);
            o.put("sequence", 0);
            o.put("number", JSONObject.NULL);
        }
        // Date is set in Receipt
        if (this.custCount != null) {
            o.put("custCount", this.custCount);
        } else {
            o.put("custCount", JSONObject.NULL);
        }
        o.put("price", this.getSubprice());
        o.put("taxedPrice", this.getSubtotal());
        o.put("discountRate", this.discountRate);
        o.put("finalPrice", this.getTicketPriceExcTax());
        o.put("finalTaxedPrice", this.getTicketPrice());
        // custbalance is set on Receipt
        // user is set in Receipt
        JSONArray lines = new JSONArray();
        int i = 0;
        for (TicketLine l : this.lines) {
            JSONObject line = l.toJSON(i);
            lines.put(line);
            i++;
            if (l.getProduct() instanceof CompositionInstance) {
                // Add content lines for stock and sales
                // The subline has a price of 0.
                CompositionInstance inst = (CompositionInstance) l.getProduct();
                for (Product p : inst.getProducts()) {
                    Product sub = new Product(p.getId(), p.getLabel(), null,
                            0.0, 0.0, p.getTaxId(), p.isScaled(),
                            p.hasImage(), p.getDiscountRate(), p.isDiscountRateEnabled(), p.isPrepaid());
                    TicketLine subTktLine = new TicketLine(sub, 1, getTariffArea());
                    JSONObject subline = subTktLine.toJSON(i);
                    i++;
                    lines.put(subline);
                }
            }
        }
        o.put("lines", lines);
        JSONArray taxes = new JSONArray();
        for (TaxLine tl : this.getTaxLines()) {
            JSONObject otl = new JSONObject();
            otl.put("tax", tl.getTaxId());
            otl.put("base", tl.getBase());
            otl.put("taxRate", tl.getRate());
            otl.put("amount", tl.getAmount());
            taxes.put(otl);
        }
        o.put("taxes", taxes);
        // payments is set in Receipt
        if (this.customer != null) {
            o.put("customer", Integer.parseInt(this.customer.getId()));
        } else {
            o.put("customer", JSONObject.NULL);
        }
        if (this.area != null) {
            o.put("tariffArea", Integer.parseInt(this.area.getId()));
        } else {
            o.put("tariffArea", JSONObject.NULL);
        }
        if (this.discountProfileId != null) {
            o.put("discountProfile", this.discountProfileId);
        } else {
            o.put("discountProfile", JSONObject.NULL);
        }
        return o;
    }

    public static Ticket fromJSON(Context context, JSONObject o) throws JSONException {
        return fromJSON(context, o, new TicketInstance());
    }

    protected static Ticket fromJSON(Context context, JSONObject o, TicketInstance instance)
            throws JSONException {
        String id = String.valueOf(o.getInt("id"));
        String label = null;
        try {
            label = o.getString("label");
        } catch (JSONException e) {
        }
        Ticket result = instance.newTicket(id, label);
        try {
            result.cashRegisterId = o.getInt("cashRegister");
            result.sequence = o.getInt("sequence");
            result.ticketId = String.valueOf(o.getInt("number"));
        } catch (JSONException e) {
            // Leave empty
        }
        if (!o.isNull("custCount")) {
            result.custCount = o.getInt("custCount");
        }
        // Getting server date
        try {
            result.serverDate_seconds = 0;
            if (o.has("date")) {
                result.serverDate_seconds = o.getLong("date");
            }
        } catch (JSONException e) {
            Log.e(LOGTAG, JSONERR_DATE);
        }
        // Getting Tarif area
        try {
            List<TariffArea> areas = Data.TariffArea.areas;
            if (!o.isNull("tariffArea")) {
                String tarifAreaId = Integer.toString(o.getInt("tariffArea"));
                for (int i = 0; i < areas.size(); ++i) {
                    if (tarifAreaId.equals(areas.get(i).getId())) {
                        result.area = areas.get(i);
                        break;
                    }
                }
            } else {
                result.area = null;
            }
        } catch (JSONException e) {
            Log.e(LOGTAG, JSONERR_AREA);
            result.area = null;
        }
        // Getting Customer
        try {
            List<Customer> customers = Data.Customer.customers;
            String customerId = String.valueOf(o.getInt("customer"));
            for (int i = 0; i < customers.size(); ++i) {
                if (customers.get(i).getId().equals(customerId)) {
                    result.customer = customers.get(i);
                    break;
                }
            }
        } catch (JSONException e) {
            Log.e(LOGTAG, JSONERR_CUSTOMER);
            result.customer = null;
        }

        if (!o.isNull("discountProfile")) {
            result.discountProfileId = o.getInt("discountProfile");
        }
        result.discountRate = o.getDouble("discountRate");

        // Getting all lines
        JSONArray array = o.getJSONArray("lines");
        result.articles = 0;
        for (int i = 0; i < array.length(); ++i) {
            try {
                JSONObject current = array.getJSONObject(i);
                TicketLine currentLine = TicketLine.fromJSON(context, current, result.area);
                result.articles += currentLine.getQuantity();
                result.lines.add(currentLine);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return result;
    }

    private static Ticket newTicket(String id, String label) {
        return new Ticket(id, label);
    }

    @Override
    public String toString() {
        return this.ticketId + " (" + this.articles + " articles)";
    }

    public String getDiscountRateString() {
        double pourcent = this.discountRate * 100;
        return ((int) pourcent) + " %";
    }

    public double getFinalDiscount() {
        double result = 0;
        for (TicketLine l : this.lines) {
            result += l.getTotalDiscPIncTax();
        }
        return CalculPrice.getDiscountCost(result, this.discountRate);
    }

    public double getGenericPrice(Product p, int binaryMask) {
        return p.getGenericPrice(this.area, binaryMask);
    }

    public Ticket getRefundTicket() {
        Ticket result = new Ticket();
        for (TicketLine line : lines) {
            result.addLine(line.getRefundLine());
        }
        result.setCustomer(getCustomer());
        return result;
    }

    private void addLine(TicketLine refundLine) {
        this.articles += refundLine.getQuantity();
        _addTicketLine(refundLine);
    }

    protected static class TicketInstance {
        public Ticket newTicket(String id, String label) {
            return new Ticket(id, label);
        }
    }

    public class TaxLine {
        protected int taxId;
        /** base holds either untaxed or taxed values.
         * Once finalize is called, it is set to its final value.
         * This is ugly but consistent with desktop implementation. */
        protected double base;
        protected double rate;
        protected double amount;

        public TaxLine(Tax tax) {
            this.taxId = tax.getId();
            this.base = 0.0;
            this.rate = tax.getRate();
            this.amount = 0.0;
        }
        /** Add a value to get or extract tax from. It must be called
         * before finalize. */
        public void add(double value) {
            this.base += value;
        }
        /** Compute the final values. Once finalized, the getter can be called.
         * @param taxIncluded If the tax should be extracted from the sum or
         * computed from it. */
        public void finalize(boolean taxIncluded, double discountRate) {
            double sum = CalculPrice.discount(this.base, discountRate);
            if (taxIncluded) {
                double tax = CalculPrice.extractTax(sum, this.rate);
                double base = CalculPrice.untax(sum, this.rate);
                this.base = base;
                this.amount = tax;
            } else {
                double tax = CalculPrice.getTax(sum, this.rate);
                this.base = sum;
                this.amount = tax;
            }
        }

        public int getTaxId() { return this.taxId; }
        public double getRate() { return this.rate; }
        public double getBase() { return this.base; }
        public double getAmount() { return this.amount; }
    }
}
