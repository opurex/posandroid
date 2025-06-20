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
package com.opurex.client.drivers;

import android.bluetooth.BluetoothDevice;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.util.Log;
import com.opurex.client.Configure;
import com.opurex.client.drivers.printer.documents.PrintableDocument;
import com.opurex.client.drivers.utils.DeviceManagerEvent;
import com.opurex.client.drivers.utils.DeviceManagerEventListener;
import com.opurex.client.utils.DefaultPosDeviceTask;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/** Service to manage interaction to a device.
 * Don't forget to register as listener when binding and unregister on unbind.
 * POSConnectedTrackedActivity handle this. */
public class DeviceService extends Service implements DeviceManagerEventListener
{
    private static final String LOG_TAG = "Opurex/DeviceService";
    /** Time in milliseconds during which reconnections requests will be ignored
     * to leave the time to the service to actually connect. */
    private static final long RECONNECT_DELAY_MILLIS = 1000;
    /** The device is not available. */
    private static final int STATUS_DISCONNECTED = 0;
    /** The device is connecting but still not available. */
    private static final int STATUS_CONNECTING = 1;
    /** The device is emptying the printing queue. */
    private static final int STATUS_PRINTING = 2;
    /** The device is ready and waiting. */
    private static final int STATUS_IDLE = 3;

    /** Device managers. The first (index 0) is the primary one that prints tickets,
     * the other are auxiliaries to optionaly print orders on different machines. */
    private List<POSDeviceManager> deviceManagers;
    private Binder binder;
    private List<DeviceManagerEventListener> listeners;

    /** Number of connection tries for devices. The first (index 0) is for the primary one. */
    private List<Integer> connectionTries;
    private int maxConnectionTries;
    /** Connection status of devices. The first (index 0) is for the primary one. */
    private List<Integer> printerStatus;
    /** Last connection time of devices. The first (index 0) is for the primary one. */
    private List<Long> connectTimes;
    private List<List<PrintableDocument>> printQueues;
    private List<Timer> printTimers;

    // General Service functions

    private int getDeviceCount() {
         // Hardcoded 1 main device and 2 auxiliaries
         return 3;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        // Create list of statuses
        this.connectionTries = new ArrayList<Integer>();
        this.deviceManagers = new ArrayList<POSDeviceManager>();
        this.printerStatus = new ArrayList<Integer>();
        this.connectTimes = new ArrayList<Long>();
        this.listeners = new ArrayList<DeviceManagerEventListener>();
        // Setup
        this.maxConnectionTries = Configure.getPrinterConnectTry();
        this.binder = new DeviceService.Binder();
        int deviceCount = this.getDeviceCount();
        this.printQueues = new ArrayList<List<PrintableDocument>>();
        this.printTimers = new ArrayList<Timer>();
        for (int i = 0; i < deviceCount; i++) {
            this.deviceManagers.add(POSDeviceManagerFactory.createPosConnection(this, i));
            Log.i(LOG_TAG, "Created device manager " + this.deviceManagers.get(i).getName());
            this.printerStatus.add(DeviceService.STATUS_DISCONNECTED);
            this.connectTimes.add(new Long(0));
            this.connectionTries.add(new Integer(0));
            this.printQueues.add(new ArrayList<PrintableDocument>());
            this.printTimers.add(new Timer());
        }

        // Connect
        this.registerToBluetoothIntents();
        for (int i = 0; i < deviceCount; i++) {
            this.connect(i);
        }
        Log.i(LOG_TAG, "DeviceService created");
    }

    @Override
    public int onStartCommand(Intent i, int flags, int startId) {
        return super.onStartCommand(i, flags, startId);
    }

    /** Get the DeviceService.Binder that gives an access to this,
     * with getService(). */
    @Override
    public IBinder onBind(Intent i) {
        return this.binder;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        for (int i = 0; i < this.getDeviceCount(); i++) {
            this.printTimers.get(i).cancel();
            if (this.deviceManagers.get(i) != null) {
                this.deviceManagers.get(i).disconnect();
            }
        }
        this.unregisterToBluetoothIntents();
        Log.i(LOG_TAG, "DeviceService stopped");
    }

    /** The local binder proxy that allow access to the DeviceService. */
    public class Binder extends android.os.Binder
    {
        public DeviceService getService() {
            return DeviceService.this;
        }
    }

    // Inner connection management

