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

import com.opurex.client.interfaces.PaymentEditListener;
import com.opurex.client.models.Payment;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import com.opurex.client.utils.ReadList;

public class PaymentsAdapter extends BaseAdapter {

    private ReadList<Payment> payments;
    private PaymentEditListener listener;

    public PaymentsAdapter(ReadList<Payment> payments, PaymentEditListener l) {
        super();
        this.payments = payments;
        this.listener = l;
    }

    public void setPayments(ReadList<Payment> payments) {
        this.payments = payments;
    }

    @Override
    public boolean areAllItemsEnabled() {
        return false;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public Object getItem(int position) {
        return this.payments.get(position);
    }

    @Override
    public int getCount() {
        return this.payments.size();
    }
    
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        Payment payment = this.payments.get(position);
        if (convertView != null && convertView instanceof PaymentItem) {
            // Reuse the view
            PaymentItem item = (PaymentItem) convertView;
            item.reuse(payment, parent.getContext());
            return item;
        } else {
            // Create the view
            Context ctx = parent.getContext();
            PaymentItem item = new PaymentItem(ctx, payment);
            item.setEditListener(this.listener);
            return item;
        }
    }
}
