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

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ws.WebServiceMessage;
import org.springframework.ws.client.WebServiceClientException;
import org.springframework.ws.client.support.interceptor.ClientInterceptor;
import org.springframework.ws.context.MessageContext;
import org.springframework.ws.soap.SoapEnvelope;
import org.springframework.ws.soap.SoapHeader;
import org.springframework.ws.soap.SoapMessage;

import com.microsoft.exchange.exception.ExchangeWebServicesRuntimeException;
import com.microsoft.exchange.impl.http.CredentialsProviderFactory;
import com.microsoft.exchange.types.ExchangeVersionType;
import com.microsoft.exchange.types.RequestServerVersion;

/**
 * {@link ClientInterceptor} to add a {@link RequestServerVersion}
 * with the configured {@link ExchangeVersionType} to the SOAP Header.
 * 
 * Note that while Microsoft documents the {@link RequestServerVersion} element in the header
 * as OPTIONAL, you will need to require this interceptor if you are using the {@link CredentialsProviderFactory}
 * integration strategy (as opposed to impersonation).
 * 
 * @author Nicholas Blair
 */
public class RequestServerVersionClientInterceptor implements ClientInterceptor {

	protected final Log log = LogFactory.getLog(this.getClass()); 
	private JAXBContext jaxbContext;
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
	
	
	/* (non-Javadoc)
	 * @see org.springframework.ws.client.support.interceptor.ClientInterceptor#handleRequest(org.springframework.ws.context.MessageContext)
	 */
	@Override
	public boolean handleRequest(MessageContext messageContext)
			throws WebServiceClientException {
		WebServiceMessage request = messageContext.getRequest();
		if(request instanceof SoapMessage) {
			SoapMessage soapMessage = (SoapMessage) request;
			SoapEnvelope envelope = soapMessage.getEnvelope();
			SoapHeader header = envelope.getHeader();
			RequestServerVersion rsv = new RequestServerVersion();

			try {
				Marshaller m = jaxbContext.createMarshaller();
				m.marshal(rsv, header.getResult());
			} catch (JAXBException e) {
				log.error("JAXBException raised while attempting to add RequestServerVersion to soap header " + rsv, e);
				throw new ExchangeWebServicesRuntimeException("JAXBException raised while attempting to add RequestServerVersion to soap header " + rsv, e);
			}
		}
		return true;
	}

	/* (non-Javadoc)
	 * @see org.springframework.ws.client.support.interceptor.ClientInterceptor#handleResponse(org.springframework.ws.context.MessageContext)
	 */
	@Override
	public boolean handleResponse(MessageContext messageContext)
			throws WebServiceClientException {
		return true;
	}

	/* (non-Javadoc)
	 * @see org.springframework.ws.client.support.interceptor.ClientInterceptor#handleFault(org.springframework.ws.context.MessageContext)
	 */
	@Override
	public boolean handleFault(MessageContext messageContext)
			throws WebServiceClientException {
		return true;
	}

}
