/*
    Open Manager, an open source file manager for the Android system
    Copyright (C) 2009, 2010, 2011  Joe Berria <nexesdevelopment@gmail.com>

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.zhan_dui.dictionary.filemanager;

import java.io.File;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.ListActivity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.GpsStatus.NmeaListener;
import android.net.Uri;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.zhan_dui.dictionary.R;
import com.zhan_dui.dictionary.utils.NotificationUtils;

/**
 * This is the main activity. The activity that is presented to the user as the
 * application launches. This class is, and expected not to be, instantiated. <br>
 * <p>
 * This class handles creating the buttons and text views. This class relies on
 * the class EventHandler to handle all button press logic and to control the
 * data displayed on its ListView. This class also relies on the FileManager
 * class to handle all file operations such as copy/paste zip/unzip etc. However
 * most interaction with the FileManager class is done via the EventHandler
 * class. Also the SettingsMangager class to load and save user settings. <br>
 * <p>
 * The design objective with this class is to control only the look of the GUI
 * (option menu, context menu, ListView, buttons and so on) and rely on other
 * supporting classes to do the heavy lifting.
 * 
 * @author Joe Berria
 * 
 */
public final class FileSelectorActivity extends ListActivity {

	private static final String PREFS_NAME = "ManagerPrefsFile"; // user
																	// preference
																	// file name
	private static final String PREFS_HIDDEN = "hidden";
	private static final String PREFS_THUMBNAIL = "thumbnail";
	private static final String PREFS_SORT = "sort";

	private static final int SETTING_REQ = 0x10; // request code for intent

	private FileManager mFileMag;
	private EventHandler mHandler;
	private EventHandler.TableRow mTable;

