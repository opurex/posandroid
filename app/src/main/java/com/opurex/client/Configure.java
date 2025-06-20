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
package com.opurex.client;

import java.io.*;
import java.util.Properties;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;
import com.opurex.client.data.CashArchive;
import com.opurex.client.data.Data;
import com.opurex.client.data.DataSavable.AbstractJsonDataSavable;
import com.opurex.client.drivers.mpop.MPopEntries;
import com.opurex.client.drivers.mpop.MPopPort;
import com.opurex.client.utils.*;
import com.opurex.client.utils.file.ExternalFile;
import com.opurex.client.utils.file.InternalFile;

import static org.apache.commons.io.IOUtils.copy;

//Deprecation concerns the PreferenceFragment
@SuppressWarnings("deprecation")
public class Configure extends PreferenceActivity
        implements Preference.OnPreferenceChangeListener
{

    public static final int STATUS_ACCOUNT = 0;
    public static final int STATUS_DEMO = 1;
    public static final int STATUS_NONE = 2;

    /** @deprecated Automatically switches to standard since v7. */
    @Deprecated
    public static final int SIMPLE_MODE = 0;
    public static final int STANDARD_MODE = 1;
    public static final int RESTAURANT_MODE = 2;

    public static final String ERROR = "Error";

    /* Default values
     * Don't forget to update /res/xml/configure.xml to set the same
     * default value */
    private static final String DEMO_HOST = null;
    private static final String DEMO_USER = null; // set not null to enable demo
    private static final String DEMO_PASSWORD = null;
    private static final String DEMO_CASHREGISTER = null;
    private static final String DEFAULT_HOST = "10.0.2.2";
    private static final String DEFAULT_USER = "prexra";
    private static final String DEFAULT_PASSWORD = "Prexra789?";
    private static final String DEFAULT_CASHREGISTER = "Caisse";
    private static final int DEFAULT_PRINTER_CONNECT_TRY = 3;
    private static final boolean DEFAULT_SSL = true;
    private static final boolean DEFAULT_DISCOUNT = true;
    private static String LABEL_STATUS = "status";

    private ListPreference printerDrivers;
    private ListPreference auxPrinterDrivers1;
    private ListPreference auxPrinterDrivers2;
    private ListPreference printerModels;
    private ListPreference auxPrinterModels1;
    private ListPreference auxPrinterModels2;

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);
        // Set default values
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        if (!prefs.contains("payleven")) {
            SharedPreferences.Editor edit = prefs.edit();
            edit.putBoolean("payleven", Compat.hasPaylevenApp(this));
            edit.apply();
        }
        // Load preferences
        this.addPreferencesFromResource(R.xml.configure);
        this.printerDrivers = (ListPreference) this.findPreference("printer_driver");
        this.auxPrinterDrivers1 = (ListPreference) this.findPreference("printer_driver1");
        this.auxPrinterDrivers2 = (ListPreference) this.findPreference("printer_driver2");
        this.printerModels = (ListPreference) this.findPreference("printer_model");
        this.auxPrinterModels1 = (ListPreference) this.findPreference("printer_model1");
        this.auxPrinterModels2 = (ListPreference) this.findPreference("printer_model2");
        this.printerDrivers.setOnPreferenceChangeListener(this);
        this.auxPrinterDrivers1.setOnPreferenceChangeListener(this);
        this.auxPrinterDrivers2.setOnPreferenceChangeListener(this);
        this.updatePrinterPrefs(0, null);
        this.updatePrinterPrefs(1, null);
        this.updatePrinterPrefs(2, null);

        ListPreference card_processor = (ListPreference) this.findPreference("card_processor");
        card_processor.setOnPreferenceChangeListener(this);
        this.updateCardProcessorPreferences(null);
        if (this.comesFromError()) {
            this.showError(this.getError());
        }
    }

    /**
     * Display an AlertDialog
     * Based on Error.showError() but Configuration is not a TrackedActivity
     *
     * @param message to display
     */
    private void showError(String message) {
        AlertDialog.Builder b = new AlertDialog.Builder(this);
        b.setTitle(R.string.error_title);
        b.setMessage(message);
        b.setIcon(android.R.drawable.ic_dialog_alert);
        b.setCancelable(true);
        b.setNegativeButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                Configure.this.invalidateError();
            }
        });
        b.show();
    }

    private void invalidateError() {
        getIntent().removeExtra(Configure.ERROR);
    }

    private boolean comesFromError() {
        return getIntent().hasExtra(Configure.ERROR);
    }

    private String getError() {
        return getIntent().getStringExtra(Configure.ERROR);
    }

    private static String getString(Context ctx, int id) {
        return ctx.getResources().getString(id);
    }

    private void updateCardProcessorPreferences(String newValue) {
        if (newValue == null) {
            newValue = Configure.getCardProcessor(this);
        }

        ListPreference card_processor = (ListPreference) this.findPreference("card_processor");

        EditTextPreference atos_address = (EditTextPreference) this.findPreference("worldline_address");
        EditTextPreference xengo_userid = (EditTextPreference) this.findPreference("xengo_userid");
        EditTextPreference xengo_password = (EditTextPreference) this.findPreference("xengo_password");
        EditTextPreference xengo_terminalid = (EditTextPreference) this.findPreference("xengo_terminalid");

        atos_address.setEnabled("atos_classic".equals(newValue));
        xengo_userid.setEnabled("atos_xengo".equals(newValue));
        xengo_password.setEnabled("atos_xengo".equals(newValue));
        xengo_terminalid.setEnabled("atos_xengo".equals(newValue));


        card_processor.setSummary(newValue);
        int i = 0;
        for (CharSequence entry : card_processor.getEntryValues()) {
            if (newValue.equals(entry)) {
                card_processor.setSummary(card_processor.getEntries()[i]);
            }
            i++;
        }
    }

    private void updatePrinterPrefs(int index, Object newValue) {
        if (newValue == null) {
            newValue = OpurexPOS.getConf().getPrinterDriver(index);
        }
        ListPreference printerModels = this.printerModels;
        switch (index) {
            case 1:
                findPreference("printer_address1").setEnabled(true);
                printerModels = this.auxPrinterModels1;
                break;
            case 2:
                findPreference("printer_address2").setEnabled(true);
                printerModels = this.auxPrinterModels2;
                break;
            case 0:
            default:
                findPreference("printer_address").setEnabled(true);
                printerModels = this.printerModels;
                break;
        }
        printerModels.setOnPreferenceClickListener(null);
        if (OpurexConfiguration.PrinterDriver.NONE.equals(newValue)) {
            printerModels.setEnabled(false);
        } else if (OpurexConfiguration.PrinterDriver.EPSON_IP.equals(newValue)) {
            printerModels.setEnabled(true);
            printerModels.setEntries(R.array.config_printer_model_epson_epos);
            printerModels.setEntryValues(R.array.config_printer_model_epson_epos_values);
            printerModels.setValueIndex(0);
        } else if (OpurexConfiguration.PrinterDriver.LKPXX.equals(newValue)) {
            printerModels.setEnabled(true);
            printerModels.setEntries(R.array.config_printer_model_lk_pxx);
            printerModels.setEntryValues(R.array.config_printer_model_lk_pxx_values);
            printerModels.setValueIndex(0);
        } else if (OpurexConfiguration.PrinterDriver.WOOSIM.equals(newValue)) {
            printerModels.setEnabled(true);
            printerModels.setEntries(R.array.config_printer_model_woosim);
            printerModels.setEntryValues(R.array.config_printer_model_woosim_values);
            printerModels.setValueIndex(0);
        } else if (OpurexConfiguration.PrinterDriver.POWAPOS.equals(newValue)) {
            printerModels.setEnabled(true);
            printerModels.setEntries(R.array.config_printer_model_powapos);
            printerModels.setEntryValues(R.array.config_printer_model_powapos_values);
            printerModels.setValueIndex(0);
        } else if (OpurexConfiguration.PrinterDriver.STARMPOP.equals(newValue)) {
            printerModels.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    updateStarmPopPrinter();
                    return true;
                }
            });
            updateStarmPopPrinter();
        }
    }

    private void updateStarmPopPrinter() {
        MPopEntries ports = MPopPort.searchPrinterEntry();
        boolean hasContent = ports.size() > 0;
        this.printerModels.setEnabled(true); //needs to be true for preferenceClickListener
        this.printerModels.setEntries(ports.getEntries());
        this.printerModels.setEntryValues(ports.getValues());
        if (hasContent) {
            this.printerModels.setDefaultValue(ports.getValues()[0]);
            this.printerModels.setValueIndex(0);
        }
        findPreference("printer_address").setEnabled(false);
    }


    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference.getKey().equals("printer_driver")
                || preference.getKey().equals("printer_driver1")
                || preference.getKey().equals("printer_driver2")) {
            // On printer driver update, change models
            if (newValue.equals("EPSON ePOS")
                    && !Compat.isEpsonPrinterCompatible()) {
                Toast t = Toast.makeText(this, R.string.not_compatible,
                        Toast.LENGTH_SHORT);
                t.show();
                return false;
            } else if ((newValue.equals("LK-PXX")
                    && !Compat.isLKPXXPrinterCompatible())
                    || (newValue.equals("Woosim")
                    && !Compat.isWoosimPrinterCompatible())) {
                Toast t = Toast.makeText(this, R.string.not_compatible,
                        Toast.LENGTH_SHORT);
                t.show();
                return false;
            }
            if (preference.getKey().equals("printer_driver1")) {
                this.updatePrinterPrefs(1, newValue);
            } else if (preference.getKey().equals("printer_driver2")) {
                this.updatePrinterPrefs(2, newValue);
            } else {
                this.updatePrinterPrefs(0, newValue);
            }
        } else if ("card_processor".equals(preference.getKey())) {
            if ("payleven".equals(newValue) && !Compat.hasPaylevenApp(this)) {
                // Trying to enable payleven without app: download
                AlertDialog.Builder b = new AlertDialog.Builder(this);
                b.setTitle(R.string.config_payleven_download_title);
                b.setMessage(R.string.config_payleven_download_message);
                b.setIcon(android.R.drawable.ic_dialog_info);
                b.setNegativeButton(android.R.string.cancel, null);
                b.setPositiveButton(R.string.config_payleven_download_ok,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog,
                                                int which) {
                                dialog.dismiss();
                                Intent i = new Intent(Intent.ACTION_VIEW,
                                        Uri.parse("market://details?id=de.payleven.androidphone"));
                                Configure.this.startActivity(i);
                            }
                        });
                b.show();
                return false;
            }
            this.updateCardProcessorPreferences((String) newValue);
        }
        return true;
    }

    public static boolean isConfigured(Context ctx) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
        return !prefs.getString("user", "").equals("");
    }

    public static String getHost(Context ctx) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
        return prefs.getString("host", DEFAULT_HOST);
    }

    public static boolean getSsl(Context ctx) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
        return prefs.getBoolean("ssl", DEFAULT_SSL);
    }

    public static boolean getDiscount(Context ctx) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
        return prefs.getBoolean("discount", DEFAULT_DISCOUNT);
    }

    public static String getUser(Context ctx) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
        return prefs.getString("user", DEFAULT_USER);
    }

    public static String getPassword(Context ctx) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
        return prefs.getString("password", DEFAULT_PASSWORD);
    }

    public static String getMachineName(Context ctx) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
        return prefs.getString("machine_name", DEFAULT_CASHREGISTER);
    }

    public static boolean getCheckStockOnClose(Context ctx) {
        return false; // TODO: add config value for CheckStockOnClose
    }

    public static int getTicketsMode(Context ctx) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
        int mode = Integer.parseInt(prefs.getString("tickets_mode",
                        String.valueOf(STANDARD_MODE)));
        if (mode == SIMPLE_MODE) { mode = STANDARD_MODE; } // Legacy compat.
        return mode;
    }

    public static int getPrinterConnectTry() {
        return getPref("printer_connect_try", DEFAULT_PRINTER_CONNECT_TRY);
    }

    private static String getPref(String option, String defaultValue) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(OpurexPOS.getAppContext());
        if (sharedPreferences == null) {
            return defaultValue;
        } else {
            return sharedPreferences.getString(option, defaultValue);
        }
    }

    private static int getPref(String option, int defaultValue) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(OpurexPOS.getAppContext());
        if (sharedPreferences == null) {
            return defaultValue;
        } else {
            String value = sharedPreferences.getString(option, null);
            if (value == null) {
                return defaultValue;
            } else {
                return Integer.parseInt(value);
            }
        }
    }

    private static final int MENU_IMPORT_ID = 0;
    private static final int MENU_EXPORT_ID = 1;
    private static final int MENU_DEBUG_ID = 2;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        int i = 0;
        MenuItem imp = menu.add(Menu.NONE, MENU_IMPORT_ID, i++,
                this.getString(R.string.menu_cfg_import));
        imp.setIcon(android.R.drawable.ic_menu_revert);
        MenuItem exp = menu.add(Menu.NONE, MENU_EXPORT_ID, i++,
                this.getString(R.string.menu_cfg_export));
        exp.setIcon(android.R.drawable.ic_menu_edit);
        MenuItem dbg = menu.add(Menu.NONE, MENU_DEBUG_ID, i,
                this.getString(R.string.menu_cfg_debug));
        dbg.setIcon(android.R.drawable.ic_menu_report_image);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case MENU_EXPORT_ID:
                export();
                break;
            case MENU_IMPORT_ID:
                // Get properties file
                // TODO: check external storage state and access
                File path = Environment.getExternalStorageDirectory();
                path = new File(path, "opurex");
                File file = new File(path, "opurex.properties");
                FileInputStream fis;
                try {
                    fis = new FileInputStream(file);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                    Toast t = Toast.makeText(this,
                            R.string.cfg_import_file_not_found,
                            Toast.LENGTH_SHORT);
                    t.show();
                    return true;
                }
                Properties props = new Properties();
                try {
                    props.load(fis);
                } catch (IOException e) {
                    e.printStackTrace();
                    Toast t = Toast.makeText(this,
                            R.string.cfg_import_read_error,
                            Toast.LENGTH_SHORT);
                    t.show();
                    return true;
                }
                // Load props
                String host = props.getProperty("host", DEFAULT_HOST);
                String machineName = props.getProperty("machine_name",
                        null);
                String ticketsMode = props.getProperty("tickets_mode",
                        "simple");
                String user = props.getProperty("user", null);
                String password = props.getProperty("password",
                        null);
                String location = props.getProperty("stock_location", "");
                String printDrv = props.getProperty("printer_driver",
                        "None");
                String printModel = props.getProperty("printer_model",
                        "");
                String printAddr = props.getProperty("printer_address",
                        "");
                String printCtxTry = String.valueOf(getPref("printer_connect_try", DEFAULT_PRINTER_CONNECT_TRY));
                // Save
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
                SharedPreferences.Editor edit = prefs.edit();
                edit.putString("host", host);
                edit.putString("machine_name", machineName);
                // Set tickets mode, standard by default
                switch (ticketsMode) {
                    case "restaurant":
                        edit.putString("tickets_mode",
                                String.valueOf(RESTAURANT_MODE));
                        break;
                    default:
                        edit.putString("tickets_mode",
                                String.valueOf(STANDARD_MODE));
                        break;
                }
                edit.putString("user", user);
                edit.putString("password", password);
                edit.putString("stock_location", location);
                edit.putString("printer_driver", printDrv);
                edit.putString("printer_model", printModel);
                edit.putString("printer_address", printAddr);
                edit.putString("printer_connect_try", printCtxTry);
                edit.apply();
                Toast t = Toast.makeText(this, R.string.cfg_import_done,
                        Toast.LENGTH_SHORT);
                t.show();
                // Reset activity to reload values
                this.finish();
                Intent i = new Intent(this, Configure.class);
                this.startActivity(i);
                break;
            case MENU_DEBUG_ID:
                i = new Intent(Configure.this, Debug.class);
                this.startActivity(i);
                break;
        }
        return true;
    }

    private void export() {
        Data.export(AbstractJsonDataSavable.getStaticDirectory());
        try {
            File file = new InternalFile(CashArchive.getDir(), com.opurex.client.utils.file.File.DIRECTORY);
            String[] list = file.list();
            for (String filename : list) {
                copy(new FileInputStream(new InternalFile(CashArchive.getDir(), filename)),
                        new FileOutputStream(new ExternalFile(CashArchive.getDir(), filename)));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String getCardProcessor(Context ctx) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
        return prefs.getString("card_processor", "none");
    }

    public static String getWorldlineAddress(Context ctx) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
        return prefs.getString("worldline_address", "");
    }

    public static String getXengoUserId(Context ctx) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
        return prefs.getString("xengo_userid", "");
    }

    public static String getXengoTerminalId(Context ctx) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
        return prefs.getString("xengo_terminalid", "");
    }

    public static String getXengoPassword(Context ctx) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
        return prefs.getString("xengo_password", "");
    }

    private static void set(Context ctx, String label, String value) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
        prefs.edit()
                .putString(label, value)
                .apply();
    }

    private static void set(Context ctx, String label, int value) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
        prefs.edit()
                .putInt(label, value)
                .apply();
    }

    public static void setHost(Context ctx, String host) {
        Configure.set(ctx, "host", host);
    }

    public static void setUser(Context ctx, String user) {
        Configure.set(ctx, "user", user);
    }

    public static void setPassword(Context ctx, String psswd) {
        Configure.set(ctx, "password", psswd);
    }

    public static void setCashRegister(Context ctx, String cash) {
        Configure.set(ctx, "machine_name", cash);
    }

    public static void setStatus(Context ctx, int status) {
        Configure.set(ctx, LABEL_STATUS, status);
    }

    public static int getStatus(Context ctx) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
        return prefs.getInt(LABEL_STATUS, Configure.STATUS_NONE);
    }

    public static boolean canDemo() {
        return DEMO_USER != null;
    }

    public static void setDemo(Context ctx) {
        if (canDemo()) {
            Configure.setAccount(ctx,
                    DEMO_HOST, DEMO_USER, DEMO_PASSWORD, DEMO_CASHREGISTER,
                    true);
        }
    }

