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

package com.microsoft.exchange.impl;

import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.ws.client.core.WebServiceMessageCallback;
import org.springframework.ws.client.core.support.WebServiceGatewaySupport;
import org.springframework.ws.soap.client.SoapFaultClientException;
import org.springframework.ws.soap.client.core.SoapActionCallback;
import org.springframework.ws.transport.WebServiceMessageSender;
import org.springframework.ws.transport.http.HttpComponentsMessageSender;

import com.microsoft.exchange.ExchangeWebServices;
import com.microsoft.exchange.exception.ExchangeInvalidUPNRuntimeException;
import com.microsoft.exchange.exception.ExchangeWebServicesRuntimeException;
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
 * Spring {@link WebServiceGatewaySupport} backed implementatoin of {@link ExchangeWebServices}.
 * 
 * @author Nicholas Blair
 */
@Component
public class ExchangeWebServicesClient extends WebServiceGatewaySupport implements ExchangeWebServices {

	/**
	 * This appears as the message of a SoapFault in the event the client encounters throttle policy limits.
	 */
	protected static final String RETRY_ERROR_MESSAGE = "The server cannot service this request right now. Try again later.";
	protected static final Log log = LogFactory.getLog(ExchangeWebServicesClient.class);
	
	private KeyStore keyStore;
	private char[] keyStorePassword;
	private KeyStore trustStore;
	/**
	 * @param keyStore the keyStore to set
	 */
	public void setKeyStore(Resource keyStore) {
		this.keyStore = getKeystoreFromResource(keyStore, keyStorePassword);
	}
	/**
	 * @param trustStore the trustStore to set
	 */
	public void setTrustStore(Resource trustStore) {
		this.trustStore = getKeystoreFromResource(trustStore, keyStorePassword);
	}
	/**
	 * @param keyStorePassword the keyStorePassword to set
	 */
	public void setKeyStorePassword(char[] keyStorePassword) {
		this.keyStorePassword = keyStorePassword;
	}
	/**
	 * 
	 * @param resource
	 * @param password
	 * @return
	 */
	protected static KeyStore getKeystoreFromResource(Resource resource, char[] password) {
		try {
			KeyStore k = KeyStore.getInstance(KeyStore.getDefaultType());
			k.load(resource.getInputStream(), password);
			log.info("keystore loaded: " +k.toString() +", from:"+ resource.getFilename());
			return k;
		} catch (KeyStoreException e) {
			throw new IllegalArgumentException("failed to load keystore from " + resource.getDescription(), e);
		} catch (CertificateException e) {
			throw new IllegalArgumentException("failed to load keystore from " + resource.getDescription(), e);
		} catch (IOException e) {
			throw new IllegalArgumentException("failed to load keystore from " + resource.getDescription(), e);
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalArgumentException("failed to load keystore from " + resource.getDescription(), e);
		}
		
	}
	/* (non-Javadoc)
	 * @see org.springframework.ws.client.core.support.WebServiceGatewaySupport#initGateway()
	 */
	@Override
	protected void initGateway() throws Exception {
		super.initGateway();
		WebServiceMessageSender [] senders = getWebServiceTemplate().getMessageSenders();
		for(WebServiceMessageSender sender: senders) {
			if(sender instanceof HttpComponentsMessageSender) {
				HttpComponentsMessageSender hSender = (HttpComponentsMessageSender) sender;
				ClientConnectionManager connectionManager = hSender.getHttpClient().getConnectionManager();
				SchemeRegistry schemeRegistry = connectionManager.getSchemeRegistry();
				SSLSocketFactory sf = new SSLSocketFactory(keyStore, safeToString(keyStorePassword), trustStore);
				Scheme https = new Scheme("https", 443, sf);
				schemeRegistry.register(https);
				log.info("initGateway connection manager with https scheme");
			}
		}
	}

	String safeToString(char[] value) {
		if(value == null) {
			return null;
		}
		
		return new String(value);
	}
	/* (non-Javadoc)
	 * @see com.microsoft.exchange.ExchangeWebServices#resolveNames(com.microsoft.exchange.messages.ResolveNames)
	 */
	@Override
	public ResolveNamesResponse resolveNames(ResolveNames request) {
		ResolveNamesResponse response = (ResolveNamesResponse) internalInvoke(request);
		return response;
	}

	/* (non-Javadoc)
	 * @see com.microsoft.exchange.ExchangeWebServices#expandDL(com.microsoft.exchange.messages.ExpandDL)
	 */
	@Override
	public ExpandDLResponse expandDL(ExpandDL request) {
		ExpandDLResponse response = (ExpandDLResponse) internalInvoke(request);
		return response;
	}

