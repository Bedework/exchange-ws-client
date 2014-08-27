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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.xml.bind.JAXBElement;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.util.CollectionUtils;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import com.microsoft.exchange.exception.ExchangeCannotDeleteRuntimeException;
import com.microsoft.exchange.exception.ExchangeExceededFindCountLimitRuntimeException;
import com.microsoft.exchange.exception.ExchangeItemNotFoundRuntimeException;
import com.microsoft.exchange.exception.ExchangeRuntimeException;
import com.microsoft.exchange.exception.ExchangeTimeoutRuntimeException;
import com.microsoft.exchange.messages.ArrayOfFreeBusyResponse;
import com.microsoft.exchange.messages.ArrayOfResponseMessagesType;
import com.microsoft.exchange.messages.BaseResponseMessageType;
import com.microsoft.exchange.messages.CreateFolderResponse;
import com.microsoft.exchange.messages.CreateItemResponse;
import com.microsoft.exchange.messages.DeleteFolderResponse;
import com.microsoft.exchange.messages.EmptyFolderResponse;
import com.microsoft.exchange.messages.FindFolderResponse;
import com.microsoft.exchange.messages.FindFolderResponseMessageType;
import com.microsoft.exchange.messages.FindItemResponse;
import com.microsoft.exchange.messages.FindItemResponseMessageType;
import com.microsoft.exchange.messages.FolderInfoResponseMessageType;
import com.microsoft.exchange.messages.FreeBusyResponseType;
import com.microsoft.exchange.messages.GetFolderResponse;
import com.microsoft.exchange.messages.GetItemResponse;
import com.microsoft.exchange.messages.GetServerTimeZonesResponse;
import com.microsoft.exchange.messages.GetServerTimeZonesResponseMessageType;
import com.microsoft.exchange.messages.GetUserAvailabilityResponse;
import com.microsoft.exchange.messages.ItemInfoResponseMessageType;
import com.microsoft.exchange.messages.ResolveNamesResponse;
import com.microsoft.exchange.messages.ResolveNamesResponseMessageType;
import com.microsoft.exchange.messages.ResponseCodeType;
import com.microsoft.exchange.messages.ResponseMessageType;
import com.microsoft.exchange.messages.ResponseMessageType.MessageXml;
import com.microsoft.exchange.messages.SuggestionsResponseType;
import com.microsoft.exchange.messages.UpdateFolderResponse;
import com.microsoft.exchange.types.ArrayOfFoldersType;
import com.microsoft.exchange.types.ArrayOfRealItemsType;
import com.microsoft.exchange.types.ArrayOfResolutionType;
import com.microsoft.exchange.types.ArrayOfSuggestionDayResult;
import com.microsoft.exchange.types.ArrayOfTimeZoneDefinitionType;
import com.microsoft.exchange.types.BaseFolderType;
import com.microsoft.exchange.types.ContactItemType;
import com.microsoft.exchange.types.EmailAddressDictionaryEntryType;
import com.microsoft.exchange.types.EmailAddressDictionaryType;
import com.microsoft.exchange.types.FindFolderParentType;
import com.microsoft.exchange.types.FindItemParentType;
import com.microsoft.exchange.types.FolderIdType;
import com.microsoft.exchange.types.FreeBusyView;
import com.microsoft.exchange.types.ItemIdType;
import com.microsoft.exchange.types.ItemType;
import com.microsoft.exchange.types.ResolutionType;
import com.microsoft.exchange.types.ResponseClassType;
import com.microsoft.exchange.types.SuggestionDayResult;
import com.microsoft.exchange.types.TimeZoneDefinitionType;


public class ExchangeResponseUtilsImpl implements ExchangeResponseUtils  {
	protected final Log log = LogFactory.getLog(this.getClass());
	
	/* (non-Javadoc)
	 * @see com.microsoft.exchange.ExchangeResponseUtils#parseCreateFolderResponse(com.microsoft.exchange.messages.CreateFolderResponse)
	 */
	@Override
	public Set<FolderIdType> parseCreateFolderResponse(CreateFolderResponse response) {
		return parseFolderResponse(response);
	}
	
