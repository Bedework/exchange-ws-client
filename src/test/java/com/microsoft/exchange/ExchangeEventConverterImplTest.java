package com.microsoft.exchange;

import static org.junit.Assert.*;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.Duration;
import javax.xml.datatype.XMLGregorianCalendar;

import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.ComponentList;
import net.fortuna.ical4j.model.component.VEvent;
import net.fortuna.ical4j.model.parameter.PartStat;
import net.fortuna.ical4j.model.property.Clazz;
import net.fortuna.ical4j.model.property.DtStart;
import net.fortuna.ical4j.model.property.Priority;
import net.fortuna.ical4j.model.property.ProdId;
import net.fortuna.ical4j.model.property.Summary;
import net.fortuna.ical4j.model.property.Version;

import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Test;

import com.microsoft.exchange.impl.ExchangeEventConverterImpl;
import com.microsoft.exchange.types.CalendarItemType;
import com.microsoft.exchange.types.ImportanceChoicesType;
import com.microsoft.exchange.types.ResponseTypeType;
import com.microsoft.exchange.types.SensitivityChoicesType;

import edu.emory.mathcs.backport.java.util.Collections;

public class ExchangeEventConverterImplTest {
	protected final Log log = LogFactory.getLog(this.getClass());

	ExchangeEventConverter eventConverter = new ExchangeEventConverterImpl();
	
	ExchangeEventConverterImpl eventConverterImpl = new ExchangeEventConverterImpl();
	
	@Test
	public void convertedCalendarNotNull(){
		Calendar calendar = eventConverter.convertToCalendar(null, null);
		assertNotNull(calendar);
		ComponentList components = calendar.getComponents();
		assertNotNull(components);
		assertTrue(components.isEmpty());
		log.info(calendar);
	}
	
	@Test
	public void convertedCalendarHasProdId(){
		Calendar calendar = eventConverter.convertToCalendar(null, null);
		ProdId productId = calendar.getProductId();
		assertNotNull(productId);
		assertEquals(ExchangeEventConverter.PROD_ID, productId);
		log.info("productId="+productId);
	}
	
	@Test
	public void convertedCalendarHasVersion2(){
		Calendar calendar = eventConverter.convertToCalendar(null, null);
		Version version = calendar.getVersion();
		assertNotNull(version);
		assertEquals(ExchangeEventConverter.VERSION, version);
		log.info("version="+version);
	}
	
	@Test
	public void convertEmptyCalendarItem(){
		CalendarItemType calendarItem = new CalendarItemType();
		Calendar calendar = eventConverter.convertToCalendar(Collections.singleton(calendarItem), null);
		
		//calendar should not be null
		assertNotNull(calendar);

		ComponentList components = calendar.getComponents();
		
		//components should not be null
		assertNotNull(components);
		
		//components should be empty
		assertTrue(components.isEmpty());
	}
	
	@Test
	public void convertCalendarItemNoEnd(){
		CalendarItemType calendarItem = new CalendarItemType();
		calendarItem.setStart(DateHelp.convertDateToXMLGregorianCalendar(new Date()));
		Calendar calendar = eventConverter.convertToCalendar(Collections.singleton(calendarItem), null);
		
		//calendar should not be null
		assertNotNull(calendar);

		ComponentList components = calendar.getComponents();
		
		//components should not be null
		assertNotNull(components);
		
		//components should be empty
		assertTrue(components.isEmpty());
	}
	
	@Test
	public void convertedCalendarMatchesSubject() throws DatatypeConfigurationException{
		CalendarItemType calendarItem = new CalendarItemType();
		String randomSubject = RandomStringUtils.random(32);
		
		calendarItem.setStart(DateHelp.convertDateToXMLGregorianCalendar(new Date()));
		Duration duration = DatatypeFactory.newInstance().newDuration(1000 * 60 * 60);
		XMLGregorianCalendar end = calendarItem.getStart();
		end.add(duration);
		calendarItem.setEnd(end);
		
		calendarItem.setSubject(randomSubject);
		log.info("created calendar item with subject="+randomSubject);
		Calendar calendar = eventConverter.convertToCalendar(Collections.singleton(calendarItem), null);
		
		//calendar should not be null
		assertNotNull(calendar);
		ComponentList components = calendar.getComponents();
		//calendar should have components
		assertNotNull(components);
		
		//calendar should have exactly one component
		assertEquals(1, components.size());
		
		//components should be events
		assertEquals(components, calendar.getComponents(VEvent.VEVENT));
		
		Object object = components.get(0);
		assertNotNull(object);
		assertTrue(object instanceof VEvent);
		
		VEvent event =(VEvent) object;
		assertNotNull(event);
		
		Summary summary = event.getSummary();
		assertNotNull(summary);
		assertNotNull(summary.getValue());
		assertEquals(randomSubject, summary.getValue());
		log.info("converted event summary["+summary.getValue()+"] matches calendar item sujbect["+calendarItem.getSubject()+"]");
	}
	