	/* (non-Javadoc)
	 * @see com.microsoft.exchange.ExchangeWebServices#findFolder(com.microsoft.exchange.messages.FindFolder)
	 */
	@Override
	public FindFolderResponse findFolder(FindFolder request) {
		FindFolderResponse response = (FindFolderResponse) internalInvoke(request);
		return response;
	}

	/* (non-Javadoc)
	 * @see com.microsoft.exchange.ExchangeWebServices#findItem(com.microsoft.exchange.messages.FindItem)
	 */
	@Override
	public FindItemResponse findItem(FindItem request) {
		FindItemResponse response = (FindItemResponse) internalInvoke(request);
		return response;
	}

	/* (non-Javadoc)
	 * @see com.microsoft.exchange.ExchangeWebServices#getFolder(com.microsoft.exchange.messages.GetFolder)
	 */
	@Override
	public GetFolderResponse getFolder(GetFolder request) {
		GetFolderResponse response = (GetFolderResponse) internalInvoke(request);
		return response;
	}

	/* (non-Javadoc)
	 * @see com.microsoft.exchange.ExchangeWebServices#convertId(com.microsoft.exchange.messages.ConvertId)
	 */
	@Override
	public ConvertIdResponse convertId(ConvertId request) {
		ConvertIdResponse response = (ConvertIdResponse) internalInvoke(request);
		return response;
	}

	/* (non-Javadoc)
	 * @see com.microsoft.exchange.ExchangeWebServices#createFolder(com.microsoft.exchange.messages.CreateFolder)
	 */
	@Override
	public CreateFolderResponse createFolder(CreateFolder request) {
		CreateFolderResponse response = (CreateFolderResponse) internalInvoke(request);
		return response;
	}

	/* (non-Javadoc)
	 * @see com.microsoft.exchange.ExchangeWebServices#deleteFolder(com.microsoft.exchange.messages.DeleteFolder)
	 */
	@Override
	public DeleteFolderResponse deleteFolder(DeleteFolder request) {
		DeleteFolderResponse response = (DeleteFolderResponse) internalInvoke(request);
		return response;
	}

	/* (non-Javadoc)
	 * @see com.microsoft.exchange.ExchangeWebServices#updateFolder(com.microsoft.exchange.messages.UpdateFolder)
	 */
	@Override
	public UpdateFolderResponse updateFolder(UpdateFolder request) {
		UpdateFolderResponse response = (UpdateFolderResponse) internalInvoke(request);
		return response;
	}

	/* (non-Javadoc)
	 * @see com.microsoft.exchange.ExchangeWebServices#moveFolder(com.microsoft.exchange.messages.MoveFolder)
	 */
	@Override
	public MoveFolderResponse moveFolder(MoveFolder request) {
		MoveFolderResponse response = (MoveFolderResponse) internalInvoke(request);
		return response;
	}

	/* (non-Javadoc)
	 * @see com.microsoft.exchange.ExchangeWebServices#copyFolder(com.microsoft.exchange.messages.CopyFolder)
	 */
	@Override
	public CopyFolderResponse copyFolder(CopyFolder request) {
		CopyFolderResponse response = (CopyFolderResponse) internalInvoke(request);
		return response;
	}

	/* (non-Javadoc)
	 * @see com.microsoft.exchange.ExchangeWebServices#subscribe(com.microsoft.exchange.messages.Subscribe)
	 */
	@Override
	public SubscribeResponse subscribe(Subscribe request) {
		SubscribeResponse response = (SubscribeResponse) internalInvoke(request);
		return response;
	}

	/* (non-Javadoc)
	 * @see com.microsoft.exchange.ExchangeWebServices#unsubscribe(com.microsoft.exchange.messages.Unsubscribe)
	 */
	@Override
	public UnsubscribeResponse unsubscribe(Unsubscribe request) {
		UnsubscribeResponse response = (UnsubscribeResponse) internalInvoke(request);
		return response;
	}

	/* (non-Javadoc)
	 * @see com.microsoft.exchange.ExchangeWebServices#getEvents(com.microsoft.exchange.messages.GetEvents)
	 */
	@Override
	public GetEventsResponse getEvents(GetEvents request) {
		GetEventsResponse response = (GetEventsResponse) internalInvoke(request);
		return response;
	}

	/* (non-Javadoc)
	 * @see com.microsoft.exchange.ExchangeWebServices#syncFolderHierarchy(com.microsoft.exchange.messages.SyncFolderHierarchy)
	 */
	@Override
	public SyncFolderHierarchyResponse syncFolderHierarchy(
			SyncFolderHierarchy request) {
		SyncFolderHierarchyResponse response = (SyncFolderHierarchyResponse) internalInvoke(request);
		return response;
	}

