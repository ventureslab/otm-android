package org.azavea.otm.rest;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

import org.azavea.otm.App;
import org.azavea.otm.LoginManager;
import org.azavea.otm.data.Model;
import org.azavea.otm.data.Plot;
import org.azavea.otm.data.PlotContainer;
import org.azavea.otm.data.User;
import org.azavea.otm.rest.handlers.ContainerRestHandler;
import org.json.JSONException;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;

import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.BinaryHttpResponseHandler;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;


public class RequestGenerator {
	private RestClient client;
	LoginManager loginManager = App.getLoginManager();
	
	public RequestGenerator() {
		client = new RestClient();
	}
	
	// Dependency injection to support unit-testing
	public void setClient(RestClient client) {
		this.client = client;
	}
	
	/**
	 * Cancel any pending or active requests started from the provided context
	 */
	public void cancelRequests(Context activityContext) {
		client.cancelRequests(activityContext);
	}
	
	public void getVersion(JsonHttpResponseHandler handler) {
		client.get("/version", null, handler);
	}
	
	public void getPlot(int id, JsonHttpResponseHandler handler) {
		client.get("/plots/" + id, null, handler);
	}
	
	public void getImage(int plotId, int imageId, BinaryHttpResponseHandler binaryHttpResponseHandler) {
		client.get("/plots/" + plotId + "/tree/photo/" + imageId, null, binaryHttpResponseHandler);
	}
	
	public void addTreePhoto(int plotId, Bitmap bm) {
		
	}
	
	
	public void getPlotsNearLocation(double geoY, double geoX, ContainerRestHandler<PlotContainer> handler) {
		String url = "/locations/" + geoY + "," + geoX + "/plots";
		try {
			if (loginManager.isLoggedIn()) {
				client.getWithAuthentication(App.getInstance(), url, 
						loginManager.loggedInUser.getUserName(), 
						loginManager.loggedInUser.getPassword(), 
						null, handler);
			} else {
				client.get(url, null, handler);	
			}
		} catch (JSONException e) {
			// If user json error, request with no auth
			client.get(url, null, handler);
		}
	}
	
	public void getPlotsNearLocation(double geoY, double geoX, boolean recent, boolean pending, 
			ContainerRestHandler<PlotContainer> handler) {
		SharedPreferences sharedPrefs = App.getSharedPreferences();
		String maxPlots = sharedPrefs.getString("max_nearby_plots", "10");
		
		RequestParams params = new RequestParams();
		params.put("max_plots", maxPlots);
		params.put("filter_recent", Boolean.toString(recent));
		params.put("filter_pending", Boolean.toString(pending));
		
		String url = "/locations/" + geoY + "," + geoX + "/plots";
		
		try {
			if (loginManager.isLoggedIn()) {
				client.getWithAuthentication(App.getInstance(), url, 
						loginManager.loggedInUser.getUserName(), 
						loginManager.loggedInUser.getPassword(), 
						params, handler);
			} else {
				client.get(url, params, handler);	
			}
		} catch (JSONException e) {
			// If user json error, request with no auth
			client.get(url, params, handler);
		}		
	}
	
	public void updatePlot(Context context, int id, Plot plot, 
			AsyncHttpResponseHandler handler) throws UnsupportedEncodingException {
		if (loginManager.isLoggedIn()) {
			try {
				client.putWithAuthentication(context, "/plots/", 
						loginManager.loggedInUser.getUserName(), 
						loginManager.loggedInUser.getPassword(), id, plot, handler);
			} catch (JSONException e) {
				handleBadResponse(e);
			}
		} else {
			redirectToLoginActivity();
		}
	}

	public void getUserEdits(Context context, User user, int offset, int count, AsyncHttpResponseHandler handler) 
			throws JSONException {
		if (user != null) {
			Map<String, String> params = new HashMap<String, String>();
			params.put("offset", Integer.toString(offset));
			params.put("length", Integer.toString(count));
			
			client.getWithAuthentication(context, "/user/" + user.getId() + "/edits", 
					loginManager.loggedInUser.getUserName(), 
					loginManager.loggedInUser.getPassword(),
						new RequestParams(params), handler);
		}
	}
	
	public void addUser(Context context, User user, AsyncHttpResponseHandler handler) 
			throws JSONException, UnsupportedEncodingException {
		client.postWithAuthentication(context, "/user/", 
				loginManager.loggedInUser.getUserName(), 
				loginManager.loggedInUser.getPassword(), user, handler);
	}
	
	public void logIn(Context context, String username, String password, 
			JsonHttpResponseHandler handler) {
			client.getWithAuthentication(context, "/login", username, password, null, handler);
	}
	
	public void getAllSpecies(JsonHttpResponseHandler handler) {
		client.get("/species", null, handler);
	}

	public void deleteCurrentTreeOnPlot(Context context, int plotId, JsonHttpResponseHandler handler)
			throws JSONException {

		client.deleteWithAuthentication(context, "/plots/" + plotId + "/tree", 
				loginManager.loggedInUser.getUserName(), loginManager.loggedInUser.getPassword(), 
				handler);
	}
	
	public void deletePlot(Context context, int plotId, JsonHttpResponseHandler handler)
			throws JSONException {

		client.deleteWithAuthentication(context, "/plots/" + plotId, 
				loginManager.loggedInUser.getUserName(), loginManager.loggedInUser.getPassword(), 
				handler);
	}

	public void addTreePhoto(Context context, int plotId, Bitmap bm, 
			JsonHttpResponseHandler handler)
			throws JSONException {
		
		Log.d("sqh", "requestGenerator.addTreePhoto");
		
		client.postWithAuthentication(context, "/plots/" + plotId + "/tree/photo", bm, 
				loginManager.loggedInUser.getUserName(), loginManager.loggedInUser.getPassword(),
				handler);
		
	}

	
	
	
	private void handleBadResponse(JSONException e) {
		Log.e(App.LOG_TAG, "Unable to parse JSON response", e);
	}	
	
	private void redirectToLoginActivity() {
		// TODO Auto-generated method stub
		
	}
}
