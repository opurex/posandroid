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

import com.opurex.client.data.Data;
import com.opurex.client.utils.CalculPrice;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;

public class ZTicket {

    private Cash cash;
    private int ticketCount;
    private double total;
    private double subtotal;
    private Double expectedCash;
    private Map<Integer, PaymentDetail> payments;
    /** @deprecated Amount of tax bases by rate */
    @Deprecated
    private Map<Double, Double> taxBases;
    private Map<Integer, TaxLine> taxLines;
    private Map<Integer, PaymentLine> paymentLines;
    private Map<Integer, CatSalesLine> catSales;
    private Map<String, CatTaxLine> catTaxes;
    private Map<Integer, CustBalanceLine> custBalances;

    /** Build current Z ticket */
    public ZTicket(Context ctx) {
        this.cash = Data.Cash.currentCash(ctx);
        List<Receipt> receipts = Data.Receipt.getReceipts(ctx);
        this.ticketCount = receipts.size();
        this.total = 0.0;
        this.subtotal = 0.0;
        this.payments = new HashMap<>();
        this.taxBases = new HashMap<Double, Double>();
        this.taxLines = new HashMap<Integer, TaxLine>();
        this.paymentLines = new HashMap<Integer, PaymentLine>();
        this.catSales = new HashMap<Integer, CatSalesLine>();
        this.catTaxes = new HashMap<String, CatTaxLine>();
        this.custBalances = new HashMap<Integer, CustBalanceLine>();
        Catalog catalog = Data.Catalog.catalog(ctx);
        com.opurex.client.models.Currency currency = null; // hacky for single currency
        Integer cashPaymentModeId = null;
        for (Receipt r : receipts) {
            Customer cust = r.getTicket().getCustomer();
            double balance = 0.0;
            // Payments
            for (Payment p : r.getPayments()) {
                if (cashPaymentModeId == null && "cash".equals(p.getMode().getCode())) {
                    cashPaymentModeId = p.getMode().getId();
                }
                if (currency == null) { currency = p.getOpurexCurrency(); }
                getOrCreatePaymentModeDetail(p.getMode()).add(p.getGiven());
                this.total += p.getGiven();
                if (p.getMode().isPrepaid() || p.getMode().isDebt()) {
                    balance -= p.getGiven();
                }
                // Check for give back
                Payment back = p.getBackPayment();
                if (back != null) {
                    if (cashPaymentModeId == null && "cash".equals(back.getMode().getCode())) {
                        cashPaymentModeId = back.getMode().getId();
                    }
                    // Same process
                    getOrCreatePaymentModeDetail(back.getMode()).add(back.getGiven());
                    this.total += back.getGiven();
                    if (back.getMode().isPrepaid() || back.getMode().isDebt()) {
                        balance -= back.getGiven();
                    }
                }
            }
            for (Integer pmId : this.payments.keySet()) {
                PaymentLine pml = new PaymentLine(pmId, currency.getId());
                pml.add(this.payments.get(pmId).getTotal(),
                        this.payments.get(pmId).getTotal());
                this.paymentLines.put(pmId, pml);
            }
            // Taxes, categories and prepayment refill
            Ticket t = r.getTicket();
            // TaxLines from Ticket
            for (Ticket.TaxLine tl : t.getTaxLines()) {
                int taxId = tl.getTaxId();
                double rate = tl.getRate();
                if (this.taxLines.get(taxId) == null) {
                    this.taxLines.put(taxId, new TaxLine(taxId, rate));
                }
                this.taxLines.get(taxId).add(tl.getBase(), tl.getAmount());
            }
            for (TicketLine l : t.getLines()) {
                double subtotal = CalculPrice.applyDiscount(l.getTotalDiscPExcTax(),
                        t.getDiscountRate());
                // Prepayment refill
                if (cust != null && l.getProduct().isPrepaid()) {
                    balance += l.getTotalExcTax();
                }
                // Tax by individual lines
                int taxId = l.getProduct().getTaxId();
                double taxRate = l.getProduct().getTaxRate();
                Double base = taxBases.get(taxRate);
                double newBase = 0.0;
                if (base == null) {
                    newBase = subtotal;
                } else {
                    newBase = base + subtotal;
                }
                this.subtotal += subtotal;
                this.taxBases.put(taxRate, newBase);
                // Category
                Integer catId = l.getProduct().getCategoryId();
                String reference = null;
                String label = null;
                if (catId != null) {
                    Category c = catalog.getCategory(String.valueOf(catId));
                    if (c != null) {
                        reference = c.getReference();
                        label = c.getLabel();
                        if (this.catSales.get(catId) == null) {
                            CatSalesLine cl = new CatSalesLine(catId,
                                    c.getReference(), c.getLabel());
                            this.catSales.put(catId, cl);
                        }
                        this.catSales.get(catId).add(subtotal);
                        // Tax by category
                        String idKey = catTaxIdKey(reference, taxId);
                        if (this.catTaxes.get(idKey) == null) {
                            CatTaxLine ctl = new CatTaxLine(reference, label, taxId);
                            this.catTaxes.put(idKey, ctl);
                        }
                        this.catTaxes.get(idKey).add(subtotal,
                                CalculPrice.getTax(subtotal, taxRate));
                    }
                }
            }
            // Register customer balance if any
            if (cust != null && (balance > 0.005 || balance < -0.005)) {
                int custId = Integer.parseInt(cust.getId());
                if (this.custBalances.get(custId) == null) {
                    this.custBalances.put(custId, new CustBalanceLine(custId));
                }
                this.custBalances.get(custId).add(balance);
            }
        }
        // Compute expected cash
        if (cashPaymentModeId != null) {
            this.expectedCash = this.cash.getOpenCash();
            this.expectedCash = CalculPrice.add(this.expectedCash,
                    this.paymentLines.get(cashPaymentModeId).getAmount());
        } else {
            this.expectedCash = this.cash.getOpenCash();
        }
    }

