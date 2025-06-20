package com.opurex.client.sync;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.opurex.client.models.Ticket;
import com.opurex.client.utils.URLTextGetter;
import android.content.Context;
import android.os.Handler;
import android.os.Message;

public class TicketUpdater {
    private Handler endHandler;

    static public final String TAG = "TicketUpdater";

    private Context ctx;

    public TicketUpdater(Context ctx) {
        this.ctx = ctx;
    }

    /** Synchronous call to get a shared ticket. */
    public Ticket getSharedTicket(String id)
        throws URLTextGetter.ServerException, JSONException,
               SocketTimeoutException, IOException {
        ServerLoader loader = new ServerLoader(this.ctx);
        ServerLoader.Response r = loader.read("TicketsAPI", "getShared",
                "id", id);
        Ticket ticket = this.parseTicket(r);
        return ticket;
    }

    /** Asynchronous call to get a shared ticket. */
    public void getSharedTicket(final String id, final Handler h) {
        new Thread() {
            public void run() {
                try {
                    Ticket tkts = TicketUpdater.this.getSharedTicket(id);
                    TicketUpdater.this.notifyOk(h, tkts);
                } catch (URLTextGetter.ServerException e) {
                    TicketUpdater.this.notifyErr(h, e);
                } catch (JSONException e) {
                    TicketUpdater.this.notifyErr(h, e);
                } catch (SocketTimeoutException e) {
                    TicketUpdater.this.notifyErr(h, e);
                } catch (IOException e) {
                    TicketUpdater.this.notifyErr(h, e);
                }
            }
        }.start();
    }

    public boolean removeSharedTicket(String id)
        throws URLTextGetter.ServerException, JSONException,
               SocketTimeoutException, IOException {
        ServerLoader loader = new ServerLoader(this.ctx);
        ServerLoader.Response r = loader.write("TicketsAPI", "delShared",
                "id", id);
        return r.getResponse().getBoolean("content");
    }

    public void removeSharedTicket(final String id, final Handler h) {
        new Thread() {
            public void run() {
                try {
                    boolean ok = TicketUpdater.this.removeSharedTicket(id);
                    TicketUpdater.this.notifyOk(h, new Boolean(ok));
                } catch (URLTextGetter.ServerException e) {
                    TicketUpdater.this.notifyErr(h, e);
                } catch (JSONException e) {
                    TicketUpdater.this.notifyErr(h, e);
                } catch (SocketTimeoutException e) {
                    TicketUpdater.this.notifyErr(h, e);
                } catch (IOException e) {
                    TicketUpdater.this.notifyErr(h, e);
                }
            }
        }.start();
    }

    private boolean sendSharedTicket(Ticket t)
        throws URLTextGetter.ServerException, JSONException,
               SocketTimeoutException, IOException {
        ServerLoader loader = new ServerLoader(this.ctx);
        ServerLoader.Response r = loader.write("TicketsAPI", "share",
                "ticket", t.toJSON(true).toString());
        return r.getResponse().getBoolean("content");
    }

    public void sendSharedTicket(final Ticket t, final Handler h) {
        new Thread() {
            public void run() {
                try {
                    boolean ok = TicketUpdater.this.sendSharedTicket(t);
                    TicketUpdater.this.notifyOk(h, new Boolean(ok));
                } catch (URLTextGetter.ServerException e) {
                    TicketUpdater.this.notifyErr(h, e);
                } catch (JSONException e) {
                    TicketUpdater.this.notifyErr(h, e);
                } catch (SocketTimeoutException e) {
                    TicketUpdater.this.notifyErr(h, e);
                } catch (IOException e) {
                    TicketUpdater.this.notifyErr(h, e);
                }
            }
        }.start();
    }

    public List<Ticket> getAllSharedTickets()
        throws URLTextGetter.ServerException, JSONException,
               SocketTimeoutException, IOException {
        ServerLoader loader = new ServerLoader(this.ctx);
        ServerLoader.Response r = loader.read("TicketsAPI", "getAllShared");
        List<Ticket> tickets = this.parseAllTickets(r);
        return tickets;
    }

    public void getAllSharedTickets(final Handler h) {
        new Thread() {
            public void run() {
                try {
                    List<Ticket> tkts = TicketUpdater.this.getAllSharedTickets();
                    TicketUpdater.this.notifyOk(h, tkts);
                } catch (URLTextGetter.ServerException e) {
                    TicketUpdater.this.notifyErr(h, e);
                } catch (JSONException e) {
                    TicketUpdater.this.notifyErr(h, e);
                } catch (IOException e) {
                    TicketUpdater.this.notifyErr(h, e);
                }
            }
        }.start();
    }

    private void notifyOk(Handler h, Object obj) {
        if (h == null) { return; }
        Message m = h.obtainMessage();
        m.what = ServerLoader.OK;
        m.obj = obj;
        m.sendToTarget();
    }
    private void notifyErr(Handler h, Exception e) {
        if (h == null) { return; }
        Message m = h.obtainMessage();
        m.what = ServerLoader.ERR;
        m.obj = e;
        m.sendToTarget();
    }

    private synchronized Ticket parseTicket(ServerLoader.Response resp)
    throws JSONException {
        JSONObject jsTkt = resp.getObjContent();
        Ticket t = Ticket.fromJSON(this.ctx, jsTkt);
        return t;
    }

    private synchronized List<Ticket> parseAllTickets(ServerLoader.Response resp)
    throws JSONException {
        List<Ticket> sharedTickets = new ArrayList<Ticket>();
        JSONArray jsTkts = resp.getArrayContent();
        for (int i = 0; i < jsTkts.length(); ++i) {
            Ticket t = Ticket.fromJSON(this.ctx, jsTkts.getJSONObject(i));
            sharedTickets.add(t);
        }
        return sharedTickets;
    }

}
