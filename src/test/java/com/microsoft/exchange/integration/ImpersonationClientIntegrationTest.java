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

import static org.junit.Assert.assertNotNull;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.StopWatch;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.microsoft.exchange.DateHelp;
import com.microsoft.exchange.ExchangeEventConverterOLD;
import com.microsoft.exchange.impl.ThreadLocalImpersonationConnectingSIDSourceImpl;
import com.microsoft.exchange.messages.ArrayOfResponseMessagesType;
import com.microsoft.exchange.messages.FindFolder;
import com.microsoft.exchange.messages.FindFolderResponse;
import com.microsoft.exchange.messages.FindFolderResponseMessageType;
import com.microsoft.exchange.messages.FindItem;
import com.microsoft.exchange.messages.FindItemResponse;
import com.microsoft.exchange.messages.FindItemResponseMessageType;
import com.microsoft.exchange.messages.GetServerTimeZones;
import com.microsoft.exchange.messages.GetServerTimeZonesResponse;
import com.microsoft.exchange.messages.GetServerTimeZonesResponseMessageType;
import com.microsoft.exchange.messages.GetUserAvailabilityRequest;
import com.microsoft.exchange.messages.GetUserAvailabilityResponse;
import com.microsoft.exchange.messages.ResolveNames;
import com.microsoft.exchange.messages.ResolveNamesResponse;
import com.microsoft.exchange.messages.ResolveNamesResponseMessageType;
import com.microsoft.exchange.messages.ResponseMessageType;
import com.microsoft.exchange.types.ArrayOfFoldersType;
import com.microsoft.exchange.types.ArrayOfRealItemsType;
import com.microsoft.exchange.types.ArrayOfResolutionType;
import com.microsoft.exchange.types.ArrayOfTimeZoneDefinitionType;
import com.microsoft.exchange.types.BaseFolderType;
import com.microsoft.exchange.types.CalendarItemType;
import com.microsoft.exchange.types.ConnectingSIDType;
import com.microsoft.exchange.types.ContactItemType;
import com.microsoft.exchange.types.DefaultShapeNamesType;
import com.microsoft.exchange.types.EmailAddressDictionaryEntryType;
import com.microsoft.exchange.types.EmailAddressDictionaryType;
import com.microsoft.exchange.types.FindFolderParentType;
import com.microsoft.exchange.types.FindItemParentType;
import com.microsoft.exchange.types.ItemType;
import com.microsoft.exchange.types.NonEmptyArrayOfTimeZoneIdType;
import com.microsoft.exchange.types.ResolutionType;
import com.microsoft.exchange.types.ResolveNamesSearchScopeType;
import com.microsoft.exchange.types.TimeZoneDefinitionType;

