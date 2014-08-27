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
package com.microsoft.exchange.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.bind.JAXBContext;

import net.fortuna.ical4j.model.Calendar;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.apache.commons.lang.time.StopWatch;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.validator.routines.EmailValidator;
import org.joda.time.Interval;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.support.DataAccessUtils;
import org.springframework.util.CollectionUtils;

import com.microsoft.exchange.DateHelp;
import com.microsoft.exchange.ExchangeRequestFactory;
import com.microsoft.exchange.ExchangeResponseUtils;
import com.microsoft.exchange.ExchangeResponseUtilsImpl;
import com.microsoft.exchange.ExchangeWebServices;
import com.microsoft.exchange.exception.ExchangeExceededFindCountLimitRuntimeException;
import com.microsoft.exchange.exception.ExchangeInvalidUPNRuntimeException;
import com.microsoft.exchange.exception.ExchangeRuntimeException;
import com.microsoft.exchange.messages.CreateFolder;
import com.microsoft.exchange.messages.CreateFolderResponse;
import com.microsoft.exchange.messages.CreateItem;
import com.microsoft.exchange.messages.CreateItemResponse;
import com.microsoft.exchange.messages.DeleteFolder;
import com.microsoft.exchange.messages.DeleteFolderResponse;
import com.microsoft.exchange.messages.DeleteItem;
import com.microsoft.exchange.messages.DeleteItemResponse;
import com.microsoft.exchange.messages.EmptyFolderResponse;
import com.microsoft.exchange.messages.FindFolder;
import com.microsoft.exchange.messages.FindFolderResponse;
import com.microsoft.exchange.messages.FindItem;
import com.microsoft.exchange.messages.FindItemResponse;
import com.microsoft.exchange.messages.GetFolder;
import com.microsoft.exchange.messages.GetFolderResponse;
import com.microsoft.exchange.messages.GetItem;
import com.microsoft.exchange.messages.GetItemResponse;
import com.microsoft.exchange.messages.GetServerTimeZones;
import com.microsoft.exchange.messages.GetServerTimeZonesResponse;
import com.microsoft.exchange.messages.ResolveNames;
import com.microsoft.exchange.messages.ResolveNamesResponse;
import com.microsoft.exchange.types.BaseFolderIdType;
import com.microsoft.exchange.types.BaseFolderType;
import com.microsoft.exchange.types.CalendarFolderType;
import com.microsoft.exchange.types.CalendarItemCreateOrDeleteOperationType;
import com.microsoft.exchange.types.CalendarItemType;
import com.microsoft.exchange.types.ConnectingSIDType;
import com.microsoft.exchange.types.DefaultShapeNamesType;
import com.microsoft.exchange.types.DisposalType;
import com.microsoft.exchange.types.DistinguishedFolderIdNameType;
import com.microsoft.exchange.types.ExtendedPropertyType;
import com.microsoft.exchange.types.FolderIdType;
import com.microsoft.exchange.types.FolderQueryTraversalType;
import com.microsoft.exchange.types.ItemIdType;
import com.microsoft.exchange.types.ItemType;
import com.microsoft.exchange.types.TasksFolderType;
import com.microsoft.exchange.types.TimeZoneDefinitionType;
import com.microsoft.exchange.messages.EmptyFolder;


/**
 * TODO - what is the intent for this class? Does it belong within the client?
 * If so, is the intent that it be a base class for interacting with {@link ExchangeWebServices}?
 * Should it be abstract? 
 * 
 * @author Collin Cudd
 */
public class BaseExchangeCalendarDataDao {

	protected final Log log = LogFactory.getLog(this.getClass());
	
	private JAXBContext jaxbContext;
	private ExchangeWebServices webServices;
	
	// TODO no references, needed internally?
	private ExchangeRequestFactory requestFactory = new ExchangeRequestFactory();
	// TODO no references, needed internally?
	private ExchangeResponseUtils responseUtils = new ExchangeResponseUtilsImpl();

	private int maxRetries = 10;
	
	@Value("${username}")
	private String adminUsername;
	
	public int getMaxRetries() {
		return maxRetries;
	}
	public void setMaxRetries(int maxRetries) {
		this.maxRetries = maxRetries;
	}
	public ExchangeWebServices getWebServices() {
		return webServices;
	}
	/**
	 * @param exchangeWebServices the exchangeWebServices to set
	 */
	@Autowired @Qualifier("ewsClient")
	public void setWebServices(ExchangeWebServices exchangeWebServices) {
		this.webServices = exchangeWebServices;
	}

