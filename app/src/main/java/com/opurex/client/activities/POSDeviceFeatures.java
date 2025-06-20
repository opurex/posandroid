package com.opurex.client.activities;

import android.os.Bundle;
import android.os.Looper;
import android.os.Handler;
import androidx.collection.ArrayMap;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.Switch;
import android.widget.TextView;
import com.opurex.client.OpurexPOS;
import com.opurex.client.R;
import com.opurex.client.drivers.POSDeviceManager;
import com.opurex.client.drivers.utils.DeviceManagerEvent;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Map;
import java.text.SimpleDateFormat;

/** Activity to test the connection to the device and send test commands.
 * Created by svirch_n on 11/03/16
 * Last edited at 16:27.
 */
public class POSDeviceFeatures
    extends POSConnectedTrackedActivity
    implements View.OnClickListener
{
    public static final Integer SCAN_NUMBER_SUCCESS = R.id.scan_number_success;
    public static final Integer SCAN_NUMBER_FAILURE = R.id.scan_number_failure;
    public static final Integer PRINT_NUMBER_SUCCESS = R.id.printer_success_number;
    public static final Integer PRINT_NUMBER_FAILURE = R.id.printer_failure_number;
    public static final Integer PRINT_NUMBER_PENDING = R.id.printer_pending_number;

    private Map<Integer, Integer> counters = new ArrayMap<>();
    private Map<Integer, Boolean> connected = new ArrayMap<>();
    private boolean hasCashDrawer = false;
    private String logsText = "";

    private TextView logs;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.pos_device_features);
        logs = (TextView) this.findViewById(R.id.logs);
        logs.setMovementMethod(new ScrollingMovementMethod());
        // Set device info
        ((TextView) this.findViewById(R.id.driver)).setText("Device driver: " + OpurexPOS.getConfiguration().getPrinterDriver());
        ((TextView) this.findViewById(R.id.model)).setText("Device model: " + OpurexPOS.getConfiguration().getPrinterModel());
        ((TextView) this.findViewById(R.id.address)).setText("Device address: " + OpurexPOS.getConfiguration().getPrinterAddress());
        // Set buttons
        this.findViewById(R.id.onOpenCashClick).setOnClickListener(this);
        this.findViewById(R.id.onPrinterClick).setOnClickListener(this);
        updateSwitchStatus(connected);
        updateNumbers(counters);
        updateLogs(logsText);
        // Restore stats
        if (savedInstanceState != null) {
            counters.put(SCAN_NUMBER_SUCCESS, savedInstanceState.getInt("scan_number_success"));
            counters.put(SCAN_NUMBER_FAILURE, savedInstanceState.getInt("scan_number_failure"));
            counters.put(PRINT_NUMBER_SUCCESS, savedInstanceState.getInt("print_number_success"));
            counters.put(PRINT_NUMBER_FAILURE, savedInstanceState.getInt("print_number_failure"));
            counters.put(PRINT_NUMBER_PENDING, savedInstanceState.getInt("print_number_pending"));
            connected.put(R.id.pos_switch, savedInstanceState.getBoolean("pos_switch"));
            connected.put(R.id.printer_switch, savedInstanceState.getBoolean("printer_switch"));
            connected.put(R.id.scanner_switch, savedInstanceState.getBoolean("scanner_switch"));
            logsText = savedInstanceState.getString("logs");
            this.hasCashDrawer = savedInstanceState.getBoolean("hasCashDrawer");
        }
        this.updateCashDrawerButtonState();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        // Save stats
        outState.putInt("scan_number_success", (Integer) getValue(counters, SCAN_NUMBER_SUCCESS, 0));
        outState.putInt("scan_number_failure", (Integer) getValue(counters, SCAN_NUMBER_FAILURE, 0));
        outState.putInt("print_number_success", (Integer) getValue(counters, PRINT_NUMBER_SUCCESS, 0));
        outState.putInt("print_number_failure", (Integer) getValue(counters, PRINT_NUMBER_FAILURE, 0));
        outState.putInt("print_number_pending", (Integer) getValue(counters, PRINT_NUMBER_PENDING, 0));
        outState.putBoolean("pos_switch", (Boolean) getValue(connected, R.id.pos_switch, false));
        outState.putBoolean("printer_switch", (Boolean) getValue(connected, R.id.printer_switch, false));
        outState.putBoolean("scanner_switch", (Boolean) getValue(connected, R.id.scanner_switch, false));
        outState.putString("logs", logsText);
        outState.putBoolean("hasCashDrawer", this.hasCashDrawer);
    }

    private Object getValue(Map<Integer, ?> map, Integer scanNumberSuccess, Object defaultValue) {
        Object result = map.get(scanNumberSuccess);
        return result == null ? defaultValue : result;
    }

    /** Enable/disable the button to open the cash drawer (if there is any). */
    private void updateCashDrawerButtonState() {
        this.findViewById(R.id.onOpenCashClick).setEnabled(this.hasCashDrawer);
    }

    private void updateSwitchStatus(int id, boolean value) {
        connected.put(id, value);
        ((Switch) this.findViewById(id)).setChecked(value);
    }

    private void updateNumbers(Map<Integer, Integer> counters) {
        for (Map.Entry<Integer, Integer> each : counters.entrySet()) {
            ((TextView) this.findViewById(each.getKey())).setText(String.valueOf(each.getValue()));
        }
    }

    private void updateSwitchStatus(Map<Integer, Boolean> connected) {
        for (Map.Entry<Integer, Boolean> each : connected.entrySet()) {
            ((Switch) this.findViewById(each.getKey())).setChecked(each.getValue());
        }
    }

    private void printScannerStatus(DeviceManagerEvent event) {
        addLog("Scanner readed: " + event.getString());
        addLog("Scanner Succeed");
        inc(SCAN_NUMBER_SUCCESS);
    }

    private void inc(Integer key) {
        Integer value = this.counters.get(key);
        if (value == null) {
            value = 0;
        }
        this.counters.put(key, ++value);
        ((TextView) this.findViewById(key)).setText(String.valueOf(value));
    }

    private void dec(Integer key) {
        Integer value = this.counters.get(key);
        if (value == null) {
            value = 0;
        }
        this.counters.put(key, --value);
        ((TextView) this.findViewById(key)).setText(String.valueOf(value));
    }

    private void addLogResult(boolean isSuccess, final String action, final Exception exception) {
        if (isSuccess) {
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    addLog(action + ": No issues in thread");
                }
            });
        } else {
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    addLog(action + ": " + exception.toString());
                }
            });
        }
    }

    public void onScannerSwitchClick(View view) {
    }

    public void onPrinterClick(View view) {
        inc(PRINT_NUMBER_PENDING);
        addLog(" - Printing Test..");
        this.printTest();
    }

    public void onOpenCashClick(View view) {
        addLog(" - Opening Cash..");
        this.openCashDrawer();
    }

    public void updateLogs(String logText) {
        if (this.logs != null) {
            this.logs.setText(logText);
        }
    }

    public void addLog(String newLog) {
        this.logsText = getTime() + ": " + newLog + "\n" + logsText;
        updateLogs(this.logsText);
    }

    public String getTime() {
        GregorianCalendar calendar = new GregorianCalendar();
        Date date = calendar.getTime();
        return new SimpleDateFormat("HH:mm:ss").format(date);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.onOpenCashClick:
                onOpenCashClick(v);
                break;
            case R.id.onPrinterClick:
                onPrinterClick(v);
                break;
        }
    }

    protected void onServiceConnected() {
        this.hasCashDrawer = this.deviceService.hasCashDrawer();
        this.updateCashDrawerButtonState();
    }

    public void onDeviceManagerEvent(POSDeviceManager manager, DeviceManagerEvent event) {
        switch (event.what) {
            case DeviceManagerEvent.ScannerReader:
                printScannerStatus(event);
                break;
            case DeviceManagerEvent.ScannerFailure:
                inc(SCAN_NUMBER_FAILURE);
                addLog("Scanner Failed");
                break;
            case DeviceManagerEvent.PrintDone:
                inc(PRINT_NUMBER_SUCCESS);
                dec(PRINT_NUMBER_PENDING);
                addLog("Printing Done");
                break;
            case DeviceManagerEvent.PrintError:
                inc(PRINT_NUMBER_FAILURE);
                dec(PRINT_NUMBER_PENDING);
                addLog("Printing Error");
                break;
            case DeviceManagerEvent.DeviceConnectFailure:
                addLog("Could not connect device");
                break;
            case DeviceManagerEvent.PrinterConnectFailure:
                addLog("Could not connect printer");
                break;
            case DeviceManagerEvent.PrinterConnected:
                updateSwitchStatus(R.id.printer_switch, true);
                addLog("Printer Connected");
                break;
            case DeviceManagerEvent.ScannerConnected:
                updateSwitchStatus(R.id.scanner_switch, true);
                addLog("Scanner Connected");
                break;
            case DeviceManagerEvent.PrinterDisconnected:
                addLog("Printer Disconnected");
                updateSwitchStatus(R.id.printer_switch, false);
                break;
            case DeviceManagerEvent.ScannerDisconnected:
                addLog("Scanner Disconnected");
                updateSwitchStatus(R.id.scanner_switch, false);
                break;
            case DeviceManagerEvent.CashDrawerOpened:
                addLog("Cash Drawer Opened");
                break;
            case DeviceManagerEvent.CashDrawerClosed:
                addLog("Cash Drawer Closed");
                break;
            case DeviceManagerEvent.PrintQueued:
                addLog("Print queued");
                break;
            default:
                addLog("Log not managed n°" + event.what);
                break;
        }
    }

}
