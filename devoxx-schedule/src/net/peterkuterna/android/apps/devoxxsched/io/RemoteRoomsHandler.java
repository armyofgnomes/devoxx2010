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
import net.peterkuterna.android.apps.devoxxsched.provider.ScheduleContract.Rooms;
import net.peterkuterna.android.apps.devoxxsched.util.Lists;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.net.Uri;
import android.util.Log;


/**
 * Handle a remote {@link JSONArray} that defines a set of {@link Rooms}
 * entries.
 */
public class RemoteRoomsHandler extends JSONHandler {

    private static final String TAG = "RoomsHandler";

    public RemoteRoomsHandler() {
		super(ScheduleContract.CONTENT_AUTHORITY, false);
	}

	@Override
	public ArrayList<ContentProviderOperation> parse(JSONArray rooms,
			ContentResolver resolver) throws JSONException {
		final ArrayList<ContentProviderOperation> batch = Lists.newArrayList();
		final HashSet<String> roomIds = new HashSet<String>();
		
		Log.d(TAG, "Retrieved " + rooms.length() + " room entries.");

        for (int i = 0; i< rooms.length(); i++) {
            JSONObject room = rooms.getJSONObject(i);
            String id = room.getString("id");
            
            final String roomId = sanitizeId(id);
            final Uri roomUri = Rooms.buildRoomUri(roomId);
            roomIds.add(roomId);
            
            ContentProviderOperation.Builder builder;
            if (isRowExisting(Rooms.buildRoomUri(roomId), RoomsQuery.PROJECTION, resolver)) {
            	builder = ContentProviderOperation.newUpdate(roomUri);
            } else {
	            builder = ContentProviderOperation.newInsert(Rooms.CONTENT_URI);
	            builder.withValue(Rooms.ROOM_ID, roomId);
            }
		    builder.withValue(Rooms.NAME, room.getString("name"));
		    builder.withValue(Rooms.CAPACITY, room.getString("capacity"));
		    batch.add(builder.build());
        }

        if (rooms.length() > 0) {
		    for (String lostId : getLostIds(roomIds, Rooms.CONTENT_URI, RoomsQuery.PROJECTION, RoomsQuery.ROOM_ID, resolver)) {
		    	final Uri lostRoomUri = Rooms.buildRoomUri(lostId);
		    	batch.add(ContentProviderOperation.newDelete(lostRoomUri).build());
		    }
        }

	    return batch;
	}
	
    private interface RoomsQuery {
        String[] PROJECTION = {
                Rooms.ROOM_ID,
        };

        int ROOM_ID = 0;
    }

}
