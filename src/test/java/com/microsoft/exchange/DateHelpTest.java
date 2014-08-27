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
package com.microsoft.exchange;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;
import java.util.TimeZone;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.XMLGregorianCalendar;

import net.fortuna.ical4j.util.TimeZones;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.joda.time.DateTime;
import org.joda.time.Interval;
import org.junit.Test;
import org.springframework.util.CollectionUtils;

public class DateHelpTest {
	
	private final Log log = LogFactory.getLog(this.getClass());

	@Test
	public void systemTimeZone(){
		List<String> availableIDs = Arrays.asList(TimeZone.getAvailableIDs());
		assertFalse(CollectionUtils.isEmpty(availableIDs));
		assertTrue(availableIDs.contains(TimeZones.UTC_ID));
		assertTrue(availableIDs.contains("UTC"));
		
		TimeZone ical4jUTC = TimeZone.getTimeZone(TimeZones.UTC_ID);
		TimeZone sysUTC = TimeZone.getTimeZone("UTC");
		
		assertEquals(ical4jUTC.getDSTSavings(), sysUTC.getDSTSavings());
		assertEquals(ical4jUTC.getRawOffset(), sysUTC.getRawOffset());
		assertTrue(ical4jUTC.hasSameRules(sysUTC));
		
		TimeZone origDefaultTimeZone = TimeZone.getDefault();
		assertNotNull(origDefaultTimeZone);
		assertEquals(TimeZone.getDefault().getRawOffset(), origDefaultTimeZone.getRawOffset());
		
		log.info("TimeZone.DisplayName="+origDefaultTimeZone.getDisplayName());
		log.info("TimeZone.ID="+origDefaultTimeZone.getID());
		log.info("TimeZone.DSTSavings="+origDefaultTimeZone.getDSTSavings());
		log.info("TimeZone.RawOffset="+origDefaultTimeZone.getRawOffset());
		log.info("TimeZone.useDaylightTime="+origDefaultTimeZone.useDaylightTime());
		
		TimeZone.setDefault(ical4jUTC);
		assertEquals(ical4jUTC, TimeZone.getDefault());
		log.info(" -- Defualt Time Zone has been changed successfully! -- ");
		
		TimeZone newDefaultTimeZone = TimeZone.getDefault();
		log.info("TimeZone.DisplayName="+newDefaultTimeZone.getDisplayName());
		log.info("TimeZone.ID="+newDefaultTimeZone.getID());
		log.info("TimeZone.DSTSavings="+newDefaultTimeZone.getDSTSavings());
		log.info("TimeZone.RawOffset="+newDefaultTimeZone.getRawOffset());
		log.info("TimeZone.useDaylightTime="+newDefaultTimeZone.useDaylightTime());
	}
	
	/**
	 * Gets an {@link XMLGregorianCalendar} for the current date time using the default {@link TimeZone}
	 * Test ensures the generated {@link XMLGregorianCalendar} has an offset which is equivalant to the default timezones rawOffSet + dstSavings
	 * @throws DatatypeConfigurationException
	 */
	@Test
	public void getXMLGregorianCalendarNow() throws DatatypeConfigurationException{
		XMLGregorianCalendar xmlGregorianCalendarNow = DateHelp.getXMLGregorianCalendarNow();
		assertNotNull(xmlGregorianCalendarNow);
		int xmlTimeZoneOffsetMinutes = xmlGregorianCalendarNow.getTimezone();
		
		TimeZone xmlTimeZone = xmlGregorianCalendarNow.getTimeZone(xmlTimeZoneOffsetMinutes);
		assertNotNull(xmlTimeZone);
		
		TimeZone jvmTimeZone = TimeZone.getDefault();
		
		xmlGregorianCalendareMatchesTimeZone(xmlGregorianCalendarNow,jvmTimeZone);
		
	}
	
