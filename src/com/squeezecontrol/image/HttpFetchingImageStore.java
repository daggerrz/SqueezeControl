/*
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 3 as
 * published by the Free Software Foundation.
 */

package com.squeezecontrol.image;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;

import java.io.IOException;

public class HttpFetchingImageStore implements ImageStore {

    private String baseUrl;
    private DefaultHttpClient mClient;

    public HttpFetchingImageStore(String baseUrl, String username,
                                  String password) {
        this.baseUrl = baseUrl;

        HttpParams params = new BasicHttpParams();

        // Turn off stale checking. Our connections break all the time anyway,
        // and it's not worth it to pay the penalty of checking every time.
        HttpConnectionParams.setStaleCheckingEnabled(params, false);

        // Default connection and socket timeout of 20 seconds. Tweak to taste.
        HttpConnectionParams.setConnectionTimeout(params, 20 * 1000);
        HttpConnectionParams.setSoTimeout(params, 20 * 1000);
        HttpConnectionParams.setSocketBufferSize(params, 8192);
        SchemeRegistry schemeRegistry = new SchemeRegistry();
        schemeRegistry.register(new Scheme("http",
                PlainSocketFactory.getSocketFactory(), 80));
        schemeRegistry.register(new Scheme("https",
                SSLSocketFactory.getSocketFactory(), 443));

        ClientConnectionManager mgr = new ThreadSafeClientConnManager(params, schemeRegistry);
        mClient = new DefaultHttpClient(mgr, params);
        if (username != null && !"".equals(username)) {
            Credentials defaultcreds = new UsernamePasswordCredentials("dag",
                    "test");
            mClient.getCredentialsProvider().setCredentials(AuthScope.ANY,
                    defaultcreds);
        }
    }

    @Override
    public Bitmap getImage(String url) {
        HttpResponse response = null;
        try {
            HttpGet get = new HttpGet(baseUrl == null ? url : baseUrl + url);
            response = (BasicHttpResponse) mClient.execute(get);
            return BitmapFactory
                    .decodeStream(response.getEntity().getContent());
        } catch (IOException e) {
            return null;
        } finally {
            if (response != null)
                try {
                    response.getEntity().consumeContent();
                } catch (IOException e) {
                }
        }
    }

}
