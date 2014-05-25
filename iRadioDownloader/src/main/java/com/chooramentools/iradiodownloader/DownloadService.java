package com.chooramentools.iradiodownloader;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;
import android.util.Xml;

import org.xmlpull.v1.XmlPullParser;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeoutException;

/**
 * Created by Bhiefer on 4.5.2014.
 */
public class DownloadService extends Service {
    public static final String TAG = "iRadio";
    Thread mThread;
    private Notification mNotification;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {

        super.onCreate();

        mNotification = new Notification.Builder(getApplicationContext())
                .setOngoing(true)
                .setTicker("Stahuju audioknizky...")
                .setWhen(System.currentTimeMillis())
                .setProgress(0, 0, true)
                .setContentTitle("Začínám stahovat")
                .setContentText("... příprava ...")
                .build();

        startForeground(R.id.download_notification, mNotification);

        if (mThread != null) {
            mThread.interrupt();
        }

        mThread = new DownloadThread();

        mThread.start();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    private class DownloadThread extends Thread {
        private NotificationManager mNotifyManager;

        @Override
        public void run() {

            List<Item> filtered = new ArrayList<Item>();
            List<Item> downloaded = new ArrayList<Item>();
            try {
                List<Item> items = getItems();

                Log.d(TAG, items.toString());


                for (Item i : items) {
                    if (i.getFile().exists()) {
                        Log.d(TAG, i.toString() + ": File exists");
                    } else {
                        filtered.add(i);
                    }
                }

                Log.d(TAG, "Total: " + items.size() + ", download:" + filtered.size());

                int pos = 1;

                for (Item i : filtered) {
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

                    if (i.getFile().exists()) {
                        Log.d(TAG, "File exists");
                    } else {
                        try {
                            get(i.getFile(), i.getUrl());
                            if (i.getFile().exists()) {
                                downloaded.add(i);
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        } catch (TimeoutException e) {
                            e.printStackTrace();
                        }
                    }

                    pos++;
                }
            }
            finally {

                stopForeground(true);

                Notification.InboxStyle bla = new Notification.InboxStyle(

                        new Notification.Builder(getApplicationContext())
                                .setContentTitle(filtered.size() + " nových souborů")
                                .setContentText(downloaded.size() != filtered.size() ? (filtered.size() - downloaded.size() + " selhaly"):"Vše OK")
                                .setSmallIcon(R.drawable.ic_launcher)
                                .setWhen(System.currentTimeMillis())

                );

                for (int i = 0; i < downloaded.size() && i < 5; i++) {
                    bla.addLine((downloaded.get(i).getArtist() == null ? "" : downloaded.get(i).getArtist()+ "-")   + downloaded.get(i).getTitle());
                }

                if (downloaded.size() > 5) {
                    bla.setSummaryText("+" + (downloaded.size() - 5) + " dalších");
                }

                bla.setBigContentTitle(downloaded.size() != filtered.size() ? (filtered.size() - downloaded.size() + " selhaly"):"Vše OK");

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

    private List<Item> getItems() {
        List<Item> items = new ArrayList<Item>();

        boolean empty = true;
        int offset = 0;
        int lastSize = 0;

        do {

            empty = true;
            try {
                File listFile = File.createTempFile("list", offset + "");
                URL url = new URL("http://hledani.rozhlas.cz/iRadio/?missingRedirect=&offset=" + offset + "&projekt=Rozhlasov%C3%A9+hry+a+pov%C3%ADdky");

                get(listFile, url);

                XmlPullParser parser = Xml.newPullParser();

                String name = null;
                InputStream is = null;
                try {
                    is = new FileInputStream(listFile);
                    // auto-detect the encoding from the stream
                    parser.setInput(is, null);
                    int eventType = parser.getEventType();

                    Item item = null;

                    while (eventType != XmlPullParser.END_DOCUMENT) {
                        switch (eventType) {
                            case XmlPullParser.START_TAG:
                                name = parser.getName();

                                try {
                                    if ("ul".equals(name)) {
                                        for (int i = 0; i < parser.getAttributeCount(); i++) {
                                            if ("class".equals(parser.getAttributeName(i)) && "box-audio-archive".equals(parser.getAttributeValue(i))) {
                                                if (item != null) {
                                                    items.add(item);
                                                    empty = false;
                                                }

                                                item = new Item();

                                                break;
                                            }
                                        }
                                    }

                                    if ("div".equals(name)) {
                                        for (int i = 0; i < parser.getAttributeCount(); i++) {
                                            if ("class".equals(parser.getAttributeName(i)) && "edition".equals(parser.getAttributeValue(i))) {
                                                if (parser.next() == XmlPullParser.START_TAG && parser.getName().equals("strong") && item != null) {
                                                    item.setEdition(repairFilename(parser.nextText()));
                                                }

                                                break;
                                            }

                                            if ("class".equals(parser.getAttributeName(i)) && "station".equals(parser.getAttributeValue(i))) {
                                                if (parser.next() == XmlPullParser.START_TAG && parser.getName().equals("a") && item != null) {
                                                    item.setStation(repairFilename(parser.nextText()));
                                                }

                                                break;
                                            }

                                            if ("class".equals(parser.getAttributeName(i)) && "title".equals(parser.getAttributeValue(i))) {
                                                if (parser.next() == XmlPullParser.START_TAG && parser.getName().equals("span") && item != null) {
                                                    String date = parser.nextText();
                                                    SimpleDateFormat dateFormat = new SimpleDateFormat("d.M.yyyy H:mm", Locale.US);

                                                    try {
                                                        item.setDate(dateFormat.parse(date));
                                                    } catch (ParseException e) {
                                                        e.printStackTrace();
                                                    }
                                                }

                                                parser.next();
                                                if (item != null) {
                                                    String title = parser.getText().trim();

                                                    title = title.replaceAll("\\([^\\(\\)]*\\.[^\\(\\)]*\\.[^\\(\\)]*\\)", "");

                                                    int last = title.indexOf('/');

                                                    if (last != -1) {
                                                        int first = last;

                                                        do {
                                                            first--;
                                                        }
                                                        while (Character.isDigit(title.charAt(first)));

                                                        item.setTrack(Integer.parseInt(title.substring(first + 1, last)));
                                                        setTitleAndArtist(item, title.substring(0, first));

                                                        last++;
                                                        first = last;

                                                        do {
                                                            last++;
                                                        }
                                                        while (Character.isDigit(title.charAt(last)));

                                                        item.setTotal(Integer.parseInt(title.substring(first, last)));
                                                    } else {
                                                        title = title.trim();
                                                        if (title.length() > 15) {
                                                            String truncated = title.substring(15, Math.min(title.length(), 92));

                                                            int point = truncated.indexOf('.');
                                                            if (point == -1) {
                                                                title = title.length() > 60 ? title.substring(0, 60) : title;
                                                            } else {
                                                                title = title.substring(0, point + 15);
                                                            }
                                                        }

                                                        setTitleAndArtist(item, title);

                                                    }
                                                }

                                                break;
                                            }

                                            if ("class".equals(parser.getAttributeName(i)) && "action action-player".equals(parser.getAttributeValue(i))) {
                                                if (parser.next() == XmlPullParser.START_TAG && parser.getName().equals("a")) {
                                                    for (int j = 0; j < parser.getAttributeCount(); j++) {
                                                        if ("href".equals(parser.getAttributeName(j))) {
                                                            String value = parser.getAttributeValue(i);
                                                            item.setId(Long.parseLong(value.substring(value.lastIndexOf('/') + 1)));
                                                            break;
                                                        }
                                                    }
                                                }

                                                break;
                                            }


                                        }
                                    }
                                }
                                catch (Exception ex)
                                {
                                    Log.e(TAG, Log.getStackTraceString(ex));
                                }
                                break;
                            case XmlPullParser.END_TAG:
                                name = parser.getName();
                                break;
                            default:
                                break;
                        }

                        try {
                            eventType = parser.next();
                        } catch (Exception ex) {
                            Log.e(TAG, "Error during parsing: " + name + ": " + ex.toString());
                        }
                    }

                    if (item != null) {
                        items.add(item);
                        empty = false;
                    }
                } catch (Exception ex) {
                    Log.e(TAG, "Error during parsing: " + name + ": " + ex.toString());
                } finally {
                    try {
                        is.close();
                    } catch (IOException e1) {
                        Log.e(TAG, Log.getStackTraceString(e1));
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            for(int i = lastSize; i < items.size(); i++)
            {
                Log.d(TAG, "Parsed: " + items.get(i));
            }

            lastSize = items.size();

            offset += 10;
        } while (!empty);

        return items;
    }

    private void setTitleAndArtist(Item item, String longTitle) {
        int dash = longTitle.indexOf(':');

        if(dash == -1)
        {
            dash = longTitle.indexOf('?');
        }

        if(dash == -1)
        {
            dash = longTitle.indexOf('-');
        }

        if(dash == -1)
        {
            item.setTitle(repairFilename(longTitle.replace('/', '-').trim()));
        }
        else
        {
            item.setArtist(repairFilename(longTitle.substring(0, dash).replace('/', '-').trim()));

            if(longTitle.length() > dash) {
                item.setTitle(repairFilename(longTitle.substring(dash + 1).replace('/', '-').trim()));
            }
        }
    }

    public boolean get(File outFile, URL url) throws IOException, TimeoutException {
        Log.d(TAG, "Downloading: " + outFile.getAbsolutePath() + " from " + url);

        try {
            File parent = outFile.getParentFile();

            // create dirs if parent dir doesn't exist
            if (!parent.exists()) {
                if (!parent.mkdirs()) {
                    Log.e(TAG, "Parent directory cannot be created: " + parent);
                    return false;
                }
            }

            int fileSize = get(new FileOutputStream(outFile), url);

            if (fileSize != -1 && outFile.length() != fileSize) {
                Log.e(TAG, "Downloading of " + outFile.getName() + " failed. Deleting... (" + fileSize + ',' + outFile.length() + ')');
                // the download was canceled, so let's delete the partially
                // downloaded file
                outFile.delete();
                return false;
            } else {
//                if (mProgressListener != null) {
//                    mProgressListener.onProgress(fileSize, fileSize);
//                }

                // notify completion
                if(fileSize == -1) {
                    Log.d(TAG, "File: " + outFile.getAbsoluteFile() + " hopefully downloaded. Size: " + outFile.length() + " B");
                }
                else
                {
                    Log.d(TAG, "File: " + outFile.getAbsoluteFile() + " successfully downloaded. Size: " + outFile.length() + " B");
                }
                return true;
            }
        } catch (Exception e) {
            Log.e(TAG, Log.getStackTraceString(e));
        }

        return false;
    }


    public int get(OutputStream fileStream, URL url) throws IOException, TimeoutException {
        HttpURLConnection conn = null;
        int fileSize;
        BufferedInputStream inStream;
        BufferedOutputStream outStream;

        try {
            conn = (HttpURLConnection) url.openConnection();
            conn.setUseCaches(false);
            conn.setConnectTimeout(20000);
            conn.setReadTimeout(20000);
            conn.connect();
            Log.d(TAG, " HTTP response code: " + conn.getResponseCode());

            fileSize = conn.getContentLength();

            // notify download start
            // int fileSizeInKB = fileSize / 1024;

            // start download
            inStream = new BufferedInputStream(conn.getInputStream());
            outStream = new BufferedOutputStream(fileStream, 4096);
            byte[] data = new byte[4096];
            int bytesRead = 0;
            int totalBytesRead = 0;
            int i = 0;
            while ((bytesRead = inStream.read(data, 0, data.length)) >= 0) {
                outStream.write(data, 0, bytesRead);

                totalBytesRead += bytesRead;

                if (i++ % 200 == 0) {
//                    if (mProgressListener != null)
//                    {
////							log.d("update progress");
//                        mProgressListener.onProgress(totalBytesRead, fileSize);
//                    }
//
//                    if (mCancelledCallback != null)
//                    {
//                        mCancelledCallback.checkCancellation();
//                    }
                }
            }

            outStream.close();
            fileStream.close();
            inStream.close();

            return fileSize;

        } catch (FileNotFoundException e) {
            Log.e(TAG, "Connection refused by server");

            return 0;
        } finally {
            if (conn != null)
                conn.disconnect();
        }
    }

    public static String repairFilename(String filename)
    {
        if (filename == null)
        {
            return null;
        }

        filename = filename.replaceAll(":", "-");
        filename = filename.replaceAll("\\*", "-");
        filename = filename.replaceAll("\\?", ",");
        filename = filename.replaceAll(">", "-");
        filename = filename.replaceAll("<", "-");
        filename = filename.replaceAll("\"", "''");
        filename = filename.replaceAll("\\|", "-");
        filename = filename.replaceAll("/", "-");
        filename = filename.replaceAll("\\\\", "-");
        filename = filename.replaceAll("\\r|\\n", "");


        return filename;
    }
}
