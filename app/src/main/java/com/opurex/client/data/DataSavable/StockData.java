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

import com.google.gson.reflect.TypeToken;
import com.opurex.client.models.Stock;

import android.content.Context;
import com.opurex.client.utils.file.InternalFile;

import java.io.IOException;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class StockData extends AbstractJsonDataSavable {

    private static final String FILENAME = "stock.data";
    private static final String LOC_FILENAME = "location.json";

    public Map<String, Stock> stocks;

    @Override
    protected String getFileName() {
        return StockData.FILENAME;
    }

    @Override
    protected List<Object> getObjectList() {
        List<Object> result = new ArrayList<>();
        result.add(stocks);
        return result;
    }

    @Override
    protected List<Type> getClassList() {
        List<Type> result = new ArrayList<>();
        result.add(new TypeToken<Map<String, Stock>>(){}.getType());
        return result;
    }

    @Override
    protected int getNumberOfObjects() {
        return 1;
    }

    @Override
    protected void recoverObjects(List<Object> objs) {
        this.stocks = (Map<String, Stock>) objs.get(0);
    }

    public static boolean saveLocation(Context ctx, String location, String id)
        throws IOException {
        FileOutputStream fos = new FileOutputStream(new InternalFile(AbstractJsonDataSavable.getStaticDirectory(), LOC_FILENAME));
        ObjectOutputStream oos = new ObjectOutputStream(fos);
        oos.writeObject(location);
        oos.writeObject(id);
        oos.close();
        return true;
    }

    /** Get location id. Return null if not found or not requested location. */
    public static String getLocationId(Context ctx, String location)
        throws IOException {
        FileInputStream fis = new FileInputStream(new InternalFile(AbstractJsonDataSavable.getStaticDirectory(), LOC_FILENAME));
        ObjectInputStream ois = new ObjectInputStream(fis);
        String id = null;
        try {
            String loc = (String) ois.readObject();
            if (loc.equals(location)) {
                id = (String) ois.readObject();
            }
        } catch (ClassNotFoundException cnfe) {
            // Should never happen
        }
        ois.close();
        return id;
    }
}
