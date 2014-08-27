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

import java.util.List;

import com.microsoft.exchange.exception.AutodiscoverException;

/**
 * All you should need for implementing an autodiscover client.
 *
 * @see <a href="http://msdn.microsoft.com/EN-US/library/office/ee332364(v=exchg.140).aspx">Implementing an Autodiscover Client in Microsoft Exchange</a>
 * 
 * @author ctcudd
 *
 */
public interface ExchangeAutodiscoverService {

	/**
	 * 
	 * Query all potential autodiscover endpoints ({@link com.microsoft.exchange.autodiscover.ExchangeAutodiscoverService.getPotentialAutodiscoverEndpoints(String)} 
	 * and return the EWS URL property from the first valid response.
	 * 
	 * 
	 * 
	 * @param email
	 * @return
	 * @throws AutodiscoverException if no endpoint can be found
	 */
	public String getAutodiscoverEndpoint(String email) throws AutodiscoverException;
	
    /**
     * Return an {@link List} of strings representing autodiscover endpoints, ordered by preference.
     *  returned endoints are based on the domain of the email address and the service specific autodiscover Suffix
     * 
     * @param
     * @return a never null List containing at least 1 URI
     */
	List<String> getPotentialAutodiscoverEndpoints(String email);

}