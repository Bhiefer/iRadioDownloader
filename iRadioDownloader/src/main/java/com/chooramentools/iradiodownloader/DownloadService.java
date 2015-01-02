package com.chooramentools.iradiodownloader;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Environment;
import android.os.IBinder;
import android.os.StatFs;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
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

		mNotification = new NotificationCompat.Builder(getApplicationContext())
				.setOngoing(true)
				.setTicker("Stahuju audioknizky...")
				.setSmallIcon(R.drawable.ic_notif)
				.setContentTitle("Začínám stahovat")
				.setContentText("... příprava ...")
				.setWhen(System.currentTimeMillis())
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
			checkSpace();

			List<Item> filtered = new ArrayList<Item>();
			List<Item> downloaded = new ArrayList<Item>();

			try
			{
				List<Item> items = mDownloader.getItems();

//				Log.d(TAG, items.toString());

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

				Collections.sort(filtered);

				for (Item i : filtered)
				{
					URL url = mDownloader.getItemUrl(i);

					if (url != null)
					{
						mDownloader.getItemDetails(url, i);
					}

					Log.d(TAG, "Parsed:" + i);
				}

				Log.d(TAG, "Total: " + items.size() + ", download:" + filtered.size());

				int pos = 1;

				for (Item i : filtered)
				{

					mNotification = new NotificationCompat.Builder(getApplicationContext())
							.setOngoing(true)
							.setTicker("Stahuju audioknizky...")
							.setWhen(System.currentTimeMillis())
							.setProgress(filtered.size(), pos, false)
							.setSmallIcon(R.drawable.ic_notif)
							.setContentTitle(i.getArtist() == null ? i.getEdition() : i.getArtist())
							.setContentText((i.getTrack() != 0 ? i.getTrack() + ". " : "") + i.getTitle())
							.build();

					startForeground(R.id.download_notification, mNotification);

					Log.d(TAG, i.toString());

					boolean valid = false;

					try
					{
						if (i.getFile().exists())
						{
							if (i.getFile().length() == mDownloader.getLength(i.getUrl()))
							{
								Log.d(TAG, "File exists");
								valid = true;
							}
							else
							{
								Log.w(TAG, "File exists with incorrect length");
								i.getFile().delete();
							}
						}
					}
					catch (Exception e)
					{
						Log.e(TAG, Log.getStackTraceString(e));
					}

					// soubor neni spravne ulozen, stahnout
					if (!valid)
					{
						try
						{
							mDownloader.get(i.getFile(), i.getUrl());
							if (i.getFile().exists())
							{
								if (i.getFile().length() == mDownloader.getLength(i.getUrl()))
								{
									downloaded.add(i);
								}
								else
								{
									i.getFile().delete();
								}
							}

							File artwork = i.getArtworkFile();

							if (!artwork.exists() && i.getArtwork() != null)
							{
								mDownloader.get(artwork, i.getArtwork());
							}

							File info = i.getInfoFile();

							if (!info.exists() && i.getComment() != null)
							{
								writeInfoFile(i);
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

				NotificationCompat.InboxStyle bla = new NotificationCompat.InboxStyle(

						new NotificationCompat.Builder(getApplicationContext())
								.setContentTitle(filtered.size() + " nových souborů")
								.setContentText(downloaded.size() != filtered.size() ? (filtered.size() - downloaded.size() + " selhaly") : "Vše OK")
								.setSmallIcon(R.drawable.ic_notif)
								.setWhen(System.currentTimeMillis())
				);

				for (int i = 0; i < downloaded.size() && i < 10; i++)
				{
					bla.addLine((downloaded.get(i).getArtist() == null ? "" : downloaded.get(i).getArtist() + "-") + downloaded.get(i).getTitle());
				}

				if (downloaded.size() > 10)
				{
					bla.setSummaryText("+" + (downloaded.size() - 10) + " dalších");
				}

				bla.setBigContentTitle(filtered.size() + " nových souborů");

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

	private void checkSpace()
	{
		File dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC);

		if (dir == null)
		{
			return;
		}

		dir = new File(dir.getAbsolutePath() + File.separator + "Audiobooks");

		long availableSpace = -1L;
		try
		{
			StatFs stat = new StatFs(dir.getAbsolutePath());
			availableSpace = getAvailableBlocks(stat) * getBlockSize(stat);
		}
		catch (Exception e)
		{
			e.printStackTrace();
			return;
		}

		availableSpace /= 1024 * 1024;

		Log.d(TAG, "Volne misto: " + availableSpace);

		if (availableSpace < 200)
		{
			// mene nez 200 MB volneho mista
			try
			{
				delete(dir);
			}
			catch (IOException e)
			{
				return;
			}
		}
	}

	void delete(File f) throws IOException
	{
		if (f.isDirectory())
		{
			for (File c : f.listFiles())
			{
				delete(c);
			}
		}

		f.delete();
	}

	private void writeInfoFile(Item i)
	{
		PrintWriter pw = null;
		try
		{
			pw = new PrintWriter(i.getInfoFile());

			pw.println((i.getArtist() != null) ? i.getArtist() : "Neznámý autor");
			pw.println((i.getTitle() != null) ? i.getTitle() : "Bez názvu");

			pw.println(i.getTotal() < 2 ? "1 soubor" : (i.getTotal() > 1 && i.getTotal() < 5 ? i.getTotal() + " soubory" : i.getTotal() + " souborů"));
			if (i.getStation() != null)
			{
				pw.println(i.getStation());
			}
			if (i.getEdition() != null)
			{
				pw.println(i.getEdition());
			}

			pw.println();

			pw.println((i.getComment() != null) ? i.getComment() : "Žádný komentář.");
		}
		catch (Exception e)
		{
			Log.d(TAG, Log.getStackTraceString(e));
		}
		finally
		{
			if (pw != null)
			{
				pw.close();
			}
		}

	}

	@SuppressWarnings("deprecation")
	private static long getAvailableBlocks(StatFs stat)
	{
		if (Build.VERSION.SDK_INT >= 18)
		{
			return stat.getAvailableBlocksLong();
		}
		else
		{
			return stat.getAvailableBlocks();
		}
	}

	@SuppressWarnings("deprecation")
	private static long getBlockSize(StatFs stat)
	{
		if (Build.VERSION.SDK_INT >= 18)
		{
			return stat.getBlockSizeLong();
		}
		else
		{
			return stat.getBlockSize();
		}
	}

}
