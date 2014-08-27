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

import com.microsoft.exchange.messages.AddDelegate;
import com.microsoft.exchange.messages.AddDelegateResponse;
import com.microsoft.exchange.messages.ConvertId;
import com.microsoft.exchange.messages.ConvertIdResponse;
import com.microsoft.exchange.messages.CopyFolder;
import com.microsoft.exchange.messages.CopyFolderResponse;
import com.microsoft.exchange.messages.CopyItem;
import com.microsoft.exchange.messages.CopyItemResponse;
import com.microsoft.exchange.messages.CreateAttachment;
import com.microsoft.exchange.messages.CreateAttachmentResponse;
import com.microsoft.exchange.messages.CreateFolder;
import com.microsoft.exchange.messages.CreateFolderResponse;
import com.microsoft.exchange.messages.CreateItem;
import com.microsoft.exchange.messages.CreateItemResponse;
import com.microsoft.exchange.messages.CreateManagedFolder;
import com.microsoft.exchange.messages.CreateManagedFolderResponse;
import com.microsoft.exchange.messages.DeleteAttachment;
import com.microsoft.exchange.messages.DeleteAttachmentResponse;
import com.microsoft.exchange.messages.DeleteFolder;
import com.microsoft.exchange.messages.DeleteFolderResponse;
import com.microsoft.exchange.messages.DeleteItem;
import com.microsoft.exchange.messages.DeleteItemResponse;
import com.microsoft.exchange.messages.EmptyFolder;
import com.microsoft.exchange.messages.EmptyFolderResponse;
import com.microsoft.exchange.messages.ExpandDL;
import com.microsoft.exchange.messages.ExpandDLResponse;
import com.microsoft.exchange.messages.FindFolder;
import com.microsoft.exchange.messages.FindFolderResponse;
import com.microsoft.exchange.messages.FindItem;
import com.microsoft.exchange.messages.FindItemResponse;
import com.microsoft.exchange.messages.GetAttachment;
import com.microsoft.exchange.messages.GetAttachmentResponse;
import com.microsoft.exchange.messages.GetDelegate;
import com.microsoft.exchange.messages.GetDelegateResponse;
import com.microsoft.exchange.messages.GetEvents;
import com.microsoft.exchange.messages.GetEventsResponse;
import com.microsoft.exchange.messages.GetFolder;
import com.microsoft.exchange.messages.GetFolderResponse;
import com.microsoft.exchange.messages.GetItem;
import com.microsoft.exchange.messages.GetItemResponse;
import com.microsoft.exchange.messages.GetServerTimeZones;
import com.microsoft.exchange.messages.GetServerTimeZonesResponse;
import com.microsoft.exchange.messages.GetUserAvailabilityRequest;
import com.microsoft.exchange.messages.GetUserAvailabilityResponse;
import com.microsoft.exchange.messages.GetUserOofSettingsRequest;
import com.microsoft.exchange.messages.GetUserOofSettingsResponse;
import com.microsoft.exchange.messages.MoveFolder;
import com.microsoft.exchange.messages.MoveFolderResponse;
import com.microsoft.exchange.messages.MoveItem;
import com.microsoft.exchange.messages.MoveItemResponse;
import com.microsoft.exchange.messages.RemoveDelegate;
import com.microsoft.exchange.messages.RemoveDelegateResponse;
import com.microsoft.exchange.messages.ResolveNames;
import com.microsoft.exchange.messages.ResolveNamesResponse;
import com.microsoft.exchange.messages.SendItem;
import com.microsoft.exchange.messages.SendItemResponse;
import com.microsoft.exchange.messages.SetUserOofSettingsRequest;
import com.microsoft.exchange.messages.SetUserOofSettingsResponse;
import com.microsoft.exchange.messages.Subscribe;
import com.microsoft.exchange.messages.SubscribeResponse;
import com.microsoft.exchange.messages.SyncFolderHierarchy;
import com.microsoft.exchange.messages.SyncFolderHierarchyResponse;
import com.microsoft.exchange.messages.SyncFolderItems;
import com.microsoft.exchange.messages.SyncFolderItemsResponse;
import com.microsoft.exchange.messages.Unsubscribe;
import com.microsoft.exchange.messages.UnsubscribeResponse;
import com.microsoft.exchange.messages.UpdateDelegate;
import com.microsoft.exchange.messages.UpdateDelegateResponse;
import com.microsoft.exchange.messages.UpdateFolder;
import com.microsoft.exchange.messages.UpdateFolderResponse;
import com.microsoft.exchange.messages.UpdateItem;
import com.microsoft.exchange.messages.UpdateItemResponse;

/**
 * Interface representing the methods available via Exchange Web Services.
 * 
 * @author Nicholas Blair
 */
public interface ExchangeWebServices {

	ResolveNamesResponse resolveNames(ResolveNames request);
	
	ExpandDLResponse expandDL(ExpandDL request);
	
	FindFolderResponse findFolder(FindFolder request);
	
	FindItemResponse findItem(FindItem request);
	
	EmptyFolderResponse emptyFolder(EmptyFolder request);
	
	GetFolderResponse getFolder(GetFolder request);
	
	ConvertIdResponse convertId(ConvertId request);
	
	CreateFolderResponse createFolder(CreateFolder request);
	
	DeleteFolderResponse deleteFolder(DeleteFolder request);
	
	UpdateFolderResponse updateFolder(UpdateFolder request);
	
	MoveFolderResponse moveFolder(MoveFolder request);
	
	CopyFolderResponse copyFolder(CopyFolder request);
	
	SubscribeResponse subscribe(Subscribe request);
	
	UnsubscribeResponse unsubscribe(Unsubscribe request);
	
	GetEventsResponse getEvents(GetEvents request);
	
	SyncFolderHierarchyResponse syncFolderHierarchy(SyncFolderHierarchy request);
	
	SyncFolderItemsResponse syncFolderItems(SyncFolderItems request);
	
	CreateManagedFolderResponse createManagedFolder(CreateManagedFolder request);
	
	GetItemResponse getItem(GetItem request);
	
	CreateItemResponse createItem(CreateItem request);
	
	DeleteItemResponse deleteItem(DeleteItem request);
	
	UpdateItemResponse updateItem(UpdateItem request);
	
	SendItemResponse sendItem(SendItem request);
	
	MoveItemResponse moveItem(MoveItem request);
	
	CopyItemResponse copyItem(CopyItem request);
	
	CreateAttachmentResponse createAttachment(CreateAttachment request);
	
	DeleteAttachmentResponse deleteAttachment(DeleteAttachment request);
	
	GetAttachmentResponse getAttachment(GetAttachment request);
	
	GetDelegateResponse getDelegate(GetDelegate request);
	
	AddDelegateResponse addDelegate(AddDelegate request);
	
	RemoveDelegateResponse removeDelegate(RemoveDelegate request);
	
	UpdateDelegateResponse updateDelegate(UpdateDelegate request);
	
	GetUserAvailabilityResponse getUserAvailability(GetUserAvailabilityRequest request);
	
	GetUserOofSettingsResponse getUserOofSettings(GetUserOofSettingsRequest request);
	
	SetUserOofSettingsResponse setUserOofSettings(SetUserOofSettingsRequest request);
	
	GetServerTimeZonesResponse getServerTimeZones(GetServerTimeZones request);

}
