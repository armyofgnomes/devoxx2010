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
package net.peterkuterna.android.apps.devoxxsched.provider;

import net.peterkuterna.android.apps.devoxxsched.provider.ScheduleDatabase.Tables;
import net.peterkuterna.android.apps.devoxxsched.util.ParserUtils;
import android.app.SearchManager;
import android.content.ContentProvider;
import android.content.ContentUris;
import android.graphics.Color;
import android.net.Uri;
import android.provider.BaseColumns;
import android.text.format.DateUtils;


/**
 * Contract class for interacting with {@link ScheduleProvider}. Unless
 * otherwise noted, all time-based fields are milliseconds since epoch and can
 * be compared against {@link System#currentTimeMillis()}.
 * <p>
 * The backing {@link ContentProvider} assumes that {@link Uri} are generated
 * using stronger {@link String} identifiers, instead of {@code int}
 * {@link BaseColumns#_ID} values, which are prone to shuffle during sync.
 */
public class ScheduleContract {
	
	interface SyncColumns {
		String URI_ID = "uri_id";
		String URI = "uri";
		String MD5 = "md5";
	}

    interface BlocksColumns {
        /** Unique string identifying this block of time. */
        String BLOCK_ID = "block_id";
        /** Title describing this block of time. */
        String BLOCK_TITLE = "block_title";
        /** Time when this block starts. */
        String BLOCK_START = "block_start";
        /** Time when this block ends. */
        String BLOCK_END = "block_end";
        /** Type describing this block. */
        String BLOCK_TYPE = "block_type";
    }
    
    interface TracksColumns {
        /** Unique string identifying this track. */
        String TRACK_ID = "track_id";
        /** Name describing this track. */
        String TRACK_NAME = "track_name";
        /** Color used to identify this track, in {@link Color#argb} format. */
        String TRACK_COLOR = "track_color";
    }

    interface RoomsColumns {
        /** Unique string identifying this room. */
    	String ROOM_ID = "room_id";
        /** Name describing this room. */
    	String NAME = "name";
        /** Capacity of the room. */
    	String CAPACITY = "capacity";
    }

    interface SessionsColumns {
        /** Unique string identifying this session. */
    	String SESSION_ID = "session_id";
        /** Title describing this session. */
    	String TITLE = "title";
        /** Body of text explaining this session in detail. */
    	String SUMMARY = "summary";
        /** Experience that attendees should meet. */
    	String EXPERIENCE = "experience";
        /** Type of session, such as difficulty level. */
    	String TYPE = "type";
        /** Note for a session. */
    	String NOTE = "note";
        /** User-specific flag indicating starred status. */
    	String STARRED = "starred";
        /** Field to mark if this session was updated. */
    	String UPDATED = "updated";
        /** Field to mark if this session was new. */
    	String NEW = "new";
    }

    interface SpeakersColumns {
        /** Unique string identifying this speaker. */
    	String SPEAKER_ID = "speaker_id";
        /** First name of this speaker. */
    	String FIRST_NAME = "first_name";
        /** Last name of this speaker. */
    	String LAST_NAME = "last_name";
        /** Company this speaker works for. */
    	String COMPANY = "company";
        /** Body of text describing this speaker in detail. */
    	String BIO = "bio";
        /** URL towards image of speaker. */
    	String IMAGE_URL = "image_url";
    }

    interface NotesColumns {
        /** Time this note was created. */
        String NOTE_TIME = "note_time";
        /** User-generated content of note. */
        String NOTE_CONTENT = "note_content";
    }

    public static final String CONTENT_AUTHORITY = "net.peterkuterna.android.apps.devoxxsched";

    private static final Uri BASE_CONTENT_URI = Uri.parse("content://" + CONTENT_AUTHORITY);

