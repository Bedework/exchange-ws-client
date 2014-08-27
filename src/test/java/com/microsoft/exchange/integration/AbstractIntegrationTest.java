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

package com.microsoft.exchange.integration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import java.io.ByteArrayOutputStream;
import java.util.Date;
import java.util.List;
import java.util.Set;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.datatype.XMLGregorianCalendar;

import org.apache.commons.lang.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.support.DataAccessUtils;
import org.springframework.util.CollectionUtils;

import com.microsoft.exchange.DateHelp;
import com.microsoft.exchange.ExchangeRequestFactory;
import com.microsoft.exchange.ExchangeResponseUtils;
import com.microsoft.exchange.ExchangeResponseUtilsImpl;
import com.microsoft.exchange.ExchangeWebServices;
import com.microsoft.exchange.impl.ExchangeOnlineThrottlingPolicy;
import com.microsoft.exchange.impl.ExchangeWebServicesClient;
import com.microsoft.exchange.messages.ArrayOfResponseMessagesType;
import com.microsoft.exchange.messages.CreateItem;
import com.microsoft.exchange.messages.CreateItemResponse;
import com.microsoft.exchange.messages.DeleteItem;
import com.microsoft.exchange.messages.DeleteItemResponse;
import com.microsoft.exchange.messages.FindFolder;
import com.microsoft.exchange.messages.FindFolderResponse;
import com.microsoft.exchange.messages.FindItem;
import com.microsoft.exchange.messages.GetFolder;
import com.microsoft.exchange.messages.GetFolderResponse;
import com.microsoft.exchange.messages.GetUserAvailabilityRequest;
import com.microsoft.exchange.messages.ItemInfoResponseMessageType;
import com.microsoft.exchange.messages.ResponseCodeType;
import com.microsoft.exchange.messages.ResponseMessageType;
import com.microsoft.exchange.messages.UpdateItem;
import com.microsoft.exchange.messages.UpdateItemResponse;
import com.microsoft.exchange.types.ArrayOfMailboxData;
import com.microsoft.exchange.types.ArrayOfRealItemsType;
import com.microsoft.exchange.types.BaseFolderType;
import com.microsoft.exchange.types.BasePathToElementType;
import com.microsoft.exchange.types.BodyType;
import com.microsoft.exchange.types.BodyTypeType;
import com.microsoft.exchange.types.CalendarItemCreateOrDeleteOperationType;
import com.microsoft.exchange.types.CalendarItemType;
import com.microsoft.exchange.types.CalendarItemUpdateOperationType;
import com.microsoft.exchange.types.CalendarViewType;
import com.microsoft.exchange.types.ConflictResolutionType;
import com.microsoft.exchange.types.DayOfWeekType;
import com.microsoft.exchange.types.DefaultShapeNamesType;
import com.microsoft.exchange.types.DisposalType;
import com.microsoft.exchange.types.DistinguishedFolderIdNameType;
import com.microsoft.exchange.types.DistinguishedFolderIdType;
import com.microsoft.exchange.types.Duration;
import com.microsoft.exchange.types.EmailAddressType;
import com.microsoft.exchange.types.FolderQueryTraversalType;
import com.microsoft.exchange.types.FolderResponseShapeType;
import com.microsoft.exchange.types.FreeBusyViewOptions;
import com.microsoft.exchange.types.IndexBasePointType;
import com.microsoft.exchange.types.IndexedPageViewType;
import com.microsoft.exchange.types.ItemChangeType;
import com.microsoft.exchange.types.ItemQueryTraversalType;
import com.microsoft.exchange.types.ItemResponseShapeType;
import com.microsoft.exchange.types.ItemType;
import com.microsoft.exchange.types.Mailbox;
import com.microsoft.exchange.types.MailboxData;
import com.microsoft.exchange.types.MeetingAttendeeType;
import com.microsoft.exchange.types.NonEmptyArrayOfAllItemsType;
import com.microsoft.exchange.types.NonEmptyArrayOfBaseFolderIdsType;
import com.microsoft.exchange.types.NonEmptyArrayOfBaseItemIdsType;
import com.microsoft.exchange.types.NonEmptyArrayOfItemChangeDescriptionsType;
import com.microsoft.exchange.types.NonEmptyArrayOfItemChangesType;
import com.microsoft.exchange.types.ObjectFactory;
import com.microsoft.exchange.types.PathToUnindexedFieldType;
import com.microsoft.exchange.types.SerializableTimeZoneTime;
import com.microsoft.exchange.types.SetItemFieldType;
import com.microsoft.exchange.types.TargetFolderIdType;
import com.microsoft.exchange.types.TimeZone;
import com.microsoft.exchange.types.UnindexedFieldURIType;

