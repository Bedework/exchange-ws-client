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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.xml.bind.JAXBElement;
import javax.xml.datatype.XMLGregorianCalendar;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.util.CollectionUtils;

import com.microsoft.exchange.messages.CreateFolder;
import com.microsoft.exchange.messages.CreateItem;
import com.microsoft.exchange.messages.DeleteFolder;
import com.microsoft.exchange.messages.DeleteItem;
import com.microsoft.exchange.messages.EmptyFolder;
import com.microsoft.exchange.messages.FindFolder;
import com.microsoft.exchange.messages.FindItem;
import com.microsoft.exchange.messages.GetFolder;
import com.microsoft.exchange.messages.GetItem;
import com.microsoft.exchange.messages.GetServerTimeZones;
import com.microsoft.exchange.messages.GetUserAvailabilityRequest;
import com.microsoft.exchange.messages.GetUserConfiguration;
import com.microsoft.exchange.messages.ResolveNames;
import com.microsoft.exchange.messages.UpdateFolder;
import com.microsoft.exchange.types.AcceptItemType;
import com.microsoft.exchange.types.AffectedTaskOccurrencesType;
import com.microsoft.exchange.types.AndType;
import com.microsoft.exchange.types.ArrayOfCalendarPermissionsType;
import com.microsoft.exchange.types.ArrayOfMailboxData;
import com.microsoft.exchange.types.BaseFolderIdType;
import com.microsoft.exchange.types.BaseFolderType;
import com.microsoft.exchange.types.BaseItemIdType;
import com.microsoft.exchange.types.BasePathToElementType;
import com.microsoft.exchange.types.BodyTypeResponseType;
import com.microsoft.exchange.types.CalendarFolderType;
import com.microsoft.exchange.types.CalendarItemCreateOrDeleteOperationType;
import com.microsoft.exchange.types.CalendarPermissionLevelType;
import com.microsoft.exchange.types.CalendarPermissionReadAccessType;
import com.microsoft.exchange.types.CalendarPermissionSetType;
import com.microsoft.exchange.types.CalendarPermissionType;
import com.microsoft.exchange.types.CalendarViewType;
import com.microsoft.exchange.types.ConstantValueType;
import com.microsoft.exchange.types.DefaultShapeNamesType;
import com.microsoft.exchange.types.DeleteFolderFieldType;
import com.microsoft.exchange.types.DisposalType;
import com.microsoft.exchange.types.DistinguishedFolderIdNameType;
import com.microsoft.exchange.types.DistinguishedFolderIdType;
import com.microsoft.exchange.types.ExtendedPropertyType;
import com.microsoft.exchange.types.FieldOrderType;
import com.microsoft.exchange.types.FieldURIOrConstantType;
import com.microsoft.exchange.types.FolderChangeDescriptionType;
import com.microsoft.exchange.types.FolderChangeType;
import com.microsoft.exchange.types.FolderIdType;
import com.microsoft.exchange.types.FolderQueryTraversalType;
import com.microsoft.exchange.types.FolderResponseShapeType;
import com.microsoft.exchange.types.FolderType;
import com.microsoft.exchange.types.FreeBusyViewOptions;
import com.microsoft.exchange.types.IndexBasePointType;
import com.microsoft.exchange.types.IndexedPageViewType;
import com.microsoft.exchange.types.IsGreaterThanOrEqualToType;
import com.microsoft.exchange.types.IsLessThanOrEqualToType;
import com.microsoft.exchange.types.ItemIdType;
import com.microsoft.exchange.types.ItemQueryTraversalType;
import com.microsoft.exchange.types.ItemResponseShapeType;
import com.microsoft.exchange.types.ItemType;
import com.microsoft.exchange.types.MailboxData;
import com.microsoft.exchange.types.MessageDispositionType;
import com.microsoft.exchange.types.NonEmptyArrayOfAllItemsType;
import com.microsoft.exchange.types.NonEmptyArrayOfBaseFolderIdsType;
import com.microsoft.exchange.types.NonEmptyArrayOfBaseItemIdsType;
import com.microsoft.exchange.types.NonEmptyArrayOfFieldOrdersType;
import com.microsoft.exchange.types.NonEmptyArrayOfFolderChangeDescriptionsType;
import com.microsoft.exchange.types.NonEmptyArrayOfFolderChangesType;
import com.microsoft.exchange.types.NonEmptyArrayOfFoldersType;
import com.microsoft.exchange.types.NonEmptyArrayOfPathsToElementType;
import com.microsoft.exchange.types.ObjectFactory;
import com.microsoft.exchange.types.PathToExtendedFieldType;
import com.microsoft.exchange.types.PathToUnindexedFieldType;
import com.microsoft.exchange.types.PermissionActionType;
import com.microsoft.exchange.types.ResolveNamesSearchScopeType;
import com.microsoft.exchange.types.RestrictionType;
import com.microsoft.exchange.types.SearchFolderTraversalType;
import com.microsoft.exchange.types.SearchFolderType;
import com.microsoft.exchange.types.SearchParametersType;
import com.microsoft.exchange.types.SetFolderFieldType;
import com.microsoft.exchange.types.SortDirectionType;
import com.microsoft.exchange.types.SuggestionsViewOptions;
import com.microsoft.exchange.types.TargetFolderIdType;
import com.microsoft.exchange.types.TasksFolderType;
import com.microsoft.exchange.types.TimeZone;
import com.microsoft.exchange.types.UnindexedFieldURIType;
import com.microsoft.exchange.types.UserConfigurationNameType;
import com.microsoft.exchange.types.UserIdType;

public class ExchangeRequestFactory {

	protected final Log log = LogFactory.getLog(this.getClass());
	private static final int INIT_BASE_OFFSET = 0;
	
	
	/**
	 * @see <a href="http://msdn.microsoft.com/en-us/library/office/jj945066(v=exchg.150).aspx">EWS throttling in Exchange</a>
	 */
	private static final int EWSFindCountLimit = 1000;
	
	private int maxFindItems = 500;

	public int getMaxFindItems() {
		return maxFindItems;
	}

	public void setMaxFindItems(int maxFindItems) {
		this.maxFindItems = maxFindItems;
	}

	public EmptyFolder constructEmptyFolder(boolean deleteSubFolders, DisposalType disposalType, Collection<? extends BaseFolderIdType> folderIds){
		EmptyFolder request = new EmptyFolder();
		request.setDeleteSubFolders(deleteSubFolders);
		request.setDeleteType(disposalType);
		NonEmptyArrayOfBaseFolderIdsType nonEmptyArrayOfBaseFolderIds = new NonEmptyArrayOfBaseFolderIdsType();
		nonEmptyArrayOfBaseFolderIds.getFolderIdsAndDistinguishedFolderIds().addAll(folderIds);
		request.setFolderIds(nonEmptyArrayOfBaseFolderIds);
		return request;
	}
	
