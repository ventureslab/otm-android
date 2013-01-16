package org.azavea.otm.ui;

/*
 * Copyright (C) 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnMapClickListener;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.TileOverlayOptions;
import com.google.android.gms.maps.model.TileProvider;
import com.loopj.android.http.BinaryHttpResponseHandler;

import org.azavea.map.TileProviderFactory;

import org.azavea.otm.App;
import org.azavea.otm.R;
import org.azavea.otm.data.Plot;
import org.azavea.otm.data.PlotContainer;
import org.azavea.otm.data.Tree;
import org.azavea.otm.rest.RequestGenerator;
import org.azavea.otm.rest.handlers.ContainerRestHandler;
import org.json.JSONException;
import org.json.JSONObject;


import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.location.Location;
import android.location.LocationManager;

import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

public class MapDisplay extends MapActivity{
	private static final LatLng PHILADELPHIA = new LatLng(39.952622, -75.165708) ;
	private static final int DEFAULT_ZOOM_LEVEL = 12;
	private static final int FILTER_INTENT = 1;
	private static final int INFO_INTENT = 2;
	
	private TextView plotSpeciesView;
	private TextView plotAddressView;
	private TextView plotDiameterView;
	private TextView plotUpdatedByView;
	private ImageView plotImageView;
	private RelativeLayout plotPopup;
	private Plot currentPlot; // The Plot we're currently showing a pop-up for, if any
	private Marker plotMarker;
    private GoogleMap mMap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map_display_2);
        setUpMapIfNeeded();
		plotPopup = (RelativeLayout) findViewById(R.id.plotPopup);
		setPopupViews();
    }

    @Override
    protected void onResume() {
        super.onResume();
        setUpMapIfNeeded();
    }

    
    /**
     * Sets up the map if it is possible to do so (i.e., the Google Play services APK is correctly
     * installed) and the map has not already been instantiated.. This will ensure that we only ever
     * call {@link #setUpMap()} once when {@link #mMap} is not null.
     * <p>
     * If it isn't installed {@link SupportMapFragment} (and
     * {@link com.google.android.gms.maps.MapView
     * MapView}) will show a prompt for the user to install/update the Google Play services APK on
     * their device.
     * <p>
     * A user can return to this Activity after following the prompt and correctly
     * installing/updating/enabling the Google Play services. Since the Activity may not have been
     * completely destroyed during this process (it is likely that it would only be stopped or
     * paused), {@link #onCreate(Bundle)} may not be called again so we should call this method in
     * {@link #onResume()} to guarantee that it will be called.
     */
    private void setUpMapIfNeeded() {
        // Do a null check to confirm that we have not already instantiated the map.
        if (mMap == null) {
            // Try to obtain the map from the SupportMapFragment.
            mMap = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map))
                    .getMap();
            // Check if we were successful in obtaining the map.
            if (mMap != null) {
                setUpMap();
            } else {
            	Log.e(App.LOG_TAG, "Map was null!");
            }
        }       
    }
    
    private void setUpMap() {
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(PHILADELPHIA, DEFAULT_ZOOM_LEVEL));  
        TileProvider tileProvider = TileProviderFactory.getTileProvider("otm");
        mMap.addTileOverlay(new TileOverlayOptions().tileProvider(tileProvider));
        mMap.setOnMapClickListener(mapClickListener);
    }
    
    public void showPopup(Plot plot) {
		//set default text
		plotDiameterView.setText(getString(R.string.dbh_missing));
		plotSpeciesView.setText(getString(R.string.species_missing));
		plotAddressView.setText(getString(R.string.address_missing));
		plotImageView.setImageResource(R.drawable.ic_action_search);
		
		try {
	        plotUpdatedByView.setText(plot.getLastUpdatedBy());
	        if (plot.getAddress().length() != 0) {
	        	plotAddressView.setText(plot.getAddress());
	        }
			Tree tree = plot.getTree();
			if (tree != null) {
				String speciesName;
				try {
					speciesName = tree.getSpeciesName();
				} catch (JSONException e) {
					speciesName = "No species name";
				}
				plotSpeciesView.setText(speciesName);
			
				if (tree.getDbh() != 0) {
					plotDiameterView.setText(String.valueOf(tree.getDbh()) + " " + getString(R.string.dbh_units));
				} 
				showImage(plot);
				
				zoomToAndMarkCurrentPlot(new LatLng(plot.getGeometry().getLat(), plot.getGeometry().getLon()));
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}
		currentPlot = plot;
		plotPopup.setVisibility(View.VISIBLE);
	}

	public void hidePopup() {
		RelativeLayout plotPopup = (RelativeLayout) findViewById(R.id.plotPopup);
		plotPopup.setVisibility(View.INVISIBLE);
		currentPlot = null;
	}

	private void setPopupViews() {
    	plotSpeciesView = (TextView) findViewById(R.id.plotSpecies);
    	plotAddressView = (TextView) findViewById(R.id.plotAddress);
    	plotDiameterView = (TextView) findViewById(R.id.plotDiameter);
    	plotUpdatedByView = (TextView) findViewById(R.id.plotUpdatedBy);
    	plotImageView = (ImageView) findViewById(R.id.plotImage);
    }

	private void showImage(Plot plot) throws JSONException {
		plot.getTreePhoto(new BinaryHttpResponseHandler(Plot.IMAGE_TYPES) {
			@Override
			public void onSuccess(byte[] imageData) {
				Bitmap scaledImage = Plot.createTreeThumbnail(imageData);
				ImageView plotImage = (ImageView) findViewById(R.id.plotImage);
				plotImage.setImageBitmap(scaledImage);
			}
			
			@Override
			public void onFailure(Throwable e, byte[] imageData) {
				// Log the error, but not important enough to bother the user
				Log.e(App.LOG_TAG, "Could not retreive tree image", e);
			}
		});
	}	  
    
        
    // onClick handler for tree-details pop-up touch event
 	public void showFullTreeInfo(View view) {
 		// Show TreeInfoDisplay with current plot
 		Intent viewPlot = new Intent(MapDisplay.this, TreeInfoDisplay.class);
 		viewPlot.putExtra("plot", currentPlot.getData().toString());
 		
 		if (App.getLoginManager().isLoggedIn()) {
 			viewPlot.putExtra("user", App.getLoginManager().loggedInUser.getData().toString());
 		}
 		startActivityForResult(viewPlot, INFO_INTENT);
 	}
 	
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_map_display, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_filter:
            	Intent filter = new Intent(this, FilterDisplay.class);
            	startActivityForResult(filter, FILTER_INTENT);
            	break;
        }
        return true;
    }
    
    
 	@Override 
 	public void onActivityResult(int requestCode, int resultCode, Intent data) {     
 	  super.onActivityResult(requestCode, resultCode, data); 
 	  switch(requestCode) { 
 	  	case FILTER_INTENT: 
 	  		// This is debugging code!
 	  		// TODO Make filters work.
 	  		if (resultCode == Activity.RESULT_OK) { 
 	  			String activeFilters = App.getFilterManager().getActiveFiltersAsQueryString();
 	  			if (!activeFilters.equals("")) {
 	  				Toast.makeText(this, App.getFilterManager().getActiveFiltersAsQueryString(),
 	  						Toast.LENGTH_LONG).show();
 	  			} else {
 	  				Toast.makeText(this,  "No filters", Toast.LENGTH_LONG).show();
 	  			}
 	  		} 
 	  		break; 
 	  	case INFO_INTENT:
 	  		if (resultCode == TreeDisplay.RESULT_PLOT_EDITED) {
 	  	 
 	  			try {
 	  				// The plot was updated, so update the pop-up with any new data
 	  				Plot updatedPlot = new Plot();
					String plotJSON = data.getExtras().getString("plot");
					updatedPlot.setData(new JSONObject(plotJSON));
					showPopup(updatedPlot);
	 	  	 
		 	  	 } catch (JSONException e) {
		 	  		 		Log.e(App.LOG_TAG, "Unable to deserialze updated plot for map popup", e);
		 	  		 		hidePopup();
		 		 }
  			} else if (resultCode == TreeDisplay.RESULT_PLOT_DELETED) {
  				hidePopup();
  				// TODO: Do we need to refresh the map tile?
 	  	 	}
 	  	 break;
 	  } 
 	}
 	
 	// onClick handler for "My Location" button
    public void showMyLocation(View view) {
    	Context context = MapDisplay.this;
    	LocationManager locationManager = 
    			(LocationManager) context.getSystemService(context.LOCATION_SERVICE);
    	Location lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
    	mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(
    			lastKnownLocation.getLatitude(),
    			lastKnownLocation.getLongitude()
    	), DEFAULT_ZOOM_LEVEL+2));	
    }
    
    @Override
	public void onBackPressed() {
		hidePopup();
	}

	private void zoomToAndMarkCurrentPlot(LatLng point) {
		mMap.animateCamera(CameraUpdateFactory.newLatLng(point));
		if (plotMarker != null) {
			plotMarker.remove();
		}
		plotMarker = mMap.addMarker(new MarkerOptions().position(point).title(""));
	}

	// call backs for base layer switcher buttons
    public void hybridBaselayer(View view) {
    	mMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);
    }
    public void mapBaselayer(View view) {
    	mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);    	
    }
    public void satelliteBaselayer(View view) {
    	mMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
    }
    
    // map click listener.  zoom to selected plot and show popup.
    private OnMapClickListener mapClickListener = new GoogleMap.OnMapClickListener() {	
    	@Override
		public void onMapClick(LatLng point) {		
			Log.d("TREE_CLICK", "(" + point.latitude + "," + point.longitude + ")");
			
			final ProgressDialog dialog = ProgressDialog.show(MapDisplay.this, "", 
                  "Loading. Please wait...", true);
			dialog.show();
			
			final RequestGenerator rg = new RequestGenerator();
			rg.getPlotsNearLocation(
				point.latitude,
				point.longitude,
				new ContainerRestHandler<PlotContainer>(new PlotContainer()) {

					@Override
					public void onFailure(Throwable e, String message) {
						dialog.hide();
						Log.e("TREE_CLICK",
								"Error retrieving plots on map touch event: "
										+ e.getMessage());
						e.printStackTrace();
					}
			
					@Override
					public void dataReceived(PlotContainer response) {
						try {
							Plot plot = response.getFirst();
							if (plot != null) {
								Log.d("TREE_CLICK", "Using Plot (id: " + plot.getId() + ") with coords X: " + plot.getGeometry().getLon() + ", Y:" + plot.getGeometry().getLat());
								showPopup(plot);
							} else {
								hidePopup();
							}
						} catch (JSONException e) {
							Log.e("TREE_CLICK",
									"Error retrieving plot info on map touch event: "
											+ e.getMessage());
							e.printStackTrace();
						} finally {
							dialog.hide();
						}
					}
				});
    	}
    };
 }	
 