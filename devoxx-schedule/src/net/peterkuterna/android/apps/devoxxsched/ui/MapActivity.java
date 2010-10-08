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
 * Modified by Peter Kuterna to support the Devoxx Conference.
 * Use a TabActivity instead of one Activity.
 * First tab holds the WebView as original.
 * Second and third tab holds a defined layout with an ImageView.
 */
package net.peterkuterna.android.apps.devoxxsched.ui;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import net.peterkuterna.android.apps.devoxxsched.R;
import net.peterkuterna.android.apps.devoxxsched.ui.widget.FloorView;
import net.peterkuterna.android.apps.devoxxsched.util.UIUtils;
import android.app.TabActivity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.TabHost;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.TabHost.OnTabChangeListener;

public class MapActivity extends TabActivity {

	private static final String TAG = "MapActivity";

    public static final String EXTRA_FOCUS_TAG = "net.peterkuterna.android.apps.devoxxsched.extra.FOCUS_TAG";
    public static final String EXTRA_ROOM_NAME = "net.peterkuterna.android.apps.devoxxsched.extra.ROOM_NAME";

    public static final String TAG_MAP = "map";
    public static final String TAG_GROUND_FLOOR = "ground";
    public static final String TAG_TALKS_FLOOR = "talks";
    
    private static final String MAP_JSI_NAME = "MAP_CONTAINER";
    private static final String MAP_URL = "http://devoxx2010.appspot.com/devoxx_map.html";
    
    private static final String NAVIGATION_QUERY = buildMetropolisQuery();
    
    private String mRoomName;
    private WebView mWebView;
    private View mRefreshSeparator;
    private View mRefreshButton;
    private View mRefreshProgress;
    
    private boolean mLoadingVisible = false;
    private boolean mMapInitialized = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        ((TextView) findViewById(R.id.title_text)).setText(getTitle());
        
        mRefreshSeparator = findViewById(R.id.refresh_map_separator);
        mRefreshButton = findViewById(R.id.btn_title_refresh);
        mRefreshProgress = findViewById(R.id.title_refresh_progress);
        
        mRoomName = getIntent().getStringExtra(EXTRA_ROOM_NAME);
        
        setupMapTab();
        setupGroundFloorTab();
        setupTalksFloorTab();

        // Show specific focus tag when requested, otherwise default
        String focusTag = getIntent().getStringExtra(EXTRA_FOCUS_TAG);
        if (focusTag == null) focusTag = TAG_MAP;
        onTabChange(focusTag);

        getTabHost().setOnTabChangedListener(new OnTabChangeListener() {
			@Override
			public void onTabChanged(String tabId) {
				onTabChange(tabId);
			}
		});
        
