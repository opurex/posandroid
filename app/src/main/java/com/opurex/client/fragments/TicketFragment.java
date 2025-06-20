package com.opurex.client.fragments;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListPopupWindow;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.io.IOError;
import java.util.ArrayList;
import java.util.List;

import com.opurex.client.Configure;
import com.opurex.client.R;
import com.opurex.client.RestaurantTicketSelect;
import com.opurex.client.TicketSelect;
import com.opurex.client.interfaces.TicketLineEditListener;
import com.opurex.client.data.Data;
import com.opurex.client.models.*;
import com.opurex.client.sync.TicketUpdater;
import com.opurex.client.utils.ReadList;
import com.opurex.client.utils.ScreenUtils;
import com.opurex.client.widgets.SessionTicketsAdapter;
import com.opurex.client.widgets.TariffAreasAdapter;
import com.opurex.client.widgets.TicketLinesAdapter;

public class TicketFragment extends ViewPageFragment
        implements TicketLineEditListener,
        TicketLineEditDialog.Listener,
        CustomerInfoDialog.TicketListener {

    public interface Listener {
        void onTfCheckInClick();
        void onTFTicketChanged();
        void onTfCheckOutClick();
    }

    public interface HandlerCallback {
        void callback(List<Ticket> sharedTickets);

        void callback(Ticket Ticket);
    }

    public static final int CHECKIN_STATE = 0;
    public static final int CHECKOUT_STATE = 1;

    private static final String LOG_TAG = "Opurex/TicketInfo";
    // Serialize string
    private static final String TICKET_DATA = "ticket";
    private static final String PAGE_STATE = "page_state";

    //Data
    private Listener mListener;
    private Ticket mTicketData;
    private int mCurrentState;
    private boolean mbEditable;
    //View
    private TextView mTitle;
    private TextView mCustomer;
    private TextView mTotal;
    private TextView mTariffArea;
    private ImageView mCustomerImg;
    private ImageButton mNewBtn;
    private ImageButton mDeleteBtn;
    private ListView mTicketLineList;
    private ImageButton mCheckInCart;
    private ImageButton mCheckOutCart;
    private RelativeLayout mCustomerBtn;
    private TextView mDiscount;
    private ViewGroup mDiscountHolder;
    private ImageButton mDeleteDiscBtn;
    private ListPopupWindow mPopUpWindow;

    @SuppressWarnings("unused") // Used via class reflection
    public static TicketFragment newInstance(int pageNumber) {
        TicketFragment frag = new TicketFragment();
        ViewPageFragment.initPageNumber(pageNumber, frag);
        return frag;
    }

    /*
     *  PUBLIC
     */

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (Listener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement TicketFragment Listener!");
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        reuseData(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View layout = inflater.inflate(R.layout.ticket_information, container, false);
        layout.setPadding(1, 0, 1, 0);
        mTitle = (TextView) layout.findViewById(R.id.ticket_label);
        mCustomer = (TextView) layout.findViewById(R.id.ticket_customer_name);
        mTotal = (TextView) layout.findViewById(R.id.ticket_total);
        mTariffArea = (TextView) layout.findViewById(R.id.ticket_area);
        mCustomerImg = (ImageView) layout.findViewById(R.id.ticket_customer_img);
        mNewBtn = (ImageButton) layout.findViewById(R.id.ticket_new);
        mDeleteBtn = (ImageButton) layout.findViewById(R.id.ticket_delete);
        mCheckInCart = (ImageButton) layout.findViewById(R.id.btn_cart_back);
        mDiscount = (TextView) layout.findViewById(R.id.ticket_discount);
        mDiscountHolder = (ViewGroup) layout.findViewById(R.id.ticket_discount_holder);
        mDeleteDiscBtn = (ImageButton) layout.findViewById(R.id.ticket_discount_delete);
        mCheckOutCart = (ImageButton) layout.findViewById(R.id.pay);
        mCustomerBtn = (RelativeLayout) layout.findViewById(R.id.ticket_customer);

        mTicketLineList = (ListView) layout.findViewById(R.id.ticket_content);
        mTicketLineList.setAdapter(new TicketLinesAdapter(mTicketData.getLines(), this, mbEditable));

        mDeleteDiscBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                removeDiscount();
            }
        });

        mTitle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchTicketClick(v);
            }
        });
        mTariffArea.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchTariffAreaClick(v);
            }
        });
        mNewBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addTicketClick(v);
            }
        });
        mDeleteBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                deleteTicketClick(v);
            }
        });
        mCheckInCart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mListener.onTfCheckInClick();
            }
        });
        mCheckOutCart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mListener.onTfCheckOutClick();
            }
        });
        mCustomerBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CustomerInfoDialog dial = CustomerInfoDialog.newInstance(false, mTicketData.getCustomer());
                dial.setDialogTicketListener(TicketFragment.this);
                dial.show(getFragmentManager());
            }
        });

        updatePageState();

        //TODO: Implement line 89 TARIFF AREA
        // Check presence of tariff areas
        if (Data.TariffArea.areas.size() == 0) {
            //layout.findViewById(R.id.change_area).setVisibility(View.GONE);
            mTariffArea.setVisibility(View.GONE);
        }
        return layout;
    }

    @Override
    public void onResume() {
        super.onResume();
        updateViewNoSave();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable(TICKET_DATA, mTicketData);
        outState.putInt(PAGE_STATE, mCurrentState);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case TicketSelect.CODE_TICKET:
                switch (resultCode) {
                    case Activity.RESULT_CANCELED:
                        break;
                    case Activity.RESULT_OK:
                        switchTicket(Data.Session.currentSession(mContext).getCurrentTicket());
                        break;
                }
                break;
            default:
                super.onActivityResult(requestCode, resultCode, data);
                break;
        }
    }

    public TariffArea getTariffArea() {
        return mTicketData.getTariffArea();
    }

    public double getTicketPrice() {
        return mTicketData.getTicketPrice();
    }

    public Customer getCustomer() {
        return mTicketData.getCustomer();
    }

    public ReadList<Payment> getPaymentList() {
        return mTicketData.getPaymentList();
    }

    public Ticket getTicketData() {
        return mTicketData;
    }

    // This prepaid is what's registered in the ticket
    public double getTicketPrepaid() {
        double prepaid = 0;
        for (TicketLine l : mTicketData.getLines()) {
            Product p = l.getProduct();
            if (p.isPrepaid()) {
                prepaid += l.getProductIncTax() * l.getQuantity();
            }
        }
        return prepaid;
    }

    public void setState(int state) {
        mCurrentState = state;
        mbEditable = (mCurrentState == CHECKIN_STATE);
    }

    public void setCustomer(Customer customer) {
        mTicketData.setCustomer(customer);
    }

    public void updateView() {
        updateViewNoSave();
        saveSession();
    }

    public void updateViewNoSave() {
        // Update ticket info
        String total = getString(R.string.ticket_total,
                mTicketData.getTicketPrice());
        String label = getString(R.string.ticket_label,
                mTicketData.getLabel());
        mTitle.setText(label);
        mTotal.setText(total);
        if (mTicketData.getDiscountRate() != 0) {
            mDiscount.setText(mTicketData.getDiscountRateString());
            mDiscountHolder.setVisibility(View.VISIBLE);
        } else {
            mDiscountHolder.setVisibility(View.GONE);
        }
        // Update customer info
        Customer c = mTicketData.getCustomer();
        if (c != null) {
            String name;
            if (c.getPrepaid() > 0.005) {
                name = getString(R.string.customer_prepaid_label,
                        c.getName(), c.getPrepaid());
            } else {
                name = c.getName();
            }
            mCustomer.setText(name);
            mCustomer.setVisibility(View.VISIBLE);
            mCustomerImg.setVisibility(View.VISIBLE);
        } else {
            mCustomer.setVisibility(View.GONE);
            mCustomerImg.setVisibility(View.GONE);
        }
        // Update tariff area info
        if (mTicketData.getTariffArea() == null) {
            mTariffArea.setText(R.string.default_tariff_area);
        } else {
            mTariffArea.setText(mTicketData.getTariffArea().getLabel());
        }
        mTicketLineList.setAdapter(new TicketLinesAdapter(mTicketData.getLines(), this, mbEditable));
        updatePageState();
    }

    public void updatePageState() {
        mCheckInCart.setEnabled(mCurrentState == CHECKOUT_STATE);
        mCheckOutCart.setEnabled(mCurrentState == CHECKIN_STATE);
        mNewBtn.setEnabled(mCurrentState == CHECKIN_STATE);
        mDeleteBtn.setEnabled(mCurrentState == CHECKIN_STATE);
        mDeleteDiscBtn.setEnabled(mTicketData.getDiscountRate() != Discount.DEFAULT_DISCOUNT_RATE);
        TicketLinesAdapter adp = ((TicketLinesAdapter) mTicketLineList.getAdapter());
        adp.setEditable(mbEditable);
        adp.notifyDataSetChanged();
    }

    public int addProduct(Product p) {
        // Simply return pos if you want to make the list view focus on the modified item;
        int pos = mTicketData.addProduct(p);
        return (pos == mTicketLineList.getCount()) ? (pos) : (-1);
    }

    public int addProductReturn(Product p) {
        int pos = mTicketData.addProductReturn(p);
        return (pos == mTicketLineList.getCount()) ? (pos) : (-1);
    }

    public int addProduct(CompositionInstance compo) {
        return mTicketData.addProduct(compo);
    }

    public void addLine(TicketLine line) {
        mTicketData.addTicketLine(line);
    }

    public void addScaledProduct(Product p, double scale) {
        mTicketData.addScaledProduct(p, scale);
    }

    public void addScaledProductReturn(Product p, double scale) {
        mTicketData.addScaledProductReturn(p, scale);
    }

    public void setDiscountRate(double rate) {
        mTicketData.setDiscountRate(rate);
    }

    public double getDiscountRate(double rate) {
        return mTicketData.getDiscountRate();
    }

    public void removeDiscount() {
        mTicketData.setDiscountRate(Discount.DEFAULT_DISCOUNT_RATE);
        updateView();
    }

    public void switchTicket(Ticket t) {
        Data.Session.currentSession(mContext).setCurrentTicket(t);
        updateCurrentTicket();
        updateView();
    }

    public void switchTariffArea(TariffArea ta) {
        mTicketData.setTariffArea(ta);
        updateView();
    }

    private void updateCurrentTicket() {
        mTicketData = Data.Session.currentSession().getCurrentTicket();
        mListener.onTFTicketChanged();
    }

    public void removePayment(Payment payment) {
        mTicketData.removePayment(payment);
    }

    public void addPayment(Payment payment) {
        mTicketData.addPayment(payment);
    }

    public void scrollDown() {
        scrollTo(mTicketLineList.getCount() - 1);
    }

    public void scrollTo(final int position) {
        if (position < 0) return;
        mTicketLineList.post(new Runnable() {
            @Override
            public void run() {
                mTicketLineList.setSelection(position);
            }
        });
    }

    /*
     *  INTERFACES
     */

    @Override
    public void addQty(TicketLine l) {
        mTicketData.adjustQuantity(l, 1);
        updateView();
    }

    @Override
    public void remQty(TicketLine l) {
        mTicketData.adjustQuantity(l, -1);
        updateView();
    }

    /**
     * Modifies the weight of the product by asking the user a new one
     *
     * @param l the ticket's line
     */
    @Override
    public void mdfyQty(final TicketLine l) {
        Product p = l.getProduct();
        if (p.isScaled()) {
            ProductScaleDialog dial = ProductScaleDialog.newInstance(p, false);
            dial.setDialogListener(new ProductScaleDialog.Listener() {
                @Override
                public void onPsdPositiveClick(Product p, double weight, boolean ignored) {
                    mTicketData.adjustScale(l, weight);
                    updateView();
                }
            });
            dial.show(getFragmentManager(), ProductScaleDialog.TAG);
        }
    }

    @Override
    public void editProduct(final TicketLine l) {
        TicketLineEditDialog dial = TicketLineEditDialog.newInstance(l);
        dial.setDialogListener(this);
        dial.show(getFragmentManager(), TicketLineEditDialog.TAG);
    }

    @Override
    public void delete(TicketLine l) {
        mTicketData.removeTicketLine(l);
        updateView();
    }

    @Override
    public void onTicketLineEdited(TicketLine line) {
        updateView();
    }

    /*
     *  PRIVATES
     */

    private void reuseData(Bundle savedInstanceState) {
        if (savedInstanceState == null) {
            setState(CHECKIN_STATE);
            mTicketData = Data.Session.currentSession(mContext).getCurrentTicket();
        } else {
            setState(savedInstanceState.getInt(PAGE_STATE));
            mTicketData = (Ticket) savedInstanceState.getSerializable(TICKET_DATA);
        }
    }

    public void displayTicketsPopUp() {
        if (mPopUpWindow != null) {
            mPopUpWindow.dismiss();
        }
        final ListPopupWindow popup = new ListPopupWindow(mContext);
        mPopUpWindow = popup;
        ListAdapter adapter = new SessionTicketsAdapter(mContext);
        if (Configure.getTicketsMode(mContext) == Configure.RESTAURANT_MODE) {
            adapter = new SessionTicketsAdapter(mContext);
        }
        popup.setAnchorView(mTitle);
        popup.setAdapter(adapter);
        popup.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View v,
                                    int position, long id) {
                // TODO: handle connected mode on switch
                Ticket t = Data.Session.currentSession(mContext).getTickets().get(position);
                switchTicket(t);
                popup.dismiss();
            }

            public void onNothingSelected(AdapterView v) {
            }
        });
        popup.setWidth(ScreenUtils.inToPx(2, mContext));
        int ticketsCount = adapter.getCount();
        int height = (int) (ScreenUtils.dipToPx(SessionTicketsAdapter.HEIGHT_DIP *
                Math.min(5, ticketsCount), mContext) + mTitle.getHeight() / 2 + 0.5f);
        popup.setHeight(height);
        popup.show();
    }

    public void displayTariffAreaPopUp() {
        if (mPopUpWindow != null) {
            mPopUpWindow.dismiss();
        }
        final ListPopupWindow popup = new ListPopupWindow(mContext);
        mPopUpWindow = popup;
        final List<TariffArea> data = new ArrayList<TariffArea>();
        data.add(null);
        data.addAll(Data.TariffArea.areas);
        ListAdapter adapter = new TariffAreasAdapter(data);
        popup.setAnchorView(mTariffArea);
        popup.setAdapter(adapter);
        popup.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View v,
                                    int position, long id) {
                TariffArea ta = data.get(position);
                switchTariffArea(ta);
                popup.dismiss();
            }

            public void onNothingSelected(AdapterView v) {
            }
        });
        popup.setWidth(ScreenUtils.inToPx(2, mContext));
        int areaCount = adapter.getCount();
        int height = (int) (ScreenUtils.dipToPx(SessionTicketsAdapter.HEIGHT_DIP *
                Math.min(5, areaCount), mContext) + mTariffArea.getHeight() / 2 + 0.5f);
        popup.setHeight(height);
        popup.show();
    }

    /*
     *  BUTTON CLICK
     */

    private void switchTicketClick(View v) {
        displayTicketsPopUp();
    }

    private void switchTariffAreaClick(View v) {
        displayTariffAreaPopUp();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mPopUpWindow != null) {
            mPopUpWindow.dismiss();
        }
    }

    private void updateSharedTicket() {
        new TicketUpdater(getActivity()).getAllSharedTickets(
                new DataHandler(new HandlerCallback() {
                    @Override
                    public void callback(List<Ticket> sharedTickets) {
                        TicketFragment.this.updateReceivedSharedTickets(sharedTickets);
                        TicketFragment.this.displayTicketsPopUp();
                    }

                    @Override
                    public void callback(Ticket ticket) {
                        TicketFragment.this.updateReceivedTicket(ticket);
                    }
                }));
    }

    private void addTicketClick(View v) {
        Session currSession = Data.Session.currentSession(mContext);
        this.newTicket();
        switchTicket(currSession.getCurrentTicket());
    }

    private void deleteTicketClick(View v) {
        // Show confirmation
        AlertDialog.Builder b = new AlertDialog.Builder(mContext);
        b.setTitle(getString(R.string.delete_ticket_title));
        String message = getResources().getQuantityString(
                R.plurals.delete_ticket_message,
                mTicketData.getArticlesCount(), mTicketData.getArticlesCount());
        b.setMessage(message);
        b.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                Session currSession = Data.Session.currentSession(mContext);
                Ticket current = currSession.getCurrentTicket();
                for (Ticket t : currSession.getTickets()) {
                    if (t.getLabel().equals(current.getLabel())) {
                        currSession.removeTicket(t);
                        break;
                    }
                }
                if (currSession.getTickets().size() == 0) {
                    TicketFragment.this.newTicket();
                } else {
                    currSession.setCurrentTicket(currSession.getTickets().get(currSession.getTickets().size() - 1));
                }
                switchTicket(currSession.getCurrentTicket());
            }
        });
        b.setNegativeButton(android.R.string.no, null);
        b.show();
    }

    private void newTicket() {
        switch (Configure.getTicketsMode(mContext)) {
            case Configure.STANDARD_MODE:
                Data.Session.currentSession(mContext).newCurrentTicket();
                break;
            case Configure.RESTAURANT_MODE:
                goBackToRestaurantTicketSelect();
                break;
        }
    }

    private void goBackToRestaurantTicketSelect() {
        Intent i = new Intent(mContext, RestaurantTicketSelect.class);
        i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(i);
    }

    private void switchAreaClick(View v) {
        // Open tariff area popup
        final ListPopupWindow popup = new ListPopupWindow(mContext);
        final List<TariffArea> data = new ArrayList<TariffArea>();
        data.add(null);
        data.addAll(Data.TariffArea.areas);
        ListAdapter adapter = new TariffAreasAdapter(data);
        popup.setAnchorView(mTariffArea);
        popup.setAdapter(adapter);
        popup.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View v,
                                    int position, long id) {
                // TODO: handle connected mode on switch
                TariffArea area = data.get(position);
                mTicketData.setTariffArea(area);
                updateView();
                popup.dismiss();
            }

            public void onNothingSelected(AdapterView v) {
            }
        });
        popup.setWidth(ScreenUtils.inToPx(2, mContext));
        int areaCount = adapter.getCount();
        int height = (int) (ScreenUtils.dipToPx(TariffAreasAdapter.HEIGHT_DIP * Math.min(5, areaCount), mContext) + mTitle.getHeight() / 2 + 0.5f);
        popup.setHeight(height);
        popup.show();
    }

    private void saveSession() {
        try {
            Data.Session.save();
        } catch (IOError e) {
            Log.e(LOG_TAG, "Unable to save session", e);
        }
    }

    private void updateReceivedSharedTickets(List<Ticket> tickets) {
        Data.Session.currentSession().updateSharedTickets(tickets);
    }

    private void updateReceivedTicket(Ticket ticket) {
        Data.Session.currentSession().updateLocalTicket(ticket);
    }

    @Override
    public void onTicketRefund(Ticket ticket) {
        switchTicket(ticket.getRefundTicket());
    }

    private class DataHandler extends Handler {

        HandlerCallback callback;

        public DataHandler(HandlerCallback callback) {
            this.callback = callback;
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            Object obj = msg.obj;
            if (obj != null
                    && obj instanceof Ticket) {
                if (this.callback != null) {
                    this.callback.callback((Ticket) obj);
                }
            } else if (obj != null
                    && obj instanceof List<?>) {
                if (this.callback != null) {
                    this.callback.callback((List<Ticket>) obj);
                }
            }
        }
    }
}
