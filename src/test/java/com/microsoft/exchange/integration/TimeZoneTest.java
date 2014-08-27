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
package com.microsoft.exchange.integration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TimeZone;
import java.util.TreeMap;

import javax.annotation.Resource;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;

import net.fortuna.ical4j.model.TimeZoneRegistryFactory;

import org.apache.commons.lang.time.StopWatch;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.microsoft.exchange.ExchangeEventConverterOLD;
import com.microsoft.exchange.impl.ThreadLocalImpersonationConnectingSIDSourceImpl;
import com.microsoft.exchange.messages.ArrayOfResponseMessagesType;
import com.microsoft.exchange.messages.GetServerTimeZones;
import com.microsoft.exchange.messages.GetServerTimeZonesResponse;
import com.microsoft.exchange.messages.GetServerTimeZonesResponseMessageType;
import com.microsoft.exchange.messages.ResponseMessageType;
import com.microsoft.exchange.types.ArrayOfTimeZoneDefinitionType;
import com.microsoft.exchange.types.ConnectingSIDType;
import com.microsoft.exchange.types.TimeZoneDefinitionType;

import edu.emory.mathcs.backport.java.util.Arrays;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations="classpath:/com/microsoft/exchange/exchangeContext-usingCredentials.xml")
public class TimeZoneTest extends AbstractIntegrationTest{

	@Value("${username}")
	private String userName;
	@Value("${password}")
	private String password;
	@Value("${username}")
	private String emailAddress;
	