	/**
	 * @return the requestFactory
	 */
	public ExchangeRequestFactory getRequestFactory() {
		return requestFactory;
	}
	/**
	 * @param requestFactory the requestFactory to set
	 */
	@Autowired(required=false)
	public void setRequestFactory(ExchangeRequestFactory exchangeRequestFactory) {
		this.requestFactory = exchangeRequestFactory;
	}
	
	public ExchangeResponseUtils getResponseUtils() {
		return responseUtils;
	}
	public void setResponseUtils(ExchangeResponseUtils exchangeResponseUtils) {
		this.responseUtils = exchangeResponseUtils;
	}
	
	/**
	 * @return the jaxbContext
	 */
	public JAXBContext getJaxbContext() {
		return jaxbContext;
	}
	/**
	 * @param jaxbContext the jaxbContext to set
	 */
	@Autowired
	public void setJaxbContext(JAXBContext jaxbContext) {
		this.jaxbContext = jaxbContext;
	}
	
	public static long getWaitTimeExp(int retryCount) {
		long waitTime = ((long) Math.pow(2, retryCount) * 100L);
	    return waitTime;
	}
	protected void setContextCredentials(String upn) {
		Validate.isTrue(StringUtils.isNotBlank(upn), "upn argument cannot be blank");
		ConnectingSIDType connectingSID = new ConnectingSIDType();
		connectingSID.setPrincipalName(upn);
		ThreadLocalImpersonationConnectingSIDSourceImpl.setConnectingSID(connectingSID);
	}
	
	public CalendarFolderType getCalendarFolder(String upn, FolderIdType folderId) {
		BaseFolderType folder = getFolder(upn, folderId);
		CalendarFolderType calendarFolderType = null;
		if(folder instanceof CalendarFolderType) {
			calendarFolderType=  (CalendarFolderType) folder;
		}
		return calendarFolderType;
	}
	
	public TasksFolderType getTaskFolder(String upn, FolderIdType folderId) {
		BaseFolderType folder = getFolder(upn, folderId);
		TasksFolderType taskFolderType = null;
		if(folder instanceof TasksFolderType) {
			taskFolderType =  (TasksFolderType) folder;
		}
		return taskFolderType;
	}
	
	protected BaseFolderType getFolder(String upn, FolderIdType folderIdType){
		setContextCredentials(upn);
		GetFolder getFolderRequest = getRequestFactory().constructGetFolderById(folderIdType);
		GetFolderResponse getFolderResponse = getWebServices().getFolder(getFolderRequest);
		Set<BaseFolderType> response = getResponseUtils().parseGetFolderResponse(getFolderResponse);
		return DataAccessUtils.singleResult(response);
	}
	
	protected BaseFolderType getPrimaryFolder(String upn, DistinguishedFolderIdNameType parent) {
		setContextCredentials(upn);
		GetFolder getFolderRequest = getRequestFactory().constructGetFolderByName(parent);
		GetFolderResponse getFolderResponse = getWebServices().getFolder(getFolderRequest);
		Set<BaseFolderType> response = getResponseUtils().parseGetFolderResponse(getFolderResponse);
		return DataAccessUtils.singleResult(response);
	}
	
	public BaseFolderType getPrimaryCalendarFolder(String upn){
		return getPrimaryFolder(upn, DistinguishedFolderIdNameType.CALENDAR);		
	}
	
	public BaseFolderType getPrimaryTaskFolder(String upn) {
		return getPrimaryFolder(upn, DistinguishedFolderIdNameType.TASKS);
	}
	
	private List<BaseFolderType> getFoldersByType(String upn, DistinguishedFolderIdNameType parent){
		List<BaseFolderType> folders = new ArrayList<BaseFolderType>();
		BaseFolderType baseFolderType = getPrimaryFolder(upn, parent);
		if(null != baseFolderType) {
			folders.add(baseFolderType);
		}
		List<BaseFolderType> seondaryFolders = getSeondaryFolders(upn, parent);
		if(!CollectionUtils.isEmpty(seondaryFolders)) {
			for(BaseFolderType b: seondaryFolders ) {
				if(baseFolderType.getClass().equals(b.getClass())) {
					folders.add(b);
				}
			}
		}
		return folders;
	}
	
