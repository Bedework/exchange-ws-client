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

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.support.DataAccessUtils;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.ibm.icu.util.Region;
import com.ibm.icu.util.TimeZone;
import com.microsoft.exchange.exception.ExchangeCannotDeleteRuntimeException;
import com.microsoft.exchange.exception.ExchangeRuntimeException;
import com.microsoft.exchange.impl.BaseExchangeCalendarDataDao;
import com.microsoft.exchange.messages.GetServerTimeZones;
import com.microsoft.exchange.types.BaseFolderType;
import com.microsoft.exchange.types.CalendarItemType;
import com.microsoft.exchange.types.DisposalType;
import com.microsoft.exchange.types.FolderIdType;
import com.microsoft.exchange.types.ItemIdType;
import com.microsoft.exchange.types.ItemType;
import com.microsoft.exchange.types.TimeZoneDefinitionType;


@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations= {"classpath:test-contexts/exchangeContext.xml"})
public class BaseExchangeCalendarDataDaoIntegrationTest {
	protected final Log log = LogFactory.getLog(this.getClass());

	String username = "someusername";
	
	@Value("${integration.email:someemailaddress@on.yourexchangeserver.edu}")
	String upn;
	
	@Autowired
	BaseExchangeCalendarDataDao exchangeCalendarDataDao;

	@Rule
	public ExpectedException exception = ExpectedException.none();
	
	@Test
	public void isAutowired() {
		assertNotNull(exchangeCalendarDataDao);
		assertNotNull(upn);
	}
	
	@Test
	public void resolveEmailAddresses() {
		Set<String> results = exchangeCalendarDataDao.resolveEmailAddresses(upn);
		assertNotNull(results);
		for(String r: results) log.info(r);
	}
	
	@Test
	public void resolveUpn() {
		String resolved = exchangeCalendarDataDao.resolveUpn(upn);
		assertNotNull(resolved);
		log.info(upn+" resolved to "+ resolved);
	}
	
	/**
	 * Executes a {@link GetServerTimeZones} request, retrieving all {@link TimeZoneDefinitionType}s from Exchange.
	 * Attempts to map each {@link TimeZoneDefinitionType} to an {@link TimeZone} and logs the result.
	 */
	@Test
	public void getTimeZoneIds(){
		int validTimeZoneCount = 0;
		List<TimeZoneDefinitionType> zones = exchangeCalendarDataDao.getServerTimeZones(null, false);
		log.info("Found "+zones.size() +" exchange time zones.");
		for(TimeZoneDefinitionType zone: zones){
			String sysTimeZoneID = TimeZone.getIDForWindowsID(zone.getId(), "US");
			
			if(StringUtils.isNotBlank(sysTimeZoneID)){
				log.info(zone.getId() +" mapped to "+ sysTimeZoneID);
				
				String windowsID = TimeZone.getWindowsID(sysTimeZoneID);
				assertEquals(zone.getId(), windowsID);
				validTimeZoneCount++;
			}else{
				log.warn("no mapping for windowsID="+zone.getId());
			}
			
		}
		log.info("Succesfully mapped "+validTimeZoneCount+"/"+zones.size()+" WindowsTimeZones");
	}
	
	@Test
	public void getPrimaryCalendarFolder(){
		BaseFolderType primaryCalendarFolder = exchangeCalendarDataDao.getPrimaryCalendarFolder(upn);
		assertNotNull(primaryCalendarFolder);
	}
	
	@Test
	public void getPrimaryTaskFolder(){
		BaseFolderType primaryCalendarFolder = exchangeCalendarDataDao.getPrimaryCalendarFolder(upn);
		assertNotNull(primaryCalendarFolder);
	}
	
	@Test
	public void getCalendarFolders(){
		Map<String, String> folderMap = exchangeCalendarDataDao.getCalendarFolderMap(upn);
		for(String folderId: folderMap.keySet()){
			String folderName = folderMap.get(folderId);
			log.info(folderName);
		}
	}
	
	@Test
	public void getTaskFolders(){
		Map<String, String> folderMap = exchangeCalendarDataDao.getTaskFolderMap(upn);
		for(String folderId: folderMap.keySet()){
			String folderName = folderMap.get(folderId);
			log.info(folderName);
		}
	}
	
	@Test
	public void deleteTaskFolders(){
		Map<String, String> taskFoldersMap = exchangeCalendarDataDao.getTaskFolderMap(upn);
		for(String taskId :  taskFoldersMap.keySet()){
			String taskFolderName = taskFoldersMap.get(taskId);
			if(taskFolderName.equals("Tasks")) continue;
			FolderIdType taskFolderId= new FolderIdType();
			taskFolderId.setId(taskId);
			
			log.info("deleting taskFolder '"+taskFolderName+"' ");
			boolean deleteTaskFolderSuccess = exchangeCalendarDataDao.deleteFolder(upn, DisposalType.SOFT_DELETE, taskFolderId);
			assertTrue(deleteTaskFolderSuccess);
		}
	}
	
