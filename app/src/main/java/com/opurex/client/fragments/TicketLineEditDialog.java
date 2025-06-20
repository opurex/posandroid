package com.opurex.client.fragments;

import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Bundle;
import androidx.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.opurex.client.R;
import com.opurex.client.data.ImagesData;
import com.opurex.client.models.Product;
import com.opurex.client.models.TicketLine;

public class TicketLineEditDialog extends DialogFragment {
    public final static String TAG = "TicketLineDialogTAG";

    private final static String TICKETLINE_ARG = "line_arg";
    //  DATA
    private Context mContext;
    private Listener mListener;
    private TicketLine mLine;
    //  VIEWS
    private EditText mQtyTxt;
    private EditText mTariffTxt;
    private EditText mDiscountTxt;

    public interface Listener {
        void onTicketLineEdited(TicketLine line);
    }

    /** Create a new instance with a sample line. */
    public static TicketLineEditDialog newInstance(TicketLine line) {
        Bundle args = new Bundle();
        args.putSerializable(TICKETLINE_ARG, line);
        TicketLineEditDialog dial = new TicketLineEditDialog();
        dial.setArguments(args);
        return dial;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = getActivity();
        mLine = (TicketLine) getArguments().getSerializable(TICKETLINE_ARG);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View layout = inflater.inflate(R.layout.ticket_item_edit, null);
        mQtyTxt = (EditText) layout.findViewById(R.id.quantity_edit);
        mTariffTxt = (EditText) layout.findViewById(R.id.tariff_edit);
        mDiscountTxt = (EditText) layout.findViewById(R.id.reduction_edit);
        Button mPositiveBtn = (Button) layout.findViewById(R.id.btn_positive);

        mPositiveBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TicketLineEditDialog.this.setCustomQuantity();
                TicketLineEditDialog.this.setCustomPrice();
                TicketLineEditDialog.this.setCustomReduction();
                if (mListener != null) mListener.onTicketLineEdited(mLine);
                TicketLineEditDialog.this.getDialog().dismiss();
            }
        });
        layout.findViewById(R.id.btn_negative).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TicketLineEditDialog.this.getDialog().cancel();
            }
        });

        // Dialog Layout Modification
        ViewGroup table = (ViewGroup) layout.findViewById(R.id.table_characteristics);
        final int childCount = table.getChildCount();
        // TODO: Dynamically load dropdown menu content
        for (int i = 0; i < childCount; ++i) {
            final View row = table.getChildAt(i);
            String str1 = getString(R.string.ticketitem_edit_characteristic_label, i + 1);
            String str2 = getString(R.string.ticketitem_edit_characteristic_label, i + 4);
            ((TextView) row.findViewById(R.id.row_characteristic_odd_label)).setText(str1);
            ((TextView) row.findViewById(R.id.row_characteristic_even_label)).setText(str2);
        }

        // Adding Product info in layout
        Product p = mLine.getProduct();
        Bitmap img;
        if (p.hasImage() && null != (img = ImagesData.getProductImage(p.getId()))) {
            ((ImageView) layout.findViewById(R.id.product_img)).setImageBitmap(img);
        }
        ((TextView) layout.findViewById(R.id.product_label)).setText(p.getLabel());
        mTariffTxt.setText(Double.toString(mLine.getProductIncTax()));
        mDiscountTxt.setText(Double.toString(mLine.getDiscountRate() * 100));

        return layout;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        dialog.setCanceledOnTouchOutside(true);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        return dialog;
    }

    public void setDialogListener(Listener listener) {
        mListener = listener;
    }

    private boolean setCustomQuantity() {
        String qtyString = mQtyTxt.getText().toString();
        if (!qtyString.trim().equals("")) {
            double qty = Double.valueOf(qtyString);
            mLine.setQuantity(qty);
            return true;
        }
        return false;
    }

    private boolean setCustomPrice() {
        String priceString = mTariffTxt.getText().toString();
        if (!priceString.trim().equals("")) {
            double price = Double.valueOf(priceString);
            if (mLine.hasCustomPrice() || price != mLine.getProductIncTax()) {
                mLine.setCustomPrice(price);
                return true;
            }
        }
        return false;
    }

    private boolean setCustomReduction() {
        String discountString = mDiscountTxt.getText().toString();
        if (!discountString.trim().equals("")) {
            double discountRate = Double.valueOf(discountString) / 100;
            if (mLine.hasCustomDiscount() || discountRate != mLine.getDiscountRate()) {
                mLine.setCustomDiscount(discountRate);
                return true;
            }
        }
        return false;
    }
}
