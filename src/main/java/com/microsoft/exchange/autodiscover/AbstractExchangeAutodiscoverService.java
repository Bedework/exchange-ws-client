package com.microsoft.exchange.autodiscover;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.validator.routines.EmailValidator;
import org.apache.http.entity.ContentType;

import com.microsoft.exchange.exception.AutodiscoverException;

public abstract class AbstractExchangeAutodiscoverService implements ExchangeAutodiscoverService {

	protected Log log = LogFactory.getLog(this.getClass());

	
	/**
	 * Don't use this.  all you should need is an email address to discover an EWS ENDPOINT
	 */
	@Deprecated
	protected static final String AUTODISCOVER_URL = "https://autodiscover-s.outlook.com/autodiscover/autodiscover.xml";
	
	private static final String TEXT_XML = "text/xml";
	private static final String UTF_8 =  "UTF-8";
	
	private static final ContentType CONTENT_TYPE = ContentType.create(TEXT_XML, UTF_8);
	
	private static final List<String> SCHEMES;
	private static final List<String> AUTODISCOVER_ENDPOINT_PATTERNS;
	static{
		List<String> autoDiscoverEndpoints = new ArrayList<String>();
		autoDiscoverEndpoints.add("{scheme}://{domain}/autodiscover/autodiscover.{serviceSuffix}");
		autoDiscoverEndpoints.add("{scheme}://autodiscover.{domain}/autodiscover/autodiscover.{serviceSuffix}");
		//fallback pattern
		//autoDiscoverEndpoints.add("https://autodiscover-s.outlook.com/autodiscover/autodiscover.{serviceSuffix}");
		AUTODISCOVER_ENDPOINT_PATTERNS = Collections.unmodifiableList(autoDiscoverEndpoints);
		
		//prefer https, see: http://msdn.microsoft.com/en-us/library/office/jj900169(v=exchg.150).aspx
		List<String> schemes = new ArrayList<String>();
		schemes.add("https");
		schemes.add("http");
		SCHEMES = Collections.unmodifiableList(schemes);
	}
	
	protected ContentType getContentType(){
		return CONTENT_TYPE;
	}
	
	/**
	 * 
	 * @return the url suffix for the autodiscover service.
	 * 
	 * POX = .xml
	 * SOAP = .svc
	 */
	abstract protected String getServiceSuffix();
	
	@Override
	public List<String> getPotentialAutodiscoverEndpoints(String email) {
		String domain = null;
		List<String> potentialEndpoints = new ArrayList<String>();
		try{
			domain = extractDomainFromEmail(email);
		}catch(AutodiscoverException e){
			log.error("Failed to generate potential autodiscover urls for email = "+email, e);
			return potentialEndpoints;
		}
		for(String scheme: SCHEMES){
			for(String pattern : AUTODISCOVER_ENDPOINT_PATTERNS){
				String uri = pattern.replace("{scheme}", scheme);
				uri = uri.replace("{domain}", domain);
				uri = uri.replace("{serviceSuffix}", getServiceSuffix());
				if(!potentialEndpoints.contains(uri)){
					potentialEndpoints.add(uri);
				}
			}
		}
		return potentialEndpoints;
	}
	
	/*
	 * validate the given email address and extract the domain.  Throw an exception if not found.
	 */
	protected String extractDomainFromEmail(String email) throws AutodiscoverException{
		EmailValidator validator = EmailValidator.getInstance(false);
		if(StringUtils.isNotBlank(email) && validator.isValid(email)){
			String domain = StringUtils.substringAfter(email, "@");
			if(StringUtils.isNotBlank(domain)){
				return domain;
			}
		}
		throw new AutodiscoverException("INVALID EMAIL: "+email );
	}
}
