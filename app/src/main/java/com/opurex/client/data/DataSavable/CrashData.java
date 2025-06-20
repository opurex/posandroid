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
import com.opurex.client.utils.exception.DataCorruptedException;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class CrashData extends AbstractJsonDataSavable {

    private static final String FILENAME = "crash.json";

    public Boolean dirty = new Boolean(false);
    public String error;

    public String customLoad(Context ctx) {
        this.loadNoMatterWhat(ctx);
        // loadNoMatterWhat calls load() which calls recoverObjects.
        // this.error is then set and can be returned
        return error;
    }

    public void save(String error, Context ctx) throws DataCorruptedException {
        this.error = error;
        this.save();
    }

    @Override
    protected String getFileName() {
        return CrashData.FILENAME;
    }

    @Override
    protected List<Object> getObjectList() {
        List<Object> result = new ArrayList<>();
        result.add(dirty);
        result.add(error);
        return result;
    }

    @Override
    protected List<Type> getClassList() {
        List<Type> result = new ArrayList<>();
        result.add(Boolean.class);
        result.add(String.class);
        return result;
    }

    @Override
    protected int getNumberOfObjects() {
        return 2;
    }

    @Override
    protected void recoverObjects(List<Object> objs) {
        dirty = (Boolean) objs.get(0);
        error = (String) objs.get(1);
    }
}
