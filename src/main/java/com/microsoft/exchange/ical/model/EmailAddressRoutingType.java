/**
 * 
 */
package com.microsoft.exchange.ical.model;

import com.microsoft.exchange.types.EmailAddressType;

import net.fortuna.ical4j.model.parameter.XParameter;

/**
 * XParamater intended to hold the value from {@link EmailAddressType}.getRoutingType()
 * 
 * @author ctcudd
 *
 */
public class EmailAddressRoutingType extends XParameter {

	/**
	 * 
	 */
	private static final long serialVersionUID = 6772316318466614629L;
	
	private static final String EMAIL_ADDRESS_ROUTING_TYPE = "X-EWS-EMAIL-ADDRESS-ROUTING-TYPE";
	
	public EmailAddressRoutingType(String routingType) {
		super(EMAIL_ADDRESS_ROUTING_TYPE, routingType);
	}

}