	private SharedPreferences mSettings;
	private boolean mReturnIntent = false;
	private boolean mUseBackKey = true;
	private TextView mPathLabel;
	private ImageButton mSettingButton;
	private Context mContext;
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getWindow().requestFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.main);
		mContext = this;
		/* read settings */
		mSettings = getSharedPreferences(PREFS_NAME, 0);
		boolean hide = mSettings.getBoolean(PREFS_HIDDEN, false);
		boolean thumb = mSettings.getBoolean(PREFS_THUMBNAIL, true);

		int sort = mSettings.getInt(PREFS_SORT, 3);

		mFileMag = new FileManager();
		mFileMag.setShowHiddenFiles(hide);
		mFileMag.setSortType(sort);

		if (savedInstanceState != null)
			mHandler = new EventHandler(FileSelectorActivity.this, mFileMag,
					savedInstanceState.getString("location"));
		else
			mHandler = new EventHandler(FileSelectorActivity.this, mFileMag);

		mTable = mHandler.new TableRow();

		/*
		 * sets the ListAdapter for our ListActivity andgives our EventHandler
		 * class the same adapter
		 */
		mHandler.setListAdapter(mTable);
		setListAdapter(mTable);

		/* register context menu for our list view */
		registerForContextMenu(getListView());

		mPathLabel = (TextView) findViewById(R.id.path_label);
		mPathLabel.setText("path: /sdcard");
		mSettingButton = (ImageButton) findViewById(R.id.setting_file_manager);
		mSettingButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				Intent settings_int = new Intent(FileSelectorActivity.this,
						FileManagerSettings.class);
				settings_int.putExtra("HIDDEN",
						mSettings.getBoolean(PREFS_HIDDEN, false));
				settings_int.putExtra("SORT", mSettings.getInt(PREFS_SORT, 0));
				startActivityForResult(settings_int, SETTING_REQ);
			}
		});

	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putString("location", mFileMag.getCurrentDir());
	}

	@Override
	public void onListItemClick(ListView parent, View view, int position,
			long id) {
		final String item = mHandler.getData(position);
		final File file = new File(mFileMag.getCurrentDir() + "/" + item);
		String item_ext = null;

		try {
			item_ext = item.substring(item.lastIndexOf(".") + 1, item.length());
		} catch (IndexOutOfBoundsException e) {
			item_ext = "";
		}

		/*
		 * If the user has multi-select on, we just need to record the file not
		 * make an intent for it.
		 */

		if (file.isDirectory()) {
			if (file.canRead()) {
				mHandler.updateDirectory(mFileMag.getNextDir(item, false));
				mPathLabel.setText(mFileMag.getCurrentDir());

				/*
				 * set back button switch to true (this will be better
				 * implemented later)
				 */
				if (!mUseBackKey)
					mUseBackKey = true;

			} else {
				Toast.makeText(this, R.string.permission, Toast.LENGTH_SHORT)
						.show();
			}
		}

		else if (item_ext.equalsIgnoreCase("zip")) {
			AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
			builder.setTitle(R.string.alert);
			builder.setMessage(R.string.sure_this_directory);
			builder.setPositiveButton(R.string.ok,
					new DialogInterface.OnClickListener() {

						@Override
						public void onClick(DialogInterface dialog, int which) {
							// TODO 添加解压离线词典
							Toast.makeText(mContext, file.getPath(),
									Toast.LENGTH_SHORT).show();
							NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
							Notification notification = new Notification(
									R.drawable.unzip, "开始解压", System
											.currentTimeMillis());
							notification.flags = Notification.FLAG_AUTO_CANCEL;
							PendingIntent pendingIntent = PendingIntent
									.getActivity(mContext, 0, null, 0);
							notification.setLatestEventInfo(mContext, "", "刷新",
									pendingIntent);
							notificationManager.notify(0, notification);
						}
					});
			builder.setNegativeButton(R.string.no,
					new DialogInterface.OnClickListener() {

						@Override
						public void onClick(DialogInterface dialog, int which) {
							dialog.dismiss();
						}
					});
			builder.show();
		}
	}
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		SharedPreferences.Editor editor = mSettings.edit();
		boolean check;
		boolean thumbnail;
		int sort;

		/*
		 * resultCode must equal RESULT_CANCELED because the only way out of
		 * that activity is pressing the back button on the phone this publishes
		 * a canceled result code not an ok result code
		 */
		if (requestCode == SETTING_REQ && resultCode == RESULT_CANCELED) {
			// save the information we get from settings activity
			check = data.getBooleanExtra("HIDDEN", false);
			sort = data.getIntExtra("SORT", 0);

			editor.putBoolean(PREFS_HIDDEN, check);
			editor.putInt(PREFS_SORT, sort);
			editor.commit();

			mFileMag.setShowHiddenFiles(check);
			mFileMag.setSortType(sort);
			mHandler.updateDirectory(mFileMag.getNextDir(
					mFileMag.getCurrentDir(), true));
		}
	}

	/*
	 * (non-Javadoc) This will check if the user is at root directory. If so, if
	 * they press back again, it will close the application.
	 * 
	 * @see android.app.Activity#onKeyDown(int, android.view.KeyEvent)
	 */
	@Override
	public boolean onKeyDown(int keycode, KeyEvent event) {
		String current = mFileMag.getCurrentDir();

		if (keycode == KeyEvent.KEYCODE_BACK && mUseBackKey
				&& !current.equals("/")) {
			if (mHandler.isMultiSelected()) {
				mTable.killMultiSelect(true);
				Toast.makeText(FileSelectorActivity.this,
						"Multi-select is now off", Toast.LENGTH_SHORT).show();
			} else {
				// stop updating thumbnail icons if its running
				mHandler.updateDirectory(mFileMag.getPreviousDir());
				mPathLabel.setText(mFileMag.getCurrentDir());
			}
			return true;

		} else if (keycode == KeyEvent.KEYCODE_BACK && mUseBackKey
				&& current.equals("/")) {
			Toast.makeText(FileSelectorActivity.this,
					"Press back again to quit.", Toast.LENGTH_SHORT).show();

			if (mHandler.isMultiSelected()) {
				mTable.killMultiSelect(true);
				Toast.makeText(FileSelectorActivity.this,
						"Multi-select is now off", Toast.LENGTH_SHORT).show();
			}

			mUseBackKey = false;
			mPathLabel.setText(mFileMag.getCurrentDir());

			return false;

		} else if (keycode == KeyEvent.KEYCODE_BACK && !mUseBackKey
				&& current.equals("/")) {
			finish();

			return false;
		}
		return false;
	}
}
