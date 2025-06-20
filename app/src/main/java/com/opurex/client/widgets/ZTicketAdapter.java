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

import com.opurex.client.models.ZTicket;
import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import java.util.List;

public class ZTicketAdapter extends BaseAdapter {

    private List<ZTicket> zs;

    public ZTicketAdapter(List<ZTicket> zs) {
        super();
        this.zs = zs;
    }

    @Override
    public boolean areAllItemsEnabled() {
        return true;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public Object getItem(int position) {
        return this.zs.get(position);
    }

    @Override
    public int getCount() {
        return this.zs.size();
    }
    
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ZTicket z = this.zs.get(position);
        if (convertView != null && convertView instanceof ZTicketItem) {
            // Reuse the view
            ZTicketItem item = (ZTicketItem) convertView;
            item.reuse(z);
            return item;
        } else {
            // Create the view
            Context ctx = parent.getContext();
            ZTicketItem item = new ZTicketItem(ctx, z);
            return item;
        }
    }
}
