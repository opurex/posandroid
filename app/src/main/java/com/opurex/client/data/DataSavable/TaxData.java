package com.opurex.client.data.DataSavable;

import com.google.gson.reflect.TypeToken;
import com.opurex.client.models.Tax;
import com.opurex.client.utils.exception.DataCorruptedException;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by nsvir on 27/08/15.
 * n.svirchevsky@gmail.com
 */
public class TaxData extends AbstractJsonDataSavable {

    private final static String FILENAME = "taxes.json";

    public List<Tax> taxes = new ArrayList<>();

    @Override
    protected String getFileName() {
        return FILENAME;
    }

    @Override
    protected List<Object> getObjectList() {
        List<Object> result = new ArrayList<>();
        result.add(taxes);
        return result;
    }

    @Override
    protected List<Type> getClassList() {
        List<Type> result = new ArrayList<>();
        result.add(new TypeToken<List<Tax>>(){}.getType());
        return result;
    }

    @Override
    protected int getNumberOfObjects() {
        return 1;
    }

    @Override
    protected void recoverObjects(List<Object> objs) throws DataCorruptedException {
        this.taxes = (List<Tax>) objs.get(0);
        if (this.taxes == null) {
            throw new DataCorruptedException(null, DataCorruptedException.Action.LOADING);
        }
    }

    public void setTaxes(List<Tax> taxes) {
        this.taxes = taxes;
    }

    public List<Tax> getTaxes() {
        return this.taxes;
    }
}
