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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.json.JSONObject;
import org.json.JSONException;
import org.json.JSONArray;

public class Inventory implements Serializable {

    public static final int STOCK_AVAILABLE = 1;
    public static final int STOCK_LOST = 2;
    public static final int STOCK_DEFECT = 3;

    private List<InventoryItem> items;
    private List<InventoryItem> lost;
    private List<InventoryItem> defect;
    private String locationId;
    private long date;

    /** Create an empty inventory at current date. */
    public Inventory(String locationId) {
        this.locationId = locationId;
        this.date = System.currentTimeMillis() / 1000;
        this.items = new ArrayList<InventoryItem>();
        this.lost = new ArrayList<InventoryItem>();
        this.defect = new ArrayList<InventoryItem>();
    }

    public Inventory(String locationId, long date) {
        this.locationId = locationId;
        this.date = date;
        this.items = new ArrayList<InventoryItem>();
        this.lost = new ArrayList<InventoryItem>();
        this.defect = new ArrayList<InventoryItem>();
    }

    public String getLocationId() {
        return this.locationId;
    }
    public long getDate() {
        return this.date;
    }
    /** Set date to current date. */
    public void setNow() {
        this.date = System.currentTimeMillis() / 1000;
    }

    public void addProduct(String productId, int stockType) {
        this.addProduct(productId, 1.0, stockType);
    }
    public void addProduct(Product p, int stockType) {
        this.addProduct(p.getId(), 1.0, stockType);
    }
    public void addProduct(InventoryItem item, int stockType) {
        this.addProduct(item.getProductId(), 1.0, stockType);
    }

    private List<InventoryItem> getRef(int stockType) {
        switch (stockType) {
        case STOCK_LOST:
            return this.lost;
        case STOCK_DEFECT:
            return this.defect;
        case STOCK_AVAILABLE:
        default:
            return this.items;
        }
    }

    public void addProduct(String productId, double quantity, int stockType) {
        List<InventoryItem> ref = this.getRef(stockType);
        for (InventoryItem i : ref) {
            if (i.getProductId().equals(productId)) {
                i.add(quantity);
                return;
            }
        }
        ref.add(new InventoryItem(productId, quantity));
    }
    public void addProduct(Product p, double quantity, int stockType) {
        this.addProduct(p.getId(), quantity, stockType);
    }
    public void addProduct(InventoryItem item, double quantity, int stockType) {
        this.addProduct(item.getProductId(), quantity, stockType);
    }

    public void setQuantity(String productId, double quantity, int stockType) {
        List<InventoryItem> ref = this.getRef(stockType);
        for (InventoryItem i : ref) {
            if (i.getProductId().equals(productId)) {
                i.set(quantity);
                return;
            }
        }
        ref.add(new InventoryItem(productId, quantity));
    }
    public void setQuantity(Product p, double quantity, int stockType) {
        this.setQuantity(p.getId(), quantity, stockType);
    }
    public void setQuantity(InventoryItem i, double quantity, int stockType) {
        this.setQuantity(i.getProductId(), quantity, stockType);
    }

    public void remove(InventoryItem item, int stockType) {
        this.getRef(stockType).remove(item);
    }

    public InventoryItem getItemAt(int position, int stockType) {
        return this.getRef(stockType).get(position);
    }
    public int size(int stockType) {
        return this.getRef(stockType).size();
    }

    public JSONObject toJSON() throws JSONException {
        JSONObject o = new JSONObject();
        o.put("id", null);
        o.put("date", this.date);
        o.put("locationId", this.locationId);
        Set<String> allIds = new HashSet<String>();
        Map<String, InventoryItem> itemMap = new HashMap<String, InventoryItem>();
        Map<String, InventoryItem> lostMap = new HashMap<String, InventoryItem>();
        Map<String, InventoryItem> defMap = new HashMap<String, InventoryItem>();
        for (InventoryItem i : this.items) {
            itemMap.put(i.getProductId(), i);
            allIds.add(i.getProductId());
        }
        for (InventoryItem i : this.lost) {
            lostMap.put(i.getProductId(), i);
            allIds.add(i.getProductId());
        }
        for (InventoryItem i : this.defect) {
            defMap.put(i.getProductId(), i);
            allIds.add(i.getProductId());
        }
        JSONArray jsItems = new JSONArray();
        for (String prdId : allIds) {
            JSONObject jsItem = new JSONObject();
            jsItem.put("productId", prdId);
            jsItem.put("attrSetInstId", JSONObject.NULL);
            if (itemMap.containsKey(prdId)) {
                jsItem.put("qty", itemMap.get(prdId).getQuantity());
            } else {
                jsItem.put("qty", 0.0);
            }
            if (lostMap.containsKey(prdId)) {
                jsItem.put("lostQty", lostMap.get(prdId).getQuantity());
            } else {
                jsItem.put("lostQty", 0.0);
            }
            if (defMap.containsKey(prdId)) {
                jsItem.put("defectQty", defMap.get(prdId).getQuantity());
            } else {
                jsItem.put("defectQty", 0.0);
            }
            jsItem.put("missingQty", JSONObject.NULL);
            jsItem.put("unitValue", JSONObject.NULL);
            jsItems.put(jsItem);
        }
        o.put("items", jsItems);
        return o;
    }

    public class InventoryItem implements Serializable {

        private String productId;
        private double quantity;

        public InventoryItem(String productId, double qty) {
            this.productId = productId;
            this.quantity = qty;
        }

        public String getProductId() {
            return this.productId;
        }
        public double getQuantity() {
            return this.quantity;
        }
        public void add(double qty) {
            this.quantity += qty;
        }
        public void set(double qty) {
            this.quantity = qty;
        }

        @Override
        public boolean equals(Object o) {
            return ((o instanceof InventoryItem)
                    && ((InventoryItem) o).productId.equals(this.productId));
        }
        @Override
        public int hashCode() {
            return this.productId.hashCode();
        }
    }

}