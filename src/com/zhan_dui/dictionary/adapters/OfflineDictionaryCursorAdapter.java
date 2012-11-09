package com.zhan_dui.dictionary.adapters;

import android.content.Context;
import android.database.Cursor;
import android.support.v4.widget.CursorAdapter;
import android.view.View;
import android.view.ViewGroup;

public class OfflineDictionaryCursorAdapter extends CursorAdapter {

	public OfflineDictionaryCursorAdapter(Context context, Cursor c,
			boolean autoRequery) {
		super(context, c, autoRequery);
	}

	@Override
	public void bindView(View convertView, Context context, Cursor cursor) {

	}

	@Override
	public View newView(Context context, Cursor cursor, ViewGroup root) {
		
		return null;
	}

}