	/**
	 * Computes the time zone offset for a given {@link XMLGregorianCalendar} and compares to the specified {@link TimeZone}
	 * 
	 * 
	 * 
	 * @param xmlGregorianCalendar
	 * @param timeZone
	 */
	public boolean xmlGregorianCalendareMatchesTimeZone(XMLGregorianCalendar xmlGregorianCalendar, TimeZone timeZone){
		int xmlTimeZoneOffsetMinutes = xmlGregorianCalendar.getTimezone();
		TimeZone xmlTimeZone = xmlGregorianCalendar.getTimeZone(xmlTimeZoneOffsetMinutes);
		int jvmRawOffsetMinutes = (timeZone.getRawOffset()/1000/60);
		int jvmDstOffsetMinutes = (timeZone.getDSTSavings()/1000/60);
		
		int xmlRawOffsetMinutes = (xmlTimeZone.getRawOffset()/1000/60);
		int xmlDstOffsetMinutes = (xmlTimeZone.getDSTSavings()/1000/60);
		
		//XMLGregorianCalendar only stores an Int for offset, no DST information.  
		//as a result the xmlTimeZone and jvmTimeZone almost never follow the same rules
		if(timeZone.hasSameRules(xmlTimeZone)){
			log.debug("xmlTimeZoneId="+xmlTimeZone.getID()+ " hasSameRules as jvmTimeZone="+timeZone.getID());
			return true;
		}
		
		if(timeZone.useDaylightTime()){
			//they definately do not when the jvmTimeZone uses DST.
			assertFalse(xmlTimeZone.hasSameRules(timeZone));
			jvmRawOffsetMinutes+=jvmDstOffsetMinutes;
		}
		
		if(xmlTimeZone.useDaylightTime()){
			xmlRawOffsetMinutes+=xmlDstOffsetMinutes;
		}
		if(xmlTimeZoneOffsetMinutes != xmlRawOffsetMinutes){
			log.info("xmlTimeZoneId="+xmlTimeZone.getID()+ " has weird rules");
		}
		
		return (jvmRawOffsetMinutes == xmlRawOffsetMinutes);
		
	}
	
	@Test
	public void getXMLGregorianCalendarsForTimeZones() throws DatatypeConfigurationException{
		List<String> availableIDs = Arrays.asList(TimeZone.getAvailableIDs());
		for(String timeZoneID :availableIDs ){
			TimeZone currTimeZone = TimeZone.getTimeZone(timeZoneID);
			XMLGregorianCalendar currXmlCalendar = DateHelp.getXMLGregorianCalendarNow(currTimeZone);
			boolean match = xmlGregorianCalendareMatchesTimeZone(currXmlCalendar,currTimeZone);
			if(!match)	log.info(currTimeZone.getID() +" "+(match ? "PASSED":"FAILED"));
		}
	}
	
	@Test
	public void splitIntervalTest() {
		DateTime start= new DateTime();
		DateTime end = start.plusYears(1);
		assertTrue(start.isBefore(end));
		
		assertTrue( (end.getMillis() > start.getMillis()) );
		
		List<Interval> intervals = DateHelp.generateIntervals(start.toDate(), end.toDate());
		assertNotNull(intervals);
		assertEquals(2, intervals.size());
		Interval lastInterval = null;
		for(Interval interval : intervals) {
			log.info(interval);
			if(lastInterval != null) {
				assertTrue(interval.abuts(lastInterval));
			}
			lastInterval=interval;
		}
		
	}
	
	@Test
	public void splitMultipleIntervalTest() {
		DateTime start= new DateTime();
		DateTime end = start.plusYears(1);
		assertTrue(start.isBefore(end));
		
		assertTrue( (end.getMillis() > start.getMillis()) );
		
		List<Interval> intervals = DateHelp.generateMultipleIntervals(start.toDate(), end.toDate(),4);
		assertNotNull(intervals);
		assertEquals(4, intervals.size());
		Interval lastInterval = null;
		for(Interval interval : intervals) {
			log.info(interval);
			if(lastInterval != null) {
				assertTrue(interval.abuts(lastInterval));
			}
			lastInterval=interval;
		}
		
	}


}
