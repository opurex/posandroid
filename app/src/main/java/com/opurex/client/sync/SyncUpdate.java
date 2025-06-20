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
package com.opurex.client.sync;

import android.content.Context;
import android.os.Handler;
import android.util.Log;

import java.io.IOError;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.io.IOException;

import com.opurex.client.data.Data;
import com.opurex.client.models.*;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.opurex.client.Configure;
import com.opurex.client.data.ImagesData;

import java.text.ParseException;

/**
 * Some request need an order. Parsers call the next request We think that this
 * implementation can be improved..
 *
 * Here is the request life cycle Version
 * '-> CashRegister
 * '-> TAX -> CATEGORY -> PRODUCT -> COMPOSITION
 * '-> ROLE -> USER
 * '-> CUSTOMER
 * '-> CASH
 * '-> TARIFF
 * '-> PAYMENTMODE
 * '-> RESSOURCES
 * '-> PLACES
 * '-> LOCATION -> STOCK
 * '-> DISCOUNT
 *
 * @author nsvir
 */
public class SyncUpdate {

    private static final String LOG_TAG = "Opurex/SyncUpdate";

    // Note: SyncUpdate uses positive values, SyncSend negative ones
    public static final int SYNC_DONE = 1;
    public static final int CONNECTION_FAILED = 2;
    public static final int CATALOG_SYNC_DONE = 3;
    public static final int USERS_SYNC_DONE = 4;
    public static final int CATEGORIES_SYNC_DONE = 5;
    public static final int CASH_SYNC_DONE = 6;
    public static final int PLACES_SYNC_DONE = 7;
    public static final int SYNC_ERROR = 8;
    public static final int CUSTOMERS_SYNC_DONE = 9;
    public static final int COMPOSITIONS_SYNC_DONE = 11;
    public static final int TARIFF_AREAS_SYNC_DONE = 12;
    public static final int CATEGORIES_SYNC_ERROR = 14;
    public static final int CATALOG_SYNC_ERROR = 15;
    public static final int USERS_SYNC_ERROR = 16;
    public static final int CUSTOMERS_SYNC_ERROR = 17;
    public static final int CASH_SYNC_ERROR = 18;
    public static final int PLACES_SYNC_ERROR = 19;
    public static final int COMPOSITIONS_SYNC_ERROR = 20;
    public static final int TARIFF_AREA_SYNC_ERROR = 21;
    public static final int INCOMPATIBLE_VERSION = 22;
    public static final int ROLES_SYNC_DONE = 23;
    public static final int ROLES_SYNC_ERROR = 24;
    public static final int TAXES_SYNC_DONE = 25;
    public static final int TAXES_SYNC_ERROR = 26;
    public static final int VERSION_DONE = 29;
    public static final int PLACES_SKIPPED = 31;
    public static final int CASHREG_SYNC_DONE = 32;
    public static final int CASHREG_SYNC_ERROR = 33;
    public static final int CASHREG_SYNC_NOTFOUND = 34;
    public static final int RESOURCE_SYNC_DONE = 35;
    public static final int RESOURCE_SYNC_ERROR = 36;
    public static final int PAYMENTMODE_SYNC_DONE = 37;
    public static final int PAYMENTMODE_SYNC_ERROR = 38;
    public static final int DISCOUNT_SYNC_DONE = 39;
    public static final int DISCOUNT_SYNC_ERROR = 40;
    public static final int SYNC_ERROR_NOT_LOGGED = 41;
    public static final int PRODUCTS_SYNC_DONE = 42;
    public static final int PRODUCTS_SYNC_ERROR = 43;
    public static final int CURRENCIES_SYNC_DONE = 44;
    public static final int CURRENCIES_SYNC_ERROR = 45;

    private static final String[] resToLoad = new String[]{"MobilePrinter.Header", "MobilePrinter.Footer", "MobilePrinter.Logo"};

    private Context ctx;
    private Handler listener;
    private boolean versionDone;
    private boolean discountDone;
    private boolean cashRegDone;
    private boolean taxesDone;
    private boolean categoriesDone;
    private boolean productsDone;
    private boolean rolesDone;
    private boolean usersDone;
    private boolean customersDone;
    private boolean cashDone;
    private boolean placesDone;
    private boolean compositionsDone;
    private boolean tariffAreasDone;
    private boolean paymentModesDone;
    private boolean currenciesDone;
    private int resLoaded;
    private boolean isANewUserAccount;
    public static final int STEPS = 16;
    /**
     * Stop parallel messages in case of error
     */
    private boolean stop;

