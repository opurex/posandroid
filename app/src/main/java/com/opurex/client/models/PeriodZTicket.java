/*
    Opurex Android client
    Copyright (C) Opurex contributors

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.
*/

package com.opurex.client.models;

import android.content.Context;
import com.opurex.client.data.Data;
import com.opurex.client.utils.CalculPrice;
import java.util.*;

public class PeriodZTicket {

    private int ticketCount;
    private double total;
    private double subtotal;
    private Map<Integer, PaymentDetail> payments;
    private Map<Integer, ZTicket.TaxLine> taxLines;
    private Map<Integer, ZTicket.CatSalesLine> catSales;
    private Map<String, ZTicket.CatTaxLine> catTaxes;
    private Map<Integer, ZTicket.CustBalanceLine> custBalances;

    // NEW: Add product sales map
    private Map<Integer, ProductSalesLine> productSales;

    public PeriodZTicket(Context ctx, List<Receipt> receipts) {
        this.ticketCount = receipts.size();
        this.total = 0.0;
        this.subtotal = 0.0;
        this.payments = new HashMap<>();
        this.taxLines = new HashMap<>();
        this.catSales = new HashMap<>();
        this.catTaxes = new HashMap<>();
        this.custBalances = new HashMap<>();
        this.productSales = new HashMap<>();

        Catalog catalog = Data.Catalog.catalog(ctx);

        for (Receipt r : receipts) {
            Ticket t = r.getTicket();
            Customer cust = t.getCustomer();
            double balance = 0.0;

            // Payments
            for (Payment p : r.getPayments()) {
                getOrCreatePaymentDetail(p.getMode()).add(p.getGiven());
                total += p.getGiven();

                if (p.getMode().isPrepaid() || p.getMode().isDebt()) {
                    balance -= p.getGiven();
                }

                Payment back = p.getBackPayment();
                if (back != null) {
                    getOrCreatePaymentDetail(back.getMode()).add(back.getGiven());
                    total += back.getGiven();
                    if (back.getMode().isPrepaid() || back.getMode().isDebt()) {
                        balance -= back.getGiven();
                    }
                }
            }

            // Taxes from Ticket
            for (Ticket.TaxLine tl : t.getTaxLines()) {
                int taxId = tl.getTaxId();
                double rate = tl.getRate();
                if (!taxLines.containsKey(taxId)) {
                    taxLines.put(taxId, new ZTicket(ctx).new TaxLine(taxId, rate));
                }
                Objects.requireNonNull(taxLines.get(taxId)).add(tl.getBase(), tl.getAmount());
            }

            // Ticket Lines for subtotal, categories, category taxes, and product sales
            for (TicketLine l : t.getLines()) {
                double lineSubtotal = CalculPrice.applyDiscount(
                        l.getTotalDiscPExcTax(), t.getDiscountRate());
                subtotal += lineSubtotal;

                if (cust != null && l.getProduct().isPrepaid()) {
                    balance += l.getTotalExcTax();
                }

                int taxId = l.getProduct().getTaxId();
                Integer catId = l.getProduct().getCategoryId();
                if (catId != null) {
                    Category c = catalog.getCategory(String.valueOf(catId));
                    if (c != null) {
                        if (!catSales.containsKey(catId)) {
                            catSales.put(catId, new ZTicket(ctx).new CatSalesLine(
                                    catId, c.getReference(), c.getLabel()));
                        }
                        Objects.requireNonNull(catSales.get(catId)).add(lineSubtotal);

                        String idKey = catTaxIdKey(c.getReference(), taxId);
                        if (!catTaxes.containsKey(idKey)) {
                            catTaxes.put(idKey, new ZTicket(ctx).new CatTaxLine(
                                    c.getReference(), c.getLabel(), taxId));
                        }
                        Objects.requireNonNull(catTaxes.get(idKey)).add(lineSubtotal,
                                CalculPrice.getTax(lineSubtotal, l.getProduct().getTaxRate()));
                    }
                }

                // NEW: Product sales aggregation
                int productId = Integer.parseInt(l.getProduct().getId());
                String productName = l.getProduct().getLabel();
                double quantity = l.getQuantity();

                if (!productSales.containsKey(productId)) {
                    productSales.put(productId, new ProductSalesLine(productId, productName));
                }
                Objects.requireNonNull(productSales.get(productId)).add(lineSubtotal, quantity);
            }

            // Customer balances
            if (cust != null && (balance > 0.005 || balance < -0.005)) {
                int custId = Integer.parseInt(cust.getId());
                if (!custBalances.containsKey(custId)) {
                    custBalances.put(custId, new ZTicket(ctx).new CustBalanceLine(custId));
                }
                custBalances.get(custId).add(balance);
            }
        }
    }

    private PaymentDetail getOrCreatePaymentDetail(PaymentMode mode) {
        if (!payments.containsKey(mode.getId())) {
            payments.put(mode.getId(), new PaymentDetail());
        }
        return payments.get(mode.getId());
    }

    public int getTicketCount() { return ticketCount; }
    public double getTotal() { return total; }
    public double getSubtotal() { return subtotal; }
    public Map<Integer, PaymentDetail> getPayments() { return payments; }
    public Map<Integer, ZTicket.TaxLine> getTaxLines() { return taxLines; }
    public Map<Integer, ZTicket.CatSalesLine> getCatSales() { return catSales; }
    public Map<String, ZTicket.CatTaxLine> getCatTaxes() { return catTaxes; }
    public Map<Integer, ZTicket.CustBalanceLine> getCustBalances() { return custBalances; }

    // NEW: Getter for product sales
    public Map<Integer, ProductSalesLine> getProductSales() { return productSales; }

    private static String catTaxIdKey(String reference, int taxId) {
        return (reference == null) ? "-" + taxId : reference + "-" + taxId;
    }
}
