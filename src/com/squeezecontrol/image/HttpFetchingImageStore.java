package com.squeezecontrol.image;

import java.io.IOException;

import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicHttpResponse;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

public class HttpFetchingImageStore implements ImageStore {

	private String baseUrl;
	private DefaultHttpClient mClient;

	public HttpFetchingImageStore(String baseUrl, String username,
			String password) {
		this.baseUrl = baseUrl;
		mClient = new DefaultHttpClient();
		if (username != null && !"".equals(username)) {
			Credentials defaultcreds = new UsernamePasswordCredentials("dag",
					"test");
			mClient.getCredentialsProvider().setCredentials(AuthScope.ANY,
					defaultcreds);
		}
	}

	@Override
	public Bitmap getImage(String url) {
		try {
			HttpGet get = new HttpGet(baseUrl == null ? url : baseUrl + url);
			HttpResponse response = (BasicHttpResponse) mClient.execute(get);
			return BitmapFactory
					.decodeStream(response.getEntity().getContent());
		} catch (IOException e) {
			return null;
		}
	}

}
