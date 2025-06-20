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

import com.opurex.client.OpurexPOS;
import com.opurex.client.models.Catalog;

import android.content.Context;

import java.util.ArrayList;
import java.util.List;

public class CatalogData extends AbstractObjectDataSavable {

    private static final String FILENAME = "catalog.data";

    private Catalog catalog;

    public Catalog catalog(Context ctx) {
        if (catalog == null) {
           this.loadNoMatterWhat(ctx);
        }
        return catalog;
    }

    public void setCatalog(Catalog c) {
        catalog = c;
    }

    @Override
    protected String getFileName() {
        return CatalogData.FILENAME;
    }

    @Override
    protected List<Object> getObjectList() {
        List<Object> result = new ArrayList<>();
        result.add(catalog);
        return result;
    }

/*
    JsonDataSavable implementation
    @Override
    protected List<Type> getClassList() {
        List<Type> result = new ArrayList<>();
        result.add(Catalog.class);
        return result;
    }*/

    @Override
    protected int getNumberOfObjects() {
        return 1;
    }

    @Override
    protected void recoverObjects(List<Object> objs) {
        this.catalog = (Catalog) objs.get(0);
    }

    @Override
    public void export(String dir) {
        OpurexPOS.Log.w("Export not implemented with catalog");
    }
}
