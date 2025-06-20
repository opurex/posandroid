package com.opurex.client;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import androidx.fragment.app.FragmentTransaction;
import android.view.Menu;
import android.view.MenuItem;

import com.opurex.client.activities.TrackedActivity;
import com.opurex.client.data.Data;
import com.opurex.client.fragments.RestaurantTicketSelectFragment;
import com.opurex.client.models.Place;
import com.opurex.client.models.Ticket;
import com.opurex.client.models.User;
import com.opurex.client.sync.TicketUpdater;

/**
 * Created by svirch_n on 23/05/16
 * Last edited at 11:50.
 */
public class RestaurantTicketSelect extends TrackedActivity {

    private static final int MENU_CLOSE_CASH = 0;
    private static final int MENU_SYNC_TICKET = 1;
    private RestaurantTicketSelectFragment restaurantTicketSelectFragment;

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);
        if (state == null) {
            FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
            restaurantTicketSelectFragment = new RestaurantTicketSelectFragment();
            fragmentTransaction.add(android.R.id.content, restaurantTicketSelectFragment);
            fragmentTransaction.commit();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        requestAllTickets();
    }

    @SuppressWarnings("Duplicates")
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
            case MENU_SYNC_TICKET:
                updateAllTickets();
                refreshList();
                break;
        }
        return true;
    }

    /**
     * End activity correctly according to ticket mode. Call once current
     * ticket is set in session
     */
    private void selectTicket(Ticket t) {
        Data.Session.currentSession(this).setCurrentTicket(t);
        this.setResult(Activity.RESULT_OK);
        Intent i = new Intent(this, Flavor.Transaction);
        this.startActivity(i);
    }


    public void accessPlace(Place place) {
        Ticket ticket = place.getAssignedTicket();
        if (ticket == null) {
            ticket = Data.Session.currentSession().newTicket();
            ticket.assignToPlace(place);
            selectTicket(ticket);
        } else {
            requestTicket(ticket);
        }
    }

    /**
     * Smart ticket updater
     * Update only if the application is configured to update
     */
    private void requestTicket(Ticket t) {
        selectTicket(t);
    }

    /**
     * Smart tickets updater
     * Update only if the application is configured to update
     */
    private void requestAllTickets() {
        this.refreshList();
    }

    /**
     * Update the tickets
     * And refresh the view
     */
    private void updateAllTickets() {
        new TicketUpdater(this).getAllSharedTickets(new ListTktHandler());
    }

    /**
     * Update the ticket
     * And do the selectTicket(ticket) thing on response
     *
     * @param ticket to update
     */
    private void updateAndSelectTicket(Ticket ticket) {
        new TicketUpdater(this).getSharedTicket(ticket.getId(), new SingleTktHandler(ticket));
    }

    private void refreshList() {
        restaurantTicketSelectFragment.refreshView();
    }

    //Handle the request response
    private class ListTktHandler extends Handler {
        @Override
        public void handleMessage (Message msg) {
            RestaurantTicketSelect.this.refreshList();
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
                RestaurantTicketSelect.this.selectTicket(t);
            } else {
                // Nothing found from server, use local one
                // TODO: make a difference from new ticket and deleted one
                RestaurantTicketSelect.this.selectTicket(this.requestedTkt);
            }
        }
    }

}
