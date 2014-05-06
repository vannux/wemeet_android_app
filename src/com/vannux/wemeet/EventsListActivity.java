package com.vannux.wemeet;

import java.io.IOException;
import java.io.InputStream;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Locale;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.os.AsyncTask;
import android.os.Bundle;
import android.app.Activity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.support.v4.app.NavUtils;
import android.annotation.TargetApi;
import android.content.Intent;
import android.os.Build;

import com.vannux.wemeet.entities.*;

public class EventsListActivity extends Activity {

	private String apikey = null;
	private ListView listEvents = null;
	private ArrayAdapter<Event> la = null;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_events_list);
		// Show the Up button in the action bar.
		setupActionBar();
		Intent i = getIntent();
		apikey = i.getStringExtra("apikey");
		listEvents = (ListView) findViewById(R.id.listEvents);
		la = new ArrayAdapter<Event>(this, R.layout.simplerow);
		listEvents.setAdapter( la );
		listEvents.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int position,
					long arg3) {
				Event event = (Event) listEvents.getItemAtPosition(position);
				Intent editEventIntent = new Intent(getApplicationContext(), EditEventActivity.class); /** Class name here */
				editEventIntent.putExtra("eventId", event.getId());
				editEventIntent.putExtra("apikey", apikey);
				startActivity( editEventIntent );
			}
		});
		//Carico Lista Eventi
		loadEvents();
	}
	
	private void loadEvents() {
		new LongRunningGetIO().execute();
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
		getMenuInflater().inflate(R.menu.events_list, menu);
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
		case R.id.create_event:
			//Apre maschera creazione evento
			Intent mapEventIntent = new Intent(getApplicationContext(), EditEventActivity.class); /** Class name here */
			mapEventIntent.putExtra("apikey", apikey);
			startActivity( mapEventIntent );
			return true;
		}
			
		return super.onOptionsItemSelected(item);
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
			HttpGet httpGet = new HttpGet( getResources().getString(R.string.apiCommonUrl) + "/parties/" +
					apikey + "/all");
			String text = null;
			try {
				HttpResponse response = httpClient.execute(httpGet, localContext);
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
					JSONArray jsonarr = new JSONArray(results);
					DateFormat m_ISO8601Local = new SimpleDateFormat ("yyyy-MM-dd'T'HH:mm:ssZ", Locale.US);
					for(int i = 0; i < jsonarr.length(); i++){
						Event event = new Event();
						JSONObject jObject = jsonarr.getJSONObject(i);
						event.setId(jObject.getLong("id"));
						event.setCreationdate(m_ISO8601Local.parse(jObject.getString("creationdate")));
						event.setEventdate(m_ISO8601Local.parse(jObject.getString("eventdate")));
						event.setIsPublic(jObject.getString("public"));
						event.setName(jObject.getString("name"));
						event.setCity(jObject.getString("city"));
						event.setGeolat(jObject.getDouble("geolat"));
						event.setGeolon(jObject.getDouble("geolon"));
						event.setLocation(jObject.getString("location"));
						//event.setCreatedby(jObject.getLong("createdby"));
						la.add(event);
					}
					la.notifyDataSetChanged();
				} catch (JSONException e) {
					e.printStackTrace();
				} catch (ParseException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}
}
