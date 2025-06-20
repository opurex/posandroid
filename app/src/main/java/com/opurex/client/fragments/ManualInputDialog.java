package com.opurex.client.fragments;

import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import java.util.ArrayList;
import java.util.List;

import com.opurex.client.BuildConfig;
import com.opurex.client.R;
import com.opurex.client.data.Data;
import com.opurex.client.data.ImagesData;
import com.opurex.client.models.Catalog;
import com.opurex.client.models.Product;
import com.opurex.client.models.Tax;
import com.opurex.client.utils.CalculPrice;


/**
 * The activity that creates an instance of this dialog fragment
 * must implement MIDialogListener to get results
 */
public class ManualInputDialog extends DialogFragment {
    // TODO: Maybe do a base Tab DialogFragment ?

    public static String TAG = "ManualInputDFRAG";

    private final TextWatcher BARCODE_INPUT_TW;
    private Listener mListener;
    private Context mContext;
    private Boolean mNotFoundToast;
    private BarcodeListAdapter mMatchingItems;

    public interface Listener {
        /**
         * Called when creating a product in manual input
         *
         * @param product is the newly created product
         */
        void onMidProductCreated(Product product);

        /**
         * Called when picking an item in the list.
         *
         * @param product is the selected scanned product
         */
        void onMidProductPick(Product product);
    }

