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

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Map;

import org.json.JSONObject;

public class URLBinGetter {

    public static byte[] getBinary(final String url,
            final Map<String, String> getParams)
        throws SocketTimeoutException, IOException, ServerException {
        return getBinary(url, getParams, null);
    }

    public static byte[] getBinary(final String url,
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
        String token = conn.getHeaderField("token");
        InputStream in = null;
        // Convert the response to the good ol' JSON format
        JSONObject resp = new JSONObject();
        switch (code / 100) {
        case 2:
            break;
        case 4:
        case 5:
        default:
            throw new ServerException("Server not available: status " + code);
        }
        byte[] content = null;
        try {
            in = conn.getInputStream();
            byte[] buffer = new byte[1024];
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            int read = in.read(buffer);
            while (read != -1) {
                bos.write(buffer, 0, read);
                read = in.read(buffer);
            }
            content = bos.toByteArray();
        } catch (ClassCastException e) {
            throw new ServerException("Unknown content " + conn.getContentType().getClass(), e);
        }
        conn.disconnect();
        return content;
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
