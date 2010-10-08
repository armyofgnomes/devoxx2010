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
 */
package net.peterkuterna.android.apps.devoxxsched.util;

import java.io.InputStream;
import java.util.regex.Pattern;

import net.peterkuterna.android.apps.devoxxsched.io.JSONHandler;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import android.content.ContentProvider;
import android.net.Uri;
import android.text.format.Time;


/**
 * Various utility methods used by {@link JSONHandler} implementations.
 */
public class ParserUtils {

    public static final String BLOCK_TYPE_REGISTRATION = "Registration";
    public static final String BLOCK_TYPE_BREAK = "Break";
    public static final String BLOCK_TYPE_COFFEE_BREAK = "Coffee Break";
    public static final String BLOCK_TYPE_LUNCH = "Lunch";
    public static final String BLOCK_TYPE_BREAKFAST = "Breakfast";
    public static final String BLOCK_TYPE_KEYNOTE = "Keynote";
    public static final String BLOCK_TYPE_TALK = "Talk";

    /** Used to sanitize a string to be {@link Uri} safe. */
    private static final Pattern sSanitizePattern = Pattern.compile("[^a-z0-9-_]");
    private static final Pattern sParenPattern = Pattern.compile("\\(.*?\\)");

    private static Time sTime = new Time();
    private static XmlPullParserFactory sFactory;

    /**
     * Sanitize the given string to be {@link Uri} safe for building
     * {@link ContentProvider} paths.
     */
    public static String sanitizeId(String input) {
        return sanitizeId(input, false);
    }

    /**
     * Sanitize the given string to be {@link Uri} safe for building
     * {@link ContentProvider} paths.
     */
    public static String sanitizeId(String input, boolean stripParen) {
        if (input == null) return null;
        if (stripParen) {
            // Strip out all parenthetical statements when requested.
            input = sParenPattern.matcher(input).replaceAll("");
        }
        return sSanitizePattern.matcher(input.toLowerCase()).replaceAll("");
    }

    /**
     * Build and return a new {@link XmlPullParser} with the given
     * {@link InputStream} assigned to it.
     */
    public static XmlPullParser newPullParser(InputStream input) throws XmlPullParserException {
        if (sFactory == null) {
            sFactory = XmlPullParserFactory.newInstance();
        }
        final XmlPullParser parser = sFactory.newPullParser();
        parser.setInput(input, null);
        return parser;
    }

    /**
     * Parse the given string as a RFC 3339 timestamp, returning the value as
     * milliseconds since the epoch.
     */
    public static long parseTime(String time) {
        sTime.parse3339(time);
        return sTime.toMillis(false);
    }

    public static long parseDevoxxTime(String time) {
        parseTime(time.replace(' ', 'T') + "00+01:00");
        return sTime.toMillis(false);
    }

}
