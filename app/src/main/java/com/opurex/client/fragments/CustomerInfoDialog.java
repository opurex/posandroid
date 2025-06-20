package com.opurex.client.fragments;

import android.app.*;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import androidx.annotation.Nullable;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;

import com.opurex.client.OpurexPOS;
import com.opurex.client.data.Data;
import com.opurex.client.models.Receipt;
import com.opurex.client.models.LocalTicket;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOError;
import java.lang.ref.WeakReference;
import java.util.*;

import com.opurex.client.utils.Error;
import com.opurex.client.R;
import com.opurex.client.models.Customer;
import com.opurex.client.models.Ticket;
import com.opurex.client.sync.ServerLoader;
import com.opurex.client.activities.TrackedActivity;
import com.opurex.client.widgets.CustomerTicketHistoryAdapter;
import com.opurex.client.widgets.ProgressPopup;

import static android.content.Context.INPUT_METHOD_SERVICE;

public class CustomerInfoDialog extends DialogFragment
        implements View.OnClickListener {

    public static final String TAG = CustomerSelectDialog.class.getSimpleName();
    private static final String EDITABLE_ARG = "EDITABLE_ARG";
    private static final String CUSTOMER_ARG = "CUSTOMER_ARG";
    private static final int DATAHANDLER_CUSTOMER = 1;
    private static final int DATAHANDLER_HISTORY = 2;

    // Data
    private Context mCtx;
    private CustomerListener mCustomerListener;
    private TrackedActivity mParentActivity;
    private Customer mNewCustomer;
    private boolean mbEditable;
    private boolean mbShowHistory;
    private Customer mCustomer;
    private List<Ticket> mHistoryData;
    private CustomerTicketHistoryAdapter mAdapter;
    // View
    private EditText mName;
    private EditText mZipCode;
    private EditText mPhone1;
    private EditText mMail;
    private EditText mNote;
    private ProgressPopup mPopup;
    private TextView mTicketListEmpty;
    private ProgressBar mSpinningWheel;
    private TicketListener mTicketListener;

    public void looseKeyboardFocus() {
        InputMethodManager inputMethodManager = (InputMethodManager) getActivity().getSystemService(INPUT_METHOD_SERVICE);
        IBinder token = getView().getWindowToken();
        inputMethodManager.hideSoftInputFromWindow(token, InputMethodManager.HIDE_NOT_ALWAYS);
    }

    public interface CustomerListener {
        void onCustomerCreated(Customer customer);
    }

    public interface TicketListener {
        void onTicketRefund(Ticket ticket);
    }

    public static CustomerInfoDialog newInstance(boolean editable, @Nullable Customer c) {
        Bundle args = new Bundle();
        args.putBoolean(EDITABLE_ARG, editable);
        args.putSerializable(CUSTOMER_ARG, c);
        CustomerInfoDialog dial = new CustomerInfoDialog();
        dial.setArguments(args);
        return dial;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mCtx = getActivity();
        mCustomer = (Customer) getArguments().getSerializable(CUSTOMER_ARG);
        mbEditable = getArguments().getBoolean(EDITABLE_ARG);
        mHistoryData = new ArrayList<>();
        mbShowHistory = (mCustomer != null);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View layout = inflater.inflate(R.layout.customer_info, null);

        //Remove the picture and capture button
        //Not implemented in 6.0
        layout.findViewById(R.id.customer_image_block).setVisibility(View.GONE);

        mName = (EditText) layout.findViewById(R.id.name);
        mZipCode = (EditText) layout.findViewById(R.id.zip_code);
        mPhone1 = (EditText) layout.findViewById(R.id.phone);
        mMail = (EditText) layout.findViewById(R.id.email);
        mNote = (EditText) layout.findViewById(R.id.note);
        mSpinningWheel = (ProgressBar) layout.findViewById(R.id.history_progress_bar);
        Button positive = (Button) layout.findViewById(R.id.btn_positive);
        Button negative = (Button) layout.findViewById(R.id.btn_negative);
        Button capture = (Button) layout.findViewById(R.id.btn_capture);
        ListView ticketList = (ListView) layout.findViewById(R.id.customer_ticket_history);

        mName.setEnabled(mbEditable);
        mZipCode.setEnabled(mbEditable);
        mPhone1.setEnabled(mbEditable);
        mMail.setEnabled(mbEditable);
        mNote.setEnabled(mbEditable);

        positive.setOnClickListener(this);
        negative.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getDialog().dismiss();
            }
        });
        capture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //TODO: Capture picture
            }
        });

        if (mCustomer != null) {
            mName.setText(mCustomer.getFirstName());
            mZipCode.setText(mCustomer.getZipCode());
            mPhone1.setText(mCustomer.getPhone1());
            mMail.setText(mCustomer.getMail());
            mNote.setText(mCustomer.getNote());
        }

        if (!mbEditable) {
            RelativeLayout.LayoutParams params =
                    (RelativeLayout.LayoutParams) positive.getLayoutParams();
            params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
            positive.setText(R.string.ok);
            positive.setLayoutParams(params);
            negative.setVisibility(View.GONE);
        }

        if (!mbShowHistory) {
            layout.findViewById(R.id.ticket_history_label_grp).setVisibility(View.GONE);
            ticketList.setVisibility(View.GONE);
        } else {
            addLocalTicketToHistoryData();
            mAdapter = new CustomerTicketHistoryAdapter(mCtx, mHistoryData);
            mTicketListEmpty = new TextView(mCtx);
            mTicketListEmpty.setText(R.string.customerinfo_history_loading);
            mTicketListEmpty.setLayoutParams(ticketList.getLayoutParams());
            ((RelativeLayout) ticketList.getParent()).addView(mTicketListEmpty);
            ticketList.setAdapter(mAdapter);
            ticketList.setOnItemClickListener(new onItemClick());
            ticketList.setEmptyView(mTicketListEmpty);

            // Fetch TicketList content
            ServerLoader loader = new ServerLoader(mCtx);
            loader.asyncRead(new DataHandler(CustomerInfoDialog.this),
                    "TicketsAPI", "search", "customerId", mCustomer.getId());
        }

        // show soft keyboard
        mName.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    getDialog().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
                    mName.setInputType(mName.getInputType() | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
                }
            }
        });
        if (mbEditable) {
            mName.requestFocus();
        }

        return layout;
    }

    /**
     * recursive function to get a ascendant order of Ticket.
     * @param it
     */
    private void addATicketToHistoryData(Iterator<Receipt> it) {
        if (it.hasNext()) {
            Ticket ticket = it.next().getTicket();
            addATicketToHistoryData(it);
            if (mCustomer.equals(ticket.getCustomer())) {
                mHistoryData.add(ticket);
            }
        }
    }

    private void addLocalTicketToHistoryData() {
        List<Receipt> list = Data.Receipt.getReceipts(OpurexPOS.getAppContext());
        addATicketToHistoryData(list.iterator());
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dial = super.onCreateDialog(savedInstanceState);
        dial.requestWindowFeature(Window.FEATURE_NO_TITLE);
        if (mbEditable) {
            dial.getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL, WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL);
            dial.getWindow().setFlags(WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH, WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH);
        }
        return dial;
    }

    @Override
    public void onStart() {
        super.onStart();
        int dialogWidth = (int) getResources().getDimension(R.dimen.customerInfoWidth);
        int dialogHeight = WindowManager.LayoutParams.WRAP_CONTENT;

        if (mbShowHistory) {
            dialogWidth += getResources().getDimension(R.dimen.customerInfoHistoryWidth);
        }
        getDialog().getWindow().setLayout(dialogWidth, dialogHeight);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mParentActivity = (TrackedActivity) activity;
        } catch (ClassCastException e) {
            throw new RuntimeException(TAG + "parent activity must extend TrackedActivity");
        }
    }

    public void setDialogCustomerListener(CustomerListener listener) {
        mCustomerListener = listener;
    }

    public void setDialogTicketListener(TicketListener listener) {
        mTicketListener = listener;
    }

    public void show(FragmentManager manager) {
        show(manager, TAG);
    }

    @Override
    public void onClick(View v) {
        //TODO: Handle customer edition
        if (!mbEditable) {
            getDialog().dismiss();
            return;
        }
        String firstNameStr = mName.getText().toString();
        String lastNameStr = mName.getText().toString();
        String address1Str = "";
        String address2Str = "";
        String zipCodeStr = mZipCode.getText().toString();
        String cityStr = "";
        String departmentStr = "";
        String countryStr = "";
        String phone1Str = mPhone1.getText().toString();
        String phone2Str = "";
        String mailStr = mMail.getText().toString();
        String faxStr = "";
        String noteStr = mNote.getText().toString();
        if (lastNameStr.equals("") || firstNameStr.equals("")) {
            Toast.makeText(mCtx, getString(R.string.emptyField), Toast.LENGTH_SHORT).show();
        } else if (!mailStr.equals("") && !isEmailValid(mailStr)) {
            Toast.makeText(mCtx, getString(R.string.badMail), Toast.LENGTH_SHORT).show();
        } else if (!phone1Str.equals("") && !isPhoneValid(phone1Str)) {
            Toast.makeText(mCtx, getString(R.string.badPhone), Toast.LENGTH_SHORT).show();
        } else {
            //noinspection UnnecessaryLocalVariable
            String dispName = lastNameStr;
            Customer c = new Customer(null, dispName, "",
                    firstNameStr, lastNameStr, address1Str,
                    address2Str, zipCodeStr, cityStr, departmentStr,
                    countryStr, mailStr, phone1Str, phone2Str, faxStr,
                    0.0, 0.0, "0", noteStr);
            storeLocalCustomer(c);
        }
    }

    private boolean isEmailValid(CharSequence email) {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    private boolean isPhoneValid(CharSequence phone) {
        return android.util.Patterns.PHONE.matcher(phone).matches();
    }

    private void storeLocalCustomer(Customer c) {
        // Generates local temp unique id
        c.setId("new customer:" + UUID.randomUUID().toString());
        Data.Customer.addCreatedCustomer(c);
        try {
            Data.Customer.save();
        } catch (IOError ioe) {
            Log.w(TAG, "Unable to save customers");
            Error.showError(getString(R.string.err_save_local_customer), mParentActivity);
        }
        if (mCustomerListener != null) {
            mCustomerListener.onCustomerCreated(c);
        }
        getDialog().dismiss();
    }

    /**
     * Uploads client to server
     * Called in sync mode
     *
     * @param c is the customer to upload
     */
    private void uploadCustomer(Customer c) {
        try {
            ServerLoader loader = new ServerLoader(mCtx);
            loader.asyncWrite(new DataHandler(this), "CustomersAPI", "save",
                    "customer", c.toJSON().toString());
            mPopup = new ProgressPopup(mCtx);
            mPopup.setIndeterminate(true);
            mPopup.setMessage(getString(R.string.saving_customer_message));
            mPopup.show();
            mNewCustomer = c;
        } catch (JSONException e) {
            Log.e(TAG, "Unable to json new customer", e);
            Error.showError(R.string.err_save_online_customer, mParentActivity);
        }
    }

    private void parseCustomer(JSONObject resp) {
        try {
            JSONObject o = resp.getJSONObject("content");
            JSONArray ids = o.getJSONArray("saved");
            String id = ids.getString(0);
            mNewCustomer.setId(id);
            // Update local customer list
            Data.Customer.customers.add(mNewCustomer);
            try {
                Data.Customer.save();
            } catch (IOError ioe) {
                Log.e(TAG, "Unable to save customers");
                Error.showError(R.string.err_save_local_customer, mParentActivity);
            }
            if (mCustomerListener != null) {
                mCustomerListener.onCustomerCreated(mNewCustomer);
            }
            mNewCustomer = null;
            getDialog().dismiss();
        } catch (JSONException e) {
            Log.e(TAG, "Error while parsing customer result", e);
            Error.showError(R.string.err_save_local_customer, mParentActivity);
        }
    }

    /**
     * Called when fetching history content
     *
     * @param result is a JSON array of Tickets
     */
    private void parseHistory(JSONObject result) {
        try {
            JSONArray array = result.getJSONArray("content");
            int length = array.length();
            for (int i = 0; i < length; ++i) {
                JSONObject o = array.getJSONObject(i);
                LocalTicket ticket = LocalTicket.fromJSON(mCtx, o);
                mHistoryData.add(ticket);
            }
            mParentActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mAdapter.notifyDataSetChanged();
                    mTicketListEmpty.setText(R.string.customerinfo_history_empty);
                    mSpinningWheel.setVisibility(View.GONE);
                }
            });
        } catch (JSONException e) {
            Log.e(TAG, "Error parsing history", e);
            Error.showError(R.string.err_search_customer_history, mParentActivity);
        }
    }

    private static class DataHandler extends Handler {
        WeakReference<CustomerInfoDialog> mSelfRef;

        public DataHandler(CustomerInfoDialog self) {
            mSelfRef = new WeakReference<>(self);
        }

        @Override
        public void handleMessage(Message msg) {
            CustomerInfoDialog self = mSelfRef.get();

            if (self == null) return;
            if (self.mPopup != null) {
                self.mPopup.dismiss();
                self.mPopup = null;
            }

            switch (msg.what) {
                case ServerLoader.OK:
                    // Parse content
                    String content = (String) msg.obj;
                    try {
                        JSONObject result = new JSONObject(content);
                        String status = result.getString("status");
                        if (!status.equals("ok")) {
                            JSONObject err = result.getJSONObject("content");
                            String error = err.getString("code");
                            Log.i(TAG, "Server error " + error);
                            showError(self, msg.arg1);
                        } else {
                            parseContent(self, msg.arg1, result);
                        }
                    } catch (JSONException e) {
                        Log.w(TAG, "Json error: " + content, e);
                        Error.showError(R.string.err_json_read, self.mParentActivity);
                    }
                    break;
                case ServerLoader.ERR:
                    Log.e(TAG, "URLTextGetter error", (Exception) msg.obj);
                    Error.showError(R.string.err_server_error, self.mParentActivity);
                    break;
            }
        }

        private static void showError(CustomerInfoDialog self, int who) {
            switch (who) {
                case DATAHANDLER_CUSTOMER:
                    Error.showError(R.string.err_save_online_customer, self.mParentActivity);
                    break;
                case DATAHANDLER_HISTORY:
                    Error.showError(R.string.err_search_customer_history, self.mParentActivity);
                    break;
            }
        }

        private static void parseContent(CustomerInfoDialog self, int who, JSONObject result) {
            switch (who) {
                case DATAHANDLER_CUSTOMER:
                    self.parseCustomer(result);
                    break;
                case DATAHANDLER_HISTORY:
                    self.parseHistory(result);
                    break;
            }
        }
    }

    private class onItemClick implements AdapterView.OnItemClickListener {
        @Override
        public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
            final Ticket ticket = (Ticket) adapterView.getItemAtPosition(i);
            AlertDialog.Builder b = new AlertDialog.Builder(getActivity());
            b.setTitle(ticket.getLabel());
            b.setMessage(OpurexPOS.getStringResource(R.string.ticket_ask_refund));
            b.setPositiveButton(R.string.refund, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    if (mTicketListener != null) {
                        mTicketListener.onTicketRefund(ticket);
                        CustomerInfoDialog.this.dismiss();
                    }
                }
            });
            b.setNegativeButton(R.string.cancel, null);
            b.show();
        }
    }
}