	/**
	 * This method can fail when there are many (1k+) items in a folder, probably due to throttling limits....
	 * "Exchange Web Services are not currently available for this request because none of the Client Access Servers in the destination site could process the request." 
	 *
	 *This method can also fail when disposalType is set to MOVE_TO_DELETED_ITEMS
	 *If impersonation is used you have to use the MoveToDeletedItems method...
	 *see note here: http://msdn.microsoft.com/en-us/library/office/aa580484(v=exchg.150).aspx
	 *
	 *
	 */
	@Test
	public void deleteFolder(){
		FolderIdType calendarFolderId = exchangeCalendarDataDao.getCalendarFolderId(upn, "A2");
		assertNotNull(calendarFolderId);
		
		boolean deleteFolderResult = exchangeCalendarDataDao.deleteFolder(upn,DisposalType.SOFT_DELETE,calendarFolderId);
		assertTrue(deleteFolderResult);
	}
	
	@Test
	public void findAllItemIds(){
		FolderIdType calendarFolderId = exchangeCalendarDataDao.getCalendarFolderId(upn, "A2");
		assertNotNull(calendarFolderId);
		
		Set<FolderIdType> singleton = Collections.singleton(calendarFolderId);
		Set<ItemIdType> itemIds = exchangeCalendarDataDao.findAllItemIds(upn, singleton);
		
		for(ItemIdType itemId :itemIds){
			log.info(itemId.getId());
		}
	}
	
	@Test
	public void emptyCalendarFolder(){
		FolderIdType calendarFolderId = exchangeCalendarDataDao.getCalendarFolderId(upn, "A1");
		assertNotNull(calendarFolderId);
		boolean emptyCalendarFolder = exchangeCalendarDataDao.emptyCalendarFolder(upn, calendarFolderId);
		assertTrue(emptyCalendarFolder);
		
		Set<ItemIdType> findItemIds = exchangeCalendarDataDao.findItemIds(upn, Collections.singleton(calendarFolderId));
		assertTrue(findItemIds.isEmpty());
	}
	
	/**
	 * An error response that includes the ErrorCannotDeleteObject error
	 * code will be returned for a DeleteItem operation when a delegate
	 * tries to delete an item in the principal's mailbox by setting the
	 * DisposalType to MoveToDeletedItems. To delete an item by moving it to
	 * the Deleted Items folder, a delegate must use the MoveItem operation.
	 * 
	 * @see http://msdn.microsoft.com/en-us/library/office/aa580484(v=exchg.150).aspx
	 */
	@Test
	public void createDeleteCalendarFolder(){
		String displayName = "TEST "+upn;
		FolderIdType folderId = exchangeCalendarDataDao.createCalendarFolder(upn, displayName);
		assertNotNull(folderId);
		
		boolean deleteFolderSuccess = false;
		
		try{
			deleteFolderSuccess =exchangeCalendarDataDao.deleteFolder(upn, DisposalType.MOVE_TO_DELETED_ITEMS, folderId);
			fail("MOVE_TO_DELETED_ITEMS should have thrown an exception!");
		}catch(ExchangeCannotDeleteRuntimeException e){	}
		
		assertFalse(deleteFolderSuccess);
		log.info("deleteFolder via MOVE_TO_DELETED_ITEMS failed as expected, attempting SOFT_DELETE");
		deleteFolderSuccess = exchangeCalendarDataDao.deleteFolder(upn, DisposalType.SOFT_DELETE, folderId);
		if(deleteFolderSuccess){
			log.info("deleteFolder via SOFT_DELETE success!");
		}else{
			log.info("deleteFolder via SOFT_DELETE failure, attempting HARD_DELETE");
			deleteFolderSuccess = exchangeCalendarDataDao.deleteFolder(upn, DisposalType.HARD_DELETE, folderId);
		}
		assertTrue(deleteFolderSuccess);
		exception.expect(ExchangeRuntimeException.class);
		folderId = exchangeCalendarDataDao.getCalendarFolderId(upn, displayName);
	}
	
	@Test
	public void getEmptyDeleteCalendarFolder(){
		String displayName = "TEST "+upn;
		FolderIdType calendarFolderId = exchangeCalendarDataDao.getCalendarFolderId(upn, displayName);
		assertNotNull(calendarFolderId);
		
		boolean result = exchangeCalendarDataDao.deleteCalendarFolder(upn, calendarFolderId);
		assertTrue(result);
		
		exception.expect(ExchangeRuntimeException.class);
		calendarFolderId = exchangeCalendarDataDao.getCalendarFolderId(upn, displayName);
		
		assertNull(calendarFolderId);
	}
	

	@Test
	public void createGetDeleteEmptyCalendarItem(){
		
		CalendarItemType calendarItem = new CalendarItemType();
		ItemIdType calendarItemId = exchangeCalendarDataDao.createCalendarItem(upn, calendarItem);
		assertNotNull(calendarItemId);
		Set<CalendarItemType> createdCalendarItems = exchangeCalendarDataDao.getCalendarItems(upn, Collections.singleton(calendarItemId));
		CalendarItemType createdCalendarItem = DataAccessUtils.singleResult(createdCalendarItems);
		assertNotNull(createdCalendarItem);
		assertNotNull(createdCalendarItem.getStart());
		
		boolean deleteSuccess = exchangeCalendarDataDao.deleteCalendarItems(upn, Collections.singleton(calendarItemId));
		assertTrue(deleteSuccess);
		
	}
}
