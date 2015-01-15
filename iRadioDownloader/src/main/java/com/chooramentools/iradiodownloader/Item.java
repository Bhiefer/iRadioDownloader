package com.chooramentools.iradiodownloader;

import android.os.Environment;

import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.audio.mp3.MP3File;
import org.jaudiotagger.tag.FieldDataInvalidException;
import org.jaudiotagger.tag.TagOptionSingleton;
import org.jaudiotagger.tag.id3.AbstractID3v2Frame;
import org.jaudiotagger.tag.id3.AbstractID3v2Tag;
import org.jaudiotagger.tag.id3.ID3v11Tag;
import org.jaudiotagger.tag.id3.ID3v24FieldKey;
import org.jaudiotagger.tag.id3.ID3v24Frame;
import org.jaudiotagger.tag.id3.ID3v24Frames;
import org.jaudiotagger.tag.id3.ID3v24Tag;
import org.jaudiotagger.tag.id3.framebody.FrameBodyCOMM;
import org.jaudiotagger.tag.id3.framebody.FrameBodyTCOM;
import org.jaudiotagger.tag.id3.framebody.FrameBodyTPE3;
import org.jaudiotagger.tag.id3.framebody.FrameBodyTRCK;
import org.jaudiotagger.tag.images.ArtworkFactory;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;

/**
 * Created by Bhiefer on 4.5.2014.
 */
public class Item implements Comparable
{

	private String mTitle;
	private String mArtist;
	private Date mDate;
	private String mEdition;
	private String mStation;

	private String mComposer;
	private String mConductor;

	private String mComment;
	private URL mArtwork;
	private long mId;

	private int mTrack;
	private int mTotal;

	public File getFile(String filename)
	{
		File dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC);

		if (dir == null)
		{
			return null;
		}

		dir = new File(dir.getAbsolutePath() + File.separator + "Audiobooks");

		if (!dir.exists())
		{
			dir.mkdirs();
		}

		if (mTrack != 0)
		{
			return new File(dir.getAbsolutePath() + File.separator + mEdition + " (" + mStation + ')' + File.separator + (mArtist == null ? "" : mArtist + "-") + mTitle + " " + mTotal + (mTotal > 4 ? " dílů" : " díly") + File.separator + filename);
		}
		else
		{
			return new File(dir.getAbsolutePath() + File.separator + mEdition + " (" + mStation + ')' + File.separator + (mArtist == null ? "" : mArtist + "-") + mTitle + File.separator + filename);
		}
	}

	public File getFile()
	{
		return getFile(getFilename(mTrack != 0 ? mTrack : 1));
	}

	public File getArtworkFile()
	{
		return getFile("albumart.jpg");
	}

	public File getInfoFile()
	{
		return getFile("info.txt");
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

			f.setID3v1Tag(getID3v1Tag());

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
			tag.addField(tag.createField(ID3v24FieldKey.TITLE, mTitle));
			tag.addField(tag.createField(ID3v24FieldKey.ALBUM, mTitle));

			if (mArtist != null)
			{
				tag.addField(tag.createField(ID3v24FieldKey.ARTIST, mArtist));
			}

			if (getArtworkFile().exists())
			{
				try
				{
					tag.addField(tag.createField(ArtworkFactory.createArtworkFromFile(getArtworkFile())));
				}
				catch (IOException e)
				{
					e.printStackTrace();
				}
			}

			AbstractID3v2Frame frame = getTrackFrame();
			if (frame != null)
			{
				tag.addField(frame);
			}

			frame = getComposerFrame();
			if (frame != null)
			{
				tag.addField(frame);
			}

			frame = getConductorFrame();
			if (frame != null)
			{
				tag.addField(frame);
			}

			frame = getCommentFrame();
			if (frame != null)
			{
				tag.addField(frame);
			}
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
		fb.setTrackNo(mTrack == 0 ? 1 : mTrack);
		fb.setTrackTotal(mTotal == 0 ? 1 : mTotal);
		frame.setBody(fb);
		return frame;
	}

	public ID3v24Frame getComposerFrame()
	{
		if (mComposer != null)
		{
			ID3v24Frame frame = new ID3v24Frame(ID3v24Frames.FRAME_ID_COMPOSER);
			FrameBodyTCOM fb = new FrameBodyTCOM();
			fb.setText(mComposer);
			frame.setBody(fb);
			return frame;
		}
		else
		{
			return null;
		}
	}

	public ID3v24Frame getConductorFrame()
	{
		if (mConductor != null)
		{
			ID3v24Frame frame = new ID3v24Frame(ID3v24Frames.FRAME_ID_CONDUCTOR);
			FrameBodyTPE3 fb = new FrameBodyTPE3();
			fb.setText(mConductor);
			frame.setBody(fb);
			return frame;
		}
		else
		{
			return null;
		}
	}

	public ID3v24Frame getCommentFrame()
	{
		if (mComment != null)
		{
			ID3v24Frame frame = new ID3v24Frame(ID3v24Frames.FRAME_ID_COMMENT);
			FrameBodyCOMM fb = new FrameBodyCOMM();
			fb.setLanguage("cze");
			fb.setText(mComment);
			frame.setBody(fb);
			return frame;
		}
		else
		{
			return null;
		}
	}

	private ID3v11Tag getID3v1Tag()
	{
		ID3v11Tag tag = new ID3v11Tag();

		if (mArtist != null)
		{
			tag.setArtist(Utils.toIso8859(mArtist));
		}
		tag.setAlbum(Utils.toIso8859(mTitle));
		tag.setTitle(Utils.toIso8859(mTitle));

		if (mComment != null)
		{
			tag.setComment(Utils.toIso8859(mComment));
		}

		tag.setTrack(mTrack == 0 ? "1" : mTrack + "");
		return tag;
	}

	public String getPlayerUrlString()
	{
		return "http://prehravac.rozhlas.cz/audio/" + mId;
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

	public int getTotal()
	{
		return mTotal;
	}

	public void setTrack(int mTrack)
	{
		this.mTrack = mTrack;
	}

	@Override
	public String toString()
	{
		return getFile().getAbsolutePath() + " " + mId + ", artwork:" + (mArtwork == null ? "null" : mArtwork.toString()) + "\nComment:\n" + mComment;
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

	public URL getArtwork()
	{
		return mArtwork;
	}

	public void setArtwork(URL artwork)
	{
		mArtwork = artwork;
	}

	public String getComment()
	{
		return mComment;
	}

	public void setComment(String comment)
	{
		mComment = comment.trim();
	}

	@Override
	public int compareTo(Object o)
	{
		if (o instanceof Item)
		{
			Item i = (Item) o;

			return i.getFile().getAbsolutePath().compareTo(this.getFile().getAbsolutePath());
		}

		return 1;
	}
}