/**
 * All integration tests depend on creation of an exchange.properties file in src/test/resources.
 * Your local exchange.properties should also include the following properties:
 * 
 * <pre>
   integrationtest.email=someemailaddress@on.yourexchangeserver.edu
   integrationtest.startDate=2014-07-01
   integrationtest.endDate=2014-07-31
   </pre>
 * 
 * See src/main/resources/exchange-SAMPLE.properties for a reference copy.
 * 
 * @author Nicholas Blair
 */
public abstract class AbstractIntegrationTest {

	protected final Log log = LogFactory.getLog(this.getClass());

	@Autowired
	protected ExchangeWebServicesClient ewsClient;
	
	@Autowired
	protected JAXBContext jaxbContext;

	protected ExchangeResponseUtils responseUtils = new ExchangeResponseUtilsImpl();
	protected ExchangeRequestFactory requestFactory = new ExchangeRequestFactory();
	
	@Value("${integrationtest.email}")
	protected String emailAddress = "someemailaddress@on.yourexchangeserver.edu";
	@Value("${integrationtest.startDate}")
	protected String startDate = "2012-10-11";
	@Value("${integrationtest.endDate}")
	protected String endDate = "2014-10-12";
	
	/**
	 * This method gets called at the beginning of each integration test method.
	 * The purpose is for subclasses to set the necessary credentials.
	 */
	public abstract void initializeCredentials();

	public void findFolders() {
		initializeCredentials();
		FindFolder request = constructFindFolderRequest();
		assertNotNull(request);
		
		FindFolderResponse response = ewsClient.findFolder(request);
		assertNotNull(response);
		
		List<BaseFolderType> foundFolders = responseUtils.parseFindFolderResponse(response);
		
		for(BaseFolderType folder: foundFolders ) {
			log.info(folder.getDisplayName());
		}
	}
	
	public void getPrimaryCalendarFolder() {
		initializeCredentials();
		GetFolder request = requestFactory.constructGetFolderByName(DistinguishedFolderIdNameType.CALENDAR);
		assertNotNull(request);
		
		GetFolderResponse response = ewsClient.getFolder(request);
		assertNotNull(response);
		
		Set<BaseFolderType> results = responseUtils.parseGetFolderResponse(response);
		assertNotNull(results);
		
		assertFalse(CollectionUtils.isEmpty(results));
		assertEquals(1, results.size());
		BaseFolderType singleResult = DataAccessUtils.singleResult(results);
		assertNotNull(singleResult);
		assertEquals("Calendar", singleResult.getDisplayName());
	}

