package com.chooramentools.iradiodownloader;

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
import java.nio.charset.Charset;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeoutException;

/**
 * Created by Bhiefer on 25.5.2014.
 */
public class Downloader
{
	public static final String TAG = "iRadio";

	public List<Item> getItems()
	{
		List<Item> items = new ArrayList<Item>();

		boolean empty = true;
		int offset = 0;
		int lastSize = 0;

		do
		{

			empty = true;
			try
			{
				File listFile = File.createTempFile("list", offset + "");
				URL url = new URL("http://hledani.rozhlas.cz/iRadio/?missingRedirect=&offset=" + offset + "&projekt=Rozhlasov%C3%A9+hry+a+pov%C3%ADdky");

				get(listFile, url);

				XmlPullParser parser = Xml.newPullParser();

				String name = null;
				InputStream is = null;
				try
				{
					is = new FileInputStream(listFile);
					// auto-detect the encoding from the stream
					parser.setInput(is, null);
					int eventType = parser.getEventType();

					Item item = null;

					while (eventType != XmlPullParser.END_DOCUMENT)
					{
						switch (eventType)
						{
							case XmlPullParser.START_TAG:
								name = parser.getName();

								try
								{
									if ("ul".equals(name))
									{
										for (int i = 0; i < parser.getAttributeCount(); i++)
										{
											if ("class".equals(parser.getAttributeName(i)) && "box-audio-archive".equals(parser.getAttributeValue(i)))
											{
												if (item != null)
												{
													items.add(item);
													empty = false;
												}

												item = new Item();

												break;
											}
										}
									}

									if ("div".equals(name))
									{
										for (int i = 0; i < parser.getAttributeCount(); i++)
										{
											if ("class".equals(parser.getAttributeName(i)) && "edition".equals(parser.getAttributeValue(i)))
											{
												if (parser.next() == XmlPullParser.START_TAG && parser.getName().equals("strong") && item != null)
												{
													item.setEdition(Utils.repairFilename(parser.nextText()));
												}

												break;
											}

											if ("class".equals(parser.getAttributeName(i)) && "station".equals(parser.getAttributeValue(i)))
											{
												if (parser.next() == XmlPullParser.START_TAG && parser.getName().equals("a") && item != null)
												{
													item.setStation(Utils.repairFilename(parser.nextText()));
												}

												break;
											}

											if ("class".equals(parser.getAttributeName(i)) && "title".equals(parser.getAttributeValue(i)))
											{
												if (parser.next() == XmlPullParser.START_TAG && parser.getName().equals("span") && item != null)
												{
													String date = parser.nextText();
													SimpleDateFormat dateFormat = new SimpleDateFormat("d.M.yyyy H:mm", Locale.US);

													try
													{
														item.setDate(dateFormat.parse(date));
													}
													catch (ParseException e)
													{
														e.printStackTrace();
													}
												}

												parser.next();
												if (item != null)
												{
													String title = parser.getText().trim();

													title = title.replaceAll("\\([^\\(\\)]*\\.[^\\(\\)]*\\.[^\\(\\)]*\\)", "");

													int last = title.indexOf('/');

													if (last != -1)
													{
														int first = last;

														do
														{
															first--;
														}
														while (Character.isDigit(title.charAt(first)));

														item.setTrack(Integer.parseInt(title.substring(first + 1, last)));
														setTitleAndArtist(item, title.substring(0, first));

														last++;
														first = last;

														do
														{
															last++;
														}
														while (Character.isDigit(title.charAt(last)));

														item.setTotal(Integer.parseInt(title.substring(first, last)));
													}
													else
													{
														title = title.trim();
														if (title.length() > 15)
														{
															String truncated = title.substring(15, Math.min(title.length(), 92));

															int point = truncated.indexOf('.');
															if (point == -1)
															{
																title = title.length() > 60 ? title.substring(0, 60) : title;
															}
															else
															{
																title = title.substring(0, point + 15);
															}
														}

														setTitleAndArtist(item, title);

													}
												}

												break;
											}

											if ("class".equals(parser.getAttributeName(i)) && "action action-player".equals(parser.getAttributeValue(i)))
											{
												if (parser.next() == XmlPullParser.START_TAG && parser.getName().equals("a"))
												{
													for (int j = 0; j < parser.getAttributeCount(); j++)
													{
														if ("href".equals(parser.getAttributeName(j)))
														{
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

						try
						{
							eventType = parser.next();
						}
						catch (Exception ex)
						{
							Log.e(TAG, "Error during parsing: " + name + ": " + ex.toString());
						}
					}

					if (item != null)
					{
						items.add(item);
						empty = false;
					}
				}
				catch (Exception ex)
				{
					Log.e(TAG, "Error during parsing: " + name + ": " + ex.toString());
				}
				finally
				{
					try
					{
						is.close();
					}
					catch (IOException e1)
					{
						Log.e(TAG, Log.getStackTraceString(e1));
					}
				}
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}

			for (int i = lastSize; i < items.size(); i++)
			{
				Log.d(TAG, "Parsed: " + items.get(i));
			}

			lastSize = items.size();

			offset += 10;
		} while (!empty);

		return items;
	}

	public URL getItemUrl(Item item)
	{
		try
		{
			String station;

			if (item.getStation().toLowerCase(Locale.getDefault()).contains("vltava"))
			{
				station = "vltava";
			}
			else if (item.getStation().toLowerCase(Locale.getDefault()).contains("dvojka"))
			{
				station = "dvojka";
			}
			else
			{
				return null;
			}

			File listFile = File.createTempFile(station, item.getId() + "");
			URL url = new URL("http://www.rozhlas.cz/" + station + "/stream");

			get(listFile, url);

			XmlPullParser parser = Xml.newPullParser();

			String name = null;
			InputStream is = null;
			try
			{
				is = new FileInputStream(listFile);

				byte[] matchUrl = item.getPlayerUrlString().getBytes(Charset.forName("US-ASCII"));

				do
				{
					boolean match = true;
					for (int i = 0; i < matchUrl.length; i++)
					{
						int ch = is.read();

						if (ch == -1)
						{
							return null;
						}

						if (matchUrl[i] == ch)
						{
//							Log.d(TAG, "Match: " + (char) matchUrl[i]);
						}
						else
						{
							match = false;
							break;
						}
					}

					if (match)
					{
						byte[] matchHref = "<a href=\"".getBytes(Charset.forName("US-ASCII"));
						while (true)
						{
							match = true;
							for (int i = 0; i < matchHref.length; i++)
							{
								int ch = is.read();

								if (ch == -1)
								{
									return null;
								}

								if (matchHref[i] == ch)
								{
//									Log.d(TAG, "Match: " + (char) matchHref[i]);
								}
								else
								{
									match = false;
									break;
								}
							}

							if (match)
							{
								StringBuilder sb = new StringBuilder();

								char ch = (char) is.read();

								while (ch != '"')
								{
									sb.append(ch);

									ch = (char) is.read();
								}

								String urlString = "http://www.rozhlas.cz" + sb.toString();
								Log.d(TAG, urlString);
								return new URL(urlString);
							}
						}
					}

				} while (true);
			}

			catch (
					Exception ex
					)

			{
				Log.e(TAG, "Error during parsing: " + name + ": " + ex.toString());
			}

			finally

			{
				try
				{
					is.close();
				}
				catch (IOException e1)
				{
					Log.e(TAG, Log.getStackTraceString(e1));
				}
			}
		}

		catch (
				Exception e
				)

		{
			e.printStackTrace();
		}

		return null;
	}

	public void getItemDetails(URL url, Item item)
	{
		try
		{

			File listFile = File.createTempFile("details", item.getId() + "");

			get(listFile, url);

			XmlPullParser parser = Xml.newPullParser();

			StringBuilder comment = new StringBuilder();

			String name = null;
			InputStream is = null;
			boolean inPosition = false;
			try
			{
				is = new FileInputStream(listFile);

				// auto-detect the encoding from the stream
				parser.setInput(is, null);
				int eventType = parser.getEventType();

				while (eventType != XmlPullParser.END_DOCUMENT)
				{
					switch (eventType)
					{
						case XmlPullParser.START_TAG:
							name = parser.getName();

							try
							{
								if ("meta".equals(name) && "image".equals(Utils.getAttribute(parser, "itemprop")))
								{
									item.setArtwork(new URL(Utils.getAttribute(parser, "content")));
								}
								else if ("div".equals(name) && "audio".equals(Utils.getAttribute(parser, "class")))
								{
									int div = 1;
									while ((eventType = parser.next()) != XmlPullParser.END_TAG || !"div".equals(name = parser.getName()) || div > 0)
									{

										if (eventType == XmlPullParser.END_TAG && "div".equals(name))
										{
											div--;
										}
									}

									inPosition = true;
								}
								else if ("div".equals(name) && "image".equals(Utils.getAttribute(parser, "class")))
								{
									while (true)
									{
										try
										{
											if ((eventType = parser.nextTag()) == XmlPullParser.END_TAG && "div".equals(name = parser.getName()))
											{
												break;
											}
										}
										catch (Exception e)
										{
											Log.e(TAG, "Error during parsing: " + name + ": " + e.toString());
										}
									}

									inPosition = true;
								}
								else if ("div".equals(name) && "author".equals(Utils.getAttribute(parser, "class")))
								{
									inPosition = false;

									item.setComment(comment.toString());

									return;
								}
								if ("p".equals(name) && inPosition)
								{
									if (!"perex".equals(Utils.getAttribute(parser, "class")))
									{
										comment.append(parser.nextText()).append('\n');
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

					try
					{
						eventType = parser.next();
					}
					catch (Exception ex)
					{
						Log.e(TAG, "Error during parsing: " + name + ": " + ex.toString());
					}
				}
			}

			catch (Exception ex)
			{
				Log.e(TAG, "Error during parsing: " + name + ": " + ex.toString());
			}

			finally

			{
				try
				{
					is.close();
				}
				catch (IOException e1)
				{
					Log.e(TAG, Log.getStackTraceString(e1));
				}
			}
		}

		catch (Exception e)
		{
			e.printStackTrace();
		}

	}

	private void setTitleAndArtist(Item item, String longTitle)
	{
		int dash = longTitle.indexOf(':');

		if (dash == -1)
		{
			dash = longTitle.indexOf('?');
		}

		if (dash == -1)
		{
			dash = longTitle.indexOf('-');
		}

		if (dash == -1)
		{
			item.setTitle(Utils.repairFilename(longTitle.replace('/', '-').trim()));
		}
		else
		{
			item.setArtist(Utils.repairFilename(longTitle.substring(0, dash).replace('/', '-').trim()));

			if (longTitle.length() > dash)
			{
				item.setTitle(Utils.repairFilename(longTitle.substring(dash + 1).replace('/', '-').trim()));
			}
		}
	}

	public boolean get(File outFile, URL url) throws IOException, TimeoutException
	{
		Log.d(TAG, "Downloading: " + outFile.getAbsolutePath() + " from " + url);

		try
		{
			File parent = outFile.getParentFile();

			// create dirs if parent dir doesn't exist
			if (!parent.exists())
			{
				if (!parent.mkdirs())
				{
					Log.e(TAG, "Parent directory cannot be created: " + parent);
					return false;
				}
			}

			int fileSize = get(new FileOutputStream(outFile), url);

			if (fileSize != -1 && outFile.length() != fileSize)
			{
				Log.e(TAG, "Downloading of " + outFile.getName() + " failed. Deleting... (" + fileSize + ',' + outFile.length() + ')');
				// the download was canceled, so let's delete the partially
				// downloaded file
				outFile.delete();
				return false;
			}
			else
			{
//                if (mProgressListener != null) {
//                    mProgressListener.onProgress(fileSize, fileSize);
//                }

				// notify completion
				if (fileSize == -1)
				{
					Log.d(TAG, "File: " + outFile.getAbsoluteFile() + " hopefully downloaded. Size: " + outFile.length() + " B");
				}
				else
				{
					Log.d(TAG, "File: " + outFile.getAbsoluteFile() + " successfully downloaded. Size: " + outFile.length() + " B");
				}
				return true;
			}
		}
		catch (Exception e)
		{
			Log.e(TAG, Log.getStackTraceString(e));
		}

		return false;
	}

	public int get(OutputStream fileStream, URL url) throws IOException, TimeoutException
	{
		HttpURLConnection conn = null;
		int fileSize;
		BufferedInputStream inStream;
		BufferedOutputStream outStream;

		try
		{
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
			while ((bytesRead = inStream.read(data, 0, data.length)) >= 0)
			{
				outStream.write(data, 0, bytesRead);

				totalBytesRead += bytesRead;

				if (i++ % 200 == 0)
				{
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

		}
		catch (FileNotFoundException e)
		{
			Log.e(TAG, "Connection refused by server");

			return 0;
		}
		finally
		{
			if (conn != null)
			{
				conn.disconnect();
			}
		}
	}
}
