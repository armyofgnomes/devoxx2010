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
 * Adapted by Peter Kuterna for the Devoxx conference.
 */
package net.peterkuterna.android.apps.devoxxsched.ui;

import net.peterkuterna.android.apps.devoxxsched.R;
import net.peterkuterna.android.apps.devoxxsched.provider.ScheduleContract.Notes;
import net.peterkuterna.android.apps.devoxxsched.provider.ScheduleContract.Sessions;
import net.peterkuterna.android.apps.devoxxsched.provider.ScheduleContract.Tracks;
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
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.StateListDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.text.format.DateUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

/**
 * {@link ListActivity} that displays a set of {@link Notes}, as requested
 * through {@link Intent#getData()}.
 */
public class NotesActivity extends ListActivity implements AsyncQueryListener {

	private static final String TAG = "NotesActivity";
	
    public static final String EXTRA_SHOW_INSERT = "net.peterkuterna.android.apps.devoxxsched.extra.SHOW_INSERT";
    
    private static final String DIALOG_NOTE_ID_ARG = "id";

    private Uri notesUri;
    private NotesAdapter mAdapter;

    private boolean mShowInsert = false;
    private boolean categoryTab = false;

    private NotifyingAsyncQueryHandler mHandler;

	public static final class NotesListItemViews {
		TextView sessionTitle;
		ImageView groupIndicator;
		View gotoSessionView;
		TextView noteContent;
		TextView noteTime;
		View deleteNoteView;
	}
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        notesUri = getIntent().getData();
        
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

