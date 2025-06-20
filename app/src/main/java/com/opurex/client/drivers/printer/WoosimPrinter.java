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

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.graphics.Bitmap;
import android.os.AsyncTask;

import com.woosim.printer.WoosimCmd;
import com.opurex.client.drivers.utils.DeviceManagerEvent;
import com.opurex.client.drivers.utils.DeviceManagerEventListener;
import java.io.IOException;
import java.io.OutputStream;
import java.util.UUID;

public class WoosimPrinter implements Printer {

    // Unique UUID for this application
	private static final UUID SPP_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");


    private BluetoothSocket sock;
    private OutputStream printerStream;
    private boolean connected;
    private String address;
    private DeviceManagerEventListener listener;

    public WoosimPrinter(String address, DeviceManagerEventListener listener) {
        super();
        this.address = address;
        this.listener = listener;
    }

    public String getAddress() {
        return this.address;
    }

    private void notifyListener(int event) { this.notifyListener(event, null); }
    private void notifyListener(int event, Object data) {
        if (this.listener != null) {
            this.listener.onDeviceManagerEvent(null, new DeviceManagerEvent(event, data));
        }
    }

    @Override
	public void connect() {
        BluetoothAdapter btadapt = BluetoothAdapter.getDefaultAdapter();
        try {
            BluetoothDevice dev = btadapt.getRemoteDevice(this.address.toUpperCase());
            // Get a BluetoothSocket
            this.sock = dev.createRfcommSocketToServiceRecord(SPP_UUID);
            new ConnTask().execute(dev);
        } catch (IllegalArgumentException | IOException e) {
            connected = false;
            this.notifyListener(DeviceManagerEvent.PrinterConnectFailure, e);
        }
    }

    @Override
    public void disconnect() {
        try {
            this.sock.close();
            if (this.printerStream != null) {
                this.printerStream.close();
            }
            this.connected = false;
            this.notifyListener(DeviceManagerEvent.PrinterDisconnected);
        } catch (IOException e) {
            this.notifyListener(DeviceManagerEvent.PrinterDisconnectFailure, e);
        }
    }

    public void forceDisconnect() {
        try {
            this.sock.close();
        } catch (IOException e) {
        }
        try {
            if (this.printerStream != null) {
                this.printerStream.close();
            }
        } catch (IOException e) {
        }
        this.connected = false;
        this.notifyListener(DeviceManagerEvent.PrinterDisconnected);
    }

    @Override
    public void initPrint() { }

    @Override
	public void printLine(String data) {
        String ascii = data.replace("é", "e");
        ascii = ascii.replace("è", "e");
        ascii = ascii.replace("ê", "e");
        ascii = ascii.replace("ë", "e");
        ascii = ascii.replace("à", "a");
        ascii = ascii.replace("ï", "i");
        ascii = ascii.replace("ô", "o");
        ascii = ascii.replace("ç", "c");
        ascii = ascii.replace("ù", "u");
        ascii = ascii.replace("É", "E");
        ascii = ascii.replace("È", "E");
        ascii = ascii.replace("Ê", "E");
        ascii = ascii.replace("Ë", "E");
        ascii = ascii.replace("À", "A");
        ascii = ascii.replace("Ï", "I");
        ascii = ascii.replace("Ô", "O");
        ascii = ascii.replace("Ç", "c");
        ascii = ascii.replace("Ù", "u");
        ascii = ascii.replace("€", "E");
        try {
            this.printerStream.write(ascii.getBytes());
            this.printerStream.write(WoosimCmd.printData());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
	public void printLine() {
        try {
            this.printerStream.write(WoosimCmd.printData());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
	public void cut() {
        try {
            this.printerStream.write(WoosimCmd.cutPaper(WoosimCmd.CUT_PARTIAL));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void flush() { }

    @Override
    public void printBitmap(Bitmap bitmap) { }

    @Override
    public boolean isConnected() {
        return connected;
    }

    // Bluetooth Connection Task.
	class ConnTask extends AsyncTask<BluetoothDevice, Void, Integer> {

		@Override
		protected void onPreExecute()
		{
			super.onPreExecute();
		}

		@Override
		protected Integer doInBackground(BluetoothDevice... params)
		{
			Integer retVal = null;
			try
			{
                sock.connect();
                printerStream = sock.getOutputStream();
                printerStream.write(WoosimCmd.initPrinter());
				retVal = new Integer(0);
			}
			catch (IOException e) {
                e.printStackTrace();
				retVal = new Integer(-1);
			}
			return retVal;
		}

		@Override
		protected void onPostExecute(Integer result)
		{
			if(result == 0)	// Connection success.
			{
				connected = true;
                notifyListener(DeviceManagerEvent.PrinterConnected);
			}
			else	// Connection failed.
			{
			}
			super.onPostExecute(result);
		}
	}
}