    /**
     * The catalog to build with multiple syncs
     */
    private Catalog catalog;
    /**
     * Categories by id for quick products assignment
     */
    private Map<String, Category> categories;
    /**
     * Permissions by role id
     */
    private Map<String, String> permissions;
    /**
     * Tax rates by tax id.
     */
    private Map<Integer, Tax> taxes;
    /**
     * Tax ids by tax cat id
     */
    private int cashRegId;

    public SyncUpdate(Context ctx, Handler listener) {
        this.listener = listener;
        this.ctx = ctx;
        this.isANewUserAccount = !Data.Login.equalsConfiguredAccount(this.ctx);
        this.catalog = new Catalog();
        this.categories = new HashMap<String, Category>();
        this.permissions = new HashMap<String, String>();
        this.taxes = new HashMap<Integer, Tax>();
    }

    /**
     * Launch synchronization
     */
    public void startSyncUpdate() {
        synchronize();
    }

    public void synchronize() {
        new Thread() {
            public void run() {
                SyncUpdate thiss = SyncUpdate.this;
                ServerLoader loader = new ServerLoader(thiss.ctx);
                ServerLoader.Response resp = null;
                try {
                    resp = loader.read("api/version");
                } catch (Exception e) {
                    Log.e(LOG_TAG, "Connection error", e);
                    SyncUtils.notifyListener(thiss.listener, SYNC_ERROR, e);
                    return;
                }
                // Parse version and check for compatibility
                try {
                    JSONObject o = resp.getObjContent();
                    String version = o.getString("version");
                    String level = o.getString("level");
                    Version.setVersion(version, level);
                    if (!Version.isValid(thiss.ctx)) {
                        SyncUtils.notifyListener(thiss.listener, INCOMPATIBLE_VERSION);
                        return;
                    } else {
                        SyncUtils.notifyListener(thiss.listener, VERSION_DONE);
                    }
                } catch (JSONException e) {
                    Log.e(LOG_TAG, "Unable to parse version response: "
                            + resp.toString(), e);
                    SyncUtils.notifyListener(thiss.listener, SYNC_ERROR,
                            "Unable to parse version response: "
                            + resp.toString());
                    return;
                } catch (Exception e) {
                    Log.e(LOG_TAG, "Unable to get version from server", e);
                    SyncUtils.notifyListener(thiss.listener, SYNC_ERROR,
                            "Unable to get version from server "
                            + e.toString());
                    return;
                }
                // Version OK, synchronize
                try {
                    resp = loader.read("api/sync/"
                            + URLEncoder.encode(Configure.getMachineName(thiss.ctx)));
                } catch (Exception e) {
                    Log.e(LOG_TAG, "Connection error", e);
                    SyncUtils.notifyListener(thiss.listener, SYNC_ERROR,
                            "Connection error " + e.toString());
                    return;
                }
                if (ServerLoader.Response.STATUS_REJECTED.equals(resp.getStatus())) {
                    Log.e(LOG_TAG, "Server rejected sync call " + resp.getResponse().toString());
                    SyncUtils.notifyListener(thiss.listener, SYNC_ERROR,
                            "Unable to connect to server " + resp.getResponse().toString());
                    return;
                }
                if (ServerLoader.Response.STATUS_ERROR.equals(resp.getStatus())) {
                    Log.e(LOG_TAG, "Server error on sync " + resp.getResponse().toString());
                    SyncUtils.notifyListener(thiss.listener, SYNC_ERROR,
                            "Unable to connect to server " + resp.getResponse().toString());
                    return;
                }
                JSONObject jsContent = resp.getObjContent();
                if (jsContent == null) {
                    Log.e(LOG_TAG, "Response is null");
                    SyncUtils.notifyListener(thiss.listener, SYNC_ERROR,
                            "Response is null");
                    return;
                }
                try {
                    if (!thiss.parseCashRegister(jsContent.getJSONObject("cashRegister"))) {
                        Log.e(LOG_TAG, "Response is null");
                        SyncUtils.notifyListener(thiss.listener, SYNC_ERROR,
                                "Caisse non reconnue, vérifiez le nom de la machine.");
                        return;
                    }
                    thiss.parseCash(jsContent.getJSONObject("cash"));
                    thiss.parseRoles(jsContent.getJSONArray("roles"));
                    thiss.parseUsers(jsContent.getJSONArray("users"));
                    thiss.parseCurrencies(jsContent.getJSONArray("currencies"));
                    thiss.parseTariffAreas(jsContent.getJSONArray("tariffareas"));
                    thiss.parseTaxes(jsContent.getJSONArray("taxes"));
                    thiss.parseCategories(jsContent.getJSONArray("categories"));
                    thiss.parseProducts(jsContent.getJSONArray("products"));
                    thiss.parsePlaces(jsContent.getJSONArray("floors"));
                    thiss.parseCustomers(jsContent.getJSONArray("customers"));
                    thiss.parsePaymentModes(jsContent.getJSONArray("paymentmodes"));
                    //thiss.parseDiscountProfiles(jsContent.getJSONArray("discountProfiles"));
                    thiss.parseResources(jsContent.getJSONArray("resources"));
                    thiss.parseDiscounts(jsContent.getJSONArray("discounts"));
                    thiss.finish();
                } catch (JSONException e) {
                    Log.e(LOG_TAG, "Error while parsing server response: "
                            + jsContent.toString(), e);
                    SyncUtils.notifyListener(thiss.listener, SYNC_ERROR,
                            "Error while parsing server response " + e.toString());
                }
            }
        }.start();
    }