	public GetServerTimeZones constructGetServerTimeZones(String tzid, boolean returnFullTimeZoneData){
		GetServerTimeZones request = new GetServerTimeZones();
		if(StringUtils.isNotBlank(tzid)){
			request.getIds().getIds().add(tzid);
		}
		
		request.setReturnFullTimeZoneData(returnFullTimeZoneData);
		return request;
	}
	
	public ResolveNames constructResolveNames(String alias) {
		return constructResolveNames(alias, true, ResolveNamesSearchScopeType.ACTIVE_DIRECTORY_CONTACTS, DefaultShapeNamesType.ALL_PROPERTIES);
	}

	private ResolveNames constructResolveNames(String alias, boolean returnFullContactData, ResolveNamesSearchScopeType searchScope, DefaultShapeNamesType contactDataShape) {
		ResolveNames resolveNames = new ResolveNames();
		
		resolveNames.setContactDataShape(contactDataShape);
		
		resolveNames.setReturnFullContactData(returnFullContactData);
		
		resolveNames.setSearchScope(searchScope);
		
		resolveNames.setUnresolvedEntry(alias);
		
		return resolveNames;
		
	}
	
	public GetUserConfiguration constructGetUserConfiguration(String name,
			DistinguishedFolderIdType distinguishedFolderIdType) {
		GetUserConfiguration getUserConfiguration = new GetUserConfiguration();
		UserConfigurationNameType userConfigurationNameType = new UserConfigurationNameType();
		userConfigurationNameType
				.setDistinguishedFolderId(distinguishedFolderIdType);
		userConfigurationNameType.setName(name);
		getUserConfiguration
				.setUserConfigurationName(userConfigurationNameType);
		return getUserConfiguration;
	}

	public GetUserAvailabilityRequest constructGetUserAvailabilityRequest(Collection<? extends MailboxData> mailboxData, FreeBusyViewOptions freeBusyView, SuggestionsViewOptions suggestionsView, TimeZone timeZone){
		GetUserAvailabilityRequest request = new GetUserAvailabilityRequest();
		
		if(!CollectionUtils.isEmpty(mailboxData)){
			ArrayOfMailboxData arrayOfMailboxData = new ArrayOfMailboxData();
			arrayOfMailboxData.getMailboxDatas().addAll(mailboxData);
			request.setMailboxDataArray(arrayOfMailboxData);
		}
		if(null != suggestionsView){
			request.setSuggestionsViewOptions(suggestionsView);
		}
		
		if(null != freeBusyView){
			request.setFreeBusyViewOptions(freeBusyView);
		}
		
		if(null != timeZone){
			request.setTimeZone(timeZone);
		}
		
		return request;
	}
	
	public CreateItem constructCreateCalendarItem(
			List<? extends ItemType> list,
			CalendarItemCreateOrDeleteOperationType sendTo,
			FolderIdType folderIdType) {
		DistinguishedFolderIdNameType parent = DistinguishedFolderIdNameType.CALENDAR;
		return constructCreateItem(list, parent, sendTo, folderIdType);
	}

	public CreateItem constructCreateCalendarItem(Set<? extends ItemType> set,
			CalendarItemCreateOrDeleteOperationType sendTo,
			FolderIdType folderIdType) {
		DistinguishedFolderIdNameType parent = DistinguishedFolderIdNameType.CALENDAR;
		return constructCreateItem(set, parent, sendTo, folderIdType);
	}
	
	public CreateItem constructCreateCalendarItem(Set<? extends ItemType> set,
			FolderIdType folderIdType) {
		DistinguishedFolderIdNameType parent = DistinguishedFolderIdNameType.CALENDAR;
		return constructCreateItem(set, parent, null, folderIdType);
	}

	public CreateItem constructCreateTaskItem(List<? extends ItemType> list,
			FolderIdType folderIdType) {
		DistinguishedFolderIdNameType parent = DistinguishedFolderIdNameType.TASKS;
		return constructCreateItem(list, parent, null, folderIdType);
	}

	public CreateItem constructCreateMessageItem(List<? extends ItemType> list,
			FolderIdType folderIdType) {
		DistinguishedFolderIdNameType parent = DistinguishedFolderIdNameType.INBOX;
		MessageDispositionType disposition = MessageDispositionType.SEND_ONLY;
		CalendarItemCreateOrDeleteOperationType sendTo = CalendarItemCreateOrDeleteOperationType.SEND_ONLY_TO_ALL;
		return constructCreateItem(list, parent, disposition, sendTo,
				folderIdType);
	}
	
	public CreateItem constructCreateAcceptItem(ItemIdType itemId) {
		CreateItem request = new CreateItem();
		request.setMessageDisposition(MessageDispositionType.SEND_AND_SAVE_COPY);
		NonEmptyArrayOfAllItemsType arrayOfItems = new NonEmptyArrayOfAllItemsType();
		
		AcceptItemType acceptItem = new AcceptItemType();
		acceptItem.setReferenceItemId(itemId);
		
		arrayOfItems.getItemsAndMessagesAndCalendarItems().add(acceptItem);
		request.setItems(arrayOfItems);
		return request;
	}

	private CreateItem constructCreateItem(List<? extends ItemType> list,
			DistinguishedFolderIdNameType parent,
			MessageDispositionType dispositionType,
			CalendarItemCreateOrDeleteOperationType sendTo,
			FolderIdType folderIdType) {

		CreateItem request = new CreateItem();

		NonEmptyArrayOfAllItemsType arrayOfItems = new NonEmptyArrayOfAllItemsType();
		arrayOfItems.getItemsAndMessagesAndCalendarItems().addAll(list);

		request.setItems(arrayOfItems);

		// When the MessageDispositionType is used for the CreateItemType, it
		// only applies to e-mail messages.
		if (null != dispositionType) {
			request.setMessageDisposition(dispositionType);
		}

		TargetFolderIdType tagetFolderId = new TargetFolderIdType();
		if (folderIdType == null || folderIdType.getId() == null
				|| StringUtils.isBlank(folderIdType.getId())) {
			DistinguishedFolderIdType parentDistinguishedFolderId = getParentDistinguishedFolderId(parent);
			log.debug("calendarId is null or empty. tagetFolderId = "
					+ parentDistinguishedFolderId);
			tagetFolderId.setDistinguishedFolderId(parentDistinguishedFolderId);
		} else {
			// don't set changeKey on create as it may have changed in a prior
			// operation
			FolderIdType fIdType = new FolderIdType();
			fIdType.setId(folderIdType.getId());
			tagetFolderId.setFolderId(fIdType);
		}
		request.setSavedItemFolderId(tagetFolderId);

		if (null != sendTo) {
			request.setSendMeetingInvitations(sendTo);
		}

		return request;
	}

