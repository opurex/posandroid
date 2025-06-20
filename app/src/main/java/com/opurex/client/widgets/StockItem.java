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
package com.opurex.client.widgets;

import com.opurex.client.R;
import com.opurex.client.models.Product;
import com.opurex.client.models.Stock;

import android.content.Context;
import android.view.LayoutInflater;
import android.widget.TextView;
import android.widget.RelativeLayout;

public class StockItem extends RelativeLayout {

    private Stock stock;
    private Product product;

    private TextView label;
    private TextView quantity;

    public StockItem (Context context, Stock stock, Product product) {
        super(context);
        LayoutInflater.from(context).inflate(R.layout.stock_item,
                this, true);
        this.label = (TextView) this.findViewById(R.id.product_label);
        this.quantity = (TextView) this.findViewById(R.id.product_quantity);
        this.reuse(stock, product);
    }

    public void reuse(Stock stock, Product product) {
        this.stock = stock;
        this.product = product;
        this.label.setText(this.product.getLabel());
        this.quantity.setText(String.valueOf(this.stock.getQuantity()));
    }

    public Stock getStock() {
        return this.stock;
    }

    public Product getProduct() {
        return this.product;
    }
}
