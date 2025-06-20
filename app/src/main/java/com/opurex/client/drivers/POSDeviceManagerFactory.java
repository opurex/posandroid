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

import com.opurex.client.OpurexPOS;
import com.opurex.client.drivers.mpop.MPopDeviceManager;
import com.opurex.client.drivers.utils.DeviceManagerEventListener;
import com.opurex.client.utils.OpurexConfiguration;

public class POSDeviceManagerFactory
{

    /** Create a POSDeviceManager for the main device. Alias of createPosConnection(listener, 0). */
    public static POSDeviceManager createPosConnection(DeviceManagerEventListener listener) {
        return createPosConnection(listener, 0);
    }

    /** Create a POSDeviceManager according to the configuration
     * and set it's listener. */
    public static POSDeviceManager createPosConnection(DeviceManagerEventListener listener, int deviceIndex) {
        POSDeviceManager manager = null;
        switch (OpurexPOS.getConfiguration().getPrinterDriver(deviceIndex)) {
            case OpurexConfiguration.PrinterDriver.POWAPOS:
                return new PowaDeviceManager(listener);
            case OpurexConfiguration.PrinterDriver.STARMPOP:
                return new MPopDeviceManager(listener);
            default:
                return new DefaultDeviceManager(listener, deviceIndex);
        }
    }

}