	/* (non-Javadoc)
	 * @see com.microsoft.exchange.ExchangeWebServices#syncFolderItems(com.microsoft.exchange.messages.SyncFolderItems)
	 */
	@Override
	public SyncFolderItemsResponse syncFolderItems(SyncFolderItems request) {
		SyncFolderItemsResponse response = (SyncFolderItemsResponse) internalInvoke(request);
		return response;
	}

	/* (non-Javadoc)
	 * @see com.microsoft.exchange.ExchangeWebServices#createManagedFolder(com.microsoft.exchange.messages.CreateManagedFolder)
	 */
	@Override
	public CreateManagedFolderResponse createManagedFolder(
			CreateManagedFolder request) {
		CreateManagedFolderResponse response = (CreateManagedFolderResponse) internalInvoke(request);
		return response;
	}

	/* (non-Javadoc)
	 * @see com.microsoft.exchange.ExchangeWebServices#getItem(com.microsoft.exchange.messages.GetItem)
	 */
	@Override
	public GetItemResponse getItem(GetItem request) {
		GetItemResponse response = (GetItemResponse) internalInvoke(request);
		return response;
	}

	/* (non-Javadoc)
	 * @see com.microsoft.exchange.ExchangeWebServices#createItem(com.microsoft.exchange.messages.CreateItem)
	 */
	@Override
	public CreateItemResponse createItem(CreateItem request) {
		CreateItemResponse response = (CreateItemResponse) internalInvoke(request);
		return response;
	}

	/* (non-Javadoc)
	 * @see com.microsoft.exchange.ExchangeWebServices#deleteItem(com.microsoft.exchange.messages.DeleteItem)
	 */
	@Override
	public DeleteItemResponse deleteItem(DeleteItem request) {
		DeleteItemResponse response = (DeleteItemResponse) internalInvoke(request);
		return response;
	}

	/* (non-Javadoc)
	 * @see com.microsoft.exchange.ExchangeWebServices#updateItem(com.microsoft.exchange.messages.UpdateItem)
	 */
	@Override
	public UpdateItemResponse updateItem(UpdateItem request) {
		UpdateItemResponse response = (UpdateItemResponse) internalInvoke(request);
		return response;
	}

	/* (non-Javadoc)
	 * @see com.microsoft.exchange.ExchangeWebServices#sendItem(com.microsoft.exchange.messages.SendItem)
	 */
	@Override
	public SendItemResponse sendItem(SendItem request) {
		SendItemResponse response = (SendItemResponse) internalInvoke(request);
		return response;
	}

	/* (non-Javadoc)
	 * @see com.microsoft.exchange.ExchangeWebServices#moveItem(com.microsoft.exchange.messages.MoveItem)
	 */
	@Override
	public MoveItemResponse moveItem(MoveItem request) {
		MoveItemResponse response = (MoveItemResponse) internalInvoke(request);
		return response;
	}

	/* (non-Javadoc)
	 * @see com.microsoft.exchange.ExchangeWebServices#copyItem(com.microsoft.exchange.messages.CopyItem)
	 */
	@Override
	public CopyItemResponse copyItem(CopyItem request) {
		CopyItemResponse response = (CopyItemResponse) internalInvoke(request);
		return response;
	}

	/* (non-Javadoc)
	 * @see com.microsoft.exchange.ExchangeWebServices#createAttachment(com.microsoft.exchange.messages.CreateAttachment)
	 */
	@Override
	public CreateAttachmentResponse createAttachment(CreateAttachment request) {
		CreateAttachmentResponse response = (CreateAttachmentResponse) internalInvoke(request);
		return response;
	}

	/* (non-Javadoc)
	 * @see com.microsoft.exchange.ExchangeWebServices#deleteAttachment(com.microsoft.exchange.messages.DeleteAttachment)
	 */
	@Override
	public DeleteAttachmentResponse deleteAttachment(DeleteAttachment request) {
		DeleteAttachmentResponse response = (DeleteAttachmentResponse) internalInvoke(request);
		return response;
	}

	/* (non-Javadoc)
	 * @see com.microsoft.exchange.ExchangeWebServices#getAttachment(com.microsoft.exchange.messages.GetAttachment)
	 */
	@Override
	public GetAttachmentResponse getAttachment(GetAttachment request) {
		GetAttachmentResponse response = (GetAttachmentResponse) internalInvoke(request);
		return response;
	}

