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

package net.peterkuterna.android.apps.devoxxsched;

/**
 * Class with constants.
 */
public class Constants {

	public static final String REST_BASE_URL = "http://cfp.devoxx.com/rest/v1/";
	public static final String PRESENTATIONS_URL = REST_BASE_URL + "events/1/presentations";
	public static final String SPEAKERS_URL = REST_BASE_URL + "events/1/speakers";
	public static final String ROOMS_URL = REST_BASE_URL + "events/1/schedule/rooms";
	public static final String SCHEDULE_URL = REST_BASE_URL + "events/1/schedule";
	public static final String MYSCHEDULE_ACTIVATE_URL = REST_BASE_URL + "events/users/activate";
	public static final String MYSCHEDULE_PUBLISH_URL = REST_BASE_URL + "events/users/publish";
	public static final String MYSCHEDULE_EMAIL_URL = REST_BASE_URL + "events/users/email";

}
