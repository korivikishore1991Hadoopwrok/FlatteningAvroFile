package com.sabre.bigdata.smav2.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.joda.time.DateTime;

public class PathDateParser {
	private static final Pattern DATE_PATTERN = Pattern.compile(".*year=(\\d+).*month=(\\d+).*day=(\\d+)");
	private static final String DATE_FORMAT_PATTERN = "YYYYMMdd";

	public static String parseDateFromPath(String path) {
		Matcher matcher = DATE_PATTERN.matcher(path);
		if (!matcher.matches()) {
			System.err.println("Could not find date in the given path: " + path + ", returning current date");
			return DateTime.now().toString(DATE_FORMAT_PATTERN);
		}

		String year = matcher.group(1);
		String month = matcher.group(2);
		String day = matcher.group(3);

		return year + month + day;
	}
}
