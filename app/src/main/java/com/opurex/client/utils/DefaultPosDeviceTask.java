package com.opurex.client.utils;

import com.opurex.client.drivers.POSDeviceManager;

/**
 * Created by nanosvir on 04 Jan 16.
 */
public class DefaultPosDeviceTask extends PosDeviceTask<Void> {
    public DefaultPosDeviceTask(POSDeviceManager manager) {
        super(manager);
    }

    public static abstract class DefaultSynchronizedTask extends SynchronizedTask{}
}
