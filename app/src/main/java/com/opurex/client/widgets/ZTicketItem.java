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
import com.opurex.client.models.ZTicket;
import com.opurex.client.utils.StringUtils;

import android.content.Context;
import android.view.LayoutInflater;
import android.widget.TextView;
import android.widget.RelativeLayout;

public class ZTicketItem extends RelativeLayout
{
    private ZTicket z;

    private TextView label;
    private TextView dateStart;
    private TextView dateStop;

    public ZTicketItem(Context context, ZTicket z) {
        super(context);
        LayoutInflater.from(context).inflate(R.layout.z_item,
                this, true);
        this.label = (TextView) this.findViewById(R.id.z_label);
        this.dateStart = (TextView) this.findViewById(R.id.z_opendate);
        this.dateStop = (TextView) this.findViewById(R.id.z_closedate);
        this.reuse(z);
    }

    public void reuse(ZTicket z) {
        this.z = z;
        // Ticket number and amount
        this.label.setText("[" + this.getContext().getString(R.string.z_sequence) + " " + z.getCash().getSequence() + "]");
        // Date and time
        this.dateStart.setText(StringUtils.formatDateTimeNumeric(this.getContext(), z.getCash().getOpenDate() * 1000));
        this.dateStop.setText(StringUtils.formatDateTimeNumeric(this.getContext(), z.getCash().getCloseDate() * 1000));
    }

    public ZTicket getZTicket() {
        return this.z;
    }
}