	/**
	 * Create a single {@link CalendarItemType} and submit with {@link ExchangeWebServicesClient#createItem(CreateItem)}.
	 * @throws JAXBException 
	 */
	@Test
	public void testCreateCalendarItem() throws JAXBException {
		NonEmptyArrayOfBaseItemIdsType createdIds = new NonEmptyArrayOfBaseItemIdsType();
		try {
			initializeCredentials();

			CalendarItemType calendarItem = new CalendarItemType();
			final Date start = DateHelp.parseDateTimePhrase("20131109-1200");
			final Date end = DateHelp.parseDateTimePhrase("20131109-1300");

			calendarItem.setStart(DateHelp.convertDateToXMLGregorianCalendar(start));
			calendarItem.setEnd(DateHelp.convertDateToXMLGregorianCalendar(end));
			calendarItem.setSubject("integration test: testCreateCalendarItem");
			calendarItem.setLocation("test location");
			BodyType body = new BodyType();
			body.setBodyType(BodyTypeType.TEXT);
			body.setValue("test ran at " + new Date());
			calendarItem.setBody(body);

			CreateItem request = new CreateItem();
			request.setSendMeetingInvitations(CalendarItemCreateOrDeleteOperationType.SEND_TO_ALL_AND_SAVE_COPY);

			NonEmptyArrayOfAllItemsType arrayOfItems = new NonEmptyArrayOfAllItemsType();
			arrayOfItems.getItemsAndMessagesAndCalendarItems().add(calendarItem);
			request.setItems(arrayOfItems);
			DistinguishedFolderIdType folder = new DistinguishedFolderIdType();
			folder.setId(DistinguishedFolderIdNameType.CALENDAR);
			TargetFolderIdType target = new TargetFolderIdType();
			target.setDistinguishedFolderId(folder);
			request.setSavedItemFolderId(target);

			StopWatch stopWatch = new StopWatch();
			stopWatch.start();
			CreateItemResponse response = ewsClient.createItem(request);
			stopWatch.stop();
			Assert.assertNotNull(response);
			String captured = capture(response);
			log.debug("CreateItem request (1 CalendarItem) completed in " + stopWatch + ", response: " + captured);

			
			ArrayOfResponseMessagesType responseMessages = response.getResponseMessages();
			Assert.assertNotNull(responseMessages);
			Assert.assertEquals(1, responseMessages.getCreateItemResponseMessagesAndDeleteItemResponseMessagesAndGetItemResponseMessages().size());
			JAXBElement<? extends ResponseMessageType> m = responseMessages.getCreateItemResponseMessagesAndDeleteItemResponseMessagesAndGetItemResponseMessages().get(0);
			Assert.assertEquals(ResponseCodeType.NO_ERROR, m.getValue().getResponseCode());

			ItemInfoResponseMessageType itemType = (ItemInfoResponseMessageType) m.getValue();
			ArrayOfRealItemsType itemArray = itemType.getItems();
			ItemType item = itemArray.getItemsAndMessagesAndCalendarItems().get(0);
			createdIds.getItemIdsAndOccurrenceItemIdsAndRecurringMasterItemIds().add(item.getItemId());

		} finally {
			deleteItems(createdIds);
		}
	}

	/**
	 * Create 3 {@link CalendarItemType}s and submit with 1 {@link ExchangeWebServicesClient#createItem(CreateItem)} invocation.
	 */
	@Test
	public void testCreate3CalendarItems() {
		NonEmptyArrayOfBaseItemIdsType createdIds = new NonEmptyArrayOfBaseItemIdsType();
		try {
			initializeCredentials();

			CalendarItemType item1 = constructCalendarItem(DateHelp.parseDateTimePhrase("20121109-1300"), DateHelp.parseDateTimePhrase("20121109-1400"), 
					"integration test: testCreate3CalendarItems, item1", "test location", "test ran at " + new Date());
			CalendarItemType item2 = constructCalendarItem(DateHelp.parseDateTimePhrase("20121109-1400"), DateHelp.parseDateTimePhrase("20121109-1500"), 
					"integration test: testCreate3CalendarItems, item2", "test location", "test ran at " + new Date());
			CalendarItemType item3 = constructCalendarItem(DateHelp.parseDateTimePhrase("20121109-1500"), DateHelp.parseDateTimePhrase("20121109-1600"), 
					"integration test: testCreate3CalendarItems, item3", "test location", "test ran at " + new Date());

			CreateItem request = new CreateItem();
			request.setSendMeetingInvitations(CalendarItemCreateOrDeleteOperationType.SEND_TO_ALL_AND_SAVE_COPY);

			NonEmptyArrayOfAllItemsType arrayOfItems = new NonEmptyArrayOfAllItemsType();
			arrayOfItems.getItemsAndMessagesAndCalendarItems().add(item1);
			arrayOfItems.getItemsAndMessagesAndCalendarItems().add(item2);
			arrayOfItems.getItemsAndMessagesAndCalendarItems().add(item3);
			request.setItems(arrayOfItems);
			DistinguishedFolderIdType folder = new DistinguishedFolderIdType();
			folder.setId(DistinguishedFolderIdNameType.CALENDAR);
			TargetFolderIdType target = new TargetFolderIdType();
			target.setDistinguishedFolderId(folder);
			request.setSavedItemFolderId(target);

			StopWatch stopWatch = new StopWatch();
			stopWatch.start();
			CreateItemResponse response = ewsClient.createItem(request);
			stopWatch.stop();
			log.debug("CreateItem request (3 CalendarItems) completed in " + stopWatch);
			Assert.assertNotNull(response);
			ArrayOfResponseMessagesType responseMessages = response.getResponseMessages();
			Assert.assertNotNull(responseMessages);
			Assert.assertEquals(3, responseMessages.getCreateItemResponseMessagesAndDeleteItemResponseMessagesAndGetItemResponseMessages().size());
			for(JAXBElement<? extends ResponseMessageType> m : responseMessages.getCreateItemResponseMessagesAndDeleteItemResponseMessagesAndGetItemResponseMessages()) {
				Assert.assertEquals(ResponseCodeType.NO_ERROR, m.getValue().getResponseCode());

				ItemInfoResponseMessageType itemType = (ItemInfoResponseMessageType) m.getValue();
				ArrayOfRealItemsType itemArray = itemType.getItems();
				ItemType item = itemArray.getItemsAndMessagesAndCalendarItems().get(0);
				createdIds.getItemIdsAndOccurrenceItemIdsAndRecurringMasterItemIds().add(item.getItemId());
			}
		} finally {
			deleteItems(createdIds);
		}
	}
	
