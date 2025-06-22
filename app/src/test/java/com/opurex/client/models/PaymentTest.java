package com.opurex.client.models;

import org.json.JSONObject;
import org.json.JSONArray;
import org.junit.Test;

import static org.junit.Assert.*;

public class PaymentTest {

    @Test
    public void testPaymentToJSON() throws Exception {
        // Build Currency JSONObject
        JSONObject currencyJson = new JSONObject();
        currencyJson.put("id", 1);
        currencyJson.put("reference", "KES");
        currencyJson.put("label", "Kenyan Shilling");
        currencyJson.put("symbol", "KSh");
        currencyJson.put("decimalSeparator", ".");
        currencyJson.put("thousandsSeparator", ",");
        currencyJson.put("format", "#,##0.00");
        currencyJson.put("rate", 1.0);
        currencyJson.put("main", true);
        currencyJson.put("visible", true);
        Currency currency = new Currency(currencyJson);

        // Build PaymentMode JSONObject
        JSONObject modeJson = new JSONObject();
        modeJson.put("id", 1);
        modeJson.put("reference", "CASH");
        modeJson.put("label", "Cash Payment");
        modeJson.put("backLabel", "Refund");
        modeJson.put("type", 0);
        modeJson.put("hasImage", false);
        modeJson.put("visible", true);
        modeJson.put("dispOrder", 1);
        modeJson.put("values", new JSONArray());
        modeJson.put("returns", new JSONArray());
        PaymentMode mode = PaymentMode.fromJSON(modeJson);

        // Create Payment
        double amount = 100.0;
        double given = 120.0;
        Payment payment = new Payment(mode, currency, amount, given);

        // Convert Payment to JSON
        JSONObject paymentJson = payment.toJSON();

        // Assertions based on your real toJSON() structure
        assertNotNull(paymentJson);
        assertEquals(mode.getCode(), paymentJson.getJSONObject("mode").getString("code"));
        assertEquals(currency.getId(), paymentJson.getJSONObject("currency").getInt("id"));
        assertEquals(amount, paymentJson.getDouble("amount"), 0.001);
        assertEquals(given, paymentJson.getDouble("given"), 0.001);
    }
}
