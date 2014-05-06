package com.vannux.wemeet;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.location.Address;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.NavUtils;
import android.text.format.DateFormat;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TimePicker;

public class EditEventActivity extends FragmentActivity {

	private String apikey = null;
	private long eventId = -1;
	private List<Address> resultList;
	private double longitude;
	private double latitude;
	private Button btnOk;
	
	private LongRunningSetEvent okApiCall = null;
	private LongRunningGetEvent eventLoader = null;
	
	private EditText fldEventName;
	private EditText fldMultiEventDescription;
	private EditText fldEventDate;
	private EditText fldEventTime;
	private EditText fldEventCity;
	private EditText fldLocationSearch;
	private CheckBox chkEventPublic;

	@SuppressLint("CutPasteId")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_edit_event);
		// Show the Up button in the action bar.
		setupActionBar();
		Intent i = getIntent();
		eventId = i.getLongExtra("eventId", -1);
		apikey = i.getStringExtra("apikey");

		btnOk = (Button)findViewById(R.id.btnOk);
		btnOk.setOnClickListener(new OnClickListener() {
			public void onClick(View view) { onClickOk(); }
		});

		fldEventName = (EditText) findViewById(R.id.fldEventName);
		fldMultiEventDescription = (EditText) findViewById(R.id.fldMultiEventDescription);
		fldEventDate = (EditText) findViewById(R.id.fldEventDate);
		fldEventTime = (EditText) findViewById(R.id.fldEventTime);
		fldEventCity = (EditText) findViewById(R.id.fldEventCity);
		fldLocationSearch = (EditText) findViewById(R.id.fldLocationSearch);
		chkEventPublic = (CheckBox) findViewById(R.id.chkEventPublic);

		if (eventId != -1) {
			loadEvent();
		}
		
		AutoCompleteTextView autoCompView = (AutoCompleteTextView) findViewById(R.id.fldLocationSearch);
		autoCompView.setAdapter(new PlacesAutoCompleteAdapter(this, R.layout.list_item));
		autoCompView.setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				// When clicked, show a toast with the TextView text
				longitude = resultList.get(position).getLongitude();
				latitude = resultList.get(position).getLatitude();
				fldLocationSearch.setText(resultList.get(position).getAddressLine(1));
				fldEventCity.setText(resultList.get(position).getLocality());
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

	public List<Address> autocomplete(String address)
			throws JSONException, UnsupportedEncodingException {

		StringBuffer searchString = new StringBuffer(address);
		if ( fldEventCity.getText().toString().trim().length() > 0 ) {
			searchString.append(" " + fldEventCity.getText().toString().trim());
		}
		HttpGet httpGet = new HttpGet(
				"http://maps.google.com/maps/api/geocode/json?address="
						+ URLEncoder.encode(searchString.toString(),"UTF-8") + "&sensor=true");
	
		HttpParams params = new BasicHttpParams();
		HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);
		HttpProtocolParams.setContentCharset(params, "utf-8");
		HttpClient client = new DefaultHttpClient(params);

		HttpResponse response;
		StringBuilder stringBuilder = new StringBuilder();

		try {
			response = client.execute(httpGet);
			HttpEntity entity = response.getEntity();
			stringBuilder.append(EntityUtils.toString(entity, HTTP.UTF_8));
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
				Address addr = new Address(Locale.getDefault());
				addr.setAddressLine(0, result.getString("formatted_address"));
				addr.setAddressLine(1, getAddress(result));
				addr.setLocality(getLocality(result));
				double lat = result.getJSONObject("geometry").getJSONObject("location").getDouble("lat");
				double lon = result.getJSONObject("geometry").getJSONObject("location").getDouble("lng");
				addr.setLatitude(lat);
				addr.setLongitude(lon);
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
					if ((constraint != null) && (constraint.length() > 2)) {
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
	
	private void loadEvent() {
		eventLoader = new LongRunningGetEvent();
		eventLoader.execute();
	}

	private void onClickOk() {
		okApiCall = new LongRunningSetEvent();
		okApiCall.execute();
	}
	
	private class LongRunningGetEvent extends AsyncTask<Void, Void, String> {

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
			HttpPost httpPost = new HttpPost( getResources().getString(R.string.apiCommonUrl) + "/parties/" + apikey);
			httpPost.addHeader("Accept", "application/json");
			
			String text = null;
			try {
				JSONObject jsonObj = new JSONObject();
				jsonObj.put("id", eventId);
				
				List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(1);
				nameValuePairs.add(new BasicNameValuePair("jsonPayload", jsonObj.toString()));
				httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
				HttpResponse response = httpClient.execute(httpPost);
				HttpEntity entity = response.getEntity();
				text = getASCIIContentFromEntity(entity);
			} catch (Exception e) {
				return e.getLocalizedMessage();
			}
			return text;
		}

		@SuppressLint("SimpleDateFormat")
		protected void onPostExecute(String results) {
			if (results!=null) {
				try {
					java.text.DateFormat m_ISO8601Local = new SimpleDateFormat ("yyyy-MM-dd'T'HH:mm:ssZ", Locale.US);
					SimpleDateFormat fieldTimeFormat = new SimpleDateFormat("hh:mm");
					java.text.DateFormat fieldDateFormat = java.text.DateFormat.getDateInstance();
					JSONArray jsonarr = new JSONArray(results);
					if (jsonarr.length() > 0) {
						JSONObject jObject = jsonarr.getJSONObject(0);
						
						eventId = jObject.getLong("id");
						fldEventName.setText(jObject.getString("name"));
						fldMultiEventDescription.setText(jObject.getString("description"));
						fldEventDate.setText(fieldDateFormat.format(m_ISO8601Local.parse(jObject.getString("eventdate"))));
						fldEventTime.setText(fieldTimeFormat.format(m_ISO8601Local.parse(jObject.getString("eventdate"))));
						fldEventCity.setText(jObject.getString("city"));
						fldLocationSearch.setText(jObject.getString("location"));
						latitude = jObject.getDouble("geolat");
						longitude  = jObject.getDouble("geolon");
						chkEventPublic.setChecked((jObject.getString("public").equalsIgnoreCase("Y") ? true : false));
					}
				} catch (JSONException e) {
					e.printStackTrace();
				} catch (ParseException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		
	}

	private class LongRunningSetEvent extends AsyncTask <Void, Void, String> {
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

		@SuppressLint("SimpleDateFormat")
		@Override
		protected String doInBackground(Void... params) {
			HttpClient httpClient = new DefaultHttpClient();
			//HttpContext localContext = new BasicHttpContext();
			HttpPut httpPut = new HttpPut( getResources().getString(R.string.apiCommonUrl) + "/parties/" + apikey);
			//httpPut.addHeader("Content-Type", "application/json");
			httpPut.addHeader("Accept", "application/json");

			String text = null;
			try {
				java.text.DateFormat inputDate = java.text.DateFormat.getDateInstance();
				SimpleDateFormat outputDate = new SimpleDateFormat("MM/dd/yyyy");
				// Add your data
				JSONObject jsonObj = new JSONObject();
				if (eventId == -1) {
					jsonObj.put("id", "");
				} else {
					jsonObj.put("id", eventId);
				}
				jsonObj.put("name", fldEventName.getText().toString());
				jsonObj.put("description", fldMultiEventDescription.getText().toString());
				jsonObj.put("eventdate", outputDate.format(inputDate.parse(fldEventDate.getText().toString())) + " " + 
						fldEventTime.getText().toString());
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
	
	/**
	 * TIME PICKER
	 */
	public void showTimePickerDialog(View v) {
		TimePickerFragment newFragment = new TimePickerFragment();
	    newFragment.setField(fldEventTime);
	    newFragment.show(getSupportFragmentManager(), "timePicker");
	}
	
	public static class TimePickerFragment extends DialogFragment
		implements TimePickerDialog.OnTimeSetListener {

		EditText fld = null;
		
		@Override
		public Dialog onCreateDialog(Bundle savedInstanceState) {
			// Use the current time as the default values for the picker
			final Calendar c = Calendar.getInstance();
			int hour = c.get(Calendar.HOUR_OF_DAY);
			int minute = c.get(Calendar.MINUTE);

			// Create a new instance of TimePickerDialog and return it
			return new TimePickerDialog(getActivity(), this, hour, minute,
					DateFormat.is24HourFormat(getActivity()));
		}

		@SuppressLint("SimpleDateFormat")
		public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
			// Do something with the time chosen by the user
			SimpleDateFormat df = new SimpleDateFormat("hh:mm");
			Calendar c = GregorianCalendar.getInstance();
			c.set(Calendar.HOUR_OF_DAY, hourOfDay);
			c.set(Calendar.MINUTE, minute);
			fld.setText(df.format(c.getTime()));
		}
		
		public void setField(EditText fld) {
			this.fld = fld;
		}
	}
	
	/**
	 * DATE PICKER
	 */
	public void showDatePickerDialog(View v) {
		DatePickerFragment newFragment = new DatePickerFragment();
	    newFragment.setField(fldEventDate);
	    newFragment.show(getSupportFragmentManager(), "datePicker");
	}
	
	public static class DatePickerFragment extends DialogFragment
	implements DatePickerDialog.OnDateSetListener {

		EditText fld = null;
		
		@Override
		public Dialog onCreateDialog(Bundle savedInstanceState) {
			// Use the current date as the default date in the picker
			final Calendar c = Calendar.getInstance();
			int year = c.get(Calendar.YEAR);
			int month = c.get(Calendar.MONTH);
			int day = c.get(Calendar.DAY_OF_MONTH);

			// Create a new instance of DatePickerDialog and return it
			return new DatePickerDialog(getActivity(), this, year, month, day);
		}

		public void onDateSet(DatePicker view, int year, int month, int day) {
			// Do something with the date chosen by the user
			java.text.DateFormat df = java.text.DateFormat.getDateInstance();
			Calendar d = GregorianCalendar.getInstance();
			d.set(year, month, day);
			fld.setText(df.format(d.getTime()));
		}
		
		public void setField(EditText fld) {
			this.fld = fld;
		}
	}
	
	private String getAddress(JSONObject json) throws JSONException {
		JSONArray address_components = json.getJSONArray("address_components");
		String streetNumber = null;
		String address = null;

		for (int i = 0; i < address_components.length(); i++) {
			JSONObject jobj = address_components.getJSONObject(i);
			if ( "street_number".equalsIgnoreCase(jobj.getJSONArray("types").get(0).toString()) ) {
				streetNumber = jobj.getString("long_name");
			} else if ( "route".equalsIgnoreCase(jobj.getJSONArray("types").get(0).toString()) ) {
				address = jobj.getString("long_name");
			}
		}
		if ((address != null) && (streetNumber != null)) {
			address += ", " + streetNumber;
		}
		return address;
	}
	
	private String getLocality(JSONObject json) throws JSONException {
		JSONArray address_components = json.getJSONArray("address_components");
		String locality = null;

		for (int i = 0; i < address_components.length(); i++) {
			JSONObject jobj = address_components.getJSONObject(i);
			if ( "locality".equalsIgnoreCase(jobj.getJSONArray("types").get(0).toString()) ) {
				locality = jobj.getString("long_name");
			}
		}
		
		return locality;
	}
}