    private static final String PATH_SESSIONS = "sessions";
    private static final String PATH_SPEAKERS = "speakers";
    private static final String PATH_ROOMS = "rooms";
    private static final String PATH_WITH_NAME = "name";
    private static final String PATH_BLOCKS = "blocks";
    private static final String PATH_NOTES = "notes";
    private static final String PATH_EXPORT = "export";
    private static final String PATH_STARRED = "starred";
    private static final String PATH_NEW = "new";
    private static final String PATH_UPDATED = "updated";
    private static final String PATH_TRACKS = "tracks";
    private static final String PATH_AT = "at";
    private static final String PATH_BETWEEN = "between";
    private static final String PATH_PARALLEL = "parallel";
    private static final String PATH_SEARCH = "search";
    private static final String PATH_SEARCH_SUGGEST = "search_suggest_query";
    private static final String PATH_SYNC = "sync";

    /**
     * Blocks are generic timeslots that {@link Sessions} and other related
     * events fall into.
     */
    public static class Blocks implements BlocksColumns, BaseColumns {
        public static final Uri CONTENT_URI =
                BASE_CONTENT_URI.buildUpon().appendPath(PATH_BLOCKS).build();

        public static final String CONTENT_TYPE =
                "vnd.android.cursor.dir/vnd.devoxx.block";
        public static final String CONTENT_ITEM_TYPE =
                "vnd.android.cursor.item/vnd.devoxx.block";

        /** Count of {@link Sessions} inside given block. */
        public static final String SESSIONS_COUNT = "sessions_count";

        /**
         * Flag indicating that at least one {@link Sessions#SESSION_ID} inside
         * this block has {@link Sessions#STARRED} set.
         */
        public static final String CONTAINS_STARRED = "contains_starred";

        /** Default "ORDER BY" clause. */
        public static final String DEFAULT_SORT = BlocksColumns.BLOCK_START + " ASC, "
                + BlocksColumns.BLOCK_END + " ASC";

        /** Build {@link Uri} for requested {@link #BLOCK_ID}. */
        public static Uri buildBlockUri(String blockId) {
            return CONTENT_URI.buildUpon().appendPath(blockId).build();
        }

        /**
         * Build {@link Uri} that references any {@link Sessions} associated
         * with the requested {@link #BLOCK_ID}.
         */
        public static Uri buildSessionsUri(String blockId) {
            return CONTENT_URI.buildUpon().appendPath(blockId).appendPath(PATH_SESSIONS).build();
        }

        /**
         * Build {@link Uri} that references any {@link Blocks} that occur
         * between the requested time boundaries.
         */
        public static Uri buildBlocksBetweenDirUri(long startTime, long endTime) {
            return CONTENT_URI.buildUpon().appendPath(PATH_BETWEEN).appendPath(
                    String.valueOf(startTime)).appendPath(String.valueOf(endTime)).build();
        }

        /** Read {@link #BLOCK_ID} from {@link Blocks} {@link Uri}. */
        public static String getBlockId(Uri uri) {
            return uri.getPathSegments().get(1);
        }

        /**
         * Generate a {@link #BLOCK_ID} that will always match the requested
         * {@link Blocks} details.
         */
        public static String generateBlockId(String kind, long startTime, long endTime) {
            startTime /= DateUtils.SECOND_IN_MILLIS;
            endTime /= DateUtils.SECOND_IN_MILLIS;
            return ParserUtils.sanitizeId(kind + "-" + startTime + "-" + endTime);
        }
    }

    /**
     * Tracks are overall categories for {@link Sessions},
     * such as "Desktop/RIA/Mobile" or "Cloud/NoSQL."
     */
    public static class Tracks implements TracksColumns, BaseColumns {
        public static final Uri CONTENT_URI =
                BASE_CONTENT_URI.buildUpon().appendPath(PATH_TRACKS).build();

        public static final String CONTENT_TYPE =
                "vnd.android.cursor.dir/vnd.devoxx.track";
        public static final String CONTENT_ITEM_TYPE =
                "vnd.android.cursor.item/vnd.devoxx.track";

