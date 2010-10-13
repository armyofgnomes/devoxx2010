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
 * Modified by Peter Kuterna to support the Devoxx conference.
 * The 'Moderator' tab has been replaced by a 'In parallel' tab.
 */
package net.peterkuterna.android.apps.devoxxsched.ui;

import net.peterkuterna.android.apps.devoxxsched.R;
import net.peterkuterna.android.apps.devoxxsched.provider.ScheduleContract.Blocks;
import net.peterkuterna.android.apps.devoxxsched.provider.ScheduleContract.Rooms;
import net.peterkuterna.android.apps.devoxxsched.provider.ScheduleContract.Sessions;
import net.peterkuterna.android.apps.devoxxsched.provider.ScheduleContract.Speakers;
import net.peterkuterna.android.apps.devoxxsched.provider.ScheduleContract.Tracks;
import net.peterkuterna.android.apps.devoxxsched.util.FractionalTouchDelegate;
import net.peterkuterna.android.apps.devoxxsched.util.NotifyingAsyncQueryHandler;
import net.peterkuterna.android.apps.devoxxsched.util.UIUtils;
import net.peterkuterna.android.apps.devoxxsched.util.NotifyingAsyncQueryHandler.AsyncQueryListener;
import android.app.Activity;
import android.app.TabActivity;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.RectF;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TabHost;
import android.widget.TextView;
import android.widget.CompoundButton.OnCheckedChangeListener;

/**
 * {@link Activity} that displays details about a specific
 * {@link Sessions#SESSION_ID}, as requested through {@link Intent#getData()}.
 */
