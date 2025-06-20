
package com.opurex.client;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.widget.TextView;
import android.widget.ListView;
import android.graphics.Color;
import androidx.core.content.ContextCompat;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.*;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.utils.ColorTemplate;

import com.opurex.client.activities.POSConnectedTrackedActivity;
import com.opurex.client.data.Data;
import com.opurex.client.models.*;
import com.opurex.client.widgets.DashboardProductAdapter;

import java.util.*;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;

public class SalesDashboard extends POSConnectedTrackedActivity {
    
    private TextView todaySalesTotal;
    private TextView transactionCount;
    private TextView avgTransactionValue;
    private TextView hourlyRevenue;
    private LineChart hourlyChart;
    private PieChart topProductsChart;
    private ListView topProductsList;
    
    private Handler refreshHandler;
    private Runnable refreshRunnable;
    private DecimalFormat currencyFormat;
    private SimpleDateFormat timeFormat;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.sales_dashboard);
        
        initializeViews();
        setupFormatters();
        setupAutoRefresh();
        loadDashboardData();
    }
    
    private void initializeViews() {
        todaySalesTotal = findViewById(R.id.today_sales_total);
        transactionCount = findViewById(R.id.transaction_count);
        avgTransactionValue = findViewById(R.id.avg_transaction_value);
        hourlyRevenue = findViewById(R.id.hourly_revenue);
        hourlyChart = findViewById(R.id.hourly_chart);
        topProductsChart = findViewById(R.id.top_products_chart);
        topProductsList = findViewById(R.id.top_products_list);
    }
    
    private void setupFormatters() {
        currencyFormat = new DecimalFormat("#,##0.00");
        timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
    }
    
    private void setupAutoRefresh() {
        refreshHandler = new Handler();
        refreshRunnable = new Runnable() {
            @Override
            public void run() {
                loadDashboardData();
                refreshHandler.postDelayed(this, 30000); // Refresh every 30 seconds
            }
        };
    }
    
    private void loadDashboardData() {
        Calendar today = Calendar.getInstance();
        today.set(Calendar.HOUR_OF_DAY, 0);
        today.set(Calendar.MINUTE, 0);
        today.set(Calendar.SECOND, 0);
        
        try {
            // Get today's tickets
            List<Ticket> todaysTickets = Data.Ticket.getTicketsByDate(today.getTime(), new Date());
            
            updateSalesMetrics(todaysTickets);
            updateHourlyChart(todaysTickets);
            updateTopProductsChart(todaysTickets);
            updateTopProductsList(todaysTickets);
            
        } catch (Exception e) {
            // Handle error gracefully
            showErrorMessage(getString(R.string.dashboard_load_error));
        }
    }
    
    private void updateSalesMetrics(List<Ticket> tickets) {
        double totalSales = 0;
        int totalTransactions = tickets.size();
        
        for (Ticket ticket : tickets) {
            if (ticket.isFinished()) {
                totalSales += ticket.getTicketPrice();
            }
        }
        
        double avgTransaction = totalTransactions > 0 ? totalSales / totalTransactions : 0;
        double currentHourRevenue = getCurrentHourRevenue(tickets);
        
        todaySalesTotal.setText(getString(R.string.currency_symbol) + currencyFormat.format(totalSales));
        transactionCount.setText(String.valueOf(totalTransactions));
        avgTransactionValue.setText(getString(R.string.currency_symbol) + currencyFormat.format(avgTransaction));
        hourlyRevenue.setText(getString(R.string.currency_symbol) + currencyFormat.format(currentHourRevenue));
    }
    
    private double getCurrentHourRevenue(List<Ticket> tickets) {
        Calendar currentHour = Calendar.getInstance();
        currentHour.set(Calendar.MINUTE, 0);
        currentHour.set(Calendar.SECOND, 0);
        
        Calendar nextHour = (Calendar) currentHour.clone();
        nextHour.add(Calendar.HOUR_OF_DAY, 1);
        
        double hourlyRevenue = 0;
        for (Ticket ticket : tickets) {
            Date ticketDate = ticket.getDate();
            if (ticketDate.after(currentHour.getTime()) && ticketDate.before(nextHour.getTime()) && ticket.isFinished()) {
                hourlyRevenue += ticket.getTicketPrice();
            }
        }
        return hourlyRevenue;
    }
    
    private void updateHourlyChart(List<Ticket> tickets) {
        Map<Integer, Double> hourlySales = new HashMap<>();
        
        // Initialize all hours with 0
        for (int i = 0; i < 24; i++) {
            hourlySales.put(i, 0.0);
        }
        
        // Calculate sales by hour
        Calendar cal = Calendar.getInstance();
        for (Ticket ticket : tickets) {
            if (ticket.isFinished()) {
                cal.setTime(ticket.getDate());
                int hour = cal.get(Calendar.HOUR_OF_DAY);
                hourlySales.put(hour, hourlySales.get(hour) + ticket.getTicketPrice());
            }
        }
        
        // Prepare chart data
        List<Entry> entries = new ArrayList<>();
        for (int i = 0; i < 24; i++) {
            entries.add(new Entry(i, hourlySales.get(i).floatValue()));
        }
        
        LineDataSet dataSet = new LineDataSet(entries, getString(R.string.hourly_sales));
        dataSet.setColor(ContextCompat.getColor(this, R.color.primary));
        dataSet.setCircleColor(ContextCompat.getColor(this, R.color.primary));
        dataSet.setLineWidth(2f);
        dataSet.setCircleRadius(4f);
        dataSet.setFillAlpha(65);
        dataSet.setFillColor(ContextCompat.getColor(this, R.color.primary));
        dataSet.setHighLightColor(Color.rgb(244, 117, 117));
        dataSet.setDrawCircleHole(false);
        
        LineData lineData = new LineData(dataSet);
        hourlyChart.setData(lineData);
        
        Description description = new Description();
        description.setText("");
        hourlyChart.setDescription(description);
        hourlyChart.invalidate();
    }
    
    private void updateTopProductsChart(List<Ticket> tickets) {
        Map<String, Double> productSales = new HashMap<>();
        
        for (Ticket ticket : tickets) {
            if (ticket.isFinished()) {
                for (TicketLine line : ticket.getLines()) {
                    String productName = line.getProduct().getLabel();
                    double lineTotal = line.getPrice() * line.getQuantity();
                    productSales.put(productName, productSales.getOrDefault(productName, 0.0) + lineTotal);
                }
            }
        }
        
        // Get top 5 products
        List<Map.Entry<String, Double>> sortedProducts = new ArrayList<>(productSales.entrySet());
        sortedProducts.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));
        
        List<PieEntry> entries = new ArrayList<>();
        int maxProducts = Math.min(5, sortedProducts.size());
        
        for (int i = 0; i < maxProducts; i++) {
            Map.Entry<String, Double> product = sortedProducts.get(i);
            entries.add(new PieEntry(product.getValue().floatValue(), product.getKey()));
        }
        
        PieDataSet dataSet = new PieDataSet(entries, getString(R.string.top_products));
        dataSet.setColors(ColorTemplate.MATERIAL_COLORS);
        dataSet.setValueTextSize(12f);
        dataSet.setValueTextColor(Color.WHITE);
        
        PieData pieData = new PieData(dataSet);
        topProductsChart.setData(pieData);
        
        Description description = new Description();
        description.setText("");
        topProductsChart.setDescription(description);
        topProductsChart.invalidate();
    }
    
    private void updateTopProductsList(List<Ticket> tickets) {
        Map<String, ProductSalesInfo> productStats = new HashMap<>();
        
        for (Ticket ticket : tickets) {
            if (ticket.isFinished()) {
                for (TicketLine line : ticket.getLines()) {
                    String productId = line.getProduct().getId();
                    ProductSalesInfo info = productStats.getOrDefault(productId, 
                        new ProductSalesInfo(line.getProduct()));
                    info.addSale(line.getQuantity(), line.getPrice() * line.getQuantity());
                    productStats.put(productId, info);
                }
            }
        }
        
        List<ProductSalesInfo> sortedProducts = new ArrayList<>(productStats.values());
        sortedProducts.sort((a, b) -> Double.compare(b.totalRevenue, a.totalRevenue));
        
        DashboardProductAdapter adapter = new DashboardProductAdapter(this, sortedProducts);
        topProductsList.setAdapter(adapter);
    }
    
    private void showErrorMessage(String message) {
        // Show toast or dialog with error message
        android.widget.Toast.makeText(this, message, android.widget.Toast.LENGTH_LONG).show();
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        refreshHandler.post(refreshRunnable);
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        refreshHandler.removeCallbacks(refreshRunnable);
    }
    
    // Inner class for product sales information
    public static class ProductSalesInfo {
        public Product product;
        public double totalQuantity;
        public double totalRevenue;
        public int transactionCount;
        
        public ProductSalesInfo(Product product) {
            this.product = product;
            this.totalQuantity = 0;
            this.totalRevenue = 0;
            this.transactionCount = 0;
        }
        
        public void addSale(double quantity, double revenue) {
            this.totalQuantity += quantity;
            this.totalRevenue += revenue;
            this.transactionCount++;
        }
    }
}
