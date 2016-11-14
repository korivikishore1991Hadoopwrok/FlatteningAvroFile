package com.sabre.bigdata.smav2.mapper;

import static com.sabre.bigdata.smav2.util.ApplicationConstants.D;
import static com.sabre.bigdata.smav2.util.ApplicationConstants.DATE_RANGE_PROP;
import static com.sabre.bigdata.smav2.util.ApplicationConstants.DEFAULT_DATE_RANGE;
import static com.sabre.bigdata.smav2.util.ApplicationConstants.EIGHT;
import static com.sabre.bigdata.smav2.util.ApplicationConstants.EMPTY;
import static com.sabre.bigdata.smav2.util.ApplicationConstants.FIELDS_SEPARATOR_PROP;
import static com.sabre.bigdata.smav2.util.ApplicationConstants.ONE_WAY;
import static com.sabre.bigdata.smav2.util.ApplicationConstants.PIPE;
import static com.sabre.bigdata.smav2.util.ApplicationConstants.POS_BOOKING_CHANNELS_PROP;
import static com.sabre.bigdata.smav2.util.ApplicationConstants.ROUND_TRIP;
import static com.sabre.bigdata.smav2.util.ApplicationConstants.SHOP_RECORD_DATA;
import static com.sabre.bigdata.smav2.util.ApplicationConstants.SIX;
import static com.sabre.bigdata.smav2.util.ApplicationConstants.TN_PCC_MAX_LENGTH;
import static com.sabre.bigdata.smav2.util.ApplicationConstants.ZERO;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.avro.mapred.AvroKey;
import org.apache.commons.lang.StringUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;
import org.joda.time.DateTime;
import org.joda.time.Interval;
import org.joda.time.LocalDate;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import com.sabre.bigdata.smav2.types.TaggedKey;
import com.sabre.bigdata.smav2.types.TaggedValue;
import com.sabre.bigdata.smav2.util.BadRecordCounters;
import com.sabre.bigsky.avro.shoprecord.Flight;
import com.sabre.bigsky.avro.shoprecord.Itinerary;
import com.sabre.bigsky.avro.shoprecord.OrigDestStruct;
import com.sabre.bigsky.avro.shoprecord.PAXStruct;
import com.sabre.bigsky.avro.shoprecord.RequestStruct;
import com.sabre.bigsky.avro.shoprecord.ShopRecord;

public class ShopRecordMapper extends Mapper<AvroKey<ShopRecord>, NullWritable, TaggedKey, TaggedValue> {

	private DateTimeFormatter dateTimeFormatter = DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss");

	private DateTimeFormatter dateFormatter = DateTimeFormat.forPattern("yyyy-MM-dd");

	private TaggedKey taggedKey = new TaggedKey();

	private Text joinKey = new Text();

	private int dateRange;

	private String separator;

	private Collection<String> allowedPOSBookingChannels;

	@Override
	protected void setup(Context context) throws IOException, InterruptedException {
		Configuration config = context.getConfiguration();
		// read allowed POS Booking Channels from input parameter
		allowedPOSBookingChannels = config.getStringCollection(POS_BOOKING_CHANNELS_PROP);
		// read fields separator
		separator = config.get(FIELDS_SEPARATOR_PROP, PIPE);
		// read departure date range
		dateRange = config.getInt(DATE_RANGE_PROP, DEFAULT_DATE_RANGE);
	}

