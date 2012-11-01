package com.example.viewpagertest;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.widget.Toast;

public class UnzipHandler extends Handler {

	private Context context;
	private ProgressDialog progressDialog = null;

	public UnzipHandler(Context context) {
		this.context = context;
	}

	/**
	 * what 代表状态 arg1 代表进度 arg2 代表总大小
	 */
	@SuppressWarnings("null")
	@Override
	public void handleMessage(Message msg) {
		super.handleMessage(msg);
		if (msg.what == Constants.UNZIPPING) {
			progressDialog.setMessage(String.valueOf(msg.arg1 / 1024) + "KB");
		} else if (msg.what == Constants.UNZIP_START) {
			progressDialog = new ProgressDialog(context);
			progressDialog.show();
			progressDialog.setTitle("Unzipping");
		} else if (msg.what == Constants.UNZIP_ERROR) {
			Toast.makeText(context, "Unzip error", Toast.LENGTH_SHORT).show();
			progressDialog.dismiss();
		} else if (msg.what == Constants.UNZIP_FINISH) {
			progressDialog.dismiss();
		}
	}
}
