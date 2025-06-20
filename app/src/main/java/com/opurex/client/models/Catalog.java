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
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.SortedMap;
import java.util.TreeMap;

import org.json.JSONException;
import org.json.JSONArray;

/**
 * A catalog with categories and matching products
 */
public class Catalog implements Serializable {

    /**
     * The first level of the category tree
     */
    private List<Category> categories;
    public Map<Category, List<Product>> products;
    private Map<String, Product> database;
    public Map<String, Product> barcodeDb;

    public Catalog() {
        this.categories = new ArrayList<Category>();
        this.products = new HashMap<Category, List<Product>>();
        this.database = new HashMap<String, Product>();
        this.barcodeDb = new TreeMap<String, Product>();
    }

    /**
     * Add a root category and all its subcategories.
     * Warning: subcategories should not be added after, this would cause
     * the catalog to be out of sync.
     */
    public void addRootCategory(Category c) {
        this.categories.add(c);
        if (!this.products.containsKey(c)) {
            this.products.put(c, new ArrayList<Product>());
        }
        this.addSubcategories(c);
    }

    private void addSubcategories(Category c) {
        for (Category sub : c.getSubcategories()) {
            if (!this.products.containsKey(sub)) {
                this.products.put(sub, new ArrayList<Product>());
            }
            this.addSubcategories(sub);
        }
    }

    public void addProduct(Category c, Product p) {
        this.products.get(c).add(p);
        this.database.put(p.getId(), p);
        if (p.getBarcode() != null) {
            this.barcodeDb.put(p.getBarcode(), p);
        }
    }

    /**
     * Add a product not directly accessible from catalog
     */
    public void addProduct(Product p) {
        this.database.put(p.getId(), p);
        if (p.getBarcode() != null) {
            this.barcodeDb.put(p.getBarcode(), p);
        }
    }

    public List<Category> getRootCategories() {
        return this.categories;
    }

    /**
     * Get all categories in the form of a flatten tree.
     */
    public List<Category> getAllCategories() {
        List<Category> allCats = new ArrayList<Category>();
        for (Category c : this.categories) {
            allCats.add(c);
            this.addSubCats(allCats, c);
        }
        return allCats;
    }

    /**
     * Recursive subroutine to flatten category tree.
     */
    private void addSubCats(List<Category> list, Category parent) {
        for (Category sub : parent.getSubcategories()) {
            list.add(sub);
            this.addSubCats(list, sub);
        }
    }

    public Category getCategory(String id) {
        for (Category c : this.categories) {
            if (c.getId().equals(id)) {
                return c;
            }
        }
        return null;
    }

    public List<Product> getProducts(Category c) {
        return this.products.get(c);
    }

    public Product getProduct(String id) {
        return this.database.get(id);
    }

    public Product getProductByBarcode(String barcode) {
        return this.barcodeDb.get(barcode);
    }

    public List<Product> getProductLikeBarcode(String barcode) {
        SortedMap<String, Product> sm = ((TreeMap<String, Product>) this.barcodeDb).subMap(barcode, barcode + Character.MAX_VALUE);
        return new ArrayList<Product>(sm.values());
    }

    public int getProductCount() {
        return this.database.keySet().size();
    }

    public Catalog fromJSON(JSONArray array) throws JSONException {
        return null;
    }
}
