package com.opurex.client;

import android.os.Bundle;
import android.os.Handler;
import android.widget.*;
import android.graphics.Color;
import androidx.core.content.ContextCompat;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.*;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.opurex.client.activities.POSConnectedTrackedActivity;
import com.opurex.client.data.Data;
import com.opurex.client.drivers.POSDeviceManager;
import com.opurex.client.drivers.utils.DeviceManagerEvent;
import com.opurex.client.models.*;
import com.opurex.client.widgets.DashboardProductAdapter;

import java.util.*;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;

public class SalesDashboard extends POSConnectedTrackedActivity {

    private TextView todaySalesTotal, transactionCount, avgTransactionValue;
    private LineChart hourlyChart;
    private PieChart topProductsChart;
    private Handler refreshHandler;
    private Runnable refreshRunnable;
    private DecimalFormat currencyFormat;
    private SimpleDateFormat timeFormat;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.sales_dashboard);

        todaySalesTotal = findViewById(R.id.today_sales_total);
        transactionCount = findViewById(R.id.transaction_count);
        avgTransactionValue = findViewById(R.id.avg_transaction_value);
        hourlyChart = findViewById(R.id.hourly_chart);
        topProductsChart = findViewById(R.id.top_products_chart);

        currencyFormat = new DecimalFormat("#,##0.00");
        timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());

        refreshHandler = new Handler();
        refreshRunnable = () -> {
            loadDashboardData();
            refreshHandler.postDelayed(refreshRunnable, 30000);
        };
    }

    /**
     * @param manager
     * @param event
     */
    @Override
    public void onDeviceManagerEvent(POSDeviceManager manager, DeviceManagerEvent event) {

    }

    @Override
    public void onResume() {
        super.onResume();
        refreshHandler.post(refreshRunnable);
    }

    @Override
    public void onPause() {
        super.onPause();
        refreshHandler.removeCallbacks(refreshRunnable);
    }

    private void loadDashboardData() {
        try {
            Calendar todayStart = Calendar.getInstance();
            todayStart.set(Calendar.HOUR_OF_DAY, 0);
            todayStart.set(Calendar.MINUTE, 0);
            todayStart.set(Calendar.SECOND, 0);
            todayStart.set(Calendar.MILLISECOND, 0);

            List<Receipt> allReceipts = Data.Receipt.getReceipts(this);
            List<Receipt> todaysReceipts = new ArrayList<>();

            for (Receipt r : allReceipts) {
                long receiptTimeMs = r.getPaymentTime() * 1000L;
                if (receiptTimeMs >= todayStart.getTimeInMillis()) {
                    todaysReceipts.add(r);
                }
            }

            PeriodZTicket zticket = new PeriodZTicket(this, todaysReceipts);
            updateSalesMetrics(zticket);
            updateHourlyChart(todaysReceipts);
            updateTopProductsChart(zticket);
            updateProductSalesList(zticket);
        } catch (Exception e) {
            Toast.makeText(this, "Failed to load dashboard: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void updateSalesMetrics(PeriodZTicket zticket) {
        double totalSales = zticket.getTotal();
        int totalTransactions = zticket.getTicketCount();
        double avgTransaction = totalTransactions > 0 ? totalSales / totalTransactions : 0;

        todaySalesTotal.setText(currencyFormat.format(totalSales));
        transactionCount.setText(String.valueOf(totalTransactions));
        avgTransactionValue.setText(currencyFormat.format(avgTransaction));
    }
    private void updateProductSalesList(PeriodZTicket zticket) {
        ListView productSalesList = findViewById(R.id.product_sales_list);
        List<ProductSalesLine> productList = new ArrayList<>(zticket.getProductSales().values());
        Collections.sort(productList, (a, b) -> Double.compare(b.getTotalRevenue(), a.getTotalRevenue()));

        DashboardProductAdapter adapter = new DashboardProductAdapter(this, productList);
        productSalesList.setAdapter(adapter);
    }
    private void updateHourlyChart(List<Receipt> receipts) {
        Map<Integer, Double> hourlyMap = new HashMap<>();
        for (int i = 0; i < 24; i++) hourlyMap.put(i, 0.0);

        Calendar cal = Calendar.getInstance();
        for (Receipt r : receipts) {
            cal.setTimeInMillis(r.getPaymentTime() * 1000L);
            int hour = cal.get(Calendar.HOUR_OF_DAY);

            double receiptTotal = 0.0;
            for (Payment p : r.getPayments()) {
                receiptTotal += p.getGiven();
            }
            hourlyMap.put(hour, hourlyMap.get(hour) + receiptTotal);
        }

        List<Entry> entries = new ArrayList<>();
        for (int i = 0; i < 24; i++) {
            entries.add(new Entry(i, hourlyMap.get(i).floatValue()));
        }

        LineDataSet dataSet = new LineDataSet(entries, "Hourly Sales");
        dataSet.setColor(ContextCompat.getColor(this, R.color.primary));
        dataSet.setCircleColor(ContextCompat.getColor(this, R.color.primary));
        dataSet.setLineWidth(2f);
        dataSet.setCircleRadius(4f);
        dataSet.setHighLightColor(Color.rgb(244, 117, 117));
        dataSet.setDrawCircleHole(false);
        LineData data = new LineData(dataSet);
        hourlyChart.setData(data);
        hourlyChart.setDescription(new Description());
        hourlyChart.invalidate();
    }

    private void updateTopProductsChart(PeriodZTicket zticket) {
        Map<Integer, ZTicket.CatSalesLine> catSales = zticket.getCatSales();
        List<Map.Entry<Integer, ZTicket.CatSalesLine>> entriesList = new ArrayList<>(catSales.entrySet());

        Collections.sort(entriesList, new Comparator<Map.Entry<Integer, ZTicket.CatSalesLine>>() {
            @Override
            public int compare(Map.Entry<Integer, ZTicket.CatSalesLine> o1, Map.Entry<Integer, ZTicket.CatSalesLine> o2) {
                return Double.compare(o2.getValue().getAmount(), o1.getValue().getAmount());
            }
        });

        List<PieEntry> pieEntries = new ArrayList<>();
        for (int i = 0; i < Math.min(5, entriesList.size()); i++) {
            ZTicket.CatSalesLine cs = entriesList.get(i).getValue();
            pieEntries.add(new PieEntry((float) cs.getAmount(), cs.getLabel()));
        }

        PieDataSet pieDataSet = new PieDataSet(pieEntries, "Top Categories");
        pieDataSet.setColors(ColorTemplate.MATERIAL_COLORS);
        pieDataSet.setValueTextColor(Color.WHITE);
        pieDataSet.setValueTextSize(12f);
        PieData pieData = new PieData(pieDataSet);
        topProductsChart.setData(pieData);
        topProductsChart.setDescription(new Description());
        topProductsChart.invalidate();
    }

    /**
     * Called when pointer capture is enabled or disabled for the current window.
     *
     * @param hasCapture True if the window has pointer capture.
     */
    @Override
    public void onPointerCaptureChanged(boolean hasCapture) {
        super.onPointerCaptureChanged(hasCapture);
    }


}
