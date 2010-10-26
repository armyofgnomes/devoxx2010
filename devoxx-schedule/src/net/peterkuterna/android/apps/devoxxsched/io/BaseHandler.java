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

public abstract class BaseHandler {

	public static final int LOCAL_SYNC = 0;
	public static final int LOCAL_LAB_SYNC = 1;
	public static final int REMOTE_SYNC = 2;
	
    private final String mAuthority;
	private final int mSyncType;
	
	public BaseHandler(String authority, int syncType) {
		this.mAuthority = authority;
		this.mSyncType = syncType;
	}

	public String getAuthority() {
		return mAuthority;
	}

	public boolean isLocalSync() {
		return mSyncType == LOCAL_SYNC;
	}
	
	public boolean isLocalLabSync() {
		return mSyncType == LOCAL_LAB_SYNC;
	}
	
	public boolean isRemoteSync() {
		return mSyncType == REMOTE_SYNC;
	}
}