	private CreateItem constructCreateItem(Set<? extends ItemType> list,
			DistinguishedFolderIdNameType parent,
			MessageDispositionType dispositionType,
			CalendarItemCreateOrDeleteOperationType sendTo,
			FolderIdType folderIdType) {

		CreateItem request = new CreateItem();

		NonEmptyArrayOfAllItemsType arrayOfItems = new NonEmptyArrayOfAllItemsType();
		arrayOfItems.getItemsAndMessagesAndCalendarItems().addAll(list);

		request.setItems(arrayOfItems);

		// When the MessageDispositionType is used for the CreateItemType, it
		// only applies to e-mail messages.
		if (null != dispositionType) {
			request.setMessageDisposition(dispositionType);
		}

		TargetFolderIdType tagetFolderId = new TargetFolderIdType();
		if (folderIdType == null || folderIdType.getId() == null
				|| StringUtils.isBlank(folderIdType.getId())) {
			DistinguishedFolderIdType parentDistinguishedFolderId = getParentDistinguishedFolderId(parent);
			log.debug("calendarId is null or empty. tagetFolderId = "
					+ parentDistinguishedFolderId);
			tagetFolderId.setDistinguishedFolderId(parentDistinguishedFolderId);
		} else {
			// don't set changeKey on create as it may have changed in a prior
			// operation
			FolderIdType fIdType = new FolderIdType();
			fIdType.setId(folderIdType.getId());
			tagetFolderId.setFolderId(fIdType);
		}
		request.setSavedItemFolderId(tagetFolderId);

		if (null != sendTo) {
			request.setSendMeetingInvitations(sendTo);
		}

		return request;
	}

	private CreateItem constructCreateItem(List<? extends ItemType> list,
			DistinguishedFolderIdNameType parent,
			CalendarItemCreateOrDeleteOperationType sendTo,
			FolderIdType folderIdType) {

		return constructCreateItem(list, parent, null, sendTo, folderIdType);
	}

	private CreateItem constructCreateItem(Set<? extends ItemType> set,
			DistinguishedFolderIdNameType parent,
			CalendarItemCreateOrDeleteOperationType sendTo,
			FolderIdType folderIdType) {

		return constructCreateItem(set, parent, null, sendTo, folderIdType);
	}

	/**
	 * FOLDER OPERATIONS
	 * 
	 * @param searchRoot
	 * @param restriction
	 * @param displayName
	 */

	public CreateFolder constructCreateSearchFolder(String displayName,
			DistinguishedFolderIdNameType searchRoot,
			RestrictionType restriction) {
		CreateFolder createFolder = new CreateFolder();

		// create new searchFolderType
		SearchFolderType searchFolderType = new SearchFolderType();

		// create search parameters
		SearchParametersType searchParameters = new SearchParametersType();

		// search folders recursively
		searchParameters.setTraversal(SearchFolderTraversalType.DEEP);

		NonEmptyArrayOfBaseFolderIdsType baseFolderIds = new NonEmptyArrayOfBaseFolderIdsType();
		baseFolderIds.getFolderIdsAndDistinguishedFolderIds().add(
				getParentDistinguishedFolderId(searchRoot));

		// set the baase of the search
		searchParameters.setBaseFolderIds(baseFolderIds);

		// set the search restriction
		searchParameters.setRestriction(restriction);

		// add search parameters to folder
		searchFolderType.setSearchParameters(searchParameters);

		// set the search folder display name
		searchFolderType.setDisplayName(displayName);

		// add searchFolder to CreatFolder request
		NonEmptyArrayOfFoldersType nonEmptyArrayOfFoldersType = new NonEmptyArrayOfFoldersType();
		nonEmptyArrayOfFoldersType
				.getFoldersAndCalendarFoldersAndContactsFolders().add(
						searchFolderType);
		createFolder.setFolders(nonEmptyArrayOfFoldersType);

		createFolder
				.setParentFolderId(getParentTargetFolderId(DistinguishedFolderIdNameType.SEARCHFOLDERS));

		return createFolder;
	}
	
	public CreateFolder constructCreateCalendarFolder(String displayName,
			Collection<ExtendedPropertyType> exProps) {
		Validate.isTrue(StringUtils.isNotBlank(displayName),"displayName argument cannot be empty");
		BaseFolderType c = new CalendarFolderType();
		c.setDisplayName(displayName);
		if(!CollectionUtils.isEmpty(exProps)){
			c.getExtendedProperties().addAll(exProps);
		}
		return constructCreateFolder(DistinguishedFolderIdNameType.CALENDAR, c);
	}

	public CreateFolder constructCreateTaskFolder(String displayName,
			Collection<ExtendedPropertyType> exProps) {
		Validate.isTrue(StringUtils.isNotBlank(displayName),"displayName argument cannot be empty");
		BaseFolderType c = new TasksFolderType();
		c.setDisplayName(displayName);
		if(!CollectionUtils.isEmpty(exProps)){
			c.getExtendedProperties().addAll(exProps);
		}
		return constructCreateFolder(DistinguishedFolderIdNameType.TASKS, c);
	}

	/**
	 * Attempt to create a calendar group i.e. a folder that may contain a number of sub calendars.
	 * I don't think you can create a calendar group using EWS
	 * @param upn
	 * @param displayName
	 * @return
	 */
	@Deprecated
	public CreateFolder constructCreateCalendarFolderGroup(String upn,
			String displayName) {
		CalendarFolderType calendarFolderType = new CalendarFolderType();
		calendarFolderType.setDisplayName(displayName);
		CalendarPermissionType calendarPermissionType = new CalendarPermissionType();
		UserIdType userId = new UserIdType();
		userId.setPrimarySmtpAddress(upn);

		calendarPermissionType.setUserId(userId);
		calendarPermissionType.setCanCreateSubFolders(true);
		calendarPermissionType.setIsFolderOwner(true);
		calendarPermissionType.setIsFolderContact(true);
		calendarPermissionType.setIsFolderVisible(true);
		calendarPermissionType.setEditItems(PermissionActionType.ALL);
		calendarPermissionType.setDeleteItems(PermissionActionType.ALL);
		calendarPermissionType
				.setReadItems(CalendarPermissionReadAccessType.FULL_DETAILS);
		calendarPermissionType
				.setCalendarPermissionLevel(CalendarPermissionLevelType.OWNER);

		ArrayOfCalendarPermissionsType arrayOfCalendarPermissionsType = new ArrayOfCalendarPermissionsType();
		arrayOfCalendarPermissionsType.getCalendarPermissions().add(
				calendarPermissionType);

		CalendarPermissionSetType calendarPermissionSetType = new CalendarPermissionSetType();
		calendarPermissionSetType
				.setCalendarPermissions(arrayOfCalendarPermissionsType);

		calendarFolderType.setPermissionSet(calendarPermissionSetType);
		return constructCreateFolder(DistinguishedFolderIdNameType.CALENDAR,
				calendarFolderType);
	}

	public CreateFolder constructCreateFolder(
			DistinguishedFolderIdNameType parent, BaseFolderType folder) {
		return constructCreateFolder(parent, Collections.singletonList(folder));
	}

	public CreateFolder constructCreateFolder(
			DistinguishedFolderIdNameType parent,
			Collection<? extends BaseFolderType> folders) {
		CreateFolder createFolder = new CreateFolder();
		TargetFolderIdType parentTargetFolderId = getParentTargetFolderId(parent);
		createFolder.setParentFolderId(parentTargetFolderId);

		NonEmptyArrayOfFoldersType folderArray = new NonEmptyArrayOfFoldersType();
		folderArray.getFoldersAndCalendarFoldersAndContactsFolders().addAll(
				folders);

		createFolder.setFolders(folderArray);
		return createFolder;
	}

