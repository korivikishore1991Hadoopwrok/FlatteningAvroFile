package com.sabre.bigdata.smav2.db.writer;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import com.sabre.bigdata.smav2.db.model.ShopRequestDbRecord;

public class ShopRequestRecordWriter {

	public static final String INSERT_SQL = "INSERT INTO ";
	
	public static final String VALUES_SQL = " VALUES(?,?,?,?,?)";

	private Connection connection;

	private PreparedStatement statement;

	private long batchSize;

	private long numRecords;
	
	private boolean autoCommit = true;

	public ShopRequestRecordWriter(Connection connection, String table,
			long batchSize, boolean autoCommit) throws SQLException {
		this.connection = connection;
		this.batchSize = batchSize;
		this.autoCommit = autoCommit;
		// set AutoCommit to false to allow Vertica to reuse the same COPY statement
		connection.setAutoCommit(autoCommit);
		StringBuilder sql = new StringBuilder();
		sql.append(INSERT_SQL).append(table).append(VALUES_SQL);
		statement = connection.prepareStatement(sql.toString());
	}

	public void write(ShopRequestDbRecord request) throws IOException {
		try {
			statement.setString(1, request.getShopId());
			statement.setString(2, request.getAirlineCd());
			statement.setDate(3, request.getShopDate());
			statement.setInt(4, request.getPaxCount());
			statement.setString(5, request.getRoboticShoppingInd());
			// add row to the batch
			statement.addBatch();
			numRecords++;
			if (numRecords % batchSize == 0) {
				statement.executeBatch();
			}
		} catch (Exception e) {
			throw new IOException(e);
		}
	}

	public void close() throws IOException {
		try {
			statement.executeBatch();
			if (!autoCommit) {
				// commit the transaction to close the COPY command
				connection.commit();
			}
			connection.close();
		} catch (SQLException e) {
			throw new IOException(e);
		}
	}
}