	/**
	 * Gets all secondary folders for given DistinguisheFolderName
	 * @param upn
	 * @param parent
	 * @return
	 */
	private List<BaseFolderType> getSeondaryFolders(String upn, DistinguishedFolderIdNameType parent) {
		Validate.notEmpty(upn, "upn cannnot be empty");
		setContextCredentials(upn);
		FindFolder findFolderRequest = getRequestFactory().constructFindFolder(parent, DefaultShapeNamesType.ALL_PROPERTIES, FolderQueryTraversalType.DEEP);
		FindFolderResponse findFolderResponse = getWebServices().findFolder(findFolderRequest);
		return getResponseUtils().parseFindFolderResponse(findFolderResponse);
	}
	/*
	 * return all calendar folders
	 */
	public List<BaseFolderType> getAllCalendarFolders(String upn) {
		Validate.notEmpty(upn, "upn cannnot be empty");
		return getFoldersByType(upn, DistinguishedFolderIdNameType.CALENDAR);
	}
	
	public List<BaseFolderType> getAllTaskFolders(String upn) {
		Validate.notEmpty(upn, "upn cannnot be empty");
		return getFoldersByType(upn, DistinguishedFolderIdNameType.TASKS);
	}
	
	public FolderIdType getCalendarFolderId(String upn, String calendarName) {
		Map<String, String> calendarFolderMap = getCalendarFolderMap(upn);
		if(!CollectionUtils.isEmpty(calendarFolderMap) && calendarFolderMap.containsValue(calendarName)) {
			for(String c_id: calendarFolderMap.keySet()) {
				String c_name = calendarFolderMap.get(c_id);
				if(calendarName.equals(c_name)) {
					FolderIdType folderIdType = new FolderIdType();
					folderIdType.setId(c_id);
					return folderIdType;
				}
			}
		}
		throw new ExchangeRuntimeException("No calendar folder with name of '"+calendarName+"' for "+upn); 
	}

	public Map<String, String> getCalendarFolderMap(String upn){
		Map<String, String> calendarsMap = new HashMap<String, String>();
		List<BaseFolderType> allCalendarFolders = getAllCalendarFolders(upn);
		for(BaseFolderType folderType: allCalendarFolders) {
			String name = folderType.getDisplayName();
			String id = folderType.getFolderId().getId();
			calendarsMap.put(id, name);
		}
		return calendarsMap;
	}
	
	public Map<String, String> getTaskFolderMap(String upn){
		Map<String, String> taskFolderMap = new HashMap<String, String>();
		List<BaseFolderType> allTaskFolders = getAllTaskFolders(upn);
		for(BaseFolderType b : allTaskFolders) {
			String displayName = b.getDisplayName();
			String id = b.getFolderId().getId();
			taskFolderMap.put( id,displayName);
		}
		return taskFolderMap;
	}
	
