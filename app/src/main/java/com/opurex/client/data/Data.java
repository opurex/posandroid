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
package com.opurex.client.data;

import android.content.Context;
import android.util.Log;

import com.opurex.client.data.DataSavable.*;
import com.opurex.client.data.DataSavable.interfaces.DataSavable;
import com.opurex.client.utils.exception.DataCorruptedException;

import java.io.IOError;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility class to check and load local data
 */
public class Data {

    private static final String LOG_TAG = "Opurex/Data";

    public static CurrencyData Currency = new CurrencyData();
    public static CatalogData Catalog = new CatalogData();
    public static CashData Cash = new CashData();
    public static CashRegisterData CashRegister = new CashRegisterData();
    public static CompositionData Composition = new CompositionData();
    public static CrashData Crash = new CrashData();
    public static CustomerData Customer = new CustomerData();
    public static DiscountData Discount = new DiscountData();
    public static LoginData Login = new LoginData();
    public static PaymentModeData PaymentMode = new PaymentModeData();
    public static PlaceData Place = new PlaceData();
    public static ReceiptData Receipt = new ReceiptData();
    public static SessionData Session = new SessionData();
    public static TariffAreaData TariffArea = new TariffAreaData();
    public static UserData User = new UserData();
    public static StockData Stock = new StockData();
    public static TaxData Tax = new TaxData();
    public static TicketIdData TicketId = new TicketIdData();

    private static boolean loadedSuccesfully = true;

    private static List<DataSavable> getDataToLoad() {
        ArrayList<DataSavable> list = new ArrayList<>();
        list.add(Currency);
        list.add(Catalog);
        list.add(Cash);
        list.add(CashRegister);
        list.add(Composition);
        list.add(Crash);
        list.add(Customer);
        list.add(Discount);
        list.add(Login);
        list.add(PaymentMode);
        list.add(Place);
        list.add(Receipt);
        list.add(Session);
        list.add(Stock);
        list.add(TariffArea);
        list.add(User);
        list.add(TicketId);
        list.add(Tax);
        return list;
    }

    public static boolean loadAll(Context ctx) {
        Data.loadedSuccesfully = true;
        List<DataSavable> list = getDataToLoad();
        for (DataSavable data: list) {
            try {
                data.load();
                Log.i(LOG_TAG, "Correctly loaded: " + data.getClass().getName());
            } catch (DataCorruptedException e) {
                if (data.onLoadingFailed(e)) {
                    Data.loadedSuccesfully = false;
                }
                Log.d(LOG_TAG, "Warning: " + data.getClass().getName());
                Log.d(LOG_TAG, e.inspectError());
            } catch (IOError e) {
                if (data.onLoadingError(e)) {
                    Data.loadedSuccesfully = false;
                }
                Log.e(LOG_TAG, "Fatal IO Error: " + data.getClass().getName(), e);
            }
        }
        return Data.loadedSuccesfully;
    }

    public static boolean loadingSuccessfull() {
        return Data.loadedSuccesfully;
    }

    public static void dataUpdated() {
        Data.loadedSuccesfully = true;
    }

    public static boolean dataLoaded(Context ctx) {
        return Data.User.users(ctx) != null && Data.User.users(ctx).size() > 0
                && Data.Currency.currencies(ctx) != null
                && Data.Currency.currencies(ctx).size() > 0
                && Data.Catalog.catalog(ctx) != null
                && Data.Catalog.catalog(ctx).getRootCategories().size() > 0
                && Data.Catalog.catalog(ctx).getProductCount() > 0
                && Data.PaymentMode.paymentModes(ctx) != null
                && Data.PaymentMode.paymentModes(ctx).size() > 0
                && Data.Cash.currentCash(ctx) != null;
    }

    public static boolean hasCashOpened(Context ctx) {
        return (Data.Receipt.getReceipts(ctx).size() > 0)
                || Data.Cash.dirty;
    }

    public static boolean hasArchive(Context ctx) {
        return CashArchive.hasArchives(ctx);
    }

    public static boolean hasLocalData(Context ctx) {
        return Data.hasCashOpened(ctx) || Data.hasArchive(ctx);
    }

    public static void removeLocalData(Context ctx) {
        Data.Receipt.clear(ctx);
        Data.Cash.clear(ctx);
        CashArchive.clear(ctx);
    }

    public static void export(String dir) {
        List<DataSavable> list = getDataToLoad();

        for (DataSavable data: list) {
            data.export(dir);
        }
    }
}
