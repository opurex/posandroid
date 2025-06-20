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

public class Customer implements Serializable {

    private String id;
    private String name;
    private String card;
    private String firstName;
    private String lastName;
    private String address1;
    private String address2;
    private String zipCode;
    private String city;
    private String department;
    private String country;
    private String mail;
    private String phone1;
    private String phone2;
    private String fax;
    private double balance;
    private double maxDebt;
    private String tariffAreaId;
    private String note;

    public Customer(String id, String name, String card, String firstName, String lastName, String address1,
                    String address2, String zipCode, String city, String department, String country,
                    String mail, String phone1, String phone2, String fax,
                    double balance, double maxDebt, String area, String note) {
        this.id = id;
        this.name = name;
        this.card = card;
        this.balance = balance;
        this.maxDebt = maxDebt;
        this.firstName = firstName;
        this.lastName = lastName;
        this.address1 = address1;
        this.address2 = address2;
        this.zipCode = zipCode;
        this.city = city;
        this.department = department;
        this.country = country;
        this.mail = mail;
        this.phone1 = phone1;
        this.phone2 = phone2;
        this.fax = fax;
        this.tariffAreaId = area;
        this.note = note;
    }

    public String getId() {
        return this.id;
    }
    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return this.name;
    }

    public String getFirstName() {
        return this.firstName;
    }

    public String getLastName() {
        return this.lastName;
    }

    public String getAddress1() {
        return this.address1;
    }

    public String getAddress2() {
        return this.address2;
    }

    public String getZipCode() {
        return this.zipCode;
    }

    public String getCity() {
        return this.city;
    }

    public String getCountry() {
        return this.country;
    }

    public String getPhone1() {
        return this.phone1;
    }

    public String getMail() {
        return this.mail;
    }

    public String getCard() {
        return this.card;
    }

       /** @deprecated */
    @Deprecated
    public double getCurrDebt() {
        if (this.balance < 0.0) {
            return this.balance * -1;
        }
        return 0.0;
    }

    public double getMaxDebt() {
        return this.maxDebt;
    }

       /** @deprecated */
    @Deprecated
    public void addDebt(double amount) {
        this.balance -= amount;
    }

       /** @deprecated */
    @Deprecated
    public double getPrepaid() {
        if (this.balance > 0.0) {
            return this.balance;
        }
        return 0.0;
    }

       /** @deprecated */
    @Deprecated
    public void setPrepaid(double value) {
        this.balance = balance;
    }
       /** @deprecated */
    @Deprecated
    public void addPrepaid(double amount) {
        this.balance += amount;
    }

    public double getBalance() { return this.balance; }
    /** Update the balance. Use positive amount to fill the account
     * and negative value to use it.
     */
    public void updateBalance(double amount) {
        this.balance += amount;
    }

    public String getTariffAreaId() {
        return this.tariffAreaId;
    }

    public String getNote() {
        return this.note.equals("null") ? "" : this.note;
    }

    public static Customer fromJSON(JSONObject o) throws JSONException {
        String id = String.valueOf(o.getInt("id"));
        String name = o.getString("dispName");
        String card = o.getString("card");
        double maxDebt = o.getDouble("maxDebt");
        double balance = o.getDouble("balance");
        String firstName = o.getString("firstName");
        String lastName = o.getString("lastName");
        String mail = o.getString("email");
        String fax = o.getString("fax");
        String phone1 = o.getString("phone1");
        String phone2 = o.getString("phone2");
        String address1 = o.getString("addr1");
        String address2 = o.getString("addr2");
        String zipCode = o.getString("zipCode");
        String city = o.getString("city");
        String department = o.getString("region");
        String country = o.getString("country");
        String note = o.getString("note");
        // visible
        // hasImage
        // expireDate
        // discountProfile
        String tariffAreaId = null;
        if (!o.isNull("tariffArea")) {
            tariffAreaId = String.valueOf(o.getInt("tariffArea"));
        }
        // tax
        return new Customer(id, name, card, firstName,lastName, address1,address2, zipCode, city, department,
                country,mail, phone1, phone2, fax, balance, maxDebt, tariffAreaId, note);
    }

    public JSONObject toJSON() throws JSONException {
        JSONObject o = new JSONObject();
        o.put("dispName", this.name);
        o.put("card", this.card);
        o.put("maxDebt", this.maxDebt);
        o.put("balance", this.balance); // Ignored by server
        o.put("firstName", this.firstName);
        o.put("lastName", this.lastName);
        o.put("email", this.mail);
        o.put("phone1", this.phone1);
        o.put("phone2", this.phone2);
        o.put("fax", this.fax);
        o.put("addr1", this.address1);
        o.put("addr2", this.address2);
        o.put("zipCode", this.zipCode);
        o.put("city", this.city);
        o.put("region", this.department);
        o.put("country", this.country);
        o.put("note", this.note);
        o.put("visible", true);
        o.put("hasImage", false);
        o.put("expireDate", JSONObject.NULL);
        o.put("discountProfile", JSONObject.NULL);
        if (this.tariffAreaId != null) {
            o.put("tariffArea", Integer.parseInt(this.tariffAreaId));
        } else {
            o.put("tariffArea", JSONObject.NULL);
        }
        o.put("tax", JSONObject.NULL);
        return o;
    }

    @Override
    public String toString() {
        return this.name;
    }

    @Override
    public boolean equals(Object o) {
        return (o instanceof Customer) && this.id.equals(((Customer)o).id);
    }
}
