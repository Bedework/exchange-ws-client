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
package com.microsoft.exchange.integration;

import static org.junit.Assert.*;

import java.util.GregorianCalendar;
import java.util.Set;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.XMLGregorianCalendar;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.dao.support.DataAccessUtils;

import com.ibm.icu.util.TimeZone;
import com.microsoft.exchange.DateHelp;
import com.microsoft.exchange.ExchangeWebServices;
import com.microsoft.exchange.exception.ExchangeWebServicesRuntimeException;
import com.microsoft.exchange.impl.BaseExchangeCalendarDataDao;
import com.microsoft.exchange.impl.RequestServerTimeZoneInterceptor;
import com.microsoft.exchange.types.CalendarItemType;
import com.microsoft.exchange.types.ItemIdType;

import edu.emory.mathcs.backport.java.util.Collections;

public class TimeZoneIntegrationTest {
	
	String upn = "ctcudd@wisctest.wisc.edu";
	String badTimeZoneID ="Asia/Gaza";
	protected final Log log = LogFactory.getLog(this.getClass());

	/**
	 * A timezone with no Windows equivalant is set before initalization.
	 *  This causes {@link RequestServerTimeZoneInterceptor} to set the default {@link TimeZone} to UTC
	 *  AND sets {@link RequestServerTimeZoneInterceptor}.windowsID to UTC
	 */
	@Test
	public void unmappableTimeZoneTest(){
		
		TimeZone badTimeZone = TimeZone.getTimeZone(badTimeZoneID);
		TimeZone.setDefault(badTimeZone);
		assertEquals(TimeZone.getDefault(), badTimeZone);
		
		ApplicationContext context = new ClassPathXmlApplicationContext("classpath:test-contexts/exchangeContext.xml");
		RequestServerTimeZoneInterceptor timeZoneInterceptor = context.getBean(RequestServerTimeZoneInterceptor.class);
		
		assertEquals(RequestServerTimeZoneInterceptor.FALLBACK_TIMEZONE_ID, TimeZone.getDefault().getID());
		assertEquals(RequestServerTimeZoneInterceptor.FALLBACK_TIMEZONE_ID, timeZoneInterceptor.getWindowsTimeZoneID());
	}
	
	@Test
	public void badTimeZoneContextTest() throws DatatypeConfigurationException{
		
		TimeZone badTimeZone = TimeZone.getTimeZone(badTimeZoneID);
		TimeZone.setDefault(badTimeZone);
		assertEquals(TimeZone.getDefault(), badTimeZone);
		
		ApplicationContext context = new ClassPathXmlApplicationContext("classpath:test-contexts/exchangeContext.xml");
		RequestServerTimeZoneInterceptor timeZoneInterceptor = context.getBean(RequestServerTimeZoneInterceptor.class);
		
		assertEquals(RequestServerTimeZoneInterceptor.FALLBACK_TIMEZONE_ID, TimeZone.getDefault().getID());
		assertEquals(RequestServerTimeZoneInterceptor.FALLBACK_TIMEZONE_ID, timeZoneInterceptor.getWindowsTimeZoneID());
		
		//time zone currently set to utc, create a date using default time zone
		XMLGregorianCalendar now = DateHelp.getXMLGregorianCalendarNow();
		
		//change the default time zone, so the RequestServerTimeZoneInterceptor is out of sync with default time zone.
		TimeZone.setDefault(badTimeZone);

		ExchangeWebServices ews = context.getBean(ExchangeWebServices.class);
		BaseExchangeCalendarDataDao exchangeCalendarDao = context.getBean(BaseExchangeCalendarDataDao.class);
		exchangeCalendarDao.setWebServices(ews);
		
		//attempt an ews call, which should fail...
		try{
			exchangeCalendarDao.getCalendarFolderMap("ctcudd@wisctest.wisc.edu");
			fail("exchangeCalendarDao should have thrown exception...");
		}catch(ExchangeWebServicesRuntimeException e){}
		
		//change timezone back and call should now succed
		TimeZone.setDefault(TimeZone.getTimeZone(RequestServerTimeZoneInterceptor.FALLBACK_TIMEZONE_ID));
		exchangeCalendarDao.getCalendarFolderMap(upn);
	}
	
