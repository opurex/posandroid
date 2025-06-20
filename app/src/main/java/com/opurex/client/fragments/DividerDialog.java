package com.opurex.client.fragments;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import androidx.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import com.opurex.client.Configure;
import com.opurex.client.OpurexPOS;
import com.opurex.client.R;
import com.opurex.client.data.Data;
import com.opurex.client.interfaces.TicketLineEditListener;
import com.opurex.client.models.Floor;
import com.opurex.client.models.Ticket;
import com.opurex.client.models.TicketLine;
import com.opurex.client.models.Place;
import com.opurex.client.utils.Tuple;
import com.opurex.client.widgets.TicketLineItem;
import com.opurex.client.widgets.TicketLinesAdapter;

import java.util.List;

/**
 * Created by svirch_n on 25/05/16
 * Last edited at 17:33.
 * The ticket divider dialog.
 * It manipulates a copy of the original ticket and a create one
 * for the resulting ticket.
 * They are both passed to the callback on confirm.
 */
public class DividerDialog extends OpurexPopupFragment {

    public static final String TAG = "DIVIDER_DIALOG";
    private static final String TICKET_TAG = "TICKET_TAG";
    private static final String TICKET_TAG2 = "TICKET_TAG2";

    private ResultListener resultListener;
    private DividerAdapter newTicketAdapter;
    private Ticket ticketToDivide;
    private Ticket dividedTicket;

    public interface ResultListener {
        /** Callback to effectively proceed to the split.
         * @param originalTicket The original ticket after it has been removed
         * the splitted lines.
         * @param splitTicket The new ticket created from the original one by
         * extracting some items. */
        void onDividerDialogResult(Ticket originalTicket, Ticket splitTicket);
    }

    public interface RequestResultListener {
        ResultListener onDividerDialogRequestResultListener();
    }

    @Override
    protected void onNegativeClickListener() {
        this.dismiss();
    }

    @Override
    protected void onPositiveClickListener() {
        this.dismiss();
        this.resultListener.onDividerDialogResult(this.ticketToDivide,
                this.dividedTicket);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.ticketToDivide = (Ticket) getArguments().getSerializable(TICKET_TAG);
        this.dividedTicket = (Ticket) getArguments().getSerializable(TICKET_TAG2);
    }

    @Override
    public View onCreateFrameView(LayoutInflater inflater, FrameLayout frameContainer, Bundle savedInstanceState) {
        // Load views, set title and buttons
        View result = inflater.inflate(R.layout.divider_dialog, frameContainer, false);
        frameContainer.setLayoutParams(new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        setTitle(OpurexPOS.getStringResource(R.string.menu_divider));
        setPositiveTitle(OpurexPOS.getStringResource(R.string.divider_button_positive));
        setNegativeTitle(OpurexPOS.getStringResource(R.string.divider_button_negative));
        ListView originalTicketListView = (ListView) result.findViewById(R.id.list1);
        DividerAdapter originalAdapter = new DividerAdapter(this.ticketToDivide);
        originalTicketListView.setAdapter(originalAdapter);
        // Set destination ticket views and adapters
        ListView newTicketListView = (ListView) result.findViewById(R.id.list2);
        this.newTicketAdapter = new DividerAdapter(this.dividedTicket);
        newTicketListView.setAdapter(this.newTicketAdapter);
        // Link adapters
        originalAdapter.setTarget(this.newTicketAdapter);
        this.newTicketAdapter.setTarget(originalAdapter);
        return result;
    }

    /**
     * @deprecated not called since API 23 (Marshmallow)
     * @param activity
     */
    @Deprecated
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        onAttach((RequestResultListener) activity);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        onAttach((RequestResultListener) context);
    }

    private void onAttach(RequestResultListener context) {
        resultListener = context.onDividerDialogRequestResultListener();
    }

    @NonNull
    public static DividerDialog newInstance(Ticket ticketToDivide, Context ctx) {
        Ticket tmpCopy = ticketToDivide.getTmpTicketCopy();
        Ticket destTicket = new Ticket();
        if (Configure.getTicketsMode(ctx) == Configure.RESTAURANT_MODE) {
            // Assign the new ticket to the same place
            for (Floor f : Data.Place.floors) {
                boolean found = false;
                for (Place p : f.getPlaces()) {
                    if (tmpCopy.getId().equals(p.getId())) {
                        destTicket.assignToPlace(p);
                        found = true;
                        break;
                    }
                }
                if (found) {
                    break;
                }
            }
        }
        // Pass the arguments in Bundle for onCreate
        DividerDialog result = new DividerDialog();
        Bundle args = new Bundle();
        args.putSerializable(DividerDialog.TICKET_TAG, tmpCopy);
        args.putSerializable(DividerDialog.TICKET_TAG2, destTicket);
        result.setArguments(args);
        return result;
    }

    private class DividerAdapter extends TicketLinesAdapter {

        private final Ticket ticket;
        private DividerAdapter targetAdapter;

        private View.OnClickListener onClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Extract the selected line to manipulate it.
                TicketLineItem ticketLineItem = (TicketLineItem) view;
                TicketLine ticketLine = ticketLineItem.getLine();
                int index = DividerAdapter.this.ticket.getLines().indexOf(ticketLine);
                // Remove from ticket to update it later.
                DividerAdapter.this.ticket.getLines().remove(ticketLine);
                try {
                    // Get the tuple of resulting lines after extracting one qty
                    Tuple<TicketLine, TicketLine> ticketLineTicketLineTuple = ticketLine.splitTicketLineArticle();
                    // Inject the exctracted line in the target adapter.
                    DividerAdapter.this.targetAdapter.addTicketLine(ticketLineTicketLineTuple.first());
                    // Put the edited original line back if still there.
                    TicketLine second = ticketLineTicketLineTuple.second();
                    if (second != null) {
                        DividerAdapter.this.ticket.getLines().add(index, second);
                    }
                } catch (TicketLine.CannotSplitScaledProductException e) {
                    // For scaled products, just move the whole line.
                    DividerAdapter.this.targetAdapter.addTicketLine(ticketLine);
                }
                notifyDataSetChanged();
                targetAdapter.notifyDataSetChanged();
            }
        };

        DividerAdapter(Ticket ticket) {
            super(ticket.getLines(), nullListener, false);
            this.ticket = ticket;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View result = super.getView(position, convertView, parent);
            result.findViewById(R.id.product_edit_group).setVisibility(View.GONE);
            result.setOnClickListener(onClickListener);
            return result;
        }

        /** Set the target adapter that will receive the picked lines. */
        public void setTarget(DividerAdapter adapter) {
            this.targetAdapter = adapter;
        }

        /** Inject a ticket line. It will merge the line with an existing one
         * if possible and update the underlying ticket. */
        public void addTicketLine(TicketLine ticketLine) {
            for (TicketLine each: this.ticket.getLines()) {
                if (each.canMerge(ticketLine)) {
                    each.merge(ticketLine);
                    return;
                }
            }
            this.ticket.getLines().add(ticketLine);
        }


        public List<TicketLine> getLines() {
            return this.ticket.getLines();
        }
    }

    private static final TicketLineEditListener nullListener = new TicketLineEditListener() {
        @Override
        public void addQty(TicketLine t) {

        }

        @Override
        public void remQty(TicketLine t) {

        }

        @Override
        public void mdfyQty(TicketLine t) {

        }

        @Override
        public void editProduct(TicketLine t) {

        }

        @Override
        public void delete(TicketLine t) {

        }
    };
}
