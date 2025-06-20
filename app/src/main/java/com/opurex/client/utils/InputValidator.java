
package com.opurex.client.utils;

import android.content.Context;
import android.text.TextUtils;
import android.util.Patterns;
import java.math.BigDecimal;
import java.util.regex.Pattern;

/**
 * Input validation utility with user-friendly error feedback
 */
public class InputValidator {
    
    private static final Pattern BARCODE_PATTERN = Pattern.compile("^[0-9]{8,13}$");
    private static final Pattern PHONE_PATTERN = Pattern.compile("^[+]?[0-9]{10,15}$");
    
    /**
     * Validate and format currency input
     */
    public static class CurrencyValidation {
        public boolean isValid;
        public String formattedValue;
        public String errorMessage;
        
        public CurrencyValidation(boolean isValid, String formattedValue, String errorMessage) {
            this.isValid = isValid;
            this.formattedValue = formattedValue;
            this.errorMessage = errorMessage;
        }
    }
    
    /**
     * Validate currency input and format it properly
     */
    public static CurrencyValidation validateCurrency(Context context, String input) {
        if (TextUtils.isEmpty(input)) {
            return new CurrencyValidation(false, "", "Amount cannot be empty");
        }
        
        try {
            // Remove any formatting and parse
            String cleanInput = input.replaceAll("[^0-9.]", "");
            BigDecimal amount = new BigDecimal(cleanInput);
            
            if (amount.compareTo(BigDecimal.ZERO) < 0) {
                return new CurrencyValidation(false, "", "Amount cannot be negative");
            }
            
            if (amount.compareTo(new BigDecimal("999999.99")) > 0) {
                return new CurrencyValidation(false, "", "Amount exceeds maximum limit");
            }
            
            // Format with 2 decimal places and add commas
            String formatted = String.format("%,.2f", amount.doubleValue());
            return new CurrencyValidation(true, formatted, "");
            
        } catch (NumberFormatException e) {
            return new CurrencyValidation(false, "", "Invalid amount format");
        }
    }
    
    /**
     * Validate email address
     */
    public static boolean isValidEmail(String email) {
        return !TextUtils.isEmpty(email) && Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }
    
    /**
     * Validate phone number
     */
    public static boolean isValidPhone(String phone) {
        if (TextUtils.isEmpty(phone)) return false;
        String cleanPhone = phone.replaceAll("[\\s()-]", "");
        return PHONE_PATTERN.matcher(cleanPhone).matches();
    }
    
    /**
     * Validate barcode format
     */
    public static boolean isValidBarcode(String barcode) {
        if (TextUtils.isEmpty(barcode)) return false;
        return BARCODE_PATTERN.matcher(barcode).matches();
    }
    
    /**
     * Format phone number for display
     */
    public static String formatPhoneNumber(String phone) {
        if (!isValidPhone(phone)) return phone;
        
        String cleanPhone = phone.replaceAll("[\\s()-]", "");
        if (cleanPhone.length() == 10) {
            return String.format("(%s) %s-%s", 
                cleanPhone.substring(0, 3),
                cleanPhone.substring(3, 6),
                cleanPhone.substring(6));
        }
        return phone;
    }
    
    /**
     * Validate customer name
     */
    public static boolean isValidCustomerName(String name) {
        return !TextUtils.isEmpty(name) && name.trim().length() >= 2;
    }
    
    /**
     * Validate quantity input
     */
    public static boolean isValidQuantity(String quantity) {
        if (TextUtils.isEmpty(quantity)) return false;
        try {
            double qty = Double.parseDouble(quantity);
            return qty > 0 && qty <= 9999;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