    /** Connect the device asynchronously in an other thread. */
    private void connect(int deviceIndex) {
        if (this.printerStatus.get(deviceIndex) != DeviceService.STATUS_DISCONNECTED) {
            return;
        }
        this.printerStatus.set(deviceIndex, DeviceService.STATUS_CONNECTING);
        POSDeviceManager m = this.deviceManagers.get(deviceIndex);
        new DefaultPosDeviceTask(this.deviceManagers.get(deviceIndex)).execute(new DefaultPosDeviceTask.DefaultSynchronizedTask() {
            public void execute(POSDeviceManager manager) throws Exception {
                manager.connect();
            }
        });
    }

    /** Disconnect the device asynchronously in an other thread. */
    private void disconnect(int deviceIndex) {
        new DefaultPosDeviceTask(this.deviceManagers.get(deviceIndex)).execute(new DefaultPosDeviceTask.DefaultSynchronizedTask() {
            public void execute(POSDeviceManager manager) throws Exception {
                manager.disconnect();
            }
        });
    }

    private void reset(int deviceIndex) {
        Log.i(LOG_TAG, "DeviceService resetting device " + deviceIndex);
        this.deviceManagers.get(deviceIndex).wasDisconnected();
        this.deviceManagers.set(deviceIndex, POSDeviceManagerFactory.createPosConnection(this, deviceIndex));
        this.printerStatus.set(deviceIndex, DeviceService.STATUS_DISCONNECTED);
        this.connectTimes.set(deviceIndex, new Long(0));
        this.connectionTries.set(deviceIndex, new Integer(0));
        this.connect(deviceIndex);
    }

    // System event handling

