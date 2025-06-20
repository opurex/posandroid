package com.opurex.client.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

/**
 * Created by svirch_n on 22/12/15.
 */
public class OpurexConfiguration
{
    public final static String PRINTER_MODEL = "printer_model";
    public final static String PRINTER_DRIVER = "printer_driver";
    public final static String PRINTER_ADDRESS = "printer_address";
    private static final String PRINTER_PRINT_TICKET_BY_DEFAULT = "printer_print_ticket_by_default";
    private static final String PRINTER_CONNECT_TRY = "printer_connect_try";
    final static int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);
    private static final int BITMAP_BUFFER_SIZE = maxMemory / 2;
    public static final java.lang.String MAIL_ENABLED = "mail_enabled";

    public boolean isPrinterDriver(String driver) {
        return false;
    }

    public boolean is(String category, String value) {
        return getShared(category).equals(value);
    }

    public boolean isPrinterThreadAPriority() {
        return false;
    }

    public int getBitmapBufferSize() {
        return BITMAP_BUFFER_SIZE;
    }

    public boolean scannerIsAutoScan() {
        return true;
    }

    // Those values must match with the ones from "config_printer_driver_values" in values.xml
    public static class PrinterDriver {
        public static final String NONE = "None";
        public static final String STARMPOP = "StarMPop";
        public static final String EPSON_IP = "EPSON ePOS IP";
        public static final String LKPXX = "LK-PXX";
        public static final String WOOSIM = "Woosim";
        public static final String POWAPOS = "PowaPOS";
    }

    private final SharedPreferences sharedPreferences;

    public OpurexConfiguration(Context appContext) {
        this.sharedPreferences = PreferenceManager.getDefaultSharedPreferences(appContext);
    }

    /** Get the driver of the main device. */
    public String getPrinterDriver() {
        return getShared(PRINTER_DRIVER, PrinterDriver.NONE);
    }

    /** Get the driver of a device.
     * @param deviceIndex Index. 0 is for the main device. */
    public String getPrinterDriver(int deviceIndex) {
        if (deviceIndex == 0) {
            return this.getPrinterDriver();
        } else {
            return getShared(PRINTER_DRIVER + deviceIndex, PrinterDriver.NONE);
        }
    }

    /** Get the address of the main printer. */
    public String getPrinterAddress() {
        return getShared(PRINTER_ADDRESS);
    }

    /** Get the address of a device.
     * @param deviceIndex Index. 0 is for the main device. */
    public String getPrinterAddress(int deviceIndex) {
        if (deviceIndex == 0) {
            return this.getPrinterAddress();
        } else {
            return getShared(PRINTER_ADDRESS + deviceIndex);
        }
    }

    /** Get the model of the main printer. */
    public String getPrinterModel() {
        return getShared(PRINTER_MODEL);
    }

    public String getPrinterModel(int deviceIndex) {
        if (deviceIndex == 0) {
            return this.getPrinterModel();
        } else {
            return getShared(PRINTER_MODEL + deviceIndex);
        }
    }

    public boolean getPrintTicketByDefault() {
        return getBooleanShared(PRINTER_PRINT_TICKET_BY_DEFAULT, true);
    }

    public int getPrinterConnectTry() {
        return new Integer(getShared(PRINTER_CONNECT_TRY, "0"));
    }

    public boolean isMailEnabled() {
        return getBooleanShared(MAIL_ENABLED, false);
    }

    private boolean getBooleanShared(String category, boolean defaultValue) {
        return this.sharedPreferences.getBoolean(category, defaultValue);
    }

    private String getShared(String category) {
        return getShared(category, "");
    }

    private String getShared(String category, String defaultValue) {
        return this.sharedPreferences.getString(category, defaultValue);
    }


}
