/**
 * 
 */
package com.microsoft.exchange.autodiscover;

import java.net.URI;
import java.net.URISyntaxException;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.ws.client.support.destination.DestinationProvider;

import com.microsoft.exchange.exception.AutodiscoverException;
import com.microsoft.exchange.impl.ImpersonationConnectingSIDSource;
import com.microsoft.exchange.types.ConnectingSIDType;

/**
 * @author ctcudd
 *
 */
public class AutodiscoverDestinationProvider implements DestinationProvider {

	protected final Log log = LogFactory.getLog(this.getClass());
	private ImpersonationConnectingSIDSource connectingSIDSource;
	private ExchangeAutodiscoverService	compositeAutodiscoverService;
	
	@Value("${endpoint:https://outlook.office365.com/ews/exchange.asmx}")
	private String defaultUri;
	
	public String getDefaultUri() {
		return defaultUri;
	}

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
	
	public ExchangeAutodiscoverService getCompositeAutodiscoverService() {
		return compositeAutodiscoverService;
	}
	
	@Autowired
	public void setCompositeAutodiscoverService(ExchangeAutodiscoverService compositeAutodiscoverService) {
		this.compositeAutodiscoverService = compositeAutodiscoverService;
	}
	
	/* (non-Javadoc)
	 * @see org.springframework.ws.client.support.destination.DestinationProvider#getDestination()
	 */
	@Override
	public URI getDestination() {
		
		URI autodiscoverURI = null;
		String autodiscoverEndpoint = getDefaultUri();
		ConnectingSIDType connectingSID = connectingSIDSource.getConnectingSID(null, null);

		if(null != connectingSID){
			
			String upn = connectingSID.getPrincipalName();
			try {
				autodiscoverEndpoint = compositeAutodiscoverService.getAutodiscoverEndpoint(upn);
			} catch (AutodiscoverException e) {
				log.warn("Failed to getAutodiscoverEndpoint for "+upn+": "+e.getMessage());
				
			}			
		}
		if(StringUtils.isNotBlank(autodiscoverEndpoint)){
			try {
				autodiscoverURI = new URI(autodiscoverEndpoint);
			} catch (URISyntaxException e) {
				log.error("Failed to getAutodiscoverEndpoint: "+e.getMessage());
			}
		}
		
		if(null == autodiscoverURI){
			log.error("AutodiscoverDestinationProvider: failed to find endpoint!");
		}
		return autodiscoverURI;
	}


}
