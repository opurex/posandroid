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
import com.opurex.client.models.CashRegister;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class CashRegisterData extends AbstractJsonDataSavable {

    private static final String FILENAME = "cashreg.json";

    private CashRegister cashRegister;

    public CashRegister current(Context ctx) {
        if (cashRegister == null) {
            this.loadNoMatterWhat(ctx);
        }
        return cashRegister;
    }

    public void set(CashRegister c) {
        cashRegister = c;
    }

    /**
     * Delete cashRegister cash
     */
    public void clear(Context ctx) {
        cashRegister = null;
        ctx.deleteFile(FILENAME);
    }

    @Override
    protected String getFileName() {
        return CashRegisterData.FILENAME;
    }

    @Override
    protected List<Object> getObjectList() {
        List<Object> result = new ArrayList<>();
        result.add(cashRegister);
        return result;
    }

    @Override
    protected List<Type> getClassList() {
        List<Type> result = new ArrayList<>();
        result.add(CashRegister.class);
        return result;
    }

    @Override
    protected int getNumberOfObjects() {
        return 1;
    }

    @Override
    protected void recoverObjects(List<Object> objs) {
        this.cashRegister = (CashRegister) objs.get(0);
    }
}
