package com.zhan_dui.dictionary;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
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

import com.zhan_dui.dictionary.adapters.ViewPagerAdapter;
import com.zhan_dui.dictionary.db.DictionaryDB;
import com.zhan_dui.dictionary.handlers.UnzipHandler;
import com.zhan_dui.dictionary.utils.Constants;
import com.zhan_dui.dictionary.utils.UnzipFileInThread;

/**
 * 
 * @ClassName:MainActivity.java
 * @Description:
 * @author xuanqinanhai
 * @date 2012-11-9 上午9:13:52
 */
@SuppressLint("HandlerLeak")
public class MainActivity extends Activity {

	private ViewPagerAdapter myAdapter = new ViewPagerAdapter();
	private Button btn_query_word, btn_words_list;
	private View line_btn_query_word, line_btn_words_list;
	private final int line_ids[] = {R.id.line_btn_query_word,
			R.id.line_btn_words_list};
	private Context context;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		context = this;
		setContentView(R.layout.activity_main);

		final ViewPager viewPager = (ViewPager) findViewById(R.id.viewpager);
		viewPager.setOnPageChangeListener(new ViewPagerScroolListener());

		View query_word_view = LayoutInflater.from(this).inflate(
				R.layout.query_word, null);
		query_word_view.findViewById(R.id.search).setOnClickListener(
				new QueryWordButtonListener());
		View view2 = LayoutInflater.from(this).inflate(R.layout.view2, null);

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

		CheckWordExist();
	}

	/**
	 * 检查基础词库是否存在，如果不存在，则将assets中的zip文件解压到sd卡的Constant.SAVE_DIRECTORY目录中
	 */
	private void CheckWordExist() {
		File file = new File(Environment.getExternalStorageDirectory() + "/"
				+ Constants.SAVE_DIRECTORY + "/" + Constants.BASE_DICTIONARY);
		if (file.exists() == false) {
			try {
				InputStream inputStream = context.getAssets().open(
						Constants.BASE_DICTIONARY_ASSET);
				new UnzipFileInThread(new UnzipHandler(MainActivity.this),
						inputStream, Environment.getExternalStorageDirectory()
								+ "/" + Constants.SAVE_DIRECTORY + "/", true)
						.start();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * 菜单按钮
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.activity_main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.menu_close :
				finish();
				break;
			case R.id.menu_online_dictionary :
				startActivity(new Intent(this, OnlineDictionaryActivity.class));
				break;
			default :
				break;
		}
		return super.onOptionsItemSelected(item);
	}

	/**
	 * 
	 * @Description:搜索按钮按下时候的监听器
	 * @date 2012-11-9 上午9:20:28
	 */
	class QueryWordButtonListener implements OnClickListener {

		@Override
		public void onClick(View v) {
			String word = ((EditText) MainActivity.this
					.findViewById(R.id.edt_search_word)).getText().toString();
			if (word.length() == 0) {
				return;
			}

			LinearLayout parentLayout = (LinearLayout) MainActivity.this
					.findViewById(R.id.dictionary_meaning_content);
			parentLayout.removeAllViewsInLayout();

			DictionaryDB dictionaryDB = new DictionaryDB(context,
					DictionaryDB.DB_NAME, null, DictionaryDB.DB_VERSION);
			SQLiteDatabase sqLiteDatabase = dictionaryDB.getReadableDatabase();
			Cursor cursor = sqLiteDatabase
					.rawQuery(
							"select * from dictionary_list where dictionary_downloaded='1'",
							null);
			while (cursor.moveToNext()) {
				String saveName = cursor.getString(cursor
						.getColumnIndex("dictionary_save_name"));
				String file_name = saveName.replace("zip", "dic");
				String config_file_name = "config-" + file_name;
				LinearLayout linearLayout = null;
				try {
					try {
						linearLayout = (LinearLayout) dictionaryDB.queryWord(
								MainActivity.this, word, file_name,
								config_file_name);
					} catch (IOException e) {
						e.printStackTrace();
					}
				} catch (ParserConfigurationException e) {
					e.printStackTrace();
				} catch (SAXException e) {
					e.printStackTrace();
				}
				parentLayout.addView(linearLayout);
			}
			sqLiteDatabase.close();
		}
	}

	/**
	 * 
	 * @ClassName:MainActivity.java
	 * @Description:ViewPager滑动监听，主要是改变底线的属性
	 * @date 2012-11-9 上午9:19:55
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

	/**
	 * 文件移动时候的Handler
	 */
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
