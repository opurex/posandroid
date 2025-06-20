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
package com.opurex.client.models;

import com.opurex.client.data.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class PaymentMode implements Serializable {

    /** Must be assigned to a customer */
    public static final int CUST_ASSIGNED = 1;
    /** Is debt (includes CUST_ASSIGNED) */
    public static final int CUST_DEBT = 2 + CUST_ASSIGNED;
    public static final int CUST_PREPAID = 4 + CUST_ASSIGNED;

    private int id;
    private String code;
    private String label;
    private String backLabel;
    private int flags;
    private boolean hasImage;
    private List<Value> values;
    private List<Return> rules;
    private boolean active;
    private int dispOrder;

    public PaymentMode(int id, String code, String label, String backLabel,
            int flags, boolean hasImage, List<Return> rules, boolean active,
            int dispOrder) {
        this.id = id;
        this.code = code;
        this.label = label;
        this.backLabel = backLabel;
        this.flags = flags;
        this.hasImage = hasImage;
        this.values = new ArrayList<Value>();
        if (rules == null) {
            this.rules = new ArrayList<Return>();
        } else {
            this.rules = rules;
        }
        this.active = active;
        this.dispOrder = dispOrder;
    }

    public static PaymentMode fromJSON(JSONObject o) throws JSONException {
        int id = o.getInt("id");
        String code = o.getString("reference");
        String label = o.getString("label");
        String backLabel = o.getString("backLabel");
        int flags = o.getInt("type");
        boolean hasImage = o.getBoolean("hasImage");
        boolean active = o.getBoolean("visible");
        int dispOrder = o.getInt("dispOrder");
        List<Value> values = new ArrayList<Value>();
        JSONArray jsValues = o.getJSONArray("values");
        for (int i = 0; i < jsValues.length(); i++) {
            values.add(Value.fromJSON(jsValues.getJSONObject(i)));
        }
        List<Return> rules = new ArrayList<Return>();
        JSONArray jsRules = o.getJSONArray("returns");
        for (int i = 0; i < jsRules.length(); i++) {
            rules.add(Return.fromJSON(jsRules.getJSONObject(i)));
        }
        return new PaymentMode(id, code, label, backLabel, flags, hasImage,
                rules, active, dispOrder);
    }

    public int getId() {
        return this.id;
    }

    public String getLabel() {
        return this.label;
    }

    public String getBackLabel() {
        return this.backLabel;
    }

    public String getCode() {
        return this.code;
    }

    public boolean isActive() {
        return this.active;
    }

    public boolean hasImage() {
        return this.hasImage;
    }

    public boolean isDebt() {
        return (this.flags & CUST_DEBT) == CUST_DEBT;
    }
    public boolean isPrepaid() {
        return (this.flags & CUST_PREPAID) == CUST_PREPAID;
    }
    public boolean isCustAssigned() {
        return (this.flags & CUST_ASSIGNED) == CUST_ASSIGNED;
    }

    public int getDispOrder() {
        return this.dispOrder;
    }

    public List<PaymentMode.Return> getRules() {
        return this.rules;
    }

    public PaymentMode getReturnMode(double exceedentAmount) {
        if (exceedentAmount < 0.005) {
            return null;
        }
        PaymentMode.Return ret = null;
        // Keep the latest mode that applies for the exceedent
        for (PaymentMode.Return r : this.rules) {
            if (r.appliesFor(exceedentAmount)) {
                ret = r;
                break;
            }
        }
        // Return its return mode
        if (ret != null) {
            return ret.getReturnMode();
        } else {
            return null;
        }
    }

    public JSONObject toJSON() throws JSONException {
        JSONObject o = new JSONObject();
        o.put("code", this.code);
        return o;
    }

    @Override
    public boolean equals(Object o) {
        return (o instanceof PaymentMode)
                && ((PaymentMode)o).code.equals(this.code);
    }
    @Override
    public int hashCode() {
        return this.code.hashCode();
    }

    public static class Return implements Serializable {

        private double minVal;
        private Integer returnId;

        public Return(double minVal, Integer returnId) {
            this.minVal = minVal;
            this.returnId = returnId;
        }

        public static Return fromJSON(JSONObject o) throws JSONException {
            double minVal = o.getDouble("minAmount");
            Integer returnId = o.getInt("returnMode");
            return new Return(minVal, returnId);
        }

        public double getMinVal() {
            return this.minVal;
        }

        public boolean hasReturnMode() {
            return this.returnId != null;
        }

        public PaymentMode getReturnMode() {
            if (this.returnId == null) {
                return null;
            }
            return Data.PaymentMode.get(this.returnId);
        }

        /** Check if the rule applies for a given exceedent */
        public boolean appliesFor(double exceedent) {
            return exceedent - this.minVal > -0.005;
        }
    }

    public static class Value implements Serializable
    {
        private double value;
        private boolean hasImage;

        public Value(double value, boolean hasImage) {
            this.value = value;
            this.hasImage = hasImage;
        }

        public static Value fromJSON(JSONObject o) throws JSONException {
            double value = o.getDouble("value");
            boolean hasImage = o.getBoolean("hasImage");
            return new Value(value, hasImage);
        }
        public double getValue() {
            return this.value;
        }

        public boolean hasImage() {
            return this.hasImage;
        }
    }
}