	/**
	 * Create a single {@link CalendarItemType} and submit with {@link ExchangeWebServicesClient#createItem(CreateItem)}.
	 * Then attempt to update the item with {@link ExchangeWebServices#updateItem(com.microsoft.exchange.messages.UpdateItem)}.
	 * 
	 * @throws JAXBException 
	 */
	@Test
	public void testUpdateCalendarItemChangeLocation() throws JAXBException {
		NonEmptyArrayOfBaseItemIdsType createdIds = new NonEmptyArrayOfBaseItemIdsType();
		try {
			initializeCredentials();

			CalendarItemType calendarItem = new CalendarItemType();
			final Date start = DateHelp.parseDateTimePhrase("20121217-1200");
			final Date end = DateHelp.parseDateTimePhrase("20121217-1300");

			calendarItem.setStart(DateHelp.convertDateToXMLGregorianCalendar(start));
			calendarItem.setEnd(DateHelp.convertDateToXMLGregorianCalendar(end));
			calendarItem.setSubject("integration test: testCreateCalendarItem");
			calendarItem.setLocation("test location");
			BodyType body = new BodyType();
			body.setBodyType(BodyTypeType.TEXT);
			body.setValue("test ran at " + new Date());
			calendarItem.setBody(body);

			CreateItem request = new CreateItem();
			request.setSendMeetingInvitations(CalendarItemCreateOrDeleteOperationType.SEND_TO_ALL_AND_SAVE_COPY);

			NonEmptyArrayOfAllItemsType arrayOfItems = new NonEmptyArrayOfAllItemsType();
			arrayOfItems.getItemsAndMessagesAndCalendarItems().add(calendarItem);
			request.setItems(arrayOfItems);
			DistinguishedFolderIdType folder = new DistinguishedFolderIdType();
			folder.setId(DistinguishedFolderIdNameType.CALENDAR);
			TargetFolderIdType target = new TargetFolderIdType();
			target.setDistinguishedFolderId(folder);
			request.setSavedItemFolderId(target);

			StopWatch stopWatch = new StopWatch();
			stopWatch.start();
			CreateItemResponse response = ewsClient.createItem(request);
			stopWatch.stop();
			Assert.assertNotNull(response);
			String captured = capture(response);
			log.debug("CreateItem request (1 CalendarItem) completed in " + stopWatch + ", response: " + captured);

			
			ArrayOfResponseMessagesType responseMessages = response.getResponseMessages();
			Assert.assertNotNull(responseMessages);
			Assert.assertEquals(1, responseMessages.getCreateItemResponseMessagesAndDeleteItemResponseMessagesAndGetItemResponseMessages().size());
			JAXBElement<? extends ResponseMessageType> m = responseMessages.getCreateItemResponseMessagesAndDeleteItemResponseMessagesAndGetItemResponseMessages().get(0);
			Assert.assertEquals(ResponseCodeType.NO_ERROR, m.getValue().getResponseCode());

			ItemInfoResponseMessageType itemType = (ItemInfoResponseMessageType) m.getValue();
			ArrayOfRealItemsType itemArray = itemType.getItems();
			ItemType item = itemArray.getItemsAndMessagesAndCalendarItems().get(0);
			createdIds.getItemIdsAndOccurrenceItemIdsAndRecurringMasterItemIds().add(item.getItemId());

			// leaf
			CalendarItemType updatedItem = new CalendarItemType();
			updatedItem.setLocation("new location from testUpdateCalendarItem");
			// 1: parent of leaf
			SetItemFieldType changeDescription = new SetItemFieldType();
			PathToUnindexedFieldType path = new PathToUnindexedFieldType();
			
			path.setFieldURI(UnindexedFieldURIType.CALENDAR_LOCATION);
			changeDescription.setPath(objectFactoryCreatePath(path));
			changeDescription.setCalendarItem(updatedItem);
			
			// 2: parent of 1
			NonEmptyArrayOfItemChangeDescriptionsType updates = new NonEmptyArrayOfItemChangeDescriptionsType();
			updates.getAppendToItemFieldsAndSetItemFieldsAndDeleteItemFields().add(changeDescription);
			// 3: parent of 2
			ItemChangeType change = new ItemChangeType();
			change.setItemId(item.getItemId());
			change.setUpdates(updates);
			// 4: parent of 3
			NonEmptyArrayOfItemChangesType changes = new NonEmptyArrayOfItemChangesType();
			changes.getItemChanges().add(change);
			
			UpdateItem updateRequest = new UpdateItem();
			updateRequest.setSendMeetingInvitationsOrCancellations(CalendarItemUpdateOperationType.SEND_ONLY_TO_CHANGED);
			// conflict resolution is required
			updateRequest.setConflictResolution(ConflictResolutionType.AUTO_RESOLVE);
			updateRequest.setItemChanges(changes);
			
			log.debug("sending UpdateItem request: " + capture(updateRequest));
			UpdateItemResponse updateResponse = this.ewsClient.updateItem(updateRequest);
			captured = capture(updateResponse);
			log.debug("UpdateItem request (1 CalendarItem) completed, response: " + captured);
			ArrayOfResponseMessagesType updateMessages = updateResponse.getResponseMessages();
			Assert.assertNotNull(updateMessages);
			Assert.assertEquals(1, updateMessages.getCreateItemResponseMessagesAndDeleteItemResponseMessagesAndGetItemResponseMessages().size());
			JAXBElement<? extends ResponseMessageType> u = updateMessages.getCreateItemResponseMessagesAndDeleteItemResponseMessagesAndGetItemResponseMessages().get(0);
			Assert.assertEquals(ResponseCodeType.NO_ERROR, u.getValue().getResponseCode());
		} finally {
			deleteItems(createdIds);
		}
	}
	
	
	/**
	 * Wraps a call to {@link ObjectFactory#createPath(BasePathToElementType)}.
	 * 
	 * @param path
	 * @return
	 */
	protected JAXBElement<? extends BasePathToElementType> objectFactoryCreatePath(PathToUnindexedFieldType path) {
		ObjectFactory of = new ObjectFactory();
		return of.createPath(path);
	}

