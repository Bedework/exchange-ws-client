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
public class PathToExtendedFieldTypePropertyType extends XParameter {

	/**
	 * 
	 */
	private static final long serialVersionUID = -3389706638539898240L;
	private static final String PATH_TO_EXTENDED_FIELD_TYPE_PROPERTY_TYPE = "X-EWS-PATH-TO-EXTENDED-FIELD-TYPE-PROPERTY-TYPE";

	public PathToExtendedFieldTypePropertyType(PathToExtendedFieldType path) {
		super(PATH_TO_EXTENDED_FIELD_TYPE_PROPERTY_TYPE, path.getPropertyType().value());
	}

}