	@Resource
	@Qualifier("timeZoneMap")
	public HashMap<String, String> timeZoneMap;
	
	
	/* (non-Javadoc)
	 * @see com.microsoft.exchange.integration.AbstractIntegrationTest#initializeCredentials()
	 */
	@Override
	public void initializeCredentials() {
		ConnectingSIDType connectingSID = new ConnectingSIDType();
		connectingSID.setPrincipalName(emailAddress);
		ThreadLocalImpersonationConnectingSIDSourceImpl.setConnectingSID(connectingSID);
	}
	
	
	@Test
	public void testGetServerTimeZones() throws JAXBException, ParseException {
		
		net.fortuna.ical4j.model.TimeZoneRegistry registry = TimeZoneRegistryFactory.getInstance().createRegistry();
        StringBuilder sb = new StringBuilder();
		initializeCredentials();
		Map<String, String> mMap = new HashMap<String, String>();
		
		GetServerTimeZones request = new GetServerTimeZones();
		request.setReturnFullTimeZoneData(false);
		
		StopWatch stopWatch = new StopWatch();
		stopWatch.start();
		GetServerTimeZonesResponse response = ewsClient.getServerTimeZones(request);
		stopWatch.stop();
		Assert.assertNotNull(response);
		String captured = capture(response);
		log.debug("GetServerTimeZones request completed in " + stopWatch + ".");
		
		ArrayOfResponseMessagesType responseMessages = response.getResponseMessages();
		for(JAXBElement<? extends ResponseMessageType> rm :responseMessages.getCreateItemResponseMessagesAndDeleteItemResponseMessagesAndGetItemResponseMessages()){
			GetServerTimeZonesResponseMessageType serverTimeZonesResponse = (GetServerTimeZonesResponseMessageType) rm.getValue();
			ArrayOfTimeZoneDefinitionType serverTimeZoneDefs =  serverTimeZonesResponse.getTimeZoneDefinitions();
			for(TimeZoneDefinitionType serverTimeZone : serverTimeZoneDefs.getTimeZoneDefinitions()){
				String tzName = serverTimeZone.getName();
				String utcString = tzName.substring(4,tzName.indexOf(")"));
				assertNotNull(utcString);
				int rawOffSet =0;
				
				if(utcString.length() > 0){
					//offset is present, extract hours and minutes
					String hourOffsetString = utcString.substring(0,utcString.indexOf(":"));
					String minOffsetString = utcString.substring(utcString.indexOf(":")+1);
					int hourMultiplier = hourOffsetString.contains("-") ?- 1 : 1;
					
					int hourOffSet = Integer.parseInt(hourOffsetString.substring(1),10) * hourMultiplier;
					int minOffSet = Integer.parseInt(minOffsetString,10);
					
					assert(hourOffSet >= -12 && hourOffSet <= 13);
					assert(minOffSet >= 0 && minOffSet <= 60);
					
					rawOffSet =  3600000*hourOffSet + 60000*minOffSet;
				}
					//select the shortest valid timezone string.  Note some tz values (such as CST) produce null values
					String[] potentialTimeZones = net.fortuna.ical4j.model.TimeZone.getAvailableIDs(rawOffSet);
					TimeZone icalChosenTimeZone = null;
					int icalTimeZoneScore = 0;
					
					for(String tz : potentialTimeZones){
						TimeZone icalTimZone = null;
						try{
							 icalTimZone = registry.getTimeZone(tz);
						}catch(Exception e){
							//failed to retrive timezone
						}
                		if(!tz.isEmpty() && icalTimZone!=null ){
                			
                			int matchScore =0;
                			List<String> zoneWords = new ArrayList<String>();
                			List<String> serverWords = new ArrayList<String>();
                			
                			serverWords.addAll(Arrays.asList(serverTimeZone.getName().replace(",","").split(" ")));
                			serverWords.addAll(Arrays.asList(serverTimeZone.getId().replace(",","").split(" ")));
                			
                			zoneWords.addAll(Arrays.asList(icalTimZone.getID().replace("/", " ").split(" ")));
                			zoneWords.addAll(Arrays.asList(icalTimZone.getDisplayName().split(" ")));
                			
                			for(String sw : serverWords){
                				for(String zw: zoneWords){
                					if(sw.equals(zw)){
                						matchScore+=3;
                					}else if(sw.equalsIgnoreCase(zw)){
                						matchScore+=2;
                					}else if(sw.contains(zw) || zw.contains(sw)){
                						matchScore++;
                					}
                				}
                			}           
                			if(matchScore >= icalTimeZoneScore){
                				icalChosenTimeZone = icalTimZone;
                				icalTimeZoneScore = matchScore;
                			}
                			sb.append(new Integer(matchScore).toString()+" [MSName: "+ serverTimeZone.getName() + "|MSID: " + serverTimeZone.getId() +""+"|iID: "+icalTimZone.getID()+"|iName: "+icalTimZone.getDisplayName()+"|iName: "+icalTimZone.getDisplayName()+"]\n");
                		}
                	
                	}
            		if(icalChosenTimeZone==null){
            			//sb.append("TIME ZONE NOT FOUND "+tzName);
            		}else{
                		//sb.append("***Best Time Zone: "+ icalChosenTimeZone.getID()+"\n");	
                		mMap.put(serverTimeZone.getName(), icalChosenTimeZone.getID());
                		mMap.put(serverTimeZone.getId(), icalChosenTimeZone.getID());
            		}
            		
					sb.append("\n\n");
								
			}
		}
		Map<String, String> treeMap = new TreeMap<String, String>(mMap);
	    Iterator<Entry<String, String>> it = treeMap.entrySet().iterator();
	    while (it.hasNext()) {
	        Map.Entry pairs = (Map.Entry)it.next();
	        //<entry key="Key 1" value="1" />
	        sb.append("<entry key=\""+toXmlString(pairs.getKey().toString()) + "\" value=\"" + toXmlString(pairs.getValue().toString())+"\" />\n");
	        it.remove(); // avoids a ConcurrentModificationException
	    }
	    log.debug(sb.toString());	
	}
	
	@Test
	public void loadTimeZoneMappings(){
		assertNotNull(timeZoneMap);	
		assertEquals(timeZoneMap, new ExchangeEventConverterOLD().timeZoneMap);
		
		StringBuilder sb = new StringBuilder("");;
	    Iterator it = timeZoneMap.entrySet().iterator();
	    while (it.hasNext()) {
	        Map.Entry pairs = (Map.Entry)it.next();
	        //<entry key="Key 1" value="1" />
	        sb.append(pairs.getKey() + " = " + pairs.getValue()+"\n");
	        it.remove(); // avoids a ConcurrentModificationException
	    }
	    log.debug(sb.toString());	
	}
	
	public String toXmlString(String s){
		s = s.replace("&","&amp;");
		s = s.replace("\"","&quot");
		s = s.replace("'", "&apos;");
		s = s.replace("<","&lt;");
		s = s.replace(">","&gt;");
		
		return s;
	}
	
}