	/**
	 * Utility method to issue {@link ExchangeWebServices#deleteItem(DeleteItem)} on the 
	 * {@link NonEmptyArrayOfBaseItemIdsType} argument.
	 * Skips the call if the argument is empty
	 * @param itemIds
	 */
	public void deleteItems(NonEmptyArrayOfBaseItemIdsType itemIds) {
		if(itemIds != null && !itemIds.getItemIdsAndOccurrenceItemIdsAndRecurringMasterItemIds().isEmpty()) {
			DeleteItem request = new DeleteItem();
			request.setSendMeetingCancellations(CalendarItemCreateOrDeleteOperationType.SEND_TO_NONE);
			request.setDeleteType(DisposalType.HARD_DELETE);
			request.setItemIds(itemIds);
			DeleteItemResponse response = ewsClient.deleteItem(request);
			log.info("submitted DeleteItem request for " + itemIds);
			ArrayOfResponseMessagesType responseMessages = response.getResponseMessages();
			for(JAXBElement<? extends ResponseMessageType> m : responseMessages.getCreateItemResponseMessagesAndDeleteItemResponseMessagesAndGetItemResponseMessages()) {
				if(!ResponseCodeType.NO_ERROR.equals(m.getValue().getResponseCode())) {
					String capture = capture(response);
					log.error("suspected failure detected for DeleteItem: " + capture);
					break;
				}
			}
		}
	}

