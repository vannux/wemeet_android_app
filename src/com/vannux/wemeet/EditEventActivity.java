package com.vannux.wemeet;

import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.json.*;

import android.location.Address;
import android.location.Geocoder;
import android.os.AsyncTask;
import android.os.Bundle;
import android.app.Activity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.*;
import android.widget.AdapterView.OnItemClickListener;
import android.support.v4.app.NavUtils;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

public class EditEventActivity extends Activity {

	private static final String LOG_TAG = "WeMeet";
	private String apikey = null;
	private List<Address> resultList;
	private double longitude;
	private double latitude;
	private Button btnCreaEvento;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_edit_event);
		// Show the Up button in the action bar.
		setupActionBar();
		Intent i = getIntent();
		apikey = i.getStringExtra("apikey");
		
		btnCreaEvento = (Button)findViewById(R.id.btnCreaEvento);
		btnCreaEvento.setOnClickListener(new OnClickListener() {
			public void onClick(View view) { onClickCreaEvento(); }
		});
		
		AutoCompleteTextView autoCompView = (AutoCompleteTextView) findViewById(R.id.fldLocationSearch);
	    autoCompView.setAdapter(new PlacesAutoCompleteAdapter(this, R.layout.list_item));
	    autoCompView.setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
			    // When clicked, show a toast with the TextView text
				longitude = resultList.get(position).getLongitude();
				latitude = resultList.get(position).getLatitude();
			}
		});
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
		getMenuInflater().inflate(R.menu.edit_event, menu);
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
	
	private List<Address> autocomplete(String input) throws IOException {
		
		if ( input.length() < 3 ) return new ArrayList<Address>(0);

		Geocoder geocoder = new Geocoder(this);
		List<Address> addresses = geocoder.getFromLocationName(input, 5);

	    return addresses;
	}
	
	private class PlacesAutoCompleteAdapter extends ArrayAdapter<String> implements Filterable {

	    public PlacesAutoCompleteAdapter(Context context, int textViewResourceId) {
	        super(context, textViewResourceId);
	    }

	    @Override
	    public int getCount() {
	        return resultList.size();
	    }

	    @Override
	    public String getItem(int index) {
	    	StringBuffer retString = new StringBuffer("");
	    	int i = 0;
	    	while ( resultList.get(index).getAddressLine(i) != null ) {
	    		retString.append(resultList.get(index).getAddressLine(i++) + ", ");
	    	}
	    	if (retString.length() > 0) { return retString.substring(0, retString.length()-1); }
	        return retString.toString();
	    }

	    @Override
	    public Filter getFilter() {
	        Filter filter = new Filter() {
	            @Override
	            protected FilterResults performFiltering(CharSequence constraint) {
	                FilterResults filterResults = new FilterResults();
	                if (constraint != null) {
	                    // Retrieve the autocomplete results.
	                    try {
							resultList = autocomplete(constraint.toString());
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}

	                    // Assign the data to the FilterResults
	                    filterResults.values = resultList;
	                    filterResults.count = resultList.size();
	                }
	                return filterResults;
	            }

	            @Override
	            protected void publishResults(CharSequence constraint, FilterResults results) {
	                if (results != null && results.count > 0) {
	                    notifyDataSetChanged();
	                }
	                else {
	                    notifyDataSetInvalidated();
	                }
	            }};
	        return filter;
	    }
	}
	
	private void onClickCreaEvento() {
		
	}
	
	private class LongRunningGetIO extends AsyncTask <Void, Void, String> {
		protected String getASCIIContentFromEntity(HttpEntity entity) throws IllegalStateException, IOException {
			InputStream in = entity.getContent();
			StringBuffer out = new StringBuffer();
			int n = 1;
			while (n>0) {
				byte[] b = new byte[4096];
				n =  in.read(b);
				if (n>0) out.append(new String(b, 0, n));
			}
			return out.toString();
		}

		@Override
		protected String doInBackground(Void... params) {
			HttpClient httpClient = new DefaultHttpClient();
			HttpContext localContext = new BasicHttpContext();
			HttpPut httpPut = new HttpPut();
			String text = null;
			try {
				HttpResponse response = httpClient.execute(httpPut, localContext);
				HttpEntity entity = response.getEntity();
				text = getASCIIContentFromEntity(entity);
			} catch (Exception e) {
				return e.getLocalizedMessage();
			}
			return text;
		}

		protected void onPostExecute(String results) {
			if (results!=null) {
				try {
					JSONObject jObject = new JSONObject(results);
					apikey = jObject.getString("apikey");
				} catch (JSONException e) {
					e.printStackTrace();
				}
			}
		}
	}
}
