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
package com.microsoft.exchange.autodiscover;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;
import javax.xml.transform.TransformerException;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.oxm.Marshaller;
import org.springframework.ws.WebServiceMessage;
import org.springframework.ws.client.core.WebServiceMessageCallback;
import org.springframework.ws.client.core.WebServiceOperations;
import org.springframework.ws.soap.SoapMessage;
import org.springframework.ws.soap.client.core.SoapActionCallback;
import org.springframework.xml.transform.StringResult;

import com.microsoft.exchange.exception.AutodiscoverException;
import com.microsoft.exchange.exception.ExchangeWebServicesRuntimeException;
import com.microsoft.exchange.exception.SoapAutodiscoverException;

/**
 * An autodiscover implementation that queries all potential SOAP
 * autodiscover endpoints for a given email address
 * 
 * @see <a
 *      href="http://msdn.microsoft.com/EN-US/library/office/ee332364(v=exchg.140).aspx">Implementing
 *      an Autodiscover Client in Microsoft Exchange</a>
 * 
 * @author ctcudd
 *
 */
public class SoapAutodiscoverServiceImpl extends AbstractExchangeAutodiscoverService{

	protected final static String AUTODISCOVER_SCHEMA = "http://schemas.microsoft.com/exchange/autodiscover/outlook/requestschema/2006";
    protected final static String AUTODISCOVER_RESPONSE_SCHEMA = "http://schemas.microsoft.com/exchange/autodiscover/outlook/responseschema/2006a";
    protected final static QName REQUEST_SERVER_VERSION_QNAME = new QName(
            "http://schemas.microsoft.com/exchange/2010/Autodiscover", "RequestedServerVersion", "a");
    protected final static QName SOAP_ACTION_HEADER_QNAME = new QName("http://www.w3.org/2005/08/addressing", "Action", "wsa");

    protected final static String SOAP_ACTION_BASE = "http://schemas.microsoft.com/exchange/2010/Autodiscover/Autodiscover/";
    protected final static String GET_USER_SETTINGS_ACTION = SOAP_ACTION_BASE + "GetUserSettings";
    private final static String INTERNAL_EWS_SERVER = "InternalEwsUrl";
    private final static String EXTERNAL_EWS_SERVER = "ExternalEwsUrl";
    private final static String ENDPOINT_SUFFIX = "svc";
    
    @Override
	public String getServiceSuffix(){
		return ENDPOINT_SUFFIX;
	}
    
    private final ObjectFactory objectFactory = new ObjectFactory();

    @Autowired
    @Qualifier("autodiscoverWebServiceTemplate")
    private WebServiceOperations webServiceOperations;
    public WebServiceOperations getWebServiceOperations() {
		return webServiceOperations;
	}
    public void setWebServiceOperations(WebServiceOperations webServiceOperations) {
        this.webServiceOperations = webServiceOperations;
    }
    
    private Marshaller marshaller;
    
	public Marshaller getMarshaller() {
		return marshaller;
	}


	public void setMarshaller(Marshaller marshaller) {
		this.marshaller = marshaller;
	}
    
	private String parseGetUserSettingsResponse( GetUserSettingsResponseMessage response) throws SoapAutodiscoverException {
		GetUserSettingsResponse soapResponse = response.getResponse().getValue();
		UserSettings userSettings = null;
		boolean warning = false;
        boolean error = false;
        StringBuilder msg = new StringBuilder();
        if (!ErrorCode.NO_ERROR.equals(soapResponse.getErrorCode())) {
            error = true;
            msg.append("Error: ").append(soapResponse.getErrorCode().value())
                    .append(": ").append(soapResponse.getErrorMessage().getValue()).append("\n");
        } else {
            JAXBElement<ArrayOfUserResponse> JAXBresponseArray = soapResponse.getUserResponses();
            ArrayOfUserResponse responseArray = JAXBresponseArray != null ? JAXBresponseArray.getValue() : null;
            List<UserResponse> responses = responseArray != null ? responseArray.getUserResponses() : new ArrayList<UserResponse>();
            if (responses.size() == 0) {
                error = true;
                msg.append("Error: Autodiscovery returned no Exchange mail server for mailbox");
            } else if (responses.size() > 1) {
                warning = true;
                msg.append("Warning: Autodiscovery returned multiple responses for Exchange server mailbox query");
            } else {
                UserResponse userResponse = responses.get(0);
                if (!ErrorCode.NO_ERROR.equals(userResponse.getErrorCode())) {
                    error = true;
                    msg.append("Received error message obtaining user mailbox's server. Error "
                            + userResponse.getErrorCode().value() + ": " + userResponse.getErrorMessage().getValue());
                }
                userSettings = userResponse.getUserSettings().getValue();
            }
        }
        if (warning || error) {
            throw new SoapAutodiscoverException("Unable to perform soap operation; try again later. Message text: "
                    + msg.toString());
        }
        //return userSettings;
		//UserSettings userSettings = sendMessageAndExtractSingleResponse(getUserSettingsSoapMessage, GET_USER_SETTINGS_ACTION);
        
        
        //Give preference to Internal URL over External URL
        String internalUri = null;
        String externalUri = null;

        for (UserSetting userSetting : userSettings.getUserSettings()) {
            String potentialAccountServiceUrl = ((StringSetting) userSetting).getValue().getValue();
            if (EXTERNAL_EWS_SERVER.equals(userSetting.getName())) {
                externalUri = potentialAccountServiceUrl;
            }
            if (INTERNAL_EWS_SERVER.equals(userSetting.getName())) {
                internalUri = potentialAccountServiceUrl;
            }
        }
        if (internalUri == null && externalUri == null) {
            throw new ExchangeWebServicesRuntimeException("Unable to find EWS Server URI in properies "
                    + EXTERNAL_EWS_SERVER + " or " + INTERNAL_EWS_SERVER + " from User's Autodiscover record");
        }
        return internalUri != null ? internalUri : externalUri;
	}