	/* (non-Javadoc)
	 * @see com.microsoft.exchange.ExchangeResponseUtils#parseUpdateFolderResponse(com.microsoft.exchange.messages.UpdateFolderResponse)
	 */
	@Override
	public Set<FolderIdType> parseUpdateFolderResponse(UpdateFolderResponse response){
		return parseFolderResponse(response);
	}
	
	/* (non-Javadoc)
	 * @see com.microsoft.exchange.ExchangeResponseUtils#parseGetItemResponse(com.microsoft.exchange.messages.GetItemResponse)
	 */
	@Override
	public Set<ItemType> parseGetItemResponse(GetItemResponse response) {
		Set<ItemType> items = new HashSet<ItemType>();
		confirmSuccess(response);
		ArrayOfResponseMessagesType responseMessages = response.getResponseMessages();
		List<JAXBElement<? extends ResponseMessageType>> getItemResponseMessages = responseMessages.getCreateItemResponseMessagesAndDeleteItemResponseMessagesAndGetItemResponseMessages();
		for(JAXBElement<? extends ResponseMessageType> responseMessageElement : getItemResponseMessages) {
			ItemInfoResponseMessageType itemType = (ItemInfoResponseMessageType) responseMessageElement.getValue();
			ArrayOfRealItemsType itemsArray = itemType.getItems();
			items.addAll(itemsArray.getItemsAndMessagesAndCalendarItems());
		}
		return items;
	}
	
	protected Set<FolderIdType> parseFolderResponse(BaseResponseMessageType response){
		confirmSuccess(response);
		Set<FolderIdType> folderIds = new HashSet<FolderIdType>();
		ArrayOfResponseMessagesType responseMessages = response.getResponseMessages();
		List<JAXBElement<? extends ResponseMessageType>> createItemResponseMessages = responseMessages.getCreateItemResponseMessagesAndDeleteItemResponseMessagesAndGetItemResponseMessages();
		for(JAXBElement<? extends ResponseMessageType> responeElement : createItemResponseMessages) {
			FolderInfoResponseMessageType itemInfo = (FolderInfoResponseMessageType) responeElement.getValue();
			ArrayOfFoldersType folders = itemInfo.getFolders();
			if(null != folders && !CollectionUtils.isEmpty(folders.getFoldersAndCalendarFoldersAndContactsFolders())) {
				List<BaseFolderType> foldersAndCalendarFoldersAndContactsFolders = folders.getFoldersAndCalendarFoldersAndContactsFolders();
				for(BaseFolderType bFolderIdType : foldersAndCalendarFoldersAndContactsFolders) {
					FolderIdType folderId = bFolderIdType.getFolderId();
					log.trace(" folderName= "+bFolderIdType.getDisplayName()+", folderId="+folderId);
					folderIds.add(folderId);
				}
			}else {
				log.error("No folders returned");
			}
		}
		return folderIds;
	}
	
	/* (non-Javadoc)
	 * @see com.microsoft.exchange.ExchangeResponseUtils#parseGetFolderResponse(com.microsoft.exchange.messages.GetFolderResponse)
	 */
	@Override
	public  Set<BaseFolderType> parseGetFolderResponse(GetFolderResponse getFolderResponse) {
		confirmSuccess(getFolderResponse);
		Set<BaseFolderType> folders =  new HashSet<BaseFolderType>();
		ArrayOfResponseMessagesType responseMessages = getFolderResponse.getResponseMessages();
		List<JAXBElement<? extends ResponseMessageType>> getFolderResponseMessages = responseMessages.getCreateItemResponseMessagesAndDeleteItemResponseMessagesAndGetItemResponseMessages();
		for(JAXBElement<? extends ResponseMessageType> responseMessage: getFolderResponseMessages) {
			FolderInfoResponseMessageType folderInfo = (FolderInfoResponseMessageType) responseMessage.getValue();
			List<BaseFolderType> f = folderInfo.getFolders().getFoldersAndCalendarFoldersAndContactsFolders();
			if(!CollectionUtils.isEmpty(f)) {
				folders.addAll(f);
			}
		}
		return folders;
	}
	
