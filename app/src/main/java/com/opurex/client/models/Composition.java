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
import java.util.List;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class Composition implements Serializable {

    private String productId;
    private List<Group> groups;

    public Composition(String productId, List<Group> groups) {
        this.productId = productId;
        this.groups = groups;
    }

    public String getProductId() {
        return this.productId;
    }

    public List<Group> getGroups() {
        return this.groups;
    }

    public static Composition fromJSON(JSONObject o)
            throws JSONException {
        String id = String.valueOf(o.getInt("id"));
        List<Group> groups = new ArrayList<Group>();
        JSONArray jsGrps = o.getJSONArray("compositionGroups");
        for (int i = 0; i < jsGrps.length(); i++) {
            Group g = Group.fromJSON(jsGrps.getJSONObject(i));
            groups.add(g);
        }
        return new Composition(id, groups);
    }

    public static class Group implements Serializable {

        private String id;
        private String label;
        private List<String> productIds;

        public Group(String id, String label, List<String> productIds) {
            this.id = id;
            this.label = label;
            this.productIds = productIds;
        }

        public String getLabel() {
            return this.label;
        }

        public List<String> getProductIds() {
            return this.productIds;
        }

        public static Group fromJSON(JSONObject o) throws JSONException {
            String id = String.valueOf(o.getInt("id"));
            String label = o.getString("label");
            List<String> prdIds = new ArrayList<String>();
            JSONArray jsPrds = o.getJSONArray("compositionProducts");
            for (int i = 0; i < jsPrds.length(); i++) {
                JSONObject choice = jsPrds.getJSONObject(i);
                String prdId = String.valueOf(choice.getInt("product"));
                prdIds.add(prdId);
            }
            return new Group(id, label, prdIds);
        }

        @Override
        public int hashCode() {
            return this.id.hashCode() + 5;
        }

        @Override
        public boolean equals(Object o) {
            return (o instanceof Group && this.id.equals(((Group)o).id));
        }
    }

}