	public CreateFolder constructCreateFolder(FolderIdType folderIdType,
			BaseFolderType folder) {
		CreateFolder createFolder = new CreateFolder();

		TargetFolderIdType targetFolderIdType = new TargetFolderIdType();
		targetFolderIdType.setFolderId(folderIdType);
		createFolder.setParentFolderId(targetFolderIdType);

		NonEmptyArrayOfFoldersType folderArray = new NonEmptyArrayOfFoldersType();
		folderArray.getFoldersAndCalendarFoldersAndContactsFolders()
				.add(folder);

		createFolder.setFolders(folderArray);
		return createFolder;
	}

	public GetFolder constructGetFolderByName(
			DistinguishedFolderIdNameType parent) {
		DistinguishedFolderIdType parentDistinguishedFolderId = getParentDistinguishedFolderId(parent);
		return constructGetFolderById(parentDistinguishedFolderId);
	}

	public GetFolder constructGetFolderById(BaseFolderIdType folderIdType) {
		GetFolder getFolder = new GetFolder();
		NonEmptyArrayOfBaseFolderIdsType foldersArray = new NonEmptyArrayOfBaseFolderIdsType();
		foldersArray.getFolderIdsAndDistinguishedFolderIds().add(folderIdType);
		getFolder.setFolderIds(foldersArray);
		FolderResponseShapeType responseShape = new FolderResponseShapeType();
		responseShape.setBaseShape(DefaultShapeNamesType.ALL_PROPERTIES);
		getFolder.setFolderShape(responseShape);
		return getFolder;
	}


	public FindFolder constructFindFolder(DistinguishedFolderIdNameType parent, DefaultShapeNamesType folderShape, FolderQueryTraversalType folderQueryTraversalType) {
		return constructFindFolder(parent, folderShape, folderQueryTraversalType, null);
	}
	
	public FindFolder constructFindFolder(DistinguishedFolderIdNameType parent,
			DefaultShapeNamesType folderShape,
			FolderQueryTraversalType folderQueryTraversalType,
			RestrictionType restriction) {
		Validate.notNull(parent, "parent cannot be null");
		Validate.notNull(folderQueryTraversalType,
				"traversal type cannot be null");
		Validate.notNull(folderShape, "baseShape cannot be null");

		FindFolder findFolder = new FindFolder();

		findFolder.setTraversal(folderQueryTraversalType);

		FolderResponseShapeType responseShape = new FolderResponseShapeType();
		responseShape.setBaseShape(folderShape);
		findFolder.setFolderShape(responseShape);

		IndexedPageViewType pageView = constructIndexedPageView(INIT_BASE_OFFSET, EWSFindCountLimit, false);
		findFolder.setIndexedPageFolderView(pageView);

		DistinguishedFolderIdType parentDistinguishedFolderId = getParentDistinguishedFolderId(parent);
		NonEmptyArrayOfBaseFolderIdsType array = new NonEmptyArrayOfBaseFolderIdsType();
		array.getFolderIdsAndDistinguishedFolderIds().add(
				parentDistinguishedFolderId);
		findFolder.setParentFolderIds(array);

		if (null != restriction) {
			findFolder.setRestriction(restriction);
		}

		return findFolder;
	}

	public UpdateFolder constructRenameFolder(String newName,
			FolderIdType folderId) {
		FolderType folder = new FolderType();
		folder.setDisplayName(newName);

		PathToUnindexedFieldType path = new PathToUnindexedFieldType();
		path.setFieldURI(UnindexedFieldURIType.FOLDER_DISPLAY_NAME);

		ObjectFactory of = new ObjectFactory();

		return constructUpdateFolderSetField(folder, of.createPath(path),
				folderId);
	}

	protected UpdateFolder constructUpdateFolderDeleteExtendedProperty(
			FolderIdType folderId, ExtendedPropertyType exProp) {
		return constructUpdateFolderDeleteField(
				getPathForExtendedPropertyType(exProp), folderId);
	}

	protected UpdateFolder constructUpdateFolderSetField(FolderType folder,
			JAXBElement<? extends BasePathToElementType> path,
			FolderIdType folderId) {
		SetFolderFieldType changeDescription = new SetFolderFieldType();
		changeDescription.setFolder(folder);
		changeDescription.setPath(path);
		return constructUpdateFolderInternal(changeDescription, folderId);
	}

	protected UpdateFolder constructUpdateFolderDeleteField(
			JAXBElement<? extends BasePathToElementType> path,
			FolderIdType folderId) {
		DeleteFolderFieldType changeDescription = new DeleteFolderFieldType();
		changeDescription.setPath(path);
		return constructUpdateFolderInternal(changeDescription, folderId);
	}

	private UpdateFolder constructUpdateFolderInternal(
			FolderChangeDescriptionType changeDescription, FolderIdType folderId) {

		NonEmptyArrayOfFolderChangeDescriptionsType folderUpdates = new NonEmptyArrayOfFolderChangeDescriptionsType();
		folderUpdates
				.getAppendToFolderFieldsAndSetFolderFieldsAndDeleteFolderFields()
				.add(changeDescription);

		FolderChangeType folderChange = new FolderChangeType();
		folderChange.setFolderId(folderId);
		folderChange.setUpdates(folderUpdates);

		NonEmptyArrayOfFolderChangesType changes = new NonEmptyArrayOfFolderChangesType();
		changes.getFolderChanges().add(folderChange);

		UpdateFolder updateRequest = new UpdateFolder();
		updateRequest.setFolderChanges(changes);

		return updateRequest;
	}

	public GetItem constructGetItemIds(Collection<ItemIdType> itemIds) {
		Validate.isTrue(!CollectionUtils.isEmpty(itemIds),"itemIds cannot be empty");
		Set<PathToExtendedFieldType> extendedPropertyPaths = getExtendedPropertyPaths();
		ItemResponseShapeType responseShape = constructTextResponseShape(DefaultShapeNamesType.ID_ONLY, extendedPropertyPaths);
		GetItem getItem = constructGetItem(itemIds, responseShape);
		return getItem;
	}
	
	public GetItem constructGetItems(Collection<ItemIdType> itemIds) {
		Validate.isTrue(!CollectionUtils.isEmpty(itemIds),"itemIds cannot be empty");
		Set<PathToExtendedFieldType> extendedPropertyPaths = getExtendedPropertyPaths();
		ItemResponseShapeType responseShape = constructTextResponseShape(DefaultShapeNamesType.ALL_PROPERTIES, extendedPropertyPaths);
		GetItem getItem = constructGetItem(itemIds, responseShape);
		return getItem;
	}
	
