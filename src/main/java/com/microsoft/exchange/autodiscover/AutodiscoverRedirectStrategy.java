package com.microsoft.exchange.autodiscover;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.ProtocolException;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.LaxRedirectStrategy;
import org.apache.http.protocol.HttpContext;

import com.microsoft.exchange.exception.ExchangeWebServicesRuntimeException;
	/**
	 * 
	 * BLATANTLY STOLEN FROM https://github.com/Jasig/email-preview
	 * 
	 * Redirect strategy for http client that allows following redirects (HTTP 301, 302, 307) for POST messages after
	 * validating the redirect location is "safe".
	 *
	 * This class does not limit redirect hops; DefaultHttpClient limits redirect hops to 100 (see
	 * ClientPNames.MAX_REDIRECTS ='http.protocol.max-redirects' in
	 * http://hc.apache.org/httpcomponents-client-ga/tutorial/pdf/httpclient-tutorial.pdf.
	 *
	 * @author James Wennmacher, jwennmacher@unicon.net
	 */

	public class AutodiscoverRedirectStrategy extends LaxRedirectStrategy {

		protected final Log log = LogFactory.getLog(this.getClass());

	    private List<String> unsafeUriExclusionPatterns;
	    private List<Pattern> unsafeUriPatterns = new ArrayList<Pattern>();
	    private List<String> requiredUriPatterns;
	    private List<Pattern> uriRequirementPatterns = new ArrayList<Pattern>();

	    public AutodiscoverRedirectStrategy() {
	        setRequiredUriPatterns(Arrays.asList(new String[]{
	                "^https:.*"
	        }));
	    }

	    public void setUnsafeUriExclusionPatterns(List<String> unsafeUriExclusionPatterns) {
	        this.unsafeUriExclusionPatterns = unsafeUriExclusionPatterns;
	        unsafeUriPatterns = new ArrayList<Pattern>();
	        for (String pattern : unsafeUriExclusionPatterns) {
	            unsafeUriPatterns.add(Pattern.compile(pattern));
	        }
	    }

	    public void setRequiredUriPatterns(List<String> requiredUriPatterns) {
	        this.requiredUriPatterns = requiredUriPatterns;
	        uriRequirementPatterns = new ArrayList<Pattern>();
	        for (String pattern : requiredUriPatterns) {
	            uriRequirementPatterns.add(Pattern.compile(pattern));
	        }
	    }

	    private boolean matchesPatternSet(URI uri, List<Pattern> patterns) {
	        for (Pattern pattern : patterns) {
	            Matcher matcher = pattern.matcher(uri.toString());
	            if (matcher.matches()) {
	                return true;
	            }
	        }
	        return false;
	    }

	    /**
	     * Overrides behavior to follow redirects for POST messages, AND to have the redirect be a POST.  Behavior of
	     * <code>DefaultRedirectStrategy</code> is to use a GET for the redirect (though against spec this is the
	     * de-facto standard, see http://www.mail-archive.com/httpclient-users@hc.apache.org/msg06327.html and
	     * http://www.alanflavell.org.uk/www/post-redirect.html).
	     *
	     * For our application, we want to follow the redirect for a 302 as long as it is to a safe location and
	     * have the redirect be a POST.
	     *
	     * This code is modified from http-components' http-client 4.2.5.  Since we only use POST the code for the
	     * other HTTP methods has been removed to simplify this method.
	     *
	     * @param request Http request
	     * @param response Http response
	     * @param context Http context
	     * @return Request to issue to the redirected location
	     * @throws ProtocolException protocol exception
	     */
	    @Override
	    public HttpUriRequest getRedirect(
	            final HttpRequest request,
	            final HttpResponse response,
	            final HttpContext context) throws ProtocolException {
	        URI uri = getLocationURI(request, response, context);
	        log.info("Following redirect to "+ uri.toString());
	        String method = request.getRequestLine().getMethod();
	        int status = response.getStatusLine().getStatusCode();

	        // Insure location is safe
	        if (matchesPatternSet(uri, unsafeUriPatterns)) {
	            log.warn("Not following to URI {} - matches a configured unsafe URI pattern "+ uri.toString());
	            throw new ExchangeWebServicesRuntimeException("Autodiscover redirected to unsafe URI " + uri.toString());
	        }

	        if (!matchesPatternSet(uri, uriRequirementPatterns) && uriRequirementPatterns.size() > 0) {
	            log.warn("Not following to URI {} - URI does not match a required URI pattern "+ uri.toString());
	            throw new ExchangeWebServicesRuntimeException("Autodiscover redirected to URI not matching required pattern. URI="
	                    + uri.toString());
	        }

	        // Follow forwards for 301 and 302 in addition to 307, to validate the redirect location,
	        // and to use a POST method.
	        if (status == HttpStatus.SC_TEMPORARY_REDIRECT
	                || status == HttpStatus.SC_MOVED_PERMANENTLY
	                || status == HttpStatus.SC_MOVED_TEMPORARILY) {
	            if (method.equalsIgnoreCase(HttpPost.METHOD_NAME)) {
	                return copyEntity(new HttpPost(uri), request);
	            }
	        }

	        // Should not get here, but return sensible value just in case.  A GET will likely fail.
	        return new HttpGet(uri);
	    }

	    private HttpUriRequest copyEntity(
	            final HttpEntityEnclosingRequestBase redirect, final HttpRequest original) {
	        if (original instanceof HttpEntityEnclosingRequest) {
	            redirect.setEntity(((HttpEntityEnclosingRequest) original).getEntity());
	        }
	        return redirect;
	    }

	}

