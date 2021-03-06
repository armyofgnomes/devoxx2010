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

package net.peterkuterna.android.apps.devoxxsched.util;

import net.peterkuterna.android.apps.devoxxsched.R;
import net.peterkuterna.android.apps.devoxxsched.provider.ScheduleContract.Sessions;
import net.peterkuterna.android.apps.devoxxsched.ui.SettingsActivity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;

/**
 * Handles notification of new and changed (@link Sessions}.
 */
public class NotificationUtils {
	
	public static final int NOTIFICATION_NEW_SESSIONS = 1;
	public static final int NOTIFICATION_CHANGED_SESSIONS = 2;
	
	public static void cancelNotifications(Context context) {
		final NotificationManager mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
		mNotificationManager.cancel(NOTIFICATION_NEW_SESSIONS);
		mNotificationManager.cancel(NOTIFICATION_CHANGED_SESSIONS);
	}
	
	public static void notifyNewSessions(Context context, ContentResolver resolver) {
		final SharedPreferences prefs = context.getSharedPreferences(SettingsActivity.SETTINGS_NAME, Context.MODE_PRIVATE);
		final boolean notifyNewSession = prefs.getBoolean(context.getString(R.string.notify_new_sessions_key), true);
		
		if (notifyNewSession) {
			final NotificationManager mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
			final int newSessions = getNewSessionCount(resolver);
			
			if (newSessions > 0) {
				final int icon = R.drawable.stat_devoxx;
				final long when = System.currentTimeMillis();
				Notification notification = new Notification(icon, "New Devoxx session", when);
		        final Intent notificationIntent = new Intent(Intent.ACTION_VIEW, Sessions.CONTENT_NEW_URI);
		        notificationIntent.putExtra(Intent.EXTRA_TITLE, context.getString(R.string.title_new_sessions));
				PendingIntent contentIntent = PendingIntent.getActivity(context, 0, notificationIntent, 0);
				notification.setLatestEventInfo(context, "New Devoxx session", newSessions + " in total.", contentIntent);
				notification.flags |= Notification.FLAG_AUTO_CANCEL;
				mNotificationManager.notify(NOTIFICATION_NEW_SESSIONS, notification);
			}
		}
	}
	
	public static void notifyChangedStarredSessions(Context context, ContentResolver resolver) {
		final SharedPreferences prefs = context.getSharedPreferences(SettingsActivity.SETTINGS_NAME, Context.MODE_PRIVATE);
		final boolean notifyChangedStarredSession = prefs.getBoolean(context.getString(R.string.notify_changed_starred_sessions_key), true);
		
		if (notifyChangedStarredSession) {
			final NotificationManager mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
			final int changedStarredSessions = getChangedStarredSessionCount(resolver);

			if (changedStarredSessions > 0) {
				final int icon = R.drawable.stat_devoxx;
				final long when = System.currentTimeMillis();
				Notification notification = new Notification(icon, "Changed Devoxx session", when);
		        final Intent notificationIntent = new Intent(Intent.ACTION_VIEW, Sessions.CONTENT_UPDATED_STARRED_URI);
		        notificationIntent.putExtra(Intent.EXTRA_TITLE, context.getString(R.string.title_changed_starred_sessions));
				PendingIntent contentIntent = PendingIntent.getActivity(context, 0, notificationIntent, 0);
				notification.setLatestEventInfo(context, "Changed Devoxx session", changedStarredSessions + " in total.", contentIntent);
				notification.flags |= Notification.FLAG_AUTO_CANCEL;
				mNotificationManager.notify(NOTIFICATION_CHANGED_SESSIONS, notification);
			}
		}
	}
	
	private static int getNewSessionCount(ContentResolver resolver) {
		return getCount(resolver, Sessions.CONTENT_NEW_URI, SessionsQuery.PROJECTION);
	}
	
	private static int getChangedStarredSessionCount(ContentResolver resolver) {
		return getCount(resolver, Sessions.CONTENT_UPDATED_STARRED_URI, SessionsQuery.PROJECTION);
	}
	
	private static int getCount(ContentResolver resolver, Uri uri, String [] projection) {
		final Cursor cursor = resolver.query(uri, projection, null, null, null);
		try {
			if (!cursor.moveToFirst()) return 0;
			return cursor.getCount(); 
		} finally {
			cursor.close();
		}
	}
	
	/** {@link Sessions} query parameters */
	private interface SessionsQuery {
		String [] PROJECTION = {
			Sessions.SESSION_ID,
		};
		
		int SESSION_ID = 0;
	}
	
}
