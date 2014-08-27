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

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.ws.WebServiceMessage;
import org.springframework.ws.client.WebServiceClientException;
import org.springframework.ws.client.support.interceptor.ClientInterceptor;
import org.springframework.ws.context.MessageContext;
import org.springframework.ws.soap.SoapEnvelope;
import org.springframework.ws.soap.SoapHeader;
import org.springframework.ws.soap.SoapMessage;

import com.ibm.icu.util.TimeZone;
import com.microsoft.exchange.exception.ExchangeWebServicesRuntimeException;
import com.microsoft.exchange.types.TimeZoneContext;
import com.microsoft.exchange.types.TimeZoneDefinitionType;

/**
 * 
 * The TimeZoneContext element is used in the Simple Object Access Protocol
 * (SOAP) header to specify the time zone definition that is to be used as the
 * default when assigning the time zone for the DateTime properties of objects
 * that are created, updated, and retrieved by using Exchange Web Services
 * (EWS).
 * 
 * @see <a href="http://msdn.microsoft.com/en-us/library/dd899417(EXCHG.140).aspx>Working with Time Zones in Exchange 2010 Exchange Web Services</a>
 * @see <a href="http://msdn.microsoft.com/en-us/library/office/dd899417(v=exchg.150).aspx">TimeZoneContext</a>
 * @see <a href="http://support.microsoft.com/kb/973627">Microsoft Time Zone Index Values</a>'
 * 
 * Other important links for consideration:
 * <a href="http://icu-project.org/apiref/icu4j/com/ibm/icu/util/TimeZone.html">ICU4J</a> has a set of mappings for windows timezones. 
 * They get the info from unicode.org, I assume I can trust their mappings:  http://cldr.unicode.org/development/development-process/design-proposals/extended-windows-olson-zid-mapping
 * http://www.unicode.org/cldr/charts/latest/supplemental/zone_tzid.html
 * @author ctcudd
 * 
 */
public class RequestServerTimeZoneInterceptor implements ClientInterceptor, InitializingBean {

	protected final Log log = LogFactory.getLog(this.getClass());
	private JAXBContext jaxbContext;

	/**
	 * @return the jaxbContext
	 */
	public JAXBContext getJaxbContext() {
		return jaxbContext;
	}

	/**
	 * @param jaxbContext
	 *            the jaxbContext to set
	 */
	@Autowired
	public void setJaxbContext(JAXBContext jaxbContext) {
		this.jaxbContext = jaxbContext;
	}

	//try to set this with a valid windows time zone id which matches the jvm timezone
	private String windowsTimeZoneID;
	
	public String getWindowsTimeZoneID(){
		return this.windowsTimeZoneID;
	}

	//as long as we set the time zone context to match the jvm time zone, things should be ok.  UTC should match regardless of environment(s)
	public static final String FALLBACK_TIMEZONE_ID="UTC";
	
	@Override
	public boolean handleRequest(MessageContext messageContext)
			throws WebServiceClientException {
		WebServiceMessage request = messageContext.getRequest();
		
		if(!isTimeZoneValid()){
			throw new ExchangeWebServicesRuntimeException("RequestServerTimeZoneInterceptor - the windowsTimeZoneID specified ("+this.windowsTimeZoneID+") does not match this systems default time zone ("+TimeZone.getDefault().getID()+")");
		}
		
		if (request instanceof SoapMessage) {
			SoapMessage soapMessage = (SoapMessage) request;
			SoapEnvelope envelope = soapMessage.getEnvelope();
			SoapHeader header = envelope.getHeader();

			TimeZoneContext tzc = new TimeZoneContext();
			TimeZoneDefinitionType timeZoneDef = new TimeZoneDefinitionType();
			timeZoneDef.setId(windowsTimeZoneID);
			tzc.setTimeZoneDefinition(timeZoneDef);

			try {
				Marshaller m = jaxbContext.createMarshaller();
				m.marshal(tzc, header.getResult());
			} catch (JAXBException e) {
				log.error(
						"JAXBException raised while attempting to add TimeZoneContext to soap header "
								+ tzc, e);
				throw new ExchangeWebServicesRuntimeException(
						"JAXBException raised while attempting to add TimeZoneContext to soap header "
								+ tzc, e);
			}
		}
		return true;
	}

	@Override
	public boolean handleResponse(MessageContext messageContext)
			throws WebServiceClientException {
		return true;
	}

	@Override
	public boolean handleFault(MessageContext messageContext)
			throws WebServiceClientException {
		return true;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		boolean timeZoneSet = false;
		TimeZone jvmTimeZone = TimeZone.getDefault();
		if(jvmTimeZone != null){
			String windowsID = TimeZone.getWindowsID(jvmTimeZone.getID());
			if(StringUtils.isNotBlank(windowsID)){
				log.info("windows time zone context has been set to '"+windowsID+"'.  All dates and times sent to (or recieved from) EWS must use this timezone information.");
				this.windowsTimeZoneID = windowsID;
				timeZoneSet=true;
			}else{
				log.warn("No windows time zone mapping for "+jvmTimeZone.getID());
			}
		}else{
			log.warn("jvm timezone is not set, this should never happen");
		}
		
		if(!timeZoneSet){
			log.warn("Failed to identify a matching time zone scheme.  Falling back to UTC.");
			TimeZone fallbackTimeZone = TimeZone.getTimeZone(FALLBACK_TIMEZONE_ID);
			TimeZone.setDefault(fallbackTimeZone);
			this.windowsTimeZoneID = FALLBACK_TIMEZONE_ID;
		}
	}
	
	public boolean isTimeZoneValid(){
		boolean tzValid = false;
		TimeZone tzDefault = TimeZone.getDefault();
		if(null != tzDefault && StringUtils.isNotBlank(tzDefault.getID())){
			String windowsID = TimeZone.getWindowsID(tzDefault.getID());
			tzValid = this.windowsTimeZoneID.equals(windowsID);	
		}
		return tzValid;
	}

}
