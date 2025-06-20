package com.opurex.client;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Created by nanosvir on 04 Jan 16.
 */
public class OpurexPOSTest {

    @Test
    public void testGetUniversalLog() throws Exception {
        assertEquals("Opurex:OpurexTest:testGetUniversalLog", OpurexPOS.Log.getUniversalLog());
    }

    @Test
    public void testRemovePackageNoIndex() throws Exception {
        String expected = "cannotFindIndex ";
        assertEquals(OpurexPOS.Log.removePackage(expected), expected);
    }
}