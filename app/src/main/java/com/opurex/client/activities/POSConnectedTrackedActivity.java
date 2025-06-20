package com.opurex.client.activities;

import android.content.Context;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;

import com.opurex.client.drivers.DeviceService;
import com.opurex.client.drivers.POSDeviceManager;
import com.opurex.client.drivers.printer.documents.*;
import com.opurex.client.drivers.utils.DeviceManagerEvent;
import com.opurex.client.drivers.utils.DeviceManagerEventListener;
import com.opurex.client.models.CashRegister;
import com.opurex.client.models.Receipt;
import com.opurex.client.models.Ticket;
import com.opurex.client.models.ZTicket;

import java.io.Serializable;

/**
 * Activity to manage connected devices
 * Manage connection/disconnection in the activity lifecycle
 * Created by svirch_n on 23/12/15.
 */
public abstract class POSConnectedTrackedActivity
    extends TrackedActivity
    implements DeviceManagerEventListener, Serializable
{

    /** The connection to the device. Is null while the connection
     * to the service is not established. */
    protected DeviceService deviceService;
    private DeviceServiceConnection deviceServiceConnection;

    public boolean printOrder(int deviceIndex, Ticket t) {
        if (this.deviceService != null) {
            return this.deviceService.print(deviceIndex, new OrderDocument(t));
        }
        return false;
    }

    public boolean printReceipt(Receipt r) {
        if (this.deviceService != null) {
            return this.deviceService.print(new ReceiptDocument(r));
        }
        return false;
    }
    public boolean printZTicket(ZTicket z, CashRegister cashRegister) {
        if (this.deviceService != null) {
            return this.deviceService.print(new ZTicketDocument(z, cashRegister));
        }
        return false;
    }
    public boolean printTest() {
        if (this.deviceService != null) {
            return this.deviceService.print(new TestDocument());
        }
        return false;
    }
    public void openCashDrawer() {
        if (this.deviceService != null) {
            this.deviceService.openCashDrawer();
        }
    }
    public void reconnect() {
        if (this.deviceService != null) {
            this.deviceService.reconnect();
        }
    }

    public final boolean deviceManagerHasCashDrawer() {
        if (this.deviceService != null) {
            return this.deviceService.hasCashDrawer();
        } else {
            return false;
        }
    }

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);
        this.deviceServiceConnection = new DeviceServiceConnection();
    }

    protected void onStart() {
        super.onStart();
        this.bind();
    }

    protected void onStop() {
        super.onStop();
        this.unbind();
    }

    private void bind() {
        this.bindService(new Intent(this, DeviceService.class),
                this.deviceServiceConnection, Context.BIND_AUTO_CREATE);
    }

    private void unbind() {
        if (this.deviceService != null) {
            this.deviceService.removeListener(POSConnectedTrackedActivity.this);
            this.deviceService = null;
        }
        this.unbindService(this.deviceServiceConnection);
    }

    private class DeviceServiceConnection implements ServiceConnection {
        @Override
        public void onServiceConnected(ComponentName className, IBinder binder) {
            deviceService = ((DeviceService.Binder)binder).getService();
            deviceService.addListener(POSConnectedTrackedActivity.this);
            POSConnectedTrackedActivity.this.onServiceConnected();
        }
        @Override
        public void onServiceDisconnected(ComponentName className) {
            deviceService = null;
            POSConnectedTrackedActivity.this.onServiceDisconnected();
        }
    }

    @Override
    public abstract void onDeviceManagerEvent(final POSDeviceManager manager, final DeviceManagerEvent event);

    protected void onServiceConnected() {}
    protected void onServiceDisconnected() {}
}
