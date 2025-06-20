package com.opurex.client.drivers.mpop;

import android.os.AsyncTask;
import android.util.Log;
import com.starmicronics.stario.StarIOPort;
import com.starmicronics.stario.StarIOPortException;
import com.opurex.client.OpurexPOS;

/**
 * MPopManager is the public class which centralize all mPop behaviors
 * Thread safety Area
 * Created by svirch_n on 22/12/15.
 */
public class MPopManager {

    private static MPopManager mPopManager;
    private final StarIOPort port;

    /**
     * Take the saved port in configuration
     */
    MPopManager() throws StarIOPortException {
        this.port = MPopPort.getPort(OpurexPOS.getConfiguration().getPrinterModel());
    }

    /**
     * This is a singleton getter and is used by at least SendingCommand
     * @return MPopManager's singleton
     * @throws StarIOPortException
     */
    static synchronized MPopManager getMPOPManager() throws StarIOPortException {
        if (mPopManager == null) {
            mPopManager = new MPopManager();
        }
        return mPopManager;
    }

    static synchronized void invalidateMPOPManager() {
        disconnect();
    }

    static synchronized void disconnect() {
        if (mPopManager != null && mPopManager.port != null) {
            try {
                StarIOPort.releasePort(mPopManager.port);
            } catch (StarIOPortException e) {
                e.printStackTrace();
            }
        }
        mPopManager = null;
    }

    public static void openDrawer() {
        byte[] commands = MPopFunction.createCommandsOpenCashDrawer();
        new SendingCommand().execute(commands);
    }

    static void sendCommand(byte[] byteArray) {
        new SendingCommand().execute(byteArray);
    }

    public static class SendingCommand extends AsyncTask<byte[], Void, MPopCommunication.Result> {

        @Override
        protected MPopCommunication.Result doInBackground(byte[]... bytes) {
            MPopCommunication.Result result = MPopCommunication.Result.ErrorUnknown;
            if (bytes.length > 0)
                try {
                    MPopManager mPopManager = MPopManager.getMPOPManager();
                    synchronized (mPopManager.port) {
                        result = MPopCommunication.sendCommands(bytes[0], mPopManager.port);
                    }
                } catch (StarIOPortException e) {
                    e.printStackTrace();
                }
            return result;
        }

        @Override
        protected void onPostExecute(MPopCommunication.Result result) {
            Log.d(OpurexPOS.TAG + MPopManager.class.getName(), result.getAsText());
            if (result != MPopCommunication.Result.Success) {
                MPopManager.invalidateMPOPManager();
            }
        }
    }
}
