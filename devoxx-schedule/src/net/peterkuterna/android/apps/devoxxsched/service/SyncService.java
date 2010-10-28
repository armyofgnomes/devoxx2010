/*
 * Copyright 2010 Google Inc.
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

/*
 * Modified by Peter Kuterna to support the Devoxx REST API.
 */
package net.peterkuterna.android.apps.devoxxsched.service;

import java.util.ArrayList;

import net.peterkuterna.android.apps.devoxxsched.Constants;
import net.peterkuterna.android.apps.devoxxsched.R;
import net.peterkuterna.android.apps.devoxxsched.io.LocalExecutor;
import net.peterkuterna.android.apps.devoxxsched.io.LocalSearchSuggestHandler;
import net.peterkuterna.android.apps.devoxxsched.io.RemoteExecutor;
import net.peterkuterna.android.apps.devoxxsched.io.RemoteRoomsHandler;
import net.peterkuterna.android.apps.devoxxsched.io.RemoteScheduleHandler;
import net.peterkuterna.android.apps.devoxxsched.io.RemoteSessionsHandler;
import net.peterkuterna.android.apps.devoxxsched.io.RemoteSpeakersHandler;
import net.peterkuterna.android.apps.devoxxsched.model.RequestHash;
import net.peterkuterna.android.apps.devoxxsched.provider.ScheduleProvider;
import net.peterkuterna.android.apps.devoxxsched.ui.SettingsActivity;
import net.peterkuterna.android.apps.devoxxsched.util.NotificationUtils;
import net.peterkuterna.android.apps.devoxxsched.util.SyncUtils;

import org.apache.http.client.HttpClient;

import android.app.IntentService;
import android.app.Service;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.NetworkInfo.State;
import android.os.Bundle;
import android.os.ResultReceiver;
import android.util.Log;

/**
 * Background {@link Service} that synchronizes data living in
 * {@link ScheduleProvider}. Reads data from both local {@link Resources} and
 * from remote sources, such as a spreadsheet.
 */
public class SyncService extends IntentService {

	private static final String TAG = "SyncService";

    public static final String EXTRA_STATUS_RECEIVER =
            "net.peterkuterna.android.apps.devoxxsched.extra.STATUS_RECEIVER";
    public static final String EXTRA_FORCE_REFRESH =
        "net.peterkuterna.android.apps.devoxxsched.extra.FORCE_REFRESH";

    public static final int STATUS_RUNNING = 0x1;
    public static final int STATUS_ERROR = 0x2;
    public static final int STATUS_FINISHED = 0x3;

    private static final String [] URLS = {
    	Constants.ROOMS_URL,
    	Constants.SPEAKERS_URL,
    	Constants.PRESENTATIONS_URL,
    	Constants.SCHEDULE_URL,
    	Constants.LABS_SPEAKERS_URL,
    	Constants.LABS_PRESENTATIONS_URL,
    	Constants.LABS_SCHEDULE_URL,
    };

    private static final int VERSION_NONE = 0;
    private static final int VERSION_LOCAL = 1;
    private static final int VERSION_REMOTE = 4;

    private LocalExecutor mLocalExecutor;
    private RemoteExecutor mRemoteExecutor;
    private HttpClient mHttpClient;
    private ContentResolver mResolver;
    
    public SyncService() {
        super(TAG);
    }

