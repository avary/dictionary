package com.zhan_dui.dictionary.adapters;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Random;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CursorAdapter;
import android.widget.TextView;

import com.zhan_dui.dictionary.OnlineDictionaryActivity;
import com.zhan_dui.dictionary.R;
import com.zhan_dui.dictionary.db.DictionaryDB;
import com.zhan_dui.dictionary.handlers.DownloadNotificationHandler;
import com.zhan_dui.dictionary.runnables.DownloadRunnable;
import com.zhan_dui.dictionary.runnables.DownloadRunnable.DownloadInformation;
import com.zhan_dui.dictionary.utils.Constants;

public class OnlineListCursorAdapter extends CursorAdapter {

	LayoutInflater layoutInflater;
	Context context;
	ArrayList<String> dictionarysInfos = new ArrayList<String>();
	private static Hashtable<String, DownloadRunnable> downloadingRunnable = new Hashtable<String, DownloadRunnable>();

	public static ArrayList<String> dictionarysDownloading = new ArrayList<String>();

	public OnlineListCursorAdapter(Context context, Cursor c) {
		super(context, c);
		this.context = context;
		if (c == null) {
			DictionaryDB dictionaryDB = new DictionaryDB(context,
					DictionaryDB.DB_NAME, null, DictionaryDB.DB_VERSION);
			SQLiteDatabase sqLiteDatabase = dictionaryDB.getReadableDatabase();
			c = sqLiteDatabase.rawQuery("select * from dictionary_list", null);
			sqLiteDatabase.close();
		}
		layoutInflater = LayoutInflater.from(context);

	}

	@Override
	public void bindView(View convertView, Context context, Cursor cursor) {
		ViewHolder viewHolder = new ViewHolder();
		Log.i("bind", "bind");

		viewHolder.DictionaryName = (TextView) convertView
				.findViewById(R.id.item_dictionary_name);
		viewHolder.DictionarySize = (TextView) convertView
				.findViewById(R.id.item_dictionary_size);
		viewHolder.DictionaryDownloadButton = (Button) convertView
				.findViewById(R.id.item_download);

		String dictionary_name = cursor.getString(cursor
				.getColumnIndex("dictionary_name"));
		String dictionary_size = cursor.getString(cursor
				.getColumnIndex("dictionary_size"));
		String dictionary_xml = cursor.getString(cursor
				.getColumnIndex("dictionary_xml"));
		String dictionary_save_name = cursor.getString(cursor
				.getColumnIndex("dictionary_save_name"));
		String dictionary_url = cursor.getString(cursor
				.getColumnIndex("dictionary_url"));
		String dictionary_downloaded = cursor.getString(cursor
				.getColumnIndex("dictionary_downloaded"));
		String dictionary_xml_downloaded = cursor.getString(cursor
				.getColumnIndex("dictionary_xml_downloaded"));
		viewHolder.DictionaryName.setText(dictionary_name);// 字典的名字
		viewHolder.DictionarySize.setText(dictionary_size);// 大小

		viewHolder.DictionaryDownloadButton
				.setOnClickListener(new DownloadDictionaryListener(
						dictionary_xml, dictionary_url, dictionary_save_name,
						dictionary_name, dictionary_size, context));

		if (dictionary_downloaded.equals("1")
				&& dictionary_xml_downloaded.equals("1")) {
			viewHolder.DictionaryDownloadButton.setText(context
					.getString(R.string.download_finished));
			viewHolder.DictionaryDownloadButton.setEnabled(false);
		} else if (dictionarysDownloading.contains(dictionary_name)) {
			viewHolder.DictionaryDownloadButton.setEnabled(true);
			viewHolder.DictionaryDownloadButton.setText(context
					.getString(R.string.download_cancel));
			viewHolder.DictionaryDownloadButton
					.setOnClickListener(new DownloadCancelListener(
							dictionary_name));
		} else {
			viewHolder.DictionaryDownloadButton.setText(context
					.getString(R.string.dictionary_download));
			if (viewHolder.DictionaryDownloadButton.isEnabled() == false) {
				viewHolder.DictionaryDownloadButton.setEnabled(true);
			}
		}

	}

	/**
	 * 下载取消事件
	 * 
	 * @author xuanqinanhai
	 * 
	 */
	class DownloadCancelListener implements OnClickListener {

		private String dictionaryName;

		public DownloadCancelListener(String dictionaryName) {
			this.dictionaryName = dictionaryName;
		}

		@Override
		public void onClick(View v) {
			if (downloadingRunnable.containsKey(dictionaryName)) {
				((DownloadRunnable) downloadingRunnable.get(dictionaryName))
						.stop();
				downloadingRunnable.remove(dictionaryName);
			}
			if (downloadingRunnable.contains("config-" + dictionaryName)) {
				((DownloadRunnable) downloadingRunnable.get("config-"
						+ dictionaryName)).stop();
				downloadingRunnable.remove("config-" + dictionaryName);
			}

		}
	}

	class ViewHolder {
		TextView DictionaryName, DictionarySize;
		Button DictionaryDownloadButton;
	}

