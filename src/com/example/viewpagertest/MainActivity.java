package com.example.viewpagertest;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

@SuppressLint("HandlerLeak")
public class MainActivity extends Activity {

	private ViewPagerAdapter myAdapter = new ViewPagerAdapter();
	private Config config;
	private Button btn_query_word, btn_words_list;
	private View line_btn_query_word, line_btn_words_list;
	private final int line_ids[] = { R.id.line_btn_query_word,
			R.id.line_btn_words_list };
	private Context context;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		config = new Config(getApplicationContext());
		context = this;
		setContentView(R.layout.activity_main);

		final ViewPager viewPager = (ViewPager) findViewById(R.id.viewpager);
		viewPager.setOnPageChangeListener(new ViewPagerScroolListener());

		View query_word_view = LayoutInflater.from(this).inflate(
				R.layout.query_word, null);
		query_word_view.findViewById(R.id.search).setOnClickListener(
				new QueryWordButtonListener());
		View view2 = LayoutInflater.from(this).inflate(R.layout.view2, null);
		view2.findViewById(R.id.button1).setOnClickListener(
				new OnClickListener() {

					@Override
					public void onClick(View v) {
						new Thread() {
							public void run() {
								// db.queryWordId("make");
							};
						}.start();
					}
				});

		myAdapter.addPageView(query_word_view);
		myAdapter.addPageView(view2);
		viewPager.setAdapter(myAdapter);

		btn_query_word = (Button) findViewById(R.id.btn_query_word);
		btn_words_list = (Button) findViewById(R.id.btn_words_list);
		line_btn_query_word = (View) findViewById(R.id.line_btn_query_word);
		line_btn_words_list = (View) findViewById(R.id.line_btn_words_list);

		btn_query_word.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				viewPager.setCurrentItem(0);
				line_btn_query_word
						.setBackgroundResource(R.color.navigate_line_green);
				line_btn_words_list.setBackgroundResource(R.color.gray);
			}
		});

		btn_words_list.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				viewPager.setCurrentItem(1);
				line_btn_words_list
						.setBackgroundResource(R.color.navigate_line_green);
				line_btn_query_word.setBackgroundResource(R.color.gray);
			}
		});

		//CheckIfFirst();
		//CheckWordExist();
	}

	private void CheckWordExist() {
		File file = new File(Environment.getExternalStorageDirectory()
				+ Constants.SAVE_DIRECTORY + "/" + Constants.BASE_DICTIONARY);
		if (file.exists() == false) {
			try {
				InputStream inputStream = context.getAssets().open(
						Constants.BASE_DICTIONARY_ASSET);
				new UnzipFile(new UnzipHandler(MainActivity.this), inputStream,
						Environment.getExternalStorageDirectory() + "/"
								+ Constants.SAVE_DIRECTORY + "/", true).start();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * 检查是否是第一次启动
	 */
	private void CheckIfFirst() {
		int result = config.getIntPreference(Constants.PREF_FIRST);
		result = 0;
		if (result == 0) {
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setMessage(R.string.first_move_alert)
					.setCancelable(false)
					.setPositiveButton(R.string.move_start,
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int id) {
									try {
										UnzipHandler unzipHandler = new UnzipHandler(
												MainActivity.this);
										InputStream inputStream = MainActivity.this
												.getAssets().open(
														"collins.sqlite.zip");
										new UnzipFile(
												unzipHandler,
												inputStream,
												Environment
														.getExternalStorageDirectory()
														+ "/dictionary/", true)
												.start();

										config.setPreference(
												Constants.PREF_FIRST, 1);
									} catch (IOException e) {
										e.printStackTrace();
									}
								}
							})
					.setNegativeButton(R.string.cancel,
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int id) {
									dialog.cancel();
								}
							});
			AlertDialog alert = builder.create();
			alert.show();
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.activity_main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_close:
			finish();
			break;
		case R.id.menu_online_dictionary:
			startActivity(new Intent(this, OnlineDictionaryActivity.class));
			break;
		default:
			break;
		}
		return super.onOptionsItemSelected(item);
	}

	/**
	 * 搜索按钮
	 * 
	 * @author xuanqinanhai
	 * 
	 */
	class QueryWordButtonListener implements OnClickListener {

		@Override
		public void onClick(View v) {
			String word = ((EditText) MainActivity.this
					.findViewById(R.id.edt_search_word)).getText().toString();
			if (word.length() != 0) {
				DictionaryDB db = new DictionaryDB(MainActivity.this,
						DictionaryDB.DB_BASE_DIC, null, DictionaryDB.DB_VERSION);
				LinearLayout parentLayout = (LinearLayout) MainActivity.this
						.findViewById(R.id.dictionary_meaning_content);
				parentLayout.removeAllViewsInLayout();
				// View view = db.queryWord(word);
				// LinearLayout linearLayout = (LinearLayout) view;

				// parentLayout.addView(linearLayout);
				// db.close();
				try {
					LinearLayout linearLayout = (LinearLayout) db.queryWord(
							MainActivity.this, word,
							Environment.getExternalStorageDirectory() + "/"
									+ Constants.SAVE_DIRECTORY
									+ "/config-collins.xml");
					parentLayout.addView(linearLayout);
				} catch (ParserConfigurationException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (SAXException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}

	}

	/**
	 * ViewPager滑动监听
	 */
	class ViewPagerScroolListener implements OnPageChangeListener {

		@Override
		public void onPageScrollStateChanged(int position) {

		}

		@Override
		public void onPageScrolled(int before, float arg1, int after) {
		}

		@Override
		public void onPageSelected(int position) {
			for (int i = 0; i < line_ids.length; i++) {
				findViewById(line_ids[i]).setBackgroundResource(R.color.gray);
			}
			findViewById(line_ids[position]).setBackgroundResource(
					R.color.navigate_line_green);
		}

	}

	@SuppressLint("HandlerLeak")
	Handler MoveHandler = new Handler() {
		private ProgressDialog progressDialog;

		@Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
			if (msg.what == Constants.MOVE_START) {
				progressDialog = new ProgressDialog(MainActivity.this);
				progressDialog.setTitle(R.string.move_dialog_title);
				progressDialog
						.setMessage(getString(R.string.move_dialog_content));
				progressDialog.setCancelable(true);
				progressDialog
						.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
				progressDialog.setMax(msg.arg1);
				progressDialog.show();
			} else if (msg.what == Constants.MOVING) {
				progressDialog.setProgress(msg.arg1);
			} else if (msg.what == Constants.MOVE_END) {
				progressDialog.dismiss();
			} else if (msg.what == Constants.MOVE_ERROR) {
				Toast.makeText(getApplicationContext(), "error",
						Toast.LENGTH_SHORT).show();
			}
			msg = null;
		}
	};
}
