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
package com.opurex.client.drivers.printer;

import android.graphics.Bitmap;

/** General interface for printers.
 * A printer is driven by a POSDeviceManager and communicates with listeners
 * throught DeviceManagerEvents. */
public interface Printer
{
    public String getAddress();
    /** Request the connection to the printer. */
    public void connect();
    /** Request the disconnection from the printer. */
    public void disconnect();
    /** Disconnect the printer and/or mark it as disconnected in any case. */
    public void forceDisconnect();
    public boolean isConnected();
    /** Initialize the printer state. */
    public void initPrint();
    /** Send an empty line to the printer. */
    public void printLine();
    /** Send a text line to the printer. */
    public void printLine(String data);
    /** Send a bitmap to the printer. */
    public void printBitmap(Bitmap bitmap);
    /** Send a cut command to the printer. */
    public void cut();
    /** Request the printer to flush it's buffer. */
    public void flush();
}