	/* (non-Javadoc)
	 * @see com.microsoft.exchange.ExchangeResponseUtils#confirmSuccess(com.microsoft.exchange.messages.BaseResponseMessageType)
	 */
	@Override
	public  boolean confirmSuccess(BaseResponseMessageType response) {
		Boolean success = null;
		
		ArrayOfResponseMessagesType messages = response.getResponseMessages();
		List<JAXBElement<? extends ResponseMessageType>> inner = messages.getCreateItemResponseMessagesAndDeleteItemResponseMessagesAndGetItemResponseMessages();
		for(JAXBElement<? extends ResponseMessageType> innerResponse : inner){
			if(success == null){
				success =  ResponseCodeType.NO_ERROR.equals(innerResponse.getValue().getResponseCode());
			}else{
				success = success && ResponseCodeType.NO_ERROR.equals(innerResponse.getValue().getResponseCode());
			}
			if(!success || !ResponseCodeType.NO_ERROR.equals(innerResponse.getValue().getResponseCode())){
				ResponseCodeType responseCode = innerResponse.getValue().getResponseCode();
				String err =  parseInnerResponse(innerResponse);
				
				if(ResponseCodeType.ERROR_INTERNAL_SERVER_ERROR.equals(responseCode) || ResponseCodeType.ERROR_INTERNAL_SERVER_TRANSIENT_ERROR.equals(responseCode)) {
					//TODO recover (switch Credentials)
				}
				
				if(ResponseCodeType.ERROR_TIMEOUT_EXPIRED.equals(responseCode)){
					throw new ExchangeTimeoutRuntimeException(err);
				}
				
				if(ResponseCodeType.ERROR_CANNOT_DELETE_OBJECT.equals(responseCode)) {
					throw new ExchangeCannotDeleteRuntimeException(err);
				}
				
				if(ResponseCodeType.ERROR_ITEM_NOT_FOUND.equals(responseCode)) {
					throw new ExchangeItemNotFoundRuntimeException(err);
				}

				if(ResponseCodeType.ERROR_EXCEEDED_FIND_COUNT_LIMIT.equals(responseCode)) {
					throw new ExchangeExceededFindCountLimitRuntimeException(err);
				}
				
				throw new ExchangeRuntimeException(err);
			}
		}
		return success;
	}
	
	private boolean confirmSuccessInternal(ResponseMessageType responseMessage){
		boolean success =false;
		
		if(null != responseMessage){
			
			success = ResponseCodeType.NO_ERROR.equals(responseMessage.getResponseCode());
		}
		
		
		return success;
	}
	
	/* (non-Javadoc)
	 * @see com.microsoft.exchange.ExchangeResponseUtils#confirmSuccessOrWarning(com.microsoft.exchange.messages.BaseResponseMessageType)
	 */
	@Override
	public  boolean confirmSuccessOrWarning(BaseResponseMessageType response) {
		List<String> errors = new ArrayList<String>();
		List<String> warnings = new ArrayList<String>();
		
		ArrayOfResponseMessagesType messages = response.getResponseMessages();
		List<JAXBElement<? extends ResponseMessageType>> inner = messages.getCreateItemResponseMessagesAndDeleteItemResponseMessagesAndGetItemResponseMessages();
		for(JAXBElement<? extends ResponseMessageType> innerResponse : inner){
			if(innerResponse != null && innerResponse.getValue() != null) {
				String parsedMsg = parseInnerResponse(innerResponse);
				
				ResponseMessageType innerResponseValue = innerResponse.getValue();
				
				ResponseClassType responseClass = innerResponseValue.getResponseClass();

				switch (responseClass) {
				case WARNING:
					log.warn(parsedMsg);
					warnings.add(parsedMsg);
					break;
					
				case ERROR:
					log.error(parsedMsg);
					errors.add(parsedMsg);
					break;
					
				default:
					break;
				}
			}
		}
		if(CollectionUtils.isEmpty(errors)) {
			return true;
		}else {
			return false;
		}
	}

	
	
