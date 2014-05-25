package com.chooramentools.iradiodownloader;

import android.content.Context;
import android.os.Environment;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Created by Bhiefer on 4.5.2014.
 */
public class Item {

    private String mTitle;
    private String mArtist;
    private Date mDate;
    private String mEdition;
    private String mStation;
    private long mId;

    private int mTrack;
    private int mTotal;

    public File getFile()
    {
        File dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC);

        if(dir == null)
        {
            return null;
        }

        dir = new File(dir.getAbsolutePath() + File.separator + "Audiobooks");

        if(!dir.exists())
        {
            dir.mkdirs();
        }

        if(mTrack != 0) {
            return new File(dir.getAbsolutePath() + File.separator + mEdition + " (" + mStation + ')' + File.separator +(mArtist == null ? "" : mArtist + "-" ) + mTitle + " "+ mTotal + " dílů" + File.separator + getFilename(mTrack));
        }
        else
        {
            return new File(dir.getAbsolutePath() + File.separator + mEdition + " (" + mStation + ')'  + File.separator +(mArtist == null ? "" : mArtist + "-" ) + mTitle + File.separator + getFilename(1));
        }
    }

    private String getFilename(int track)
    {
        String filename = String.format("%02d", track) + '.' + mTitle/* + ' ' + new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(mDate)*/;
        return (filename.length() > 92 ? filename.substring(0, 92) : filename) + ".mp3";
    }

    public URL getUrl() throws MalformedURLException {
        return new URL("http://media.rozhlas.cz/_audio/" + mId + ".mp3");
    }

    public String getEdition() {
        return mEdition;
    }

    public void setEdition(String mEdition) {
        this.mEdition = mEdition;
    }

    public Date getDate() {
        return mDate;
    }

    public void setDate(Date mDate) {
        this.mDate = mDate;
    }

    public String getTitle() {
        return mTitle;
    }

    public void setTitle(String mTitle) {
        this.mTitle = mTitle;
    }

    public String getStation() {
        return mStation;
    }

    public void setStation(String mStation) {
        this.mStation = mStation;
    }

    public long getId() {
        return mId;
    }

    public void setId(long mId) {
        this.mId = mId;
    }

    public int getTrack() {
        return mTrack;
    }

    public void setTrack(int mTrack) {
        this.mTrack = mTrack;
    }

    @Override
    public String toString() {
        return getFile().getAbsolutePath() + " " + mId;
    }

    public void setTotal(int total) {
        this.mTotal = total;
    }

    public String getArtist() {
        return mArtist;
    }

    public void setArtist(String artist) {
        this.mArtist = artist;
    }
}