	public Set<ItemIdType> findCalendarItemIds(String upn, Date startDate, Date endDate){
		return findCalendarItemIdsInternal(upn, startDate, endDate, null, 0);
	}
	public Set<ItemIdType> findCalendarItemIds(String upn, Date startDate, Date endDate, Collection<FolderIdType> calendarIds) {
		return findCalendarItemIdsInternal(upn, startDate, endDate, calendarIds, 0);
	}
	
	
	
	
	/**
	 * This method uses a CalendarView...
	 * 
	 * The FindItem operation can return results in a CalendarView element. The
	 * CalendarView element returns single calendar items and all occurrences.
	 * If a CalendarView element is not used, single calendar items and
	 * recurring master calendar items are returned. The occurrences must be
	 * expanded from the recurring master if a CalendarView element is not used.
	 * 
	 * -- http://msdn.microsoft.com/en-us/library/office/aa566107(v=exchg.140).
	 * aspx
	 * 
	 * @param upn
	 * @param startDate
	 * @param endDate
	 * @param calendarIds
	 * @param depth
	 * @return
	 */
	private Set<ItemIdType> findCalendarItemIdsInternal(String upn, Date startDate, Date endDate, Collection<FolderIdType> calendarIds, int depth){
		Validate.isTrue(StringUtils.isNotBlank(upn), "upn argument cannot be blank");
		Validate.notNull(startDate, "startDate argument cannot be null");
		Validate.notNull(endDate, "endDate argument cannot be null");
		
		//folderIds can be null

		int newDepth = depth +1;
		if(depth > getMaxRetries()) {
			throw new ExchangeRuntimeException("findCalendarItemIdsInternal(upn="+upn+",startDate="+startDate+",+endDate="+endDate+",...) failed "+getMaxRetries()+ " consecutive attempts.");
		}else {
			setContextCredentials(upn);
			FindItem request = getRequestFactory().constructFindCalendarItemIdsByDateRange(startDate, endDate, calendarIds);
			try {
				FindItemResponse response = getWebServices().findItem(request);
				return getResponseUtils().parseFindItemIdResponseNoOffset(response);
				
			}catch(ExchangeInvalidUPNRuntimeException e0) {
				log.warn("findCalendarItemIdsInternal(upn="+upn+",startDate="+startDate+",+endDate="+endDate+",...) ExchangeInvalidUPNRuntimeException.  Attempting to resolve valid upn... - failure #"+newDepth);
				
				String resolvedUpn = resolveUpn(upn);
				if(StringUtils.isNotBlank(resolvedUpn) && (!resolvedUpn.equalsIgnoreCase(upn))){
					return findCalendarItemIdsInternal(resolvedUpn, startDate, endDate, calendarIds, newDepth);
				}else {
					//rethrow
					throw e0;
				}
			}catch(ExchangeExceededFindCountLimitRuntimeException e1) {
				log.warn("findCalendarItemIdsInternal(upn="+upn+",startDate="+startDate+",+endDate="+endDate+",...) ExceededFindCountLimit splitting request and trying again. - failure #"+newDepth);
				Set<ItemIdType> foundItems = new HashSet<ItemIdType>();
				List<Interval> intervals = DateHelp.generateIntervals(startDate, endDate);
				for(Interval i: intervals) {
					foundItems.addAll(findCalendarItemIdsInternal(upn,i.getStart().toDate(), i.getEnd().toDate(),calendarIds,newDepth));
				}
				return foundItems;
			}catch(Exception e2) {
				long backoff = getWaitTimeExp(newDepth);
				log.warn("findCalendarItemIdsInternal(upn="+upn+",startDate="+startDate+",+endDate="+endDate+",...) - failure #"+newDepth+". Sleeping for "+backoff+" before retry. " +e2.getMessage());
				try {
					Thread.sleep(backoff);
				} catch (InterruptedException e1) {
					log.warn("InterruptedException="+e1);
				}
				return findCalendarItemIdsInternal(upn, startDate, endDate, calendarIds, newDepth);
			}
		}
	}
	
	public Set<ItemIdType> findindFirstItemIdSet(String upn, Collection<FolderIdType> folderIds){
		FindItem request = getRequestFactory().constructFindFirstItemIdSet(folderIds);
		Pair<Set<ItemIdType>, Integer> pair =  findItemIdsInternal(upn, request, 0);
		return pair.getLeft();
	}
	
	
	public Set<ItemIdType> findItemIds(String upn, Collection<FolderIdType> folderIds){
		FindItem request = getRequestFactory().constructFindFirstItemIdSet(folderIds);
		Pair<Set<ItemIdType>, Integer> pair = findItemIdsInternal(upn, request, 0);
		return pair.getLeft();
	}
	
	public Set<ItemIdType> findAllItemIds(String upn, Collection<FolderIdType> folderIds){
		FindItem request = getRequestFactory().constructFindFirstItemIdSet(folderIds);
		Pair<Set<ItemIdType>, Integer> pair = findItemIdsInternal(upn, request, 0);
		Set<ItemIdType> itemIds = pair.getLeft();
		Integer nextOffset = pair.getRight();
		while(nextOffset > 0){
			request = getRequestFactory().constructFindNextItemIdSet(nextOffset, folderIds);
			pair = findItemIdsInternal(upn, request, 0);
			itemIds.addAll(pair.getLeft());
			nextOffset = pair.getRight();
		}
		return itemIds;
	}

	
	
