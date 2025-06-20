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
package com.opurex.client;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.EditText;
import android.widget.GridView;

import java.util.ArrayList;
import java.util.List;

import com.opurex.client.models.PaymentMode;
import com.opurex.client.activities.TrackedActivity;
import com.opurex.client.utils.CalculPrice;
import com.opurex.client.widgets.NumKeyboard;
import com.opurex.client.widgets.PaymentModeValueBtnItem;
import com.opurex.client.widgets.PaymentModeValuesBtnAdapter;

/** Activity to be called for result to count cash on close.
 * The amount is sent back to the caller. */
public class CloseCashCount
extends TrackedActivity
implements PaymentModeValueBtnItem.Listener, Handler.Callback
{
    private static final String LOG_TAG = "Opurex/CloseCashCount";
    public static final int CODE_CASH = 0;
    /** The key to hold the amount, passed as result. */
    public static final String AMOUNT_KEY = "amount";
    /** The key for the initial Intent to give the expected amount. */
    public static final String EXPECTED_AMOUNT_KEY = "expectedAmount";
    /** Inner key to store the count of each value. */
    private static final String COUNT_KEY = "count";

    private double total;
    private double expected;
    private EditText totalAmount;
    private EditText expectedAmount;
    private PaymentModeValuesBtnAdapter coinButtons;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.expected = -1.0;
        List<PaymentMode.Value> values = new ArrayList<PaymentMode.Value>();
        values.add(new PaymentMode.Value(50.0, true));
        values.add(new PaymentMode.Value(20.0, true));
        values.add(new PaymentMode.Value(10.0, true));
        values.add(new PaymentMode.Value(5.0, true));
        values.add(new PaymentMode.Value(2.0, true));
        values.add(new PaymentMode.Value(1.0, true));
        values.add(new PaymentMode.Value(0.5, true));
        values.add(new PaymentMode.Value(0.2, true));
        values.add(new PaymentMode.Value(0.1, true));
        values.add(new PaymentMode.Value(0.05, true));
        values.add(new PaymentMode.Value(0.02, true));
        values.add(new PaymentMode.Value(0.01, true));
        List<Integer> counts = new ArrayList<Integer>();
        for (PaymentMode.Value v : values) {
            counts.add(new Integer(0));
        }
        this.coinButtons = new PaymentModeValuesBtnAdapter(values, counts);
        if (savedInstanceState != null) {
            this.restoreFromState(savedInstanceState);
        } else {
            Intent i = this.getIntent();
            Bundle b = i.getExtras();
            if (b != null) {
                this.expected = b.getDouble(EXPECTED_AMOUNT_KEY, -1.0);
            }
        }
        setContentView(R.layout.close_cash_count);
        this.totalAmount = (EditText) this.findViewById(R.id.close_cash_amount);
        this.totalAmount.setFocusable(false);
        this.expectedAmount = (EditText) this.findViewById(R.id.expected_cash_amount);
        this.expectedAmount.setFocusable(false);
        if (this.expected != -1.0) {
            this.expectedAmount.setText(this.getString(R.string.ticket_total, this.expected));
        }
        NumKeyboard kbd = (NumKeyboard) this.findViewById(R.id.numkeyboard);
        kbd.setValidateLabel(this.getString(R.string.close_cash));
        kbd.setKeyHandler(new Handler(this));
        ((GridView) this.findViewById(R.id.close_cash_values)).setAdapter(this.coinButtons);
        this.coinButtons.setListener(this);
        this.updateMatchingCount();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putDouble(EXPECTED_AMOUNT_KEY, this.expected);
        List<Integer> counts = this.coinButtons.getCounts();
        for (int i = 0; i < counts.size(); i++) {
            outState.putInt(COUNT_KEY + i, counts.get(i));
        }
        outState.putDouble(AMOUNT_KEY, this.total);
    }

    private void restoreFromState(Bundle state) {
        this.expected = state.getDouble(EXPECTED_AMOUNT_KEY, -1.0);
        List<Integer> counts = this.coinButtons.getCounts();
        for (int i = 0; i < counts.size(); i++) {
            counts.set(i, state.getInt(COUNT_KEY + i, 0));
        }
        this.total = state.getDouble(AMOUNT_KEY, 0.0);
    }

    public void close() {
        // Send back the amount to the caller
        Intent i = new Intent();
        i.putExtra(AMOUNT_KEY, this.total);
        this.setResult(Activity.RESULT_OK, i);
        this.finish();
    }

    private void resetCashCount() {
        this.total = 0.0;
        for (int i = 0; i < this.coinButtons.getCount(); i++) {
            PaymentModeValueBtnItem btn = (PaymentModeValueBtnItem) this.coinButtons.getItem(i);
            btn.setCount(0);
        }
    }

    /** From CoinCount.Listener */
    public void coinAdded(double amount, int newCount) {
        this.total = CalculPrice.add(this.total, amount);
        this.totalAmount.setText(this.getString(R.string.ticket_total, this.total));
        this.updateMatchingCount();
    }
    /** From CoinCount.Listener */
    public void countUpdated(double amount, int newCount) {
        this.updateAmount();
    }

    public void updateAmount() {
        if (this.totalAmount == null) {
            return;
        }
        this.total = 0.0;
        for (int i = 0; i < this.coinButtons.getCount(); i++) {
            PaymentMode.Value v = (PaymentMode.Value) this.coinButtons.getItem(i);
            int count = this.coinButtons.getCount(i);
            this.total = CalculPrice.add(this.total, v.getValue() * count);
        }
        this.totalAmount.setText(this.getString(R.string.ticket_total, this.total));
        this.updateMatchingCount();
    }

    /** Check if total and expected amount are equal
     * and update UI accordingly
     */
    public void updateMatchingCount() {
        if (this.total != this.expected) {
            this.totalAmount.setTextColor(Color.RED);
        } else {
            this.totalAmount.setTextColor(this.getResources().getColor(R.color.content1_txt));
        }
    }

    @Override
        public boolean handleMessage(Message msg) {
            View view = this.getWindow().getCurrentFocus();
            EditText focused = null;
            if (view instanceof EditText) {
                focused = (EditText) view;
            }
            switch (msg.what) {
                case NumKeyboard.KEY_ENTER:
                    close();
                    break;
                case NumKeyboard.KEY_0:
                    if (focused != null) {
                        if (!focused.getText().toString().startsWith("0")) {
                            focused.setText(focused.getText().toString() + "0");
                        }
                    }
                    break;
                case NumKeyboard.KEY_1:
                    if (focused != null) {
                        focused.setText(focused.getText().toString() + "1");
                    }
                    break;
                case NumKeyboard.KEY_2:
                    if (focused != null) {
                        focused.setText(focused.getText().toString() + "2");
                    }
                    break;
                case NumKeyboard.KEY_3:
                    if (focused != null) {
                        focused.setText(focused.getText().toString() + "3");
                    }
                    break;
                case NumKeyboard.KEY_4:
                    if (focused != null) {
                        focused.setText(focused.getText().toString() + "4");
                    }
                    break;
                case NumKeyboard.KEY_5:
                    if (focused != null) {
                        focused.setText(focused.getText().toString() + "5");
                    }
                    break;
                case NumKeyboard.KEY_6:
                    if (focused != null) {
                        focused.setText(focused.getText().toString() + "6");
                    }
                    break;
                case NumKeyboard.KEY_7:
                    if (focused != null) {
                        focused.setText(focused.getText().toString() + "7");
                    }
                    break;
                case NumKeyboard.KEY_8:
                    if (focused != null) {
                        focused.setText(focused.getText().toString() + "8");
                    }
                    break;
                case NumKeyboard.KEY_9:
                    if (focused != null) {
                        focused.setText(focused.getText().toString() + "9");
                    }
                    break;
                case NumKeyboard.KEY_00:
                    if (focused != null) {
                        if (!focused.getText().toString().startsWith("0")) {
                            focused.setText(focused.getText().toString() + "00");
                        }
                    }
                    break;
                case NumKeyboard.KEY_ERASE:
                    if (focused != null) {
                        focused.setText("");
                    }
                    break;
                default:
                    break;
            }
            if (focused != null && focused.getText().toString().length() > 1
                    && focused.getText().toString().startsWith("0")) {
                focused.setText(focused.getText().toString().substring(1));
            }
            return true;
        }
}
