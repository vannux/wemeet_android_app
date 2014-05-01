package com.vannux.wemeet;

import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HTTP;
import org.apache.http.protocol.HttpContext;
import org.json.*;

import com.google.android.gms.maps.model.LatLng;

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
	private Button btnOk;
	private LongRunningGetIO okApiCall = null;
	private EditText fldEventName;
	private EditText fldMultiEventDescription;
	private EditText fldEventDate;
	private EditText fldEventCity;
	private EditText fldLocationSearch;
	private CheckBox chkEventPublic;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_edit_event);
		// Show the Up button in the action bar.
		setupActionBar();
		Intent i = getIntent();
		apikey = i.getStringExtra("apikey");

		btnOk = (Button)findViewById(R.id.btnOk);
		btnOk.setOnClickListener(new OnClickListener() {
			public void onClick(View view) { onClickOk(); }
		});

		fldEventName = (EditText) findViewById(R.id.fldEventName);
		fldMultiEventDescription = (EditText) findViewById(R.id.fldMultiEventDescription);
		fldEventDate = (EditText) findViewById(R.id.fldEventDate);
		fldEventCity = (EditText) findViewById(R.id.fldEventCity);
		fldLocationSearch = (EditText) findViewById(R.id.fldLocationSearch);
		chkEventPublic = (CheckBox) findViewById(R.id.chkEventPublic);

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

	public static List<Address> autocomplete(String address)
			throws JSONException, UnsupportedEncodingException {

		HttpGet httpGet = new HttpGet(
				"http://maps.google.com/maps/api/geocode/json?address="
						+ URLEncoder.encode(address,"UTF-8") + "&sensor=true");
		HttpClient client = new DefaultHttpClient();
		HttpResponse response;
		StringBuilder stringBuilder = new StringBuilder();

		try {
			response = client.execute(httpGet);
			HttpEntity entity = response.getEntity();
			InputStream stream = entity.getContent();
			int b;
			while ((b = stream.read()) != -1) {
				stringBuilder.append((char) b);
			}
		} catch (ClientProtocolException e) {
		} catch (IOException e) {
		}

		JSONObject jsonObject = new JSONObject();
		jsonObject = new JSONObject(stringBuilder.toString());

		List<Address> retList = null;
		retList = new ArrayList<Address>();

		if ("OK".equalsIgnoreCase(jsonObject.getString("status"))) {
			JSONArray results = jsonObject.getJSONArray("results");
			for (int i = 0; i < results.length(); i++) {
				JSONObject result = results.getJSONObject(i);
				String indiStr = result.getString("formatted_address");
				Address addr = new Address(Locale.getDefault());
				addr.setAddressLine(0, indiStr);
				retList.add(addr);
			}
		}

		return retList;
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
			if (resultList.size() > index) {
				while ( resultList.get(index).getAddressLine(i) != null ) {
					retString.append(resultList.get(index).getAddressLine(i++) + ", ");
				}
				if (retString.length() > 0) { return retString.substring(0, retString.length()-1); }
			}
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
						} catch (Exception e) {
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

	private void onClickOk() {
		okApiCall = new LongRunningGetIO();
		okApiCall.execute();
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
			//HttpContext localContext = new BasicHttpContext();
			HttpPut httpPut = new HttpPut( getResources().getString(R.string.apiCommonUrl) + "/parties/" + apikey);
			//httpPut.addHeader("Content-Type", "application/json");
			httpPut.addHeader("Accept", "application/json");

			String text = null;
			try {
				SimpleDateFormat inputDate = new SimpleDateFormat("dd/MM/yyyy");
				SimpleDateFormat outputDate = new SimpleDateFormat("MM/dd/yyyy");
				// Add your data
				JSONObject jsonObj = new JSONObject();
				jsonObj.put("id", "");
				jsonObj.put("name", fldEventName.getText().toString());
				jsonObj.put("description", fldMultiEventDescription.getText().toString());
				jsonObj.put("eventdate", outputDate.format(inputDate.parse(fldEventDate.getText().toString())));
				jsonObj.put("city", fldEventCity.getText().toString());
				jsonObj.put("location", fldLocationSearch.getText().toString());
				jsonObj.put("geolat", Double.toString(latitude));
				jsonObj.put("geolon", Double.toString(longitude));
				jsonObj.put("public", (chkEventPublic.isChecked() ? "Y" : "N"));


				List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(1);
				nameValuePairs.add(new BasicNameValuePair("jsonPayload", jsonObj.toString()));
				httpPut.setEntity(new UrlEncodedFormEntity(nameValuePairs));


				//StringEntity se = new StringEntity(jsonObj.toString());
				//se.setContentType(new BasicHeader(HTTP.CONTENT_TYPE, "application/json"));
				//httpPut.setEntity(se);
				HttpResponse response = httpClient.execute(httpPut);
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
