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
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONException;

public class Cash implements Serializable {

    public static final int CLOSE_SIMPLE = 0;
    public static final int CLOSE_PERIOD = 1;
    public static final int CLOSE_FYEAR = 2;

       /** @deprecated */
    @Deprecated
    private String id;
    private int cashRegisterId;
    private int sequence;
    private long openDate;
    private long closeDate;
    private Double openCash;
    private Double closeCash;
    private Double expectedCash;
    /** Close operation type. Set only on close. */
    private int closeType;
    /** Flag to detect if the local database was deleted (potentially with
     * unsaved tickets) between two runs. */
    private boolean continuous;
    /** CS sum of the period given by server and updated on close
     * with CS from ZTicket. Required for printing. */
    private double csPeriod;
    /** CS sum of the fiscal year given by server and updated on close
     * with CS from ZTicket. Required for printing. Null when it is not
     * provided. */
    private double csFYear;
    private Double csPerpetual;
    private List<TaxSum> taxes;

    /** Create an empty cash */
    Cash() {
        this.openDate = -1;
        this.closeDate = -1;
        this.taxes = new ArrayList<TaxSum>();
    }

    public Cash(JSONObject o) throws JSONException {
        this.id = "0";
        this.cashRegisterId = o.getInt("cashRegister");
        this.sequence = o.getInt("sequence");
        this.openDate = (!o.isNull("openDate")) ? o.getLong("openDate") : -1;
        this.closeDate = (!o.isNull("closeDate")) ? o.getLong("closeDate") : -1;
        if (!o.isNull("openCash")) {
            this.openCash = o.getDouble("openCash");
        }
        if (!o.isNull("closeCash")) {
            this.closeCash = o.getDouble("closeCash");
        }
        if (!o.isNull("expectedCash")) {
            this.expectedCash = o.getDouble("expectedCash");
        }
        this.csPeriod = o.getDouble("csPeriod");
        this.csFYear = o.getDouble("csFYear");
        if (o.has("csPerpetual")) {
            this.csPerpetual = o.getDouble("csPerpetual");
        }
        this.taxes = new ArrayList<TaxSum>();
        JSONArray taxes = o.getJSONArray("taxes");
        for (int i = 0; i < taxes.length(); i++) {
            this.taxes.add(new TaxSum(taxes.getJSONObject(i)));
        }
        // Continuous is managed locally
    }

    /** Create the next cash session */
    public Cash next() {
        Cash next = new Cash();
        next.cashRegisterId = this.getCashRegisterId();
        next.sequence = this.getSequence() + 1;
        next.setContinuous(true);
        // Report tax sums and reset them according to close type
        if (this.closeType != CLOSE_FYEAR) {
            for (TaxSum s : this.taxes) {
                TaxSum newSum = new TaxSum(s, this.closeType);
                next.taxes.add(newSum);
            }
        }
        // Report cs sums and reset them according to close type
        next.csPeriod = this.csPeriod;
        next.csFYear = this.csFYear;
        next.csPerpetual = this.csPerpetual;
        switch (this.closeType) {
        case CLOSE_FYEAR: next.csFYear = 0.0; // nobreak;
        case CLOSE_PERIOD: next.csPeriod = 0.0; // nobreak;
        case CLOSE_SIMPLE: // reset nothing
        }
        return next;
    }

    /** Merge a new cash into this one (if equals).
     * It can be done only when this cash is not opened (i.e. there wasn't
     * any operation done on it).
     * @return True if c was absorbed into this. False otherwise.
     */
    public boolean absorb(Cash c) {
        if (c.equals(this) && !this.isOpened()) {
            long open = Math.max(c.getOpenDate(), this.getOpenDate());
            long close = Math.max(c.getCloseDate(), this.getCloseDate());
            this.openDate = c.getOpenDate();
            this.closeDate = c.getCloseDate();
            this.openCash = c.getOpenCash();
            this.closeCash = c.getCloseCash();
            this.expectedCash = c.getExpectedCash();
            return true;
        }
        return false;
    }

