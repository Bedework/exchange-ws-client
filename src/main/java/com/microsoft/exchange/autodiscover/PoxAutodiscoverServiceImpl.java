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
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.w3c.dom.CharacterData;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.microsoft.exchange.exception.AutodiscoverException;
import com.microsoft.exchange.exception.PoxAutodiscoverException;

/**
 * An autodiscover implementation that queries all potential POX
 * autodiscover endpoints for a given email address
 * 
 * @see <a
 *      href="http://msdn.microsoft.com/EN-US/library/office/ee332364(v=exchg.140).aspx">Implementing
 *      an Autodiscover Client in Microsoft Exchange</a>
 * 
 * @author ctcudd
 *
 */
public class PoxAutodiscoverServiceImpl extends AbstractExchangeAutodiscoverService{

	/**
	 * client should be configured with PoolingClientConnectionManager and Autodiscovery Redirect Strategy
	 */
	private DefaultHttpClient httpClient;
	protected final Log log = LogFactory.getLog(this.getClass());
	private static final String ENDPOINT_SUFFIX = "xml";
	
	@Override
	public String getServiceSuffix(){
		return ENDPOINT_SUFFIX;
	}

	private static final String POX_REQUEST_FORMAT = "<?xml version=\"1.0\" encoding=\"utf-8\"?>" +
													 "<Autodiscover xmlns=\"http://schemas.microsoft.com/exchange/autodiscover/outlook/requestschema/2006\">" +
													 "<Request>"+
													 "<EMailAddress>%s</EMailAddress>"+
													 "<AcceptableResponseSchema>http://schemas.microsoft.com/exchange/autodiscover/outlook/responseschema/2006a</AcceptableResponseSchema>"+
													 "</Request>"+
													 "</Autodiscover>";
	
	private static final String AS_REQUEST_FORMAT = "<?xml version=\"1.0\" encoding=\"utf-8\"?>" +
			 "<Autodiscover xmlns=\"http://schemas.microsoft.com/exchange/autodiscover/mobilesync/requestschema/2006\">" +
			 "<Request>"+
			 "<EMailAddress>%s</EMailAddress>"+
             "<AcceptableResponseSchema>http://schemas.microsoft.com/exchange/autodiscover/mobilesync/responseschema/2006</AcceptableResponseSchema>" +
			 "</Request>"+
			 "</Autodiscover>";
	
	
	@Override
	public String getAutodiscoverEndpoint(String email) throws AutodiscoverException {
		String ewsUrl = null;
		String responseString = null;
		String payload = String.format(POX_REQUEST_FORMAT,email);
		
		for(String potential : getPotentialAutodiscoverEndpoints(email)){
			log.info("attempting pox autodiscover for email="+email+" uri="+potential);
			HttpPost request = new HttpPost(potential);
			StringEntity requestEntity = new StringEntity(payload, getContentType());
			request.setEntity(requestEntity);
			try {
				HttpResponse response = executeInternal(request);
				responseString = parseHttpResponseToString(response);
				if(StringUtils.isNotBlank(responseString)){
					ewsUrl = parseResponseString(responseString);
					if(StringUtils.isNotBlank(ewsUrl))
						return ewsUrl;
				}
			} catch (Exception e) {
				log.warn("caught exception while attempting POX autodiscover: "+e.getMessage());
			}
		}
		throw new PoxAutodiscoverException("POX autodiscover failed.  cannot find ewsurl for email="+email);
	}


	
	private HttpResponse executeInternal(HttpPost request) {
		HttpResponse response = null;
		try {
			response = getHttpClient().execute(request);
		} catch (Exception e) {
			log.error("Failed to execute request="+request+". "+e.getMessage());
		}
		return response;
	}
	
