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
package com.opurex.client.widgets;

import com.opurex.client.R;
import com.opurex.client.models.Floor;

import android.content.Context;
import android.view.LayoutInflater;
import android.widget.TextView;
import android.widget.RelativeLayout;

public class RestaurantFloorItem extends RelativeLayout {

    private Floor floor;

    private TextView label;

    public RestaurantFloorItem(Context context, Floor f) {
        super(context);
        LayoutInflater.from(context).inflate(R.layout.restaurant_floor_item,
                                             this,
                                             true);
        this.label = (TextView) this.findViewById(R.id.floor_label);
        this.reuse(f);
    }

    public void reuse(Floor f) {
        this.floor = f;
        this.label.setText(this.floor.getName());
    }

    public Floor getFloor() {
        return this.floor;
    }

}