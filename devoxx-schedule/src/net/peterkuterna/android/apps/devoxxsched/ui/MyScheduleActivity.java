/*
 * Copyright 2010 Peter Kuterna
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

package net.peterkuterna.android.apps.devoxxsched.ui;

import java.util.ArrayList;

import net.peterkuterna.android.apps.devoxxsched.Constants;
import net.peterkuterna.android.apps.devoxxsched.R;
import net.peterkuterna.android.apps.devoxxsched.util.SyncUtils;
import net.peterkuterna.android.apps.devoxxsched.util.UIUtils;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

/**
 * {@link Activity} that handles the registration of the Devoxx MySchedule functionality.
 */
public class MyScheduleActivity extends Activity {

    private static final String TAG = "MyScheduleActivity";
    
    private static final String EMAIL_REGEX =  "^[_A-Za-z0-9-]+(\\.[_A-Za-z0-9-]+)*@[A-Za-z0-9-]+(\\.[A-Za-z0-9-]+)*(\\.[_A-Za-z0-9-]+)";

    private View mRegisterView;
    private View mRegisteredView;
    private EditText mFirstName;
	private EditText mLastName;
	private EditText mEmail;
	private EditText mActivationCode;
	private Button mCancelButton;
	private Button mClearButton;
	private Button mOkButton;
	private Button mRegisterButton;
	
