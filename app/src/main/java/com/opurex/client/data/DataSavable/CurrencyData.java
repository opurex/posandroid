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
import com.google.gson.reflect.TypeToken;
import com.opurex.client.models.Currency;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class CurrencyData extends AbstractJsonDataSavable {

    private static final String FILENAME = "currency.json";

    public List<Currency> currencies = new ArrayList<Currency>();

    public void set(List<Currency> c) {
        currencies = c;
    }

    public List<Currency> currencies(Context ctx) {
        if (currencies == null) {
            this.loadNoMatterWhat(ctx);
        }
        return currencies;
    }

    public Currency getMain(Context ctx) {
        List<Currency> currencies = this.currencies(ctx);
        Currency main = null;
        for (Currency c : currencies) {
            if (c.isMain()) { return c; }
        }
        return null;
    }

    @Override
    protected String getFileName() {
        return CurrencyData.FILENAME;
    }

    @Override
    protected List<Object> getObjectList() {
        List<Object> result = new ArrayList<>();
        result.add(currencies);
        return result;
    }

    @Override
    protected List<Type> getClassList() {
        List<Type> result = new ArrayList<>();
        result.add(new TypeToken<List<Currency>>(){}.getType());
        return result;
    }

    @Override
    protected int getNumberOfObjects() {
        return 1;
    }

    @Override
    protected void recoverObjects(List<Object> objs) {
        currencies = (List<Currency>) objs.get(0);
    }

}
