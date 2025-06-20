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
import com.opurex.client.models.PaymentMode;

import android.content.Context;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;

public class PaymentModeValueBtnItem extends LinearLayout implements TextWatcher
{
    private PaymentMode.Value value;
    private int count;
    private Button button;
    private EditText input;
    private PaymentModeValueBtnItem.Listener listener;

    public PaymentModeValueBtnItem(Context context, PaymentMode.Value value, int count) {
        super(context);
        LayoutInflater.from(context).inflate(R.layout.value_btn_item, this, true);
        this.setOrientation(LinearLayout.VERTICAL);
        this.button = (Button) this.findViewById(R.id.value_button);
        this.input = (EditText) this.findViewById(R.id.count);
        this.input.setInputType(InputType.TYPE_NULL);
        this.button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                PaymentModeValueBtnItem t = PaymentModeValueBtnItem.this;
                t.input.removeTextChangedListener(t);
                t.setCount(t.getCount() + 1);
                t.input.requestFocus();
                t.input.addTextChangedListener(t);
                if (t.listener != null) {
                    t.listener.coinAdded(t.getValue().getValue(), t.getCount());
                }
            }
        });
        this.input.addTextChangedListener(this);
        this.reuse(value, count);
    }

    public void reuse(PaymentMode.Value value, int count) {
        this.value = value;
        this.count = count;
        this.input.removeTextChangedListener(this);
        this.input.setText(Integer.toString(this.count));
        this.input.addTextChangedListener(this);
        long val = Math.round(this.value.getValue() * 100);
        if (val == 5000) {
            this.button.setBackgroundResource(R.drawable.currency50);
        } else if (val == 2000) {
            this.button.setBackgroundResource(R.drawable.currency20);
        } else if (val == 1000) {
            this.button.setBackgroundResource(R.drawable.currency10);
        } else if (val == 500) {
            this.button.setBackgroundResource(R.drawable.currency5);
        } else if (val == 200) {
            this.button.setBackgroundResource(R.drawable.currency2);
        } else if (val == 100) {
            this.button.setBackgroundResource(R.drawable.currency1);
        } else if (val == 50) {
            this.button.setBackgroundResource(R.drawable.currency05);
        } else if (val == 20) {
            this.button.setBackgroundResource(R.drawable.currency02);
        } else if (val == 10) {
            this.button.setBackgroundResource(R.drawable.currency01);
        } else if (val == 5) {
            this.button.setBackgroundResource(R.drawable.currency005);
        } else if (val == 2) {
            this.button.setBackgroundResource(R.drawable.currency002);
        } else if (val == 1) {
            this.button.setBackgroundResource(R.drawable.currency001);
        } else {
            this.button.setBackground(null);
        }
        this.button.setText(this.getContext().getString(R.string.ticket_total, this.value.getValue()));
    }

    public PaymentMode.Value getValue() {
        return this.value;
    }

    public int getCount() {
        return this.count;
    }

    public double getAmount() {
        return this.count * this.value.getValue();
    }

    public void setCount(int count) {
        this.count = count;
        this.input.setText(Integer.toString(count));
    }

    private void updateCount() {
        try {
            this.count = Integer.parseInt(this.input.getText().toString());
        } catch (NumberFormatException e) {
            this.count = 0;
        }
    }

    public void setListener(PaymentModeValueBtnItem.Listener l) {
        this.listener = l;
    }

    public static interface Listener {
        public void coinAdded(double amount, int newCount);
        public void countUpdated(double amount, int newCount);
    }

    @Override
    public void afterTextChanged(Editable s) {
        this.updateCount();
        if (this.listener != null) {
            this.listener.countUpdated(this.value.getValue(), this.count);
        }
    }

    public void beforeTextChanged(CharSequence s, int start, int before, int count) {}
    public void onTextChanged(CharSequence s, int start, int before, int count) {}
}
