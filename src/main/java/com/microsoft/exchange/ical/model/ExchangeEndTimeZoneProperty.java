/**
 * 
 */
package com.microsoft.exchange.ical.model;

import net.fortuna.ical4j.model.property.XProperty;

/**
 * @author ctcudd
 *
 */
public class ExchangeEndTimeZoneProperty extends XProperty {

	/**
	 * 
	 */
	private static final long serialVersionUID = 5723883378729283373L;
	private static final String X_EWS_END_TIMEZONE ="X-EWS-END-TIMEZONE";
	
	public ExchangeEndTimeZoneProperty(String endTimeZoneId) {
		super(X_EWS_END_TIMEZONE, endTimeZoneId);
	}

}
