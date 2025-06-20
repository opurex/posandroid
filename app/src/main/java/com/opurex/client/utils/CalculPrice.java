package com.opurex.client.utils;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Created by nsvir on 24/08/15.
 * n.svirchevsky@gmail.com
 */
public class CalculPrice {

    public class Type {
        public static final int NONE = 0;
        public static final int DISCOUNT = 1;
        public static final int DISCOUNT_COST = 2;
        public static final int TAXE = 4;
        public static final int TAXE_COST = 8;
    }

    public static final int DEFAULT_DECIMAL_NUMBER = 5;

    private static boolean hasOption(int binaryMask, int model) {
        return (binaryMask & model) == model;
    }

    public static final double getGenericPrice(double price, double discount, double taxe, int binaryMask) {
        if (hasOption(binaryMask, Type.DISCOUNT_COST)) {
            return getDiscountCost(price, discount);
        }
        if (hasOption(binaryMask, Type.TAXE_COST)) {
            return getTaxCost(price, taxe);
        }
        if (hasOption(binaryMask, Type.DISCOUNT)) {
            price = applyDiscount(price, discount);
        }
        if (hasOption(binaryMask, Type.TAXE)) {
            price = applyTax(price, taxe);
        }
        return price;
    }

    public static final double removeTaxe(double price, double tax) {
        return round(price / (1 + tax));
    }

    public static final double getDiscountCost(double price, double discount) {
        return round(price * discount);
    }

    public static final double applyDiscount(double price, double discount) {
        return round(price * (1 - discount));
    }

    public static final double mergeDiscount(double productDiscount, double ticketDiscount) {
        return round(productDiscount + ticketDiscount - (productDiscount * ticketDiscount));
    }

    public static double applyTax(double price, double taxRate) {
        return round(price + getTaxCost(price, taxRate));
    }

    public static double getTaxCost(double price, double taxRate) {
        return round(price * taxRate);
    }

    public static double round(double number) {
        return round(number, DEFAULT_DECIMAL_NUMBER);
    }

    public static double round(double number, int decimalNumber) {
        return new BigDecimal(number).setScale(decimalNumber, RoundingMode.HALF_UP).doubleValue();
    }

    // Port from desktop, some functions are duplicated.
    /** Round a value to 2 decimals. */
    private static double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    /** Multiply a base price by quantity to get a rounded price.
     * This is the first operation to do. It gives the base on 2 decimals
     * that can be used to compute discount on. */
    public static double inQuantity(double unitPrice, double quantity) {
        return CalculPrice.round2(unitPrice * quantity);
    }

    /** Apply a discount to a price in quantity.
     * It is rounded with 2 decimals.
     * This is the last operation to do. It gives the final value on 2 decimals
     * that can be used to add or remove tax from. */
    public static double discount(double price, double discountRate) {
        return CalculPrice.round2(price * (1.0 - discountRate));
    }

    /** Extract the final tax amount from a taxed final price.
     * It is done by computing untaxed price (on 2 decimals)  and substracting
     * it from the final taxed price (which is also on 2 decimals).
     * @param finalTaxedPrice The final taxed price. There shouldn't be any
     * other operation on it and it must be rounded on 2 decimals.
     * @param taxRate The tax rate included in the price. */
    public static double extractTax(double finalTaxedPrice, double taxRate) {
        return CalculPrice.round2(finalTaxedPrice
                - CalculPrice.untax(finalTaxedPrice, taxRate));
    }
    /** Get the final untaxed price from a final taxed price and a tax rate.
     * @param finalTaxedPrice The final taxed price. There shouldn't be any
     * other operation on it and it must be rounded on 2 decimals.
     * @param taxRate The tax rate included in the price. */
    public static double untax(double finalTaxedPrice, double taxRate) {
        return CalculPrice.round2(finalTaxedPrice / (1.0 + taxRate));
    }
    /** Get the final tax amount from a final untaxed price and a tax rate.
     * @param finalUntaxedPrice The final untaxed price. There shouldn't be any
     * other operation on it and it must be rounded on 2 decimals.
     * @param taxRate The tax rate to include in the price. */
    public static double getTax(double finalUntaxedPrice, double taxRate) {
        return CalculPrice.round2(finalUntaxedPrice * taxRate);
    }
    /** Get the final taxed price from a final untaxed price and a tax rate.
     * It is done by computing the tax amount (on 2 decimals) and adding it
     * to the final untaxed price (which is also on 2 decimals).
     * @param finalUntaxedPrice The final untaxed price. There shouldn't be any
     * other operation on it and it must be rounded on 2 decimals.
     * @param taxRate The tax rate to include in the price. */
    public static double addTax(double finalUntaxedPrice, double taxRate) {
        return CalculPrice.round2(finalUntaxedPrice
                + CalculPrice.getTax(finalUntaxedPrice, taxRate));
    }

    /** Add a final price to an other. Both must be rounded on 2 decimals. */
    public static double add(double price1, double price2) {
        return CalculPrice.round2(price1 + price2);
    }

}
