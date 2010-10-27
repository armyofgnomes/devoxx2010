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
 * Modified by Peter Kuterna to also support local cache files in JSON format.
 */

package net.peterkuterna.android.apps.devoxxsched.io;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import net.peterkuterna.android.apps.devoxxsched.io.JSONHandler.JSONHandlerException;
import net.peterkuterna.android.apps.devoxxsched.io.XmlHandler.XmlHandlerException;
import net.peterkuterna.android.apps.devoxxsched.util.Lists;
import net.peterkuterna.android.apps.devoxxsched.util.ParserUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;


public class LocalExecutor {
    private Resources mRes;
    private ContentResolver mResolver;

    public LocalExecutor(Resources res, ContentResolver resolver) {
        mRes = res;
        mResolver = resolver;
    }

    public void execute(int resId, XmlHandler handler) throws XmlHandlerException {
        final XmlResourceParser parser = mRes.getXml(resId);
        try {
        	handler.setLocalSync(true);
            handler.parseAndApply(parser, mResolver);
        } finally {
            parser.close();
        }
    }
    
    public void execute(Context context, String assetName, XmlHandler handler) throws XmlHandlerException {
		try {
		    final InputStream input = context.getAssets().open(assetName);
		    final XmlPullParser parser = ParserUtils.newPullParser(input);
        	handler.setLocalSync(true);
		    handler.parseAndApply(parser, mResolver);
		} catch (XmlHandlerException e) {
		    throw e;
		} catch (XmlPullParserException e) {
		    throw new XmlHandlerException("Problem parsing local asset: " + assetName, e);
		} catch (IOException e) {
		    throw new XmlHandlerException("Problem parsing local asset: " + assetName, e);
		}
    }

    public void execute(Context context, String assetName, JSONHandler handler)
            throws JSONHandlerException {
        try {
            final InputStream input = context.getAssets().open(assetName);
            byte [] buffer = new byte[input.available()];
            while (input.read(buffer) != -1);
            String jsontext = new String(buffer);
            ArrayList<JSONArray> entries = Lists.newArrayList();
            entries.add(new JSONArray(jsontext));
        	handler.setLocalSync(true);
            handler.parseAndApply(entries, mResolver);
        } catch (JSONHandlerException e) {
            throw e;
        } catch (JSONException e) {
            throw new JSONHandlerException("Problem parsing local asset: " + assetName, e);
        } catch (IOException e) {
            throw new JSONHandlerException("Problem parsing local asset: " + assetName, e);
        }
    }

}
