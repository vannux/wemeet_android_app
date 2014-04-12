package com.vannux.wemeet;

import java.io.IOException;
import java.util.List;




import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;

import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.NavUtils;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Intent;
import android.os.Build;

public class MapEventsActivity extends FragmentActivity {

	private String apikey = null;
	private Button buttonSearch;
	private EditText fldLocationSearch;
	private GoogleMap map;
	
	@SuppressLint("NewApi")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_map_events);
		// Show the Up button in the action bar.
		setupActionBar();
		Intent i = getIntent();
		apikey = i.getStringExtra("apikey");
		System.out.println(apikey);
		buttonSearch = (Button)findViewById(R.id.buttonSearch);
		buttonSearch.setOnClickListener(new OnClickListener() {
			public void onClick(View view) { try {
				onClickButtonSearch();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} }
		});
		fldLocationSearch = (EditText) findViewById(R.id.fldLocationSearch);
		map = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map)).getMap();
	}

	/**
	 * Set up the {@link android.app.ActionBar}, if the API is available.
	 */
	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	private void setupActionBar() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			getActionBar().setDisplayHomeAsUpEnabled(true);
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.map_events, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			// This ID represents the Home or Up button. In the case of this
			// activity, the Up button is shown. Use NavUtils to allow users
			// to navigate up one level in the application structure. For
			// more details, see the Navigation pattern on Android Design:
			//
			// http://developer.android.com/design/patterns/navigation.html#up-vs-back
			//
			NavUtils.navigateUpFromSameTask(this);
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
	
	private void onClickButtonSearch() throws IOException {
		Geocoder geocoder = new Geocoder(this);
		List<Address> addresses = geocoder.getFromLocationName(fldLocationSearch.getText().toString(), 5);
		if ( addresses.size() > 0 ) {
			Address address = addresses.get(0);
			
			map.moveCamera( CameraUpdateFactory.newLatLngZoom(new LatLng(address.getLatitude(), address.getLongitude()), map.getCameraPosition().zoom) );
		}
	}
}