    /**
     * Get the PaymentDetail of a PaymentMode
     * Create and add it to the payments map if it doesn't exist.
     * @param paymentMode the paymentMode concerned
     * @return th paymentDetail of the paymentMode
     */
    private PaymentDetail getOrCreatePaymentModeDetail(PaymentMode paymentMode) {
        PaymentDetail result = this.payments.get(paymentMode.getId());
        if (result == null) {
            result = new PaymentDetail();
            this.payments.put(paymentMode.getId(), result);
        }
        return result;
    }

    public double getTotal() { return this.total; }
    public double getSubtotal() { return this.subtotal; }
    public double getTaxAmount() {
        double amount = 0.0;
        for (Integer tlId : this.taxLines.keySet()) {
            TaxLine tl = this.taxLines.get(tlId);
            amount = CalculPrice.add(amount, tl.getAmount());
        }
        return amount;
    }
    public int getTicketCount() { return this.ticketCount; }
    public Map<Integer, PaymentDetail> getPayments() { return this.payments; }
    public Map<Double, Double> getTaxBases() { return this.taxBases; }
    public Map<Integer, PaymentLine> getPaymentLines() {
        return this.paymentLines;
    }
    public Map<Integer, TaxLine> getTaxLines() { return this.taxLines; }
    public Map<Integer, CatSalesLine> getCatSales() { return this.catSales; }
    public Map<Integer, CustBalanceLine> getCustBalanceLines() {
        return this.custBalances;
    }
    public Cash getCash() { return this.cash; }
    public Double getExpectedCash() { return this.expectedCash; }

    public JSONObject toJSON() throws JSONException {
        JSONObject o = new JSONObject();
        o.put("cashRegister", this.cash.getCashRegisterId());
        o.put("sequence", this.cash.getSequence());
        o.put("continuous", this.cash.isContinuous());
        o.put("openDate", this.cash.getOpenDate());
        o.put("closeDate", this.cash.getCloseDate());
        o.put("closeType", this.cash.getCloseType());
        if (this.cash.getOpenCash() == null) {
            o.put("openCash", JSONObject.NULL);
        } else {
            o.put("openCash", this.cash.getOpenCash());
        }
        if (this.cash.getCloseCash() == null) {
            o.put("closeCash", JSONObject.NULL);
        } else {
            o.put("closeCash", this.cash.getCloseCash());
        }
        if (this.cash.getExpectedCash() == null) {
            o.put("expectedCash", JSONObject.NULL);
        } else {
            o.put("expectedCash", this.cash.getExpectedCash());
        }
        o.put("ticketCount", this.ticketCount);
        o.put("custCount", JSONObject.NULL); // custCount not implemented
        o.put("cs", this.subtotal);
        o.put("csPeriod", this.cash.getCsPeriod());
        o.put("csFYear", this.cash.getCsFYear());
        if (this.cash.getCsPerpetual() != null) {
            o.put("csPerpetual", this.cash.getCsPerpetual());
        }
        JSONArray jsPays = new JSONArray();
        JSONArray jsTaxes = new JSONArray();
        JSONArray jsCatSales = new JSONArray();
        JSONArray jsCatTaxes = new JSONArray();
        JSONArray jsCustBalances = new JSONArray();
        for (Integer pmId : this.paymentLines.keySet()) {
            jsPays.put(this.paymentLines.get(pmId).toJSON());
        }
        for (Cash.TaxSum sum : this.cash.getTaxSums()) {
            jsTaxes.put(sum.toJSON());
        }
        for (Integer taxId : this.taxLines.keySet()) {
            jsTaxes.put(this.taxLines.get(taxId).toJSON());
        }
        for (Integer catId : this.catSales.keySet()) {
            jsCatSales.put(this.catSales.get(catId).toJSON());
        }
        for (String idKey : this.catTaxes.keySet()) {
            jsCatTaxes.put(this.catTaxes.get(idKey).toJSON());
        }
        for (Integer custId : this.custBalances.keySet()) {
            jsCustBalances.put(this.custBalances.get(custId).toJSON());
        }
        o.put("payments", jsPays);
        o.put("taxes", jsTaxes);
        o.put("catSales", jsCatSales);
        o.put("catTaxes", jsCatTaxes);
        o.put("custBalances", jsCustBalances);
        return o;
    }

