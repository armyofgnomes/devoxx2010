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

package net.peterkuterna.android.apps.devoxxsched.ui.widget;

import android.content.Context;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.PorterDuff.Mode;
import android.graphics.drawable.LayerDrawable;
import android.util.AttributeSet;
import android.widget.ImageView;

/**
 * Extension of {@link ImageView} that applies a {@link ColorFilter} and alpha
 * level to a certain layer of {@link LayerDrawable}.
 */
public class FloorView extends ImageView {

	private final int accentColor = Color.parseColor("#df1831");
	private LayerDrawable layerDrawable;
	
	public FloorView(Context context) {
		this(context, null);
	}

	public FloorView(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public FloorView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
			
		layerDrawable = (LayerDrawable) getDrawable();
		if (layerDrawable != null) {
			for (int i = 1; i < layerDrawable.getNumberOfLayers(); i++) {
				layerDrawable.getDrawable(i).setAlpha(0);
			}

		}
	}
	
	public void highlightRoom(int visibleRoom) {
		if (layerDrawable != null) {
			if (visibleRoom > 0 && visibleRoom < layerDrawable.getNumberOfLayers()) {
				layerDrawable.getDrawable(visibleRoom).setColorFilter(accentColor, Mode.SRC_ATOP);
				layerDrawable.getDrawable(visibleRoom).setAlpha(180);
			}
		}
	}

}
