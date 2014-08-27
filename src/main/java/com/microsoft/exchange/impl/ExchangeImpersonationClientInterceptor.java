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

import com.microsoft.exchange.types.ConnectingSIDType;
import com.microsoft.exchange.types.ExchangeImpersonation;

/**
 * {@link ClientInterceptor} for adding an {@link ExchangeImpersonation} to a soap 
 * request's {@link SoapHeader}.
 * 
 * The technique for using Exchange Impersonation is documented here:
 * a href="http://msdn.microsoft.com/en-us/library/bb204088.aspx">http://msdn.microsoft.com/en-us/library/bb204088.aspx</a>
 * 
 * Setting up exchange impersonation for Office 365 accounts:
 * <a href="http://msdn.microsoft.com/en-us/library/exchange/gg194012(v=exchg.140).aspx">http://msdn.microsoft.com/en-us/library/exchange/gg194012(v=exchg.140).aspx</a>
 * 
 * @author Nicholas Blair
 */
public class ExchangeImpersonationClientInterceptor implements
ClientInterceptor {

	protected final Log log = LogFactory.getLog(this.getClass());
	private ImpersonationConnectingSIDSource connectingSIDSource;
	private JAXBContext jaxbContext;
	/**
	 * @return the connectingSIDSource
	 */
	public ImpersonationConnectingSIDSource getConnectingSIDSource() {
		return connectingSIDSource;
	}
	/**
	 * @param connectingSIDSource the connectingSIDSource to set
	 */
	@Autowired
	public void setConnectingSIDSource(ImpersonationConnectingSIDSource connectingSIDSource) {
		this.connectingSIDSource = connectingSIDSource;
	}
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

			ConnectingSIDType connectingSID = connectingSIDSource.getConnectingSID(soapMessage, messageContext);
			if(connectingSID != null) {
				ExchangeImpersonation impersonation = new ExchangeImpersonation();
				impersonation.setConnectingSID(connectingSID);
				
				SoapEnvelope envelope = soapMessage.getEnvelope();
				SoapHeader header = envelope.getHeader();

				try {
					Marshaller m = jaxbContext.createMarshaller();
					m.marshal(impersonation, header.getResult());
					
				} catch (JAXBException e) {
					log.error("JAXBException raised while attempting to add ExchangeImpersonation header with SID: " + connectingSID, e);
					throw new ExchangeImpersonationException("JAXBException raised while attempting to add ExchangeImpersonation header with SID: " + connectingSID, e);
				}
			} else {
				if(log.isDebugEnabled()) {
					log.debug("no connectingSID found for " + soapMessage);
				}
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

	/**
	 * {@link WebServiceClientException} raised if there was a problem adding the ExchangeImpersonation
	 * to the soap header.
	 * 
	 * @author Nicholas Blair
	 */
	public static class ExchangeImpersonationException extends WebServiceClientException {

		/**
		 * 
		 */
		private static final long serialVersionUID = -6792584434434405497L;
		/**
		 * 
		 * @param msg
		 */
		public ExchangeImpersonationException(String msg) {
			super(msg);
		}
		/**
		 * @param msg
		 * @param ex
		 */
		public ExchangeImpersonationException(String msg, Throwable ex) {
			super(msg, ex);
		}

	}
}
