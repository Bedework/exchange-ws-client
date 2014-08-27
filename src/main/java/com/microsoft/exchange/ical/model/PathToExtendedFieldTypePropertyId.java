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
public class PathToExtendedFieldTypePropertyId extends XParameter {

	/**
	 * 
	 */
	private static final long serialVersionUID = -8857164759801589763L;
	private static final String PATH_TO_EXTENDED_FIELD_TYPE_PROPERTY_ID = "X-EWS-PATH-TO-EXTENDED-FIELD-TYPE-PROPERTY-ID";

	public PathToExtendedFieldTypePropertyId(PathToExtendedFieldType path) {
		super(PATH_TO_EXTENDED_FIELD_TYPE_PROPERTY_ID,path.getPropertyId().toString());
	}

}