private static void setAccount(Context ctx, String host, String user, String pwd, String cash, boolean isDemo) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
        SharedPreferences.Editor edit = prefs.edit();
        edit.putString("host", host);
        edit.putString("password", pwd);
        edit.putString("user", user);
        edit.putString("machine_name", cash);
        if (canDemo() && isDemo) {
            edit.putInt(LABEL_STATUS, STATUS_DEMO);
        } else {
            edit.putInt(LABEL_STATUS, STATUS_ACCOUNT);
        }
        edit.apply();
    }

    public static void setAccount(Context ctx, String host, String user, String passwd, String cash) {
        Configure.setAccount(ctx, host, user, passwd, cash, false);
    }

    public static void invalidateAccount(Context ctx) {
        setHost(ctx, DEFAULT_HOST);
        setUser(ctx, DEFAULT_USER);
        setPassword(ctx, DEFAULT_PASSWORD);
        setStatus(ctx, STATUS_NONE);
    }

    /**
     * Very important function!
     * Start.removeLocalData rely on this on
     *
     * @param ctx the application's context
     * @return <code>true</code> if the current account is a demo
     */
    public static boolean isDemo(Context ctx) {
        return getStatus(ctx) == STATUS_DEMO;
    }

    public static boolean noAccount(Context ctx) {
        return getStatus(ctx) == STATUS_NONE;
    }

    public static boolean isAccount(Context ctx) {
        return getStatus(ctx) == STATUS_ACCOUNT;
    }

/*

    public static boolean getPayleven(Context ctx) {
		return "payleven".equals(getCardProcessor(ctx));

		// Old code enabled payleven automatically if the app was installed
//        boolean defaultVal = Compat.hasPaylevenApp(ctx);
//        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
//        boolean payleven = prefs.getBoolean("payleven", defaultVal);
//        return payleven;
    }*/
}
