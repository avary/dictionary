package com.zhan_dui.dictionary.db;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.SAXException;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Environment;
import android.text.Html;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TextView;

import com.zhan_dui.dictionary.R;
import com.zhan_dui.dictionary.db.DictionaryParseInfomation.EchoViews;
import com.zhan_dui.dictionary.db.DictionaryParseInfomation.TextArg;
import com.zhan_dui.dictionary.handlers.DictionaryXMLHandler;
import com.zhan_dui.dictionary.utils.Constants;

public class DictionaryDB extends SQLiteOpenHelper {

	private final static String DB_PATH = Environment
			.getExternalStorageDirectory()
			+ "/"
			+ Constants.SAVE_DIRECTORY
			+ "/";
	private final Context context;
	public final static int DB_VERSION = 2;
	public final static String DB_NAME = "dictionary.sqlite";
	public final static String DB_BASE_DIC = "dictionary_word.sqlite";

	public final static String DB_DICTIONARY_LIST_NAME = "dictionary_list";

	public DictionaryDB(Context context, String name, CursorFactory factory,
			int version) {
		super(context, name, factory, version);
		this.context = context;
	}

	@Override
	public void onCreate(SQLiteDatabase sqLiteDatabase) {
		String createSql = "create table dictionary_list (_id INTEGER PRIMARY KEY AUTOINCREMENT,dictionary_name text,dictionary_size text,dictionary_url text,dictionary_save_name text,dictionary_downloaded INTEGER default 0,dictionary_show INTEGER default 0);";
		sqLiteDatabase.execSQL(createSql);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

	}

	/**
	 * 在基础词库中查询一个单词的ID
	 * 
	 * @param sqLiteDatabase
	 *            数据库链接
	 * @param word
	 *            单词
	 * @return 返回一个int代表单词的id
	 */
	private int queryWordId(SQLiteDatabase sqLiteDatabase, String word) {
		String[] tableStrings = {"id"};
		Cursor cursor = sqLiteDatabase.query("word", tableStrings, "word='"
				+ word + "'", null, null, null, null);
		if (cursor.moveToNext()) {
			Log.i("word_id", cursor.getInt(0) + "");
			int id = cursor.getInt(0);

			return id;
		}
		return -1;
	}

	private final String BOLD = "<b>%s</b>";
	private final String ITALIC = "<i>%s</i>";
	private final String UNDERLINE = "<u>%s</u>";
	private final String SIZE_SMALL = "<small>%s</small>";
	private final String SIZE_LARGE = "<big>%s</big>";
	private final String FONT = "<font color='%s'>%s</font>";
	// private final String LI_WRAPPER = "<li>%s</li>";
	// private final String UL_WRAPPER = "<ul>%s</ul>";
	// private final String OL_WRAPPER = "<ol>%s</ol>";

	private HashMap<String, DictionaryParseInfomation> cacheXMLInformation = new HashMap<String, DictionaryParseInfomation>();

	/**
	 * 查询单词，根据配置的XML文件查询
	 * 
	 * @param context
	 *            上下文
	 * @param word
	 *            单词
	 * @param xmlfile
	 *            配置文件
	 * @return 返回一个排版好的View，直接用来添加到query_word.xml中
	 * @throws ParserConfigurationException
	 *             XML文件转换异常
	 * @throws SAXException
	 *             SAX转换异常
	 * @throws IOException
	 */
	public View queryWord(Context context, String word, String sqliteFileName,
			String xmlFileName) throws ParserConfigurationException,
			SAXException, IOException {

		SAXParserFactory saxParserFactory = SAXParserFactory.newInstance();
		SAXParser saxParser = saxParserFactory.newSAXParser();
		DictionaryXMLHandler dictionaryXMLHandler = new DictionaryXMLHandler();
		DictionaryParseInfomation dictionaryParseInfomation;

		if (cacheXMLInformation.containsKey(sqliteFileName)) {
			dictionaryParseInfomation = cacheXMLInformation.get(sqliteFileName);
		} else {
			String xmlFilePath = Environment.getExternalStorageDirectory()
					+ "/" + Constants.SAVE_DIRECTORY + "/" + xmlFileName;
			saxParser.parse(new File(xmlFilePath), dictionaryXMLHandler);
			dictionaryParseInfomation = dictionaryXMLHandler.getResults();
			cacheXMLInformation.put(sqliteFileName, dictionaryParseInfomation);
		}

		SQLiteDatabase sqLiteDatabase = SQLiteDatabase.openDatabase(
				Environment.getExternalStorageDirectory() + "/"
						+ Constants.SAVE_DIRECTORY + "/" + sqliteFileName,
				null, SQLiteDatabase.OPEN_READWRITE);

		String table = dictionaryParseInfomation.table;
		String[] columns = (String[]) (dictionaryParseInfomation.queryWords
				.toArray(new String[0]));

		SQLiteDatabase iddatabase = SQLiteDatabase.openDatabase(DB_PATH
				+ DB_BASE_DIC, null, SQLiteDatabase.OPEN_READWRITE);
		int word_id = queryWordId(iddatabase, word);
		Log.i("word_id", word_id + "");
		iddatabase.close();

		String[] selectionArgs = {word_id + ""};
		Cursor cursor = sqLiteDatabase.query(table, columns, "word_id=?",
				selectionArgs, null, null, null);

		LinearLayout linearLayout = new LinearLayout(context);
		linearLayout.setOrientation(LinearLayout.VERTICAL);
		ViewGroup.LayoutParams layoutParams = new LayoutParams(
				LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT);
		linearLayout.setLayoutParams(layoutParams);
		TextView titleView = new TextView(context);
		titleView.setText(dictionaryParseInfomation.title);
		titleView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 18);
		titleView.setTextColor(context.getResources().getColor(
				R.color.lightblack));

