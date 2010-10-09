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
import net.peterkuterna.android.apps.devoxxsched.provider.ScheduleContract.Sessions;
import net.peterkuterna.android.apps.devoxxsched.util.NotifyingAsyncQueryHandler;
import net.peterkuterna.android.apps.devoxxsched.util.UIUtils;
import net.peterkuterna.android.apps.devoxxsched.util.NotifyingAsyncQueryHandler.AsyncQueryListener;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.text.format.DateUtils;
import android.view.ContextMenu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.AdapterView;
import android.widget.CursorAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AdapterView.AdapterContextMenuInfo;

/**
 * {@link ListActivity} that displays a set of {@link Notes}, as requested
 * through {@link Intent#getData()}.
 */
public class NotesActivity extends ListActivity implements AsyncQueryListener {

    public static final String EXTRA_SHOW_INSERT = "net.peterkuterna.android.apps.devoxxsched.extra.SHOW_INSERT";
    
    private static final String DIALOG_NOTE_ID_ARG = "id";

    private NotesAdapter mAdapter;

    private boolean mShowInsert = false;
    private boolean categoryTab = false;

    private NotifyingAsyncQueryHandler mHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final Uri notesUri = getIntent().getData();
        
        if (!getIntent().hasCategory(Intent.CATEGORY_TAB)) {
            setContentView(R.layout.activity_notes);
            ((TextView) findViewById(R.id.title_text)).setText(getTitle());
        } else {
            setContentView(R.layout.activity_notes_content);
            categoryTab = true;
        }

        mShowInsert = getIntent().getBooleanExtra(EXTRA_SHOW_INSERT, false);
        if (mShowInsert) {
            final ListView listView = getListView();
            final View view = getLayoutInflater().inflate(R.layout.list_item_note_create,
                    listView, false);
            listView.addHeaderView(view, null, true);
        }

        mAdapter = new NotesAdapter(this);
        setListAdapter(mAdapter);

        registerForContextMenu(getListView());
        
        // Start background query to load notes
        mHandler = new NotifyingAsyncQueryHandler(getContentResolver(), this);
        mHandler.startQuery(notesUri, NotesQuery.PROJECTION, Notes.DEFAULT_SORT);
    }

    @Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
    	super.onCreateContextMenu(menu, v, menuInfo);
    	final AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
    	final int position = info.position;
    	if (!mShowInsert || mShowInsert && position > 0) {
    		final MenuInflater inflater = getMenuInflater();
    		inflater.inflate(R.menu.context_menu_notes, menu);
    		if (categoryTab) {
    			menu.removeItem(R.id.note_goto_session);
    		}
    	}
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
		switch (item.getItemId()) {
			case R.id.note_goto_session:
				if (mAdapter.getCursor().moveToPosition(mShowInsert ? info.position + 1 : info.position)) {
					final String sessionId = mAdapter.getCursor().getString(NotesQuery.SESSION_ID);
					final Uri sessionUri = Sessions.buildSessionUri(sessionId);
			        final Intent intent = new Intent(Intent.ACTION_VIEW, sessionUri);
			        startActivity(intent);
				}
		        return true;
			case R.id.note_delete:
				Bundle bundle = new Bundle();
				bundle.putLong(DIALOG_NOTE_ID_ARG, info.id);
				showDialog(R.id.dialog_delete_confirm, bundle);
				return true;
			default:
				return super.onContextItemSelected(item);
	  	}
	}

    @Override
	protected void onPrepareDialog(int id, Dialog dialog, Bundle args) {
        switch (id) {
        	case R.id.dialog_delete_confirm:
        		long notesId = args.getLong(DIALOG_NOTE_ID_ARG);
        		((AlertDialog) dialog).setButton(
        				AlertDialog.BUTTON_POSITIVE, 
        				getString(android.R.string.ok), 
        				new DeleteConfirmClickListener(notesId));
        		break;
        	default:
        		super.onPrepareDialog(id, dialog, args);
        }
	}

	@Override
    protected Dialog onCreateDialog(int id, Bundle bundle) {
        switch (id) {
            case R.id.dialog_delete_confirm: {
                return new AlertDialog.Builder(this)
                        .setTitle(R.string.note_delete_title)
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .setMessage(R.string.note_delete_confirm)
                        .setNegativeButton(android.R.string.cancel, null)
                        .setPositiveButton(android.R.string.ok, null)
                        .setCancelable(false)
                        .create();
            }
        }
        return super.onCreateDialog(id, bundle);
    }

    private class DeleteConfirmClickListener implements DialogInterface.OnClickListener {
    	private final long notesId;
    	
		public DeleteConfirmClickListener(long notesId) {
			this.notesId = notesId;
		}

		public void onClick(DialogInterface dialog, int which) {
			final Uri uri = Notes.buildNoteUri(notesId);
			mHandler.startDelete(uri);
			mAdapter.getCursor().requery();
        }
    }

    /** {@inheritDoc} */
    public void onQueryComplete(int token, Object cookie, Cursor cursor) {
        startManagingCursor(cursor);
        mAdapter.changeCursor(cursor);
    }

    /** {@inheritDoc} */
    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        if (id >= 0) {
            // Edit an existing note
            final Uri noteUri = Notes.buildNoteUri(id);
            startActivity(new Intent(Intent.ACTION_EDIT, noteUri));
        } else {
            // Insert new note
            final Uri notesDirUri = getIntent().getData();
            startActivity(new Intent(Intent.ACTION_INSERT, notesDirUri));
        }
    }

    /** Handle "home" title-bar action. */
    public void onHomeClick(View v) {
        UIUtils.goHome(this);
    }

    /** Handle "share" title-bar action. */
    public void onShareClick(View v) {
        final String shareText = getString(R.string.share_notes);

        final Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("plain/html");
        intent.putExtra(Intent.EXTRA_SUBJECT, shareText);
        intent.putExtra(Intent.EXTRA_TEXT, shareText);
        intent.putExtra(Intent.EXTRA_STREAM, Notes.CONTENT_EXPORT_URI);

        startActivity(Intent.createChooser(intent, getText(R.string.title_share)));
    }

    /** Handle "search" title-bar action. */
    public void onSearchClick(View v) {
        UIUtils.goSearch(this);
    }

    /**
     * {@link CursorAdapter} that renders a {@link NotesQuery}.
     */
    private class NotesAdapter extends CursorAdapter {
        public NotesAdapter(Context context) {
            super(context, null);
        }

        /** {@inheritDoc} */
        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            return getLayoutInflater().inflate(R.layout.list_item_note, parent, false);
        }

        /** {@inheritDoc} */
        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            // TODO: format notes with better layout
            ((TextView)view.findViewById(R.id.note_content)).setText(cursor
                    .getString(NotesQuery.NOTE_CONTENT));

            // TODO: format using note_before/into/after
            final long time = cursor.getLong(NotesQuery.NOTE_TIME);
            final CharSequence relativeTime = DateUtils.getRelativeTimeSpanString(time);
            ((TextView) view.findViewById(R.id.note_time)).setText(relativeTime);
        }

        @Override
        public boolean isEmpty() {
            return mShowInsert ? false : super.isEmpty();
        }
    }

    /** {@link Notes} query parameters. */
    private interface NotesQuery {
        String[] PROJECTION = {
                BaseColumns._ID,
                Notes.NOTE_TIME,
                Notes.NOTE_CONTENT,
                Sessions.SESSION_ID,
        };

        int _ID = 0;
        int NOTE_TIME = 1;
        int NOTE_CONTENT = 2;
        int SESSION_ID = 3;
    }
}