    @Override
    public void onCreate() {
        super.onCreate();

        mHttpClient = SyncUtils.getHttpClient(this);
        mResolver = getContentResolver();

        mLocalExecutor = new LocalExecutor(getResources(), mResolver);
        mRemoteExecutor = new RemoteExecutor(mHttpClient, mResolver);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        final ResultReceiver receiver = intent.getParcelableExtra(EXTRA_STATUS_RECEIVER);
        if (receiver != null) receiver.send(STATUS_RUNNING, Bundle.EMPTY);

        final Context context = this;
        final SharedPreferences syncServicePrefs = getSharedPreferences(SyncPrefs.DEVOXXSCHED_SYNC, Context.MODE_PRIVATE);
        final int localVersion = syncServicePrefs.getInt(SyncPrefs.LOCAL_VERSION, VERSION_NONE);
        final long lastRemoteSync = syncServicePrefs.getLong(SyncPrefs.LAST_REMOTE_SYNC, 0);
        
        try {
            // Bulk of sync work, performed by executing several fetches from
            // local and online sources.

            final long startLocal = System.currentTimeMillis();
            final boolean localParse = localVersion < VERSION_LOCAL;
            Log.d(TAG, "found localVersion=" + localVersion + " and VERSION_LOCAL=" + VERSION_LOCAL);
            if (localParse) {
                // Parse values from local cache first
                mLocalExecutor.execute(R.xml.search_suggest, new LocalSearchSuggestHandler());
            	mLocalExecutor.execute(context, "cache-rooms.json", new RemoteRoomsHandler());
            	mLocalExecutor.execute(context, "cache-speakers.json", new RemoteSpeakersHandler());
            	mLocalExecutor.execute(context, "cache-presentations.json", new RemoteSessionsHandler());
            	mLocalExecutor.execute(context, "cache-schedule.json", new RemoteScheduleHandler());

                // Save local parsed version
            	syncServicePrefs.edit().putInt(SyncPrefs.LOCAL_VERSION, VERSION_LOCAL).commit();
            }
            Log.d(TAG, "local sync took " + (System.currentTimeMillis() - startLocal) + "ms");

            final long startRemote = System.currentTimeMillis();
            boolean performRemoteSync = performRemoteSync(mResolver, mHttpClient, intent, context);
            if (performRemoteSync) {
            	// Parse values from REST interface
	            ArrayList<RequestHash> result = mRemoteExecutor.executeGet(new String [] {
	            			Constants.ROOMS_URL,
	            		}, new RemoteRoomsHandler());
	            for (RequestHash requestHash : result) {
	            	SyncUtils.updateLocalMd5(mResolver, requestHash.getUrl(), requestHash.getMd5());
	            }
	            result = mRemoteExecutor.executeGet(new String [] {
	            			Constants.SPEAKERS_URL,
	            			Constants.LABS_SPEAKERS_URL,
	            		}, new RemoteSpeakersHandler());
	            for (RequestHash requestHash : result) {
	            	SyncUtils.updateLocalMd5(mResolver, requestHash.getUrl(), requestHash.getMd5());
	            }
	            result = mRemoteExecutor.executeGet(new String [] {
	            			Constants.PRESENTATIONS_URL,
	            			Constants.LABS_PRESENTATIONS_URL,
	    				}, new RemoteSessionsHandler());
	            for (RequestHash requestHash : result) {
	            	SyncUtils.updateLocalMd5(mResolver, requestHash.getUrl(), requestHash.getMd5());
	            }
	            result = mRemoteExecutor.executeGet(new String [] {
	            			Constants.SCHEDULE_URL,
	            			Constants.LABS_SCHEDULE_URL,
    					}, new RemoteScheduleHandler());
	            for (RequestHash requestHash : result) {
	            	SyncUtils.updateLocalMd5(mResolver, requestHash.getUrl(), requestHash.getMd5());
	            }

	            // Save last remote sync time
	            syncServicePrefs.edit().putLong(SyncPrefs.LAST_REMOTE_SYNC, startRemote).commit();
	            // Save remote parsed version
	            syncServicePrefs.edit().putInt(SyncPrefs.LOCAL_VERSION, VERSION_REMOTE).commit();
            }
            Log.d(TAG, "remote sync took " + (System.currentTimeMillis() - startRemote) + "ms");

            if (!localParse && performRemoteSync) {
            	NotificationUtils.cancelNotifications(context);
            	NotificationUtils.notifyNewSessions(context, getContentResolver());
            	NotificationUtils.notifyChangedStarredSessions(context, getContentResolver());
            }
        } catch (Exception e) {
            Log.e(TAG, "Problem while syncing", e);

            if (receiver != null) {
                // Pass back error to surface listener
                final Bundle bundle = new Bundle();
                bundle.putString(Intent.EXTRA_TEXT, e.toString());
                receiver.send(STATUS_ERROR, bundle);
            }
        }

        // Announce success to any surface listener
        Log.d(TAG, "sync finished");
        if (receiver != null) receiver.send(STATUS_FINISHED, Bundle.EMPTY);
    }

    /**
     * Should we perform a remote sync?
     */
    private static boolean performRemoteSync(ContentResolver resolver, HttpClient httpClient, Intent intent, Context context) {
        final SharedPreferences settingsPrefs = context.getSharedPreferences(SettingsActivity.SETTINGS_NAME, MODE_PRIVATE);
        final SharedPreferences syncServicePrefs = context.getSharedPreferences(SyncPrefs.DEVOXXSCHED_SYNC, Context.MODE_PRIVATE);
        final boolean onlySyncWifi = settingsPrefs.getBoolean(context.getString(R.string.sync_only_wifi_key), false);
        final int localVersion = syncServicePrefs.getInt(SyncPrefs.LOCAL_VERSION, VERSION_NONE);
        if (!onlySyncWifi || isWifiConnected(context)) {
            final boolean remoteParse = localVersion < VERSION_REMOTE;
	        final boolean forceRemoteRefresh = intent.getBooleanExtra(EXTRA_FORCE_REFRESH, false);
	        final boolean hasContentChanged = hasContentChanged(resolver, httpClient);
	        return remoteParse || forceRemoteRefresh || hasContentChanged;
        }
        return false;
    }
    
    /**
     * Are we connected to a WiFi network?
     */
    private static boolean isWifiConnected(Context context) {
    	final ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

    	if (connectivityManager != null) {
    		NetworkInfo networkInfo = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
    		return (networkInfo != null && networkInfo.getState().equals(State.CONNECTED));
    	}
    	
    	return false;
    }
    
    /**
     * Checks for changes to the responses of the Devoxx REST API.
     */
    private static boolean hasContentChanged(ContentResolver resolver, HttpClient httpClient) {
    	for (String url : URLS) {
    		if (isContentChanged(resolver, httpClient, url)) {
    			return true;
    		}
    	}
    	
    	return false;
    }

    /**
     * Checks if the content of a given url has changed.
     */
    private static boolean isContentChanged(ContentResolver resolver, HttpClient httpClient, String url) {
    	final String localMd5 = SyncUtils.getLocalMd5(resolver, url);
    	final String remoteMd5 = SyncUtils.getRemoteMd5(httpClient, url);
    	return (remoteMd5 != null && !remoteMd5.equals(localMd5));
    }
    
    private interface SyncPrefs {
        String DEVOXXSCHED_SYNC = "devoxxsched_sync";
        String LOCAL_VERSION = "local_version";
        String LAST_REMOTE_SYNC = "last_remote_sync";
    }
    
}