    private GetUserSettingsRequestMessage createGetUserSettingsSoapMessage(String emailAddress) {
        GetUserSettingsRequest msg = objectFactory.createGetUserSettingsRequest();

        User user = objectFactory.createUser();
        user.setMailbox(emailAddress);
        Users users = objectFactory.createUsers();
        users.getUsers().add(user);
        msg.setUsers(users);

        msg.setRequestedVersion(ExchangeVersion.EXCHANGE_2010);

        RequestedSettings settings = objectFactory.createRequestedSettings();
        settings.getSettings().add(EXTERNAL_EWS_SERVER);
        settings.getSettings().add(INTERNAL_EWS_SERVER);
        msg.setRequestedSettings(settings);

        // Construct the SOAP request object to use
        GetUserSettingsRequestMessage request = objectFactory.createGetUserSettingsRequestMessage();
        request.setRequest(objectFactory.createGetUserSettingsRequestMessageRequest(msg));
        return request;
    }
    

    /**
     * 
     * @param soapRequest
     * @param soapAction
     * @param uri
     * @return
     */
    private GetUserSettingsResponseMessage getUserSettings(String uri, GetUserSettingsRequestMessage soapRequest, final String soapAction) {
    	GetUserSettingsResponseMessage response = null;
        final WebServiceMessageCallback actionCallback = new SoapActionCallback(
                soapAction);

        final WebServiceMessageCallback customCallback = new WebServiceMessageCallback() {

            @Override
            public void doWithMessage(WebServiceMessage message) throws IOException, TransformerException {
                actionCallback.doWithMessage(message);
                SoapMessage soap = (SoapMessage) message;
                soap.getEnvelope().getHeader().addHeaderElement(REQUEST_SERVER_VERSION_QNAME)
                        .setText(ExchangeVersion.EXCHANGE_2010.value());
                soap.getEnvelope().getHeader().addHeaderElement(SOAP_ACTION_HEADER_QNAME).setText(soapAction);
            }

        };
        if (log.isDebugEnabled()) {
            StringResult message = new StringResult();
            try {
                marshaller.marshal(soapRequest, message);
                log.debug("Attempting to send SOAP request to "+uri+"\nSoap Action: "+soapAction+"\nSoap message body"
                        + " (not exact, log org.apache.http.wire to see actual message):\n"+ message);
            } catch (IOException ex) {
                log.debug("IOException attempting to display soap response", ex);
            }
        }

        // use the request to retrieve data from the Exchange server
        try{
        	response = (GetUserSettingsResponseMessage) webServiceOperations.marshalSendAndReceive(uri, soapRequest, customCallback);
        }catch(Exception e){
        	log.warn("getUserSettings for uri="+uri+" failed: "+e.getMessage());
        }
        if (log.isDebugEnabled()) {
            StringResult messageResponse = new StringResult();
            try {
                marshaller.marshal(response, messageResponse);
                log.debug("Soap response body (not exact, log org.apache.http.wire to see actual message):\n"+messageResponse );
            } catch (IOException exception) {
                log.debug("IOException attempting to display soap response", exception);
            }
        }
        return response;
    }

	@Override
	public String getAutodiscoverEndpoint(String email) throws AutodiscoverException {
		String ewsUrl = null;
		GetUserSettingsRequestMessage request = createGetUserSettingsSoapMessage(email);
		
		for(String potential : getPotentialAutodiscoverEndpoints(email)){
			log.info("attempting soap autodiscover for email="+email+" uri="+potential);
			GetUserSettingsResponseMessage response = null;
			 try {
				response = getUserSettings(potential, request, GET_USER_SETTINGS_ACTION);
				if(null != response){
					ewsUrl = parseGetUserSettingsResponse(response);
					if(StringUtils.isNotBlank(ewsUrl))
						return ewsUrl;
				}
			} catch (Exception e) {
				log.warn("caught exception while attempting SOAP autodiscover : "+e.getMessage());
			} 
		}
		throw new SoapAutodiscoverException("SOAP autodiscover failed.  cannot find ewsurl for email="+email);
	}

}


