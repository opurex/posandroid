package com.opurex.client.models;

import java.io.Serializable;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by nsvir on 27/08/15.
 * n.svirchevsky@gmail.com
 */
public class Tax implements Serializable {

    private int id;
    private String label;
    private double rate;

    public Tax(int id, String label, double rate) {
        this.id = id;
        this.label = label;
        this.rate = rate;
    }

    public Tax(JSONObject o) throws JSONException {
        this.id = o.getInt("id");
        this.label = o.getString("label");
        this.rate = o.getDouble("rate");
    }

    public int getId() { return this.id; }
    public String getLabel() { return this.label; }
    public double getRate() { return this.rate; }

    public String getPercent() { return (rate * 100) + ""; }

}