	/* (non-Javadoc)
	 * @see com.microsoft.exchange.ExchangeWebServices#getDelegate(com.microsoft.exchange.messages.GetDelegate)
	 */
	@Override
	public GetDelegateResponse getDelegate(GetDelegate request) {
		GetDelegateResponse response = (GetDelegateResponse) internalInvoke(request);
		return response;
	}

	/* (non-Javadoc)
	 * @see com.microsoft.exchange.ExchangeWebServices#addDelegate(com.microsoft.exchange.messages.AddDelegate)
	 */
	@Override
	public AddDelegateResponse addDelegate(AddDelegate request) {
		AddDelegateResponse response = (AddDelegateResponse) internalInvoke(request);
		return response;
	}

	/* (non-Javadoc)
	 * @see com.microsoft.exchange.ExchangeWebServices#removeDelegate(com.microsoft.exchange.messages.RemoveDelegate)
	 */
	@Override
	public RemoveDelegateResponse removeDelegate(RemoveDelegate request) {
		RemoveDelegateResponse response = (RemoveDelegateResponse) internalInvoke(request);
		return response;
	}

	/* (non-Javadoc)
	 * @see com.microsoft.exchange.ExchangeWebServices#updateDelegate(com.microsoft.exchange.messages.UpdateDelegate)
	 */
	@Override
	public UpdateDelegateResponse updateDelegate(UpdateDelegate request) {
		UpdateDelegateResponse response = (UpdateDelegateResponse) internalInvoke(request);
		return response;
	}

	/* (non-Javadoc)
	 * @see com.microsoft.exchange.ExchangeWebServices#getUserAvailability(com.microsoft.exchange.messages.GetUserAvailabilityRequest)
	 */
	@Override
	public GetUserAvailabilityResponse getUserAvailability(
			GetUserAvailabilityRequest request) {
		GetUserAvailabilityResponse response = (GetUserAvailabilityResponse) internalInvoke(request, new SoapActionCallback("http://schemas.microsoft.com/exchange/services/2006/messages/GetUserAvailability"));
		return response;
	}

	/* (non-Javadoc)
	 * @see com.microsoft.exchange.ExchangeWebServices#getUserOofSettings(com.microsoft.exchange.messages.GetUserOofSettingsRequest)
	 */
	@Override
	public GetUserOofSettingsResponse getUserOofSettings(
			GetUserOofSettingsRequest request) {
		GetUserOofSettingsResponse response = (GetUserOofSettingsResponse) internalInvoke(request);
		return response;
	}

	/* (non-Javadoc)
	 * @see com.microsoft.exchange.ExchangeWebServices#setUserOofSettings(com.microsoft.exchange.messages.SetUserOofSettingsRequest)
	 */
	@Override
	public SetUserOofSettingsResponse setUserOofSettings(
			SetUserOofSettingsRequest request) {
		SetUserOofSettingsResponse response = (SetUserOofSettingsResponse) internalInvoke(request);
		return response;
	}
	
	/* (non-Javadoc)
	 * @see com.microsoft.exchange.ExchangeWebServices#getServerTimeZones(com.microsoft.exchange.messages.GetServerTimeZones)
	 */
	@Override
	public GetServerTimeZonesResponse getServerTimeZones(
			GetServerTimeZones request) {
		GetServerTimeZonesResponse response = (GetServerTimeZonesResponse) internalInvoke(request);
		return response;
	}
	
	@Override
	public EmptyFolderResponse emptyFolder(EmptyFolder request) {
		EmptyFolderResponse response = (EmptyFolderResponse) internalInvoke(request);
		return response;
	}
	

	/**
	 * 
	 * @param request
	 * @return
	 */
	protected Object internalInvoke(Object request) {
		return internalInvoke(request, null);
	}
	
	/**
	 * 
	 * @param request
	 * @return
	 */
	protected Object internalInvoke(Object request, WebServiceMessageCallback callback) {
		try {
			Object result;
			log.trace("ExchangeRequest="+request);
			if(null == callback) {
				result = getWebServiceTemplate().marshalSendAndReceive(request);
			} else {
				result = getWebServiceTemplate().marshalSendAndReceive(request, callback);
			}
			return result;
		} catch (SoapFaultClientException e) {
			
			if(e.getMessage().equals("The impersonation principal name is invalid.")) {
				throw new ExchangeInvalidUPNRuntimeException(e);
			}
			
			if(log.isTraceEnabled()) {
				log.error("SoapFaultClientException encountered for " + request+".  "+e.getMessage());
			}else {
				log.error("SoapFaultClientException encountered "+e.getMessage());
			}
			
			throw new ExchangeWebServicesRuntimeException(e);
		}
	}
	
}
