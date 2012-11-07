package com.zhan_dui.dictionary.utils;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

import com.zhan_dui.dictionary.utils.DownloadUtils.DownloadUtilsInterface;

public class DownloadNotification {
	private DownloadUtilsInterface downloadBehavior;
	private int notifactionXMLId;
	private int progressBarId;
	private Context context;

	private DownloadNotification() {
		
	}
	

	
	public DownloadNotification(Context context,
			DownloadUtilsInterface downloadBehavior, int notifactionXMLId,
			int progressBarId) {
		super();
		this.context = context;
		this.downloadBehavior = downloadBehavior;
		this.notifactionXMLId = notifactionXMLId;
		this.progressBarId = progressBarId;
	}

	public void start(String url, String savePath, String ShowText, int Icon) {
		NotificationManager nm = (NotificationManager) context
				.getSystemService(Context.NOTIFICATION_SERVICE);
		Notification notification = new Notification();

		notification.icon = Icon;
		notification.tickerText = ShowText;

		RemoteViews contentView = new RemoteViews(context.getPackageName(),
				notifactionXMLId);

		notification.contentView = contentView;
		// 使用自定义下拉视图时，不需要再调用setLatestEventInfo()方法
		// 但是必须定义 contentIntent
		Intent intent = new Intent();

		PendingIntent pd = PendingIntent.getActivity(context, 0, intent, 0);
		notification.contentIntent = pd;

		nm.notify(3, notification);

	}
}
