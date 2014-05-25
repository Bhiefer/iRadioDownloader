package com.chooramentools.iradiodownloader;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeoutException;

/**
 * Created by Bhiefer on 4.5.2014.
 */
public class DownloadService extends Service
{
	public static final String TAG = "iRadio";
	Thread mThread;
	private Notification mNotification;

	Downloader mDownloader;

	@Override
	public IBinder onBind(Intent intent)
	{
		return null;
	}

	@Override
	public void onCreate()
	{

		super.onCreate();

		mDownloader = new Downloader();

		mNotification = new Notification.Builder(getApplicationContext())
				.setOngoing(true)
				.setTicker("Stahuju audioknizky...")
				.setWhen(System.currentTimeMillis())
				.setProgress(0, 0, true)
				.setContentTitle("Začínám stahovat")
				.setContentText("... příprava ...")
				.build();

		startForeground(R.id.download_notification, mNotification);

		if (mThread != null)
		{
			mThread.interrupt();
		}

		mThread = new DownloadThread();

		mThread.start();
	}

	@Override
	public void onDestroy()
	{
		super.onDestroy();
	}

	private class DownloadThread extends Thread
	{
		private NotificationManager mNotifyManager;

		@Override
		public void run()
		{

			List<Item> filtered = new ArrayList<Item>();
			List<Item> downloaded = new ArrayList<Item>();
			try
			{
				List<Item> items = mDownloader.getItems();

				Log.d(TAG, items.toString());

				for (Item i : items)
				{
					if (i.getFile().exists())
					{
						Log.d(TAG, i.toString() + ": File exists");
					}
					else
					{
						filtered.add(i);
//						break;
					}
				}

				for (Item i : filtered)
				{
					URL url = mDownloader.getItemUrl(i);

					if (url != null)
					{
						mDownloader.getItemDetails(url, i);
					}
				}

				Log.d(TAG, "Total: " + items.size() + ", download:" + filtered.size());

				int pos = 1;

				for (Item i : filtered)
				{
					mNotification = new Notification.Builder(getApplicationContext())
							.setOngoing(true)
							.setTicker("Stahuju audioknizky...")
							.setWhen(System.currentTimeMillis())
							.setProgress(filtered.size(), pos, false)
							.setSmallIcon(R.drawable.ic_launcher)
							.setContentTitle(i.getArtist() == null ? i.getEdition() : i.getArtist())
							.setContentText(i.getTitle())
							.build();

					startForeground(R.id.download_notification, mNotification);

					Log.d(TAG, i.toString());

					if (i.getFile().exists())
					{
						Log.d(TAG, "File exists");
					}
					else
					{
						try
						{
							mDownloader.get(i.getFile(), i.getUrl());
							if (i.getFile().exists())
							{
								downloaded.add(i);
							}
						}
						catch (IOException e)
						{
							e.printStackTrace();
						}
						catch (TimeoutException e)
						{
							e.printStackTrace();
						}
					}

					pos++;
				}
			}
			finally
			{

				for (Item i : downloaded)
				{
					i.storeToTag();
				}

				stopForeground(true);

				Notification.InboxStyle bla = new Notification.InboxStyle(

						new Notification.Builder(getApplicationContext())
								.setContentTitle(filtered.size() + " nových souborů")
								.setContentText(downloaded.size() != filtered.size() ? (filtered.size() - downloaded.size() + " selhaly") : "Vše OK")
								.setSmallIcon(R.drawable.ic_launcher)
								.setWhen(System.currentTimeMillis())
				);

				for (int i = 0; i < downloaded.size() && i < 5; i++)
				{
					bla.addLine((downloaded.get(i).getArtist() == null ? "" : downloaded.get(i).getArtist() + "-") + downloaded.get(i).getTitle());
				}

				if (downloaded.size() > 5)
				{
					bla.setSummaryText("+" + (downloaded.size() - 5) + " dalších");
				}

				bla.setBigContentTitle(downloaded.size() != filtered.size() ? (filtered.size() - downloaded.size() + " selhaly") : "Vše OK");

				mNotification = bla.build();

//                mNotification = new Notification.Builder(getApplicationContext())
//                        .setTicker("Ssdfsdfsdfsdfsfknizky...")
//                        .setWhen(System.currentTimeMillis())
//                        .setSmallIcon(R.drawable.ic_launcher)
//                        .setContentTitle("asdas")
//                        .setContentText("dddddd" )
//                        .build();

				getNotificationManager().notify(R.id.download_notification, mNotification);

				stopSelf();
			}
		}

		private NotificationManager getNotificationManager()
		{
			if (mNotifyManager == null)
			{
				mNotifyManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
			}

			return mNotifyManager;
		}
	}

}
