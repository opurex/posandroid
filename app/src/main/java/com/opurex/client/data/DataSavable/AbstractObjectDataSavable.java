package com.opurex.client.data.DataSavable;

import com.opurex.client.utils.exception.DataCorruptedException;
import com.opurex.client.utils.exception.DataCorruptedException.Action;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by nsvir on 11/08/15.
 * Save huge amount of data
 * But cannot recover data if the inheritance class changes (fields, methods, ..)
 * n.svirchevsky@gmail.com
 */
public abstract class AbstractObjectDataSavable extends AbstractDataSavable {

    private static final String DATA_DIRECTORY = "data";

    @Override
    protected String getDirectory() {
        return DATA_DIRECTORY;
    }

    public final void save() {
        save(getObjectList());
    }

    public final void load() throws DataCorruptedException {
        FileInputStream fis = null;
        ObjectInputStream ois = null;
        List<Object> objs = new ArrayList<>();
        int i = 0;
        int objectsToRead = this.getNumberOfObjects();
        try {
            fis = new FileInputStream(this.file);
            ois = new ObjectInputStream(fis);
            for (i = 0; i < objectsToRead; i++) {
                objs.add(ois.readObject());
            }
        } catch (ClassNotFoundException | FileNotFoundException e) {
            throw new DataCorruptedException(e, Action.LOADING)
                    .addFileName(getFileName())
                    .addObjectIndex(i)
                    .addObjectList(getObjectList());
        } catch (IOException e) {
            throw new IOError(e);
        } finally {
            close(fis);
            close(ois);
        }
        this.recoverObjects(objs);
    }

    private void save(List<?> objs) {
        FileOutputStream fos = null;
        ObjectOutputStream oos = null;
        try {
            fos = new FileOutputStream(this.file);
            oos = new ObjectOutputStream(fos);
            for (Object obj : objs) {
                oos.writeObject(obj);
            }
            oos.flush();
        } catch (IOException e) {
            throw new IOError(e);
        } finally {
            this.close(fos);
            this.close(oos);
        }
    }

    private void close(Closeable closable) {
        if (closable != null) {
            try {
                closable.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


}