	private Pair<Set<ItemIdType>, Integer> findItemIdsInternal(String upn, FindItem request, int depth){
		Validate.isTrue(StringUtils.isNotBlank(upn), "upn argument cannot be blank");
		Validate.notNull(request, "request argument cannot be null");
		int newDepth = depth +1;
		if(depth > getMaxRetries()) {
			throw new ExchangeRuntimeException("findCalendarItemIdsInternal(upn="+upn+",request="+request+",...) failed "+getMaxRetries()+ " consecutive attempts.");
		}else {
			setContextCredentials(upn);
			try {
				FindItemResponse response = getWebServices().findItem(request);
				Pair<Set<ItemIdType>, Integer> parsed = getResponseUtils().parseFindItemIdResponse(response);

				return parsed;
			}catch(ExchangeInvalidUPNRuntimeException e0) {
				log.warn("findCalendarItemIdsInternal(upn="+upn+",request="+request+",...) ExchangeInvalidUPNRuntimeException.  Attempting to resolve valid upn... - failure #"+newDepth);
				
				String resolvedUpn = resolveUpn(upn);
				if(StringUtils.isNotBlank(resolvedUpn) && (!resolvedUpn.equalsIgnoreCase(upn))){
					return findItemIdsInternal(resolvedUpn, request, newDepth);
				}else {
					//rethrow
					throw e0;
				}
//			}catch(ExchangeExceededFindCountLimitRuntimeException e1) {
//				log.warn("findCalendarItemIdsInternal(upn="+upn+",request="+request+",...) ExceededFindCountLimit splitting request and trying again. - failure #"+newDepth);
//				Set<ItemIdType> foundItems = new HashSet<ItemIdType>();
//				List<Interval> intervals = DateHelp.generateIntervals(startDate, endDate);
//				for(Interval i: intervals) {
//					foundItems.addAll(findCalendarItemIdsInternal(upn,i.getStart().toDate(), i.getEnd().toDate(),calendarIds,newDepth));
//				}
//				return foundItems;
			}catch(Exception e2) {
				long backoff = getWaitTimeExp(newDepth);
				log.warn("findCalendarItemIdsInternal(upn="+upn+",request="+request+",...) - failure #"+newDepth+". Sleeping for "+backoff+" before retry. " +e2.getMessage());
				try {
					Thread.sleep(backoff);
				} catch (InterruptedException e1) {
					log.warn("InterruptedException="+e1);
				}
				return findItemIdsInternal(upn, request, newDepth);
			}
		}
		
	}
	
	public Set<ItemType> getCalendarItems(String upn, Date startDate, Date endDate, Collection<FolderIdType> calendarIds){
		Set<ItemIdType> itemIds = findCalendarItemIds(upn, startDate, endDate, calendarIds);
		return getCalendarItems(upn, itemIds);
	}
	
	public Set<ItemType> getCalendarItems(String upn, Set<ItemIdType> itemIds) {
		return getCalendarItemsInternal(upn, itemIds, 0);
	}
	
	private Set<ItemType> getCalendarItemsInternal(String upn, Set<ItemIdType> itemIds, int depth){
		Validate.isTrue(StringUtils.isNotBlank(upn), "upn argument cannot be blank");
		Validate.notEmpty(itemIds, "itemids argument cannot be empty");
		
		//folderIds can be null

		int newDepth = depth +1;
		if(depth > getMaxRetries()) {
			throw new ExchangeRuntimeException("getCalendarItemsInternal(upn="+upn+",...) failed "+getMaxRetries()+ " consecutive attempts.");
		}else {
			setContextCredentials(upn);
			GetItem request = getRequestFactory().constructGetItems(itemIds);	
			try {
				GetItemResponse response = getWebServices().getItem(request);
				return getResponseUtils().parseGetItemResponse(response);
			}catch(Exception e) {
				long backoff = getWaitTimeExp(newDepth);
				log.warn("getCalendarItemsInternal - failure #"+newDepth+". Sleeping for "+backoff+" before retry. " +e.getMessage());
				try {
					Thread.sleep(backoff);
				} catch (InterruptedException e1) {
					log.warn("InterruptedException="+e1);
				}
				return getCalendarItemsInternal(upn, itemIds, newDepth);
			}
		}
	}
	