    public String getId() { return this.id; }
    public int getCashRegisterId() { return this.cashRegisterId; }
    /** Get cash sequence. Returns 0 if it is not set. */
    public int getSequence() { return this.sequence; }
    public long getOpenDate() { return this.openDate; }
    /** Cash is opened when usable (opened and not closed) */
    public boolean isOpened() {
        return this.openDate != -1 && !this.isClosed();
    }
    public boolean wasOpened() { return this.openDate != -1; }
    public boolean isClosed() { return this.closeDate != -1; }
    public long getCloseDate() { return this.closeDate; }
    public int getCloseType() { return this.closeType; }
    public Double getOpenCash() { return this.openCash; }
    public Double getCloseCash() { return this.closeCash; }
    public Double getExpectedCash() { return this.expectedCash; }
    public boolean isContinuous() { return this.continuous; }
    public void setContinuous(boolean continuous) {
        this.continuous = continuous;
    }
    public double getCsPeriod() { return this.csPeriod; }
    public double getCsFYear() { return this.csFYear; }
    public Double getCsPerpetual() { return this.csPerpetual; }
    public List<TaxSum> getTaxSums() { return this.taxes; }

    public void openNow() {
        this.openDate = System.currentTimeMillis() / 1000;
    }

    public void openNow(double openCash) {
        this.openCash = openCash;
        this.openNow();
    }

    /** Set the close date and update sums according to the Z ticket. */
    public void closeNow(ZTicket z, int closeType, Double closeCash, Double expectedCash) {
        if (this.isClosed()) { return; }
        // Close
        this.closeDate = System.currentTimeMillis() / 1000;
        this.closeType = closeType;
        // Set cash
        this.expectedCash = expectedCash;
        this.closeCash = closeCash;
        // Set CS and sums
        double cs = z.getSubtotal();
        this.csPeriod += cs;
        this.csFYear += cs;
        if (this.csPerpetual != null) {
            this.csPerpetual += cs;
        }
        // Set and update TaxSums
        for (Integer ztaxId : z.getTaxLines().keySet()) {
            ZTicket.TaxLine ztax = z.getTaxLines().get(ztaxId);
            boolean merged = false;
            for (TaxSum tax : this.taxes) {
                if (tax.getTaxId() == ztax.getTaxId()) {
                    tax.set(ztax.getBase(), ztax.getAmount());
                    merged = true;
                }
            }
            if (!merged) {
                TaxSum tax = new TaxSum(ztax.getTaxId(),
                        ztax.getRate());
                tax.set(ztax.getBase(), ztax.getAmount());
                taxes.add(tax);
            }
        }
    }

    /** Two Cash are equals if they have the same cash register id
     * and sequence. */
	public boolean equals(Object o) {
        if (!( o instanceof Cash)) {
            return false;
        }
        Cash c = (Cash) o;
        return (this.cashRegisterId == c.cashRegisterId
                && this.sequence == c.sequence);
    }

    public JSONObject toJSON() throws JSONException {
        JSONObject o = this.toOpenJSON();
        if (this.isClosed()) {
            o.put("closeDate", this.getCloseDate());
            o.put("closeType", this.closeType);
        }
        if (this.closeCash == null) {
            o.put("closeCash", JSONObject.NULL);
        } else {
            o.put("closeCash", this.closeCash);
        }
        if (this.expectedCash == null) {
            o.put("expectedCash", JSONObject.NULL);
        } else {
            o.put("expectedCash", this.expectedCash);
        }
        return o;
    }

