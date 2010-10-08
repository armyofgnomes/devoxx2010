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

package net.peterkuterna.android.apps.devoxxsched.io;

import static net.peterkuterna.android.apps.devoxxsched.util.ParserUtils.sanitizeId;

import java.util.ArrayList;
import java.util.HashSet;

import net.peterkuterna.android.apps.devoxxsched.provider.ScheduleContract;
import net.peterkuterna.android.apps.devoxxsched.provider.ScheduleContract.Speakers;
import net.peterkuterna.android.apps.devoxxsched.util.Lists;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;


/**
 * Handle a remote {@link JSONArray} that defines a set of {@link Speakers}
 * entries.
 */
public class RemoteSpeakersHandler extends JSONHandler {

    private static final String TAG = "SpeakersHandler";

    public RemoteSpeakersHandler() {
		super(ScheduleContract.CONTENT_AUTHORITY, false);
	}

	@Override
	public ArrayList<ContentProviderOperation> parse(JSONArray speakers,
			ContentResolver resolver) throws JSONException {
		final ArrayList<ContentProviderOperation> batch = Lists.newArrayList();
		final HashSet<String> speakerIds = new HashSet<String>();
		
		Log.d(TAG, "Retrieved " + speakers.length() + " speaker entries.");

        for (int i=0; i < speakers.length(); i++) {
            JSONObject speaker = speakers.getJSONObject(i);
            String id = speaker.getString("id");
            
            final String speakerId = sanitizeId(id);
            final Uri speakerUri = Speakers.buildSpeakerUri(speakerId);
            speakerIds.add(speakerId);
            
            boolean speakerUpdated = false;
            boolean newSpeaker = false;
            boolean build = false;
            ContentProviderOperation.Builder builder;
            if (isRowExisting(Speakers.buildSpeakerUri(speakerId), SpeakersQuery.PROJECTION, resolver)) {
            	builder = ContentProviderOperation.newUpdate(speakerUri);
            	speakerUpdated = isSpeakerUpdated(speakerUri, speaker, resolver);
            } else {
            	newSpeaker = true;
	            builder = ContentProviderOperation.newInsert(Speakers.CONTENT_URI);
			    builder.withValue(Speakers.SPEAKER_ID, speakerId);
			    build = true;
            }
            
            if (newSpeaker || speakerUpdated) {
			    builder.withValue(Speakers.FIRST_NAME, speaker.getString("firstName"));
			    builder.withValue(Speakers.LAST_NAME, speaker.getString("lastName"));
			    builder.withValue(Speakers.BIO, speaker.getString("bio"));
			    builder.withValue(Speakers.COMPANY, speaker.getString("company"));
			    builder.withValue(Speakers.IMAGE_URL, speaker.getString("imageURI"));
			    build = true;
            }
            if (build) batch.add(builder.build());
        }
        
        if (speakers.length() > 0) {
		    for (String lostId : getLostIds(speakerIds, Speakers.CONTENT_URI, SpeakersQuery.PROJECTION, SpeakersQuery.SPEAKER_ID, resolver)) {
		    	Uri deleteUri = Speakers.buildSessionsDirUri(lostId);
		    	batch.add(ContentProviderOperation.newDelete(deleteUri).build());
		    	deleteUri = Speakers.buildSpeakerUri(lostId);
		    	batch.add(ContentProviderOperation.newDelete(deleteUri).build());
		    }
        }

        return batch;
	}

	private static boolean isSpeakerUpdated(Uri uri, JSONObject speaker, ContentResolver resolver) throws JSONException {
        final Cursor cursor = resolver.query(uri, SpeakersQuery.PROJECTION, null, null, null);
        try {
            if (!cursor.moveToFirst()) return false;

            final String curFirstName = cursor.getString(SpeakersQuery.FIRST_NAME).toLowerCase().trim();
        	final String curLastName = cursor.getString(SpeakersQuery.LAST_NAME).toLowerCase().trim();
        	final String curBio = cursor.getString(SpeakersQuery.BIO).toLowerCase().trim();
        	final String curCompany = cursor.getString(SpeakersQuery.COMPANY).toLowerCase().trim();
        	final String newFirstName = speaker.getString("firstName").toLowerCase().trim();
        	final String newLastName = speaker.getString("lastName").toLowerCase().trim();
        	final String newBio = speaker.getString("bio").toLowerCase().trim();
        	final String newCompany = speaker.getString("company").toLowerCase().trim();
        	
        	return (!curFirstName.equals(newFirstName)
        			|| !curLastName.equals(newLastName)
        			|| !curBio.equals(newBio)
        			|| !curCompany.equals(newCompany));
        } finally {
            cursor.close();
        }
	}

	
    private interface SpeakersQuery {
        String[] PROJECTION = {
                Speakers.SPEAKER_ID,
                Speakers.FIRST_NAME,
                Speakers.LAST_NAME,
                Speakers.BIO,
                Speakers.COMPANY,
        };

        int SPEAKER_ID = 0;
        int FIRST_NAME = 1;
        int LAST_NAME = 2;
        int BIO = 3;
        int COMPANY = 4;
    }

}
