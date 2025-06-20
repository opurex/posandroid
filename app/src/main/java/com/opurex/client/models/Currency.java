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

import java.io.Serializable;

import org.json.JSONException;
import org.json.JSONObject;

public class Currency implements Serializable, JSONable {

    private int id;
    private String reference;
    private String label;
    private String symbol;
    private String decimalSeparator;
    private String thousandsSeparator;
    private String format;
    private double rate;
    private boolean main;
    private boolean visible;

    public Currency(JSONObject o) throws JSONException {
        this.id = o.getInt("id");
        this.reference = o.getString("reference");
        this.label = o.getString("label");
        this.symbol = o.getString("symbol");
        this.decimalSeparator = o.getString("decimalSeparator");
        this.thousandsSeparator = o.getString("thousandsSeparator");
        this.format = o.getString("format");
        this.rate = o.getDouble("rate");
        this.main = o.getBoolean("main");
        this.visible = o.getBoolean("visible");
    }

    public int getId() { return this.id; }
    public boolean isMain() { return this.main; }

    public JSONObject toJSON() throws JSONException {
        JSONObject o = new JSONObject();
        o.put("id", this.id);
        o.put("reference", this.reference);
        o.put("label", this.label);
        o.put("symbol", this.symbol);
        o.put("decimalSeparator", this.decimalSeparator);
        o.put("thousandsSeparator", this.thousandsSeparator);
        o.put("format", this.format);
        o.put("rate", this.rate);
        o.put("main", this.main);
        o.put("visible", this.visible);
        return o;
    }
}