		linearLayout.addView(titleView);
		int counter = 1;
		while (cursor.moveToNext()) {// 对检索到的数据做相同的处理
			for (EchoViews echoView : dictionaryParseInfomation.echoViews) {// 遍历配置中需要输出的每个View
				if (echoView.viewType.equalsIgnoreCase("textview")) {
					TextView textView = new TextView(context);
					textView.setLayoutParams(layoutParams);

					textView.setPadding(echoView.view_padding_left,
							echoView.view_padding_top,
							echoView.view_padding_right,
							echoView.view_padding_bottom);

					ArrayList<String> contents = new ArrayList<String>();

					for (TextArg arg : echoView.sprintfArgs) {
						String content = cursor.getString(cursor
								.getColumnIndex(arg.argContent));

						if (arg.action != null) {
							if (arg.action.equals("split")) {
								// |||
								String[] examples = content.split("\\|\\|\\|");
								content = "";
								for (String example : examples) {
									content += example + "<br/>";
								}
							}
						}

						String textColor = arg.textColor;

						if (textColor == null) {
							textColor = "black";
						}

						content = String.format(FONT, textColor, content);

						if (arg.textStyle.equalsIgnoreCase("bold")) {
							content = String.format(BOLD, content);
						} else if (arg.textStyle.equalsIgnoreCase("italic")) {
							content = String.format(ITALIC, content);
						} else if (arg.textStyle.equalsIgnoreCase("underline")) {
							content = String.format(UNDERLINE, content);
						}

						if (arg.textSize.equalsIgnoreCase("normal")) {
						} else if (arg.textSize.equalsIgnoreCase("small")) {
							content = String.format(SIZE_SMALL, content);
						} else if (arg.textSize.equalsIgnoreCase("large")) {
							content = String.format(SIZE_LARGE, content);
						}
						contents.add(content);
					}
					String textViewText = String.format(echoView.sprintfString,
							contents.toArray());
					textView.setText(Html.fromHtml(counter++ + "."
							+ textViewText));
					linearLayout.addView(textView);
				}
			}
		}
		cursor.close();
		sqLiteDatabase.close();
		return linearLayout;
	}

	public View queryWord(String word) {
		if (word.length() == 0) {
			return null;
		}
		SQLiteDatabase sqLiteDatabase = SQLiteDatabase.openDatabase(DB_PATH
				+ DB_BASE_DIC, null, SQLiteDatabase.OPEN_READONLY);
		int word_id = queryWordId(sqLiteDatabase, word);
		String[] tableString = {"simple_meaning"};
		String[] selectionArgs = {word_id + ""};
		Cursor cursor = sqLiteDatabase.query("simpledic", tableString,
				"word_id=?", selectionArgs, null, null, null);
		LinearLayout linearLayout = new LinearLayout(context);
		linearLayout.setOrientation(LinearLayout.VERTICAL);

		ViewGroup.LayoutParams layoutParams = new LayoutParams(
				LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT);
		int count = 1;

		if (cursor.getCount() != 0) {
			TextView titleView = new TextView(context);
			titleView.setText("简明释义:");
			titleView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 18);
			titleView.setTextColor(context.getResources().getColor(
					R.color.lightblack));
			linearLayout.addView(titleView);
		}

		while (cursor.moveToNext()) {
			TextView textView = new TextView(context);
			textView.setText(count++ + "." + cursor.getString(0));
			textView.setPadding(10, 0, 0, 0);
			textView.setLayoutParams(layoutParams);
			textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
			textView.setTextColor(context.getResources()
					.getColor(R.color.black));
			linearLayout.addView(textView);
		}
		cursor.close();
		sqLiteDatabase.close();
		return linearLayout;
	}

}