	private ItemIdType createCalendarItemInternal(String upn, CalendarItemType calendarItem, int depth){
		Validate.isTrue(StringUtils.isNotBlank(upn), "upn argument cannot be blank");
		Validate.notNull(calendarItem, "calendarItem argument cannot be empty");
	
		int newDepth = depth +1;
		if(depth > getMaxRetries()) {
			throw new ExchangeRuntimeException("createCalendarItemInternal(upn="+upn+",...) failed "+getMaxRetries()+ " consecutive attempts.");
		}else {
			setContextCredentials(upn);
			
			Set<CalendarItemType> singleton = Collections.singleton(calendarItem);
			CalendarItemCreateOrDeleteOperationType sendTo = CalendarItemCreateOrDeleteOperationType.SEND_TO_ALL_AND_SAVE_COPY;
			CreateItem request = getRequestFactory().constructCreateCalendarItem(singleton, sendTo, null);
			
			try {
				CreateItemResponse response = getWebServices().createItem(request);
				List<ItemIdType> createdCalendarItems = getResponseUtils().parseCreateItemResponse(response);
				return DataAccessUtils.singleResult(createdCalendarItems);
			}catch(Exception e) {
				long backoff = getWaitTimeExp(newDepth);
				log.warn("createCalendarItemInternal - failure #"+newDepth+". Sleeping for "+backoff+" before retry. " +e.getMessage());
				try {
					Thread.sleep(backoff);
				} catch (InterruptedException e1) {
					log.warn("InterruptedException="+e1);
				}
				return createCalendarItemInternal(upn, calendarItem, newDepth);
			}
		}
		
	}

	public ItemIdType createCalendarItem(String upn, CalendarItemType calendarItem){
		return createCalendarItemInternal(upn, calendarItem, 0);
	}
	
	public FolderIdType createCalendarFolder(String upn, String displayName) {
		setContextCredentials(upn);
		log.debug("createCalendarFolder upn="+upn+", displayName="+displayName);
		//default implementation - null for extendedProperties
		CreateFolder createCalendarFolderRequest = getRequestFactory().constructCreateCalendarFolder(displayName, null);
		CreateFolderResponse createFolderResponse = getWebServices().createFolder(createCalendarFolderRequest);
		Set<FolderIdType> folders = getResponseUtils().parseCreateFolderResponse(createFolderResponse);
		return DataAccessUtils.singleResult(folders);
	}
	
	private boolean deleteCalendarItemsInternal(String upn, Collection<ItemIdType> itemIds, int depth) {
		Validate.isTrue(StringUtils.isNotBlank(upn), "upn argument cannot be blank");
		Validate.notEmpty(itemIds, "itemIds argument cannot be empty");
	
		int newDepth = depth +1;
		if(depth > getMaxRetries()) {
			throw new ExchangeRuntimeException("createCalendarItemInternal(upn="+upn+",...) failed "+getMaxRetries()+ " consecutive attempts.");
		}else {
			setContextCredentials(upn);
			DeleteItem request = getRequestFactory().constructDeleteCalendarItems(itemIds, DisposalType.HARD_DELETE, CalendarItemCreateOrDeleteOperationType.SEND_TO_NONE);

			try {
				DeleteItemResponse response = getWebServices().deleteItem(request);
				boolean success = getResponseUtils().confirmSuccess(response);
				return success;
			
			}catch(Exception e) {
				long backoff = getWaitTimeExp(newDepth);
				log.warn("deleteCalendarItemsInternal - failure #"+newDepth+". Sleeping for "+backoff+" before retry. " +e.getMessage());
				try {
					Thread.sleep(backoff);
				} catch (InterruptedException e1) {
					log.warn("InterruptedException="+e1);
				}
				return deleteCalendarItemsInternal(upn, itemIds, newDepth);
			}
		}
	}
	
	public boolean deleteCalendarItems(String upn, Collection<ItemIdType> itemIds) {
		return deleteCalendarItemsInternal(upn, itemIds,0);
	}
	
	public Set<String> resolveEmailAddresses(String alias) {
		Validate.isTrue(StringUtils.isNotBlank(alias), "alias argument cannot be blank");
		setContextCredentials(adminUsername);
		ResolveNames request = getRequestFactory().constructResolveNames(alias);
		ResolveNamesResponse response = getWebServices().resolveNames(request);
		return getResponseUtils().parseResolveNamesResponse(response);
		
	}
	