    /** The taxes collected during the session. Sums are gathered in Cash. */
    public class TaxLine {
        private int taxId;
        private double base;
        private double rate;
        private double amount;

        public TaxLine(int taxId, double rate) {
            this.taxId = taxId;
            this.rate = rate;
            this.base = 0.0;
            this.amount = 0.0;
        }
        public int getTaxId() { return this.taxId; }
        public double getRate() { return this.rate; }
        public double getBase() { return this.base; }
        public double getAmount() { return this.amount; }
        public void add(double base, double amount) {
            this.base += base;
            this.amount += amount;
        }
        public JSONObject toJSON() throws JSONException {
            JSONObject o = new JSONObject();
            o.put("base", this.base);
            o.put("taxRate", this.rate);
            o.put("amount", this.amount);
            o.put("tax", this.taxId);
            return o;
        }
    }

    public class PaymentLine {
        private int paymentModeId;
        private int currencyId;
        private double amount;
        private double currencyAmount;

        public PaymentLine(int pmId, int currencyId) {
            this.paymentModeId = pmId;
            this.currencyId = currencyId;
            this.amount = 0.0;
            this.currencyAmount = 0.0;
        }
        public int getPaymentModeId() { return this.paymentModeId; }
        public int getCurrencyId() { return this.currencyId; }
        public double getAmount() { return this.amount; }
        public double getCurrencyAmount() { return this.currencyAmount; }
        public void add(double amount, double currencyAmount) {
            this.amount += amount;
            this.currencyAmount += currencyAmount;
        }
        public JSONObject toJSON() throws JSONException {
            JSONObject o = new JSONObject();
            o.put("amount", this.amount);
            o.put("currencyAmount", this.currencyAmount);
            o.put("paymentMode", this.paymentModeId);
            o.put("currency", this.currencyId);
            return o;
        }
    }

    public class CatSalesLine {
        private int categoryId;
        private String reference;
        private String label;
        private double amount;

        public CatSalesLine(int catId, String reference, String label) {
            this.categoryId = catId;
            this.reference = reference;
            this.label = label;
            this.amount = 0.0;
        }
        public int getCategoryId() { return this.categoryId; }
        public String getReference() { return this.reference; }
        public String getLabel() { return this.label; }
        public double getAmount() { return this.amount; }
        public void add(double amount) {
            this.amount += amount;
        }
        public JSONObject toJSON() throws JSONException {
            JSONObject o = new JSONObject();
            o.put("reference", this.reference);
            o.put("label", this.label);
            o.put("amount", this.amount);
            return o;
        }
    }

    public class CatTaxLine {
        private String catReference;
        private String catLabel;
        private int taxId;
        private double base;
        private double amount;
        public CatTaxLine(String catReference, String catLabel, int taxId) {
            this.catReference = catReference;
            this.catLabel = catLabel;
            this.taxId = taxId;
            this.base = base;
            this.amount = amount;
        }
        public String getCatReference() { return this.catReference; }
        public String getCatLabel() { return this.catLabel; }
        public int getTaxId() { return this.taxId; }
        public double getBase() { return this.base; }
        public double getAmount() { return this.amount; }
        public void add(double base, double amount) {
            this.base += base;
            this.amount += amount;
        }
        public JSONObject toJSON() throws JSONException {
            JSONObject o = new JSONObject();
            o.put("reference", this.catReference);
            o.put("label", this.catLabel);
            o.put("tax", this.taxId);
            o.put("base", this.base);
            o.put("amount", this.amount);
            return o;
        }
    }
    private static String catTaxIdKey(String reference, int taxId) {
        return (reference == null) ? "-" + taxId : reference + "-" + taxId;
    }


    public class CustBalanceLine {
        private int customerId;
        private double balance;

        public CustBalanceLine(int customerId) {
            this.customerId = customerId;
            this.balance = 0.0;
        }
        public int getCustomerId() { return this.customerId; }
        public double getBalance() { return this.balance; }
        public void add(double balance) {
            this.balance += balance;
        }
        public JSONObject toJSON() throws JSONException {
            JSONObject o = new JSONObject();
            o.put("balance", this.balance);
            o.put("customer", this.customerId);
            return o;
        }
    }
}
