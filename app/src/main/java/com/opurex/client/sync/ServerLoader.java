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
package com.opurex.client.sync;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.opurex.client.Configure;
import com.opurex.client.utils.URLBinGetter;
import com.opurex.client.utils.URLTextGetter;
import com.opurex.client.utils.HostParser;

/** Utility class to send calls to Opurex API
 * and manage login token. */
public class ServerLoader {

    /** Code for async calls.
     * Used in 'what' in handlers in case of exception.
     * The object is then the exception. */
    public static final int ERR = 0;
    /** Code for async calls.
     * Used in 'what' in handlers when no exception occured.
     * The object is then the server Response. */
    public static final int OK = 1;
    
    /** Cache for latest token. This is not a critical resource so it can
     * be garbage collected from time to time. */
    public static String lastToken = null;
    /** Control for preventing using a token from a different user
        (like changing user in configuration). */
    public static String lastTokenUser = null;

    private static String getLastToken(String user) {
        if (ServerLoader.lastToken != null
                && user != null && user.equals(ServerLoader.lastTokenUser)) {
            return ServerLoader.lastToken;
        } else {
            return null;
        }
    }

    /** Lock to take and release for synchronous/asynchronous call. */
    private Object lock;
    private String url;
    private String user;
    private String password;

    private void preformatUrl() {
        if (!this.url.startsWith("http")) {
            this.url = "http://" + this.url;
        }
        if (!this.url.endsWith("/")) {
            this.url += "/";
        }
    }

    /** Create from AppConfig */
    public ServerLoader(Context ctx) {
        this.url = HostParser.getHostFromPrefs(ctx);
        this.user = Configure.getUser(ctx);
        this.password = Configure.getPassword(ctx);
    }

    private Map<String, String> params(String token, String... params) {
        Map<String, String> ret = new HashMap<String, String>();
        ret.put("Token", token);
        for (int i = 0; i < params.length; i+= 2) {
            if (params[i + 1] != null) {
                String key = params[i];
                String value = params[i + 1];
                ret.put(key, value);
            }
        }
        return ret;
    }

    /** Inner call to get a JWT from scratch. */
    private Response login()
        throws SocketTimeoutException, URLTextGetter.ServerException,
               IOException {
        JSONObject resp = URLTextGetter.getText(this.url + "api/login",
                null, this.params("", "user", this.user,
                        "password", this.password));
        try {
            Response r = new Response(resp);
            if (r.getToken() != null) {
                ServerLoader.lastToken = r.getToken();
                ServerLoader.lastTokenUser = this.user;
            }
            return r;
        } catch (JSONException e) {
            throw new URLTextGetter.ServerException(e.getMessage());
        }
    }

    /** Check if a response is not rejected by token timeout and request
     * a new one if it is the case.
     * @return True if the token is still good,
     * false if a new one will be fetched. */
    private boolean checkAndRelog(Response r) {
        if (r == null) {
            // Dafuk?
            ServerLoader.lastToken = null;
            ServerLoader.lastTokenUser = null;
            return false;
        }
        if (Response.ERR_CODE_NOT_LOGGED.equals(r.getErrorCode())) {
            // The token is invalid, reject it to get a new one.
            ServerLoader.lastToken = null;
            ServerLoader.lastTokenUser = null;
            return false;
        }
        return true;
    }

    /** Get the last valid token or request a new one. */
    private String getToken()
        throws SocketTimeoutException, URLTextGetter.ServerException,
               IOException {
        String token = ServerLoader.getLastToken(this.user);
        if (token == null) {
            Response login = this.login();
            if (!Response.STATUS_OK.equals(login.getStatus())) {
                return null;
            }
            token = login.getToken();
        }
        return token;
    }

    public Response read(String target, String... params)
        throws SocketTimeoutException, URLTextGetter.ServerException,
               IOException {
        String token = this.getToken();
        if (token == null) {
            throw new URLTextGetter.ServerException("Unable to get auth token");
        }
        JSONObject resp = URLTextGetter.getText(this.url + target,
                this.params(token, params));
        try {
            Response r = new Response(resp);
            if (!this.checkAndRelog(r)) { // Retry with new token
                return this.read(target, params);
            }
            return r;
        } catch (JSONException e) {
            throw new URLTextGetter.ServerException(e.getMessage());
        }
    }