	@Test
	public void convertedCalendarMatchesStartTime() throws DatatypeConfigurationException{
		CalendarItemType calendarItem = new CalendarItemType();		
		Date dateStartIn = new Date();
		XMLGregorianCalendar xmlStartIn = DateHelp.convertDateToXMLGregorianCalendar(dateStartIn);
		calendarItem.setStart(xmlStartIn);
		
		Duration duration = DatatypeFactory.newInstance().newDuration(1000 * 60 * 60);
		XMLGregorianCalendar end = calendarItem.getStart();
		end.add(duration);
		calendarItem.setEnd(end);
		
		log.info("created calendar item with start="+calendarItem.getStart());
		
		Calendar calendar = eventConverter.convertToCalendar(Collections.singleton(calendarItem), null);
		
		//calendar should not be null
		assertNotNull(calendar);
		ComponentList components = calendar.getComponents();
		//calendar should have components
		assertNotNull(components);
		
		//calendar should have exactly one component
		assertEquals(1, components.size());
		
		//components should be events
		assertEquals(components, calendar.getComponents(VEvent.VEVENT));
		
		Object object = components.get(0);
		assertNotNull(object);
		assertTrue(object instanceof VEvent);
		
		VEvent event =(VEvent) object;
		assertNotNull(event);
		
		DtStart dtStart = event.getStartDate();
		assertNotNull(dtStart);
		net.fortuna.ical4j.model.Date dateStartOut = dtStart.getDate();
		assertNotNull(dateStartOut);
		XMLGregorianCalendar xmlStartOut = DateHelp.convertDateToXMLGregorianCalendar(dateStartOut);
		
		log.info("dateStartIn="+dateStartIn);
		log.info("xmlStartIn="+xmlStartIn);
		log.info("dateStartOut="+dateStartOut);
		log.info("xmlStartOut+="+xmlStartOut);
		
		assertEquals(dateStartIn, new Date(dateStartIn.getTime()));
		assertEquals(xmlStartIn, xmlStartOut);
	}

	@Test
	public void convertResponseTypeTypeToPartStat(){
		for(ResponseTypeType rtt : ResponseTypeType.values()){
			PartStat partStat = ExchangeEventConverterImpl.convertResponseTypeTypeToPartStat(rtt);
			assertNotNull(partStat);
			log.info(rtt +" ==> "+partStat.getValue());
		}
	}
	
	@Test
	public void convertNullToPartStat(){
		ResponseTypeType rtt = null;
		PartStat partStat = ExchangeEventConverterImpl.convertResponseTypeTypeToPartStat(rtt);
		assertNotNull(partStat);
		assertEquals(PartStat.NEEDS_ACTION, partStat);
		log.info(rtt +" ==> "+partStat.getValue());
	}
	
	@Test
	public void convertPartStatToResponseType(){
		Set<PartStat> partStats = new HashSet<PartStat>();
		
		partStats.add(PartStat.ACCEPTED);
		partStats.add(PartStat.COMPLETED);
		partStats.add(PartStat.DECLINED);
		partStats.add(PartStat.DELEGATED);
		partStats.add(PartStat.IN_PROCESS);
		partStats.add(PartStat.NEEDS_ACTION);
		partStats.add(PartStat.TENTATIVE);
		partStats.add(null);
		
		for(PartStat ps : partStats){
			ResponseTypeType responseType = ExchangeEventConverterImpl.convertPartStatToResponseTypeType(ps);
			assertNotNull(responseType);
			if(ps == null){
				assertEquals(ResponseTypeType.UNKNOWN, responseType);
			}else if(ps.equals(PartStat.ACCEPTED)){
				assertEquals(ResponseTypeType.ACCEPT, responseType);
			}else if(ps.equals(PartStat.DECLINED)){
				assertEquals(ResponseTypeType.DECLINE, responseType);
			}else if(ps.equals(PartStat.TENTATIVE)){
				assertEquals(ResponseTypeType.TENTATIVE, responseType);
			}else if(ps.equals(PartStat.NEEDS_ACTION)){
				assertEquals(ResponseTypeType.NO_RESPONSE_RECEIVED, responseType);
			}else{
				assertEquals(ResponseTypeType.UNKNOWN, responseType);
			}
			log.info(ps + " ==> " + responseType);
		}
	}
	