public class SessionDetailActivity extends TabActivity implements AsyncQueryListener,
        OnCheckedChangeListener {

	private static final String TAG = "SessionDetailActivity";

    /**
     * Since {@link Sessions} can belong to multiple {@link Tracks}, the parent
     * {@link Activity} can send this extra specifying a {@link Tracks}
     * {@link Uri} that should be used for coloring the title-bar.
     */
    public static final String EXTRA_TRACK = "net.peterkuterna.android.apps.devoxxsched.extra.TRACK";

    private static final String TAG_SUMMARY = "summary";
    private static final String TAG_NOTES = "notes";
    private static final String TAG_PARALLEL = "parallel";

    private String mSessionId;
    private Uri mSessionUri;

    private String mTitleString;
    private String mRoomNameLowercase;

    private TextView mTitle;
    private TextView mSubtitle;
    private CompoundButton mStarred;

    private TextView mSummary;
    private TextView mExperience;
    private TextView mType;
    private TextView mNote;
    private LinearLayout mExperienceBlock;
    private LinearLayout mTypeBlock;
    private LinearLayout mNoteBlock;

    private NotifyingAsyncQueryHandler mHandler;

    private boolean mSessionCursor = false;
    private boolean mSpeakersCursor = false;
    private boolean mHasSummaryContent = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_session_detail);

        mTitle = (TextView) findViewById(R.id.session_title);
        mSubtitle = (TextView) findViewById(R.id.session_subtitle);
        mStarred = (CompoundButton) findViewById(R.id.star_button);

        mStarred.setFocusable(true);
        mStarred.setClickable(true);

        // Larger target triggers star toggle
        final View starParent = findViewById(R.id.list_item_session);
        FractionalTouchDelegate.setupDelegate(starParent, mStarred, new RectF(0.6f, 0f, 1f, 0.8f));

        mSummary = (TextView) findViewById(R.id.session_abstract);
        mExperience = (TextView) findViewById(R.id.session_experience);
        mType = (TextView) findViewById(R.id.session_type);
        mNote = (TextView) findViewById(R.id.session_note);
        mExperienceBlock = (LinearLayout) findViewById(R.id.session_experience_block);
        mTypeBlock = (LinearLayout) findViewById(R.id.session_type_block);
        mNoteBlock = (LinearLayout) findViewById(R.id.session_note_block);
        
        final Intent intent = getIntent();
        mSessionUri = intent.getData();
        mSessionId = Sessions.getSessionId(mSessionUri);
        setupSummaryTab();
        setupNotesTab();
        setupParallelSessionsTab();

        // Start background queries to load session details
        final Uri speakersUri = Sessions.buildSpeakersDirUri(mSessionId);

        mHandler = new NotifyingAsyncQueryHandler(getContentResolver(), this);
        mHandler.startQuery(SessionsQuery._TOKEN, mSessionUri, SessionsQuery.PROJECTION);
        mHandler.startQuery(SpeakersQuery._TOKEN, speakersUri, SpeakersQuery.PROJECTION);
    }

    /** Build and add "summary" tab. */
    private void setupSummaryTab() {
        final TabHost host = getTabHost();

        // Summary content comes from existing layout
        host.addTab(host.newTabSpec(TAG_SUMMARY)
                .setIndicator(buildIndicator(R.string.session_summary))
                .setContent(R.id.tab_session_summary));
    }

    /** Build and add "notes" tab. */
    private void setupNotesTab() {
        final TabHost host = getTabHost();

        final Uri notesUri = Sessions.buildNotesDirUri(mSessionId);
        final Intent intent = new Intent(Intent.ACTION_VIEW, notesUri);
        intent.addCategory(Intent.CATEGORY_TAB);
        intent.putExtra(NotesActivity.EXTRA_SHOW_INSERT, true);

        // Notes content comes from reused activity
        host.addTab(host.newTabSpec(TAG_NOTES)
                .setIndicator(buildIndicator(R.string.session_notes))
                .setContent(intent));
    }

    /** Build and add "in parallel" tab. */
    private void setupParallelSessionsTab() {
        final TabHost host = getTabHost();

        final Uri parSessionsUri = Sessions.buildSessionsParallelDirUri(mSessionId);
        final Intent intent = new Intent(Intent.ACTION_VIEW, parSessionsUri);
        intent.addCategory(Intent.CATEGORY_TAB);
        intent.putExtra(SessionsActivity.EXTRA_NO_WEEKDAY_HEADER, true);

       	host.addTab(host.newTabSpec(TAG_PARALLEL)
       			.setIndicator(buildIndicator(R.string.session_parallel))
       			.setContent(intent));
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

    /** {@inheritDoc} */
    public void onQueryComplete(int token, Object cookie, Cursor cursor) {
        if (token == SessionsQuery._TOKEN) {
            onSessionQueryComplete(cursor);
        } else if (token == TracksQuery._TOKEN) {
            onTrackQueryComplete(cursor);
        } else if (token == SpeakersQuery._TOKEN) {
            onSpeakersQueryComplete(cursor);
        } else {
            cursor.close();
        }
    }

    /** Handle {@link SessionsQuery} {@link Cursor}. */
    private void onSessionQueryComplete(Cursor cursor) {
        try {
            mSessionCursor = true;
            if (!cursor.moveToFirst()) return;
            
            // Start query to resolve track
            final String trackId = cursor.getString(SessionsQuery.TRACK_ID);
            mHandler.startQuery(TracksQuery._TOKEN, Tracks.buildTrackUri(trackId), TracksQuery.PROJECTION);

            // Format time block this session occupies
            final long blockStart = cursor.getLong(SessionsQuery.BLOCK_START);
            final long blockEnd = cursor.getLong(SessionsQuery.BLOCK_END);
            final String roomName = cursor.getString(SessionsQuery.ROOM_NAME);
            final String subtitle = UIUtils.formatSessionSubtitle(blockStart,
                    blockEnd, roomName, this);

            mTitleString = cursor.getString(SessionsQuery.TITLE);
            mTitle.setText(mTitleString);
            mSubtitle.setText(subtitle);

            mRoomNameLowercase = cursor.getString(SessionsQuery.ROOM_NAME).toLowerCase();

            // Unregister around setting checked state to avoid triggering
            // listener since change isn't user generated.
            mStarred.setOnCheckedChangeListener(null);
            mStarred.setChecked(cursor.getInt(SessionsQuery.STARRED) != 0);
            mStarred.setOnCheckedChangeListener(this);

            final String sessionSummary = cursor.getString(SessionsQuery.SUMMARY);
            if (!TextUtils.isEmpty(sessionSummary)) {
                UIUtils.setTextMaybeHtml(mSummary, sessionSummary);
                mSummary.setVisibility(View.VISIBLE);
                mHasSummaryContent = true;
            } else {
                mSummary.setVisibility(View.GONE);
            }
            final String sessionExperience = cursor.getString(SessionsQuery.EXPERIENCE);
            if (!TextUtils.isEmpty(sessionExperience)) {
            	mExperience.setText(sessionExperience);
            	mExperience.setVisibility(View.VISIBLE);
            } else {
            	mExperienceBlock.setVisibility(View.GONE);
            }
            final String sessionType = cursor.getString(SessionsQuery.TYPE);
            if (!TextUtils.isEmpty(sessionType)) {
            	mType.setText(sessionType);
            	mType.setVisibility(View.VISIBLE);
            } else {
            	mTypeBlock.setVisibility(View.GONE);
            }
            final String sessionNote = cursor.getString(SessionsQuery.NOTE);
            if (!TextUtils.isEmpty(sessionNote)) {
            	mNote.setText(sessionNote);
            	mNote.setVisibility(View.VISIBLE);
            } else {
            	mNoteBlock.setVisibility(View.GONE);
            }

            // Show empty message when all data is loaded, and nothing to show
            if (mSpeakersCursor && !mHasSummaryContent) {
                findViewById(android.R.id.empty).setVisibility(View.VISIBLE);
            }
        } finally {
            cursor.close();
        }
    }

    /** Handle {@link TracksQuery} {@link Cursor}. */
    private void onTrackQueryComplete(Cursor cursor) {
        try {
            if (!cursor.moveToFirst()) return;

            // Use found track to build title-bar
            ((TextView) findViewById(R.id.title_text)).setText(cursor
                    .getString(TracksQuery.TRACK_NAME));
            UIUtils.setTitleBarColor(findViewById(R.id.title_container),
                    cursor.getInt(TracksQuery.TRACK_COLOR));
            findViewById(R.id.list_item_session).setBackgroundColor(UIUtils.lightenColor(cursor.getInt(TracksQuery.TRACK_COLOR)));
        } finally {
            cursor.close();
        }
    }

    /** Handle {@link SpeakersQuery} {@link Cursor}. */
    private void onSpeakersQueryComplete(Cursor cursor) {
        try {
            mSpeakersCursor = true;

            // TODO: remove any existing speakers from layout, since this cursor
            // might be from a data change notification.
            final ViewGroup speakersGroup = (ViewGroup) findViewById(R.id.session_speakers_block);
            final LayoutInflater inflater = getLayoutInflater();

            boolean hasSpeakers = false;

            while (cursor.moveToNext()) {
            	final StringBuilder sb = new StringBuilder();
            	sb.append("<a href=\"");
            	sb.append(Speakers.buildSpeakerUri(cursor.getString(SpeakersQuery.SPEAKER_ID)));
            	sb.append("\">");
            	sb.append(cursor.getString(SpeakersQuery.SPEAKER_FIRST_NAME));
            	sb.append(" ");
            	sb.append(cursor.getString(SpeakersQuery.SPEAKER_LAST_NAME));
            	sb.append("</a>");
                final String speaker = sb.toString();  

                final View speakerView = inflater.inflate(R.layout.speaker_detail,
                        speakersGroup, false);
                TextView speakerHeader = (TextView) speakerView.findViewById(R.id.speaker_header);
                UIUtils.setTextMaybeHtml(speakerHeader, speaker);

                speakersGroup.addView(speakerView);
                hasSpeakers = true;
                mHasSummaryContent = true;
            }

            speakersGroup.setVisibility(hasSpeakers ? View.VISIBLE : View.GONE);

            // Show empty message when all data is loaded, and nothing to show
            if (mSessionCursor && !mHasSummaryContent) {
                findViewById(android.R.id.empty).setVisibility(View.VISIBLE);
            }

        } finally {
            cursor.close();
        }
    }

    /** Handle "home" title-bar action. */
    public void onHomeClick(View v) {
        UIUtils.goHome(this);
    }

    /** Handle "share" title-bar action. */
    public void onShareClick(View v) {
        // TODO: consider bringing in shortlink to session
        final String shareString = getString(R.string.share_template, mTitleString);

        final Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TEXT, shareString);

        startActivity(Intent.createChooser(intent, getText(R.string.title_share)));
    }

    /** Handle "map" title-bar action. */
    public void onMapClick(View v) {
        final Intent intent = new Intent(this, MapActivity.class);
        if (mRoomNameLowercase != null) {
        	intent.putExtra(MapActivity.EXTRA_ROOM_NAME, mRoomNameLowercase);
	        if (mRoomNameLowercase.startsWith("room")) {
	        	intent.putExtra(MapActivity.EXTRA_FOCUS_TAG, MapActivity.TAG_TALKS_FLOOR);
	        } else if (mRoomNameLowercase.startsWith("bof")) {
	        	intent.putExtra(MapActivity.EXTRA_FOCUS_TAG, MapActivity.TAG_GROUND_FLOOR);
	        }
        }
        startActivity(intent);
    }

    /** Handle "search" title-bar action. */
    public void onSearchClick(View v) {
        UIUtils.goSearch(this);
    }

    /** Handle toggling of starred checkbox. */
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        final ContentValues values = new ContentValues();
        values.put(Sessions.STARRED, isChecked ? 1 : 0);
        mHandler.startUpdate(mSessionUri, values);
    }

    /** {@link Sessions} query parameters. */
    private interface SessionsQuery {
        int _TOKEN = 0x1;

        String[] PROJECTION = {
                Blocks.BLOCK_START,
                Blocks.BLOCK_END,
                Sessions.TITLE,
                Sessions.SUMMARY,
                Sessions.EXPERIENCE,
                Sessions.TYPE,
                Sessions.NOTE,
                Sessions.STARRED,
                Sessions.TRACK_ID,
                Sessions.ROOM_ID,
                Rooms.NAME,
        };

        int BLOCK_START = 0;
        int BLOCK_END = 1;
        int TITLE = 2;
        int SUMMARY = 3;
        int EXPERIENCE = 4;
        int TYPE = 5;
        int NOTE = 6;
        int STARRED = 7;
        int TRACK_ID = 8;
        int ROOM_ID = 9;
        int ROOM_NAME = 10;
    }

    /** {@link Tracks} query parameters. */
    private interface TracksQuery {
        int _TOKEN = 0x2;

        String[] PROJECTION = {
                Tracks.TRACK_NAME,
                Tracks.TRACK_COLOR,
        };

        int TRACK_NAME = 0;
        int TRACK_COLOR = 1;
    }

    /** {@link Speakers} query parameters. */
    private interface SpeakersQuery {
        int _TOKEN = 0x3;

        String[] PROJECTION = {
        		Speakers.SPEAKER_ID,
                Speakers.FIRST_NAME,
                Speakers.LAST_NAME,
                Speakers.COMPANY,
                Speakers.BIO,
        };

        int SPEAKER_ID = 0;
        int SPEAKER_FIRST_NAME = 1;
        int SPEAKER_LAST_NAME = 2;
        int SPEAKER_COMPANY = 3;
        int SPEAKER_BIO = 4;
    }
}
