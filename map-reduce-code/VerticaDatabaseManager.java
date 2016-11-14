package com.sabre.bigdata.smav2.db;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;
import java.util.Random;

import org.apache.hadoop.conf.Configuration;

import com.sabre.bigdata.smav2.db.writer.ShopRequestRecordWriter;
import com.sabre.bigdata.smav2.db.writer.ShopResponseRecordWriter;

public class VerticaDatabaseManager {

	public static final String VERTICA_DRIVER_CLASS = "com.vertica.jdbc.Driver";

	public static final String VERTICA_DRIVER_CLASS_41 = "com.vertica.Driver";

	public static final String HOSTNAMES_PROP = "mapred.vertica.hostnames";

	/** Name of database to connect to */
	public static final String DATABASE_PROP = "mapred.vertica.database";

	/** User name for Vertica */
	public static final String USERNAME_PROP = "mapred.vertica.username";

	/** Password for Vertica */
	public static final String PASSWORD_PROP = "mapred.vertica.password";

	/** Port for Vertica */
	public static final String PORT_PROP = "mapred.vertica.port";

	public static final String DIRECT_BATCH_INSERT_PROP = "mapred.vertica.batch.insert.direct";

	public static final String AUTOCOMMIT_PROP = "mapred.vertica.autocommit";

	/** Batch Size for ShopRequest table */
	public static final String BATCH_SIZE_REQ_PROP = "mapred.vertica.request.table.batchsize";

	/** Batch Size for ShopResponse table */
	public static final String BATCH_SIZE_RES_PROP = "mapred.vertica.response.table.batchsize";

	/** Output request table name */
	public static final String REQ_TABLE_NAME_PROP = "mapred.vertica.request.table.name";

	/** Output response table name */
	public static final String RES_TABLE_NAME_PROP = "mapred.vertica.response.table.name";

	/** Property for speculative execution of MAP tasks */
	public static final String MAP_SPECULATIVE = "mapreduce.map.speculative";

	/** Property for speculative execution of REDUCE tasks */
	public static final String REDUCE_SPECULATIVE = "mapreduce.reduce.speculative";

	public static final int DEFAULT_BATCH_SIZE = 10000;

	public static final boolean DEFAULT_DIRECT_BATCH_INSERT = false;

	public static final boolean DEFAULT_AUTOCOMMIT = true;

	public static final int DEFAULT_PORT_NUMBER = 5433;

	private Configuration conf;

	public VerticaDatabaseManager(Configuration conf) {
		this.conf = conf;
	}

	public Connection getConnection() throws IOException, SQLException {
		try {
			Class.forName(VERTICA_DRIVER_CLASS);
		} catch (ClassNotFoundException e) {
			try {
				Class.forName(VERTICA_DRIVER_CLASS_41);
			} catch (ClassNotFoundException e2) {
				throw new RuntimeException(e);
			}
		}
		String[] hosts = conf.getStrings(HOSTNAMES_PROP);
		String user = conf.get(USERNAME_PROP);
		String pass = conf.get(PASSWORD_PROP);
		String database = conf.get(DATABASE_PROP);
		int port = conf.getInt(PORT_PROP, DEFAULT_PORT_NUMBER);
		boolean directBatchInsert = conf.getBoolean(DIRECT_BATCH_INSERT_PROP,
				DEFAULT_DIRECT_BATCH_INSERT);
		if (hosts == null) {
			throw new IOException("Vertica requies a hostname defined by "
					+ HOSTNAMES_PROP);
		}
		if (database == null) {
			throw new IOException("Vertica requies a database name defined by "
					+ DATABASE_PROP);
		}
		if (user == null) {
			throw new IOException("Vertica requires a username defined by "
					+ USERNAME_PROP);
		}
		Properties connProp = new Properties();
		connProp.put("user", user);
		connProp.put("password", pass);
		connProp.put("DirectBatchInsert", directBatchInsert);
		Random rand = new Random();
		return DriverManager.getConnection(
				"jdbc:vertica://" + hosts[rand.nextInt(hosts.length)] + ":"
						+ port + "/" + database, connProp);
	}
	
	public ShopRequestRecordWriter getShopRequestRecordWriter() throws IOException {
		boolean autoCommit = getAutoCommit();
		long reqBatchSize = getReqBatchSize();
		String reqTableName = getReqTableName();
		try {
			Connection connection = getConnection();
			return new ShopRequestRecordWriter(connection, reqTableName, reqBatchSize, autoCommit);
		} catch (Exception e) {
			throw new IOException(e);
		}
	}
	
	public ShopResponseRecordWriter getShopResponseRecordWriter() throws IOException {
		boolean autoCommit = getAutoCommit();
		long resBatchSize = getResBatchSize();
		String resTableName = getResTableName();
		try {
			Connection connection = getConnection();
			return new ShopResponseRecordWriter(connection, resTableName, resBatchSize, autoCommit);
		} catch (Exception e) {
			throw new IOException(e);
		}
	}
	
	public String getReqTableName() {
		return conf.get(REQ_TABLE_NAME_PROP);
	}
	
	public String getResTableName() {
		return conf.get(RES_TABLE_NAME_PROP);
	}
	
	public long getReqBatchSize() {
		return conf.getLong(BATCH_SIZE_REQ_PROP, DEFAULT_BATCH_SIZE);
	}
	
	public long getResBatchSize() {
		return conf.getLong(BATCH_SIZE_RES_PROP, DEFAULT_BATCH_SIZE);
	}
	
	public boolean getAutoCommit() {
		return conf.getBoolean(AUTOCOMMIT_PROP, DEFAULT_AUTOCOMMIT);
	}
}