    private boolean parseCashRegister(JSONObject o) {
        CashRegister cashReg = null;
        try {
            cashReg = CashRegister.fromJSON(o);
            this.cashRegId = cashReg.getId();
            Data.TicketId.updateTicketId(cashReg, this.isANewUserAccount);
            Data.TicketId.save();
        } catch (JSONException e) {
            Log.e(LOG_TAG, "Unable to parse cash register response: " + o.toString(),
                    e);
            SyncUtils.notifyListener(this.listener, CASHREG_SYNC_ERROR,
                    "Unable to parse cash register response " + e.toString());
            return false;
        } catch (IOError e) {
            Log.d(LOG_TAG, "Could not save ticketId");
        }
        if (cashReg != null) {
            SyncUtils.notifyListener(this.listener, CASHREG_SYNC_DONE, cashReg);
            return true;
        }
        return false;
    }

    private void parseCurrencies(JSONArray array) {
        List<Currency> currencies = new ArrayList<Currency>();
        try {
            for (int i = 0; i < array.length(); i++) {
                JSONObject o = array.getJSONObject(i);
                Currency curr = new Currency(o);
                currencies.add(curr);
            }
        } catch (JSONException e) {
            Log.e(LOG_TAG, "Unable to parse currencies response: " + array.toString(), e);
            SyncUtils.notifyListener(this.listener, CURRENCIES_SYNC_ERROR,
                    "Unable to parse currencies response " + e.toString());
            return;
        }
        SyncUtils.notifyListener(this.listener, CURRENCIES_SYNC_DONE, currencies);
    }

    private void parseTaxes(JSONArray array) {
        try {
            for (int i = 0; i < array.length(); i++) {
                JSONObject o = array.getJSONObject(i);
                Tax tax = new Tax(o);
                this.taxes.put(tax.getId(), tax);
            }
        } catch (JSONException e) {
            Log.e(LOG_TAG, "Unable to parse taxes response: " + array.toString(), e);
            SyncUtils.notifyListener(this.listener, TAXES_SYNC_ERROR,
                    "Unable to parse taxes response " + e.toString());
            this.taxesDone = true;
            this.productsDone = true;
            this.compositionsDone = true;
            return;
        }
        List<Tax> taxList = new ArrayList<Tax>();
        for (Tax t : this.taxes.values()) {
            taxList.add(t);
        }
        SyncUtils.notifyListener(this.listener, TAXES_SYNC_DONE, taxList);
    }

    /**
     * Parse categories and start products sync to create catalog
     */
    private void parseCategories(JSONArray array) {
        Map<String, List<Category>> children = new HashMap<String, List<Category>>();
        try {
            ImagesData.clearCategories(this.ctx);
        } catch (IOException e) {
            Log.w(LOG_TAG, "Unable to clear categories images", e);
        }
        try {
            // First pass: read all and register parents
            for (int i = 0; i < array.length(); i++) {
                JSONObject o = array.getJSONObject(i);
                Category c = Category.fromJSON(o);
                if (o.getBoolean("hasImage")) {
                    // TODO: restore image calls
                }
                String parent = null;
                if (!o.isNull("parent")) {
                    parent = String.valueOf(o.getInt("parent"));
                }
                if (!children.containsKey(parent)) {
                    children.put(parent, new ArrayList<Category>());
                }
                children.get(parent).add(c);
                this.categories.put(c.getId(), c);
            }
            // Second pass: build subcategories
            for (Category root : children.get(null)) {
                // Build subcategories
                this.parseSubcats(root, children);
                // This branch is ready, add to catalog
                this.catalog.addRootCategory(root);
            }
        } catch (JSONException e) {
            Log.e(LOG_TAG, "Unable to parse categories response: " + array.toString(), e);
            SyncUtils.notifyListener(this.listener, CATEGORIES_SYNC_ERROR,
                    "Unable to parse categories response " + e.toString());
            this.productsDone = true;
            this.compositionsDone = true;
            return;
        }
        SyncUtils.notifyListener(this.listener, CATEGORIES_SYNC_DONE,
                children.get(null));
    }

