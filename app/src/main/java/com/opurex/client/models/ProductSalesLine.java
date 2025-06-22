package com.opurex.client.models;

public class ProductSalesLine {
    private int productId;
    private String productName;
    private double totalRevenue;
    private double totalQuantity;

    public ProductSalesLine(int productId, String productName) {
        this.productId = productId;
        this.productName = productName;
        this.totalRevenue = 0.0;
        this.totalQuantity = 0.0;
    }

    public void add(double revenue, double quantity) {
        this.totalRevenue += revenue;
        this.totalQuantity += quantity;
    }

    public int getProductId() {
        return productId;
    }

    public String getProductName() {
        return productName;
    }

    public double getTotalRevenue() {
        return totalRevenue;
    }

    public double getTotalQuantity() {
        return totalQuantity;
    }
}
