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

import net.peterkuterna.android.apps.devoxxsched.provider.ScheduleContract.Blocks;
import net.peterkuterna.android.apps.devoxxsched.provider.ScheduleContract.BlocksColumns;
import net.peterkuterna.android.apps.devoxxsched.provider.ScheduleContract.Notes;
import net.peterkuterna.android.apps.devoxxsched.provider.ScheduleContract.NotesColumns;
import net.peterkuterna.android.apps.devoxxsched.provider.ScheduleContract.Rooms;
import net.peterkuterna.android.apps.devoxxsched.provider.ScheduleContract.RoomsColumns;
import net.peterkuterna.android.apps.devoxxsched.provider.ScheduleContract.Sessions;
import net.peterkuterna.android.apps.devoxxsched.provider.ScheduleContract.SessionsColumns;
import net.peterkuterna.android.apps.devoxxsched.provider.ScheduleContract.Speakers;
import net.peterkuterna.android.apps.devoxxsched.provider.ScheduleContract.SpeakersColumns;
import net.peterkuterna.android.apps.devoxxsched.provider.ScheduleContract.SyncColumns;
import net.peterkuterna.android.apps.devoxxsched.provider.ScheduleContract.Tracks;
import net.peterkuterna.android.apps.devoxxsched.provider.ScheduleContract.TracksColumns;
import android.app.SearchManager;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;
import android.util.Log;


/**
 * Helper for managing {@link SQLiteDatabase} that stores data for
 * {@link ScheduleProvider}.
 */
public class ScheduleDatabase extends SQLiteOpenHelper {

	private static final String TAG = "ScheduleDatabase";

    private static final String DATABASE_NAME = "schedule.db";

    // NOTE: carefully update onUpgrade() when bumping database versions to make
    // sure user data is saved.

    private static final int VER_LAUNCH = 1;
    private static final int VER_ADD_NOTE_ON_SESSION = 2;
    private static final int VER_ALTER_NOTE_ON_SESSION = 3;
    private static final int VER_RECREATE_FULLTEXT_TABLE = 4;

    private static final int DATABASE_VERSION = VER_RECREATE_FULLTEXT_TABLE;

    interface Tables {
        String SESSIONS = "sessions";
        String SPEAKERS = "speakers";
        String ROOMS = "rooms";
        String BLOCKS = "blocks";
        String TRACKS = "tracks";
        String NOTES = "notes";
        String SYNC = "sync";
        String SESSIONS_SPEAKERS = "sessions_speakers";

        String SESSIONS_SEARCH = "sessions_search";
        String SPEAKERS_SEARCH = "speakers_search";

        String SEARCH_SUGGEST = "search_suggest";

        String SESSIONS_JOIN_BLOCKS_ROOMS_TRACKS = "sessions "
            + "LEFT OUTER JOIN blocks ON sessions.block_id=blocks.block_id "
            + "LEFT OUTER JOIN rooms ON sessions.room_id=rooms.room_id "
        	+ "LEFT OUTER JOIN tracks ON sessions.track_id=tracks.track_id";

        String SESSIONS_SPEAKERS_JOIN_SPEAKERS = "sessions_speakers "
            + "LEFT OUTER JOIN speakers ON sessions_speakers.speaker_id=speakers.speaker_id";

        String SESSIONS_SPEAKERS_JOIN_SESSIONS_BLOCKS_ROOMS_TRACKS = "sessions_speakers "
            + "LEFT OUTER JOIN sessions ON sessions_speakers.session_id=sessions.session_id "
            + "LEFT OUTER JOIN blocks ON sessions.block_id=blocks.block_id "
            + "LEFT OUTER JOIN rooms ON sessions.room_id=rooms.room_id "
        	+ "LEFT OUTER JOIN tracks ON sessions.track_id=tracks.track_id";

