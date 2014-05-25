package com.chooramentools.iradiodownloader;

import android.os.Environment;

import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.audio.mp3.MP3File;
import org.jaudiotagger.tag.FieldDataInvalidException;
import org.jaudiotagger.tag.TagField;
import org.jaudiotagger.tag.TagOptionSingleton;
import org.jaudiotagger.tag.id3.AbstractID3v1Tag;
import org.jaudiotagger.tag.id3.AbstractID3v2Tag;
import org.jaudiotagger.tag.id3.ID3v11Tag;
import org.jaudiotagger.tag.id3.ID3v24FieldKey;
import org.jaudiotagger.tag.id3.ID3v24Frame;
import org.jaudiotagger.tag.id3.ID3v24Frames;
import org.jaudiotagger.tag.id3.ID3v24Tag;
import org.jaudiotagger.tag.id3.framebody.FrameBodyTCOM;
import org.jaudiotagger.tag.id3.framebody.FrameBodyTPE3;
import org.jaudiotagger.tag.id3.framebody.FrameBodyTRCK;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;

/**
 * Created by Bhiefer on 4.5.2014.
 */
public class Item
{

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

		if (dir == null)
		{
			return null;
		}

		dir = new File(dir.getAbsolutePath() + File.separator + "Audiobooks/test");

		if (!dir.exists())
		{
			dir.mkdirs();
		}

		if (mTrack != 0)
		{
			return new File(dir.getAbsolutePath() + File.separator + mEdition + " (" + mStation + ')' + File.separator + (mArtist == null ? "" : mArtist + "-") + mTitle + " " + mTotal + " dílů" + File.separator + getFilename(mTrack));
		}
		else
		{
			return new File(dir.getAbsolutePath() + File.separator + mEdition + " (" + mStation + ')' + File.separator + (mArtist == null ? "" : mArtist + "-") + mTitle + File.separator + getFilename(1));
		}
	}

	private String getFilename(int track)
	{
		String filename = String.format("%02d", track) + '.' + mTitle/* + ' ' + new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(mDate)*/;
		return (filename.length() > 92 ? filename.substring(0, 92) : filename) + ".mp3";
	}

	public void storeToTag()
	{
		try
		{
			MP3File f = (MP3File) AudioFileIO.read(getFile());
//			ID3v24Tag tag = (ID3v24Tag) f.getID3v2TagAsv24();

			if (f.hasID3v1Tag())
			{
				f.delete(f.getID3v1Tag());
			}

			if (f.hasID3v2Tag())
			{
				f.delete(f.getID3v2Tag());
			}

//			f.setID3v1Tag(getID3v1Tag());

			f.setID3v2Tag(getID3v2Tag());

			AudioFileIO.write(f);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	private AbstractID3v2Tag getID3v2Tag()
	{
		ID3v24Tag tag = new ID3v24Tag();

		try
		{
			tag.addField(tag.createField(ID3v24FieldKey.TITLE, "title"));
			tag.addField(tag.createField(ID3v24FieldKey.ALBUM, "album"));
			tag.addField(tag.createField(ID3v24FieldKey.ARTIST, "artist"));

			tag.addField(getTrackFrame());

			tag.addField(getComposerFrame());

			tag.addField(getConductorFrame());
		}
		catch (FieldDataInvalidException e)
		{
			e.printStackTrace();
		}

		return tag;
	}

	private ID3v24Frame getTrackFrame()
	{
		ID3v24Frame frame = new ID3v24Frame(ID3v24Frames.FRAME_ID_TRACK);
		TagOptionSingleton.getInstance().setPadNumbers(false);
		FrameBodyTRCK fb = new FrameBodyTRCK();
		fb.setTrackNo(1);
		fb.setTrackTotal(11);
		frame.setBody(fb);
		return frame;
	}

	public TagField getComposerFrame()
	{
		ID3v24Frame frame = new ID3v24Frame(ID3v24Frames.FRAME_ID_COMPOSER);
		FrameBodyTCOM fb = new FrameBodyTCOM();
		fb.setText("Com:" + mArtist);
		frame.setBody(fb);
		return frame;
	}

	public TagField getConductorFrame()
	{
		ID3v24Frame frame = new ID3v24Frame(ID3v24Frames.FRAME_ID_CONDUCTOR);
		FrameBodyTPE3 fb = new FrameBodyTPE3();
		fb.setText("Cond:" + mArtist);
		frame.setBody(fb);
		return frame;
	}

	private AbstractID3v1Tag getID3v1Tag()
	{
		ID3v11Tag tag = new ID3v11Tag();

		tag.setArtist(Utils.toIso8859(mArtist));
		tag.setAlbum(Utils.toIso8859(mTitle));
		tag.setTitle(Utils.toIso8859(mTitle));
		tag.setTrack(mTrack + "");
		return tag;
	}

	public URL getUrl() throws MalformedURLException
	{
		return new URL("http://media.rozhlas.cz/_audio/" + mId + ".mp3");
	}

	public String getEdition()
	{
		return mEdition;
	}

	public void setEdition(String mEdition)
	{
		this.mEdition = mEdition;
	}

	public Date getDate()
	{
		return mDate;
	}

	public void setDate(Date mDate)
	{
		this.mDate = mDate;
	}

	public String getTitle()
	{
		return mTitle;
	}

	public void setTitle(String mTitle)
	{
		this.mTitle = mTitle;
	}

	public String getStation()
	{
		return mStation;
	}

	public void setStation(String mStation)
	{
		this.mStation = mStation;
	}

	public long getId()
	{
		return mId;
	}

	public void setId(long mId)
	{
		this.mId = mId;
	}

	public int getTrack()
	{
		return mTrack;
	}

	public void setTrack(int mTrack)
	{
		this.mTrack = mTrack;
	}

	@Override
	public String toString()
	{
		return getFile().getAbsolutePath() + " " + mId;
	}

	public void setTotal(int total)
	{
		this.mTotal = total;
	}

	public String getArtist()
	{
		return mArtist;
	}

	public void setArtist(String artist)
	{
		this.mArtist = artist;
	}

}