        /** Count of {@link Sessions} inside given track. */
        public static final String SESSIONS_COUNT = "sessions_count";

        /** Default "ORDER BY" clause. */
        public static final String DEFAULT_SORT = TracksColumns.TRACK_NAME + " ASC";

        /** Build {@link Uri} for requested {@link #TRACK_ID}. */
        public static Uri buildTrackUri(String trackId) {
            return CONTENT_URI.buildUpon().appendPath(trackId).build();
        }

        /**
         * Build {@link Uri} that references any {@link Sessions} associated
         * with the requested {@link #TRACK_ID}.
         */
        public static Uri buildSessionsUri(String trackId) {
            return CONTENT_URI.buildUpon().appendPath(trackId).appendPath(PATH_SESSIONS).build();
        }

        /** Read {@link #TRACK_ID} from {@link Tracks} {@link Uri}. */
        public static String getTrackId(Uri uri) {
            return uri.getPathSegments().get(1);
        }

        /**
         * Generate a {@link #TRACK_ID} that will always match the requested
         * {@link Tracks} details.
         */
        public static String generateTrackId(String title) {
            return ParserUtils.sanitizeId(title);
        }
    }

    /**
     * Rooms are physical locations at the conference venue.
     */
    public static class Rooms implements RoomsColumns, BaseColumns {
        public static final Uri CONTENT_URI =
                BASE_CONTENT_URI.buildUpon().appendPath(PATH_ROOMS).build();

        public static final String CONTENT_TYPE =
                "vnd.android.cursor.dir/vnd.devoxx.room";
        public static final String CONTENT_ITEM_TYPE =
                "vnd.android.cursor.item/vnd.devoxx.room";

        /** Default "ORDER BY" clause. */
        public static final String DEFAULT_SORT = RoomsColumns.ROOM_ID + " ASC";

        /** Build {@link Uri} for requested {@link #ROOM_ID}. */
        public static Uri buildRoomUri(String roomId) {
            return CONTENT_URI.buildUpon().appendPath(roomId).build();
        }

        /** Build {@link Uri} for requested {@link Rooms} with name {@link #NAME}. */
        public static Uri buildRoomsWithNameUri(String name) {
            return CONTENT_URI.buildUpon().appendPath(PATH_WITH_NAME).appendPath(name).build();
        }

        /** Read {@link #ROOM_ID} from {@link Rooms} {@link Uri}. */
        public static String getRoomId(Uri uri) {
            return uri.getPathSegments().get(1);
        }

        /** Read {@link #NAME} from {@link Rooms} {@link Uri}. */
        public static String getRoomName(Uri uri) {
            return uri.getPathSegments().get(2);
        }
    }

    /**
     * Each session is a block of time that has a {@link Tracks}, a
     * {@link Rooms}, and zero or more {@link Speakers}.
     */
    public static class Sessions implements SessionsColumns, BlocksColumns, RoomsColumns, BaseColumns {
        public static final Uri CONTENT_URI =
                BASE_CONTENT_URI.buildUpon().appendPath(PATH_SESSIONS).build();
        public static final Uri CONTENT_STARRED_URI =
            CONTENT_URI.buildUpon().appendPath(PATH_STARRED).build();
        public static final Uri CONTENT_NEW_URI =
            CONTENT_URI.buildUpon().appendPath(PATH_NEW).build();
        public static final Uri CONTENT_UPDATED_URI =
            CONTENT_URI.buildUpon().appendPath(PATH_UPDATED).build();
        public static final Uri CONTENT_UPDATED_STARRED_URI =
        	CONTENT_UPDATED_URI.buildUpon().appendPath(PATH_STARRED).build();

        public static final String CONTENT_TYPE =
                "vnd.android.cursor.dir/vnd.devoxx.session";
        public static final String CONTENT_ITEM_TYPE =
                "vnd.android.cursor.item/vnd.devoxx.session";

