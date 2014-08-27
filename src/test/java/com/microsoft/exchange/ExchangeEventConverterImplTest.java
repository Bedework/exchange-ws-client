package com.microsoft.exchange;

import static org.junit.Assert.*;

import java.util.HashSet;
import java.util.Set;

import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.ComponentList;
import net.fortuna.ical4j.model.parameter.PartStat;
import net.fortuna.ical4j.model.property.Clazz;
import net.fortuna.ical4j.model.property.Priority;
import net.fortuna.ical4j.model.property.ProdId;
import net.fortuna.ical4j.model.property.Version;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Test;
import org.slf4j.IMarkerFactory;

import com.microsoft.exchange.impl.ExchangeEventConverterImpl;
import com.microsoft.exchange.types.ImportanceChoicesType;
import com.microsoft.exchange.types.ResponseTypeType;
import com.microsoft.exchange.types.SensitivityChoicesType;

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