	@Test
	public void createGetDeleteCalendarItem() throws DatatypeConfigurationException{
		
		TimeZone utcTimeZone = TimeZone.getTimeZone(RequestServerTimeZoneInterceptor.FALLBACK_TIMEZONE_ID);
		TimeZone.setDefault(utcTimeZone);
		
		ApplicationContext context = new ClassPathXmlApplicationContext("classpath:test-contexts/exchangeContext.xml");
		RequestServerTimeZoneInterceptor timeZoneInterceptor = context.getBean(RequestServerTimeZoneInterceptor.class);
		ExchangeWebServices ews = context.getBean(ExchangeWebServices.class);
		BaseExchangeCalendarDataDao exchangeCalendarDao = context.getBean(BaseExchangeCalendarDataDao.class);
		exchangeCalendarDao.setWebServices(ews);
		
		assertEquals(TimeZone.getDefault(), utcTimeZone);
		
		//XMLGregorianCalendar is sortof backed by a gregorian calendar, date/times should reflect default jvm timezone
		XMLGregorianCalendar xmlStart = DateHelp.getXMLGregorianCalendarNow();
		
		CalendarItemType calendarItem = new CalendarItemType();
		calendarItem.setStart(xmlStart);
	
		ItemIdType itemId = exchangeCalendarDao.createCalendarItem(upn, calendarItem);
		assertNotNull(itemId);
		Set<ItemIdType> itemIds = Collections.singleton(itemId);
		Set<CalendarItemType> calendarItems = exchangeCalendarDao.getCalendarItems(upn, itemIds);
		assertNotNull(calendarItems);
		CalendarItemType createdCalendarItem = DataAccessUtils.singleResult(calendarItems);
		assertNotNull(createdCalendarItem);
		XMLGregorianCalendar createdCalendarItemStart = createdCalendarItem.getStart();
		
		assertNotNull(createdCalendarItemStart);
		assertEquals(xmlStart.getTimezone(), createdCalendarItemStart.getTimezone());

		//nope! tzDisplayName = createdCalendarItem.getTimeZone()
		//assertEquals(RequestServerTimeZoneInterceptor.FALLBACK_TIMEZONE_ID, createdCalendarItem.getTimeZone());
		
		assertEquals(xmlStart.getEon(), createdCalendarItemStart.getEon());
		assertEquals(xmlStart.getEonAndYear(), createdCalendarItemStart.getEonAndYear());
		assertEquals(xmlStart.getYear(), createdCalendarItemStart.getYear());
		assertEquals(xmlStart.getMonth(), createdCalendarItemStart.getMonth());
		assertEquals(xmlStart.getDay(), createdCalendarItemStart.getDay());
		assertEquals(xmlStart.getHour(), createdCalendarItemStart.getHour());	
		assertEquals(xmlStart.getMinute(), createdCalendarItemStart.getMinute());	
		assertEquals(xmlStart.getSecond(), createdCalendarItemStart.getSecond());	
		
		//nope!  always seems to be a slight variation
		//assertEquals(xmlStart.toGregorianCalendar().getTimeInMillis(), createdCalendarItemStart.toGregorianCalendar().getTimeInMillis());
		//assertEquals(xmlStart.getMillisecond(), createdCalendarItemStart.getMillisecond());	
		//assertEquals(xmlStart.getFractionalSecond(), createdCalendarItemStart.getFractionalSecond());
		
		assertTrue(DateHelp.withinOneSecond(xmlStart, createdCalendarItemStart));
		
		assertTrue(exchangeCalendarDao.deleteCalendarItems(upn, itemIds));
	}
	
	@Test
	public void createGetDeleteCalendarItemBadTimeZone() throws DatatypeConfigurationException{
		
		ApplicationContext context = new ClassPathXmlApplicationContext("classpath:test-contexts/exchangeContext.xml");
		RequestServerTimeZoneInterceptor timeZoneInterceptor = context.getBean(RequestServerTimeZoneInterceptor.class);
		ExchangeWebServices ews = context.getBean(ExchangeWebServices.class);
		BaseExchangeCalendarDataDao exchangeCalendarDao = context.getBean(BaseExchangeCalendarDataDao.class);
		exchangeCalendarDao.setWebServices(ews);
		
		
		//XMLGregorianCalendar is sortof backed by a gregorian calendar, date/times should reflect default jvm timezone
		XMLGregorianCalendar xmlStart = DateHelp.getXMLGregorianCalendarNow(java.util.TimeZone.getTimeZone("Pacific/Palau"));
		
		CalendarItemType calendarItem = new CalendarItemType();
		calendarItem.setStart(xmlStart);
	
		ItemIdType itemId = exchangeCalendarDao.createCalendarItem(upn, calendarItem);
		assertNotNull(itemId);
		Set<ItemIdType> itemIds = Collections.singleton(itemId);
		Set<CalendarItemType> calendarItems = exchangeCalendarDao.getCalendarItems(upn, itemIds);
		assertNotNull(calendarItems);
		CalendarItemType createdCalendarItem = DataAccessUtils.singleResult(calendarItems);
		assertNotNull(createdCalendarItem);
		XMLGregorianCalendar createdCalendarItemStart = createdCalendarItem.getStart();
		
		assertNotNull(createdCalendarItemStart);

		//because the XMLGregorian calnedar was created with a time zone other than system default
		assertFalse(xmlStart.getTimezone() == createdCalendarItemStart.getTimezone());
		
		assertTrue(DateHelp.withinOneSecond(xmlStart, createdCalendarItemStart));
		
		assertTrue(exchangeCalendarDao.deleteCalendarItems(upn, itemIds));
	}
	
}