	private SharedPreferences mPrefs;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_myschedule);
		
        ((TextView) findViewById(R.id.title_text)).setText(getTitle());
        
		mPrefs = getSharedPreferences(MySchedulePrefs.DEVOXXSCHED_MYSCHEDULE, Context.MODE_PRIVATE);
		
		mRegisterView = findViewById(R.id.register);
		mRegisteredView = findViewById(R.id.registered);
		
		mFirstName = (EditText) findViewById(R.id.myschedule_firstname);
        mLastName = (EditText) findViewById(R.id.myschedule_lastname);
        mEmail = (EditText) findViewById(R.id.myschedule_email);
        mActivationCode = (EditText) findViewById(R.id.myschedule_activationcode);
        mCancelButton = (Button) findViewById(R.id.cancel_btn);
        mClearButton = (Button) findViewById(R.id.clear_btn);
        mOkButton = (Button) findViewById(R.id.ok_btn);
        mRegisterButton = (Button) findViewById(R.id.register_btn);

        final TextWatcher textWatcher = new MyScheduleTextWatcher();
        mFirstName.addTextChangedListener(textWatcher);
        mLastName.addTextChangedListener(textWatcher);
        mEmail.addTextChangedListener(textWatcher);
        mActivationCode.addTextChangedListener(textWatcher);
        
        mCancelButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				setResult(RESULT_CANCELED);
				finish();
			}
		});
        
        mClearButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
		    	final String activationCode = mPrefs.getString(MySchedulePrefs.ACTIVATION_CODE, null);
	    		final boolean activationCodeOk = activationCode != null && activationCode.length() > 0;
	    		if (activationCodeOk) {
					clearPreferences();
					updateUI(true);
	    		} else {
					setResult(RESULT_CANCELED);
					finish();
	    		}
			}
		});
        
        mRegisterButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				savePreferences();
				new RegisterTask().execute();
			}
		});
        
        mOkButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				savePreferences();
				setResult(RESULT_OK);
				finish();
			}
		});

        updateUI(true);
	}

	@Override
	protected void onResume() {
		super.onResume();
		updateUI(false);
	}

	public void onHomeClick(View v) {
        UIUtils.goHome(this);
    }
    
    private void updateUI(boolean copyFromPreferences) {
    	final String firstName = mPrefs.getString(MySchedulePrefs.FIRST_NAME, null);
    	final String lastName = mPrefs.getString(MySchedulePrefs.LAST_NAME, null);
    	final String email = mPrefs.getString(MySchedulePrefs.EMAIL, null);
    	final String activationCode = mPrefs.getString(MySchedulePrefs.ACTIVATION_CODE, null);
    	final boolean registered = mPrefs.getBoolean(MySchedulePrefs.REGISTERED, false);

    	if (copyFromPreferences) {
			mFirstName.setText(firstName);
			mLastName.setText(lastName);
			mEmail.setText(email);
			mActivationCode.setText(activationCode);
    	}

    	final boolean activationCodeOk = activationCode != null && activationCode.length() > 0;
    	mActivationCode.setEnabled(activationCodeOk ? false : true);
    	mOkButton.setEnabled(activationCodeOk ? false : true);
    	mClearButton.setText(activationCodeOk ? R.string.btn_myschedule_clear : android.R.string.cancel);

    	mRegisterView.setVisibility(registered ? View.GONE : View.VISIBLE);
    	mRegisteredView.setVisibility(registered ? View.VISIBLE : View.GONE);
    }
    
    private void savePreferences() {
    	mPrefs.edit().putString(MySchedulePrefs.FIRST_NAME, mFirstName.getText().toString().trim()).commit();
    	mPrefs.edit().putString(MySchedulePrefs.LAST_NAME, mLastName.getText().toString().trim()).commit();
    	mPrefs.edit().putString(MySchedulePrefs.EMAIL, mEmail.getText().toString().trim()).commit();
    	mPrefs.edit().putString(MySchedulePrefs.ACTIVATION_CODE, mActivationCode.getText().toString().trim()).commit();
    }
    
    private void clearPreferences() {
    	mActivationCode.setText(null);
    	savePreferences();
    	mPrefs.edit().remove(MySchedulePrefs.REGISTERED).commit();
    }
    
    public interface MySchedulePrefs {
        String DEVOXXSCHED_MYSCHEDULE = "devoxxsched_myschedule";
        String FIRST_NAME = "first_name";
        String LAST_NAME = "last_name";
        String EMAIL = "email";
        String ACTIVATION_CODE = "activation_code";
        String REGISTERED = "registered";
    }

    private class MyScheduleTextWatcher implements TextWatcher {

		@Override
		public void afterTextChanged(Editable s) {
			final boolean mFirstNameOk = mFirstName.getText().toString() != null && mFirstName.getText().toString().trim().length() > 0; 
			final boolean mLastNameOk = mLastName.getText().toString() != null && mLastName.getText().toString().trim().length() > 0; 
			final boolean mEmailOk = mEmail.getText().toString() != null && mEmail.getText().toString().trim().length() > 0 && mEmail.getText().toString().trim().matches(EMAIL_REGEX); 
			final boolean mActivationCodeOk = mActivationCode.getText().toString() != null && mActivationCode.getText().toString().trim().length() > 0;
			mRegisterButton.setEnabled(mFirstNameOk && mLastNameOk && mEmailOk);
			mOkButton.setEnabled(mActivationCodeOk);
		}

		@Override
		public void beforeTextChanged(CharSequence s, int start, int count, int after) {
		}

		@Override
		public void onTextChanged(CharSequence s, int start, int before, int count) {
		}
    	
    }
    
    private static HttpClient sHttpClient;

    private static synchronized HttpClient getHttpClient(Context context) {
        if (sHttpClient == null) {
            sHttpClient = SyncUtils.getHttpClient(context);
        }
        return sHttpClient;
    }
    
    private class RegisterTask extends AsyncTask<Void, Void, Void> {

    	private ProgressDialog mDialog;  
    	private boolean mResultOk = false;
        
        protected void onPreExecute() {
        	mDialog = ProgressDialog.show(MyScheduleActivity.this, null, "Registrating...", true, false);
        }  

        @Override
        protected Void doInBackground(Void... params) {
            try {
                final Context context = MyScheduleActivity.this;
                final HttpClient httpClient = getHttpClient(context);
                final HttpPost httpPost = new HttpPost(Constants.MYSCHEDULE_ACTIVATE_URL);
                
                final ArrayList<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
                nameValuePairs.add(new BasicNameValuePair("firstname", mFirstName.getText().toString()));
                nameValuePairs.add(new BasicNameValuePair("lastname", mLastName.getText().toString()));
                nameValuePairs.add(new BasicNameValuePair("email", mEmail.getText().toString()));
                httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs));

                final HttpResponse resp = httpClient.execute(httpPost);
                final int statusCode = resp.getStatusLine().getStatusCode();
                
                if (statusCode != HttpStatus.SC_CREATED) {
                	mResultOk = false;
                	return null;
                }
                
                mResultOk = true;
            } catch (Exception e) {
            	Log.e(TAG, e.getMessage());
            	mResultOk = false;
            	cancel(true);
            }
            
            return null;
        }

        protected void onPostExecute(Void unused) {  
            mDialog.dismiss();
            
            if (mResultOk) {
                Toast.makeText(MyScheduleActivity.this, getResources().getText(R.string.myschedule_registration_ok), Toast.LENGTH_LONG).show();  
            	mPrefs.edit().putBoolean(MySchedulePrefs.REGISTERED, true).commit();
            	updateUI(true);
            } else {  
                Toast.makeText(MyScheduleActivity.this, getResources().getText(R.string.myschedule_registration_nok), Toast.LENGTH_LONG).show();  
            }  
        }  

    }

}