/**
 * Integration test that depends on the Impersonation technique.
 * 
 * @author Nicholas Blair
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations="classpath:/com/microsoft/exchange/exchangeContext-usingImpersonation.xml")
public class ImpersonationClientIntegrationTest extends AbstractIntegrationTest {
	
	private int expectedEventCount = 1;
	
	
	/* (non-Javadoc)
	 * @see com.microsoft.exchange.integration.AbstractIntegrationTest#initializeCredentials()
	 */
	@Override
	public void initializeCredentials() {
		ConnectingSIDType connectingSID = new ConnectingSIDType();
		connectingSID.setPrincipalName(emailAddress);
		ThreadLocalImpersonationConnectingSIDSourceImpl.setConnectingSID(connectingSID);
	}
	
	@Test @Override
	public void getPrimaryCalendarFolder() {
		super.getPrimaryCalendarFolder();
	}
	
	@Test @Override
	public void findFolders() {
		super.findFolders();
	}
	
	
	
	@Test
	public void testResolveNames() {
		Set<String> addresses = new HashSet<String>();
		initializeCredentials();
		ResolveNames request = new ResolveNames();
		request.setContactDataShape(DefaultShapeNamesType.ALL_PROPERTIES);
		request.setReturnFullContactData(true);
		request.setSearchScope(ResolveNamesSearchScopeType.ACTIVE_DIRECTORY_CONTACTS);
		request.setUnresolvedEntry("ctcudd");

		ResolveNamesResponse response = ewsClient.resolveNames(request);
		ArrayOfResponseMessagesType arrayOfResponseMessages = response.getResponseMessages();
		List<JAXBElement<? extends ResponseMessageType>> responseMessages = arrayOfResponseMessages.getCreateItemResponseMessagesAndDeleteItemResponseMessagesAndGetItemResponseMessages();
		for(JAXBElement<? extends ResponseMessageType> element: responseMessages) {
			ResolveNamesResponseMessageType rnrmt = (ResolveNamesResponseMessageType) element.getValue();
			ArrayOfResolutionType arrayOfResolutionType = rnrmt.getResolutionSet();
			List<ResolutionType> resolutions = arrayOfResolutionType.getResolutions();
			for(ResolutionType resolution: resolutions) {
				ContactItemType contact = resolution.getContact();
				EmailAddressDictionaryType emailAddresses = contact.getEmailAddresses();
				List<EmailAddressDictionaryEntryType> entries = emailAddresses.getEntries();
				for(EmailAddressDictionaryEntryType entry: entries) {
					String value = entry.getValue();
					if(StringUtils.isNotBlank(value)) {
						value = value.toLowerCase();
						value = StringUtils.removeStartIgnoreCase(value, "smtp:");
						addresses.add(value);
					}
					
				}
			}
			
		}
		for(String s: addresses) {
			log.info(s);
		}
	}
	
	/**
	 * Issues a {@link GetUserAvailabilityRequest} for the configured emailAddress, startDate and endDate.
	 * Verifies a response, and that the freebusy responses match expectedEventCount.
	 */
	@Test
	public void testGetUserAvailability() {	
		initializeCredentials();
		GetUserAvailabilityRequest request = constructAvailabilityRequest(DateHelp.makeDate(startDate), DateHelp.makeDate(endDate), emailAddress);
		StopWatch stopWatch = new StopWatch();
		stopWatch.start();
		GetUserAvailabilityResponse response = ewsClient.getUserAvailability(request);
		stopWatch.stop();
		log.debug("GetUserAvailability request completed in " + stopWatch);
		Assert.assertNotNull(response);
		Assert.assertEquals(expectedEventCount, response.getFreeBusyResponseArray().getFreeBusyResponses().size());
	}
	/**
	 * Similar to {@link #testGetUserAvailability()}, but uses {@link FindItem}.
	 * 
	 * @throws JAXBException
	 */
	@Test
	public void testFindItemCalendarType() throws JAXBException {
		initializeCredentials();
		FindItem request = constructFindItemRequest(DateHelp.makeDate(startDate), DateHelp.makeDate(endDate), emailAddress);
		StopWatch stopWatch = new StopWatch();
		stopWatch.start();
		FindItemResponse response = ewsClient.findItem(request);
		stopWatch.stop();
		log.debug("FindItem request completed in " + stopWatch);
		Assert.assertNotNull(response);
		Assert.assertEquals(expectedEventCount, response.getResponseMessages().getCreateItemResponseMessagesAndDeleteItemResponseMessagesAndGetItemResponseMessages().size());
	}
	
	/**
	 * Similar to {@link #testGetUserAvailability()}, but uses {@link FindItem}.
	 * 
	 * @throws JAXBException
	 */
	@Test
	public void testFindMoreDetailedItemCalendarType() throws JAXBException {
		initializeCredentials();
		FindItem request = constructFindItemRequest(DateHelp.makeDate(startDate), DateHelp.makeDate(endDate), emailAddress);
		StopWatch stopWatch = new StopWatch();
		stopWatch.start();
		FindItemResponse response = ewsClient.findItem(request);
		String captured = capture(response);
		log.info("testFindMoreDetailedItemCalendarType response: " + captured);
		stopWatch.stop();
		log.debug("FindItem request completed in " + stopWatch);
		Assert.assertNotNull(response);
		Assert.assertEquals(expectedEventCount, response.getResponseMessages().getCreateItemResponseMessagesAndDeleteItemResponseMessagesAndGetItemResponseMessages().size());
	}
	
	
	@Test
	public void testListCalendarNames(){
		initializeCredentials();
		FindFolder request = constructFindFolderRequest();
		StopWatch stopWatch = new StopWatch();
		stopWatch.start();
		FindFolderResponse response = ewsClient.findFolder(request);
		String captured =capture(response);
		log.info("testListCalendarNames response: "+captured);
		stopWatch.stop();
		log.debug("FindFolder request completed in "+ stopWatch);
		Assert.assertNotNull(response);
		
		//now generate a Map?
		Map<String, String> msolCalendars = new LinkedHashMap<String, String>();
		ArrayOfResponseMessagesType responses = response.getResponseMessages();
		List<JAXBElement<? extends ResponseMessageType>> responseList = 
				responses.getCreateItemResponseMessagesAndDeleteItemResponseMessagesAndGetItemResponseMessages();
		//iterate over responses
		for(JAXBElement<? extends ResponseMessageType> rm : responseList){
			FindFolderResponseMessageType itemType = (FindFolderResponseMessageType) rm.getValue();
			FindFolderParentType rootFolder =  itemType.getRootFolder();
			ArrayOfFoldersType folders = rootFolder.getFolders();
			List<BaseFolderType> folderList = folders.getFoldersAndCalendarFoldersAndContactsFolders();
			for(BaseFolderType baseFolder : folderList){
				String displayName = baseFolder.getDisplayName();
				String folderId = baseFolder.getFolderId().getId();
				String changeKey = baseFolder.getFolderId().getChangeKey();
				log.debug(displayName +"(id="+ folderId +" : changeKey="+changeKey+" )");
			}
		}
	}
	
	
	
