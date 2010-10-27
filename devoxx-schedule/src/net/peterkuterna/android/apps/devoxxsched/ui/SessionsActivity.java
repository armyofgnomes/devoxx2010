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
 * Added headers with track color.
 * Added highlighting of sessions at the same time.
 */
package net.peterkuterna.android.apps.devoxxsched.ui;

import static net.peterkuterna.android.apps.devoxxsched.util.UIUtils.buildStyledSnippet;
import static net.peterkuterna.android.apps.devoxxsched.util.UIUtils.formatSessionSubtitle;

import java.util.ArrayList;

import net.peterkuterna.android.apps.devoxxsched.R;
import net.peterkuterna.android.apps.devoxxsched.provider.ScheduleContract.Blocks;
import net.peterkuterna.android.apps.devoxxsched.provider.ScheduleContract.Rooms;
import net.peterkuterna.android.apps.devoxxsched.provider.ScheduleContract.Sessions;
import net.peterkuterna.android.apps.devoxxsched.provider.ScheduleContract.Tracks;
import net.peterkuterna.android.apps.devoxxsched.util.Lists;
import net.peterkuterna.android.apps.devoxxsched.util.NotifyingAsyncQueryHandler;
import net.peterkuterna.android.apps.devoxxsched.util.NotifyingAsyncQueryHandler.AsyncQueryListener;
import net.peterkuterna.android.apps.devoxxsched.util.UIUtils;
import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.CursorWrapper;
import android.database.DataSetObserver;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.provider.BaseColumns;
import android.text.Spannable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CursorAdapter;
import android.widget.ListView;
import android.widget.TextView;

/**
 * {@link ListActivity} that displays a set of {@link Sessions}, as requested
 * through {@link Intent#getData()}.
 */
public class SessionsActivity extends ListActivity implements AsyncQueryListener {

    private static final String TAG = "SessionsActivity";
    
    public static final String EXTRA_TRACK_COLOR = "net.peterkuterna.android.apps.devoxxsched.extra.TRACK_COLOR";
    public static final String EXTRA_NO_WEEKDAY_HEADER = "net.peterkuterna.android.apps.devoxxsched.extra.NO_WEEKDAY_HEADER";
    public static final String EXTRA_HIHGLIGHT_PARALLEL_STARRED = "net.peterkuterna.android.apps.devoxxsched.extra.HIGHLIGHT_PARALLEL_STARRED";

    private CursorAdapter mAdapter;

    private NotifyingAsyncQueryHandler mHandler;
    private Handler mMessageQueueHandler = new Handler();
    private boolean mNoWeekdayHeader = false;
    private boolean mHighlightParallelStarred = false;
    
    private int mTrackColor= -1;
    
//    private final Reflect reflect = new Reflect();

    private static final String SESSIONS_SORT = Sessions.BLOCK_START + " ASC," + Rooms.NAME + " ASC";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!getIntent().hasCategory(Intent.CATEGORY_TAB)) {
            setContentView(R.layout.activity_sessions);

            final String customTitle = getIntent().getStringExtra(Intent.EXTRA_TITLE);
            ((TextView) findViewById(R.id.title_text)).setText(
                    customTitle != null ? customTitle : getTitle());

        } else {
            setContentView(R.layout.activity_sessions_content);
        }

        final Intent intent = getIntent();
        final Uri sessionsUri = intent.getData();

        mTrackColor = intent.getIntExtra(EXTRA_TRACK_COLOR, -1);
        mNoWeekdayHeader = intent.getBooleanExtra(EXTRA_NO_WEEKDAY_HEADER, false);
        mHighlightParallelStarred = intent.getBooleanExtra(EXTRA_HIHGLIGHT_PARALLEL_STARRED, false);
        
        if (mTrackColor != -1) UIUtils.setTitleBarColor(findViewById(R.id.title_container), mTrackColor);

        String[] projection;
        String sort;
        if (!Sessions.isSearchUri(sessionsUri)) {
            mAdapter = new SessionsAdapter(this);
            projection = SessionsQuery.PROJECTION;
            sort = SESSIONS_SORT;
        } else {
           	mAdapter = new SearchAdapter(this);
           	mNoWeekdayHeader = true;
            projection = SearchQuery.PROJECTION;
            sort = Sessions.DEFAULT_SORT;
        }

        setListAdapter(mAdapter);

        // Start background query to load sessions
        mHandler = new NotifyingAsyncQueryHandler(getContentResolver(), this);
        mHandler.startQuery(sessionsUri, projection, sort);
    }

    /** {@inheritDoc} */
    public void onQueryComplete(int token, Object cookie, Cursor cursor) {
    	// TODO implement scrolling correctly
//    	int scrollPos = -1;
    	if (mNoWeekdayHeader) {
    		startManagingCursor(cursor);
//    		scrollPos = getScrollPosition(cursor);
    		mAdapter.changeCursor(cursor);
    	} else {
	    	final SessionsCursorWrapper cursorWrapper = new SessionsCursorWrapper(cursor, this);
	        startManagingCursor(cursorWrapper);
//    		scrollPos = getScrollPosition(cursorWrapper);
	        mAdapter.changeCursor(cursorWrapper);
    	}
//    	if (scrollPos != -1) reflect.scrollTo(getListView(), scrollPos, scrollPos);
    }
    
