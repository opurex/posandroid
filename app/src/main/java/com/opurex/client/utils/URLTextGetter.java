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
package com.opurex.client.utils;

import com.opurex.client.sync.ServerLoader;

import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Map;
import java.util.Scanner;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class URLTextGetter {

    public static JSONObject getText(final String url,
            final Map<String, String> getParams)
        throws SocketTimeoutException, IOException, ServerException {
        return getText(url, getParams, null);
    }

    public static JSONObject getText(final String url,
            final Map<String, String> getParams,
            final Map<String, String> postParams)
        throws SocketTimeoutException, IOException, ServerException {
        // Format url
        String fullUrl = url;
        if (getParams != null && getParams.size() > 0) {
            fullUrl += "?";
            for (String param : getParams.keySet()) {
                fullUrl += URLEncoder.encode(param, "utf-8") + "="
                        + URLEncoder.encode(getParams.get(param), "utf-8") + "&";
            }
        }
        if (fullUrl.endsWith("&")) {
            fullUrl = fullUrl.substring(0, fullUrl.length() - 1);
        }
        // Init connection
        URL finalURL = new URL(fullUrl);
        HttpURLConnection conn = (HttpURLConnection) finalURL.openConnection();
        if (postParams != null) {
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            // Format POST
            String postBody = "";
            for (String key : postParams.keySet()) {
                postBody += URLEncoder.encode(key, "utf-8") + "="
                        + URLEncoder.encode(postParams.get(key), "utf-8")
                        + "&";
            }
            if (postBody.endsWith("&")) {
                postBody = postBody.substring(0, postBody.length() - 1);
            }
            // Set
            conn.setRequestProperty("Content-Length",
                    String.valueOf(postBody.length()));
            DataOutputStream os = new DataOutputStream(conn.getOutputStream());
            os.writeBytes(postBody);
            os.close ();
        } else {
            conn.setRequestMethod("GET");
        }
        // GO!
        conn.connect();
        int code = conn.getResponseCode();
        String codeMsg = conn.getResponseMessage();
        String token = conn.getHeaderField("token");
        InputStream in = null;
        // Convert the response to the good ol' JSON format
        JSONObject resp = new JSONObject();
        switch (code / 100) {
        case 2:
            in = conn.getInputStream();
            try {
                resp.put("status", ServerLoader.Response.STATUS_OK);
            } catch (JSONException e) { /* when "status" == null */ }
            break;
        case 4:
            in = conn.getErrorStream(); // because Java.
            try {
                resp.put("status", ServerLoader.Response.STATUS_REJECTED);
            } catch (JSONException e) { /* when "status" == null */ }
            break;
        case 5:
        default:
            in = conn.getErrorStream();
            Scanner s = new Scanner(in).useDelimiter("\\A");
            String content = s.hasNext() ? s.next() : "";
            conn.disconnect();
            throw new ServerException("Server not available: status " + code + ", message " + content);
        }
        // When there is no content sent, the input stream is null.
        if (ServerLoader.Response.ERR_CODE_NOT_LOGGED.equals(codeMsg)) {
            // Not logged case
            try {
                resp.put("status", ServerLoader.Response.STATUS_REJECTED);
                resp.put("content", ServerLoader.Response.ERR_CODE_NOT_LOGGED);
            } catch (JSONException e) { /* when constants are null */ }
        } else if (in != null) {
            // Use a simple hack with Scanner to get the content of a whole InputStream...
            Scanner s = new Scanner(in).useDelimiter("\\A");
            String content = s.hasNext() ? s.next() : "";
            conn.disconnect();
            if (token != null && !"".equals(token)) {
                try {
                    resp.put("token", token);
                } catch (JSONException e) { /* Desktop works without this */ }
            }
            try {
                resp.put("content", new JSONObject(content));
            } catch (JSONException e) {
                try {
                    resp.put("content", new JSONArray(content));
                } catch (JSONException ee) {
                    try {
                        resp.put("content", content);
                    } catch (JSONException eee) { /* Desktop works without this */ }
                }
            }
        } else {
            try {
                resp.put("content", conn.getResponseMessage());
            } catch (JSONException e) { /* Desktop works without this */ }
            // Do something else when there is no content
        }
        return resp;
    }

    public static class ServerException extends Exception {
        public ServerException(String message) {
            super(message);
        }
        public ServerException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