    // recursive subroutine of parseCategories
    private void parseSubcats(Category c,
            Map<String, List<Category>> children) {
        if (children.containsKey(c.getId())) {
            for (Category sub : children.get(c.getId())) {
                c.addSubcategory(sub);
                this.parseSubcats(sub, children);
            }
        }
    }

    private void parseProducts(JSONArray array) {
        Map<String, Composition> compos = new HashMap<String, Composition>();
        try {
            try {
                ImagesData.clearProducts(this.ctx);
            } catch (IOException e) {
                Log.w(LOG_TAG, "Unable to clear product images", e);
            }
            for (int i = 0; i < array.length(); i++) {
                JSONObject o = array.getJSONObject(i);
                Product p = new Product(o);
                if (o.getBoolean("hasImage")) {
                    // TODO: restore image calls
                }
                if (o.getBoolean("composition")) {
                    Composition c = Composition.fromJSON(o);
                    compos.put(c.getProductId(), c);
                }
                // Find its category and add it
                if (o.getBoolean("visible")) {
                    String catId = String.valueOf(o.getInt("category"));
                    for (Category c : this.catalog.getAllCategories()) {
                        if (c.getId().equals(catId)) {
                            this.catalog.addProduct(c, p);
                            break;
                        }
                    }
                } else {
                    this.catalog.addProduct(p);
                }
            }
        } catch (JSONException e) {
            Log.e(LOG_TAG, "Unable to parse products response: " + array.toString(), e);
            SyncUtils.notifyListener(this.listener, PRODUCTS_SYNC_ERROR,
                    "Unable to parse products response " + e.toString());
            this.compositionsDone = true;
            return;
        }
        SyncUtils.notifyListener(this.listener, PRODUCTS_SYNC_DONE,
                null);
        SyncUtils.notifyListener(this.listener, COMPOSITIONS_SYNC_DONE, compos);
        SyncUtils.notifyListener(this.listener, CATALOG_SYNC_DONE, this.catalog);
    }

    private void parseRoles(JSONArray array) {
        try {
            for (int i = 0; i < array.length(); i++) {
                JSONObject o = array.getJSONObject(i);
                String permissions = "";
                JSONArray jsPerms = o.getJSONArray("permissions");
                for (int ri = 0; ri < jsPerms.length(); ri++) {
                    if (jsPerms.getString(ri).length() > 0) {
                        permissions += ";" + jsPerms.getString(ri);
                    }
                }
                if (permissions.length() > 0) {
                    permissions = permissions.substring(1);
                }
                this.permissions.put(String.valueOf(o.getInt("id")), permissions);
            }
        } catch (JSONException e) {
            Log.e(LOG_TAG, "Unable to parse roles response: " + array.toString(), e);
            SyncUtils.notifyListener(this.listener, ROLES_SYNC_ERROR,
                    "Unable to parse roles response " + e.toString());
            return;
        }
        SyncUtils.notifyListener(this.listener, ROLES_SYNC_DONE,
                this.permissions);
    }

    /**
     * Parse users from JSONObject response. Roles must be parsed.
     */
    private void parseUsers(JSONArray array) {
        List<User> users = new ArrayList<User>();
        try {
            for (int i = 0; i < array.length(); i++) {
                JSONObject o = array.getJSONObject(i);
                String roleId = String.valueOf(o.getInt("role"));
                String permissions = this.permissions.get(roleId);
                User u = User.fromJSON(o, permissions);
                users.add(u);
            }
        } catch (JSONException e) {
            Log.e(LOG_TAG, "Unable to parse users response: " + array.toString(), e);
            SyncUtils.notifyListener(this.listener, USERS_SYNC_ERROR,
                    "Unable to parse users response " + e.toString());
            return;
        }
        SyncUtils.notifyListener(this.listener, USERS_SYNC_DONE, users);
    }

    private void parseCustomers(JSONArray array) {
        List<Customer> customers = new ArrayList<Customer>();
        try {
            for (int i = 0; i < array.length(); i++) {
                JSONObject o = array.getJSONObject(i);
                Customer c = Customer.fromJSON(o);
                customers.add(c);
            }
        } catch (JSONException e) {
            Log.e(LOG_TAG, "Unable to parse customers response: " + array.toString(), e);
            SyncUtils.notifyListener(this.listener, CUSTOMERS_SYNC_ERROR, e);
            return;
        }
        SyncUtils.notifyListener(this.listener, CUSTOMERS_SYNC_DONE, customers);
    }