	/* (non-Javadoc)
	 * @see com.microsoft.exchange.ExchangeResponseUtils#parseFindFolderResponse(com.microsoft.exchange.messages.FindFolderResponse)
	 */
	@Override
	public  List<BaseFolderType> parseFindFolderResponse(FindFolderResponse findFolderResponse) {
		ArrayOfResponseMessagesType findFolderResponseMessages = findFolderResponse.getResponseMessages();
		List<JAXBElement<? extends ResponseMessageType>> folderItemResponseMessages = findFolderResponseMessages.getCreateItemResponseMessagesAndDeleteItemResponseMessagesAndGetItemResponseMessages();
		for(JAXBElement<? extends ResponseMessageType> responseElement: folderItemResponseMessages) {
			FindFolderResponseMessageType itemType = (FindFolderResponseMessageType) responseElement.getValue();
			if(null != itemType && null != itemType.getRootFolder() && null != itemType.getRootFolder().getFolders() && null != itemType.getRootFolder().getFolders().getFoldersAndCalendarFoldersAndContactsFolders()) {
				FindFolderParentType rootFolder = itemType.getRootFolder();
				ArrayOfFoldersType folders = rootFolder.getFolders();
				return folders.getFoldersAndCalendarFoldersAndContactsFolders();
			}
		}
		return null;
	}

	/* (non-Javadoc)
	 * @see com.microsoft.exchange.ExchangeResponseUtils#getCreatedItemIds(com.microsoft.exchange.messages.CreateItemResponse)
	 */
	@Override
	public Set<ItemIdType> getCreatedItemIds(CreateItemResponse response){
		Set<ItemIdType> successfulItems = new HashSet<ItemIdType>();
		if(null == response) {
			return successfulItems;
		}
		ArrayOfResponseMessagesType messages = response.getResponseMessages();
		List<JAXBElement<? extends ResponseMessageType>> inner = messages.getCreateItemResponseMessagesAndDeleteItemResponseMessagesAndGetItemResponseMessages();
		for(JAXBElement<? extends ResponseMessageType> innerResponse : inner){
			ResponseCodeType responseCode = innerResponse.getValue().getResponseCode();
			String err = parseInnerResponse(innerResponse);
			
			if(ResponseCodeType.NO_ERROR.equals(responseCode)){
				ItemInfoResponseMessageType itemInfo = (ItemInfoResponseMessageType) innerResponse.getValue();
				ArrayOfRealItemsType items = itemInfo.getItems();
				List<ItemType> calendarItems = items.getItemsAndMessagesAndCalendarItems();
				for(ItemType itemType : calendarItems) {
					successfulItems.add(itemType.getItemId());
				}
			}else {
				log.trace("Failed to create item, "+err);
			}
		}
		return successfulItems;
	}
	
	/* (non-Javadoc)
	 * @see com.microsoft.exchange.ExchangeResponseUtils#getCreateItemErrors(com.microsoft.exchange.messages.CreateItemResponse)
	 */
	@Override
	public List<String> getCreateItemErrors(CreateItemResponse response){
		List<String> errs = new ArrayList<String>();
		if(null == response || null == response.getResponseMessages()) {
			errs.add("NO RESPONSE");
			return errs;
		}
		ArrayOfResponseMessagesType messages = response.getResponseMessages();
		List<JAXBElement<? extends ResponseMessageType>> inner = messages.getCreateItemResponseMessagesAndDeleteItemResponseMessagesAndGetItemResponseMessages();
		for(JAXBElement<? extends ResponseMessageType> innerResponse : inner){
			ResponseCodeType responseCode = innerResponse.getValue().getResponseCode();
			String err = parseInnerResponse(innerResponse);
			
			if(!ResponseCodeType.NO_ERROR.equals(responseCode)){
				errs.add(err);
			}
		}
		return errs;
	}
	
