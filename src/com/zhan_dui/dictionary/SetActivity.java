package com.zhan_dui.dictionary;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;

import com.mobeta.android.dslv.DragSortController;
import com.mobeta.android.dslv.DragSortListView;
import com.mobeta.android.dslv.DragSortListView.DropListener;
import com.mobeta.android.dslv.SimpleDragSortCursorAdapter;
import com.zhan_dui.dictionary.db.DictionaryDB;

public class SetActivity extends Activity {
	private Context context;
	private DragSortListView dragSortListView;
	private SimpleDragSortCursorAdapter simpleDragSortCursorAdapter;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.drag_and_drop);
		dragSortListView = (DragSortListView) findViewById(R.id.drag_sort_list);
		context = this;

		DictionaryDB dictionaryDB = new DictionaryDB(context,
				DictionaryDB.DB_NAME, null, DictionaryDB.DB_VERSION);
		SQLiteDatabase sqLiteDatabase = dictionaryDB.getWritableDatabase();
		Cursor cursor = sqLiteDatabase
				.rawQuery(
						"select * from dictionary_list order by dictionary_order",
						null);
		String[] from = {"dictionary_name"};
		int[] to = {R.id.dictionary_name};
		simpleDragSortCursorAdapter = new SimpleDragSortCursorAdapter(context,
				R.layout.drag_and_drop_item, cursor, from, to, 0);

		DragSortController dragSortController = new DragSortController(
				dragSortListView);
		dragSortController.setBackgroundColor(getResources().getColor(
				R.color.floatviewcolor));
		dragSortController.setDragHandleId(R.id.drag_image);

		dragSortListView.setFloatViewManager(dragSortController);
		dragSortListView.setAdapter(simpleDragSortCursorAdapter);
		dragSortListView.setDropListener(dropListener);
		dragSortListView.setOnTouchListener(dragSortController);

		sqLiteDatabase.close();
	}
	private DropListener dropListener = new DropListener() {

		@Override
		public void drop(int from, int to) {


			DictionaryDB dictionaryDB = new DictionaryDB(context,
					DictionaryDB.DB_NAME, null, DictionaryDB.DB_VERSION);
			SQLiteDatabase sqLiteDatabase = dictionaryDB.getWritableDatabase();
			sqLiteDatabase
					.execSQL("update dictionary_list set dictionary_order='10000000' where dictionary_order='"
							+ from + "'");
			if (from > to) {
				sqLiteDatabase
						.execSQL("update dictionary_list set dictionary_order=dictionary_order+1 where dictionary_order>='"
								+ to + "' and dictionary_order<'" + from + "'");
			} else if (from < to) {
				sqLiteDatabase
						.execSQL("update dictionary_list set dictionary_order=dictionary_order-1 where dictionary_order>'"
								+ from + "' and dictionary_order<='" + to + "'");
			}
			// sqLiteDatabase
			// .execSQL("update dictionary_list set dictionary_order='0' where dictionary_name='¶ÌÓï×Öµä'");
			sqLiteDatabase
					.execSQL("update dictionary_list set dictionary_order='"
							+ to + "' where dictionary_order='10000000'");

			Cursor cursor2 = sqLiteDatabase.rawQuery(
					"select * from dictionary_list order by dictionary_order",
					null);
			String[] from2 = {"dictionary_name"};
			int[] to2 = {R.id.dictionary_name};
			simpleDragSortCursorAdapter = new SimpleDragSortCursorAdapter(
					context, R.layout.drag_and_drop_item, cursor2, from2, to2,
					0);
			dragSortListView.setAdapter(simpleDragSortCursorAdapter);
			sqLiteDatabase.close();
		}
	};

}