    public ManualInputDialog() {
        BARCODE_INPUT_TW = new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                ManualInputDialog.this.readBarcode(s.toString());
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        };
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NO_TITLE, android.R.style.Theme_Holo_Light_Dialog_NoActionBar_MinWidth);
        mContext = getActivity();
        mNotFoundToast = true;
        mMatchingItems = new BarcodeListAdapter();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        View layout = inflater.inflate(R.layout.manual_input, container, false);

        // Editing layout
        TabHost tabs = (TabHost) layout.findViewById(android.R.id.tabhost);

        tabs.setup();

        TabHost.TabSpec tabpage1 = tabs.newTabSpec("tab1");
        tabpage1.setContent(R.id.input_manual);
        tabpage1.setIndicator(getString(R.string.manualinput_title));

        TabHost.TabSpec tabpage2 = tabs.newTabSpec("tab2");
        tabpage2.setContent(R.id.input_barcode);
        tabpage2.setIndicator(getString(R.string.barcodeinput_title));

        tabs.addTab(tabpage1);
        tabs.addTab(tabpage2);

        TabWidget tabWidget = tabs.getTabWidget();

        tabs.getTabWidget().setDividerDrawable(R.color.popup_outer_txt);

        int nbrTab = tabWidget.getTabCount();
        if (BuildConfig.DEBUG && nbrTab != 2) {
            throw new AssertionError();
        }
        for (int j = 0; j < nbrTab; ++j) {
            View tabView = tabWidget.getChildTabViewAt(j);
            TextView tabTitle = (TextView) tabView.findViewById(android.R.id.title);
            if (tabTitle != null) {
                tabView.setBackgroundResource(R.drawable.tab_selector);
                tabTitle.setTextColor(getResources().getColor(R.color.popup_outer_txt));
                tabTitle.setTypeface(null, Typeface.BOLD);
            }
        }

        // Setting up buttons
        layout.findViewById(R.id.tab1_btn_positive).setOnClickListener(new OnProductCreatedClick(layout));

        View.OnClickListener negativeClick = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ManualInputDialog.this.getDialog().cancel();
            }
        };
        layout.findViewById(R.id.tab1_btn_negative).setOnClickListener(negativeClick);
        layout.findViewById(R.id.tab2_btn_negative).setOnClickListener(negativeClick);

        EditText input = ((EditText) layout.findViewById(R.id.tab2_barcode_input));
        input.addTextChangedListener(BARCODE_INPUT_TW);

        // Dynamic list view
        ListView MatchedListView = (ListView) layout.findViewById(R.id.tab2_scanned_products);
        MatchedListView.setAdapter(mMatchingItems);

        ((Spinner) layout.findViewById(R.id.tab1_spin_vat)).setAdapter(new TaxesListAdapter(mContext,
                android.R.layout.simple_list_item_1, Data.Tax.getTaxes()));
        return layout;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        dialog.setCanceledOnTouchOutside(true);
        return dialog;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (Listener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement MIDialogListener");
        }
    }

    private void readBarcode(String code) {
        mMatchingItems.clearItems();
        if (!code.isEmpty()) {
            Catalog cat = Data.Catalog.catalog(mContext);
            List<Product> pList = cat.getProductLikeBarcode(code);
            if (pList.size() > 0) {
                mNotFoundToast = true;
                for (Product p : pList) {
                    mMatchingItems.addItem(p);
                }
            } else if (mNotFoundToast) {
                mNotFoundToast = false;
                String text = this.getString(R.string.barcode_not_found, code);
                Toast.makeText(mContext, text, Toast.LENGTH_LONG).show();
            }
        }
        mMatchingItems.notifyDataSetChanged();
    }

    /*
     * BUTTON CLICK
     */

    private class OnProductCreatedClick implements View.OnClickListener {
        private EditText mLabel;
        private EditText mPrice;
        private Spinner mVAT;

        OnProductCreatedClick(View layout) {
            mLabel = (EditText) layout.findViewById(R.id.tab1_product_title);
            mPrice = (EditText) layout.findViewById(R.id.tab1_edit_tariff);
            mVAT = (Spinner) layout.findViewById(R.id.tab1_spin_vat);
        }

        @Override
        public void onClick(View v) {
            String label = mLabel.getText().toString().trim();
            String sPrice = mPrice.getText().toString();
            if (label.isEmpty()) {
                mLabel.setError(getString(R.string.manualinput_error_empty));
            }
            Boolean bValid;
            if ((bValid = sPrice.isEmpty()) || (bValid = sPrice.equals("."))) {
                mPrice.setError(getString(R.string.manualinput_error_number));
            }
            Tax tax = ((TaxesListAdapter)mVAT.getAdapter()).getTax(mVAT.getSelectedItemPosition());
            if (!label.isEmpty() && !bValid) {
                Double price = Double.parseDouble(sPrice);
                Product p = new Product(label,
                        CalculPrice.removeTaxe(price, tax.getRate()),
                        price,
                        tax.getId());
                ManualInputDialog.this.mListener.onMidProductCreated(p);
                ManualInputDialog.this.dismiss();
            }
        }
    }

    /*
     *  ADAPTER
     */

    private class TaxesListAdapter extends ArrayAdapter {

        private List<Tax> list;

        public TaxesListAdapter(Context context, int resource, List<Tax> objects) {
            super(context, resource, objects);
            list = objects;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            if (view == null) {
                view = View.inflate(ManualInputDialog.this.mContext, android.R.layout.simple_list_item_1, null);
            }
            ((TextView)view.findViewById(android.R.id.text1)).setText(list.get(i).getPercent());
            return view;
        }

        @Override
        public Object getItem(int position) {
            return this.list.get(position).getPercent();
        }

        public Tax getTax(int index) {
            return this.list.get(index);
        }
    }

    private class BarcodeListAdapter extends BaseAdapter {

        private List<Product> mList;

        public BarcodeListAdapter() {
            mList = new ArrayList<Product>();
        }

        public void addItem(Product p) {
            mList.add(p);
        }

        public void clearItems() {
            mList.clear();
        }

        @Override
        public int getCount() {
            return mList.size();
        }

        @Override
        public Object getItem(int position) {
            return mList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            final Product p = mList.get(position);
            if (convertView == null) {
                // Create the view
                LayoutInflater inflater = (LayoutInflater) ManualInputDialog.this.mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                convertView = inflater.inflate(R.layout.barcode_list_item, parent, false);
            }
            // Reuse the view
            Bitmap img;
            if (p.hasImage() && null != (img = ImagesData.getProductImage(p.getId()))) {
                ((ImageView) convertView.findViewById(R.id.product_img)).setImageBitmap(img);
            } else {
                ((ImageView) convertView.findViewById(R.id.product_img)).setImageResource(R.drawable.ic_placeholder_img);
            }
            TextView label = (((TextView) convertView.findViewById(R.id.product_label)));
            label.setText(p.getLabel());

            convertView.findViewById(R.id.btn_product_select).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ManualInputDialog.this.mListener.onMidProductPick(p);
                    ManualInputDialog.this.dismiss();
                }
            });
            return convertView;
        }
    }
}
