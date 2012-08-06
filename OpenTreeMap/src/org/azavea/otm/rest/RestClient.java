package org.azavea.otm.rest;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.azavea.otm.App;
import org.azavea.otm.R;
import org.azavea.otm.data.Model;
import org.azavea.otm.data.Plot;

import android.content.Context;
import android.content.SharedPreferences;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpVersion;
import org.apache.http.message.BasicHeader;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.params.HttpParams;

import android.content.Context;
import android.preference.PreferenceManager;
import android.util.Base64;

import android.util.Log;

import com.loopj.android.http.*;

// This class is designed to take care of the base-url
// and otm api-key for REST requests
public class RestClient {
	private String baseUrl;
	
	private String apiKey;
	
	private AsyncHttpClient client;
	
	private SharedPreferences prefs;
	
	
	public RestClient() {
		client = new AsyncHttpClient();
		prefs = App.getSharedPreferences();
		baseUrl = getBaseUrl();
		apiKey = getApiKey();
	}

	// Dependency injection to support mocking
	// in unit-tests
	public void setAsyncClient(AsyncHttpClient client) {
		this.client = client;
	}
	
	public void cancelRequests(Context context) {
		client.cancelRequests(context, true);
	}
	
	public void get(String url, RequestParams params, AsyncHttpResponseHandler responseHandler) {
		RequestParams reqParams = prepareParams(params);
		Log.d("rc", baseUrl);
		client.get(getAbsoluteUrl(url), reqParams, responseHandler);
	}
	
	public void post(Context context, String url, int id, Model model, AsyncHttpResponseHandler response) throws UnsupportedEncodingException {
		String completeUrl = getAbsoluteUrl(url);
		completeUrl += id + "?apikey=" + getApiKey();
		client.post(context, completeUrl, new StringEntity(model.getData().toString()), "application/json", response);
	}
	
	public void post(String url, RequestParams params, AsyncHttpResponseHandler responseHandler) {
		RequestParams reqParams = prepareParams(params);
		client.post(getAbsoluteUrl(url), reqParams, responseHandler);
	}
	
	public void put(Context context, String url, int id, Model model, AsyncHttpResponseHandler response) throws UnsupportedEncodingException {
		String completeUrl = getAbsoluteUrl(url);
		completeUrl += id + "?apikey=" + getApiKey();
		client.put(context, completeUrl, new StringEntity(model.getData().toString()), "application/json", response);
	}
	
	/**
	 * Executes a get request and adds basic authentication headers to the request.
	 */
	public void getWithAuthentication(Context context, String url, String username, 
		String password, RequestParams params, AsyncHttpResponseHandler responseHandler) {
		RequestParams reqParams = prepareParams(params);
		Header[] headers = {createBasicAuthenticationHeader(username, password)};
		client.get(context, getAbsoluteUrl(url), headers, reqParams, responseHandler);
	}

	/**
	 * Executes a put request and adds basic authentication headers to the request.
	 */
	public void putWithAuthentication(Context context,
									  String url,
									  String username,
									  String password,
									  int id,
									  Model model,
									  AsyncHttpResponseHandler response) throws UnsupportedEncodingException {
		
		String completeUrl = getAbsoluteUrl(url);
		completeUrl += id + "?apikey=" + getApiKey();
		Header[] headers = {createBasicAuthenticationHeader(username, password)};
		StringEntity modelEntity = new StringEntity(model.getData().toString());
		
		client.put(context, completeUrl, headers, modelEntity, "application/json", response);
	}
	
	/**
	 * Executes a post request and adds basic authentication headers to the request.
	 */
	public void postWithAuthentication(Context context,
									   String url,
									   String username, 
									   String password,
									   Model model, 
									   AsyncHttpResponseHandler responseHandler) throws UnsupportedEncodingException {
		
		String completeUrl = getAbsoluteUrl(url);
		completeUrl += "?apikey=" + getApiKey();
		Header[] headers = {createBasicAuthenticationHeader(username, password)};
		StringEntity modelEntity = new StringEntity(model.getData().toString());
		
		client.post(context, completeUrl, headers, new StringEntity(model.getData().toString()), "application/json", 
				responseHandler);
	}

	public void delete(String url, AsyncHttpResponseHandler responseHandler) {
		client.delete(getAbsoluteUrl(url), responseHandler);
	}

	/**
	 * Executes a delete request and adds basic authentication headers to the request.
	 */
	public void deleteWithAuthentication(Context context, String url, String username, 
			String password, AsyncHttpResponseHandler responseHandler) {
		Header[] headers = {createBasicAuthenticationHeader(username, password)};
		client.delete(context, getAbsoluteUrl(url), headers, responseHandler);
	}
	
	private RequestParams prepareParams(RequestParams params) {
		// We'll always need a RequestParams object since we'll always
		// be sending an apikey
		RequestParams reqParams;
		if (params == null) {
			reqParams = new RequestParams();
		} else {
			reqParams = params;
		}
		
		reqParams.put("apikey", apiKey);
		
		return reqParams;
	}
	
	private String getBaseUrl() {
		String baseUrl = prefs.getString("base_url", "");
		return baseUrl;
	}
	
	private String getApiKey() {
		String apiKey = prefs.getString("api_key", "");
		return apiKey;
	}
	
	private String getAbsoluteUrl(String relativeUrl) {
		Log.d(App.LOG_TAG, baseUrl + relativeUrl);
		return baseUrl + relativeUrl;
	}
	
	private Header createBasicAuthenticationHeader(String username, String password) {
		String credentials = String.format("%s:%s", username, password);
		String encoded = Base64.encodeToString(credentials.getBytes(), Base64.NO_WRAP);
		return new BasicHeader("Authorization", String.format("%s %s", "Basic", encoded));
	}
}