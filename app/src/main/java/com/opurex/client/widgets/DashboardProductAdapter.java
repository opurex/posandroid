package com.opurex.client.widgets;

import android.content.Context;
import android.view.*;
import android.widget.*;
import com.opurex.client.R;
import com.opurex.client.models.ProductSalesLine;
import java.text.DecimalFormat;
import java.util.List;

public class DashboardProductAdapter extends BaseAdapter {

    private Context context;
    private List<ProductSalesLine> productList;
    private DecimalFormat currencyFormat = new DecimalFormat("#,##0.00");
    private DecimalFormat quantityFormat = new DecimalFormat("#,##0.##");

    public DashboardProductAdapter(Context context, List<ProductSalesLine> productList) {
        this.context = context;
        this.productList = productList;
    }

    @Override
    public int getCount() {
        return productList.size();
    }

    @Override
    public ProductSalesLine getItem(int position) {
        return productList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    private static class ViewHolder {
        TextView productName, productRevenue, productQuantity;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.dashboard_product_item, parent, false);
            holder = new ViewHolder();
            holder.productName = convertView.findViewById(R.id.product_name);
            holder.productRevenue = convertView.findViewById(R.id.product_revenue);
            holder.productQuantity = convertView.findViewById(R.id.product_quantity);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        ProductSalesLine product = getItem(position);
        holder.productName.setText(product.getProductName());
        holder.productRevenue.setText(currencyFormat.format(product.getTotalRevenue()));
        holder.productQuantity.setText("Qty: " + quantityFormat.format(product.getTotalQuantity()));
        return convertView;
    }
}