	/**
	 * 下载完成后更新数据库
	 */
	Handler DownloadStatusChangeHandler = new Handler() {
		public void handleMessage(Message msg) {
			if (msg.what == Constants.DOWNLOAD_SUCCESS) {
				DownloadInformation downloadInformation = (DownloadInformation) msg.obj;
				SQLiteDatabase sqLiteDatabase = new DictionaryDB(context,
						DictionaryDB.DB_NAME, null, DictionaryDB.DB_VERSION)
						.getWritableDatabase();
				String raw_query;
				if (downloadInformation.downloadSaveName.contains("config-")) {
					raw_query = "update dictionary_list set dictionary_xml_downloaded='1' where dictionary_name='%s'";
				} else {
					raw_query = "update dictionary_list set dictionary_downloaded='1' where dictionary_name='%s'";
					dictionarysDownloading
							.remove(downloadInformation.downloadFileName);
				}
				raw_query = String.format(raw_query,
						downloadInformation.downloadFileName);
				sqLiteDatabase.execSQL(raw_query);
				sqLiteDatabase.close();
			} else if (msg.what == Constants.CONNECTION_ERROR
					|| msg.what == Constants.FILE_CREATE_ERROR
					|| msg.what == Constants.DOWNLOAD_CANCEL) {
				DownloadInformation downloadInformation = (DownloadInformation) msg.obj;
				dictionarysDownloading
						.remove(downloadInformation.downloadFileName);
				getCursor().requery();
			}
		};
	};

	/**
	 * 下载按钮侦听
	 * 
	 * 点击之后，启动下载线程，下载线程启动通知栏，并且启动更新
	 * 
	 * @author xuanqinanhai
	 * 
	 */
	private ArrayList<Integer> notificationIDs = new ArrayList<Integer>();

	class DownloadDictionaryListener implements OnClickListener {

		private String xmlUrl, dictionaryUrl, dictionarySaveName,
				dictionaryCnName, dictionarySize;
		private Context context;
		private int notificationID;

		{
			do {
				notificationID = new Random().nextInt(100000000);
			} while (notificationIDs.contains(notificationID)
					|| notificationIDs.contains(notificationID + 1));
			notificationIDs.add(notificationID);
		}

		public DownloadDictionaryListener(String xmlurl, String dictionayUrl,
				String dictionarySaveName, String dictionaryCnName,
				String dictionarySize, Context context) {
			this.xmlUrl = xmlurl;
			this.dictionarySize = dictionarySize;
			this.dictionaryCnName = dictionaryCnName;
			this.dictionaryUrl = dictionayUrl;
			this.dictionarySaveName = dictionarySaveName;
			this.context = context;
		}

		/**
		 * 下载按钮点击事件。
		 */
		@Override
		public void onClick(View v) {
			String tickerText = context
					.getString(R.string.dictionary_downloading)
					+ dictionaryCnName + "(" + dictionarySize + ")";

			((Button) v).setText(context.getText(R.string.downloading));
			((Button) v).setEnabled(false);

			dictionarysDownloading.add(dictionaryCnName);

			Intent notificationIntent = new Intent(context,
					OnlineDictionaryActivity.class);
			PendingIntent pendingIntent = PendingIntent.getActivity(context, 0,
					notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);
			DownloadNotificationHandler downloadNotificationHandler = new DownloadNotificationHandler(
					context, android.R.drawable.stat_sys_download,
					Notification.FLAG_NO_CLEAR, tickerText, pendingIntent,
					notificationID);
			Handler handlers[] = { downloadNotificationHandler,
					DownloadStatusChangeHandler };
			DownloadRunnable dictionaryRunnable = new DownloadRunnable(
					handlers, this.dictionaryUrl, this.dictionarySaveName,
					this.dictionaryCnName, true, null);
			downloadingRunnable.put(dictionaryCnName, dictionaryRunnable);
			/* 启动字典下载线程 */
			new Thread(dictionaryRunnable).start();

			tickerText = context.getString(R.string.dictionary_downloading)
					+ dictionaryCnName + "配置文件";
			DownloadNotificationHandler downloadNotificationConfigHandler = new DownloadNotificationHandler(
					context, android.R.drawable.stat_sys_download,
					Notification.FLAG_NO_CLEAR, tickerText, pendingIntent,
					notificationID + 1);
			Handler handlersConfig[] = { downloadNotificationConfigHandler,
					DownloadStatusChangeHandler };
			/* 启动字典配置文件下载线程 */
			DownloadRunnable configDownloadRunnable = new DownloadRunnable(
					handlersConfig, this.xmlUrl, this.dictionarySaveName,
					this.dictionaryCnName, true, "config-");
			downloadingRunnable.put("config-" + dictionaryCnName,
					configDownloadRunnable);
			new Thread(configDownloadRunnable).start();
		}
	}

	@Override
	public View newView(Context context, Cursor cursor, ViewGroup parent) {
		return layoutInflater.inflate(R.layout.dictionary_list_item, null);
	}

}