	@Override
	protected void map(AvroKey<ShopRecord> key, NullWritable value, Context context) throws IOException,
			InterruptedException {
		ShopRecord shopRecord = key.datum();
		String shopId = shopRecord.getShopID().toString();
		RequestStruct request = shopRecord.getRequest();
		if (request == null) {
			context.getCounter(BadRecordCounters.EMPTY_REQUEST).increment(1);
			return;
		}
		// check response itineraries
		List<Itinerary> responseItineraries = shopRecord.getResponseItineraries();
		if (responseItineraries == null || responseItineraries.isEmpty()) {
			context.getCounter(BadRecordCounters.EMPTY_ITINERARIES).increment(1);
			return;
		}
		// check PCC length
		String posPCC = getString(request.getPOSPCC());
		if (posPCC.length() != TN_PCC_MAX_LENGTH) {
			context.getCounter(BadRecordCounters.WRONG_PCC_LEN).increment(1);
			return;
		}
		if(!StringUtils.isAlphanumeric(posPCC)){
			context.getCounter(BadRecordCounters.INCORRECT_PCC).increment(1);
			return;
		}
		// check PCC extension
		char agencyPCCExtension = posPCC.charAt(3);
		if (agencyPCCExtension == SIX || agencyPCCExtension == EIGHT || agencyPCCExtension == D) {
			context.getCounter(BadRecordCounters.WRONG_PCC_EXTENSION).increment(1);
			return;
		}
		// check POS booking channel
		String posBookingChannel = getString(request.getPOSBookingChannel());
		if (!allowedPOSBookingChannels.contains(posBookingChannel)) {
			context.getCounter(BadRecordCounters.WRONG_POS_BOOKING_CHANNEL).increment(1);
			return;
		}
		// calculate shop date (transaction date)
		DateTime shopDate = calculateShopDate(request);
		if (shopDate == null) {
			context.getCounter(BadRecordCounters.EMPTY_SHOP_DATE).increment(1);
			return;
		}
		String shopDateStr = dateFormatter.print(shopDate);
		DateTime maxDepartureDate = shopDate.plusDays(dateRange);
		Interval departureDateRange = new Interval(shopDate, maxDepartureDate);
		// check origin/destination information
		List<OrigDestStruct> origDest = request.getOrigDest();
		if (origDest == null) {
			context.getCounter(BadRecordCounters.EMPTY_ORIG_DEST_INFO).increment(1);
			return;
		}
		String firstDepDateStr = null;
		DateTime firstDepDate = null;
		String secondDepDateStr = EMPTY;
		String origAirportCd = null;
		String destAirportCd = null;
		String tripType = ONE_WAY;
		int origDestSize = origDest.size();
		if (origDest.isEmpty() || origDestSize > 2) {
			context.getCounter(BadRecordCounters.UNKNOWN_TRIP_TYPE).increment(1);
			return;
		}
		OrigDestStruct firstOrigDest = origDest.get(0);
		if (firstOrigDest != null) {
			origAirportCd = getString(firstOrigDest.getOrigAirportCode());
			destAirportCd = getString(firstOrigDest.getDestAirportCode());
			firstDepDateStr = getString(firstOrigDest.getDepartureDateTime());
			firstDepDate = convertStringFormatToDate(firstDepDateStr);
			if (firstDepDate == null) {
				context.getCounter(BadRecordCounters.WRONG_OUT_DEP_DATE).increment(1);
				return;
			}
			if (!departureDateRange.contains(firstDepDate.getMillis())) {
				context.getCounter(BadRecordCounters.OUT_DEP_DATE_NOT_IN_RANGE).increment(1);
				return;
			}
			if (origDestSize > 1) {
				OrigDestStruct secondOrigDest = origDest.get(1);
				if (secondOrigDest != null) {
					String secondOrigAirportCd = getString(secondOrigDest.getOrigAirportCode());
					String secondDestAirportCd = getString(secondOrigDest.getDestAirportCode());
					if (!secondOrigAirportCd.equalsIgnoreCase(destAirportCd)
							|| !secondDestAirportCd.equalsIgnoreCase(origAirportCd)) {
						context.getCounter(BadRecordCounters.OPEN_JAW_FLIGHTS).increment(1);
						return;
					}
					secondDepDateStr = getString(secondOrigDest.getDepartureDateTime());
					DateTime secondDepDate = convertStringFormatToDate(secondDepDateStr);
					if (secondDepDate == null) {
						context.getCounter(BadRecordCounters.WRONG_IN_DEP_DATE).increment(1);
						return;
					}
					if (!departureDateRange.contains(secondDepDate.getMillis())) {
						context.getCounter(BadRecordCounters.IN_DEP_DATE_NOT_IN_RANGE).increment(1);
						return;
					}
					if (secondDepDate.isBefore(firstDepDate.getMillis())) {
						context.getCounter(BadRecordCounters.NO_LIFE_FLIGHT).increment(1);
						return;
					}
					tripType = ROUND_TRIP;
				}
			}
		}
		else {
			context.getCounter(BadRecordCounters.NULL_ORIG_DEST_RECS).increment(1);
			return;
		}
		String outDepDate = firstDepDateStr.length() > 10 ? firstDepDateStr.substring(0, 10) : firstDepDateStr;
		String inDepDate = secondDepDateStr.length() > 10 ? secondDepDateStr.substring(0, 10) : secondDepDateStr;
		// calculate passenger count
		int totalPaxCount = calculatePassengerCount(request);
		// itineraries
		int itineraryCount = responseItineraries.size();
		Map<CharSequence, Integer> carriersRank = new HashMap<>();
		Map<CharSequence, Integer> carriersShare = new HashMap<>();
		CharSequence currentMarketingArilineCd = null;
		CharSequence currentRph = null;
		int rphChangeIndex = 1;
		for (Itinerary itinerary : responseItineraries) {
			boolean sameMarketingCarrierCd = true;
			Integer itinerarySeqNbr = itinerary.getItinerarySeqNbr();
			List<Flight> flights = itinerary.getFlights();
			if (flights == null || flights.isEmpty()) {
				context.getCounter(BadRecordCounters.EMPTY_FLIGHTS).increment(1);
				continue;
			}
			for (int i = 0; i < flights.size(); i++) {
				Flight flight = flights.get(i);
				if (flight == null) {
					context.getCounter(BadRecordCounters.NULL_FLIGHT).increment(1);
					break;
				}
				CharSequence rph = getString(flight.getRPH());
				CharSequence marketingAirlineCd = flight.getMarketingAirlineCode();
				if (i == 0) {
					currentRph = rph;
					currentMarketingArilineCd = getString(marketingAirlineCd);
				} else {
					if (!currentMarketingArilineCd.equals(marketingAirlineCd)) {
						context.getCounter(BadRecordCounters.DIFFERENT_MARKETING_CARRIER).increment(1);
						sameMarketingCarrierCd = false;
						break;
					}
				}
				if (!currentRph.equals(rph)) {
					rphChangeIndex = i;
					currentRph = rph;
				}
			}
			if (sameMarketingCarrierCd) {
				if (ROUND_TRIP.equals(tripType) && rphChangeIndex < flights.size()) {
					CharSequence outDepartureAirportCd = getString(flights.get(0).getDepartureAirportCode());
					CharSequence outArrivalAirportCd = getString(flights.get(rphChangeIndex - 1).getArrivalAirportCode());
					CharSequence inDepartureAirportCd = getString(flights.get(rphChangeIndex).getDepartureAirportCode());
					CharSequence inArrivalAirportCd = getString(flights.get(flights.size() - 1).getArrivalAirportCode());
					if (!outDepartureAirportCd.equals(inArrivalAirportCd)
							|| !outArrivalAirportCd.equals(inDepartureAirportCd)) {
						context.getCounter(BadRecordCounters.AIRPORT_MISMATCH).increment(1);
						continue;
					}
				}
				if (!carriersRank.containsKey(currentMarketingArilineCd)) {
					carriersRank.put(currentMarketingArilineCd, itinerarySeqNbr);
				}
				Integer carrierShare = carriersShare.get(currentMarketingArilineCd);
				if (carrierShare == null) {
					carriersShare.put(currentMarketingArilineCd, 1);
				} else {
					carriersShare.put(currentMarketingArilineCd, carrierShare + 1);
				}
			}
		}

		StringBuilder reqData = new StringBuilder(64);
		reqData.append(posPCC).append(separator);
		reqData.append(shopDateStr).append(separator);
		reqData.append(origAirportCd).append(separator);
		reqData.append(destAirportCd).append(separator);
		reqData.append(outDepDate).append(separator);
		reqData.append(inDepDate).append(separator);
		reqData.append(totalPaxCount).append(separator);
		reqData.append(itineraryCount);

		joinKey.set(shopId);
		taggedKey.setJoinKey(joinKey);
		taggedKey.setTag(SHOP_RECORD_DATA);
		for (CharSequence airlineCd : carriersRank.keySet()) {
			StringBuilder resData = new StringBuilder(128);
			resData.append(reqData).append(separator);
			resData.append(airlineCd).append(separator);
			resData.append(carriersRank.get(airlineCd)).append(separator);
			int carrierShare = carriersShare.get(airlineCd).intValue();
			resData.append(carrierShare);
			TaggedValue taggedValue = TaggedValue.of(resData.toString(), false);
			context.write(taggedKey, taggedValue);
		}
	}