	@Test
	public void convertSensitivityToClazz(){
		for(SensitivityChoicesType sct : SensitivityChoicesType.values()){
			Clazz clazz = ExchangeEventConverterImpl.convertSensitivityToClazz(sct);
			assertNotNull(clazz);
			log.info(sct+" ==> "+clazz.getValue());
		}
	}
	
	@Test
	public void convertNullToClazz(){
		SensitivityChoicesType sct = null;
		Clazz clazz = ExchangeEventConverterImpl.convertSensitivityToClazz(sct);
		assertNotNull(clazz);
		assertEquals(Clazz.PRIVATE, clazz);
		log.info(sct+" ==> "+clazz.getValue());
	}
	
	@Test
	public void convertClazzToSensitivity(){
		Set<Clazz> clazzSet = new HashSet<Clazz>();
		clazzSet.add(Clazz.CONFIDENTIAL);
		clazzSet.add(Clazz.PRIVATE);
		clazzSet.add(Clazz.PUBLIC);
		clazzSet.add(null);
		
		for(Clazz c : clazzSet){
			SensitivityChoicesType sensitivity = ExchangeEventConverterImpl.convertClazzToSensitivityChoicesType(c);
			assertNotNull(sensitivity);
			if(c == null){
				assertEquals(SensitivityChoicesType.PRIVATE, sensitivity);
			}else if(c.equals(Clazz.CONFIDENTIAL)){
				assertEquals(SensitivityChoicesType.CONFIDENTIAL, sensitivity);
			}else if(c.equals(Clazz.PUBLIC)){
				assertEquals(SensitivityChoicesType.NORMAL, sensitivity);
			}else{
				assertEquals(SensitivityChoicesType.PRIVATE, sensitivity);
			}
			log.info(c +" ==> "+ sensitivity);
		}
	}
	
	@Test
	public void convertImportanceChoicesTypeToPriority(){
		for(ImportanceChoicesType ict : ImportanceChoicesType.values()){
			Priority priority = ExchangeEventConverterImpl.convertImportanceChoicesTypeToPriority(ict);
			assertNotNull(priority);
			if(ict.equals(ImportanceChoicesType.HIGH)){
				assertEquals(Priority.HIGH,priority);
			}else if(ict.equals(ImportanceChoicesType.LOW)){
				assertEquals(Priority.LOW,priority);
			}else {
				assertEquals(Priority.MEDIUM,priority);
			}
			log.info(ict +" ==> "+priority.getValue());
			
		}
	}
	
	@Test
	public void convertPriorityToImportanceChoicesType(){
		Set<Priority> prioritySet = new HashSet<Priority>();
		prioritySet.add(Priority.HIGH);
		prioritySet.add(Priority.MEDIUM);
		prioritySet.add(Priority.LOW);
		prioritySet.add(Priority.UNDEFINED);
		prioritySet.add(null);
		
		for(Priority p : prioritySet){
			ImportanceChoicesType importance = ExchangeEventConverterImpl.convertPriorityToImportanceChoicesType(p);
			assertNotNull(importance);
			if(p == null){
				assertEquals(ImportanceChoicesType.NORMAL, importance);
			}else if(p.equals(Priority.HIGH)){
				assertEquals(ImportanceChoicesType.HIGH, importance);
			}else if(p.equals(Priority.LOW)){
				assertEquals(ImportanceChoicesType.LOW, importance);
			}else{
				assertEquals(ImportanceChoicesType.NORMAL, importance);
			}
			
			log.info(p + " ==> "+importance);
		}
		
	}
	
}
