package com.chooramentools.iradiodownloader;

import android.os.Build;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * Created by Bhiefer on 25.5.2014.
 */
public class Utils
{
	public static String toIso8859(String string)
	{
		String s = null;

		string = string.replace('ě', 'e');
		string = string.replace('ů', 'u');
		string = string.replace('ž', 'z');
		string = string.replace('š', 's');
		string = string.replace('č', 'c');
		string = string.replace('ř', 'r');
		string = string.replace('ď', 'd');
		string = string.replace('ť', 't');
		string = string.replace('ň', 'n');
		string = string.replace('Ě', 'E');
		string = string.replace('Ž', 'Z');
		string = string.replace('Š', 'S');
		string = string.replace('Č', 'C');
		string = string.replace('Ř', 'R');
		string = string.replace('Ď', 'D');
		string = string.replace('Ť', 'T');
		string = string.replace('Ň', 'N');

		if (Build.VERSION.SDK_INT >= 19)
		{
			s = new String(string.getBytes(StandardCharsets.ISO_8859_1), StandardCharsets.ISO_8859_1);
		}
		else
		{
			s = new String(string.getBytes(Charset.forName("ISO_8859_1")));
		}

		return s;
	}

	public static double similarity(String s1, String s2)
	{
		if (s1.length() < s2.length())
		{ // s1 should always be bigger
			String swap = s1;
			s1 = s2;
			s2 = swap;
		}
		int bigLen = s1.length();
		if (bigLen == 0)
		{
			return 1.0; /* both strings are zero length */
		}
		return (bigLen - computeEditDistance(s1, s2)) / (double) bigLen;
	}

	public static int computeEditDistance(String s1, String s2)
	{
		s1 = s1.toLowerCase();
		s2 = s2.toLowerCase();

		int[] costs = new int[s2.length() + 1];
		for (int i = 0; i <= s1.length(); i++)
		{
			int lastValue = i;
			for (int j = 0; j <= s2.length(); j++)
			{
				if (i == 0)
				{
					costs[j] = j;
				}
				else
				{
					if (j > 0)
					{
						int newValue = costs[j - 1];
						if (s1.charAt(i - 1) != s2.charAt(j - 1))
						{
							newValue = Math.min(Math.min(newValue, lastValue),
									costs[j]) + 1;
						}
						costs[j - 1] = lastValue;
						lastValue = newValue;
					}
				}
			}
			if (i > 0)
			{
				costs[s2.length()] = lastValue;
			}
		}
		return costs[s2.length()];
	}

	public static void printDistance(String s1, String s2)
	{
		System.out.println(s1 + "-->" + s2 + ": " +
				computeEditDistance(s1, s2) + " (" + similarity(s1, s2) + ")");
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