    /** Bluetooth device Listener */
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_ACL_CONNECTED.equals(action)) {
                DeviceService.this.reconnect();
            } else if (BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action)) {
                for (int i = 0; i < getDeviceCount(); i++) {
                    POSDeviceManager manager = deviceManagers.get(i);
                    BluetoothDevice device = (BluetoothDevice) intent.getParcelableExtra(android.bluetooth.BluetoothDevice.EXTRA_DEVICE);
                    if (manager.isManaging(device)) {
                        manager.wasDisconnected();
                        printerStatus.set(i, DeviceService.STATUS_DISCONNECTED);
                        connectionTries.set(i, 0);
                        connectTimes.set(i, new Long(0));
                        connect(i);
                    }
                }
            }
        }
    };

    /** Create filters to handle bluetooth device connected */
    private void registerToBluetoothIntents() {
        List<IntentFilter> filters = new ArrayList<>();
        filters.add(new IntentFilter(BluetoothDevice.ACTION_ACL_CONNECTED));
        filters.add(new IntentFilter(BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED));
        filters.add(new IntentFilter(BluetoothDevice.ACTION_ACL_DISCONNECTED));
        for (IntentFilter each : filters) {
            this.registerReceiver(this.mReceiver, each);
        }
    }

    /** Remove filters to handle bluetooth device connected */
    private void unregisterToBluetoothIntents() {
        this.unregisterReceiver(this.mReceiver);
    }

    // Public device management functions
    // Provide intents for start

    public void addListener(DeviceManagerEventListener listener) {
        this.listeners.add(listener);
    }

    public void removeListener(DeviceManagerEventListener listener) {
        this.listeners.remove(listener);
    }

    /** Print a document on the main printer. Alias to print(0, doc). */
    public boolean print(final PrintableDocument doc) {
        return this.print(0, doc);
    }

    /** Print a document. If the printer is not available, it is queued.
     * @return True. */
    public boolean print(final int deviceIndex, final PrintableDocument doc) {
        if (deviceIndex >= this.getDeviceCount()) {
            return true;
        }
        int status = this.printerStatus.get(deviceIndex);
        if (status == DeviceService.STATUS_DISCONNECTED || status == DeviceService.STATUS_CONNECTING) {
            POSDeviceManager manager = this.deviceManagers.get(deviceIndex);
            for (DeviceManagerEventListener listener : this.listeners) {
                listener.onDeviceManagerEvent(manager,
                        new DeviceManagerEvent(DeviceManagerEvent.PrintQueued, doc));
            }
        }
        this.printQueues.get(deviceIndex).add(doc);
        this.printFromQueue(deviceIndex);
        return true;
    }

    private synchronized void printFromQueue(final int deviceIndex) {
        int status = this.printerStatus.get(deviceIndex);
        switch (status) {
            case DeviceService.STATUS_DISCONNECTED:
                this.connect(deviceIndex);
                return;
            case DeviceService.STATUS_CONNECTING:
            case DeviceService.STATUS_PRINTING:
                // Wait for the event
                return;
            case DeviceService.STATUS_IDLE:
                // Continue
                break;
        }
        // Only when the printer is IDLE
        if (this.printQueues.get(deviceIndex).size() == 0) {
            this.printTimers.get(deviceIndex).cancel();
            this.printTimers.get(deviceIndex).purge();
            this.printTimers.set(deviceIndex, new Timer());
            this.printerStatus.set(deviceIndex, DeviceService.STATUS_IDLE);
            return;
        }
        this.printerStatus.set(deviceIndex, DeviceService.STATUS_PRINTING);
        final PrintableDocument doc = this.printQueues.get(deviceIndex).get(0);
        new DefaultPosDeviceTask(this.deviceManagers.get(deviceIndex)).execute(new DefaultPosDeviceTask.DefaultSynchronizedTask() {
                public void execute(POSDeviceManager manager) throws Exception {
                    manager.print(doc);
                }
            });
    }

    /** Check if the main device has a cash drawer. */
    public boolean hasCashDrawer() {
        return this.deviceManagers.get(0).hasCashDrawer();
    }

    /** Open the cash drawer of the main device. */
    public boolean openCashDrawer() {
        if (this.deviceManagers.get(0).hasCashDrawer()) {
            new DefaultPosDeviceTask(this.deviceManagers.get(0)).execute(new DefaultPosDeviceTask.DefaultSynchronizedTask() {
                public void execute(POSDeviceManager manager) throws Exception {
                    manager.openCashDrawer();
                }
            });
            return true;
        }
        return false;
    }

    /** Try to reconnect all devices if not already connected. */
    public void reconnect() {
        for (int i = 0; i < this.getDeviceCount(); i++) {
            int status = this.printerStatus.get(i);
            if (status == DeviceService.STATUS_DISCONNECTED) {
                this.connectionTries.set(i, 0);
                this.connectTimes.set(i, new Long(0));
                this.connect(i);
            }
        }
    }

    // DeviceManagerEventListener functions
    // Just propagate the events to the listeners.

    public void onDeviceManagerEvent(POSDeviceManager manager, DeviceManagerEvent event) {
        Log.i(LOG_TAG, "Received event " + event + " from " + manager.getName());
        int index = -1;
        for (int i = 0; i < this.getDeviceCount(); i++) {
            if (this.deviceManagers.get(i).equals(manager)) {
                index = i;
                break;
            }
        }
        if (index != -1) {
            switch (event.what) {
            case DeviceManagerEvent.PrinterConnected:
                this.printerStatus.set(index, DeviceService.STATUS_IDLE);
                this.connectionTries.set(index, 0);
                this.printFromQueue(index);
                break;
            case DeviceManagerEvent.PrinterConnectFailure:
            case DeviceManagerEvent.DeviceConnectFailure:
                this.printerStatus.set(index, DeviceService.STATUS_DISCONNECTED);
                int connectionTries = this.connectionTries.get(index);
                connectionTries++;
                this.connectionTries.set(index, connectionTries);
                if (connectionTries < this.maxConnectionTries) {
                    this.connect(index);
                    return; // Don't propagate the event
                } else {
                    if (this.printQueues.get(index).size() > 0) {
                        this.connect(index);
                        return;
                    } // else propagate the event
                }
                break;
            case DeviceManagerEvent.PrintDone:
                this.printerStatus.set(index, DeviceService.STATUS_IDLE);
                PrintableDocument doc = (PrintableDocument) event.getExtra();
                if (doc != null) {
                    printQueues.get(index).remove(doc);
                    final int index_final = index;
                    printTimers.get(index).schedule(new TimerTask() {
                        public void run() {
                            printFromQueue(index_final);
                        }
                    }, 3000);
                }
                break;
            case DeviceManagerEvent.PrintError:
                printQueues.get(index).remove(0);
                this.printerStatus.set(index, DeviceService.STATUS_IDLE);
                printFromQueue(index);
                break;
            case DeviceManagerEvent.PrinterDisconnected:
                this.printerStatus.set(index, DeviceService.STATUS_DISCONNECTED);
                this.printFromQueue(index); // Reconnect if required
                break;
            }
        }
        // Propagate to listeners
        for (DeviceManagerEventListener listener : this.listeners) {
            listener.onDeviceManagerEvent(manager, event);
        }
    }

}
