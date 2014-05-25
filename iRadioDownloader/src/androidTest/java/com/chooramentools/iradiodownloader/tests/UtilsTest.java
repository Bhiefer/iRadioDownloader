package com.chooramentools.iradiodownloader.tests;

import android.test.suitebuilder.annotation.SmallTest;

import com.chooramentools.iradiodownloader.Utils;

import junit.framework.TestCase;

/**
 * Created by Bhiefer on 25.5.2014.
 */
public class UtilsTest extends TestCase
{

	private final String[] inputs = new String[]
			{
					"áéěíóúůýžščřďťňÁÉĚÍÓÚÝŽŠČŘĎŤŇ"

			};

	private final String[] outputs = new String[]
			{
					"áéeíóúuýzscrdtnÁÉEÍÓÚÝZSCRDTN"
			};

	@SmallTest
	public void testUtils()
	{
		for (int i = 0; i < inputs.length; i++)
		{
			assertEquals(outputs[i], Utils.toIso8859(inputs[i]));
		}
	}
}
