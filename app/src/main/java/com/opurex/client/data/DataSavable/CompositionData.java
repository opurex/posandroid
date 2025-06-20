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
import com.opurex.client.models.Composition;
import com.opurex.client.models.Product;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CompositionData extends AbstractJsonDataSavable {

    private static final String FILENAME = "compositions.json";

    public Map<String, Composition> compositions;

    public boolean isComposition(Product p) {
        return compositions.containsKey(p.getId());
    }
    public Composition getComposition(String id) {
        return compositions.get(id);
    }


    @Override
    protected String getFileName() {
        return CompositionData.FILENAME;
    }

    @Override
    protected List<Object> getObjectList() {
        List<Object> result = new ArrayList<>();
        result.add(compositions);
        return result;
    }

    @Override
    protected List<Type> getClassList() {
        List<Type> result = new ArrayList<>();
        result.add(new TypeToken<Map<String, Composition>>(){}.getType());
        return result;
    }

    @Override
    protected int getNumberOfObjects() {
        return 1;
    }

    @Override
    protected void recoverObjects(List<Object> objs) {
        compositions = (Map<String, Composition>) objs.get(0);
    }
}
