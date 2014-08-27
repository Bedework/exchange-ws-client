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
package com.microsoft.exchange;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Test;

import com.microsoft.exchange.autodiscover.CompositeAutodiscoverServiceImpl;
import com.microsoft.exchange.autodiscover.ExchangeAutodiscoverService;
import com.microsoft.exchange.autodiscover.PoxAutodiscoverServiceImpl;
import com.microsoft.exchange.autodiscover.SoapAutodiscoverServiceImpl;

/**
 * @author ctcudd
 *
 */
public class ExchangeAutodiscoverServiceTest {

	private static final String TEST_ADDRESS = "someemailaddress@on.yourexchangeserver.edu";
	
	private Log log = LogFactory.getLog(this.getClass());
	
	@Test
	public void getPotentialSoapAutodiscoverEndpoints(){
		SoapAutodiscoverServiceImpl soapAutodiscoverService = new SoapAutodiscoverServiceImpl();
		List<String> potentials = soapAutodiscoverService.getPotentialAutodiscoverEndpoints(TEST_ADDRESS);
		assertNotNull(potentials);
		assertNotNull(potentials.get(0));
		assertTrue(potentials.get(0).startsWith("https://"));
		log.info("SoapAutodiscoverServiceImpl returned "+potentials.size() +" potential autodiscover endpoints for "+TEST_ADDRESS);
		for(String p: potentials){
			assertTrue(p.endsWith(soapAutodiscoverService.getServiceSuffix()));
			log.info(p);
		}
	}
	
	@Test
	public void getPotentialPoxAutodiscoverEndpoints(){
		PoxAutodiscoverServiceImpl poxAutodiscoverService = new PoxAutodiscoverServiceImpl();
		List<String> potentials = poxAutodiscoverService.getPotentialAutodiscoverEndpoints(TEST_ADDRESS);
		assertNotNull(potentials);
		assertNotNull(potentials.get(0));
		assertTrue(potentials.get(0).startsWith("https://"));
		log.info("PoxAutodiscoverServiceImpl returned "+potentials.size() +" potential autodiscover endpoints for "+TEST_ADDRESS);
		for(String p: potentials){
			assertTrue(p.endsWith(poxAutodiscoverService.getServiceSuffix()));
			log.info(p);
		}
		
	}
	
	@Test
	public void getPotentialAutodiscoverEndpoints(){
		PoxAutodiscoverServiceImpl poxAutodiscoverService = new PoxAutodiscoverServiceImpl();
		SoapAutodiscoverServiceImpl soapAutodiscoverService = new SoapAutodiscoverServiceImpl();

		Collection<ExchangeAutodiscoverService> autodiscoverCollection = new ArrayList<ExchangeAutodiscoverService>();
		autodiscoverCollection.add(poxAutodiscoverService);
		autodiscoverCollection.add(soapAutodiscoverService);
		
		CompositeAutodiscoverServiceImpl compositeAutodiscoverService = new CompositeAutodiscoverServiceImpl();
		compositeAutodiscoverService.setAutodiscoverServices(autodiscoverCollection);
		

		List<String> potentials = compositeAutodiscoverService.getPotentialAutodiscoverEndpoints(TEST_ADDRESS);
		log.info("CompositeAutodiscoverServiceImpl returned "+potentials.size() +" potential autodiscover endpoints for "+TEST_ADDRESS);

		assertNotNull(potentials);
		assertNotNull(potentials.get(0));
		assertTrue(potentials.get(0).startsWith("https://"));
		for(String p: potentials){
			log.info(p);
		}
	}
}
