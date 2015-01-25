/* Leichtathletik Daten Verarbeitung (LDV)
 * Copyright (C) 2004 Marc Schunk
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package eu.crushedpixel.replaymod.utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringWriter;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * 
 * @author Marc Schunk, created on 21.06.2004
 * 
 *         A Class to provide helpers of commonly user operations with binary
 *         and character Streams, such as read to an OutputStream/Writer,
 *         String, ByteArray. In addition streams can be compressed.
 */

public class StreamTools {

	/**
	 * Checks weather the given array is contained in the first array
	 * 
	 * @param ar1
	 *            - the containing array
	 * @param index1
	 *            - the index where the search starts
	 * @param ar2
	 *            - the array that schould be contained
	 * @return - true of ar2 is contained in ar1
	 */
	public static final boolean arrayMatch(char[] ar1, int index1, char[] ar2) {
		if((ar1.length - index1 - ar2.length) < 0)
			return false;
		for(int i = 0; i < ar2.length; i++) {
			if(ar1[index1 + i] != ar2[i])
				return false;
		}
		return true;
	}

	/**
	 * Reads inStream completly and writes it to outStream. Streams will not be
	 * closed.
	 * 
	 * @param inStream
	 *            - an InputStream to be read completely
	 * @param outStream
	 *            - the destination Stream
	 * @throws IOException
	 */
	public static void readStream(InputStream inStream, OutputStream outStream)
			throws IOException {
		byte[] buffer = new byte[4096];
		int len = 0;
		while((len = inStream.read(buffer, 0, buffer.length)) > -1) {
			outStream.write(buffer, 0, len);
		}
	}

	/**
	 * Reads inStream completly into an byte[]. Streams will not be closed.
	 * 
	 * @param inStream
	 *            - an InputStream to be read completely
	 * @param outStream
	 *            - the destination Stream
	 * @throws IOException
	 */
	public static byte[] readStream(InputStream inStream) throws IOException {
		byte[] buffer = new byte[4096];
		int len = 0;
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		while((len = inStream.read(buffer, 0, buffer.length)) > -1) {
			baos.write(buffer, 0, len);
		}
		return baos.toByteArray();
	}

	/**
	 * Reads the given Steam into an String *
	 * 
	 * @throws IOException
	 */
	public static String readStreamtoString(InputStream inStream)
			throws IOException {
		StringWriter sw = new StringWriter();
		InputStreamReader isr = new InputStreamReader(inStream);

		char[] buffer = new char[4096];
		int len = 0;
		while((len = isr.read(buffer, 0, buffer.length)) > -1) {
			sw.write(buffer, 0, len);
		}
		return sw.toString();
	}

	/**
	 * Reads the given Steam into an String using the given encoding
	 * 
	 * @throws IOException
	 */
	public static String readStreamtoString(InputStream inStream, String charSet)
			throws IOException {
		StringWriter sw = new StringWriter();
		InputStreamReader isr = new InputStreamReader(inStream, charSet);

		char[] buffer = new char[4096];
		int len = 0;
		while((len = isr.read(buffer, 0, buffer.length)) > -1) {
			sw.write(buffer, 0, len);
		}
		return sw.toString();
	}

	public static String stripComments(String inString, String newLineDelimiter)
			throws IOException {
		StringWriter sw = new StringWriter();
		// use both windows and linux/unix line seperators to be independent of the file orgin
		String[] lines = inString.split("\r\n|\n");
		for(String line : lines) {
			if((line.indexOf("#") == -1) && (line.trim().length() > 0)) {
				sw.append(line).append(newLineDelimiter);
			}
		}
		return sw.toString();
	}

	public static final String[] umlauteString = {"&amp;", "&#223;", "&auml;",
			"&Auml;", "&ouml;", "&Ouml;", "&uuml;", "&Uuml;", "&szlig;"};
	public static final String[] umlauteReplacement = {"&", "ß", "ä", "Ä", "ö",
			"Ö", "ü", "Ü", "ß"};

	public static String replaceSpecialCharacters(String s) {
		for(int i = 0; i < umlauteString.length; i++) {
			s = s.replaceAll(umlauteString[i], umlauteReplacement[i]);
		}
		return s;
	}

	/**
	 * Stores the given Stream in an byte[]. Stream is compressed on the fly.
	 * Streams will not be closed.
	 * 
	 * @param inStream
	 *            - an InputStream to be read completely
	 * @param outStream
	 *            - the destination Stream
	 * @throws IOException
	 */
	public static byte[] compressStreamToByteArray(InputStream inStream)
			throws IOException {
		byte[] buffer = new byte[4096];
		int len = 0;
		ByteArrayOutputStream baos = new ByteArrayOutputStream();

		GZIPOutputStream zos = new GZIPOutputStream(baos);
		while((len = inStream.read(buffer, 0, buffer.length)) > -1) {
			zos.write(buffer, 0, len);
		}
		zos.close();
		return baos.toByteArray();
	}

	/**
	 * Stores the given Stream in an byte[]. Stream is compressed on the fly.
	 * Streams will not be closed.
	 * 
	 * @param inStream
	 *            - an InputStream to be read completely
	 * @param outStream
	 *            - the destination Stream
	 * @throws IOException
	 */
	public static byte[] compress(byte[] uncompressed) throws IOException {
		ByteArrayInputStream bais = new ByteArrayInputStream(uncompressed);
		return compressStreamToByteArray(bais);
	}

	/**
	 * Stores the given Stream in an byte[]. Stream is decompressed on the fly.
	 * Thus, the input stream should contain compressed content. Streams will
	 * not be closed.
	 * 
	 * @param inStream
	 *            - an InputStream to be read completely
	 * @param outStream
	 *            - the destination Stream
	 * @throws IOException
	 */
	public static byte[] decompressStreamToByteArray(InputStream inStream)
			throws IOException {
		byte[] buffer = new byte[4096];
		int len = 0;

		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		GZIPInputStream gzis = new GZIPInputStream(inStream);
		while((len = gzis.read(buffer, 0, buffer.length)) > -1) {
			baos.write(buffer, 0, len);
		}
		inStream.close();
		return baos.toByteArray();
	}

	public static byte[] decompress(byte[] compressed) throws IOException {
		ByteArrayInputStream bais = new ByteArrayInputStream(compressed);
		return decompressStreamToByteArray(bais);
	}

	/**
	 * Reads the given Steam into an String *
	 * 
	 * @throws IOException
	 */
	public static String readReaderToString(Reader reader) throws IOException {
		StringBuffer sb = new StringBuffer();

		char[] buffer = new char[4096];
		int len = 0;
		while((len = reader.read(buffer, 0, buffer.length)) > -1) {
			sb.append(buffer, 0, len);
		}
		return sb.toString();
	}

}