	/* (non-Javadoc)
	 * @see com.microsoft.exchange.ExchangeResponseUtils#parseCreateItemResponse(com.microsoft.exchange.messages.CreateItemResponse)
	 */
	@Override
	public  List<ItemIdType> parseCreateItemResponse(CreateItemResponse response) {
		confirmSuccess(response);
		List<ItemIdType> itemIds = new ArrayList<ItemIdType>();
		ArrayOfResponseMessagesType responseMessages = response.getResponseMessages();
		List<JAXBElement<? extends ResponseMessageType>> createItemResponseMessages = responseMessages.getCreateItemResponseMessagesAndDeleteItemResponseMessagesAndGetItemResponseMessages();
		for(JAXBElement<? extends ResponseMessageType> responeElement : createItemResponseMessages) {
			ItemInfoResponseMessageType itemInfo = (ItemInfoResponseMessageType) responeElement.getValue();
			ArrayOfRealItemsType items = itemInfo.getItems();
			
			List<ItemType> calendarItems = items.getItemsAndMessagesAndCalendarItems();
			for(ItemType itemType : calendarItems) {
				itemIds.add(itemType.getItemId());
			}
		}
		return itemIds;
	}
	
	/* (non-Javadoc)
	 * @see com.microsoft.exchange.ExchangeResponseUtils#parseFindItemIdResponseNoOffset(com.microsoft.exchange.messages.FindItemResponse)
	 */
	@Override
	@Deprecated 
	public Set<ItemIdType> parseFindItemIdResponseNoOffset(FindItemResponse response) {
		Pair<Set<ItemIdType>, Integer> pair = parseFindItemIdResponse(response);
		return pair.getLeft();
	}
	
	/* (non-Javadoc)
	 * @see com.microsoft.exchange.ExchangeResponseUtils#parseFindItemIdResponse(com.microsoft.exchange.messages.FindItemResponse)
	 */
	@Override
	public Pair<Set<ItemIdType>, Integer> parseFindItemIdResponse(FindItemResponse response){
		
		confirmSuccess(response);
		Set<ItemIdType> foundItemIds = new HashSet<ItemIdType>();
		Integer nextOffset = -1;
		ArrayOfResponseMessagesType findItemResponseMessages = response.getResponseMessages();
		List<JAXBElement<? extends ResponseMessageType>> itemResponseMessages = findItemResponseMessages.getCreateItemResponseMessagesAndDeleteItemResponseMessagesAndGetItemResponseMessages();
		for(JAXBElement<? extends ResponseMessageType> element : itemResponseMessages) {
			FindItemResponseMessageType itemType = (FindItemResponseMessageType) element.getValue();
			FindItemParentType rootFolder = itemType.getRootFolder();
			
			Boolean includesLastItemInRange = rootFolder.isIncludesLastItemInRange();
			Integer totalItemsInView = rootFolder.getTotalItemsInView();
			
			if(!includesLastItemInRange){
				nextOffset = rootFolder.getIndexedPagingOffset();
			}
			ArrayOfRealItemsType items = rootFolder.getItems();
			List<ItemType> calendarItems = items.getItemsAndMessagesAndCalendarItems();
			for(ItemType it: calendarItems) {
				foundItemIds.add(it.getItemId());
			}
			
			log.info("parseFindItemIdResponse: foundItems="+foundItemIds.size()+", totalItemsInview="+totalItemsInView+" , nextOffset"+nextOffset+", includesLast="+includesLastItemInRange );
		}
		
		Pair<Set<ItemIdType>, Integer> pair = Pair.of(foundItemIds, nextOffset);
		return pair;
	}

	/* (non-Javadoc)
	 * @see com.microsoft.exchange.ExchangeResponseUtils#parseFindItemResponse(com.microsoft.exchange.messages.FindItemResponse)
	 */
	@Override
	public Set<ItemType> parseFindItemResponse(FindItemResponse response) {
		confirmSuccess(response);
		Set<ItemType> calendarItems = new HashSet<ItemType>();

		ArrayOfResponseMessagesType findItemResponseMessages = response
				.getResponseMessages();
		List<JAXBElement<? extends ResponseMessageType>> itemResponseMessages = findItemResponseMessages
				.getCreateItemResponseMessagesAndDeleteItemResponseMessagesAndGetItemResponseMessages();
		for (JAXBElement<? extends ResponseMessageType> element : itemResponseMessages) {
			FindItemResponseMessageType itemType = (FindItemResponseMessageType) element
					.getValue();
			FindItemParentType rootFolder = itemType.getRootFolder();
			ArrayOfRealItemsType items = rootFolder.getItems();
			calendarItems.addAll(items.getItemsAndMessagesAndCalendarItems());

		}
		return calendarItems;
	}
	
