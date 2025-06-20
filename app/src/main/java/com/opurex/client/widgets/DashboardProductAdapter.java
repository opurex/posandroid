
package com.opurex.client.widgets;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;
import java.text.DecimalFormat;
import java.util.List;

import com.opurex.client.R;
import com.opurex.client.SalesDashboard.ProductSalesInfo;

public class DashboardProductAdapter extends BaseAdapter {
    
    private final Context context;
    private final List<ProductSalesInfo> products;
    private final LayoutInflater inflater;
    private final DecimalFormat currencyFormat;
    private final DecimalFormat quantityFormat;
    
    public DashboardProductAdapter(Context context, List<ProductSalesInfo> products) {
        this.context = context;
        this.products = products;
        this.inflater = LayoutInflater.from(context);
        this.currencyFormat = new DecimalFormat("#,##0.00");
        this.quantityFormat = new DecimalFormat("#,##0.#");
    }
    
    @Override
    public int getCount() {
        return products.size();
    }
    
    @Override
    public ProductSalesInfo getItem(int position) {
        return products.get(position);
    }
    
    @Override
    public long getItemId(int position) {
        return position;
    }
    
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.dashboard_product_item, parent, false);
            holder = new ViewHolder();
            holder.productName = convertView.findViewById(R.id.product_name);
            holder.productRevenue = convertView.findViewById(R.id.product_revenue);
            holder.productQuantity = convertView.findViewById(R.id.product_quantity);
            holder.productTransactions = convertView.findViewById(R.id.product_transactions);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }
        
        ProductSalesInfo product = getItem(position);
        
        holder.productName.setText(product.product.getLabel());
        holder.productRevenue.setText(context.getString(R.string.currency_symbol) + 
            currencyFormat.format(product.totalRevenue));
        holder.productQuantity.setText(context.getString(R.string.qty_label) + " " + 
            quantityFormat.format(product.totalQuantity));
        holder.productTransactions.setText(String.valueOf(product.transactionCount) + " " + 
            context.getString(R.string.transactions_label));
        
        return convertView;
    }
    
    private static class ViewHolder {
        TextView productName;
        TextView productRevenue;
        TextView productQuantity;
        TextView productTransactions;
    }
}
