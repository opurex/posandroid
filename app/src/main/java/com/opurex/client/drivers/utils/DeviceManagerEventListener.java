package com.opurex.client.drivers.utils;

import com.opurex.client.drivers.POSDeviceManager;

/**
 * Created by svirch_n on 22/01/16.
 */
public interface DeviceManagerEventListener {

    public void onDeviceManagerEvent(POSDeviceManager manager, DeviceManagerEvent event);

}