    /** toJSON but without the closing data. Required to send the cash
     * only opened when it was opened and closed before sending data. */
    public JSONObject toOpenJSON() throws JSONException {
        JSONObject o = new JSONObject();
        o.put("cashRegister", this.getCashRegisterId());
        o.put("sequence", this.sequence);
        o.put("continuous", this.continuous);
        if (this.getOpenDate() == -1) {
            o.put("openDate", JSONObject.NULL);
        } else {
            o.put("openDate", this.getOpenDate());
        }
        o.put("closeDate", JSONObject.NULL);
        if (this.openCash == null) {
            o.put("openCash", JSONObject.NULL);
        } else {
            o.put("openCash", this.openCash);
        }
        o.put("closeCash", JSONObject.NULL);
        o.put("expectedCash", JSONObject.NULL);
        // For a closed cash, use ZTicket instead
        o.put("ticketCount", JSONObject.NULL);
        o.put("custCount", JSONObject.NULL);
        o.put("cs", JSONObject.NULL);
        o.put("csPeriod", this.csPeriod);
        o.put("csFYear", this.csFYear);
        if (this.csPerpetual != null) {
            o.put("csPerpetual", this.csPerpetual);
        }
        o.put("payments", new JSONArray());
        o.put("taxes", new JSONArray());
        o.put("catSales", new JSONArray());
        o.put("custBalances", new JSONArray());
        return o;
    }

    @Override
    public String toString() {
        return "(" + this.id + ", "
            + this.openDate + "-" + this.closeDate + ")";
    }

    public static class TaxSum implements Serializable {

        private int taxId;
        private double taxRate;
        private double base;
        private double amount;
        private double basePeriod;
        private double amountPeriod;
        private double baseFYear;
        private double amountFYear;

        public TaxSum(int taxId, double rate) {
            this.taxId = taxId;
            this.taxRate = rate;
        }
        /** Set the cumulative base and amount from a previous sum
         * according to the close type.
         * @param origin The previous sum.
         * @param closeType The close operation to know which sums
         * has to be kept and those that are reset. */
        public TaxSum(TaxSum origin, int closeType) {
            this.taxId = origin.taxId;
            this.taxRate = origin.taxRate;
            switch (closeType) {
            case CLOSE_SIMPLE: // Keep period
                this.basePeriod = origin.basePeriod;
                this.amountPeriod = origin.amountPeriod;
                // nobreak;
            case CLOSE_PERIOD: // Keep fiscal year
                this.baseFYear = origin.baseFYear;
                this.amountFYear = origin.amountFYear;
            }
        }
        public TaxSum(JSONObject o) throws JSONException {
            this.taxId = o.getInt("tax");
            this.taxRate = o.getDouble("taxRate");
            this.base = o.getDouble("base");
            this.amount = o.getDouble("amount");
            this.basePeriod = o.getDouble("basePeriod");
            this.amountPeriod = o.getDouble("amountPeriod");
            this.baseFYear = o.getDouble("baseFYear");
            this.amountFYear = o.getDouble("amountFYear");
        }
        public JSONObject toJSON() throws JSONException {
            JSONObject o = new JSONObject();
            o.put("tax", this.taxId);
            o.put("taxRate", this.taxRate);
            o.put("base", this.base);
            o.put("amount", this.amount);
            o.put("basePeriod", this.basePeriod);
            o.put("amountPeriod", this.amountPeriod);
            o.put("baseFYear", this.baseFYear);
            o.put("amountFYear", this.amountFYear);
            return o;
        }
        public int getTaxId() { return this.taxId; }
        public double getTaxRate() { return this.taxRate; }
        public double getBase() { return this.base; }
        public double getAmount() { return this.amount; }
        public double getBasePeriod() { return this.basePeriod; }
        public double getAmountPeriod() { return this.amountPeriod; }
        public double getBaseFYear() { return this.baseFYear; }
        public double getAmountFYear() { return this.amountFYear; }
        /** Set base and amount of tax if not already set.
         * It add those in period and fiscal year sums. */
        public void set(double base, double amount) {
            if (this.base > 0.005) { return; }
            this.base = base;
            this.amount = amount;
            this.basePeriod += base;
            this.amountPeriod += amount;
            this.baseFYear += base;
            this.amountFYear += amount;
        }
    }

}
