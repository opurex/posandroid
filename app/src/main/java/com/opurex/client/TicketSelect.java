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
package com.opurex.client;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.*;

import java.io.IOError;

import com.opurex.client.data.Data;
import com.opurex.client.models.Place;
import com.opurex.client.models.Ticket;
import com.opurex.client.models.Session;
import com.opurex.client.models.User;
import com.opurex.client.sync.TicketUpdater;
import com.opurex.client.activities.TrackedActivity;
import com.opurex.client.utils.Error;
import com.opurex.client.widgets.ProgressPopup;
import com.opurex.client.widgets.RestaurantTicketsAdapter;

public class TicketSelect extends TrackedActivity implements
        ExpandableListView.OnChildClickListener,
        AdapterView.OnItemClickListener {

    private static final String LOG_TAG = "Opurex/TicketSelect";
    public static final int CODE_TICKET = 2;

    private ListView list;
    private ProgressPopup syncPopup;
    // TODO: quick and dirty way to block multi-click, should use syncPopup
    private boolean loading;

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);
        // Set views
        setContentView(R.layout.ticket_select_restaurant);
        this.list = (ListView) this.findViewById(R.id.tickets_list);
        RestaurantTicketsAdapter adapter = new RestaurantTicketsAdapter(Data.Place.floors);
        ((ExpandableListView) this.list).setAdapter(adapter);
        ((ExpandableListView) this.list).setOnChildClickListener(this);
        this.updateSharedTicket();
    }

    private void updateSharedTicket() {
        new TicketUpdater(this).getAllSharedTickets(new ListTktHandler());
    }

    @Override
    public void onResume() {
        super.onResume();
        // Refresh data
        ExpandableListView exlist = (ExpandableListView) this.list;
        RestaurantTicketsAdapter adapt = (RestaurantTicketsAdapter) exlist
                .getExpandableListAdapter();
        boolean[] expanded = new boolean[adapt.getGroupCount()];
        for (int i = 0; i < adapt.getGroupCount(); i++) {
            expanded[i] = exlist.isGroupExpanded(i);
        }
        exlist.setAdapter(new RestaurantTicketsAdapter(Data.Place.floors));
        for (int i = 0; i < adapt.getGroupCount(); i++) {
            if (expanded[i]) {
                exlist.expandGroup(i);
            } else {
                exlist.collapseGroup(i);
            }
        }
    }

    public void refreshList() {
        this.list.invalidateViews();
    }

    /**
     * End activity correctly according to ticket mode. Call once current
     * ticket is set in session
     */
    private void selectTicket(Ticket t) {
        this.loading = false;
        Data.Session.currentSession(this).setCurrentTicket(t);
        this.setResult(Activity.RESULT_OK);
        Intent i = new Intent(this, Flavor.Transaction);
        this.startActivity(i);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
        if (this.loading) {
            return;
        }
        Ticket t = Data.Session.currentSession(this).getTickets().get(position);
        this.selectTicket(t);
    }

    @Override
    public boolean onChildClick(ExpandableListView parent, View v,
                                int groupPosition, int childPosition, long id) {
        if (this.loading) {
            return true;
        }
        ExpandableListView exlist = (ExpandableListView) this.list;
        ExpandableListAdapter adapt = exlist.getExpandableListAdapter();
        Place p = (Place) adapt.getChild(groupPosition, childPosition);
        Session currSession = Data.Session.currentSession(this);
        // Check if a ticket is already there
        for (Ticket t : currSession.getTickets()) {
            if (t.getLabel().equals(p.getName())) {
                // It's there, get it now!
                this.selectTicket(t);
                return true;
            }
        }
        // No ticket for this table
        Ticket t = currSession.newCurrentTicket();
        t.assignToPlace(p);
        this.selectTicket(t);
        return true;
    }

    private static final int MENU_CLOSE_CASH = 0;
    private static final int MENU_NEW_TICKET = 1;
    private static final int MENU_SYNC_TICKET = 2;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        int i = 0;
        User cashier = Data.Session.currentSession(this).getUser();
        if (cashier.hasPermission("com.opurex.pos.panels.JPanelCloseMoney")) {
            MenuItem close = menu.add(Menu.NONE, MENU_CLOSE_CASH, i++,
                    this.getString(R.string.menu_main_close));
            close.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM
                    | MenuItem.SHOW_AS_ACTION_WITH_TEXT);
        }
        return i > 0;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case MENU_CLOSE_CASH:
            CloseCash.close(this);
            break;
        case MENU_NEW_TICKET:
            Data.Session.currentSession(this).newCurrentTicket();
            try {
                Data.Session.save();
            } catch (IOError ioe) {
                Log.e(LOG_TAG, "Unable to save session", ioe);
                Error.showError(R.string.err_save_session, this);
            }
            this.setResult(Activity.RESULT_OK);
            this.finish();
            break;
        case MENU_SYNC_TICKET:
            new TicketUpdater(this).getAllSharedTickets(new ListTktHandler());
            refreshList();
            break;
        }
        return true;
    }

    private class ListTktHandler extends Handler {
        @Override
        public void handleMessage (Message msg) {
            TicketSelect.this.refreshList();
        }
    }

    private class SingleTktHandler extends Handler {

        private Ticket requestedTkt;

        public SingleTktHandler(Ticket requestedTkt) {
            super();
            this.requestedTkt = requestedTkt;
        }

        @Override
        public void handleMessage(Message msg) {
            Ticket t = (Ticket) msg.obj;
            if (t != null) {
                TicketSelect.this.selectTicket(t);
            } else {
                // Nothing found from server, use local one
                // TODO: make a difference from new ticket and deleted one
                TicketSelect.this.selectTicket(this.requestedTkt);
            }
        }
    }
}
