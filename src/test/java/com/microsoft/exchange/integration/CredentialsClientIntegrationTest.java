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

package com.microsoft.exchange.integration;

import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.microsoft.exchange.DateHelp;
import com.microsoft.exchange.impl.http.ThreadLocalCredentialsProviderFactory;
import com.microsoft.exchange.messages.GetUserAvailabilityRequest;
import com.microsoft.exchange.messages.GetUserAvailabilityResponse;

/**
 * Integration test for evaluating the exchangeContext-usingCredentials.xml configuration.
 * 
 * @author Nicholas Blair
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations="classpath:/com/microsoft/exchange/exchangeContext-usingCredentials.xml")
public class CredentialsClientIntegrationTest extends AbstractIntegrationTest {
	
	@Value("${username}")
	private String userName;
	@Value("${password}")
	private String password;
	
	@Value("${integration.email}")
	private String emailAddress;
	
	private String startDate = "2012-10-11";
	private String endDate = "2012-10-12";
	private int expectedEventCount = 1;
	
	/* (non-Javadoc)
	 * @see com.microsoft.exchange.integration.AbstractIntegrationTest#initializeCredentials()
	 */
	@Override
	public void initializeCredentials() {
		Credentials credentials = new UsernamePasswordCredentials(userName, password);
		ThreadLocalCredentialsProviderFactory.set(credentials);
	}

	
	
	/**
	 * Issues a {@link GetUserAvailabilityRequest} for the configured emailAddress, startDate and endDate.
	 * Verifies a response, and that the freebusy responses match expectedEventCount.
	 */
	@Test
	public void testGetUserAvailability() {
		initializeCredentials();
		GetUserAvailabilityRequest request = constructAvailabilityRequest(DateHelp.makeDate(startDate), DateHelp.makeDate(endDate), emailAddress);
		GetUserAvailabilityResponse response = ewsClient.getUserAvailability(request);
	
		Assert.assertNotNull(response);
		Assert.assertEquals(expectedEventCount, response.getFreeBusyResponseArray().getFreeBusyResponses().size());
	}
	
	
	@Test @Override
	public void FindFolders() {
		super.findFolders();
	}
	
}