    /** Asynchronous read call. Get result in the Handler. */
    public void asyncRead(final Handler h, final String target,
            final String... params) {
        new Thread() {
            public void run() {
                try {
                    Response r = ServerLoader.this.read(target, params);
                    Message m = h.obtainMessage();
                    m.what = ServerLoader.OK;
                    m.obj = r;
                    m.sendToTarget();
                } catch (Exception e) {
                    Message m = h.obtainMessage();
                    m.what = ServerLoader.ERR;
                    m.obj = e;
                    m.sendToTarget();
                }
            }
        }.start();
    }

    public Response write(String target, String... params)
        throws SocketTimeoutException, URLTextGetter.ServerException,
               IOException {
        String token = this.getToken();
        if (token == null) {
            throw new URLTextGetter.ServerException("Unable to get auth token");
        }
        JSONObject resp = URLTextGetter.getText(this.url + target, null,
                this.params(token, params));
        try {
            Response r = new Response(resp);
            if (!this.checkAndRelog(r)) { // Retry with new token
                return this.write(target, params);
            }
            return r;
        } catch (JSONException e) {
            throw new URLTextGetter.ServerException(e.getMessage());
        }
    }

    /** Asynchronous write call. Get result in the Handler. */
    public void asyncWrite(final Handler h, final String target,
            final String... params) {
        new Thread() {
            public void run() {
                try {
                    Response r = ServerLoader.this.write(target, params);
                    Message m = h.obtainMessage();
                    m.what = ServerLoader.OK;
                    m.obj = r;
                    m.sendToTarget();
                } catch (Exception e) {
                    Message m = h.obtainMessage();
                    m.what = ServerLoader.ERR;
                    m.obj = e;
                    m.sendToTarget();
                }
            }
        }.start();
    }

    public byte[] readImage(String model, int id)
        throws SocketTimeoutException, URLBinGetter.ServerException,
               IOException {
        String token = null;
        try {
            token = this.getToken();
        } catch (URLTextGetter.ServerException e) {
            throw new URLBinGetter.ServerException(e.getMessage());
        }
        if (token == null) {
            throw new URLBinGetter.ServerException("Unable to get auth token");
        }
        String target = "api/image/" + model + "/" + id;
        return URLBinGetter.getBinary(this.url + target,
                this.params(token));
    }

    public void asyncReadImage(final Handler h, final String model,
            final int id) {
        new Thread() {
            public void run() {
                try {
                    byte[] data = ServerLoader.this.readImage(model, id);
                    Message m = h.obtainMessage();
                    m.what = ServerLoader.OK;
                    m.obj = data;
                    m.sendToTarget();
                } catch (Exception e) {
                    Message m = h.obtainMessage();
                    m.what = ServerLoader.ERR;
                    m.obj = e;
                    m.sendToTarget();
                }
            }
        }.start();
    }

    public class Response {
        public static final String STATUS_OK = "ok";
        public static final String STATUS_REJECTED = "rej";
        public static final String STATUS_ERROR = "err";

        public static final String ERR_CODE_NOT_LOGGED = "Not logged";

        private String status;
        private JSONObject response;
        private String token;
        
        public Response(JSONObject content) throws JSONException {
            this.status = content.getString("status");
            this.response = content;
            if (Response.STATUS_OK.equals(this.status) && content.has("token")) {
                this.token = content.getString("token");
            }
        }

        public String getStatus() {
            return this.status;
        }

        public JSONObject getResponse() {
            return this.response;
        }

        public String getToken() {
            return this.token;
        }

        public JSONObject getObjContent() {
            try {
                return this.response.getJSONObject("content");
            } catch (JSONException e) {
                return null;
            }
        }

        public JSONArray getArrayContent() {
            try {
                return this.response.getJSONArray("content");
            } catch (JSONException e) {
                return null;
            }
        }

        /** Get error code for rejected or error response. */
        public String getErrorCode() {
            try {
                return this.response.getString("content");
            } catch (JSONException e) {
                return null;
            } catch (NullPointerException e) {
                return null;
            }
        }
        /** @Override */
        public String toString() {
            if (this.response != null) {
                return this.response.toString();
            } else {
                return "nullresponse";
            }
        }
    }
}
