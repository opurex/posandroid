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

import com.opurex.client.utils.file.InternalFile;

import java.io.IOException;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public class ResourceData {

    private static final String FILEPREFIX = "res.";
    private static final String RESOURCE_DIRECTORY = "resources";

    /** Save a string resource */
    public static boolean save(Context ctx, String resName, String data)
        throws IOException {
        FileOutputStream fos = new FileOutputStream(getFile(resName));
        ObjectOutputStream oos = new ObjectOutputStream(fos);
        oos.writeObject(data);
        oos.close();
        return true;
    }

    /** Load a stored resource as String. Returns null if not saved */
    public static String loadString(Context ctx, String resName)
        throws IOException {
        String data = null;
        FileInputStream fis = new FileInputStream(getFile(resName));
        ObjectInputStream ois = new ObjectInputStream(fis);
        try {
            data = (String) ois.readObject();
        } catch (ClassNotFoundException cnfe) {
            // Should never happen
        }
        ois.close();
        return data;
    }

    private static InternalFile getFile(String resName) {
        return new InternalFile(RESOURCE_DIRECTORY, FILEPREFIX + resName);
    }

    public static boolean delete(Context ctx, String resName) {
        return ctx.deleteFile(FILEPREFIX + resName);
    }
}
