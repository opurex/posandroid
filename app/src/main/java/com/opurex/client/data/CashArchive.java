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
package com.opurex.client.data;

import android.content.Context;
import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import com.opurex.client.OpurexPOS;
import com.opurex.client.models.Cash;
import com.opurex.client.models.Receipt;
import com.opurex.client.models.ZTicket;
import com.opurex.client.utils.exception.NoArchiveException;
import com.opurex.client.utils.exception.SaveArchiveException;
import com.opurex.client.utils.exception.loadArchiveException;
import com.opurex.client.utils.file.File;
import com.opurex.client.utils.file.InternalFile;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Stores finalized tickets
 */
public class CashArchive {

    private static final String ARCHIVESDIR = "archives";
    private static final String FILENAME = "tickets.data";
    private static String LOG_TAG = "Opurex/CashArchive";

    /**
     * Create an unique stable id from cash without id.
     */
    private static String cashId(Cash c) {
        return String.format("%d-%d", c.getCashRegisterId(), c.getSequence());
    }

    protected static void saveArchive(ZTicket z, List<Receipt> receipts) throws SaveArchiveException {
        File file = getFile(z.getCash());
        JsonArray jsonArray = getJson(z, receipts);
        try {
            file.write(jsonArray.toString());
        } catch (FileNotFoundException e) {
            throw new SaveArchiveException(e);
        }
    }

    private static JsonArray getJson(ZTicket z, List<Receipt> receipts) {
        List<Receipt> flattenRcpts = new ArrayList<Receipt>();
        for (Receipt r : receipts) {
            flattenRcpts.add(Receipt.convertFlatCompositions(r));
        }
        Gson gson = getGson();
        JsonParser parser = new JsonParser();
        JsonArray jsonArray = new JsonArray();
        jsonArray.add(parser.parse(gson.toJson(z)));
        jsonArray.add(parser.parse(gson.toJson(flattenRcpts)));
        return jsonArray;
    }

    private static Object[] getObjects(File file) throws FileNotFoundException {
        Gson gson = getGson();
        JsonParser parser = new JsonParser();
        Object[] result = new Object[2];
        JsonElement jsonElement = parser.parse(file.read());
        JsonArray jsonArray = jsonElement.getAsJsonArray();
        result[0] = gson.fromJson(jsonArray.get(0), ZTicket.class);
        result[1] = gson.fromJson(jsonArray.get(1), new TypeToken<List<Receipt>>() {}.getType());
        return result;
    }

    private static Gson getGson() {
        return new GsonBuilder().serializeNulls().create();
    }

    public static boolean archiveCurrent(ZTicket z)
            throws IOException {
        Context ctx = OpurexPOS.getAppContext();
        try {
            saveArchive(z, Data.Receipt.getReceipts(ctx));
        } catch (SaveArchiveException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    private static File getFile(Cash cash) {
        return new InternalFile(ARCHIVESDIR, cashId(cash));
    }

    public static int getArchiveCount(Context ctx) {
        java.io.File dir = ctx.getDir(ARCHIVESDIR, Context.MODE_PRIVATE);
        return dir.list().length;
    }

    public static boolean deleteArchive(Context ctx, ZTicket z) {
        File archive = getFile(z.getCash());
        return archive.exists() && archive.delete();
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    public static synchronized void updateArchive(ZTicket z,
            List<Receipt> receipts) throws IOException, SaveArchiveException {
        saveArchive(z, receipts);
    }

    /** Load the first available archive (alias for loadArchive(0)).
     * ZTicket is array[0], List<Receipt> array[1].
     */
    public static Object[] loadAnArchive() throws loadArchiveException {
        return loadArchive(0);
    }

    /** Load an archive by index. ZTicket is array[0], List<Receipt> array[1]. */
    public static Object[] loadArchive(int index) throws loadArchiveException {
        try {
            File file = new InternalFile(ARCHIVESDIR, getAFileArchive(index));
            return getObjects(file);
        } catch (NoArchiveException | FileNotFoundException e) {
            throw new loadArchiveException();
        }
    }

    public static boolean hasArchives(Context ctx) {
        return getArchiveCount(ctx) > 0;
    }

    /* Kaboom */
    @SuppressWarnings("ResultOfMethodCallIgnored")
    public static void clear(Context ctx) {
        java.io.File dir = OpurexPOS.getAppContext().getDir(ARCHIVESDIR, Context.MODE_PRIVATE);
        String[] list = dir.list();
        for (String fname : list) {
            new InternalFile(ARCHIVESDIR, fname).delete();
        }
    }

    public static String getAFileArchive(int index) throws NoArchiveException {
        // Get the list of archive filenames
        String[] file = OpurexPOS.getAppContext().getDir(ARCHIVESDIR, Context.MODE_PRIVATE).list();
        if (file.length < index) {
            throw new NoArchiveException();
        }
        int minExcludedSequence = -1;
        String candidate = null;
        int pass = 0;
        // Look for the smallest sequence, reloop with that sequence excluded
        // until it has looped the requested times.
        // This assumes there are only sessions from a single cash register.
        do {
            int minSequence = -1;
            for (String name : file) {
                String[] parts = name.split("-");
                int sequence = Integer.parseInt(parts[1]);
                if ((sequence < minSequence || minSequence == -1)
                        && sequence > minExcludedSequence) {
                    minSequence = sequence;
                    candidate = name;
                }
            }
            if (minSequence == -1) {
                // For a reason or an other, the archive can not be found
                throw new NoArchiveException();
            }
            pass++;
            minExcludedSequence = minSequence;
        } while (pass <= index);
        return candidate;
    }

    public static String getDir() {
        return ARCHIVESDIR;
    }
}