        String NOTES_JOIN_SESSIONS_TRACKS = "notes "
        	+ "LEFT OUTER JOIN sessions on notes.session_id=sessions.session_id "
        	+ "LEFT OUTER JOIN tracks on sessions.track_id=tracks.track_id";

        String SESSIONS_SEARCH_JOIN_SESSIONS_BLOCKS_ROOMS_TRACKS = "sessions_search "
            + "LEFT OUTER JOIN sessions ON sessions_search.session_id=sessions.session_id "
            + "LEFT OUTER JOIN blocks ON sessions.block_id=blocks.block_id "
            + "LEFT OUTER JOIN rooms ON sessions.room_id=rooms.room_id "
        	+ "LEFT OUTER JOIN tracks ON sessions.track_id=tracks.track_id";

        String SPEAKERS_SEARCH_JOIN_SPEAKERS = "speakers_search "
            + "LEFT OUTER JOIN speakers ON speakers_search.speaker_id=speakers.speaker_id";
    }

    private interface Triggers {
        String SESSIONS_SEARCH_INSERT = "sessions_search_insert";
        String SESSIONS_SEARCH_DELETE = "sessions_search_delete";
        String SESSIONS_SEARCH_UPDATE = "sessions_search_update";

        String SPEAKERS_SEARCH_INSERT = "speakers_search_insert";
        String SPEAKERS_SEARCH_DELETE = "speakers_search_delete";
        String SPEAKERS_SEARCH_UPDATE = "speakers_search_update";
    }

    public interface SessionsSpeakers {
        String SESSION_ID = "session_id";
        String SPEAKER_ID = "speaker_id";
    }

    interface SessionsSearchColumns {
        String SESSION_ID = "session_id";
        String BODY = "body";
    }

    interface SpeakersSearchColumns {
        String SPEAKER_ID = "speaker_id";
        String BODY = "body";
    }

    /** Fully-qualified field names. */
    private interface Qualified {
        String SESSIONS_SEARCH_SESSION_ID = Tables.SESSIONS_SEARCH + "."
                + SessionsSearchColumns.SESSION_ID;

        String SESSIONS_SEARCH = Tables.SESSIONS_SEARCH + "(" + SessionsSearchColumns.SESSION_ID
                + "," + SessionsSearchColumns.BODY + ")";

        String SPEAKERS_SEARCH_SPEAKER_ID = Tables.SPEAKERS_SEARCH + "."
        	+ SpeakersSearchColumns.SPEAKER_ID;

        String SPEAKERS_SEARCH = Tables.SPEAKERS_SEARCH + "(" + SpeakersSearchColumns.SPEAKER_ID
        	+ "," + SpeakersSearchColumns.BODY + ")";
    }

    /** {@code REFERENCES} clauses. */
    private interface References {
        String SESSION_ID = "REFERENCES " + Tables.SESSIONS + "(" + Sessions.SESSION_ID + ")";
        String SPEAKER_ID = "REFERENCES " + Tables.SPEAKERS + "(" + Speakers.SPEAKER_ID + ")";
        String ROOM_ID = "REFERENCES " + Tables.ROOMS + "(" + Rooms.ROOM_ID + ")";
        String BLOCK_ID = "REFERENCES " + Tables.BLOCKS + "(" + Blocks.BLOCK_ID + ")";
        String TRACK_ID = "REFERENCES " + Tables.TRACKS + "(" + Tracks.TRACK_ID + ")";
    }

    private interface Subquery {
        /**
         * Subquery used to build the {@link SessionsSearchColumns#BODY} string
         * used for indexing {@link Sessions} content.
         */
        String SESSIONS_BODY = "(new." + Sessions.TITLE + "||'; '||new." + Sessions.SUMMARY
                + "||'; '||new." + Sessions.EXPERIENCE + "||'; '||new." + Sessions.NOTE
                + ")";

