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
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.TextView;

import java.io.IOError;
import java.util.ArrayList;
import java.util.List;

import com.opurex.client.data.Data;
import com.opurex.client.models.PaymentMode;
import com.opurex.client.models.User;
import com.opurex.client.activities.TrackedActivity;
import com.opurex.client.utils.Error;
import com.opurex.client.widgets.NumKeyboard;
import com.opurex.client.widgets.PaymentModeValueBtnItem;
import com.opurex.client.widgets.PaymentModeValuesBtnAdapter;

public class OpenCash extends TrackedActivity
        implements PaymentModeValueBtnItem.Listener, Handler.Callback
{
    private static final String LOG_TAG = "Opurex/Cash";
    public static final int CODE_CASH = 0;
    /** Inner key to store the count of each value. */
    private static final String COUNT_KEY = "count";
    /** Inner key to store the total amount. */
    private static final String AMOUNT_KEY = "amount";

    private double total;
    private EditText totalAmount;
    private PaymentModeValuesBtnAdapter coinButtons;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
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
        User cashier = Data.Session.currentSession(this).getUser();
        if (savedInstanceState != null) {
            this.restoreFromState(savedInstanceState);
        }
        setContentView(R.layout.open_cash);
        this.totalAmount = (EditText) this.findViewById(R.id.open_cash_amount);
        this.totalAmount.setFocusable(false);
        if (!cashier.hasPermission("button.openmoney")
            || Data.Cash.currentCash(this).isClosed()) {
            this.findViewById(R.id.numkeyboard).setVisibility(View.GONE);
            this.findViewById(R.id.open_cash_values).setVisibility(View.GONE);
        }
        if (Data.Cash.currentCash(this).isClosed()) {
            TextView status = (TextView) this.findViewById(R.id.open_cash_status);
            status.setText(R.string.cash_closed);
        }
        NumKeyboard kbd = (NumKeyboard) this.findViewById(R.id.numkeyboard);
        kbd.setValidateLabel(this.getString(R.string.cash_open));
        kbd.setKeyHandler(new Handler(this));
        ((GridView) this.findViewById(R.id.open_cash_values)).setAdapter(this.coinButtons);
        this.coinButtons.setListener(this);
        this.updateAmount();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        List<Integer> counts = this.coinButtons.getCounts();
        for (int i = 0; i < counts.size(); i++) {
            outState.putInt(COUNT_KEY + i, counts.get(i));
        }
        outState.putDouble(AMOUNT_KEY, this.total);
    }

    private void restoreFromState(Bundle state) {
        List<Integer> counts = this.coinButtons.getCounts();
        for (int i = 0; i < counts.size(); i++) {
            counts.set(i, state.getInt(COUNT_KEY + i, 0));
        }
        this.total = state.getDouble(AMOUNT_KEY, 0.0);
    }


    public void open() {
        // Open cash
        Data.Cash.currentCash(this).openNow(this.total);
        Data.Cash.dirty = true;
        try {
            Data.Cash.save();
        } catch (IOError e) {
            Log.e(LOG_TAG, "Unable to save cash", e);
            Error.showError(R.string.err_save_cash, this);
        }
        // Go to ticket screen
        this.setResult(Activity.RESULT_OK);
        // Kill
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
        this.total += amount;
        this.totalAmount.setText(this.getString(R.string.ticket_total, this.total));
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
            this.total += v.getValue() * count;
        }
        this.totalAmount.setText(this.getString(R.string.ticket_total, this.total));
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
                open();
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
