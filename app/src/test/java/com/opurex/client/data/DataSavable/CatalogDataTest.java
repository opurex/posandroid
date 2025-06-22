package com.opurex.client.data.DataSavable;

import com.opurex.client.Constant;
import com.opurex.client.models.Catalog;
import com.opurex.client.utils.exception.DataCorruptedException;
import org.junit.Test;

import java.io.FileNotFoundException;

import static junit.framework.Assert.assertEquals;

/**
 * Created by nsvir on 12/10/15.
 * n.svirchevsky@gmail.com
 */
public class CatalogDataTest extends AbstractDataTest {

    @Override
    public String getTmpFilename() {
        return "catalog.data";
    }

    @Test
    public void save() throws FileNotFoundException, DataCorruptedException {
        replayContext();
        CatalogData catalogData = new CatalogData();
        catalogData.setFile(createCustomFile(Constant.BUILD_FOLDER + "catalog.data"));
        catalogData.load();
    }

    @Test
    public void simpleCatalog() throws FileNotFoundException {
        replayContext();
        CatalogData catalogData = new CatalogData();
        catalogData.setCatalog(new Catalog());
        catalogData.setFile(createDefaultTmpFile());
        catalogData.save();
    }

    @Test
    public void readCatalog() throws Throwable {
        replayContext();
        CatalogData catalogData = new CatalogData();
        catalogData.setFile(createDefaultTmpFile());
        try {
            catalogData.load();
        } catch (DataCorruptedException e) {
            throw e.Exception;
        }
        System.out.println(catalogData.toString());
    }
}