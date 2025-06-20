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

import com.opurex.client.data.Data;
import org.json.JSONException;
import org.json.JSONObject;


public class Place implements Serializable {

    private String id;
    private String name;
    private int x;
    private int y;

    public Place(String id, String name, int x, int y) {
        this.id = id;
        this.name = name;
        this.x = x;
        this.y = y;
    }

    public String getId() {
        return this.id;
    }

    public String getName() {
        return this.name;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return this.y;
    }

    public static Place fromJSON(JSONObject o) throws JSONException {
        String id = String.valueOf(o.getInt("id"));
        String name = o.getString("label");
        int x = o.getInt("x");
        int y = o.getInt("y");
        return new Place(id, name, x, y);
    }

    public boolean isOccupied() {
        return getAssignedTicket() != null;
    }

    public Ticket getAssignedTicket() {
        if (this.getName() == null) {
            return null;
        }
        for (Ticket ticket : Data.Session.currentSession().getTickets()) {
            if (this.getName().equals(ticket.getLabel())) {
                return ticket;
            }
        }
        return null;
    }
}