	/**
	 * public GetItem constructGetItems
	 * 
	 * @param itemIds
	 * @param responseShape
	 * @return
	 */
	protected GetItem constructGetItem(Collection<ItemIdType> itemIds,
			ItemResponseShapeType responseShape) {
		GetItem getItem = new GetItem();

		NonEmptyArrayOfBaseItemIdsType itemIdArray = new NonEmptyArrayOfBaseItemIdsType();
		itemIdArray.getItemIdsAndOccurrenceItemIdsAndRecurringMasterItemIds()
				.addAll(itemIds);
		getItem.setItemIds(itemIdArray);

		getItem.setItemShape(responseShape);
		return getItem;
	}

	/**
	 * public DeleteItem constructDeleteItem
	 * 
	 * @param itemIds
	 *            - Contains an array of items, occurrence items, and recurring
	 *            master items to delete from a mailbox in the Exchange store.
	 *            The DeleteItem Operation can be performed on any item type
	 * @param disposalType
	 *            - Describes how an item is deleted. This attribute is
	 *            required.
	 * @param sendTo
	 *            - Describes whether a calendar item deletion is communicated
	 *            to attendees. This attribute is required when calendar items
	 *            are deleted. This attribute is optional if non-calendar items
	 *            are deleted.
	 * @param affectedTaskOccurrencesType
	 *            - Describes whether a task instance or a task master is
	 *            deleted by a DeleteItem Operation. This attribute is required
	 *            when tasks are deleted. This attribute is optional when
	 *            non-task items are deleted.
	 * @return
	 */
	protected DeleteItem constructDeleteItem(
			Collection<? extends BaseItemIdType> itemIds,
			DisposalType disposalType,
			CalendarItemCreateOrDeleteOperationType sendTo,
			AffectedTaskOccurrencesType affectedTaskOccurrencesType) {
		Validate.notEmpty(itemIds, "must specify at least one itemId.");
		Validate.notNull(disposalType, "disposalType cannot be null");
		DeleteItem deleteItem = new DeleteItem();

		if (null != affectedTaskOccurrencesType) {
			deleteItem.setAffectedTaskOccurrences(affectedTaskOccurrencesType);
		}
		deleteItem.setDeleteType(disposalType);

		NonEmptyArrayOfBaseItemIdsType arrayOfItemIds = new NonEmptyArrayOfBaseItemIdsType();
		arrayOfItemIds
				.getItemIdsAndOccurrenceItemIdsAndRecurringMasterItemIds()
				.addAll(itemIds);
		deleteItem.setItemIds(arrayOfItemIds);

		if (null != sendTo) {
			deleteItem.setSendMeetingCancellations(sendTo);
		}
		return deleteItem;
	}

	public DeleteItem constructDeleteCalendarItems(
			Collection<? extends BaseItemIdType> itemIds,
			DisposalType disposalType,
			CalendarItemCreateOrDeleteOperationType sendTo) {
		Validate.notNull(sendTo, "sendTo must be specified");
		return constructDeleteItem(itemIds, disposalType, sendTo, null);
	}

	public DeleteItem constructDeleteCalendarItem(BaseItemIdType itemId,
			DisposalType disposalType,
			CalendarItemCreateOrDeleteOperationType sendTo) {
		return constructDeleteCalendarItems(Collections.singletonList(itemId),
				disposalType, sendTo);
	}

	public DeleteItem constructDeleteTaskItems(
			Collection<? extends BaseItemIdType> itemIds,
			DisposalType disposalType,
			AffectedTaskOccurrencesType affectedTaskOccurrencesType) {
		Validate.notNull(affectedTaskOccurrencesType,
				"affectedTaskOccurrencesType must be specified");
		return constructDeleteItem(itemIds, disposalType, null,
				affectedTaskOccurrencesType);
	}

	public DeleteFolder constructDeleteFolder(BaseFolderIdType folderId, DisposalType disposalType) {
		return constructDeleteFolder(Collections.singleton(folderId), disposalType);
	}
	
	public DeleteFolder constructDeleteFolder(
			Collection<? extends BaseFolderIdType> folderIds,
			DisposalType disposalType) {
		Validate.notEmpty(folderIds, "folderIds cannot be empty");
		DeleteFolder deleteFolder = new DeleteFolder();
		deleteFolder.setDeleteType(disposalType);

		NonEmptyArrayOfBaseFolderIdsType folderIdArray = new NonEmptyArrayOfBaseFolderIdsType();
		folderIdArray.getFolderIdsAndDistinguishedFolderIds().addAll(folderIds);
		deleteFolder.setFolderIds(folderIdArray);

		return deleteFolder;
	}


	/**
	 * 
	 * FindItem operations
	 * 
	 * 
	 * @param view
	 * @param responseShape
	 * @param traversal
	 * 
	 *            Shallow - Instructs the FindFolder operation to search only
	 *            the identified folder and to return only the folder IDs for
	 *            items that have not been deleted. This is called a shallow
	 *            traversal. Deep - Instructs the FindFolder operation to search
	 *            in all child folders of the identified parent folder and to
	 *            return only the folder IDs for items that have not been
	 *            deleted. This is called a deep traversal. SoftDeleted -
	 *            Instructs the FindFolder operation to perform a shallow
	 *            traversal search for deleted items.
	 * 
	 * @param restriction
	 * @param sortOrderList
	 * @param folderIds
	 * @return
	 */
	protected FindItem constructIndexedPageViewFindItem(
			IndexedPageViewType view, ItemResponseShapeType responseShape,
			ItemQueryTraversalType traversal, RestrictionType restriction,
			Collection<FieldOrderType> sortOrderList,
			Collection<? extends BaseFolderIdType> folderIds) {
		FindItem findItem = new FindItem();

		findItem.setIndexedPageItemView(view);
		findItem.setItemShape(responseShape);
		findItem.setTraversal(traversal);

		if (null != restriction) {
			findItem.setRestriction(restriction);
		}

		if (!CollectionUtils.isEmpty(sortOrderList)) {
			NonEmptyArrayOfFieldOrdersType sortOrder = new NonEmptyArrayOfFieldOrdersType();
			sortOrder.getFieldOrders().addAll(sortOrderList);
			findItem.setSortOrder(sortOrder);
		}

		if (!CollectionUtils.isEmpty(folderIds)) {
			NonEmptyArrayOfBaseFolderIdsType parentFolderIds = new NonEmptyArrayOfBaseFolderIdsType();
			parentFolderIds.getFolderIdsAndDistinguishedFolderIds().addAll(
					folderIds);
			findItem.setParentFolderIds(parentFolderIds);
		}

		return findItem;
	}
	
	protected FindItem constructCalendarViewFindItem(Date startTime, Date endTime, ItemResponseShapeType responseShape,	ItemQueryTraversalType traversal,Collection<? extends BaseFolderIdType> folderIds) {
		
		FindItem findItem = new FindItem();
		findItem.setCalendarView(constructCalendarView(startTime, endTime));
		findItem.setItemShape(responseShape);
		findItem.setTraversal(traversal);
		NonEmptyArrayOfBaseFolderIdsType array = new NonEmptyArrayOfBaseFolderIdsType();
		array.getFolderIdsAndDistinguishedFolderIds().addAll(folderIds);
		findItem.setParentFolderIds(array);
		
		return findItem;
		
	}

