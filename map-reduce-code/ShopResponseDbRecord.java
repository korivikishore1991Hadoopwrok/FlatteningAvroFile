package com.sabre.bigdata.smav2.db.model;

import java.sql.Date;

public class ShopResponseDbRecord {

	private String shopId;
	
	private String airlineCd;
	
	private Date shopDate;
	
	private int rank;
	
	private int share;

	public String getShopId() {
		return shopId;
	}

	public void setShopId(String shopId) {
		this.shopId = shopId;
	}

	public String getAirlineCd() {
		return airlineCd;
	}

	public void setAirlineCd(String airlineCd) {
		this.airlineCd = airlineCd;
	}

	public Date getShopDate() {
		return shopDate;
	}

	public void setShopDate(Date shopDate) {
		this.shopDate = shopDate;
	}

	public int getRank() {
		return rank;
	}

	public void setRank(int rank) {
		this.rank = rank;
	}

	public int getShare() {
		return share;
	}

	public void setShare(int share) {
		this.share = share;
	}
	
}