	/**
	 * Parses Autodiscover response {@see http://msdn.microsoft.com/en-us/library/office/bb204082(v=exchg.150).aspx}
	 * Looking for an EWS url.
	 * 
	 * 
	 * mostly from http://dev.dartmouth.edu/svn/softdev/email/exchange/exchangeweb/trunk/src/edu/dartmouth/protocol/autodiscover/pox/POXAutodiscover.java
	 * 
	 * @param xmlResponseString
	 * @return 
	 * @throws IOException 
	 * @throws SAXException 
	 * @throws ParserConfigurationException 
	 */
	public String parseResponseString(String xmlResponseString) throws PoxAutodiscoverException, SAXException, IOException, ParserConfigurationException{
		
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = dbf.newDocumentBuilder();
    	InputSource is = new InputSource();
   		is.setCharacterStream(new StringReader(xmlResponseString));
		Document doc = db.parse(is);
   		
   		// Verify there's an Autodiscover element, if not, response is invalid
		NodeList autodiscover = doc.getElementsByTagName("Autodiscover");
		if(autodiscover.getLength() != 1) {
			throw(new PoxAutodiscoverException("Autodiscover tag not found in response: " + xmlResponseString));
		}

		// Verify there's an Action element, if not, response is invalid
		NodeList action = doc.getElementsByTagName("Action");
		if(action.getLength() != 1) {
			throw(new PoxAutodiscoverException("No Action nodes found in response: " + xmlResponseString));
		}

		// Check value of Action element for redirects
      	Element line = (Element) action.item(0);
		String actionData = getCharacterDataFromElement(line);
		if(actionData == null) {
			throw(new PoxAutodiscoverException("Unable to read data from Action element: " + xmlResponseString));
		}

		// Redirect, a URL will be provided in RedirectUrl Element
		if(actionData.toLowerCase().equals("redirecturl")) {

			NodeList redirectUrl = doc.getElementsByTagName("RedirectUrl");
			if(redirectUrl.getLength() != 1) {
				throw(new PoxAutodiscoverException("Expected redirectUrl node not found in response: " + xmlResponseString));
			}

       		line = (Element) redirectUrl.item(0);
			String redirectUrlData = getCharacterDataFromElement(line);
			if(redirectUrlData == null) {
				throw(new PoxAutodiscoverException("Unable to read data from RedirectUrl element: " + xmlResponseString));
			}
			throw(new PoxAutodiscoverException("RedirectUrl = "+redirectUrlData));

		// Redirect, a new mailbox be provided in RedirectAddr Element
		} else if (actionData.toLowerCase().equals("redirectaddr")) {
			NodeList redirectAddr = doc.getElementsByTagName("RedirectAddr");
			if(redirectAddr.getLength() != 1) {
				throw(new PoxAutodiscoverException("Expected redirectAddr node not found in response: " + xmlResponseString));
			}
	    	line = (Element) redirectAddr.item(0);
			String redirectAddrData = getCharacterDataFromElement(line);
			if(redirectAddrData == null) {
				throw(new PoxAutodiscoverException("Unable to read data from RedirectAddr element: " + xmlResponseString));
			}
			throw(new PoxAutodiscoverException("RedirectAddr = "+redirectAddrData));
		}
			
		// Verify there's a Protocol element, if not, response is invalid
        NodeList protocols = doc.getElementsByTagName("Protocol");
		if(protocols.getLength() < 1) {
			throw(new PoxAutodiscoverException("No protocol nodes found in response: " + xmlResponseString));
		}
		for (int i = 0; i < protocols.getLength(); i++) {
       		Element element = (Element) protocols.item(i);
    		NodeList type = element.getElementsByTagName("Type");
			if(type.getLength() != 1) {
				throw(new PoxAutodiscoverException("Expected Type node not found in response: " + xmlResponseString));
			}
       	    line = (Element) type.item(0);
			String typeData = getCharacterDataFromElement(line);
			if(typeData == null) {
				throw(new PoxAutodiscoverException("Unable to read data from Type element: " + xmlResponseString));
			}

			// Look for Protocol type "EXCH" 
			if(typeData.toLowerCase().equals("exch")) {
       			NodeList server  = element.getElementsByTagName("Server");
				if(server.getLength() != 1) {
					throw(new PoxAutodiscoverException("Expected Server node not found in EXCH Protocol node in response: " + xmlResponseString));
				}
				line = (Element) server.item(0);
				String exchangeServer = getCharacterDataFromElement(line);
				if(exchangeServer == null) {
					throw(new PoxAutodiscoverException("Unable to read data from Server element in EXCH Protocol node: " + xmlResponseString));
				}
				NodeList ewsUrl = element.getElementsByTagName("EwsUrl");
				if(ewsUrl.getLength() != 1) {
					throw(new PoxAutodiscoverException("Expected EwsUrl node not found in EXCH Protocol node in response: " + xmlResponseString));
				}
           		line = (Element) ewsUrl.item(0);
				String exchangeEwsUrl = getCharacterDataFromElement(line);
				if(exchangeEwsUrl == null) {
					throw(new PoxAutodiscoverException("Unable to read data from EwsUrl element in EXCH Protocol node: " + xmlResponseString));
				}
				return exchangeEwsUrl;
			}
       	}

		// If we reach this point, no EXCH Protocol found in response
		throw(new PoxAutodiscoverException("Expected EXCH Type Protocol node not found in response: " + xmlResponseString));

	}
	
	/**
	 * Static utility method to extract text from an XML element
	 * @param e - an xml element
	 * @return the character datafrom the first child element as a string
	 */
	private static String getCharacterDataFromElement(Element e) {
		Node child = e.getFirstChild();
		if (child instanceof CharacterData) {
   			CharacterData cd = (CharacterData) child;
   			return cd.getData();
		}
		return null;
	}

	/**
	 * Copies the content form an HTTP response to a String and consumes the response entity.
	 * @param response
	 * @return
	 */
	private String parseHttpResponseToString(HttpResponse response) {
		String responseContent = "";
		StatusLine statusLine = response.getStatusLine();
		HttpEntity responseEntity = response.getEntity();
		StringWriter outputStream = new StringWriter();
		InputStream inputStream = null;
		try {
			inputStream = responseEntity.getContent();
			IOUtils.copy(inputStream, outputStream);
			responseContent = outputStream.toString();
			log.trace("HttpResponse: StatusCode="+statusLine.getStatusCode()+",Message="+statusLine.getReasonPhrase()+",Response="+responseContent);
			
		} catch (IOException e) {
			log.warn("Failed to parse HttpResponse:" +response, e);
			
		} finally {
			quietlyConsume(responseEntity);
		}
		return responseContent;
	}
	
	private void quietlyConsume(HttpEntity entity) {
		try {
			EntityUtils.consume(entity);
		} catch (IOException e) {
			log.info("caught IOException from EntityUtils#consume", e);
		}
	}
	
	public DefaultHttpClient getHttpClient() {
		return httpClient;
	}

	@Autowired
	public void setHttpClient(DefaultHttpClient httpClient) {
		this.httpClient = httpClient;
	}

}
