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
import com.opurex.client.interfaces.TicketLineEditListener;
import com.opurex.client.models.TicketLine;

import android.content.Context;
import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

public class TicketLineItem extends LinearLayout {

    private TicketLine line;
    private TicketLineEditListener listener;
    private boolean editable;
    private boolean scaled;
    private TextView label;
    private TextView quantity;
    /**
     * Total VAT price label
     */
    private TextView price;
    private ItemImage productImage;


    public TicketLineItem(Context context, TicketLine line,
                          boolean editable) {
        super(context);
        LayoutInflater.from(context).inflate(R.layout.ticket_item_line,
                this, true);
        this.editable = editable;
        this.label = (TextView) this.findViewById(R.id.product_label);
        this.quantity = (TextView) this.findViewById(R.id.product_quantity);
        this.price = (TextView) this.findViewById(R.id.product_price);
        this.productImage = (ItemImage) this.findViewById(R.id.product_img);
        View add = this.findViewById(R.id.product_add);
        add.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                add();
            }
        });
        View remove = this.findViewById(R.id.product_subtract);
        remove.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                remove();
            }
        });
        View edit = this.findViewById(R.id.product_edit);
        edit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                edit();
            }
        });
        this.price.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                edit();
            }
        });
        View scale = this.findViewById(R.id.product_scale);
        scale.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                scale();
            }
        });
        View delete = this.findViewById(R.id.product_delete);
        delete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                delete();
            }
        });
        // Must be called here because it won't be called in reuse
        if (!this.editable) updateEditable();
        this.reuse(line, editable);
    }

    private void updateScaleMode() {
        if (this.scaled) {
            // Weight
            this.quantity.setText(String.valueOf(this.line.getQuantity()));
            this.findViewById(R.id.product_edit).setVisibility(GONE);
            this.findViewById(R.id.product_scale).setVisibility(VISIBLE);
        } else {
            // Units
            this.quantity.setText(String.valueOf((int) this.line.getQuantity()));
            this.findViewById(R.id.product_edit).setVisibility(VISIBLE);
            this.findViewById(R.id.product_scale).setVisibility(GONE);
        }
    }

    public void reuse(TicketLine line, boolean editable) {
        this.scaled = line.getProduct().isScaled();
        this.line = line;
        this.label.setText(this.line.getProduct().getLabel());
        this.price.setText(String.format("%.2f €", this.line.getTotalDiscPIncTax()));
        this.editable = editable;
        this.updateEditable();
        this.updateScaleMode();
        // Show when line price has been edited
        if (this.line.isCustom() || this.line.getProduct().isDiscountRateEnabled()) {
            this.price.setPaintFlags(this.price.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
        } else {
            this.price.setPaintFlags(this.price.getPaintFlags() & ~Paint.UNDERLINE_TEXT_FLAG);
        }
        this.productImage.setVisibility(VISIBLE);
        this.productImage.setItem(line.getProduct());
    }

    public void setEditListener(TicketLineEditListener l) {
        this.listener = l;
    }

    public TicketLine getLine() {
        return this.line;
    }

    public void add() {
        if (this.listener != null) {
            this.listener.addQty(this.line);
        }
    }

    public void remove() {
        if (this.listener != null) {
            this.listener.remQty(this.line);
        }
    }

    /**
     * Modifies the weight of the product
     */
    public void scale() {
        if (this.listener != null) {
            this.listener.mdfyQty(this.line);
        }
    }

    public void edit() {
        if (this.listener != null) {
            this.listener.editProduct(this.line);
        }
    }

    public void delete() {
        if (this.listener != null) {
            this.listener.delete(this.line);
        }
    }

    private void updateEditable() {
        android.view.ViewGroup editGroup = (android.view.ViewGroup) this.findViewById(R.id.product_edit_group);
        for (int i = 0; i < editGroup.getChildCount(); i++) {
            editGroup.getChildAt(i).setEnabled(this.editable);
        }
        editGroup.findViewById(R.id.product_subtract).setEnabled(this.editable && !this.scaled);
        editGroup.findViewById(R.id.product_add).setEnabled(this.editable && !this.scaled);
        this.price.setClickable(this.editable);
    }
}