        /**
         * Subquery used to build the {@link SpeakersSearchColumns#BODY} string
         * used for indexing {@link Speakers} content.
         */
        String SPEAKERS_BODY = "(new." + Speakers.FIRST_NAME + "||'; '||new." + Speakers.LAST_NAME
        		+ "||'; '||new." + Speakers.COMPANY + "||'; '||" + "new." + Speakers.BIO
        		+ ")";
    }

    public ScheduleDatabase(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        Log.d(TAG, "onCreate()");

        db.execSQL("CREATE TABLE " + Tables.SESSIONS + " ("
                + BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + SessionsColumns.SESSION_ID + " TEXT NOT NULL,"
                + Sessions.BLOCK_ID + " TEXT " + References.BLOCK_ID + ","
                + Sessions.ROOM_ID + " TEXT " + References.ROOM_ID + ","
                + Sessions.TRACK_ID + " TEXT " + References.TRACK_ID + ","
                + SessionsColumns.TITLE + " TEXT NOT NULL,"
                + SessionsColumns.SUMMARY + " TEXT NOT NULL,"
                + SessionsColumns.EXPERIENCE + " TEXT NOT NULL,"
                + SessionsColumns.TYPE + " TEXT,"
                + SessionsColumns.NOTE + " TEXT NOT NULL DEFAULT '',"
                + SessionsColumns.STARRED + " INTEGER NOT NULL DEFAULT 0,"
                + SessionsColumns.NEW + " INTEGER NOT NULL DEFAULT 0,"
                + SessionsColumns.UPDATED + " INTEGER NOT NULL DEFAULT 0,"
                + "UNIQUE (" + SessionsColumns.SESSION_ID + ") ON CONFLICT REPLACE)");

        db.execSQL("CREATE TABLE " + Tables.SPEAKERS + " ("
                + BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + SpeakersColumns.SPEAKER_ID + " TEXT NOT NULL,"
                + SpeakersColumns.FIRST_NAME + " TEXT NOT NULL,"
                + SpeakersColumns.LAST_NAME + " TEXT NOT NULL,"
                + SpeakersColumns.BIO + " TEXT NOT NULL,"
                + SpeakersColumns.COMPANY + " TEXT NOT NULL,"
                + SpeakersColumns.IMAGE_URL + " TEXT NOT NULL,"
                + "UNIQUE (" + SpeakersColumns.SPEAKER_ID + ") ON CONFLICT REPLACE)");
        
        db.execSQL("CREATE TABLE " + Tables.ROOMS + " ("
                + BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + RoomsColumns.ROOM_ID + " TEXT NOT NULL,"
                + RoomsColumns.NAME + " TEXT NOT NULL,"
                + RoomsColumns.CAPACITY + " TEXT NOT NULL,"
                + "UNIQUE (" + RoomsColumns.ROOM_ID + ") ON CONFLICT REPLACE)");

        db.execSQL("CREATE TABLE " + Tables.BLOCKS + " ("
                + BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + BlocksColumns.BLOCK_ID + " TEXT NOT NULL,"
                + BlocksColumns.BLOCK_TITLE + " TEXT NOT NULL,"
                + BlocksColumns.BLOCK_START + " INTEGER NOT NULL,"
                + BlocksColumns.BLOCK_END + " INTEGER NOT NULL,"
                + BlocksColumns.BLOCK_TYPE + " TEXT NOT NULL,"
                + "UNIQUE (" + BlocksColumns.BLOCK_ID + ") ON CONFLICT REPLACE)");

        db.execSQL("CREATE TABLE " + Tables.TRACKS + " ("
                + BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + TracksColumns.TRACK_ID + " TEXT NOT NULL,"
                + TracksColumns.TRACK_NAME + " TEXT,"
                + TracksColumns.TRACK_COLOR + " INTEGER,"
                + "UNIQUE (" + TracksColumns.TRACK_ID + ") ON CONFLICT REPLACE)");

        db.execSQL("CREATE TABLE " + Tables.SESSIONS_SPEAKERS + " ("
                + BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + SessionsSpeakers.SESSION_ID + " TEXT NOT NULL " + References.SESSION_ID + ","
                + SessionsSpeakers.SPEAKER_ID + " TEXT NOT NULL " + References.SPEAKER_ID + ","
                + "UNIQUE (" + SessionsSpeakers.SESSION_ID + ","
                        + SessionsSpeakers.SPEAKER_ID + ") ON CONFLICT REPLACE)");

        db.execSQL("CREATE TABLE " + Tables.NOTES + " ("
                + BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + Notes.SESSION_ID + " TEXT NOT NULL " + References.SESSION_ID + ","
                + NotesColumns.NOTE_TIME + " INTEGER NOT NULL,"
                + NotesColumns.NOTE_CONTENT + " TEXT)");

        db.execSQL("CREATE TABLE " + Tables.SYNC + " ("
                + BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + SyncColumns.URI_ID + " TEXT NOT NULL,"
                + SyncColumns.URI + " TEXT NOT NULL,"
                + SyncColumns.MD5 + " TEXT NOT NULL,"
                + "UNIQUE (" + SyncColumns.URI_ID + ") ON CONFLICT REPLACE)");

        createSessionsSearch(db, true);
        createSpeakersSearch(db, true);

        db.execSQL("CREATE TABLE " + Tables.SEARCH_SUGGEST + " ("
                + BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + SearchManager.SUGGEST_COLUMN_TEXT_1 + " TEXT NOT NULL)");
    }