    private void parseCash(JSONObject o) {
        Cash cash = null;
        try {
            cash = new Cash(o);
        } catch (JSONException e) {
            Log.e(LOG_TAG, "Unable to parse cash session response: " + o.toString(),
                    e);
            SyncUtils.notifyListener(this.listener, CASH_SYNC_ERROR,
                    "Unable to parse cash session response " + e.toString());
            return;
        }
        SyncUtils.notifyListener(this.listener, CASH_SYNC_DONE, cash);
    }

    private void parsePlaces(JSONArray array) {
        List<Floor> floors = new ArrayList<Floor>();
        try {
            for (int i = 0; i < array.length(); i++) {
                JSONObject o = array.getJSONObject(i);
                Floor f = Floor.fromJSON(o);
                floors.add(f);
            }
        } catch (JSONException e) {
            e.printStackTrace();
            SyncUtils.notifyListener(this.listener, PLACES_SYNC_ERROR,
                    "Unable to parse places response " + e.toString());
            return;
        }
        SyncUtils.notifyListener(this.listener, PLACES_SYNC_DONE, floors);
    }

    private void parseTariffAreas(JSONArray a) {
        List<TariffArea> areas = new ArrayList<TariffArea>();
        try {
            for (int i = 0; i < a.length(); i++) {
                JSONObject o = a.getJSONObject(i);
                TariffArea area = TariffArea.fromJSON(o);
                areas.add(area);
            }
        } catch (JSONException e) {
            e.printStackTrace();
            SyncUtils.notifyListener(this.listener, TARIFF_AREA_SYNC_ERROR,
                    "Unable to parse tariff areas response " + e.toString());
            return;
        }
        SyncUtils.notifyListener(this.listener, TARIFF_AREAS_SYNC_DONE, areas);
    }

    private void parsePaymentModes(JSONArray a) {
        List<PaymentMode> modes = new ArrayList<PaymentMode>();
        try {
            ImagesData.clearPaymentModes(this.ctx);
        } catch (IOException e) {
            Log.w(LOG_TAG, "Unable to clear payment mode images", e);
        }
        try {
            for (int i = 0; i < a.length(); i++) {
                JSONObject o = a.getJSONObject(i);
                PaymentMode mode = PaymentMode.fromJSON(o);
                if (o.getBoolean("hasImage")) {
                    // TODO: restore image calls
                }
                modes.add(mode);
            }
            Collections.sort(modes, new Comparator<PaymentMode>() {
                @Override
                public int compare(PaymentMode o1, PaymentMode o2) {
                    return o1.getDispOrder() - o2.getDispOrder();
                }
            });
        } catch (JSONException e) {
            e.printStackTrace();
            SyncUtils.notifyListener(this.listener, PAYMENTMODE_SYNC_ERROR,
                    "Unable to parse payment modes response " + e.toString());
            return;
        }
        SyncUtils.notifyListener(this.listener, PAYMENTMODE_SYNC_DONE, modes);
    }

    private void parseResources(JSONArray a) {
        try {
            String resContent = null;
            String name = null;
            for (int i = 0; i < a.length(); i++) {
                JSONObject res = a.getJSONObject(i);
                resContent = res.getString("content");
                name = res.getString("label");
                SyncUtils.notifyListener(this.listener, RESOURCE_SYNC_DONE,
                        new String[]{name, resContent});
            }
        } catch (JSONException e) {
            e.printStackTrace();
            SyncUtils.notifyListener(this.listener, RESOURCE_SYNC_ERROR,
                    "Unable to parse resources " + e.toString());
        }
    }

    private void parseDiscounts(JSONArray a) {
        ArrayList<Discount> discounts = new ArrayList<>();
        try {
            for (int i = 0; i < a.length(); i++) {
                JSONObject o = a.getJSONObject(i);
                Discount disc = Discount.fromJSON(o);
                discounts.add(disc);
            }
        } catch (JSONException | ParseException e) {
            SyncUtils.notifyListener(this.listener, DISCOUNT_SYNC_ERROR,
                    "Unable to parse discounts " + e.toString());
            return;
        }
        SyncUtils.notifyListener(this.listener, DISCOUNT_SYNC_DONE, discounts);
    }

    private void finish() {
        SyncUtils.notifyListener(this.listener, SYNC_DONE);
    }

}
