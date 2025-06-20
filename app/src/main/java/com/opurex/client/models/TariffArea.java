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
import java.util.HashMap;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class TariffArea implements Serializable {

    private String id;
    private String label;
    private Map<String, Double> prices;

    public TariffArea(String id, String label) {
        this.id = id;
        this.label = label;
        this.prices = new HashMap<String, Double>();
    }

    public String getId() {
        return this.id;
    }

    public String getLabel() {
        return this.label;
    }

    public boolean hasPrice(String productId) {
        return this.prices.containsKey(productId);
    }
    public Double getPrice(String productId) {
        return this.prices.get(productId);
    }
    public void setPrice(String productId, double price) {
        this.prices.put(productId, price);
    }

    public static TariffArea fromJSON(JSONObject o)
            throws JSONException {
        String id = String.valueOf(o.getInt("id"));
        String label = o.getString("label");
        TariffArea area = new TariffArea(id, label);
        JSONArray prices = o.getJSONArray("prices");
        for (int i = 0; i < prices.length(); i++) {
            JSONObject price = prices.getJSONObject(i);
            String productId = String.valueOf(price.getInt("product"));
            double productPrice = price.getDouble("price");
            area.setPrice(productId, productPrice);
        }
        return area;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TariffArea that = (TariffArea) o;

        if (id != null ? !id.equals(that.id) : that.id != null) return false;
        if (label != null ? !label.equals(that.label) : that.label != null) return false;
        return prices != null ? prices.equals(that.prices) : that.prices == null;

    }
}
