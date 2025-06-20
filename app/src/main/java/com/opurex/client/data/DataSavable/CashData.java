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
package com.opurex.client.data.DataSavable;

import android.content.Context;
import com.opurex.client.models.Cash;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class CashData extends AbstractJsonDataSavable {

    private static final String FILENAME = "cash.json";

    private Cash currentCash;
    public Boolean dirty = false;

    public Cash currentCash(Context ctx) {
        if (this.currentCash == null) {
            this.loadNoMatterWhat(ctx);
        }
        return currentCash;
    }

    public void setCash(Cash c) {
        currentCash = c;
    }

    @Override
    protected String getFileName() {
        return FILENAME;
    }

    @Override
    protected List<Object> getObjectList() {
        List<Object> result = new ArrayList<>();
        result.add(currentCash);
        result.add(dirty);
        return result;
    }

    @Override
    protected List<Type> getClassList() {
        List<Type> result = new ArrayList<>();
        result.add(Cash.class);
        result.add(Boolean.class);
        return result;
    }

    @Override
    protected int getNumberOfObjects() {
        return 2;
    }

    @Override
    protected void recoverObjects(List<Object> objs) {
        currentCash = (Cash) objs.get(0);
        dirty = (Boolean) objs.get(1);
    }

    /** Delete current cash */
    public void clear(Context ctx) {
        currentCash = null;
        dirty = false;
        ctx.deleteFile(FILENAME);
    }

}