	@Deprecated
	protected FindItem constructCalendarViewFindItem(Date startTime,
			Date endTime, ItemResponseShapeType responseShape,
			ItemQueryTraversalType traversal, RestrictionType restriction,
			Collection<FieldOrderType> sortOrderList,
			Collection<? extends BaseFolderIdType> folderIds) {

		log.warn("Restrictions and sort order may not be specified for a CalendarView AND WILL BE OMITTED FROM THIS REQUEST!!!");
		return constructCalendarViewFindItem(startTime,endTime,responseShape,traversal,folderIds);
	}

	public FindItem constructCalendarViewFindItem(
			CalendarViewType calendarView, ItemResponseShapeType responseShape,
			ItemQueryTraversalType traversal, RestrictionType restriction,
			List<FieldOrderType> sortOrderList) {

		FindItem findItem = new FindItem();
		findItem.setCalendarView(calendarView);
		findItem.setItemShape(responseShape);
		findItem.setTraversal(traversal);
		NonEmptyArrayOfFieldOrdersType sortOrder = new NonEmptyArrayOfFieldOrdersType();
		for (FieldOrderType fot : sortOrderList) {
			sortOrder.getFieldOrders().add(fot);
		}
		findItem.setSortOrder(sortOrder);

		return findItem;
	}

	/**
	 * see: http://msdn.microsoft.com/en-us/library/aa564515(v=exchg.140).aspx
	 * 
	 * @param startTime
	 * @param endTime
	 * @return
	 */
	public CalendarViewType constructCalendarView(Date startTime, Date endTime) {
		CalendarViewType calendarView = new CalendarViewType();
		calendarView.setMaxEntriesReturned(getMaxFindItems());
		calendarView.setStartDate(DateHelp
				.convertDateToXMLGregorianCalendar(startTime));
		calendarView.setEndDate(DateHelp
				.convertDateToXMLGregorianCalendar(endTime));

		return calendarView;
	}

	public IndexedPageViewType constructIndexedPageView(Integer start,
			Integer length, Boolean reverse) {
		IndexedPageViewType view = new IndexedPageViewType();
		view.setMaxEntriesReturned(length);
		view.setOffset(start);
		if (reverse) {
			view.setBasePoint(IndexBasePointType.END);
		} else {
			view.setBasePoint(IndexBasePointType.BEGINNING);
		}
		return view;
	}

	/**
	 * ItemResponseShapeType
	 * 
	 * @param baseShape
	 *            -DefaultShapeNamesType.ALL_PROPERTIES;
	 *            -DefaultShapeNamesType.DEFAULT;
	 *            -DefaultShapeNamesType.ID_ONLY;
	 * 
	 * @param bodyType
	 * @param htmlToUtf8
	 * @param filterHtml
	 * @param includeMime
	 * @param exProps
	 * @return
	 */
	public ItemResponseShapeType constructResponseShapeExProps(
			DefaultShapeNamesType baseShape, BodyTypeResponseType bodyType,
			Boolean htmlToUtf8, Boolean filterHtml, Boolean includeMime,
			Collection<ExtendedPropertyType> exProps) {

		ItemResponseShapeType responseShape = new ItemResponseShapeType();
		responseShape.setBaseShape(baseShape);
		if (null != bodyType) {
			responseShape.setBodyType(bodyType);
		}
		if (null != htmlToUtf8) {
			responseShape.setConvertHtmlCodePageToUTF8(htmlToUtf8);
		}
		if (null != filterHtml) {
			responseShape.setFilterHtmlContent(filterHtml);
		}
		if (null != includeMime) {
			responseShape.setIncludeMimeContent(includeMime);
		}
		if (null != exProps) {
			responseShape
					.setAdditionalProperties(getPathsFromExtendedProps(exProps));
		}
		return responseShape;
	}

	
	public ItemResponseShapeType constructResponseShape(
			DefaultShapeNamesType baseShape, BodyTypeResponseType bodyType,
			Boolean htmlToUtf8, Boolean filterHtml, Boolean includeMime,
			Collection<PathToExtendedFieldType> exPaths) {

		NonEmptyArrayOfPathsToElementType additionalProperties = new NonEmptyArrayOfPathsToElementType();
		if (!CollectionUtils.isEmpty(exPaths)) {
			ObjectFactory of = new ObjectFactory();
			
			for (PathToExtendedFieldType p : exPaths) {
				JAXBElement<PathToExtendedFieldType> exFieldUri = of
						.createExtendedFieldURI(p);
				additionalProperties.getPaths().add(exFieldUri);
			}
		}
		return constructResponseShape(baseShape, bodyType, htmlToUtf8, filterHtml, includeMime, additionalProperties);
	}

	public ItemResponseShapeType constructResponseShape(
			DefaultShapeNamesType baseShape, BodyTypeResponseType bodyType,
			Boolean htmlToUtf8, Boolean filterHtml, Boolean includeMime,
			NonEmptyArrayOfPathsToElementType exProps) {

		ItemResponseShapeType responseShape = new ItemResponseShapeType();
		responseShape.setBaseShape(baseShape);
		if (null != bodyType) {
			responseShape.setBodyType(bodyType);
		}
		if (null != htmlToUtf8) {
			responseShape.setConvertHtmlCodePageToUTF8(htmlToUtf8);
		}
		if (null != filterHtml) {
			responseShape.setFilterHtmlContent(filterHtml);
		}
		if (null != includeMime) {
			responseShape.setIncludeMimeContent(includeMime);
		}
		if (null != exProps && !CollectionUtils.isEmpty(exProps.getPaths()) ) {
			responseShape.setAdditionalProperties(exProps);
		}
		return responseShape;
	}

	private NonEmptyArrayOfPathsToElementType getPathsFromExtendedProps(
			Collection<ExtendedPropertyType> exProps) {
		NonEmptyArrayOfPathsToElementType paths = new NonEmptyArrayOfPathsToElementType();
		for (ExtendedPropertyType extendedPropertyType : exProps) {
			paths.getPaths().add(
					getPathForExtendedPropertyType(extendedPropertyType));
		}
		return paths;
	}

	
	
	// TODO one of these should be deprecated.
	public ItemResponseShapeType constructTextResponseShape(DefaultShapeNamesType baseShape,
			NonEmptyArrayOfPathsToElementType exProps) {
		return constructResponseShape(baseShape, BodyTypeResponseType.TEXT, true, true, false, exProps);
	}
	
	public ItemResponseShapeType constructTextResponseShape(DefaultShapeNamesType baseShape,
			Collection<PathToExtendedFieldType> exProps) {
		return constructResponseShape(baseShape, BodyTypeResponseType.TEXT, true, true, false, exProps);
	}
	
	public ItemResponseShapeType constructResponseShape(
			DefaultShapeNamesType baseShape,
			Collection<PathToExtendedFieldType> exPaths) {
		return constructResponseShape(baseShape, null, null, null, null,
				exPaths);
	}

