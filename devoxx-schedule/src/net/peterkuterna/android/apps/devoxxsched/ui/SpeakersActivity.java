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

import static net.peterkuterna.android.apps.devoxxsched.util.UIUtils.buildStyledSnippet;
import net.peterkuterna.android.apps.devoxxsched.R;
import net.peterkuterna.android.apps.devoxxsched.provider.ScheduleContract.Speakers;
import net.peterkuterna.android.apps.devoxxsched.util.NotifyingAsyncQueryHandler;
import net.peterkuterna.android.apps.devoxxsched.util.UIUtils;
import net.peterkuterna.android.apps.devoxxsched.util.NotifyingAsyncQueryHandler.AsyncQueryListener;
import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.text.Spannable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CursorAdapter;
import android.widget.ListView;
import android.widget.TextView;

/**
 * {@link ListActivity} to list the {@link Speakers}
 */
public class SpeakersActivity extends ListActivity implements AsyncQueryListener {

    private CursorAdapter mAdapter;

    private NotifyingAsyncQueryHandler mHandler;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
        if (!getIntent().hasCategory(Intent.CATEGORY_TAB)) {
            setContentView(R.layout.activity_speakers);

            final String customTitle = getIntent().getStringExtra(Intent.EXTRA_TITLE);
            ((TextView) findViewById(R.id.title_text)).setText(
                    customTitle != null ? customTitle : getTitle());
        } else {
            setContentView(R.layout.activity_speakers_content);
        }

        final Uri speakersUri = getIntent().getData();

        String[] projection;
        if (!Speakers.isSearchUri(speakersUri)) {
            mAdapter = new SpeakersAdapter(this);
            projection = SpeakersQuery.PROJECTION;
        } else {
            mAdapter = new SearchAdapter(this);
            projection = SearchQuery.PROJECTION;
        }

        setListAdapter(mAdapter);

        mHandler = new NotifyingAsyncQueryHandler(getContentResolver(), this);
        mHandler.startQuery(speakersUri, projection, Speakers.DEFAULT_SORT);
	}

    /** {@inheritDoc} */
    public void onQueryComplete(int token, Object cookie, Cursor cursor) {
        startManagingCursor(cursor);
        mAdapter.changeCursor(cursor);
    }

    /** {@inheritDoc} */
    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        final Cursor cursor = (Cursor) mAdapter.getItem(position);
        final String speakerId = cursor.getString(SpeakersQuery.SPEAKER_ID);
        final Uri speakerUri = Speakers.buildSpeakerUri(speakerId);
        startActivity(new Intent(Intent.ACTION_VIEW, speakerUri));
    }

    /** Handle "home" title-bar action. */
    public void onHomeClick(View v) {
        UIUtils.goHome(this);
    }

    /** Handle "search" title-bar action. */
    public void onSearchClick(View v) {
        UIUtils.goSearch(this);
    }

    /**
     * {@link CursorAdapter} that renders a {@link SpeakersQuery}.
     */
    private class SpeakersAdapter extends CursorAdapter {
    	
        public SpeakersAdapter(Context context) {
            super(context, null);
        }

        /** {@inheritDoc} */
        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            return getLayoutInflater().inflate(R.layout.list_item_speaker, parent, false);
        }

        /** {@inheritDoc} */
        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            final TextView nameView = (TextView) view.findViewById(R.id.speaker_name);
            final TextView companyView = (TextView) view.findViewById(R.id.speaker_company);
            final CheckBox starButton = (CheckBox) view.findViewById(R.id.star_button);

            nameView.setText(cursor.getString(SpeakersQuery.LAST_NAME) + " " + cursor.getString(SpeakersQuery.FIRST_NAME));
            companyView.setText(cursor.getString(SpeakersQuery.COMPANY));

            final boolean starred = cursor.getInt(SpeakersQuery.CONTAINS_STARRED) != 0;
            starButton.setVisibility(starred ? View.VISIBLE : View.INVISIBLE);
            starButton.setChecked(starred);
        }
    }

    /**
     * {@link CursorAdapter} that renders a {@link SearchQuery}.
     */
    private class SearchAdapter extends CursorAdapter {
        public SearchAdapter(Context context) {
            super(context, null);
        }

        /** {@inheritDoc} */
        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            return getLayoutInflater().inflate(R.layout.list_item_speaker, parent, false);
        }

        /** {@inheritDoc} */
        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            final TextView nameView = (TextView) view.findViewById(R.id.speaker_name);
            final TextView companyView = (TextView) view.findViewById(R.id.speaker_company);
            final CheckBox starButton = (CheckBox) view.findViewById(R.id.star_button);

            final String name = cursor.getString(SearchQuery.LAST_NAME) + " " + cursor.getString(SearchQuery.FIRST_NAME);
            nameView.setText(name);

            final String snippet = cursor.getString(SearchQuery.SEARCH_SNIPPET);
            final Spannable styledSnippet = buildStyledSnippet(snippet);
            companyView.setText(styledSnippet);

            final boolean starred = cursor.getInt(SearchQuery.CONTAINS_STARRED) != 0;
            starButton.setVisibility(starred ? View.VISIBLE : View.INVISIBLE);
            starButton.setChecked(starred);
        }
    }

    /** {@link Speakers} query parameters. */
    private interface SpeakersQuery {
        String[] PROJECTION = {
                BaseColumns._ID,
                Speakers.SPEAKER_ID,
                Speakers.FIRST_NAME,
                Speakers.LAST_NAME,
                Speakers.COMPANY,
                Speakers.CONTAINS_STARRED,
        };

        int _ID = 0;
        int SPEAKER_ID = 1;
        int FIRST_NAME = 2;
        int LAST_NAME = 3;
        int COMPANY = 4;
        int CONTAINS_STARRED = 5;
    }

    /** {@link Speakers} search query parameters. */
    private interface SearchQuery {
        String[] PROJECTION = {
                BaseColumns._ID,
                Speakers.SPEAKER_ID,
                Speakers.FIRST_NAME,
                Speakers.LAST_NAME,
                Speakers.SEARCH_SNIPPET,
                Speakers.CONTAINS_STARRED,
        };

        int _ID = 0;
        int SPEAKER_ID = 1;
        int FIRST_NAME = 2;
        int LAST_NAME = 3;
        int SEARCH_SNIPPET = 4;
        int CONTAINS_STARRED = 5;
    }

}