    private static void createSessionsSearch(SQLiteDatabase db, boolean createTriggers) {
        // Using the "porter" tokenizer for simple stemming, so that
        // "frustration" matches "frustrated."

        db.execSQL("CREATE VIRTUAL TABLE " + Tables.SESSIONS_SEARCH + " USING fts3("
                + BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + SessionsSearchColumns.BODY + " TEXT NOT NULL,"
                + SessionsSearchColumns.SESSION_ID
                        + " TEXT NOT NULL " + References.SESSION_ID + ","
                + "UNIQUE (" + SessionsSearchColumns.SESSION_ID + ") ON CONFLICT REPLACE,"
                + "tokenize=porter)");

        if (createTriggers) createSessionsSearchTriggers(db);
    }
    
    /**
     * Create triggers that automatically build {@link Tables#SESSIONS_SEARCH}
     * as values are changed in {@link Tables#SESSIONS}.
     */
    private static void createSessionsSearchTriggers(SQLiteDatabase db) {
        db.execSQL("CREATE TRIGGER " + Triggers.SESSIONS_SEARCH_INSERT + " AFTER INSERT ON "
                + Tables.SESSIONS + " BEGIN INSERT INTO " + Qualified.SESSIONS_SEARCH + " "
                + " VALUES(new." + Sessions.SESSION_ID + ", " + Subquery.SESSIONS_BODY + ");"
                + " END;");

        db.execSQL("CREATE TRIGGER " + Triggers.SESSIONS_SEARCH_DELETE + " AFTER DELETE ON "
                + Tables.SESSIONS + " BEGIN DELETE FROM " + Tables.SESSIONS_SEARCH + " "
                + " WHERE " + Qualified.SESSIONS_SEARCH_SESSION_ID + "=old." + Sessions.SESSION_ID
                + ";" + " END;");

        db.execSQL("CREATE TRIGGER " + Triggers.SESSIONS_SEARCH_UPDATE + " AFTER UPDATE OF "
                + Sessions.TITLE + ", " + Sessions.SUMMARY + ", " + Sessions.EXPERIENCE 
                + ", " + Sessions.NOTE + " ON " + Tables.SESSIONS + " BEGIN UPDATE " + Tables.SESSIONS_SEARCH 
                + " SET " + SessionsSearchColumns.BODY + " = " + Subquery.SESSIONS_BODY 
                + " WHERE " + Qualified.SESSIONS_SEARCH_SESSION_ID + "=old." + Sessions.SESSION_ID
                + ";" + " END;");
    }

