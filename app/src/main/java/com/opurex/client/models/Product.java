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
import java.util.List;

import android.content.Context;
import android.graphics.Bitmap;
import com.opurex.client.data.Data;
import com.opurex.client.data.ImagesData;
import com.opurex.client.models.interfaces.Item;
import com.opurex.client.utils.CalculPrice;
import org.json.JSONException;
import org.json.JSONObject;

/** Product from server or manual input. */
public class Product implements Serializable, Item {

    /** Database Id of the product. */
    protected String id;
    /** Can be null because empty string is not a valid reference. */
    protected String reference;
    protected String label;
    protected double price;
    protected double taxedPrice;
    protected int taxId;
    protected double taxRate;
    protected boolean scaled;
    /** Never null, uses empty string instead. */
    protected String barcode = "";
    protected boolean hasImage;
    protected double discountRate;
    protected boolean discountRateEnabled;
    protected boolean isPrepaid;
    /** Can be null in case of a product created locally (manual input). */
    protected Integer categoryId;
    protected int dispOrder;

    /** Empty constructor used to set values one by one. */
    protected Product() {
        this.barcode = "";
    }

    public Product(String id, String label, String barcode, double price,
            double taxedPrice,
            int taxId, boolean scaled, boolean hasImage,
            double discountRate, boolean discountRateEnabled,
            boolean isPrepaid) {
        this.id = id;
        this.label = label;
        this.barcode = barcode;
        this.price = price;
        this.taxedPrice = taxedPrice;
        this.taxId = taxId;
        this.scaled = scaled;
        this.hasImage = hasImage;
        this.discountRate = discountRate;
        this.discountRateEnabled = discountRateEnabled;
        this.isPrepaid = isPrepaid;
    }

    /** Create a product from manual input. */
    public Product(String label, double price, double taxedPrice, int taxId) {
        // Default values for local product (manual input).
        // This is mostly useless but here only for comprehension.
        this.id = null; this.reference = null; this.barcode = "";
        this.scaled = false;
        this.hasImage = false;
        this.discountRate = 0.0; this.discountRateEnabled = false;
        this.isPrepaid = false;
        this.categoryId = null;
        // Set actual values.
        this.label = label;
        this.price = price;
        this.taxedPrice = taxedPrice;
        this.taxId = taxId;
    }

    public Product(JSONObject o) throws JSONException {
        this.id = String.valueOf(o.getInt("id"));
        this.reference = o.getString("reference");
        this.barcode = o.getString("barcode");
        this.label = o.getString("label");
        this.scaled = o.getBoolean("scaled");
        this.price = o.getDouble("priceSell");
        //this.taxValue = o.getDouble("taxValue"); // Not used
        this.taxedPrice = o.getDouble("taxedPrice");
        this.taxId = o.getInt("tax");
        this.categoryId = o.getInt("category");
        this.discountRateEnabled = o.getBoolean("discountEnabled");
        this.discountRate = o.getDouble("discountRate");
        this.isPrepaid = o.getBoolean("prepay");
        this.dispOrder = o.getInt("dispOrder");
        this.hasImage = o.getBoolean("hasImage");
    }

    /** Get the Id of the product. If it is a local product, it has no Id.
     * @return Database Id, null for a local product. */
    public String getId() { return this.id; }
    /** Get reference of the product. It is null for local products. */
    public String getReference() { return this.reference; }
    public String getLabel() { return this.label; }
    public Integer getCategoryId() { return this.categoryId; }
    public int getDispOrder() { return this.dispOrder; }
    public String getBarcode() { return this.barcode; }

    @Override
    public Type getType() {
        return Type.Product;
    }

    private double getPrice() {
        return CalculPrice.round(this.price);
    }



    private double _getGenericPrice(double price, double discount, int binaryMask) {
        return CalculPrice.getGenericPrice(price, discount, this.getTaxRate(), binaryMask);
    }

    double getGenericPrice(TariffArea area, int binaryMask) {
        return _getGenericPrice(getPrice(area), this.discountRate, binaryMask);
    }

    double getGenericPrice(TariffArea area, double discount, int binaryMask) {
        return _getGenericPrice(getPrice(area),
                CalculPrice.mergeDiscount(this.discountRate, discount), binaryMask);
    }

    public double getPrice(TariffArea area) {
        if (area != null && area.hasPrice(this.id)) {
            return area.getPrice(this.id).doubleValue();
        }
        return this.price;
    }

    public int getTaxId() {
        return this.taxId;
    }

    /** Get tax rate from Tax in cache. */
    public double getTaxRate() {
        List<Tax> taxes = Data.Tax.getTaxes();
        for (Tax t : taxes) {
            if (t.getId() == this.taxId) {
                return t.getRate();
            }
        }
        // Error
        return 0.0;
    }

    public double getDiscountRate() {
        return this.discountRate;
    }

    public boolean isScaled() {
        return this.scaled;
    }

    /** Check if this product should fill the customer's prepaid account. */
    public boolean isPrepaid() {
        return this.isPrepaid;
    }

    public boolean hasImage() {
        return this.hasImage;
    }

    @Override
    public Bitmap getImage(Context ctx) {
        return ImagesData.getProductImage(this.getId());
    }

    public boolean isDiscountRateEnabled() {
        return this.discountRateEnabled;
    }

    public JSONObject toJSON(TariffArea area) throws JSONException {
        JSONObject o = new JSONObject();
        o.put("id", this.id);
        o.put("label", this.label);
        if (this.reference != null) {
            o.put("reference", this.reference);
        } else {
            o.put("reference", JSONObject.NULL);
        }
        if (this.categoryId != null) {
            o.put("category", this.categoryId);
        } else {
            o.put("category", JSONObject.NULL);
        }
        o.put("dispOrder", this.dispOrder);
        if (area != null && area.hasPrice(this.id)) {
            o.put("price", area.getPrice(this.id));
        } else {
            o.put("price", this.price);
        }
        o.put("tax", this.taxId);
        o.put("taxRate", this.getTaxRate());
        o.put("scaled", this.scaled);
        o.put("barcode", this.barcode);
        o.put("discountRate", this.discountRate);
        o.put("discountEnabled", this.discountRateEnabled);
        o.put("prepay", this.isPrepaid);
        return o;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Product)) {
            return false;
        }
        if (((Product) o).id != null) {
            return ((Product) o).id.equals(this.id);
        } else {
            if (this.id != null) {
                return false;
            }
            Product p = (Product) o;
            return p.price == this.price;
        }
    }

    @Override
    public String toString() {
        return this.label + " (" + this.id + ")";
    }

}
