package com.example.viewpagertest;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.widget.ListView;
import android.widget.Toast;

public class OnlineDictionaryActivity extends Activity {

	private OnlineInfoHandler onlineInfoHandler;
	private ProgressDialog progressDialog;
	private ListView dictionaryList;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		onlineInfoHandler = new OnlineInfoHandler();
		setContentView(R.layout.online_dictionary);

		SharedPreferences sharedPreferences = this.getSharedPreferences(
				Config.PREFER_NAME, Context.MODE_PRIVATE);
		int last_time = sharedPreferences.getInt(
				"last_refresh_online_dictionary", 0);
		int cur_time = (int) (System.currentTimeMillis());

		dictionaryList = (ListView) findViewById(R.id.list_online_dictionary);
		dictionaryList.setSelector(R.drawable.item_dictionary_selector);

		new GetOnlineListThread().start();

	}

	/**
	 * 在线词典获取时候的Handler
	 * 
	 * @author xuanqinanhai
	 * 
	 */
	class OnlineInfoHandler extends Handler {

		@Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
			switch (msg.what) {
			case Constants.DOWNLOAD_ERROR:
				if (progressDialog != null) {
					Toast.makeText(getApplicationContext(),
							getString(R.string.progress_error),
							Toast.LENGTH_LONG).show();
					SQLiteDatabase sqLiteDatabase = new DictionaryDB(
							OnlineDictionaryActivity.this,
							DictionaryDB.DB_NAME, null, DictionaryDB.DB_VERSION)
							.getReadableDatabase();
					Cursor cursor = sqLiteDatabase.rawQuery(
							"select * from dictionary_list", null);
					startManagingCursor(cursor);
					try {
						OnlineListCursorAdapter onlineListCursorAdapter;
						onlineListCursorAdapter = new OnlineListCursorAdapter(
								OnlineDictionaryActivity.this, cursor);
						dictionaryList.setAdapter(onlineListCursorAdapter);
					} catch (JSONException e) {
					}

					progressDialog.dismiss();
				}
				break;
			case Constants.DOWNLOAD_FINISH:
				if (progressDialog != null) {
					progressDialog.dismiss();
				}
				break;
			case Constants.DOWNLOAD_SUCCESS:
				if (progressDialog != null) {
					Toast.makeText(OnlineDictionaryActivity.this,
							getString(R.string.progress_success),
							Toast.LENGTH_LONG).show();

					try {
						OnlineListCursorAdapter onlineListCursorAdapter = new OnlineListCursorAdapter(
								OnlineDictionaryActivity.this, (Cursor) msg.obj);
						dictionaryList.setAdapter(onlineListCursorAdapter);
					} catch (JSONException e) {
						e.printStackTrace();
						Toast.makeText(OnlineDictionaryActivity.this,
								getString(R.string.json_parse_error),
								Toast.LENGTH_LONG).show();
						finish();
					} finally {
						progressDialog.dismiss();
					}
				}
				break;
			case Constants.DOWNLOADING:
				progressDialog = ProgressDialog.show(
						OnlineDictionaryActivity.this,
						getString(R.string.progress_downloading),
						getString(R.string.progress_wait));
				break;
			default:
				break;
			}
		}
	}

	/**
	 * 获取在线词典列表线程
	 * 
	 * @author xuanqinanhai
	 * 
	 */
	class GetOnlineListThread extends Thread {

		/**
		 * 将获取的字典字符串数据转换成json，并且存储
		 * 
		 * @param json
		 * @throws JSONException
		 */
		public void ParseJson(String json) throws JSONException {
			JSONArray jsonArray = new JSONArray(json);
			String name, size, url, save_name, xml_url;
			JSONObject jsonObject;
			DictionaryDB dictionaryDB = new DictionaryDB(
					OnlineDictionaryActivity.this, DictionaryDB.DB_NAME, null,
					DictionaryDB.DB_VERSION);
			for (int i = 0; i < jsonArray.length(); i++) {
				jsonObject = jsonArray.getJSONObject(i);
				name = jsonObject.getString("dictionary_name");
				size = jsonObject.getString("dictionary_size");
				url = jsonObject.getString("dictionary_url");
				save_name = jsonObject.getString("dictionary_save_name");
				xml_url = jsonObject.getString("dictionary_xml");
				String checkIfExsit = "select * from dictionary_list where `dictionary_name`='"
						+ name + "'";
				SQLiteDatabase sqLiteDatabase = dictionaryDB
						.getWritableDatabase();
				Cursor cursor = sqLiteDatabase.rawQuery(checkIfExsit, null);
				if (cursor.getCount() == 0) {

					ContentValues contentValues = new ContentValues();
					contentValues.put("dictionary_name", name);
					contentValues.put("dictionary_size", size);
					contentValues.put("dictionary_url", url);
					contentValues.put("dictionary_xml", xml_url);
					contentValues.put("dictionary_save_name", save_name);

					sqLiteDatabase.insert("dictionary_list", null,
							contentValues);
				}
				sqLiteDatabase.close();

				SharedPreferences sharedPreferences = OnlineDictionaryActivity.this
						.getSharedPreferences(Config.PREFER_NAME,
								Context.MODE_PRIVATE);
				SharedPreferences.Editor editor = sharedPreferences.edit();
				editor.putInt("last_refresh_online_dictionary",
						(int) System.currentTimeMillis());
				editor.commit();

				OnlineListCursorAdapter onlineListCursorAdapter;
				if (sqLiteDatabase != null && sqLiteDatabase.isOpen()) {
					sqLiteDatabase.close();
				}
				sqLiteDatabase = null;
				try {
					sqLiteDatabase = new DictionaryDB(
							OnlineDictionaryActivity.this,
							DictionaryDB.DB_NAME, null, DictionaryDB.DB_VERSION)
							.getReadableDatabase();
					cursor = sqLiteDatabase.rawQuery(
							"select * from dictionary_list", null);
					startManagingCursor(cursor);
					onlineListCursorAdapter = new OnlineListCursorAdapter(
							OnlineDictionaryActivity.this, cursor);
					dictionaryList.setAdapter(onlineListCursorAdapter);
				} catch (JSONException e) {
					e.printStackTrace();
				} finally {
					if (sqLiteDatabase != null && sqLiteDatabase.isOpen()) {
						sqLiteDatabase.close();
					}
				}

			}

		}

		@Override
		public void run() {
			super.run();
			Message msg = new Message();
			msg.what = Constants.DOWNLOADING;
			msg.setTarget(onlineInfoHandler);
			msg.sendToTarget();
			SQLiteDatabase sqLiteDatabase = null;
			Cursor cursor;
			try {
				URL url = new URL(Constants.ONLINE_DICTIONARY_LIST_URL);
				URLConnection urlConnection = url.openConnection();
				urlConnection.setConnectTimeout(30000);
				BufferedReader bufferedReader = new BufferedReader(
						new InputStreamReader(urlConnection.getInputStream()));
				StringBuilder jsonResultBuilder = new StringBuilder();
				String line = "";
				while ((line = bufferedReader.readLine()) != null) {
					jsonResultBuilder.append(line);
				}
				Message msg_success = new Message();
				ParseJson(jsonResultBuilder.toString());

				DictionaryDB dictionaryDB = new DictionaryDB(
						OnlineDictionaryActivity.this, DictionaryDB.DB_NAME,
						null, DictionaryDB.DB_VERSION);
				sqLiteDatabase = dictionaryDB.getReadableDatabase();
				cursor = sqLiteDatabase.rawQuery(
						"select * from dictionary_list", null);
				OnlineDictionaryActivity.this.startManagingCursor(cursor);
				msg_success.setTarget(onlineInfoHandler);
				msg_success.what = Constants.DOWNLOAD_SUCCESS;
				msg_success.obj = cursor;
				msg_success.sendToTarget();
			} catch (Exception e) {
				e.printStackTrace();
				Message msg_error = new Message();
				msg_error.setTarget(onlineInfoHandler);
				msg_error.what = Constants.DOWNLOAD_ERROR;
				msg_error.sendToTarget();
			} finally {
				Message msg_finish = new Message();
				msg_finish.setTarget(onlineInfoHandler);
				msg_finish.what = Constants.DOWNLOAD_FINISH;
				msg_finish.sendToTarget();
				if (sqLiteDatabase != null) {
					sqLiteDatabase.close();
				}
			}

		}
	}

}