	public ItemResponseShapeType constructResponseShape(
			DefaultShapeNamesType baseShape,
			NonEmptyArrayOfPathsToElementType exProps) {
		return constructResponseShape(baseShape, null, null, null, null,
				exProps);
	}

	public ItemResponseShapeType constructResponseShapeWithExProps(
			DefaultShapeNamesType baseShape,
			Collection<ExtendedPropertyType> exProps) {
		return constructResponseShapeExProps(baseShape, null, null, null, null,
				exProps);
	}

	public ItemResponseShapeType constructResponseShape(
			DefaultShapeNamesType baseShape) {
		Collection<ExtendedPropertyType> exProps = null;
		return constructResponseShapeExProps(baseShape, null, null, null, null,
				exProps);
	}

	/**
	 * PARENT
	 * 
	 * getParentTargetFolderId and getParentDistinguishedFolderId both accept
	 * DistinguishedFolderIdNameType
	 * 
	 * ARCHIVEDELETEDITEMS ARCHIVEMSGFOLDERROOT ARCHIVERECOVERABLEITEMSDELETIONS
	 * ARCHIVERECOVERABLEITEMSPURGES ARCHIVERECOVERABLEITEMSROOT
	 * ARCHIVERECOVERABLEITEMSVERSIONS ARCHIVEROOT CALENDAR CONTACTS
	 * DELETEDITEMS DRAFTS INBOX JOURNAL JUNKEMAIL MSGFOLDERROOT NOTES OUTBOX
	 * PUBLICFOLDERSROOT RECOVERABLEITEMSDELETIONS RECOVERABLEITEMSPURGES
	 * RECOVERABLEITEMSROOT RECOVERABLEITEMSVERSIONS SEARCHFOLDERS SENTITEMS
	 * TASKS VOICEMAIL
	 * 
	 * @param parent
	 * 
	 * @return
	 */
	protected TargetFolderIdType getParentTargetFolderId(
			DistinguishedFolderIdNameType parent) {
		TargetFolderIdType targetFolderIdType = new TargetFolderIdType();
		targetFolderIdType
				.setDistinguishedFolderId(getParentDistinguishedFolderId(parent));
		return targetFolderIdType;
	}

	/**
	 * @param parent
	 * @return
	 */
	protected DistinguishedFolderIdType getParentDistinguishedFolderId(
			DistinguishedFolderIdNameType parent) {
		DistinguishedFolderIdType distinguishedFolderIdType = new DistinguishedFolderIdType();
		distinguishedFolderIdType.setId(parent);
		return distinguishedFolderIdType;
	}

	public DistinguishedFolderIdType getPrimaryCalendarDistinguishedFolderId() {
		return getParentDistinguishedFolderId(DistinguishedFolderIdNameType.CALENDAR);
	}

	public DistinguishedFolderIdType getPrimaryContactsDistinguishedFolderId() {
		return getParentDistinguishedFolderId(DistinguishedFolderIdNameType.CONTACTS);
	}

	public DistinguishedFolderIdType getPrimaryTasksDistinguishedFolderId() {
		return getParentDistinguishedFolderId(DistinguishedFolderIdNameType.TASKS);
	}

	public DistinguishedFolderIdType getPrimaryNotesDistinguishedFolderId() {
		return getParentDistinguishedFolderId(DistinguishedFolderIdNameType.NOTES);
	}

	public DistinguishedFolderIdType getPrimaryJournalDistinguishedFolderId() {
		return getParentDistinguishedFolderId(DistinguishedFolderIdNameType.JOURNAL);
	}

	/**
	 * 
	 * 
	 * @param extendedPropertyType
	 * @return
	 */
	public JAXBElement<PathToExtendedFieldType> getPathForExtendedPropertyType(
			ExtendedPropertyType extendedPropertyType) {
		ObjectFactory objectFactory = new ObjectFactory();
		JAXBElement<PathToExtendedFieldType> extendedFieldURI = objectFactory
				.createExtendedFieldURI(extendedPropertyType
						.getExtendedFieldURI());
		return extendedFieldURI;
	}
	
	protected FindItem constructIndexedPageViewFindFirstItemIdsShallow(RestrictionType restriction,NonEmptyArrayOfPathsToElementType exProps,  Collection<? extends BaseFolderIdType> folderIds) {
		return constructIndexedPageViewFindItemIdsShallow(INIT_BASE_OFFSET, getMaxFindItems(), restriction, exProps, folderIds);
	}
	
	private FindItem constructIndexedPageViewFindItemIdsShallow(int offset, int maxItems,  RestrictionType restriction,NonEmptyArrayOfPathsToElementType exProps,  Collection<? extends BaseFolderIdType> folderIds) {
		return constructIndexedPageViewFindItem(offset, maxItems, DefaultShapeNamesType.ID_ONLY, ItemQueryTraversalType.SHALLOW, restriction, exProps, folderIds);
	}
	
	private FindItem constructIndexedPageViewFindItem(int offset, int maxItems, DefaultShapeNamesType baseShape, ItemQueryTraversalType traversalType, RestrictionType restriction,NonEmptyArrayOfPathsToElementType exProps,  Collection<? extends BaseFolderIdType> folderIds) {
		if(maxItems > EWSFindCountLimit){
			log.warn("The default policy in Exchange limits the page size to 1000 items. Setting the page size to a value that is greater than this number has no practical effect. --http://msdn.microsoft.com/en-us/library/office/jj945066(v=exchg.150).aspx#bk_PolicyParameters");
		}
		//use indexed view as restrictions cannot be applied to calendar view
		IndexedPageViewType view = constructIndexedPageView(offset,maxItems,false);
		
		//only return id,  note you can return a limited set of additional properties
		// 	see:http://msdn.microsoft.com/en-us/library/exchange/aa563810(v=exchg.140).aspx
		ItemResponseShapeType responseShape = constructResponseShape(baseShape,exProps);
		
		
		FieldOrderType sortOrder = constructSortOrder();
		List<FieldOrderType> sortOrderList = Collections.singletonList(sortOrder);
		//FindItem findItem = constructIndexedPageViewFindItem(view, responseShape, ItemQueryTraversalType.ASSOCIATED, restriction, sortOrderList, folderIds);
		
		FindItem findItem = constructIndexedPageViewFindItem(view, responseShape, traversalType, restriction, sortOrderList, folderIds);
		return findItem;
	}
	
	protected FieldOrderType constructSortOrder() {
		ObjectFactory of = getObjectFactory();
		//set sort order (earliest items first)
		FieldOrderType sortOrder = new FieldOrderType();
		sortOrder.setOrder(SortDirectionType.ASCENDING);
		PathToUnindexedFieldType path = new PathToUnindexedFieldType();
		path.setFieldURI(UnindexedFieldURIType.ITEM_ITEM_ID);
		JAXBElement<PathToUnindexedFieldType> sortPath = of.createFieldURI(path);
		sortOrder.setPath(sortPath);
		return sortOrder;
	}
	