	public String resolveUpn(String emailAddress) {
		Validate.isTrue(StringUtils.isNotBlank(emailAddress),"emailAddress argument cannot be blank");
		Validate.isTrue(EmailValidator.getInstance().isValid(emailAddress),"emailAddress argument must be valid");
		
		emailAddress = "smtp:"+emailAddress;
		
		Set<String> results = new HashSet<String>();
		Set<String> addresses = resolveEmailAddresses(emailAddress);
		for(String addr: addresses) {
			try {
				BaseFolderType primaryCalendarFolder = getPrimaryCalendarFolder(addr);
				if(null == primaryCalendarFolder) {
					throw new ExchangeRuntimeException("CALENDAR NOT FOUND");
				}else {
					results.add(addr);
				}
			}catch(Exception e) {
				log.warn("resolveUpn -- "+addr+" NOT VALID. "+e.getMessage());
			}
		}
		if(CollectionUtils.isEmpty(results)) {
			throw new ExchangeRuntimeException("resolveUpn("+emailAddress+") failed -- no results.");
		}else {
			if(results.size() >1) {
				throw new ExchangeRuntimeException("resolveUpn("+emailAddress+") failed -- multiple results.");
			}else {
				return DataAccessUtils.singleResult(results);
			}
		}
	}
	
	
	public List<TimeZoneDefinitionType> getServerTimeZones(String tzid, boolean fullTimeZoneData){
		GetServerTimeZones request = getRequestFactory().constructGetServerTimeZones(tzid, fullTimeZoneData);
		setContextCredentials(adminUsername);
		GetServerTimeZonesResponse response = getWebServices().getServerTimeZones(request);
		return getResponseUtils().parseGetServerTimeZonesResponse(response);
	}
	/**
	 * The EmptyFolder operation empties folders in a mailbox. 
	 * Optionally, this operation enables you to delete the subfolders of the specified folder. 
	 * When a subfolder is deleted, the subfolder and the messages within the subfolder are deleted. 
	 * 
	 * *Note this method does not work for calendar or search folders: ERROR_CANNOT_EMPTY_FOLDER ... Emptying the calendar folder or search folder isn't permitted.
	 * 
	 * 
	 * @param upn
	 * @param folderId
	 * @return
	 */
	public boolean emptyFolder(String upn, boolean deleteSubFolders, DisposalType disposalType, BaseFolderIdType folderId){
		EmptyFolder request = getRequestFactory().constructEmptyFolder(deleteSubFolders, disposalType, Collections.singleton(folderId));
		setContextCredentials(upn);
		EmptyFolderResponse response = getWebServices().emptyFolder(request);
		return getResponseUtils().parseEmptyFolderResponse(response);
	}
	/**
	 * Deleting a calendarFolder with many (1k+) items is a problem.  You will always be throttled because the FindItemCount is 1000 and not configurable in Exchange Online.
	 * More info on throttling http://msdn.microsoft.com/en-us/library/office/jj945066(v=exchg.150).aspx
	 * 
	 * This method will never attempt to delete more than 500 items at once.
	 * 
	 * @param upn
	 * @param folderId
	 * @return
	 */
	public boolean emptyCalendarFolder(String upn, FolderIdType folderId){
		Integer deleteRequestCount =1;
		Set<ItemIdType> itemIds = findindFirstItemIdSet(upn, Collections.singleton(folderId));
		while(!itemIds.isEmpty()){
			List<ItemIdType> itemIdList = new ArrayList<ItemIdType>(itemIds);
			if(itemIdList.size() > 500){
				itemIdList = itemIdList.subList(0, 500);
			}
			StopWatch stopWatch = new StopWatch();
			stopWatch.start();
			log.info("emptyCalendarFolder(upn="+upn+") #"+deleteRequestCount+" deleting "+itemIdList.size()+ " calendar items");
			boolean result = deleteCalendarItems(upn, itemIdList);
			log.info("emptyCalendarFolder(upn="+upn+") #"+deleteRequestCount+" "+(result ? "Success" : "Failure")+" in "+stopWatch);
			itemIds = findindFirstItemIdSet(upn, Collections.singleton(folderId));
			deleteRequestCount++;
		}
		return true;
	}
	
	public boolean deleteCalendarFolder(String upn, FolderIdType folderId){
		boolean empty = emptyCalendarFolder(upn, folderId);
		if(empty){
			return deleteFolder(upn, DisposalType.SOFT_DELETE, folderId);					
		}
		return false;
	}
	
	public boolean deleteFolder(String upn, DisposalType disposalType, BaseFolderIdType folderId){
		DeleteFolder request = getRequestFactory().constructDeleteFolder(folderId, disposalType);
		setContextCredentials(upn);
		DeleteFolderResponse response = getWebServices().deleteFolder(request);
		return getResponseUtils().parseDeleteFolderResponse(response);
				
	}
}
