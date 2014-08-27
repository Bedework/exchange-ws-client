package com.microsoft.exchange;

import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;

import com.microsoft.exchange.messages.BaseResponseMessageType;
import com.microsoft.exchange.messages.CreateFolderResponse;
import com.microsoft.exchange.messages.CreateItemResponse;
import com.microsoft.exchange.messages.DeleteFolderResponse;
import com.microsoft.exchange.messages.EmptyFolderResponse;
import com.microsoft.exchange.messages.FindFolderResponse;
import com.microsoft.exchange.messages.FindItemResponse;
import com.microsoft.exchange.messages.GetFolderResponse;
import com.microsoft.exchange.messages.GetItemResponse;
import com.microsoft.exchange.messages.GetServerTimeZonesResponse;
import com.microsoft.exchange.messages.ResolveNamesResponse;
import com.microsoft.exchange.messages.ResponseCodeType;
import com.microsoft.exchange.messages.UpdateFolderResponse;
import com.microsoft.exchange.types.BaseFolderType;
import com.microsoft.exchange.types.FolderIdType;
import com.microsoft.exchange.types.ItemIdType;
import com.microsoft.exchange.types.ItemType;
import com.microsoft.exchange.types.TimeZoneDefinitionType;

public interface ExchangeResponseUtils {

	public Set<FolderIdType> parseCreateFolderResponse(
			CreateFolderResponse response);

	public Set<FolderIdType> parseUpdateFolderResponse(
			UpdateFolderResponse response);

	public Set<ItemType> parseGetItemResponse(GetItemResponse response);

	/**
	 * @param getFolderResponse
	 * @return 
	 */
	public Set<BaseFolderType> parseGetFolderResponse(
			GetFolderResponse getFolderResponse);

	/**
	 * 
	 * @param response
	 * @return true if the response has {@link ResponseCodeType#NO_ERROR}, false otherwise
	 */
	public boolean confirmSuccess(BaseResponseMessageType response);

	public boolean confirmSuccessOrWarning(BaseResponseMessageType response);

	public List<BaseFolderType> parseFindFolderResponse(
			FindFolderResponse findFolderResponse);

	public Set<ItemIdType> getCreatedItemIds(CreateItemResponse response);

	public List<String> getCreateItemErrors(CreateItemResponse response);

	public List<ItemIdType> parseCreateItemResponse(CreateItemResponse response);

	public Set<ItemIdType> parseFindItemIdResponseNoOffset(
			FindItemResponse response);

	public Pair<Set<ItemIdType>, Integer> parseFindItemIdResponse(
			FindItemResponse response);

	public Set<ItemType> parseFindItemResponse(FindItemResponse response);

	public boolean parseEmptyFolderResponse(EmptyFolderResponse response);

	public Set<String> parseResolveNamesResponse(ResolveNamesResponse response);

	public List<TimeZoneDefinitionType> parseGetServerTimeZonesResponse(
			GetServerTimeZonesResponse response);

	public boolean parseDeleteFolderResponse(DeleteFolderResponse response);

}