	/**
	 * Marshal the JAXB element and return the output as a {@link String}.
	 * 
	 * @param jaxbElement
	 * @return
	 */
	protected String capture(Object jaxbElement) {
		try {
			ByteArrayOutputStream stream = new ByteArrayOutputStream();
			Marshaller marshaller = jaxbContext.createMarshaller();
			marshaller.marshal(jaxbElement, stream);
			return stream.toString();
		} catch (JAXBException e) {
			log.error("failed to marshal response " + jaxbElement, e);
			throw new IllegalStateException();
		} 

	}
	/**
	 * 
	 * @param startTime
	 * @param endTime
	 * @param subject
	 * @param location
	 * @param bodyText
	 * @return
	 */
	protected CalendarItemType constructCalendarItem(Date startTime, Date endTime, String subject, String location, String bodyText) {
		CalendarItemType calendarItem = new CalendarItemType();
		calendarItem.setStart(DateHelp.convertDateToXMLGregorianCalendar(startTime));
		calendarItem.setEnd(DateHelp.convertDateToXMLGregorianCalendar(endTime));
		calendarItem.setSubject(subject);
		calendarItem.setLocation(location);
		BodyType body = new BodyType();
		body.setBodyType(BodyTypeType.TEXT);
		body.setValue(bodyText);
		calendarItem.setBody(body);
		return calendarItem;
	}

	/**
	 * Technique borrowed from Jasig CalendarPortlet for constructing a
	 * {@link GetUserAvailabilityRequest}.
	 * 
	 * @param startTime
	 * @param endTime
	 * @param emailAddress
	 * @return
	 */
	protected GetUserAvailabilityRequest constructAvailabilityRequest(Date startTime, Date endTime, String emailAddress) {
		// construct the SOAP request object to use
		GetUserAvailabilityRequest soapRequest = new GetUserAvailabilityRequest();

		// create an array of mailbox data representing the current user
		ArrayOfMailboxData mailboxes = new ArrayOfMailboxData();
		MailboxData mailbox = new MailboxData();
		Mailbox address = new Mailbox();
		address.setAddress(emailAddress);
		address.setName("");
		mailbox.setAttendeeType(MeetingAttendeeType.REQUIRED);
		mailbox.setExcludeConflicts(false);
		mailbox.setEmail(address);            
		mailboxes.getMailboxDatas().add(mailbox);
		soapRequest.setMailboxDataArray(mailboxes);

		// create a FreeBusyViewOptions representing the specified period
		FreeBusyViewOptions view = new FreeBusyViewOptions();
		view.setMergedFreeBusyIntervalInMinutes(60);
		// see http://msdn.microsoft.com/en-us/library/exchange/aa565898%28v=exchg.140%29.aspx for description of acceptable values
		view.getRequestedView().add("DetailedMerged");

		Duration dur = new Duration();

		XMLGregorianCalendar start = DateHelp.convertDateToXMLGregorianCalendar(startTime); 
		XMLGregorianCalendar end = DateHelp.convertDateToXMLGregorianCalendar(endTime); 
		dur.setEndTime(end);
		dur.setStartTime(start);

		view.setTimeWindow(dur);
		soapRequest.setFreeBusyViewOptions(view);

		// set the bias to the start time's timezone offset (in minutes 
		// rather than milliseconds)
		TimeZone tz = new TimeZone();
		java.util.TimeZone tZone = java.util.TimeZone.getTimeZone("UTC");
		tz.setBias(tZone.getRawOffset() / 1000 / 60 );

		// TODO: time zone standard vs. daylight info is temporarily hard-coded
		SerializableTimeZoneTime standard = new SerializableTimeZoneTime();
		standard.setBias(0);            
		standard.setDayOfWeek(DayOfWeekType.SUNDAY);
		standard.setDayOrder((short)1);
		standard.setMonth((short)11);
		standard.setTime("02:00:00");
		//standard.setYear("2012");
		SerializableTimeZoneTime daylight = new SerializableTimeZoneTime();
		daylight.setBias(0);
		daylight.setDayOfWeek(DayOfWeekType.SUNDAY);
		daylight.setDayOrder((short)1);
		daylight.setMonth((short)3);
		daylight.setTime("02:00:00");
		//daylight.setYear("2012");
		tz.setStandardTime(standard);
		tz.setDaylightTime(daylight);

		soapRequest.setTimeZone(tz);

		return soapRequest;
	}

