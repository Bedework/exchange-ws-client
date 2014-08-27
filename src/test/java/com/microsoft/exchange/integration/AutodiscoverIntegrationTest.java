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
import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.microsoft.exchange.autodiscover.ExchangeAutodiscoverService;
import com.microsoft.exchange.autodiscover.PoxAutodiscoverServiceImpl;
import com.microsoft.exchange.autodiscover.SoapAutodiscoverServiceImpl;
import com.microsoft.exchange.exception.AutodiscoverException;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations= {"classpath:test-contexts/exchangeContext.xml"})
public class AutodiscoverIntegrationTest {

	private Log log = LogFactory.getLog(this.getClass());
	
	@Autowired
	SoapAutodiscoverServiceImpl soapAutodiscoverService;
	
	@Autowired
	PoxAutodiscoverServiceImpl poxAutodiscoverService;
	
	@Autowired
	ExchangeAutodiscoverService compositeAutodiscoverService;
	
	@Autowired
	CacheManager ehCacheManager;
	
	@Value("${integration.email:someemailaddress@on.yourexchangeserver.edu}")
	String upn;
	
	@Test
	public void isAutowired() {
		assertNotNull(soapAutodiscoverService);
		assertNotNull(poxAutodiscoverService);
		assertNotNull(compositeAutodiscoverService);
		assertNotNull(ehCacheManager);
	}

	@Test
	public void getEndpointForEmail() throws AutodiscoverException {
		upn = "svc-caltools-iiat@uwtest.onmicrosoft.com";
		String endpointUri = compositeAutodiscoverService.getAutodiscoverEndpoint(upn);
		log.info(endpointUri);
	}
	
	@Test
	public void getEndpointForEmailTwice() throws AutodiscoverException {
		Cache cache = ehCacheManager.getCache("autodiscoverCache");
		cache.clearStatistics();
		cache.setStatisticsEnabled(true);
		Cache autodiscoverCache = ehCacheManager.getCache("autodiscoverCache");
		
		String endpointUri = compositeAutodiscoverService.getAutodiscoverEndpoint(upn);
		log.info(endpointUri);
		
		assertEquals(1, autodiscoverCache.getStatistics().getMemoryStoreObjectCount());
		assertEquals(0, autodiscoverCache.getStatistics().getCacheHits());
		assertEquals(1, autodiscoverCache.getStatistics().getCacheMisses());
		
		
		endpointUri = compositeAutodiscoverService.getAutodiscoverEndpoint(upn);
		log.info(endpointUri);
		
		assertEquals(1, autodiscoverCache.getStatistics().getMemoryStoreObjectCount());
		assertEquals(1, autodiscoverCache.getStatistics().getCacheHits());
		assertEquals(1, autodiscoverCache.getStatistics().getCacheMisses());
	}
	
	/**
	 * This test currently fails as there is no soap autodiscover endpoint for wisc.edu
	 * @throws AutodiscoverException
	 */
	@Test
	public void getSoapEndpointForEmail() throws AutodiscoverException {
		String endpointUri = soapAutodiscoverService.getAutodiscoverEndpoint(upn);
		log.info(endpointUri);
	}
	
	@Test
	public void getPoxEndpointForEmail() throws AutodiscoverException {
		String endpointUri = poxAutodiscoverService.getAutodiscoverEndpoint(upn);
		log.info(endpointUri);
	}

}
