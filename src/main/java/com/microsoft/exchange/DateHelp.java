/**
 * See the NOTICE file distributed with this work
 * for additional information regarding copyright ownership.
 * Board of Regents of the University of Wisconsin System
 * licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a
 * copy of the License at:
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
/**
 * 
 */

package com.microsoft.exchange;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.TimeZone;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeConstants;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

import net.fortuna.ical4j.model.DateTime;

import org.apache.commons.lang.time.DateUtils;
import org.joda.time.Interval;
import org.joda.time.Period;

/**
 * @author Nicholas Blair
 */
public class DateHelp {
	public static final Period MAX_PERIOD = Period.days(3660);

	protected static final String DATE_TIME_FORMAT = "yyyyMMdd-HHmm";
	private static final String DATE_FORMAT = "yyyy-MM-dd";
	/**
	 * @return a new instance of {@link SimpleDateFormat} that uses this application's common Date/Time format ("yyyyMMdd-HHmm").
	 */
	public static SimpleDateFormat getDateTimeFormat() {
		return new SimpleDateFormat(DATE_TIME_FORMAT);
	}
	
	/**
	 * Convert a {@link String} in the common date/time format for this application into a {@link Date}.
	 * 
	 * @param timePhrase format: "yyyyMMdd-HHmm"
	 * @return the corresponding date
	 * @throws IllegalArgumentException
	 */
	public static Date parseDateTimePhrase(final String timePhrase) {
		if(timePhrase == null) {
			return null;
		}
		try {
			Date time = getDateTimeFormat().parse(timePhrase);
			time = DateUtils.truncate(time, Calendar.MINUTE);
			return time;
		} catch (ParseException e) {
			throw new IllegalArgumentException("cannot parse date/time phrase " + timePhrase, e);
		}
	}
	
	/**
	 * 
	 * @param date
	 * @return
	 */
	public static XMLGregorianCalendar convertDateToXMLGregorianCalendar(final Date date) {
		return convertDateToXMLGregorianCalendar(date,null);
	}
	
	public static XMLGregorianCalendar convertDateToXMLGregorianCalendar(final Date date, TimeZone tz) {
		
		if(date == null) {
			return null;
		}
		DateTime dt = new DateTime(date);
		GregorianCalendar calendar = new GregorianCalendar();
//		calendar.setTime(date);
		calendar.setTimeInMillis(dt.getTime());
		if(tz != null){
			calendar.setTimeZone(tz);
		}
		
		XMLGregorianCalendar xmlDate = null;
		
		try {
			 xmlDate = DatatypeFactory.newInstance().newXMLGregorianCalendar(calendar);
		} catch (DatatypeConfigurationException e) {
			throw new IllegalStateException("unable to invoke DatatypeFactory.newInstance", e);
		}
		if(tz == null) xmlDate.setTimezone(DatatypeConstants.FIELD_UNDEFINED);
		
		
		long msDiff = Math.abs(xmlDate.toGregorianCalendar().getTime().getTime() - date.getTime());
		org.apache.commons.lang.Validate.isTrue(msDiff < 1000, "original time ("+xmlDate.toGregorianCalendar().getTime()+") differs from converted time ("+date+") by more than 1000ms.  Check the Timezones?");
		
		return xmlDate;
		
	}

	public static DateTime convertXMLGregorianCalendarToDateTime(XMLGregorianCalendar calendar){
		return new DateTime(calendar.toGregorianCalendar().getTime());
	}
	
	/**
	 * 
	 * @param value
	 * @return
	 */
	public static Date makeDate(String value) {
		SimpleDateFormat df = new SimpleDateFormat(DATE_FORMAT);
		try {
			Date date = df.parse(value);
			return DateUtils.truncate(date, java.util.Calendar.DATE);
		} catch (ParseException e) {
			throw new IllegalArgumentException(value + " does not match expected format " + DATE_FORMAT, e);
		}
	}
	
	/**
	 * 
	 * @param value
	 * @return
	 */
	public static Date makeDateTime(String value) {
		SimpleDateFormat df = new SimpleDateFormat(DATE_TIME_FORMAT);
		try {
			Date date = df.parse(value);
			return DateUtils.truncate(date, java.util.Calendar.MINUTE);
		} catch (ParseException e) {
			throw new IllegalArgumentException(value + " does not match expected format " + DATE_FORMAT, e);
		}
	}
	
	public static List<Interval> generateIntervals(org.joda.time.DateTime start, org.joda.time.DateTime end, Period period){
		if(period.getDays()>MAX_PERIOD.getDays()) {
			period = MAX_PERIOD;
		}
		List<Interval> list = new ArrayList<Interval>();
		org.joda.time.DateTime intervalEnd = start.plus(period);
		while(intervalEnd.isBefore(end)){
			list.add(new Interval(start, intervalEnd));
			start = intervalEnd;
			intervalEnd = intervalEnd.plus(period);
		}
		if(start.isBefore(end)) {
			list.add(new Interval(start,end));
		}
		return list;
	}
	
	/**
	 * will always return at least two intervals.
	 * 
	 * @param start
	 * @param end
	 * @param count
	 * @return
	 */
	public static List<Interval> generateMultipleIntervals(Date start, Date end, int count){
		
		List<Interval> intervals = generateIntervals(start, end);
		
		int actualCount = intervals.size();
		if(count > actualCount) {
			
			while(actualCount < count) {
				
				List<Interval> tIntervals = new ArrayList<Interval>();
				for(Interval i: intervals) {
					tIntervals.addAll(generateIntervals(i.getStart().toDate(), i.getEnd().toDate()));
					
				}
				intervals = tIntervals;
				actualCount = intervals.size();
			}
			
		}
		
		return intervals;
	}
	
	
	public static List<Interval> generateIntervals(Date start, Date end){
		org.apache.commons.lang.Validate.isTrue(end.after(start));
		long startInstant = start.getTime();
		long endInstant = end.getTime();
		long midInstant = startInstant + (endInstant - startInstant)/2;
		
		Interval a = new Interval(startInstant,midInstant);
		Interval b = new Interval(midInstant,endInstant);
		
		List<Interval> intervals = new ArrayList<Interval>();
		intervals.add(a);
		intervals.add(b);
		
		return intervals;
	}

}