//    private int getScrollPosition(Cursor cursor) {
//    	int scrollPos = -1;
//    	
//    	final long currentTime = System.currentTimeMillis();
//    	
//        if (currentTime > UIUtils.CONFERENCE_START_MILLIS &&
//        		currentTime < UIUtils.CONFERENCE_END_MILLIS) {
//	    	for (int i = 0; i < cursor.getCount(); i++) {
//	    		if (cursor instanceof SessionsCursorWrapper) {
//	    			final SessionsCursorWrapper cursorWrapper = (SessionsCursorWrapper) cursor;
//	    			if (cursorWrapper.getItemViewType(i) != 0) continue;
//	    		}
//	    		cursor.moveToPosition(i);
//	    		long blockEnd = cursor.getLong(SessionsQuery.BLOCK_END);
//	    		if (currentTime < blockEnd) {
//	    			scrollPos = i;
//	    			break;
//	    		}
//	    	}
//	    	
//	    	if ((cursor instanceof SessionsCursorWrapper) && (scrollPos == 1)) scrollPos = 0;
//        }
//    	
//    	return scrollPos;
//    }

    @Override
    protected void onResume() {
        super.onResume();
        mMessageQueueHandler.post(mRefreshSessionsRunnable);
    }

    @Override
    protected void onPause() {
        mMessageQueueHandler.removeCallbacks(mRefreshSessionsRunnable);
        super.onPause();
    }

    /** {@inheritDoc} */
    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        // Launch viewer for specific session, passing along any track knowledge
        // that should influence the title-bar.
        final Cursor cursor = (Cursor) mAdapter.getItem(position);
        final String sessionId = cursor.getString(cursor.getColumnIndex(Sessions.SESSION_ID));
        final Uri sessionUri = Sessions.buildSessionUri(sessionId);
        final Intent intent = new Intent(Intent.ACTION_VIEW, sessionUri);
        startActivity(intent);
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
     * {@link CursorAdapter} that renders a {@link SessionsQuery}.
     */
    private class SessionsAdapter extends CursorAdapter {
        public SessionsAdapter(Context context) {
            super(context, null);
        }

        /** {@inheritDoc} */
        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
        	int cursorInfo = net.peterkuterna.android.apps.devoxxsched.ui.SessionsActivity.SessionsCursorWrapper.CursorInfo.SESSION_TYPE;
        	if (!mNoWeekdayHeader) {
        		cursorInfo = cursor.getInt(SessionsCursorWrapper.CURSOR_INFO_COLUMN_INDEX);
        	}
        	switch (cursorInfo) {
        		case net.peterkuterna.android.apps.devoxxsched.ui.SessionsActivity.SessionsCursorWrapper.CursorInfo.DAY_TYPE:
        			View v = getLayoutInflater().inflate(R.layout.list_item_session_header, parent,false);
        			if (mTrackColor != -1) {
        				UIUtils.setHeaderColor(v, mTrackColor);
        			}
        			return v;
        		case net.peterkuterna.android.apps.devoxxsched.ui.SessionsActivity.SessionsCursorWrapper.CursorInfo.SESSION_TYPE:
        		default:
        			return getLayoutInflater().inflate(R.layout.list_item_session, parent,false);
        	}
        }

        /** {@inheritDoc} */
        @Override
        public void bindView(View view, Context context, Cursor cursor) {
        	int cursorInfo = net.peterkuterna.android.apps.devoxxsched.ui.SessionsActivity.SessionsCursorWrapper.CursorInfo.SESSION_TYPE;
        	if (!mNoWeekdayHeader) {
        		cursorInfo = cursor.getInt(SessionsCursorWrapper.CURSOR_INFO_COLUMN_INDEX);
        	}
        	switch (cursorInfo) {
        		case net.peterkuterna.android.apps.devoxxsched.ui.SessionsActivity.SessionsCursorWrapper.CursorInfo.DAY_TYPE:
                    final TextView headerView = (TextView) view.findViewById(R.id.session_header);
                    
                    headerView.setText(cursor.getString(SessionsCursorWrapper.WEEKDAY_COLUMN_INDEX));
        			break;
        		case net.peterkuterna.android.apps.devoxxsched.ui.SessionsActivity.SessionsCursorWrapper.CursorInfo.SESSION_TYPE:
        		default:
                    final TextView titleView = (TextView) view.findViewById(R.id.session_title);
                    final TextView subtitleView = (TextView) view.findViewById(R.id.session_subtitle);
                    final CheckBox starButton = (CheckBox) view.findViewById(R.id.star_button);
                    if (mTrackColor == -1) {
                    	view.findViewById(R.id.session_track).setBackgroundColor(cursor.getInt(SessionsQuery.TRACK_COLOR));
                    } else {
                    	view.findViewById(R.id.session_track).setVisibility(View.GONE);
                    }

                    titleView.setText(cursor.getString(SessionsQuery.TITLE));

                    // Format time block this session occupies
                    final long blockStart = cursor.getLong(SessionsQuery.BLOCK_START);
                    final long blockEnd = cursor.getLong(SessionsQuery.BLOCK_END);
                    final String roomName = cursor.getString(SessionsQuery.ROOM_NAME);
                    final String subtitle = formatSessionSubtitle(blockStart, blockEnd, roomName, context);

                    subtitleView.setText(subtitle);

                    final boolean starred = cursor.getInt(SessionsQuery.STARRED) != 0;
                    starButton.setVisibility(starred ? View.VISIBLE : View.INVISIBLE);
                    starButton.setChecked(starred);
                    
                    if (mHighlightParallelStarred) {
                        final int parallelStarredCount = cursor.getInt(SessionsQuery.STARRED_IN_BLOCK_COUNT);
                    	if (starred && parallelStarredCount > 1) {
                        	view.setBackgroundColor(0x20ff0000);
                    	} else {
                        	view.setBackgroundColor(0x00000000);
                    	}
                    } else {
                    	view.setBackgroundColor(0x00000000);
                    }
                    
                    // Possibly indicate that the session has occurred in the past.
                    UIUtils.setSessionTitleColor(blockStart, blockEnd, titleView, subtitleView);
                    break;
        	}
        }

		@Override
		public int getItemViewType(int position) {
			if (!mNoWeekdayHeader) {
				SessionsCursorWrapper wrapper = (SessionsCursorWrapper) getCursor();
				return wrapper.getItemViewType(position);
			} else {
				return super.getItemViewType(position);
			}
		}

		@Override
		public boolean areAllItemsEnabled() {
			if (!mNoWeekdayHeader) {
				return false;
			} else {
				return super.areAllItemsEnabled();
			}
		}

		@Override
		public int getViewTypeCount() {
			if (!mNoWeekdayHeader) {
				return 2;
			} else {
				return super.getViewTypeCount();
			}
		}

		@Override
		public boolean isEnabled(int position) {
			if (!mNoWeekdayHeader) {
				SessionsCursorWrapper wrapper = (SessionsCursorWrapper) getCursor();
				return wrapper.isEnabled(position);
			} else {
				return super.isEnabled(position);
			}
		}

    }
    
    /**
     * {@link CursorWrapper} to insert weekday headers for a {@link SessionsQuery}.
     */
    private class SessionsCursorWrapper extends CursorWrapper {
    	
    	private final Context context;
    	private Cursor cursor;
    	private ArrayList<CursorInfo> cursorMapping;
    	private int position = -1;
    	
    	public static final int WEEKDAY_COLUMN_INDEX = 98;
    	public static final int CURSOR_INFO_COLUMN_INDEX = 99;
    	
		public SessionsCursorWrapper(Cursor cursor, Context context) {
			super(cursor);
			
			this.context = context;
			this.cursor = cursor;
			
			registerDataSetObserver(new DataSetObserver() {

				@Override
				public void onChanged() {
					init();
				}

				@Override
				public void onInvalidated() {
					init();
				}
			});
			
			init();
		}

		@Override
		public int getCount() {
			return cursorMapping.size();
		}
		
		public boolean isEnabled(int position) {
			return cursorMapping.get(position).getType() == CursorInfo.SESSION_TYPE;
		}

		public int getItemViewType(int position) {
			return cursorMapping.get(position).getType() - 1;
		}

		@Override
		public int getInt(int columnIndex) {
			CursorInfo cursorInfo = cursorMapping.get(position);
			if (columnIndex == CURSOR_INFO_COLUMN_INDEX) {
				return cursorInfo.getType();
			} else if (cursorInfo.getType() == CursorInfo.SESSION_TYPE) {
				return super.getInt(columnIndex);
			}
			return 0;
		}

		@Override
		public long getLong(int columnIndex) {
			CursorInfo cursorInfo = cursorMapping.get(position);
			if (cursorInfo.getType() == CursorInfo.SESSION_TYPE) {
				return super.getLong(columnIndex);
			}
			return 0;
		}

		@Override
		public String getString(int columnIndex) {
			CursorInfo cursorInfo = cursorMapping.get(position);
			if (columnIndex == WEEKDAY_COLUMN_INDEX) {
				return cursorInfo.getWeekday();
			} else if (cursorInfo.getType() == CursorInfo.SESSION_TYPE) {
				return super.getString(columnIndex);
			}
			return null;
		}

		@Override
		public boolean moveToPosition(int position) {
			if (position < cursorMapping.size()) {
				this.position = position;

				final CursorInfo cursorInfo = cursorMapping.get(position);
				switch (cursorInfo.getType()) {
					case CursorInfo.SESSION_TYPE:
						return super.moveToPosition(cursorInfo.getCursorPostion());
					default:
						return true;
				}
			}
			return false;
		}
    	
		private void init() {
			if (cursor != null) {
				cursorMapping = Lists.newArrayList();
				if (cursor.moveToFirst()) {
					String prevWeekday = null;
					do {
						long blockStart = cursor.getLong(SessionsQuery.BLOCK_START);
						String weekday = UIUtils.formatWeekdayHeader(blockStart, context);
						if (!weekday.equals(prevWeekday)) {
							final CursorInfo cursorInfo = new CursorInfo(CursorInfo.DAY_TYPE, -1, weekday);
							cursorMapping.add(cursorInfo);
						}
						final CursorInfo cursorInfo = new CursorInfo(CursorInfo.SESSION_TYPE, cursor.getPosition(), null);
						cursorMapping.add(cursorInfo);
						prevWeekday = weekday;
					} while (cursor.moveToNext());
				}
			}
		}
		
		private class CursorInfo {
			
			public static final int SESSION_TYPE = 0x01;
			public static final int DAY_TYPE = 0x02;
			
			private final int type;
			private final int cursorPostion;
			private final String weekday;
			
			public CursorInfo(int type, int cursorPostion, String weekday) {
				this.type = type;
				this.cursorPostion = cursorPostion;
				this.weekday = weekday;
			}

			public int getType() {
				return type;
			}

			public int getCursorPostion() {
				return cursorPostion;
			}

			public String getWeekday() {
				return weekday;
			}
			
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
			return getLayoutInflater().inflate(R.layout.list_item_session, parent,false);
        }

        /** {@inheritDoc} */
        @Override
        public void bindView(View view, Context context, Cursor cursor) {
        	view.findViewById(R.id.session_track).setBackgroundColor(cursor.getInt(SearchQuery.TRACK_COLOR));

        	((TextView) view.findViewById(R.id.session_title)).setText(cursor.getString(SearchQuery.TITLE));

            final String snippet = cursor.getString(SearchQuery.SEARCH_SNIPPET);
            final Spannable styledSnippet = buildStyledSnippet(snippet);
            ((TextView) view.findViewById(R.id.session_subtitle)).setText(styledSnippet);

            final boolean starred = cursor.getInt(SearchQuery.STARRED) != 0;
            final CheckBox starButton = (CheckBox) view.findViewById(R.id.star_button);
            starButton.setVisibility(starred ? View.VISIBLE : View.INVISIBLE);
            starButton.setChecked(starred);
        }
    }

    private Runnable mRefreshSessionsRunnable = new Runnable() {
        public void run() {
            if (mAdapter != null) {
                // This is used to refresh session title colors.
                mAdapter.notifyDataSetChanged();
            }

            // Check again on the next quarter hour, with some padding to account for network
            // time differences.
            long nextQuarterHour = (SystemClock.uptimeMillis() / 900000 + 1) * 900000 + 5000;
            mMessageQueueHandler.postAtTime(mRefreshSessionsRunnable, nextQuarterHour);
        }
    };

    /** {@link Sessions} query parameters. */
    private interface SessionsQuery {
    	String[] PROJECTION = {
                BaseColumns._ID,
                Sessions.SESSION_ID,
                Sessions.TITLE,
                Sessions.STARRED,
                Blocks.BLOCK_START,
                Blocks.BLOCK_END,
                Rooms.NAME,
                Tracks.TRACK_COLOR,
                Sessions.STARRED_IN_BLOCK_COUNT,
        };

        int _ID = 0;
        int SESSION_ID = 1;
        int TITLE = 2;
        int STARRED = 3;
        int BLOCK_START = 4;
        int BLOCK_END = 5;
        int ROOM_NAME = 6;
        int TRACK_COLOR = 7;
        int STARRED_IN_BLOCK_COUNT = 8;
    }

	/** {@link Sessions} search query parameters. */
    private interface SearchQuery {
    	String[] PROJECTION = {
                BaseColumns._ID,
                Sessions.SESSION_ID,
                Sessions.TITLE,
                Sessions.SEARCH_SNIPPET,
                Sessions.STARRED,
                Tracks.TRACK_COLOR,
        };

        int _ID = 0;
        int SESSION_ID = 1;
        int TITLE = 2;
        int SEARCH_SNIPPET = 3;
        int STARRED = 4;
        int TRACK_COLOR = 5;
     
    }
    
//    /**
//     * Reflection class to use the smooth scrolling when available (Android2.2+)
//     */
//    public static class Reflect {
//
//		private static Method mListView_smoothScrollToPosition;
//
//		static {
//			initCompatibility();
//		};
//
//		private static void initCompatibility() {
//			try {
//				mListView_smoothScrollToPosition = AbsListView.class
//						.getMethod("smoothScrollToPosition",
//								new Class[] { int.class, int.class });
//			} catch (NoSuchMethodException nsme) {
//			}
//		}
//
//		private static void smoothScrollToPosition(AbsListView listView, int position, int boundPosition) {
//			try {
//				mListView_smoothScrollToPosition.invoke(listView, position, boundPosition);
//			} catch (InvocationTargetException ite) {
//				System.err.println("unexpected " + ite);
//			} catch (IllegalAccessException ie) {
//				System.err.println("unexpected " + ie);
//			}
//		}
//
//		public void scrollTo(ListView listView, int position, int boundPosition) {
//			if (mListView_smoothScrollToPosition != null) {
//				smoothScrollToPosition(listView, position, boundPosition);
//			} else {
//				listView.setSelection(position);
//			}
//		}
//	}
    
}
