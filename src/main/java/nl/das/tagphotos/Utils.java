/*
 * Copyright © 2020 Dutch Arrow Software - All Rights Reserved
 * You may use, distribute and modify this code under the
 * terms of the Apache Software License 2.0.
 *
 * Created 02 July 2020.
 */

package nl.das.tagphotos;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import nl.das.tagphotos.model.Photo;

/**
 * General utility methods.
 */
public class Utils {
	/**
	 * Creates a mapper object to be used in the (de-)serialization.
	 *
	 * @return the mapper object
	 */
	public static ObjectMapper createMapper() {
		ObjectMapper mapper = new ObjectMapper();
		// make sure we get ISO 8601 instead of timestamps
		mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
//		mapper.enableDefaultTyping();
		mapper.setVisibility(mapper.getSerializationConfig().getDefaultVisibilityChecker()
				.withFieldVisibility(JsonAutoDetect.Visibility.ANY)
				.withGetterVisibility(JsonAutoDetect.Visibility.NONE)
				.withSetterVisibility(JsonAutoDetect.Visibility.NONE)
				.withCreatorVisibility(JsonAutoDetect.Visibility.NONE));
		mapper.registerModule(new JavaTimeModule());
//		mapper.enableDefaultTyping(ObjectMapper.DefaultTyping.JAVA_LANG_OBJECT);
		return mapper;
	}

	/**
	 * Converts the passed-in <code>object</code> to JSON based on the configured
	 * object mapper.
	 *
	 * @param <T>    the generic type
	 * @param object the object
	 * @return the JSON string
	 */
	public static <T> String toJson(final T object) {
		try {
			return createMapper().writerWithDefaultPrettyPrinter().writeValueAsString(object);
		} catch (final Exception exception) {
			throw new RuntimeException(exception);
		}
	}

	/**
	 * Deserializes the passed-in <code>jsonString</code> based on the specified
	 * Class object <code>clz</code>. If unsuccessful, throws an exception.<br>
	 * <br>
	 * Usage:
	 * <code> Application app = Utils.fromJson(json, Application.class);</code>
	 *
	 * @param <T>        the generic type
	 * @param jsonString the json string
	 * @param clz        the clz
	 * @return the t
	 * @throws Exception
	 */
	public static <T> T fromJson(final String jsonString, final Class<T> clz) throws Exception {
		final T t = createMapper().readValue(jsonString, clz);
		if (t == null) {
			throw new Exception("Cannot convert JSON to " + clz.getSimpleName() + " object:\n" + jsonString);
		}

		return t;
	}

	/**
	 * Deserializes the passed-in <code>jsonString</code> based on the specified
	 * TypeReference object <code>clz</code>. If unsuccessful, throws an
	 * exception.<br>
	 * <br>
	 * Usage:
	 * <code> List&lt;String&gt; strs = Utils.fromJson(json, new TypeReference&lt;List&lt;String&gt;&gt;() {});</code>
	 *
	 * @param <T>        the generic type
	 * @param jsonString the json string
	 * @param clz        the TypeReference class
	 * @return the t
	 * @throws Exception
	 */
	@SuppressWarnings("unchecked")
	public static <T> T fromJson(final String jsonString, TypeReference<?> clz) throws Exception {
		final T t = (T) createMapper().readValue(jsonString, clz);
		if (t == null) {
			throw new Exception("Cannot convert JSON to " + clz.getType().getTypeName() + " object:\n" + jsonString);
		}

		return t;
	}

	/**
	 * Converts a string according to the given format to a
	 * java.util.Date.getTime().
	 * 
	 * @param date
	 * @param format
	 * @return
	 */
	public static long cvtDateFromString(String date, String format) {
		SimpleDateFormat f = new SimpleDateFormat(format);
		try {
			Date d = f.parse(date);
			return d.getTime();
		} catch (ParseException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Converts a java.util.Date.getTime() to a string according to the given
	 * format.
	 * 
	 * @param date
	 * @param format
	 * @return
	 */
	public static String cvtDateToString(long date, String format) {
		SimpleDateFormat f = new SimpleDateFormat(format);
		return f.format(new Date(date));
	}

	/**
	 * Convert a stacktrace to a string.
	 *
	 * @param exception
	 * @return stacktrace as string
	 */
	public static String stacktraceAsString(Exception exception) {
		StringWriter sw = new StringWriter();
		exception.printStackTrace(new PrintWriter(sw));
		return sw.toString();

	}

	public static void dumpPhoto(Photo photo) {
		System.out.println("START - Dump of a photo");
		System.out.println("  id       = " + photo.getId());
		System.out.println("  name     = " + photo.getOrigFilename());
		System.out.println("  o.width  = " + photo.getOrigWidth());
		System.out.println("  o.height = " + photo.getOrigHeight());
		System.out.println("  p.width  = " + photo.getPhotoWidth());
		System.out.println("  p.height = " + photo.getPhotoHeight());
		System.out.println("  t.width  = " + photo.getThumbWidth());
		System.out.println("  t.height = " + photo.getThumbHeight());
		System.out.println("  date     = " + photo.getCreationDate() + " ("
				+ Utils.cvtDateToString(photo.getCreationDate(), "yyyy:MM:dd HH:mm:ss") + ")");
		System.out.println("  tags     = " + photo.getTags());
		System.out.println("  path     = " + photo.getPath());
		System.out.println("END - Dump of a photo");
	}

}
