package com.vannux.wemeet;

import java.io.IOException;
import java.io.InputStream;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.json.JSONException;
import org.json.JSONObject;

import com.facebook.LoggingBehavior;
import com.facebook.Session;
import com.facebook.SessionState;
import com.facebook.Settings;

import android.os.AsyncTask;
import android.os.Bundle;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

public class MainActivity extends Activity {

	private Button buttonLoginLogout;
	private Button buttonMap;
	private Button buttonEventsList;
	private Session.StatusCallback fbLoginstatusCallback = new SessionStatusCallback();
	private static String apikey = null;
	private static String auth_token = null;
	private LongRunningGetIO loginApiCall = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		buttonLoginLogout = (Button)findViewById(R.id.buttonLoginLogout);
		buttonMap = (Button)findViewById(R.id.mapButton);
		buttonMap.setOnClickListener(new OnClickListener() {
			public void onClick(View view) { onClickMapEvents(); }
		});
		buttonMap.setEnabled(false);
		buttonEventsList = (Button)findViewById(R.id.eventsButton);
		buttonEventsList.setOnClickListener(new OnClickListener() {
			public void onClick(View view) { onClickEventsList(); }
		});
		buttonEventsList.setEnabled(false);
		
		if (apikey == null) {
			Settings.addLoggingBehavior(LoggingBehavior.INCLUDE_ACCESS_TOKENS);

			Session session = Session.getActiveSession();
			if (session == null) {
				if (savedInstanceState != null) {
					session = Session.restoreSession(this, null, fbLoginstatusCallback, savedInstanceState);
				}
				if (session == null) {
					session = new Session(this);
				}
				Session.setActiveSession(session);
				if (session.getState().equals(SessionState.CREATED_TOKEN_LOADED)) {
					session.openForRead(new Session.OpenRequest(this).setCallback(fbLoginstatusCallback));
				}
			}
			
		}
		updateView();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	private void updateView() {
		if (apikey == null) {
			Session session = Session.getActiveSession();
			if (session.isOpened()) {
				auth_token = session.getAccessToken();
				//Chiamata alla login tramite RESTAPI e memorizzazione apikey
				if ( loginApiCall == null) {
					loginApiCall = new LongRunningGetIO();
				}
				if (loginApiCall.getStatus() != AsyncTask.Status.RUNNING) {
					loginApiCall.execute();
				}
			} else {
				apikey = null;
				setState( false );
			}
		} else {
			setState( true );
		}
	}
	
	private void setState(boolean logged) {
		buttonLoginLogout.setText((logged ? R.string.logout : R.string.facebook_login));
		if ( logged ) {
			buttonLoginLogout.setOnClickListener(new OnClickListener() {
				public void onClick(View view) { onClickLogout(); }
			});
		} else {
			buttonLoginLogout.setOnClickListener(new OnClickListener() {
				public void onClick(View view) { onClickLogin(); }
			});
		}
		buttonMap.setEnabled(logged);
		buttonEventsList.setEnabled(logged);
	}
	
	private void onClickLogin() {
		Session session = Session.getActiveSession();
		if (!session.isOpened() && !session.isClosed()) {
			session.openForRead(new Session.OpenRequest(this).setCallback(fbLoginstatusCallback));
		} else {
			Session.openActiveSession(this, true, fbLoginstatusCallback);
		}
	}
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		Session.getActiveSession()
		.onActivityResult(this, requestCode, resultCode, data);
	}
	private void onClickLogout() {
		apikey = null;
		Session session = Session.getActiveSession();
		if (!session.isClosed()) {
			session.closeAndClearTokenInformation();
		}
		updateView();
	}

	private class SessionStatusCallback implements Session.StatusCallback {
		@Override
		public void call(Session session, SessionState state, Exception exception) {
			updateView();
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.logout:
			onClickLogout();
			return true;
		default:
			return super.onContextItemSelected(item);
		}
	}
	private class LongRunningGetIO extends AsyncTask <Void, Void, String> {
		
		private ProgressDialog dialog = new ProgressDialog(MainActivity.this);
		
		/** progress dialog to show user that the backup is processing. */
	    /** application context. */
	    @Override
	    protected void onPreExecute() {
	        this.dialog.setMessage("Checking facebook login");
	        this.dialog.show();
	    }
	    
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
			HttpGet httpGet = new HttpGet( getResources().getString(R.string.apiCommonUrl) + "/logins/" +
					auth_token + "/facebook");
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
					JSONObject jObject = new JSONObject(results);
					apikey = jObject.getString("apikey");
					setState( true );
				} catch (JSONException e) {
					e.printStackTrace();
				}
			}
			if (dialog.isShowing()) {
	            dialog.dismiss();
	        }
		}
	}
	
	private void onClickMapEvents() {
		Intent mapEventIntent = new Intent(getApplicationContext(), MapEventsActivity.class); /** Class name here */
		mapEventIntent.putExtra("apikey", apikey);
		startActivity( mapEventIntent );
	}
	
	private void onClickEventsList() {
		Intent mapEventIntent = new Intent(getApplicationContext(), EventsListActivity.class); /** Class name here */
		mapEventIntent.putExtra("apikey", apikey);
		startActivity( mapEventIntent );
	}
}
