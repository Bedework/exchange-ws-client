/**
 * See the NOTICE file distributed with this work
 * for additional information regarding copyright ownership.
 * Board of Regents of the University of Wisconsin System
 * licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a
 * copy of the License at:
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
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