        // Start background query to load notes
        mHandler = new NotifyingAsyncQueryHandler(getContentResolver(), this);
    }

    @Override
    protected void onResume() {
        startQuery();

        super.onResume();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mAdapter.changeCursor(null);
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
			mAdapter.onContentChanged();
        }
    }

    /** {@inheritDoc} */
    public void onQueryComplete(int token, Object cookie, Cursor cursor) {
        mAdapter.changeCursor(cursor);
    }

    /** {@inheritDoc} */
    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        if (id >= 0) {
        	if (mAdapter.isGroupHeader(position)) {
        		mAdapter.toggleGroup(position);
        	} else {
	            // Edit an existing note
	            final Uri noteUri = Notes.buildNoteUri(id);
	            startActivity(new Intent(Intent.ACTION_EDIT, noteUri));
        	}
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
    
    private void startQuery() {
    	mHandler.cancelOperation(NotesQuery.TOKEN);
        mHandler.startQuery(NotesQuery.TOKEN, notesUri, NotesQuery.PROJECTION, NotesQuery.SORT);
    }
    
    private class NotesAdapter extends GroupingListAdapter implements View.OnClickListener {
    	
    	public NotesAdapter(Context context) {
    		super(context);
    	}

        @Override
        protected void onContentChanged() {
            startQuery();
        }

        @Override
		public void onClick(View view) {
        	final ClickItem clickItem = (ClickItem) view.getTag();
        	switch (clickItem.getItemType()) {
        		case ClickItem.SESSION_ITEM:
                	final Uri sessionUri = Sessions.buildSessionUri(clickItem.getId());
                    final Intent intent = new Intent(Intent.ACTION_VIEW, sessionUri);
                    startActivity(intent);
        			break;
        		case ClickItem.NOTE_ITEM:
                    Bundle bundle = new Bundle();
                    bundle.putLong(DIALOG_NOTE_ID_ARG, Long.valueOf(clickItem.getId()));
                    showDialog(R.id.dialog_delete_confirm, bundle);
        			break;
        	}
		}

		@Override
		protected void addGroups(Cursor cursor) {
			int count = cursor.getCount();
			if (categoryTab || count == 0) {
				return;
			}
			
			int groupItemCount = 1;
			
			String currentValue = null;
			String value = null;
			cursor.moveToFirst();
			currentValue = cursor.getString(NotesQuery.SESSION_ID);
			for (int i = 1; i < count; i++) {
				cursor.moveToNext();
				value = cursor.getString(NotesQuery.SESSION_ID);
				boolean sameSession = currentValue.equals(value);
				if (sameSession) {
					groupItemCount++;
				} else {
					addGroup(i - groupItemCount, groupItemCount, true);
					
					groupItemCount = 1;
					
					String temp = currentValue;
					currentValue = value;
					value = temp;
				}
			}
			addGroup(count - groupItemCount, groupItemCount, true);
		}

		@Override
		protected void bindChildView(View view, Context context, Cursor cursor) {
			bindStandAloneView(view, context, cursor);
		}

		@Override
		protected void bindGroupView(View view, Context context, Cursor cursor,
				int groupSize, boolean expanded) {
			final NotesListItemViews views = (NotesListItemViews) view.getTag();
			int groupIndicator = expanded 
				? R.drawable.expander_ic_maximized
				: R.drawable.expander_ic_minimized;
			views.groupIndicator.setImageResource(groupIndicator);
			views.gotoSessionView.setOnClickListener(this);
			views.gotoSessionView.setTag(new ClickItem(ClickItem.SESSION_ITEM, cursor.getString(NotesQuery.SESSION_ID)));
			views.sessionTitle.setText(cursor.getString(NotesQuery.SESSION_TITLE));
			StateListDrawable drawable = new StateListDrawable();
			drawable.addState(new int [] { -android.R.attr.state_window_focused }, 
					new ColorDrawable(android.R.color.transparent));
			drawable.addState(new int [] { -android.R.attr.state_focused, android.R.attr.state_pressed }, 
					new ColorDrawable(android.R.drawable.list_selector_background));
			drawable.addState(new int [] { android.R.attr.state_selected }, 
					new ColorDrawable(android.R.color.transparent));
			drawable.addState(new int [] { -android.R.attr.state_selected }, 
					new ColorDrawable(UIUtils.lightenColor(cursor.getInt(NotesQuery.TRACK_COLOR))));
			view.setBackgroundDrawable(drawable);
		}

		@Override
		protected void bindStandAloneView(View view, Context context,
				Cursor cursor) {
			final NotesListItemViews views = (NotesListItemViews) view.getTag();
			views.noteContent.setText(cursor.getString(NotesQuery.NOTE_CONTENT));
			final long time = cursor.getLong(NotesQuery.NOTE_TIME);
			final CharSequence relativeTime = DateUtils.getRelativeTimeSpanString(time);
			views.noteTime.setText(relativeTime);
			views.deleteNoteView.setOnClickListener(this);
			views.deleteNoteView.setTag(new ClickItem(ClickItem.NOTE_ITEM, String.valueOf(cursor.getInt(NotesQuery._ID))));
		}

		@Override
		protected View newChildView(Context context, ViewGroup parent) {
			return newStandAloneView(context, parent);
		}

		@Override
		protected View newGroupView(Context context, ViewGroup parent) {
			View view = getLayoutInflater().inflate(R.layout.notes_list_group_item, parent, false);
			findAndCacheViews(view);
			return view;
		}

		@Override
		protected View newStandAloneView(Context context, ViewGroup parent) {
			View view = getLayoutInflater().inflate(R.layout.notes_list_child_item, parent, false);
			findAndCacheViews(view);
			return view;
		}
    	
		@Override
		public boolean isEmpty() {
            return mShowInsert ? false : super.isEmpty();
		}

		private void findAndCacheViews(View view) {
            // Get the views to bind to
            NotesListItemViews views = new NotesListItemViews();
            views.sessionTitle = (TextView) view.findViewById(R.id.session_title);
            views.groupIndicator = (ImageView) view.findViewById(R.id.groupIndicator);
            views.gotoSessionView = view.findViewById(R.id.goto_icon);
            views.noteContent = (TextView) view.findViewById(R.id.note_content);
            views.noteTime = (TextView) view.findViewById(R.id.note_time);
            views.deleteNoteView = view.findViewById(R.id.delete_icon);
            view.setTag(views);
        }
		
    }
    
    private class ClickItem {
    	public static final int SESSION_ITEM = 0x01;
    	public static final int NOTE_ITEM = 0x02;

    	private final int itemType;
    	private final String id;
		
    	public ClickItem(int itemType, String id) {
			this.itemType = itemType;
			this.id = id;
		}

		public int getItemType() {
			return itemType;
		}

		public String getId() {
			return id;
		}
    }

    /** {@link Notes} query parameters. */
    private interface NotesQuery {
    	int TOKEN = 0x01;
    	
        String[] PROJECTION = {
                BaseColumns._ID,
                Notes.NOTE_TIME,
                Notes.NOTE_CONTENT,
                Sessions.SESSION_ID,
                Sessions.TITLE,
                Tracks.TRACK_COLOR,
        };
        
        String SORT = Sessions.TITLE + " ASC, " + Notes.NOTE_TIME + " DESC";


        int _ID = 0;
        int NOTE_TIME = 1;
        int NOTE_CONTENT = 2;
        int SESSION_ID = 3;
        int SESSION_TITLE = 4;
        int TRACK_COLOR = 5;
    }
}
