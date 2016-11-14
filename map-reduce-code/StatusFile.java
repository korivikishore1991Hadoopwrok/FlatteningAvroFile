package com.sabre.bigdata.smav2.util;

import java.io.IOException;

import org.apache.commons.io.IOUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.mapreduce.Counters;

public class StatusFile {
	public static enum State {
		RUNNING, SUCCESS, FAIL
	}

	private static final String STATUS = "status";

	private final String startTime;
	private final Configuration config;
	private final String path;

	public StatusFile(String startTime, Configuration config, String path) {
		this.startTime = startTime;
		this.config = config;
		this.path = path;
	}

	public void fail() throws IOException {
		saveFile(STATUS + "=" + State.FAIL);
	}

	public void begin() throws IOException {
		saveFile(STATUS + "=" + State.RUNNING);
	}

	public void success(Counters counters) throws IOException {
		StringBuilder sb = new StringBuilder();
		sb.append(STATUS).append('=').append(State.SUCCESS).append(System.lineSeparator());
		for (BadRecordCounters counterName : BadRecordCounters.values()) {
			sb.append(counterName.name()).append('=').append(counters.findCounter(counterName).getValue())
					.append(System.lineSeparator());
		}

		saveFile(sb.toString());
	}

	private void saveFile(String body) throws IOException {
		Path filePath = new Path(path + "/Status_" + startTime);
		FileSystem fs = filePath.getFileSystem(config);
		fs.delete(filePath, false);
		FSDataOutputStream outputStream = fs.create(filePath);
		IOUtils.write(body, outputStream);
		outputStream.close();
	}
}
