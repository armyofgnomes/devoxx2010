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

package net.peterkuterna.android.apps.devoxxsched.ui;

import net.peterkuterna.android.apps.devoxxsched.R;
import net.peterkuterna.android.apps.devoxxsched.provider.ScheduleContract.Notes;
import net.peterkuterna.android.apps.devoxxsched.util.NotifyingAsyncQueryHandler;
import net.peterkuterna.android.apps.devoxxsched.util.UIUtils;
import net.peterkuterna.android.apps.devoxxsched.util.NotifyingAsyncQueryHandler.AsyncQueryListener;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

/**
 * Editor for {@link Notes}, handles both {@link Intent#ACTION_INSERT} and
 * {@link Intent#ACTION_EDIT}.
 */
public class NoteEditActivity extends Activity implements AsyncQueryListener {

    private EditText mText;

    private NotifyingAsyncQueryHandler mHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_note_edit);

        final String customTitle = getIntent().getStringExtra(Intent.EXTRA_TITLE);
        ((TextView) findViewById(R.id.title_text)).setText(
                customTitle != null ? customTitle : getTitle());

        mText = (EditText) findViewById(android.R.id.text1);

        mHandler = new NotifyingAsyncQueryHandler(getContentResolver(), this);

        final String action = getIntent().getAction();
        if (Intent.ACTION_EDIT.equals(action) && savedInstanceState == null) {
            // Start background query to load current state
            final Uri noteUri = getIntent().getData();
            mHandler.startQuery(noteUri, NotesQuery.PROJECTION);
        }
    }

    /** {@inheritDoc} */
    public void onQueryComplete(int token, Object cookie, Cursor cursor) {
        try {
            if (!cursor.moveToFirst()) return;

            // Load current note content for editing
            mText.setText(cursor.getString(NotesQuery.NOTE_CONTENT));

        } finally {
            cursor.close();
        }
    }

    public void onSaveClick(View v) {
        saveContent();
    }

    public void onCancelClick(View v) {
        final String noteContent = mText.getText().toString();
        if (TextUtils.isEmpty(noteContent)) {
            // When empty content, shortcut to cancel without confirm step
            cancelContent();
        } else {
            showDialog(R.id.dialog_cancel_confirm);
        }
    }

    /** Handle "home" title-bar action. */
    public void onHomeClick(View v) {
        UIUtils.goHome(this);
    }

    /** Handle "refresh" title-bar action. */
    public void onRefreshClick(View v) {
    }

    /** Handle "search" title-bar action. */
    public void onSearchClick(View v) {
        UIUtils.goSearch(this);
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        switch (id) {
            case R.id.dialog_cancel_confirm: {
                return new AlertDialog.Builder(this)
                        .setTitle(R.string.note_cancel_title)
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .setMessage(R.string.note_cancel_confirm)
                        .setNegativeButton(android.R.string.cancel, null)
                        .setPositiveButton(android.R.string.ok, new CancelConfirmClickListener())
                        .setCancelable(false)
                        .create();
            }
        }
        return null;
    }

    private class CancelConfirmClickListener implements DialogInterface.OnClickListener {
        public void onClick(DialogInterface dialog, int which) {
            cancelContent();
        }
    }

    /**
     * Persist the contents of into {@link Notes} backend. The actual save is
     * performed asynchronously, so this method doesn't block.
     */
    private void saveContent() {
        final String noteContent = mText.getText().toString();

        // TODO: consider passing off to startService() to prevent our
        // process from being killed before note is actually persisted.

        // When empty content, treat as discard
        if (TextUtils.isEmpty(noteContent)) {
            cancelContent();
            return;
        }

        // Always store updated note content
        final ContentValues values = new ContentValues();
        values.put(Notes.NOTE_CONTENT, noteContent);

        final String action = getIntent().getAction();
        if (Intent.ACTION_INSERT.equals(action)) {
            // Insert also includes current timestamp
            values.put(Notes.NOTE_TIME, System.currentTimeMillis());

            final Uri notesDirUri = getIntent().getData();
            mHandler.startInsert(notesDirUri, values);
        } else if (Intent.ACTION_EDIT.equals(action)) {
            final Uri noteUri = getIntent().getData();
            mHandler.startUpdate(noteUri, values);
        }

        finish();
    }

    private void cancelContent() {
        finish();
    }

    /** {@link Notes} query parameters. */
    private interface NotesQuery {
        String[] PROJECTION = {
                Notes.NOTE_TIME,
                Notes.NOTE_CONTENT,
        };

        int NOTE_TIME = 0;
        int NOTE_CONTENT = 1;
    }
}
