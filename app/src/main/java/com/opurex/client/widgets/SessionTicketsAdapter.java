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
import com.opurex.client.data.Data;
import com.opurex.client.models.Ticket;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

public class SessionTicketsAdapter extends BaseAdapter {

    public static final float HEIGHT_DIP = 52.8f; // same as in xml with margin

    private Context ctx;

    public SessionTicketsAdapter(Context ctx) {
        super();
        this.ctx = ctx;
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
        return Data.Session.currentSession(this.ctx).getTickets().get(position);
    }

    @Override
    public int getCount() {
        return Data.Session.currentSession(this.ctx).getTickets().size();
    }
    
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        Ticket t = (Ticket) this.getItem(position);
        if (convertView == null) {
            // Create the view
            LayoutInflater inflater = (LayoutInflater) this.ctx.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.ticket_select, parent, false);
        }
        // Reuse the view
        String label = this.ctx.getString(R.string.ticket_label, t.getLabel());
        ((TextView)convertView.findViewById(R.id.ticket_label)).setText(label);
        ((TextView)convertView.findViewById(R.id.ticket_prd_count)).setText(String.valueOf(t.getArticlesCount()));
        return convertView;
    }
}
