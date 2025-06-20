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

import com.opurex.client.drivers.printer.documents.PrintableDocument;

public interface POSDeviceManager
{
    /** Get the name/id of the device. */
    public String getName();
    /** Request the connection of the device. */
    public void connect();
    /** Request the disconnection of the device. */
    public void disconnect();
    /** Notify the manager that an external event disconnected the device. */
    public void wasDisconnected();
    /** Request to print a document. */
    public void print(PrintableDocument doc);
    /** Request to open the cash drawer. */
    public void openCashDrawer();
    public boolean hasCashDrawer();
    public boolean isManaging(Object o);
}