        getTabHost().setCurrentTabByTag(focusTag);
    }

    public void onHomeClick(View v) {
        UIUtils.goHome(this);
    }

    public void onRefreshClick(View v) {
        mWebView.reload();
    }

    public void onSearchClick(View v) {
        UIUtils.goSearch(this);
    }
    
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && mWebView.canGoBack()) {
            mWebView.goBack();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    private void showLoading(boolean loading) {
        if (mLoadingVisible == loading)
            return;

        if (TAG_MAP.equals(getTabHost().getCurrentTabTag())) {
	        View refreshButton = findViewById(R.id.btn_title_refresh);
	        View refreshProgress = findViewById(R.id.title_refresh_progress);
	        refreshButton.setVisibility(loading ? View.GONE : View.VISIBLE);
	        refreshProgress.setVisibility(loading ? View.VISIBLE : View.GONE);
    	}

        mLoadingVisible = loading;
    }

    private void onTabChange(String tabId) {
		if (TAG_MAP.equals(tabId)) {
	        mRefreshSeparator.setVisibility(View.VISIBLE);
	        mRefreshButton.setVisibility(mLoadingVisible ? View.GONE : View.VISIBLE);
	        mRefreshProgress.setVisibility(mLoadingVisible ? View.VISIBLE : View.GONE);
		} else if (TAG_GROUND_FLOOR.equals(tabId)
					|| TAG_TALKS_FLOOR.equals(tabId)) {
	        mRefreshSeparator.setVisibility(View.GONE);
			mRefreshButton.setVisibility(View.GONE);
			mRefreshProgress.setVisibility(View.GONE);
		}
    }

    /** Build and add "map" tab. */
    private void setupMapTab() {
        final TabHost host = getTabHost();

        host.addTab(host.newTabSpec(TAG_MAP)
                .setIndicator(buildIndicator(R.string.map_map))
                .setContent(R.id.tab_map_map));
        
        mWebView = (WebView) findViewById(R.id.webview);
        mWebView.post(new Runnable() {
            public void run() {
                // Initialize web view
                if (false) {
                    mWebView.clearCache(true);
                }

                mWebView.getSettings().setJavaScriptEnabled(true);
                mWebView.getSettings().setJavaScriptCanOpenWindowsAutomatically(false);
                mWebView.setWebChromeClient(new MapWebChromeClient());
                mWebView.setWebViewClient(new MapWebViewClient());
                mWebView.loadUrl(MAP_URL);
                mWebView.addJavascriptInterface(new MapJsiImpl(), MAP_JSI_NAME);
            }
        });
    }

    /** Build and add "ground floor" tab. */
    private void setupGroundFloorTab() {
        final TabHost host = getTabHost();

        host.addTab(host.newTabSpec(TAG_GROUND_FLOOR)
                .setIndicator(buildIndicator(R.string.map_ground))
                .setContent(R.id.tab_map_ground));
        
        if (mRoomName != null && mRoomName.startsWith("bof")) {
	        FloorView floorView = (FloorView) host.findViewById(R.id.ground_floor);
	        floorView.highlightRoom(1);
        }
    }

    /** Build and add "talks floor" tab. */
    private void setupTalksFloorTab() {
    	final TabHost host = getTabHost();

    	host.addTab(host.newTabSpec(TAG_TALKS_FLOOR)
    			.setIndicator(buildIndicator(R.string.map_talks))
    			.setContent(R.id.tab_map_talks));

    	if (mRoomName != null && mRoomName.startsWith("room")) {
    		String roomNumber = mRoomName.replace("room ", "");
    		try {
    			FloorView floorView = (FloorView) host.findViewById(R.id.talks_floor);
    			floorView.highlightRoom(Integer.valueOf(roomNumber).intValue() - 3);
    		} catch (NumberFormatException e) {
    		}
    	}
    }

    /**
     * Build a {@link View} to be used as a tab indicator, setting the requested
     * string resource as its label.
     */
    private View buildIndicator(int textRes) {
        final TextView indicator = (TextView) getLayoutInflater().inflate(R.layout.tab_indicator,
                getTabWidget(), false);
        indicator.setText(textRes);
        return indicator;
    }
    
    private boolean isNavigationInstalled() {
    	final Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("google.navigation:ll=51.350769,4.319704"));
    	return UIUtils.isIntentAvailable(this, intent);
    }
    
    private void runJs(String js) {
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "Loading javascript:" + js);
        }
        mWebView.loadUrl("javascript:" + js);
    }

    private class MapWebChromeClient extends WebChromeClient {
        @Override
        public void onProgressChanged(WebView view, int newProgress) {
            showLoading(newProgress < 100);
            super.onProgressChanged(view, newProgress);
        }

        public void onConsoleMessage(String message, int lineNumber, String sourceID) {
            Log.i(TAG, "JS Console message: (" + sourceID + ": " + lineNumber + ") " + message);
        }
    }

    private class MapWebViewClient extends WebViewClient {
        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "Page finished loading: " + url);
            }
        }

        @Override
        public void onReceivedError(WebView view, int errorCode, String description,
                String failingUrl) {
            Log.e(TAG, "Error " + errorCode + ": " + description);
            Toast.makeText(view.getContext(), "Error " + errorCode + ": " + description,
                    Toast.LENGTH_LONG).show();
            super.onReceivedError(view, errorCode, description, failingUrl);
        }
    }

    private interface MapJsi {
    	void makeCall(String number);
    	void carNavigate();
    	void walkNavigate();
        void onMapReady();
    }

    private class MapJsiImpl implements MapJsi {
    	
    	@Override
		public void makeCall(String number) {
    		final Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("tel:" + number));
    		startActivity(intent);
		}

		@Override
		public void carNavigate() {
    		navigate(false);
    	}
    	
    	@Override
    	public void walkNavigate() {
    		navigate(true);
    	}
    	
    	@Override
        public void onMapReady() {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "onMapReady");
            }

            if (isNavigationInstalled()) {
            	runJs("devoxx.showNavigationButtons();");
            }

            mMapInitialized = true;
        }
        
        private void navigate(boolean walk) {
    		final String uri = "google.navigation:ll=51.245611,4.416225"
    			+ "&q=" + NAVIGATION_QUERY + (walk ? "&mode=w" : "");
        	final Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
        	startActivity(intent);
        }
        
    }
    
    private static final String buildMetropolisQuery() {
    	try {
			return URLEncoder.encode("Metropolis, Groenendaallaan 394, 2030 Antwerpen", "UTF-8");
		} catch (UnsupportedEncodingException e) {
			return null;
		}
    }

}
