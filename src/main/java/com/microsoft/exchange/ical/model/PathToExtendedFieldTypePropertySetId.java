/**
 * 
 */
package com.microsoft.exchange.ical.model;

import com.microsoft.exchange.types.PathToExtendedFieldType;

import net.fortuna.ical4j.model.parameter.XParameter;

/**
 * @author ctcudd
 *
 */
public class PathToExtendedFieldTypePropertySetId extends XParameter {

	/**
	 * 
	 */
	private static final long serialVersionUID = -4134877982723519823L;

	private static final String PATH_TO_EXTENDED_FIELD_TYPE_PROPERTY_SET_ID = "X-EWS-PATH-TO-EXTENDED-FIELD-TYPE-PROPERTY-SET-ID";
	public PathToExtendedFieldTypePropertySetId(PathToExtendedFieldType path) {
		super(PATH_TO_EXTENDED_FIELD_TYPE_PROPERTY_SET_ID, path.getPropertySetId());
	}

}