	private String parseInnerResponse(JAXBElement<? extends ResponseMessageType> innerResponse) {
		
		ResponseMessageType responseMessage = innerResponse.getValue();
		return parseInnerResponse(responseMessage);
		
	}
	
	private String parseInnerResponse(ResponseMessageType responseMessage){
		StringBuilder responseBuilder = new StringBuilder("Response[");
		
		ResponseCodeType responseCode = responseMessage.getResponseCode();
		if(null != responseCode) {
			responseBuilder.append("code="+responseCode+", ");
		}
		ResponseClassType responseClass = responseMessage.getResponseClass();
		if(null != responseClass) {
			responseBuilder.append("class="+responseClass+", ");
		}
		String messageText = responseMessage.getMessageText();
		if(StringUtils.isNotBlank(messageText)) {
			responseBuilder.append("txt="+messageText+", ");
		}
		MessageXml messageXml = responseMessage.getMessageXml();
		if(null != messageXml) {
			StringBuilder xmlStringBuilder=new StringBuilder("messageXml=");
			List<Element> anies = messageXml.getAnies();
			if(!CollectionUtils.isEmpty(anies)) {
				for (Element element : anies) {
					String elementNameString=element.getNodeName();
					String elementValueString=element.getNodeValue();
					xmlStringBuilder.append(elementNameString+"="+elementValueString+";");
					
					if(null != element.getAttributes()) {
						NamedNodeMap attributes = element.getAttributes();
						for (int i = 0; i < attributes.getLength(); i++) {
							Node item = attributes.item(i);
							String nodeName = item.getNodeName();
							String nodeValue = item.getNodeValue();
							xmlStringBuilder.append(nodeName+"="+nodeValue+",");	
						}
					}	
				}
			}
			responseBuilder.append("xml="+xmlStringBuilder.toString()+", ");
		}
		Integer descriptiveLinkKey = responseMessage.getDescriptiveLinkKey();
		if(null != descriptiveLinkKey) {
			responseBuilder.append("link="+descriptiveLinkKey);
		}
		
		responseBuilder.append("]");
		return responseBuilder.toString();
	}
	
	
	/* (non-Javadoc)
	 * @see com.microsoft.exchange.ExchangeResponseUtils#parseEmptyFolderResponse(com.microsoft.exchange.messages.EmptyFolderResponse)
	 */
	@Override
	public boolean parseEmptyFolderResponse(EmptyFolderResponse response) {
		if(confirmSuccess(response)){
			ArrayOfResponseMessagesType responseMessages = response.getResponseMessages();
			List<JAXBElement<? extends ResponseMessageType>> messages = responseMessages.getCreateItemResponseMessagesAndDeleteItemResponseMessagesAndGetItemResponseMessages();
			for(JAXBElement<? extends ResponseMessageType> message : messages) {
				
				ResponseMessageType value = message.getValue();
				//TODO parse response value(s) appropriately
				return true;
			}
		}
		return false;
	}
	
	
	