	protected ObjectFactory getObjectFactory() {
		ObjectFactory of = new ObjectFactory();
		return of;
	}

	public Set<PathToExtendedFieldType> getExtendedPropertyPaths() {
		return new HashSet<PathToExtendedFieldType>();
	}
	
	public NonEmptyArrayOfPathsToElementType getAdditionalExtendedProperties() {
		NonEmptyArrayOfPathsToElementType aProps = new NonEmptyArrayOfPathsToElementType();
		Set<PathToExtendedFieldType> extendedPropertyPaths = getExtendedPropertyPaths();
		if(!CollectionUtils.isEmpty(extendedPropertyPaths)){
			for(PathToExtendedFieldType p: extendedPropertyPaths) {
				JAXBElement<PathToExtendedFieldType> j = getObjectFactory().createExtendedFieldURI(p);
				aProps.getPaths().add(j);
			}
		}
		return aProps;
	}

	protected JAXBElement<IsLessThanOrEqualToType> getCalendarItemEndRestriction(	Date endTime) {
		ObjectFactory of = getObjectFactory();
		IsLessThanOrEqualToType endType = new IsLessThanOrEqualToType();
		XMLGregorianCalendar end = DateHelp.convertDateToXMLGregorianCalendar(endTime);
		PathToUnindexedFieldType endPath = new PathToUnindexedFieldType();
		endPath.setFieldURI(UnindexedFieldURIType.CALENDAR_END);
		JAXBElement<PathToUnindexedFieldType> endFieldURI = of.createFieldURI(endPath);
		endType.setPath(endFieldURI);
		FieldURIOrConstantType endConstant = new FieldURIOrConstantType();
		ConstantValueType endValue = new ConstantValueType();
		endValue.setValue(end.toXMLFormat());
		endConstant.setConstant(endValue);
		endType.setFieldURIOrConstant(endConstant);
		JAXBElement<IsLessThanOrEqualToType> endSearchExpression = of.createIsLessThanOrEqualTo(endType);
		return endSearchExpression;
	}

	protected JAXBElement<IsGreaterThanOrEqualToType> getCalendarItemStartRestriction(Date startTime) {
		ObjectFactory of = getObjectFactory();
		IsGreaterThanOrEqualToType startType = new IsGreaterThanOrEqualToType();
		XMLGregorianCalendar start = DateHelp.convertDateToXMLGregorianCalendar(startTime);
		PathToUnindexedFieldType startPath = new PathToUnindexedFieldType();
		startPath.setFieldURI(UnindexedFieldURIType.CALENDAR_START);
		JAXBElement<PathToUnindexedFieldType> startFieldURI = of.createFieldURI(startPath);
		startType.setPath(startFieldURI);
		FieldURIOrConstantType startConstant = new FieldURIOrConstantType();
		ConstantValueType startValue = new ConstantValueType();
		startValue.setValue(start.toXMLFormat());
		startConstant.setConstant(startValue);
		startType.setFieldURIOrConstant(startConstant);
		JAXBElement<IsGreaterThanOrEqualToType> startSearchExpression = of.createIsGreaterThanOrEqualTo(startType);
		return startSearchExpression;
	}


	public FindItem constructFindFirstItemIdSet(Collection<FolderIdType> folderIds) {
		return constructFindAllItemIds(INIT_BASE_OFFSET, getMaxFindItems(), folderIds);
	}
	
	public FindItem constructFindNextItemIdSet(int offset, Collection<FolderIdType> folderIds) {
		return constructFindAllItemIds(offset, getMaxFindItems(), folderIds);
	}
	
	public FindItem constructFindAllItemIds(int offset, int maxItems, Collection<FolderIdType> folderIds) {
		//FindAllItems = no restriction
		RestrictionType restriction = null;
		NonEmptyArrayOfPathsToElementType exProps = new NonEmptyArrayOfPathsToElementType();
		return constructIndexedPageViewFindItemIdsShallow(offset, maxItems, restriction, exProps, folderIds);
	}
	
	
	/**
	 * @param startTime
	 * @param endTime
	 * @param of
	 * @return
	 */
	protected RestrictionType constructFindCalendarItemsByDateRangeRestriction(Date startTime, Date endTime) {
		ObjectFactory of = getObjectFactory();
		JAXBElement<IsGreaterThanOrEqualToType> startSearchExpression = getCalendarItemStartRestriction(startTime);
		
		JAXBElement<IsLessThanOrEqualToType> endSearchExpression = getCalendarItemEndRestriction( endTime);
		
		//and them all together
		AndType andType = new AndType();
		andType.getSearchExpressions().add(startSearchExpression);
		andType.getSearchExpressions().add(endSearchExpression);
		JAXBElement<AndType> andSearchExpression = of.createAnd(andType);
		
		//3) create restriction and set (2) searchExpression
		RestrictionType restrictionType = new RestrictionType();
		restrictionType.setSearchExpression(andSearchExpression);
		return restrictionType;
	}
	
	/**
	 * FindItem operations
	 */
	public FindItem constructFindItemIdsByDateRange(Date startTime, Date endTime, Collection<FolderIdType> folderIds) {
		Collection<? extends BaseFolderIdType> baseFolderIds = folderIds;
		if(CollectionUtils.isEmpty(baseFolderIds)) {
			DistinguishedFolderIdType distinguishedFolderIdType = new DistinguishedFolderIdType();
			distinguishedFolderIdType.setId(DistinguishedFolderIdNameType.CALENDAR);
			baseFolderIds = Collections.singleton(distinguishedFolderIdType);
		}
		
		RestrictionType restriction = constructFindCalendarItemsByDateRangeRestriction(startTime, endTime);
		return constructIndexedPageViewFindFirstItemIdsShallow(restriction,getAdditionalExtendedProperties(), baseFolderIds);
	}
	
	public FindItem constructFindCalendarItemIdsByDateRange(Date startTime, Date endTime, Collection<FolderIdType> folderIds) {
		Collection<? extends BaseFolderIdType> baseFolderIds = folderIds;
		if(CollectionUtils.isEmpty(baseFolderIds)) {
			DistinguishedFolderIdType distinguishedFolderIdType = new DistinguishedFolderIdType();
			distinguishedFolderIdType.setId(DistinguishedFolderIdNameType.CALENDAR);
			baseFolderIds = Collections.singleton(distinguishedFolderIdType);
		}
		ItemResponseShapeType responseShape = constructResponseShape(DefaultShapeNamesType.ID_ONLY, getAdditionalExtendedProperties());
		return constructCalendarViewFindItem(startTime, endTime, responseShape,  ItemQueryTraversalType.SHALLOW,  baseFolderIds);
	}

	public NonEmptyArrayOfPathsToElementType getAdditionalProperties() {
		// TODO strongly suggest you override this
		return null;
	}
	
	public Collection<ExtendedPropertyType> getExtendedProperties() {
		// TODO strongly suggest you override this
		List<ExtendedPropertyType> exProps = new ArrayList<ExtendedPropertyType>();
		return exProps;
	}
	
}
