package com.sabre.bigdata.smav2.db.model;

import java.sql.Date;


public class ShopRequestDbRecord {

	private String shopId;
	
	private String airlineCd;
	
	private Date shopDate;
	
	private int paxCount;
	
	private String roboticShoppingInd;

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

	public int getPaxCount() {
		return paxCount;
	}

	public void setPaxCount(int paxCount) {
		this.paxCount = paxCount;
	}

	public String getRoboticShoppingInd() {
		return roboticShoppingInd;
	}

	public void setRoboticShoppingInd(String roboticShoppingInd) {
		this.roboticShoppingInd = roboticShoppingInd;
	}
	
}