        public static final String BLOCK_ID = "block_id";
        public static final String ROOM_ID = "room_id";
        public static final String TRACK_ID = "track_id";

        public static final String STARRED_IN_BLOCK_COUNT = "starred_in_block_count";

        public static final String SEARCH_SNIPPET = "search_snippet";

        /** Default "ORDER BY" clause. */
        public static final String DEFAULT_SORT = Tables.SESSIONS + "." + SessionsColumns.SESSION_ID + " ASC";
        
        /** Build {@link Uri} for requested {@link #SESSION_ID}. */
        public static Uri buildSessionUri(String sessionId) {
            return CONTENT_URI.buildUpon().appendPath(sessionId).build();
        }

        /**
         * Build {@link Uri} that references any {@link Speakers} associated
         * with the requested {@link #SESSION_ID}.
         */
        public static Uri buildSpeakersDirUri(String sessionId) {
            return CONTENT_URI.buildUpon().appendPath(sessionId).appendPath(PATH_SPEAKERS).build();
        }

        /** Build {@link Uri} for requested {@link #SESSION_ID} for given {@link Speakers} with given {@link #SPEAKER_ID} */
        public static Uri buildSessionSpeakerUri(String sessionId, String speakerId) {
            return CONTENT_URI.buildUpon().appendPath(sessionId).appendPath(PATH_SPEAKERS).appendPath(speakerId).build();
        }

        /**
         * Build {@link Uri} that references any {@link Notes} associated with
         * the requested {@link #SESSION_ID}.
         */
        public static Uri buildNotesDirUri(String sessionId) {
            return CONTENT_URI.buildUpon().appendPath(sessionId).appendPath(PATH_NOTES).build();
        }

        public static Uri buildSessionsAtDirUri(long time) {
            return CONTENT_URI.buildUpon().appendPath(PATH_AT).appendPath(String.valueOf(time))
                    .build();
        }

        public static Uri buildSessionsParallelDirUri(String sessionId) {
            return CONTENT_URI.buildUpon().appendPath(PATH_PARALLEL).appendPath(sessionId).build();
        }

        public static Uri buildSearchUri(String query) {
            return CONTENT_URI.buildUpon().appendPath(PATH_SEARCH).appendPath(query).build();
        }

        public static boolean isSearchUri(Uri uri) {
            return PATH_SEARCH.equals(uri.getPathSegments().get(1));
        }

        /** Read {@link #SESSION_ID} from {@link Sessions} {@link Uri}. */
        public static String getSessionId(Uri uri) {
            return uri.getPathSegments().get(1);
        }

        public static String getSpeakerId(Uri uri) {
            return uri.getPathSegments().get(3);
        }

        public static String getSearchQuery(Uri uri) {
            return uri.getPathSegments().get(2);
        }

        /**
         * Generate a {@link #SESSION_ID} that will always match the requested
         * {@link Sessions} details.
         */
        public static String generateSessionId(String title) {
            return ParserUtils.sanitizeId(title);
        }

    }

    /**
     * Speakers are individual people that lead {@link Sessions}.
     */
    public static class Speakers implements SpeakersColumns, BaseColumns {
        public static final Uri CONTENT_URI =
                BASE_CONTENT_URI.buildUpon().appendPath(PATH_SPEAKERS).build();
        public static final Uri CONTENT_STARRED_URI =
            CONTENT_URI.buildUpon().appendPath(PATH_STARRED).build();

        public static final String CONTENT_TYPE =
                "vnd.android.cursor.dir/vnd.devoxx.speaker";
        public static final String CONTENT_ITEM_TYPE =
                "vnd.android.cursor.item/vnd.devoxx.speaker";

        public static final String CONTAINS_STARRED = "contains_starred";

        public static final String SEARCH_SNIPPET = "search_snippet";

        /** Default "ORDER BY" clause. */
        public static final String DEFAULT_SORT = SpeakersColumns.LAST_NAME + " ASC, " + SpeakersColumns.FIRST_NAME + " ASC";

