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