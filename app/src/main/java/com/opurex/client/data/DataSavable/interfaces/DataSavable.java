package com.opurex.client.data.DataSavable.interfaces;

import com.opurex.client.utils.exception.DataCorruptedException;

import java.io.IOError;

/**
 * Created by nsvir on 11/08/15.
 * n.svirchevsky@gmail.com
 */

/**
 * Interface used by Data
 * Classes that implements this interface have to use a decent system to save and load Objects.
 * Take a look at AbastractDataSavable to get some inspiration.
 *
 * Save/Load the object
 * @param ctx the application's context
 * @throws DataCorruptedException if the data cannot be write and the user can do something about it.
 * @throws IOError if the IO methodes throw an unexpected exception.
 */
public interface DataSavable {

    void save() throws IOError;
    void load() throws DataCorruptedException, IOError;
    void export(String dir);

    /**
     * Called on Warning Exception
     * @param e
     * @return <code>true</code> if you want the user to update data,
     * <code>false</code> if you want to remain silent about it.
     */
    boolean onLoadingFailed(DataCorruptedException e);

    /**
     * Called on Fatal Error
     * @param e
     * @return <code>true</code> if you want the user to update data,
     * <code>false</code> if you want to remain silent about it.
     */
    boolean onLoadingError(IOError e);
}
