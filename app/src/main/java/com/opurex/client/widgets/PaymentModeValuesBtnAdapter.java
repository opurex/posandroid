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

import com.opurex.client.models.PaymentMode;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import java.util.List;

public class PaymentModeValuesBtnAdapter extends BaseAdapter implements PaymentModeValueBtnItem.Listener
{
    private List<PaymentMode.Value> values;
    private List<Integer> counts;
    private PaymentModeValueBtnItem.Listener listener;

    public PaymentModeValuesBtnAdapter(List<PaymentMode.Value> values, List<Integer> counts) {
        super();
        this.values = values;
        this.counts = counts;
    }

    @Override
    public boolean areAllItemsEnabled() {
        return true;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public Object getItem(int position) {
        return this.values.get(position);
    }

    public int getCount(int position) {
        return this.counts.get(position);
    }

    @Override
    public int getCount() {
        return this.values.size();
    }

    public List<PaymentMode.Value> getValues() {
        return this.values;
    }

    public List<Integer> getCounts() {
        return this.counts;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        PaymentMode.Value v = this.values.get(position);
        int count = this.counts.get(position);
        if (convertView != null && convertView instanceof PaymentModeValueBtnItem) {
            // Reuse the view
            PaymentModeValueBtnItem item = (PaymentModeValueBtnItem) convertView;
            item.reuse(v, this.counts.get(position));
            return item;
        } else {
            // Create the view
            Context ctx = parent.getContext();
            PaymentModeValueBtnItem item = new PaymentModeValueBtnItem(ctx, v, count);
            item.setListener(this);
            return item;
        }
    }

    public void setListener(PaymentModeValueBtnItem.Listener listener) {
        this.listener = listener;
    }

    public void coinAdded(double amount, int newCount) {
        for (int i = 0; i < this.values.size(); i++) {
            PaymentMode.Value v = this.values.get(i);
            if (v.getValue() == amount) {
                this.counts.set(i, newCount);
            }
        }
        if (this.listener != null) {
            this.listener.coinAdded(amount, newCount);
        }
    }
    public void countUpdated(double amount, int newCount) {
        for (int i = 0; i < this.values.size(); i++) {
            PaymentMode.Value v = this.values.get(i);
            if (v.getValue() == amount) {
                this.counts.set(i, newCount);
            }
        }
        if (this.listener != null) {
            this.listener.countUpdated(amount, newCount);
        }
    }
}