	/* (non-Javadoc)
	 * @see com.microsoft.exchange.ExchangeResponseUtils#parseResolveNamesResponse(com.microsoft.exchange.messages.ResolveNamesResponse)
	 */
	@Override
	public Set<String> parseResolveNamesResponse(ResolveNamesResponse response) {
		Set<String> addresses = new HashSet<String>();
		if(confirmSuccessOrWarning(response)) {
			ArrayOfResponseMessagesType arrayOfResponseMessagesType = response.getResponseMessages();
			List<JAXBElement<? extends ResponseMessageType>> responseMessagesList = arrayOfResponseMessagesType.getCreateItemResponseMessagesAndDeleteItemResponseMessagesAndGetItemResponseMessages();
			for(JAXBElement<? extends ResponseMessageType> element: responseMessagesList) {
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
		}
		return addresses;
	}
	
	/* (non-Javadoc)
	 * @see com.microsoft.exchange.ExchangeResponseUtils#parseGetServerTimeZonesResponse(com.microsoft.exchange.messages.GetServerTimeZonesResponse)
	 */
	@Override
	public List<TimeZoneDefinitionType> parseGetServerTimeZonesResponse(GetServerTimeZonesResponse response){
		List<TimeZoneDefinitionType> zones = new ArrayList<TimeZoneDefinitionType>();
		if(confirmSuccess(response)){
			ArrayOfResponseMessagesType responseMessages = response.getResponseMessages();
			List<JAXBElement<? extends ResponseMessageType>> tzResponseMessages = responseMessages.getCreateItemResponseMessagesAndDeleteItemResponseMessagesAndGetItemResponseMessages();
			for(JAXBElement<? extends ResponseMessageType> message: tzResponseMessages){
				ResponseMessageType r = message.getValue();
				GetServerTimeZonesResponseMessageType itemInfo = (GetServerTimeZonesResponseMessageType) r;
				ArrayOfTimeZoneDefinitionType timeZoneDefinitions = itemInfo.getTimeZoneDefinitions();
				List<TimeZoneDefinitionType> timeZoneDefinitionsList = timeZoneDefinitions.getTimeZoneDefinitions();
				zones = timeZoneDefinitionsList;
			}
		}
		return zones;
	}
	
	/* (non-Javadoc)
	 * @see com.microsoft.exchange.ExchangeResponseUtils#parseDeleteFolderResponse(com.microsoft.exchange.messages.DeleteFolderResponse)
	 */
	@Override
	public boolean parseDeleteFolderResponse(DeleteFolderResponse response){
		if(confirmSuccess(response)){
			ArrayOfResponseMessagesType arrayOfResponseMessages = response.getResponseMessages();
			List<JAXBElement<? extends ResponseMessageType>> deleteFolderResponseMessages = arrayOfResponseMessages.getCreateItemResponseMessagesAndDeleteItemResponseMessagesAndGetItemResponseMessages();
			for(JAXBElement<? extends ResponseMessageType> message: deleteFolderResponseMessages){
				ResponseMessageType value = message.getValue();
				return value.getResponseClass().equals(ResponseClassType.SUCCESS) && value.getResponseCode().equals(ResponseCodeType.NO_ERROR);
			}
		}
		return false;
	}

	public FreeBusyView parseFreeBusyResponse(GetUserAvailabilityResponse response){
		FreeBusyView freeBusyView = null;
		
		if(null != response){
			ArrayOfFreeBusyResponse freeBusyResponseArray = response.getFreeBusyResponseArray();
			if(null != freeBusyResponseArray){
				List<FreeBusyResponseType> freeBusyResponses = freeBusyResponseArray.getFreeBusyResponses();
				if(!CollectionUtils.isEmpty(freeBusyResponses)){
					for(FreeBusyResponseType fbrt : freeBusyResponses){
						ResponseMessageType responseMessage = fbrt.getResponseMessage();
						if(confirmSuccessInternal(responseMessage)){
							freeBusyView = fbrt.getFreeBusyView();
						}else{
							String failMsg = parseInnerResponse(responseMessage);
							log.warn("FreeBusyResponseType Failure: "+failMsg);
						}
					}
				}else{
					log.debug("freeBusyResponses are empty");
				}
			}else{
				log.debug("ArrayOfFreeBusyResponse is null");
			}
		}
		
		return freeBusyView;
	}
	
	public ArrayOfSuggestionDayResult parseSuggestionDayResult(GetUserAvailabilityResponse response){
		ArrayOfSuggestionDayResult suggestionResult = null;
		if(null != response){
			SuggestionsResponseType suggestionsResponse = response.getSuggestionsResponse();

			if(null != suggestionsResponse){
				ResponseMessageType responseMessage = suggestionsResponse.getResponseMessage();
				if(confirmSuccessInternal(responseMessage)){
					suggestionResult = suggestionsResponse.getSuggestionDayResultArray();	
				}else{
					String failMsg = parseInnerResponse(responseMessage);
					log.warn("SuggestionsResponseType Failure: "+failMsg);
				}
			}else{
				log.debug("SuggestionsResponseType is null");
			}
			
		}
		return suggestionResult;
	}
	
}