	/**
	 * Construct a {@link FindItem} request object for the specified email address and bounded by
	 * 
	 * @param startTime
	 * @param endTime
	 * @param emailAddress
	 * @return
	 */
	protected FindItem constructFindItemRequest(Date startTime, Date endTime, String emailAddress) {
		FindItem findItem = new FindItem();

		CalendarViewType calendarView = new CalendarViewType();
		calendarView.setStartDate(DateHelp.convertDateToXMLGregorianCalendar(startTime));
		calendarView.setEndDate(DateHelp.convertDateToXMLGregorianCalendar(endTime));
		calendarView.setMaxEntriesReturned(ExchangeOnlineThrottlingPolicy.FIND_ITEM_MAX_ENTRIES_RETURNED);
		findItem.setCalendarView(calendarView);
		findItem.setTraversal(ItemQueryTraversalType.SHALLOW);
		ItemResponseShapeType responseShape = new ItemResponseShapeType();
		// there is a large difference in the properties set returned by the DEFAULT shape type
		responseShape.setBaseShape(DefaultShapeNamesType.ALL_PROPERTIES);
		findItem.setItemShape(responseShape);
		NonEmptyArrayOfBaseFolderIdsType array = new NonEmptyArrayOfBaseFolderIdsType();
		DistinguishedFolderIdType folderId = new DistinguishedFolderIdType();
		EmailAddressType emailAddressType = new EmailAddressType();
		emailAddressType.setEmailAddress(emailAddress);

		folderId.setId(DistinguishedFolderIdNameType.CALENDAR);
		folderId.setMailbox(emailAddressType);
		array.getFolderIdsAndDistinguishedFolderIds().add(folderId);
		findItem.setParentFolderIds(array);
		
		log.info("Constructed FindItemRequest for " + emailAddress + ". From " +startTime.toString() +" to "+ endTime.toString()+" : "+findItem.toString());
		
		return findItem;
	}
	
	public FindFolder constructFindFolderRequest(){
		FindFolder findFolder =  new FindFolder();
		
		//use DEEP to search folders recursively
		findFolder.setTraversal(FolderQueryTraversalType.DEEP);
		
		//set the response shape
		FolderResponseShapeType responseShape = new FolderResponseShapeType();
		responseShape.setBaseShape(DefaultShapeNamesType.ALL_PROPERTIES);
		findFolder.setFolderShape(responseShape);
		
		//define how paged view is returned
		IndexedPageViewType pageViewType = new IndexedPageViewType();
		pageViewType.setBasePoint(IndexBasePointType.BEGINNING);
		pageViewType.setMaxEntriesReturned(1000);
		pageViewType.setOffset(0);
		findFolder.setIndexedPageFolderView(pageViewType);
		
		NonEmptyArrayOfBaseFolderIdsType array = new NonEmptyArrayOfBaseFolderIdsType();
		DistinguishedFolderIdType folderId = new DistinguishedFolderIdType();
		folderId.setId(DistinguishedFolderIdNameType.CALENDAR);
		array.getFolderIdsAndDistinguishedFolderIds().add(folderId);
		findFolder.setParentFolderIds(array);
		
		return findFolder;
	}

	public void FindFolders() {
		// TODO Auto-generated method stub
		
	}

}
