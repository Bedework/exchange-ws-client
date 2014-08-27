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
public class PathToExtendedFieldTypePropertyTag extends XParameter {

	/**
	 * 
	 */
	private static final long serialVersionUID = -643813978660622928L;
	private static final String PATH_TO_EXTENDED_FIELD_TYPE_PROPERTY_TAG = "X-EWS-PATH-TO-EXTENDED-FIELD-TYPE-PROPERTY-TAG";

	public PathToExtendedFieldTypePropertyTag(PathToExtendedFieldType path) {
		super(PATH_TO_EXTENDED_FIELD_TYPE_PROPERTY_TAG, path.getPropertyTag());
	}

}
