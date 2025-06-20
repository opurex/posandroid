/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.opurex.client.data.DataSavable;

import com.opurex.client.OpurexPOS;
import com.opurex.client.data.Data;
import com.opurex.client.models.Product;
import com.opurex.client.models.Session;
import com.opurex.client.models.TicketLine;
import com.opurex.client.models.User;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.*;

import static org.junit.Assert.assertEquals;
import static org.powermock.api.easymock.PowerMock.mockStatic;

/**
 * @author nsvir
 */
@RunWith(PowerMockRunner.class)
//Used in setupd to mock the static method Opurex.getAppContext
@PrepareForTest(OpurexPOS.class)
public class SessionDataTest extends AbstractDataTest {

    public String getTmpFilename() {
        return "session.json";
    }

    private Product product;
    private SessionData sessionData;

    @Override
    public void setup() throws IOException {
        super.setup();
        this.product = new Product("1", "Ma salade", "0123232343", 24.5, "NoTaxe", 0, false, false, 0.2, false);
    }

    @Test
    public void stressTest() throws Exception {
        replayContext();
        this.sessionData = new SessionData();
        this.sessionData.setFile(createDefaultTmpFile());
        this.sessionData.newSessionIfEmpty();
        Session session = this.sessionData.currentSession(fakeContext);
        session.setUser(new User("id", "name", "password", "permission"));
        this.sessionData.save();

        for (int i = 0; i < 10; i++) {
            Data.Session.load();
            this.sessionData.save();
        }

    }

    @Test
    public void saveTest() throws Exception {
        replayContext();
        this.sessionData = new SessionData();
        sessionData.setFile(createDefaultTmpFile());
        sessionData.save();
        sessionData.newSessionIfEmpty();
        Session session = this.sessionData.currentSession(fakeContext);
        this.sessionData.setFile(createDefaultTmpFile());
        session.newCurrentTicket();
        session.getCurrentTicket().addProduct(this.product);

        this.sessionData.save();

        this.sessionData.load();
        session = this.sessionData.currentSession(fakeContext);

        TicketLine ticket = session.getCurrentTicket().getLineAt(0);
        Product product = ticket.getProduct();

        assertEquals(this.product, product);


    }
}
