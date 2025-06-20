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
import com.opurex.client.models.User;

import android.content.Context;
import android.view.LayoutInflater;
import android.widget.TextView;
import android.widget.RelativeLayout;

public class UserBtnItem extends RelativeLayout {

    private User user;
    private TextView name;

    public UserBtnItem(Context context, User u) {
        super(context);
        LayoutInflater.from(context).inflate(R.layout.user_btn_item, this, true);
        this.name = (TextView) this.findViewById(R.id.user_name);

        this.reuse(u);
    }

    public void reuse(User u) {
        this.user = u;
        this.name.setText(this.user.getName());
    }

    public User getUser() {
        return this.user;
    }
}