    private static void createSpeakersSearch(SQLiteDatabase db, boolean createTriggers) {
        // Using the "porter" tokenizer for simple stemming, so that
        // "frustration" matches "frustrated."

        db.execSQL("CREATE VIRTUAL TABLE " + Tables.SPEAKERS_SEARCH + " USING fts3("
                + BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + SpeakersSearchColumns.BODY + " TEXT NOT NULL,"
                + SpeakersSearchColumns.SPEAKER_ID
                        + " TEXT NOT NULL " + References.SPEAKER_ID + ","
                + "UNIQUE (" + SpeakersSearchColumns.SPEAKER_ID + ") ON CONFLICT REPLACE,"
                + "tokenize=porter)");

        if (createTriggers) createSpeakersSearchTriggers(db);
    }

    /**
     * Create triggers that automatically build {@link Tables#SPEAKERS_SEARCH}
     * as values are changed in {@link Tables#SPEAKERS}.
     */
    private static void createSpeakersSearchTriggers(SQLiteDatabase db) {
        db.execSQL("CREATE TRIGGER " + Triggers.SPEAKERS_SEARCH_INSERT + " AFTER INSERT ON "
                + Tables.SPEAKERS + " BEGIN INSERT INTO " + Qualified.SPEAKERS_SEARCH + " "
                + " VALUES(new." + Speakers.SPEAKER_ID + ", " + Subquery.SPEAKERS_BODY + ");"
                + " END;");

        db.execSQL("CREATE TRIGGER " + Triggers.SPEAKERS_SEARCH_DELETE + " AFTER DELETE ON "
                + Tables.SPEAKERS + " BEGIN DELETE FROM " + Tables.SPEAKERS_SEARCH + " "
                + " WHERE " + Qualified.SPEAKERS_SEARCH_SPEAKER_ID + "=old." + Speakers.SPEAKER_ID
                + ";" + " END;");

        db.execSQL("CREATE TRIGGER " + Triggers.SPEAKERS_SEARCH_UPDATE + " AFTER UPDATE OF "
                + Speakers.FIRST_NAME + ", " + Speakers.LAST_NAME + ", " + Speakers.COMPANY 
                + ", " + Speakers.BIO + " ON " + Tables.SPEAKERS + " BEGIN UPDATE " 
                + Tables.SPEAKERS_SEARCH + " SET " + SpeakersSearchColumns.BODY + " = " 
                + Subquery.SPEAKERS_BODY + " WHERE " + Qualified.SPEAKERS_SEARCH_SPEAKER_ID 
                + "=old." + Speakers.SPEAKER_ID + ";" + " END;");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.d(TAG, "onUpgrade() from " + oldVersion + " to " + newVersion);
        
        // NOTE: This switch statement is designed to handle cascading database
        // updates, starting at the current version and falling through to all
        // future upgrade cases. Only use "break;" when you want to drop and
        // recreate the entire database.
        int version = oldVersion;
        switch (version) {
            case VER_LAUNCH:
            	Log.d(TAG, "performing upgrade coming from VER_LAUNCH");
            	
                db.execSQL("ALTER TABLE " + Tables.SESSIONS + " ADD COLUMN "
                        + SessionsColumns.NOTE + " TEXT");
                db.execSQL("DROP TRIGGER " + Triggers.SESSIONS_SEARCH_DELETE);
                db.execSQL("DROP TRIGGER " + Triggers.SESSIONS_SEARCH_INSERT);
                db.execSQL("DROP TRIGGER " + Triggers.SESSIONS_SEARCH_UPDATE);
                
                createSessionsSearchTriggers(db);

                version = VER_ADD_NOTE_ON_SESSION;
            case VER_ADD_NOTE_ON_SESSION:
            	Log.d(TAG, "performing upgrade coming from VER_ADD_NOTE_ON_SESSION");

            	db.execSQL("DROP TRIGGER " + Triggers.SESSIONS_SEARCH_DELETE);
                db.execSQL("DROP TRIGGER " + Triggers.SESSIONS_SEARCH_INSERT);
                db.execSQL("DROP TRIGGER " + Triggers.SESSIONS_SEARCH_UPDATE);
                
            	db.execSQL("ALTER TABLE " + Tables.SESSIONS + " RENAME TO tmp_"
            			+ Tables.SESSIONS);
            	
                db.execSQL("CREATE TABLE " + Tables.SESSIONS + " ("
                        + BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                        + SessionsColumns.SESSION_ID + " TEXT NOT NULL,"
                        + Sessions.BLOCK_ID + " TEXT " + References.BLOCK_ID + ","
                        + Sessions.ROOM_ID + " TEXT " + References.ROOM_ID + ","
                        + Sessions.TRACK_ID + " TEXT " + References.TRACK_ID + ","
                        + SessionsColumns.TITLE + " TEXT NOT NULL,"
                        + SessionsColumns.SUMMARY + " TEXT NOT NULL,"
                        + SessionsColumns.EXPERIENCE + " TEXT NOT NULL,"
                        + SessionsColumns.TYPE + " TEXT,"
                        + SessionsColumns.NOTE + " TEXT NOT NULL DEFAULT '',"
                        + SessionsColumns.STARRED + " INTEGER NOT NULL DEFAULT 0,"
                        + SessionsColumns.NEW + " INTEGER NOT NULL DEFAULT 0,"
                        + SessionsColumns.UPDATED + " INTEGER NOT NULL DEFAULT 0,"
                        + "UNIQUE (" + SessionsColumns.SESSION_ID + ") ON CONFLICT REPLACE)");
                
                createSessionsSearchTriggers(db);

                db.execSQL("INSERT INTO " + Tables.SESSIONS + "("
                        + SessionsColumns.SESSION_ID + ", "
                        + Sessions.BLOCK_ID + ", "
                        + Sessions.ROOM_ID + ", "
                        + Sessions.TRACK_ID + ", "
                        + SessionsColumns.TITLE + ", "
                        + SessionsColumns.SUMMARY + ", "
                        + SessionsColumns.EXPERIENCE + ", "
                        + SessionsColumns.TYPE + ", "
                        + SessionsColumns.NOTE + ", "
                        + SessionsColumns.STARRED + ", "
                        + SessionsColumns.NEW + ", "
                        + SessionsColumns.UPDATED + ")"
                        + " SELECT "
                        + SessionsColumns.SESSION_ID + ", "
                        + Sessions.BLOCK_ID + ", "
                        + Sessions.ROOM_ID + ", "
                        + Sessions.TRACK_ID + ", "
                        + SessionsColumns.TITLE + ", "
                        + SessionsColumns.SUMMARY + ", "
                        + SessionsColumns.EXPERIENCE + ", "
                        + SessionsColumns.TYPE + ", "
                        + SessionsColumns.NOTE + ", "
                        + SessionsColumns.STARRED + ", "
                        + SessionsColumns.NEW + ", "
                        + SessionsColumns.UPDATED
                        + " FROM tmp_" + Tables.SESSIONS
                        + " WHERE " + SessionsColumns.NOTE + " IS NOT NULL");

                db.execSQL("INSERT INTO " + Tables.SESSIONS + "("
                        + SessionsColumns.SESSION_ID + ", "
                        + Sessions.BLOCK_ID + ", "
                        + Sessions.ROOM_ID + ", "
                        + Sessions.TRACK_ID + ", "
                        + SessionsColumns.TITLE + ", "
                        + SessionsColumns.SUMMARY + ", "
                        + SessionsColumns.EXPERIENCE + ", "
                        + SessionsColumns.TYPE + ", "
                        + SessionsColumns.STARRED + ", "
                        + SessionsColumns.NEW + ", "
                        + SessionsColumns.UPDATED + ")"
                        + " SELECT "
                        + SessionsColumns.SESSION_ID + ", "
                        + Sessions.BLOCK_ID + ", "
                        + Sessions.ROOM_ID + ", "
                        + Sessions.TRACK_ID + ", "
                        + SessionsColumns.TITLE + ", "
                        + SessionsColumns.SUMMARY + ", "
                        + SessionsColumns.EXPERIENCE + ", "
                        + SessionsColumns.TYPE + ", "
                        + SessionsColumns.STARRED + ", "
                        + SessionsColumns.NEW + ", "
                        + SessionsColumns.UPDATED
                        + " FROM tmp_" + Tables.SESSIONS
                        + " WHERE " + SessionsColumns.NOTE + " IS NULL");
                
                db.execSQL("DROP TABLE tmp_" + Tables.SESSIONS);

                version = VER_ALTER_NOTE_ON_SESSION;
            case VER_ALTER_NOTE_ON_SESSION:
                db.execSQL("DROP TABLE IF EXISTS " + Tables.SESSIONS_SEARCH);
                
                createSessionsSearch(db, false);
                
                db.execSQL("INSERT INTO " + Qualified.SESSIONS_SEARCH
                		+ " SELECT "
                		+ SessionsColumns.SESSION_ID
                		+ ", "
                		+ SessionsColumns.TITLE 
                		+ "||'; '||" 
                		+ SessionsColumns.SUMMARY
                        + "||'; '||" 
                        + SessionsColumns.EXPERIENCE 
                        + "||'; '||" 
                        + Sessions.NOTE
                        + " FROM " + Tables.SESSIONS);

                version = VER_RECREATE_FULLTEXT_TABLE;
        }

        Log.d(TAG, "after upgrade logic, at version " + version);
        if (version != DATABASE_VERSION) {
            Log.w(TAG, "Destroying old data during upgrade");

            db.execSQL("DROP TABLE IF EXISTS " + Tables.SESSIONS);
            db.execSQL("DROP TABLE IF EXISTS " + Tables.SPEAKERS);
            db.execSQL("DROP TABLE IF EXISTS " + Tables.ROOMS);
            db.execSQL("DROP TABLE IF EXISTS " + Tables.BLOCKS);
            db.execSQL("DROP TABLE IF EXISTS " + Tables.TRACKS);
            db.execSQL("DROP TABLE IF EXISTS " + Tables.SESSIONS_SPEAKERS);
            db.execSQL("DROP TABLE IF EXISTS " + Tables.NOTES);
            db.execSQL("DROP TABLE IF EXISTS " + Tables.SYNC);

            db.execSQL("DROP TRIGGER IF EXISTS " + Triggers.SESSIONS_SEARCH_INSERT);
            db.execSQL("DROP TRIGGER IF EXISTS " + Triggers.SESSIONS_SEARCH_DELETE);
            db.execSQL("DROP TRIGGER IF EXISTS " + Triggers.SESSIONS_SEARCH_UPDATE);
            db.execSQL("DROP TABLE IF EXISTS " + Tables.SESSIONS_SEARCH);

            db.execSQL("DROP TRIGGER IF EXISTS " + Triggers.SPEAKERS_SEARCH_INSERT);
            db.execSQL("DROP TRIGGER IF EXISTS " + Triggers.SPEAKERS_SEARCH_DELETE);
            db.execSQL("DROP TRIGGER IF EXISTS " + Triggers.SPEAKERS_SEARCH_UPDATE);
            db.execSQL("DROP TABLE IF EXISTS " + Tables.SPEAKERS_SEARCH);

            db.execSQL("DROP TABLE IF EXISTS " + Tables.SEARCH_SUGGEST);

            onCreate(db);
        }
    }
    
}