//	@Test
//	public void testGetCalendarObjects() throws JAXBException{
//		
//		ExchangeEventConverter eec = new ExchangeEventConverter();
//
//		initializeCredentials();
//		FindItem request = erh.constructFindItemRequest(DateHelp.makeDate(startDate), DateHelp.makeDate(endDate), emailAddress, DefaultShapeNamesType.ID_ONLY);
//		FindItemResponse response = ewsClient.findItem(request);
//
//		List<JAXBElement<? extends ResponseMessageType>> responseList = response.getResponseMessages()
//				.getCreateItemResponseMessagesAndDeleteItemResponseMessagesAndGetItemResponseMessages();
//		
//		//iterate over responses
//		for(JAXBElement<? extends ResponseMessageType> rm : responseList){
//			FindItemResponseMessageType itemType = (FindItemResponseMessageType) rm.getValue();
//			FindItemParentType rootFolder = itemType.getRootFolder();
//			ArrayOfRealItemsType itemArray = rootFolder.getItems();
//			List<ItemType> items = itemArray.getItemsAndMessagesAndCalendarItems();
//				
//			//iterate over items in each response
//			for(ItemType item : items){
//				CalendarItemType calItem = (CalendarItemType) item;
//				GetItem getItemRequest = erh.constructGetItemRequest(calItem);
//				GetItemResponse getItemResponse = ewsClient.getItem(getItemRequest);
//				
//				//iterate over getItemResponseMessages
//				List<JAXBElement<? extends ResponseMessageType>> getItemResponseList = getItemResponse.getResponseMessages()
//						.getCreateItemResponseMessagesAndDeleteItemResponseMessagesAndGetItemResponseMessages();
//				
//				//iterate over getItemResponseList
//				for(JAXBElement<? extends ResponseMessageType> getItemResponseMessage : getItemResponseList){
//					ItemInfoResponseMessageType itemInfoResponseMessageType = (ItemInfoResponseMessageType) getItemResponseMessage.getValue();
//					ArrayOfRealItemsType itemsArray = itemInfoResponseMessageType.getItems();
//					for(ItemType currentCalItem :  itemsArray.getItemsAndMessagesAndCalendarItems()){
//						eec.add((CalendarItemType) currentCalItem);
//					}
//				}			
//			}
//			//eCalendars.add(new CalendarWithURI(eec.ical , "microsoftEWSTest"));
//		}
//		log.debug("GetCalenderObjects returned: "+ eec.ical.toString());
//		
//	}
	
	@Test
	public void testFindCalendarObject() throws JAXBException {
		
		initializeCredentials();
		FindItem request = constructFindItemRequest(DateHelp.makeDate(startDate), DateHelp.makeDate(endDate), emailAddress);
		StopWatch stopWatch = new StopWatch();
		stopWatch.start();
		FindItemResponse response = ewsClient.findItem(request);
		String captured = capture(response);
		log.info("testFindCalendarObject response: " + captured);
		stopWatch.stop();
		log.debug("FindItem request completed in " + stopWatch);
		Assert.assertNotNull(response);
		Assert.assertEquals(expectedEventCount, response.getResponseMessages().getCreateItemResponseMessagesAndDeleteItemResponseMessagesAndGetItemResponseMessages().size());
	
		List<JAXBElement<? extends ResponseMessageType>> responseList = response.getResponseMessages()
				.getCreateItemResponseMessagesAndDeleteItemResponseMessagesAndGetItemResponseMessages();
		
		
		for(JAXBElement<? extends ResponseMessageType> rm : responseList){
			//Class<? extends ResponseMessageType> rmt = rm.getDeclaredType();
			
			FindItemResponseMessageType itemType = (FindItemResponseMessageType) rm.getValue();
			FindItemParentType rootFolder = itemType.getRootFolder();
			ArrayOfRealItemsType itemArray = rootFolder.getItems();
			List<ItemType> items = itemArray.getItemsAndMessagesAndCalendarItems();
			
			//ArrayOfRealItemsType itemArray = itemType.getItems();
			Integer itemCount = new Integer(items.size());
			Integer currentItemNum = new Integer(1);
			
			ExchangeEventConverterOLD eec = new ExchangeEventConverterOLD();
			
			for(ItemType item : items){
				
				CalendarItemType calItem = (CalendarItemType) item;
				eec.add(calItem);
				
				
				
				StringBuilder sb = new StringBuilder();
				sb.append("\n   ");
				sb.append("ItemId: "+ calItem.getItemId().toString()+"\n   ");
				sb.append("ParentFolderId: "+calItem.getParentFolderId().toString()+"\n   ");
				sb.append("ItemClass: "+calItem.getItemClass().toString()+"\n   ");
				sb.append("Subject: "+calItem.getSubject().toString()+"\n   ");
				sb.append("Sensitivity: "+calItem.getSensitivity().toString()+"\n   ");
				sb.append("DateTimeReceived: "+calItem.getDateTimeReceived().toString()+"\n   ");
				sb.append("Size: "+calItem.getSize().toString()+"\n   ");
				sb.append("Importance: "+calItem.getImportance().toString()+"\n   ");
				sb.append("IsSubmitted: "+calItem.isIsSubmitted().toString()+"\n   ");
				sb.append("IsDraft: "+calItem.isIsDraft().toString()+"\n   ");
				sb.append("IsFromMe: "+calItem.isIsFromMe().toString()+"\n   ");
				sb.append("IsResend: "+calItem.isIsResend().toString()+"\n   ");
				sb.append("IsUnmodified: "+calItem.isIsUnmodified().toString()+"\n   ");
				sb.append("DateTimeSent: "+calItem.getDateTimeSent().toString()+"\n   ");
				sb.append("DateTimeCreated: "+calItem.getDateTimeCreated().toString()+"\n   ");
				sb.append("ReminderDueBy: "+calItem.getReminderDueBy().toString()+"\n   ");
				sb.append("ReminderIsSet: "+calItem.isReminderIsSet().toString()+"\n   ");
				sb.append("ReminderMinutesBeforeStart: "+calItem.getReminderMinutesBeforeStart().toString()+"\n   ");
				sb.append("DisplayCc: "+calItem.getDisplayCc().toString()+"\n   ");
				sb.append("DisplayTo: "+calItem.getDisplayTo().toString()+"\n   ");
				sb.append("HasAttachments: "+calItem.isHasAttachments().toString()+"\n   ");
				sb.append("Culture: "+calItem.getCulture().toString()+"\n   ");
				if(calItem.getEffectiveRights()!=null){
					sb.append("EffectiveRights.isCreateAssociated: "+(calItem.getEffectiveRights()==null ? "false" : new Boolean(calItem.getEffectiveRights().isCreateAssociated()).toString())+"\n   ");
					sb.append("EffectiveRights.isCreateContents: "+new Boolean(calItem.getEffectiveRights().isCreateContents()).toString()+"\n   ");
					sb.append("EffectiveRights.isCreateHierarchy: "+new Boolean(calItem.getEffectiveRights().isCreateHierarchy()).toString()+"\n   ");
					sb.append("EffectiveRights.isDelete: "+new Boolean(calItem.getEffectiveRights().isDelete()).toString()+"\n   ");
					sb.append("EffectiveRights.isModify: "+new Boolean(calItem.getEffectiveRights().isModify()).toString()+"\n   ");
					
				}
				//sb.append("LastModifiedName: "+calItem.getLastModifiedName().toString()+"\n   ");
				//sb.append("LastModifiedTime: "+calItem.getLastModifiedTime().toString()+"\n   ");
				//sb.append("IsAssociated: "+calItem.isIsAssociated().toString()+"\n   ");
				//sb.append("WebClientReadFormQueryString: "+calItem.getWebClientReadFormQueryString().toString()+"\n   ");
				///sb.append("WebClientEditFormQueryString: "+calItem.getWebClientEditFormQueryString().toString()+"\n   ");
				//sb.append("UID: "+calItem.getUID().toString()+"\n   ");
				//sb.append("DateTimeStamp: "+calItem.getDateTimeStamp().toString()+"\n   ");
				sb.append("Start: "+calItem.getStart().toString()+"\n   ");
				sb.append("End: "+calItem.getEnd().toString()+"\n   ");
				sb.append("IsAllDayEvent: "+calItem.isIsAllDayEvent().toString()+"\n   ");
				sb.append("LegacyFreeBusyStatus: "+calItem.getLegacyFreeBusyStatus().toString()+"\n   ");
				if(null != calItem.getLocation()) sb.append("Location: "+calItem.getLocation().toString()+"\n   ");
				sb.append("IsMeeting: "+calItem.isIsMeeting().toString()+"\n   ");
				sb.append("IsCancelled: "+calItem.isIsCancelled().toString()+"\n   ");
				sb.append("IsRecurring: "+calItem.isIsRecurring().toString()+"\n   ");
				//sb.append("MeetingRequestWasSent: "+calItem.getMeetingRequestWasSent().toString()+"\n   ");
				sb.append("IsResponseRequested: "+calItem.isIsResponseRequested().toString()+"\n   ");
				sb.append("CalendarItemType: "+calItem.getCalendarItemType().toString()+"\n   ");
				sb.append("MyResponseType: "+calItem.getMyResponseType().toString()+"\n   ");
				sb.append("OrganizerName: "+calItem.getOrganizer().getMailbox().getName()+"\n   ");
				sb.append("OrganizerName: "+calItem.getOrganizer().getMailbox().getMailboxType()+"\n   ");
				sb.append("Duration: "+calItem.getDuration().toString()+"\n   ");
				sb.append("TimeZone: "+calItem.getTimeZone().toString()+"\n   ");
				sb.append("AppointmentSequenceNumber: "+calItem.getAppointmentSequenceNumber().toString()+"\n   ");
				sb.append("AppointmentState: "+calItem.getAppointmentState().toString()+"\n   ");
			
				//log.debug("FoundCalendarItem ("+currentItemNum.toString()+" of "+itemCount.toString()+"):" + sb.toString());
				currentItemNum++;
			
			}
			
			log.debug("ExchangeEventConverter results: " +  eec.ical);
			//ItemType item = itemArray.getItemsAndMessagesAndCalendarItems().get(0);
			
			
			
			
			//Fields[] fields = rmt.getFields();
		}
				
	}
	
	@Test
	public void getTimeZonesTest(){
		String tzId = "UTC";
		GetServerTimeZones request = new GetServerTimeZones();
		request.setReturnFullTimeZoneData(false);
		
		
		NonEmptyArrayOfTimeZoneIdType tzIds =  new NonEmptyArrayOfTimeZoneIdType();
		tzIds.getIds().add(tzId);
		request.setIds(tzIds);
		GetServerTimeZonesResponse response = ewsClient.getServerTimeZones(request);
		
		
		ArrayOfResponseMessagesType responseMessages = response.getResponseMessages();
		List<JAXBElement<? extends ResponseMessageType>> tzResponseMessages = responseMessages.getCreateItemResponseMessagesAndDeleteItemResponseMessagesAndGetItemResponseMessages();
		for(JAXBElement<? extends ResponseMessageType> message: tzResponseMessages){
			ResponseMessageType r = message.getValue();
			GetServerTimeZonesResponseMessageType itemInfo = (GetServerTimeZonesResponseMessageType) r;
			ArrayOfTimeZoneDefinitionType timeZoneDefinitions = itemInfo.getTimeZoneDefinitions();
			List<TimeZoneDefinitionType> timeZoneDefinitionsList = timeZoneDefinitions.getTimeZoneDefinitions();
			for(TimeZoneDefinitionType timeZoneDef: timeZoneDefinitionsList){
				if(tzId.equals(timeZoneDef.getName()) || tzId.equals(timeZoneDef.getId())){
					assertNotNull(timeZoneDef);
				}
			}
		}
		
	}
	
}
