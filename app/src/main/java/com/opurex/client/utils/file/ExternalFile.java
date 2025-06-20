package com.opurex.client.utils.file;

import com.opurex.client.OpurexPOS;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;

/**
 * Created by nsvir on 19/10/15.
 * n.svirchevsky@gmail.com
 */
public class ExternalFile extends File {

    public ExternalFile(String fileName) {
        super(OpurexPOS.getAppContext().getExternalFilesDir(null), fileName);
    }

    public ExternalFile(String dir, String filename) {
        //noinspection ConstantConditions
        super(OpurexPOS.getAppContext().getExternalFilesDir(null), filename);
    }

    /**
     * openRead is not used
     *
     * @return null
     * @throws FileNotFoundException
     */
    @Override
    protected FileInputStream openRead() throws FileNotFoundException {
        return null;
    }

    @Override
    protected FileOutputStream openWrite() throws FileNotFoundException {
        return new FileOutputStream(this);
    }
}