        /** Build {@link Uri} for requested {@link #SPEAKER_ID}. */
        public static Uri buildSpeakerUri(String speakerId) {
            return CONTENT_URI.buildUpon().appendPath(speakerId).build();
        }

        /**
         * Build {@link Uri} that references any {@link Sessions} associated
         * with the requested {@link #SPEAKER_ID}.
         */
        public static Uri buildSessionsDirUri(String speakerId) {
            return CONTENT_URI.buildUpon().appendPath(speakerId).appendPath(PATH_SESSIONS).build();
        }

        /** Read {@link #SPEAKER_ID} from {@link Speakers} {@link Uri}. */
        public static Uri buildSearchUri(String query) {
            return CONTENT_URI.buildUpon().appendPath(PATH_SEARCH).appendPath(query).build();
        }

        public static boolean isSearchUri(Uri uri) {
        	return (uri.getPathSegments().size() > 1) 
        		&& PATH_SEARCH.equals(uri.getPathSegments().get(1)); 
        }

        public static String getSpeakerId(Uri uri) {
            return uri.getPathSegments().get(1);
        }

        public static String getSearchQuery(Uri uri) {
            return uri.getPathSegments().get(2);
        }

        /**
         * Generate a {@link #SPEAKER_ID} that will always match the requested
         * {@link Speakers} details.
         */
        public static String generateSpeakerId(String id) {
            return ParserUtils.sanitizeId(id);
        }

    }


    /**
     * Notes are user-generated data related to specific {@link Sessions}.
     */
    public static class Notes implements NotesColumns, BaseColumns {
        public static final Uri CONTENT_URI =
                BASE_CONTENT_URI.buildUpon().appendPath(PATH_NOTES).build();
        public static final Uri CONTENT_EXPORT_URI =
                CONTENT_URI.buildUpon().appendPath(PATH_EXPORT).build();

        /** {@link Sessions#SESSION_ID} that this note references. */
        public static final String SESSION_ID = "session_id";

        /** Default "ORDER BY" clause. */
        public static final String DEFAULT_SORT = NotesColumns.NOTE_TIME + " DESC";

        public static final String CONTENT_TYPE =
                "vnd.android.cursor.dir/vnd.devoxx.note";
        public static final String CONTENT_ITEM_TYPE =
                "vnd.android.cursor.item/vnd.devoxx.note";

        public static Uri buildNoteUri(long noteId) {
            return ContentUris.withAppendedId(CONTENT_URI, noteId);
        }
        
        public static long getNoteId(Uri uri) {
            return ContentUris.parseId(uri);
        }
    }


    public static class Sync implements SyncColumns, BaseColumns {
        public static final Uri CONTENT_URI =
                BASE_CONTENT_URI.buildUpon().appendPath(PATH_SYNC).build();

        public static final String CONTENT_TYPE =
                "vnd.android.cursor.dir/vnd.devoxx.sync";
        public static final String CONTENT_ITEM_TYPE =
                "vnd.android.cursor.item/vnd.devoxx.sync";

        public static final String DEFAULT_SORT = Tables.SYNC + "." + SyncColumns.URI + " ASC";

        public static Uri buildSyncUri(String uriId) {
            return CONTENT_URI.buildUpon().appendPath(uriId).build();
        }

        public static String getSyncId(Uri uri) {
            return uri.getPathSegments().get(1);
        }

        public static String generateSyncId(String uri) {
            return ParserUtils.sanitizeId(uri);
        }

    }

    public static class SearchSuggest {
        public static final Uri CONTENT_URI =
                BASE_CONTENT_URI.buildUpon().appendPath(PATH_SEARCH_SUGGEST).build();

        public static final String DEFAULT_SORT = SearchManager.SUGGEST_COLUMN_TEXT_1
                + " COLLATE NOCASE ASC";
    }

    private ScheduleContract() {
    }
    
}