	private DateTime calculateShopDate(RequestStruct request) {
		DateTime shopDate = null;
		String systemTimestamp = getString(request.getSystemTimestamp());
		if (!EMPTY.equals(systemTimestamp) && !ZERO.equals(systemTimestamp)) {
			shopDate = convertMilisecondsToDate(systemTimestamp);
		} else {
			String requestTimestamp = getString(request.getRequestTimeStampWindow());
			if (!EMPTY.equals(requestTimestamp) && !ZERO.equals(requestTimestamp)) {
				shopDate = convertStringFormatToDate(requestTimestamp);
			}
		}

		return shopDate;
	}

	private int calculatePassengerCount(RequestStruct request) {
		int totalPaxCount = 0;
		List<PAXStruct> passengerList = request.getPassenger();
		if (passengerList != null) {
			for (PAXStruct passenger : passengerList) {
				if (passenger != null) {
					totalPaxCount += getInt(passenger.getPAXQuantity());
				}
			}
		}

		return totalPaxCount;
	}

	private String getString(CharSequence source) {
		if (source == null) {
			return EMPTY;
		}
		return source.toString();
	}

	private int getInt(Integer source) {
		if (source == null) {
			return 0;
		}
		return source.intValue();
	}

	private DateTime convertMilisecondsToDate(String source) {
		long timestamp = 0L;
		try {
			timestamp = Long.parseLong(source);
			return new LocalDate(timestamp).toDateTimeAtStartOfDay();
		} catch (NumberFormatException exc) {
			return null;
		}
	}

	private DateTime convertStringFormatToDate(String source) {
		try {
			return dateTimeFormatter.parseDateTime(source);
		} catch (IllegalArgumentException e) {
			return null;
		}
	}
}