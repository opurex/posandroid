package com.opurex.client.drivers.mpop;

import java.util.ArrayList;

/**
 * Created by svirch_n on 21/12/15.
 */
public class MPopEntries extends ArrayList<MPopEntry> {

    public void add(String name, String value) {
        this.add(new MPopEntry(name, value));
    }

    public CharSequence[] getEntries() {
        CharSequence[] result = new CharSequence[size()];
        for (int i = 0; i < size(); i++) {
            result[i] = this.get(i).name.substring("BT:".length());
        }
        return result;
    }

    public CharSequence[] getValues() {
        CharSequence[] result = new CharSequence[size()];
        for (int i = 0; i < size(); i++) {
            result[i] = "BT:" + this.get(i).value;
        }
        return result;
    }
}
