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
package com.microsoft.exchange.impl;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.xml.datatype.XMLGregorianCalendar;

import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.Date;
import net.fortuna.ical4j.model.DateTime;
import net.fortuna.ical4j.model.Dur;
import net.fortuna.ical4j.model.Parameter;
import net.fortuna.ical4j.model.ParameterList;
import net.fortuna.ical4j.model.TextList;
import net.fortuna.ical4j.model.component.VEvent;
import net.fortuna.ical4j.model.component.VTimeZone;
import net.fortuna.ical4j.model.component.VToDo;
import net.fortuna.ical4j.model.parameter.Cn;
import net.fortuna.ical4j.model.parameter.CuType;
import net.fortuna.ical4j.model.parameter.PartStat;
import net.fortuna.ical4j.model.parameter.Role;
import net.fortuna.ical4j.model.property.Attendee;
import net.fortuna.ical4j.model.property.Categories;
import net.fortuna.ical4j.model.property.Clazz;
import net.fortuna.ical4j.model.property.DtEnd;
import net.fortuna.ical4j.model.property.DtStamp;
import net.fortuna.ical4j.model.property.DtStart;
import net.fortuna.ical4j.model.property.Duration;
import net.fortuna.ical4j.model.property.Location;
import net.fortuna.ical4j.model.property.Organizer;
import net.fortuna.ical4j.model.property.Priority;
import net.fortuna.ical4j.model.property.Status;
import net.fortuna.ical4j.model.property.Summary;
import net.fortuna.ical4j.model.property.Transp;
import net.fortuna.ical4j.model.property.Uid;
import net.fortuna.ical4j.model.property.XProperty;

import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.util.CollectionUtils;

import com.microsoft.exchange.ExchangeEventConverter;
import com.microsoft.exchange.exception.ExchangeEventConverterException;
import com.microsoft.exchange.ical.model.EmailAddressMailboxType;
import com.microsoft.exchange.ical.model.EmailAddressRoutingType;
import com.microsoft.exchange.ical.model.ExchangeEndTimeZoneProperty;
import com.microsoft.exchange.ical.model.ExchangeStartTimeZoneProperty;
import com.microsoft.exchange.ical.model.ExchangeTimeZoneProperty;
import com.microsoft.exchange.ical.model.ItemTypeChangeKey;
import com.microsoft.exchange.ical.model.ItemTypeItemId;
import com.microsoft.exchange.ical.model.ItemTypeParentFolderChangeKey;
import com.microsoft.exchange.ical.model.ItemTypeParentFolderId;
import com.microsoft.exchange.ical.model.PathToExtendedFieldTypePropertyId;
import com.microsoft.exchange.ical.model.PathToExtendedFieldTypePropertySetId;
import com.microsoft.exchange.ical.model.PathToExtendedFieldTypePropertyTag;
import com.microsoft.exchange.ical.model.PathToExtendedFieldTypePropertyType;
import com.microsoft.exchange.types.ArrayOfStringsType;
import com.microsoft.exchange.types.AttendeeType;
import com.microsoft.exchange.types.CalendarItemType;
import com.microsoft.exchange.types.CalendarItemTypeType;
import com.microsoft.exchange.types.DistinguishedPropertySetType;
import com.microsoft.exchange.types.EmailAddressType;
import com.microsoft.exchange.types.ExtendedPropertyType;
import com.microsoft.exchange.types.FolderIdType;
import com.microsoft.exchange.types.ImportanceChoicesType;
import com.microsoft.exchange.types.ItemIdType;
import com.microsoft.exchange.types.ItemType;
import com.microsoft.exchange.types.LegacyFreeBusyType;
import com.microsoft.exchange.types.MailboxTypeType;
import com.microsoft.exchange.types.MapiPropertyTypeType;
import com.microsoft.exchange.types.NonEmptyArrayOfAttendeesType;
import com.microsoft.exchange.types.NonEmptyArrayOfPropertyValuesType;
import com.microsoft.exchange.types.PathToExtendedFieldType;
import com.microsoft.exchange.types.ResponseTypeType;
import com.microsoft.exchange.types.SensitivityChoicesType;
import com.microsoft.exchange.types.SingleRecipientType;
import com.microsoft.exchange.types.TaskType;
import com.microsoft.exchange.types.TimeZoneDefinitionType;

public class ExchangeEventConverterImpl implements ExchangeEventConverter {

	protected Log log = LogFactory.getLog(this.getClass());

	
	@Override
	public Calendar convertToCalendar(Collection<ItemType> items, String upn) {
		Calendar result = new Calendar();
		
		result.getProperties().add(PROD_ID);
		result.getProperties().add(VERSION);
		
		int size = CollectionUtils.isEmpty(items) ? 0 : items.size();
		log.debug("attempting to convert "+size+" items");
		if(!CollectionUtils.isEmpty(items)){

			for(ItemType item: items){
				if(item instanceof CalendarItemType) {
					CalendarItemType calendarItem = (CalendarItemType) item;
					Pair<VEvent, ArrayList<VTimeZone>> pair = null;
					try {
						pair = convertCalendarItemType(calendarItem, upn);
					} catch (ExchangeEventConverterException e) {
						log.error("Failed to convert calendarItem:" + e.getMessage());
					}
					
					if(null != pair){
						if(null != pair.getLeft()){
							result.getComponents().add(pair.getLeft());
						}else{
							log.warn("Failed to generate VEvent for CalendarItemType="+calendarItem);
						}
						if(!CollectionUtils.isEmpty(pair.getRight())){
							log.debug("Generated "+pair.getRight().size()+" VTimeZone components for CalendarItemType="+calendarItem);
							for(VTimeZone timeZone : pair.getRight()){
								result.getComponents().add(timeZone);
							}
						}else{
							log.warn("Failed to generate VTimeZone for CalendarItemType="+calendarItem);
						}
					}
					
				}else if(item instanceof TaskType){
					TaskType taskItem = (TaskType) item;
					Pair<VToDo,ArrayList<VTimeZone>> pair = convertTaskType(taskItem, upn);
					//TODO handle tasks
				}else{
					log.warn("Not an instanceof CalendarItemType | TaskType.  Cannot convert item: "+item);
				}
			}
		}
		
		return result;
	}


	protected Pair<VToDo, ArrayList<VTimeZone>> convertTaskType(TaskType taskItem, String upn) {
		VToDo task = new VToDo();
		ArrayList<VTimeZone> timeZones = new ArrayList<VTimeZone>();
		
		
		Pair<VToDo, ArrayList<VTimeZone>> pair = Pair.of(task, timeZones);
		return pair;
	}


	/**
	 * 
	 * TimeZones.
	 * 
	 * @param calendarItem
	 * @param upn
	 * @return
	 * @throws ExchangeEventConverterException 
	 */
	protected Pair<VEvent, ArrayList<VTimeZone>> convertCalendarItemType(CalendarItemType calendarItem, String upn) throws ExchangeEventConverterException{
		VEvent event = new VEvent();
		ArrayList<VTimeZone> timeZones = new ArrayList<VTimeZone>();
		
		if(calendarItem.getStart() == null){
			throw new ExchangeEventConverterException("calendarItem must have a valid start time.");
		}
		
		if(calendarItem.getEnd() == null && calendarItem.getDuration() == null){
			throw new ExchangeEventConverterException("calendarItem must have a valid end time or duration.");
		}
		
		
		//does this element have a timezone?
		XMLGregorianCalendar start = calendarItem.getStart();
		DtStart dtStart = new DtStart(new DateTime(start.toGregorianCalendar().getTime()));
		DtEnd dtEnd = null;
		
		if(null != calendarItem.getEnd()){
			dtEnd = new DtEnd(new DateTime(calendarItem.getEnd().toGregorianCalendar().getTime()));
		}
		
		//if all day event, must use Date
		if(null !=  calendarItem.isIsAllDayEvent()  && calendarItem.isIsAllDayEvent()) {
			dtStart = new DtStart(new Date(start.toGregorianCalendar().getTime()),true);
			dtEnd = new DtEnd(new Date(calendarItem.getEnd().toGregorianCalendar().getTime()),true);
			log.debug("set to all day event");
		}
		//this way no vtimezone is needed
		dtStart.setUtc(true);
		
		event.getProperties().add(dtStart);
		log.debug("added dtStart="+dtStart);
		
		if( null != dtEnd ){
			dtEnd.setUtc(true);
			event.getProperties().add(dtEnd);
			log.debug("added dtEnd="+dtEnd);
		}
				
		//in case dtEnd is not present but duration is.
		String duration = calendarItem.getDuration();
		if(StringUtils.isNotBlank(duration)  && event.getProperty(DtEnd.DTEND)==null){
			Dur dur = new Dur(duration);
			Duration durationProperty = new Duration(dur);
			event.getProperties().add(durationProperty);
			event.getProperties().remove(DtEnd.DTEND);
			log.debug("dtend overridden with duration="+durationProperty);
		}

		String uid = calendarItem.getUID();
		if(StringUtils.isNotBlank(uid)){
			Uid uidProperty = new Uid(uid);
			event.getProperties().add(uidProperty);
			log.debug("added Uid="+uidProperty);
		}else{
			log.debug("could not generate Uid property.");
		}
		
		//should always set dtstamp, otherwise it's auto-generated and !veventCreatedNow.equals(veventCreatedLater);
		if(null != calendarItem.getDateTimeCreated()){
			DtStamp dtstamp = new DtStamp(new DateTime(calendarItem.getDateTimeCreated().toGregorianCalendar().getTime())); 
			dtstamp.setUtc(true);
			
			event.getProperties().remove(event.getProperty(DtStamp.DTSTAMP));
			event.getProperties().add(dtstamp);
			log.debug("overide DtStamp="+dtstamp);
		}else{
			log.debug("could not generate DtStamp, property will be autogenerated.");
		}
		
		String subject = calendarItem.getSubject();
		if(StringUtils.isNotBlank(subject)) {
			Summary summaryProperty = new Summary(subject);
			event.getProperties().add(summaryProperty);
			log.debug("add summary="+summaryProperty);
		}else{
			log.debug("could not generate Summary property");
		}
		
		String location = calendarItem.getLocation();
		if(StringUtils.isNotBlank(location)){
			event.getProperties().add( new Location( location ) );
		}else{
			log.debug("could not generate location property");
		}
		
		LegacyFreeBusyType freeBusy = calendarItem.getLegacyFreeBusyStatus();
		Transp transpProperty = Transp.OPAQUE;
		if(LegacyFreeBusyType.FREE.equals(freeBusy)) {
			transpProperty = Transp.TRANSPARENT;	
		}
		event.getProperties().add(transpProperty);
		log.debug("added Transp="+transpProperty);
		
		Status status = Status.VEVENT_CONFIRMED;
		if(BooleanUtils.isTrue(calendarItem.isIsCancelled())){
			status = Status.VEVENT_CANCELLED;
		}
		event.getProperties().add(status);
		log.debug("added Status="+status);
		
		boolean organizerIsSet = false;
		SingleRecipientType calendarItemOrganizer = calendarItem.getOrganizer();
		if(null != calendarItemOrganizer ) {
			Organizer organizer = convertToOrganizer(calendarItemOrganizer);
			if(null != organizer){
				event.getProperties().add(organizer);
				organizerIsSet = true;
				log.debug("added Organizer="+organizer);
			}else{
				log.debug("could not gernate Organizer. As a result, attendees will not be added.");
			}
		}else{
			log.debug("could not gernate Organizer. As a result, attendees will not be added.");
		}
		
		//only add RequiredAttendees, OptionalAttendees and Resources if and only if organizer present.
		if(organizerIsSet){
			
			ResponseTypeType myResponseType = calendarItem.getMyResponseType();
			
			//add RequiredAttendees
			NonEmptyArrayOfAttendeesType requiredAttendees = calendarItem.getRequiredAttendees();
			if(null != requiredAttendees){
				Set<Attendee> attendees = convertRequiredAttendees(requiredAttendees, myResponseType);
				for(Attendee attendee : attendees){
					event.getProperties().add(attendee);
				}
			}else{
				log.debug("no required attendees.");
			}
			
			//add OptionalAttendees
			NonEmptyArrayOfAttendeesType optionalAttendees = calendarItem.getOptionalAttendees();
			if(null != optionalAttendees){
				Set<Attendee> attendees = convertOptionalAttendees(optionalAttendees, myResponseType);
				for(Attendee attendee : attendees){
					event.getProperties().add(attendee);
				}
			}else{
				log.debug("no optional attendees");
			}
			
			//add Resources
			NonEmptyArrayOfAttendeesType resourceAttendees = calendarItem.getResources();
			if(null != resourceAttendees){
				Set<Attendee> attendees = convertResourceAttendees(resourceAttendees, myResponseType);
				for(Attendee attendee : attendees){
					event.getProperties().add(attendee);
				}
			}
		}
		
		CalendarItemTypeType calendarItemType = calendarItem.getCalendarItemType();
		if(null != calendarItemType){
			if(CalendarItemTypeType.EXCEPTION.equals(calendarItemType) || CalendarItemTypeType.RECURRING_MASTER.equals(calendarItemType)){
				log.warn("Recurring Event Detected!  This implementation of ExchangeEventConverter does not expand recurrance.  You should use a CalendarView to expand recurrence on the Exchagne server. --http://msdn.microsoft.com/en-us/library/office/aa564515(v=exchg.150).aspx");
			}
		}
		
		
		//generate xproperties for standard item properties
		Collection<XProperty> itemXProperties = generateItemTypeXProperties(calendarItem);
		for(XProperty xp: itemXProperties){
			event.getProperties().add(xp);
		}
		
		//generate XProperty's for ExtendedProperties...
		List<ExtendedPropertyType> extendedProperties = calendarItem.getExtendedProperties();
		if(!CollectionUtils.isEmpty(extendedProperties)){
			for(ExtendedPropertyType extendedProperty : extendedProperties){
				Collection<XProperty> xProperties = convertExtendedPropertyType(extendedProperty);
				for(XProperty xp: xProperties){
					event.getProperties().add(xp);
				}
			}
		}
		
		Pair<VEvent, ArrayList<VTimeZone>> pair = Pair.of(event, timeZones);
		return pair;
	}
	
	private Collection<XProperty> generateCalendarItemTypeXProperties(CalendarItemType calendarItem){
		Collection<XProperty> xprops = new LinkedHashSet<XProperty>();
		
		String timeZone = calendarItem.getTimeZone();
		if(StringUtils.isNotBlank(timeZone)){
			xprops.add(new ExchangeTimeZoneProperty(timeZone));
		}else{
			log.warn("unable to generate ExchangeTimeZoneProperty, timeZone is blank");
		}
		TimeZoneDefinitionType startTimeZone = calendarItem.getStartTimeZone();
		if(null != startTimeZone && StringUtils.isNotBlank(startTimeZone.getId())){
			xprops.add(new ExchangeStartTimeZoneProperty(startTimeZone.getId()));
		}else{
			log.debug("unable to generate ExchangeStartTimeZoneProperty, startTimeZone is blank");
		}
		TimeZoneDefinitionType endTimeZone = calendarItem.getEndTimeZone();
		if(null != endTimeZone && StringUtils.isNotBlank(endTimeZone.getId())){
			xprops.add(new ExchangeEndTimeZoneProperty(endTimeZone.getId()));
		}else{
			log.debug("unable to generate ExchangeEndTimeZoneProperty, endTimeZone is blank");
		}
		
		return xprops;
	}
	
	/**
	 * Return a never null but possibly empty {@link Collection} of {@link XProperty}
	 * 
	 * Returned {@link XProperty}s may include:
	 * {@link ItemTypeParentFolderId}, 
	 * 
	 * @param item
	 * @return
	 */
	private Collection<XProperty> generateItemTypeXProperties(ItemType item){
		Collection<XProperty> xprops = new LinkedHashSet<XProperty>();
		
		FolderIdType parentFolderId = item.getParentFolderId();
		if(null != parentFolderId){
			String p_id = parentFolderId.getId();
			String p_ck = parentFolderId.getChangeKey();
			if(StringUtils.isNotBlank(p_id)){
				xprops.add(new ItemTypeParentFolderId(parentFolderId));
			}else{
				log.warn("unable to generate X_EWS_PARENT_FOLDER_ID, parentFolderId is blank");
			}
			if(StringUtils.isNotBlank(p_ck)){
				xprops.add(new ItemTypeParentFolderChangeKey(parentFolderId));
			}else{
				log.warn("unable to generate X_EWS_PARENT_FOLDER_CHANGEKEY, parentFolderChangeKey is blank");
			}
		}
		
		ItemIdType itemId = item.getItemId();
		if(null != itemId){
			String i_id = itemId.getId();
			String i_ck = itemId.getChangeKey();
			if(StringUtils.isNotBlank(i_id)){
				xprops.add(new ItemTypeItemId(itemId));
			}else{
				log.warn("unable to generate X_EWS_ITEM_ID, itemId is blank");
			}
			if(StringUtils.isNotBlank(i_ck)){
				xprops.add(new ItemTypeChangeKey(itemId));
			}else{
				log.warn("unable to generate X_EWS_ITEM_CHANGEKEY, itemChangeKey is blank");
			}
		}
		if(item instanceof CalendarItemType){
			CalendarItemType calendarItem = (CalendarItemType) item;
			Collection<XProperty> calendarXProps = generateCalendarItemTypeXProperties(calendarItem);
			if(!CollectionUtils.isEmpty(calendarXProps)){
				xprops.addAll(calendarXProps);
			}
		}else {
			log.warn("item is not a CalendarItemType, X_EWS...TIMEZONE properties will not be generated.");
		}
		return xprops;
	}
	
	/**
	 * return a never null but possibly empty {@link Collection} of {@link XProperty}
	 * 
	 * if an {@link ExtendedPropertyType} contains multiple values this method will return multiple {@link XProperty}'s.
	 * 
	 * @param extendedProperty
	 * @return
	 */
	private Collection<XProperty> convertExtendedPropertyType(ExtendedPropertyType extendedProperty){
		
		Collection<XProperty> xprops = new LinkedHashSet<XProperty>();
		PathToExtendedFieldType extendedFieldURI = extendedProperty.getExtendedFieldURI();
		if(null != extendedFieldURI){
			String propertyName = extendedFieldURI.getPropertyName();
			if(StringUtils.isBlank(propertyName)){
				DistinguishedPropertySetType distinguishedPropertySetId = extendedFieldURI.getDistinguishedPropertySetId();
				if(null != distinguishedPropertySetId){
					propertyName = distinguishedPropertySetId.value();
				}
			}
			ParameterList params = new ParameterList();
			String exPropSetId = extendedFieldURI.getPropertySetId();
			if(StringUtils.isNotBlank(exPropSetId)){
				params.add(new PathToExtendedFieldTypePropertySetId(extendedFieldURI));
			}
			Integer exPropId = extendedFieldURI.getPropertyId();
			if(StringUtils.isNotBlank(exPropId.toString())){
				params.add(new PathToExtendedFieldTypePropertyId(extendedFieldURI));
			}
			MapiPropertyTypeType propertyType = extendedFieldURI.getPropertyType();
			if(null != propertyType && StringUtils.isNotBlank(propertyType.value())){
				params.add(new PathToExtendedFieldTypePropertyType(extendedFieldURI));
			}
			String propertyTag = extendedFieldURI.getPropertyTag();
			if(StringUtils.isNotBlank(propertyTag)){
				params.add(new PathToExtendedFieldTypePropertyTag(extendedFieldURI));
			}
			Set<String> xPropertyValues = new HashSet<String>();
			if(StringUtils.isNotBlank(propertyName)){
				NonEmptyArrayOfPropertyValuesType values = extendedProperty.getValues();
				if(null != values && !CollectionUtils.isEmpty(values.getValues())){
					xPropertyValues.addAll(values.getValues());
				}else if(null != extendedProperty.getValue()){
					xPropertyValues.add(extendedProperty.getValue());
				}
			}else{
				log.error("Unable to generate XProperty(s). propertyName not found for ExtendedPropertyType="+extendedProperty);
			}
			if(!CollectionUtils.isEmpty(xPropertyValues)){
				Integer count = 0;
				for(String xValue: xPropertyValues){
					xprops.add(new XProperty(propertyName, params, xValue));
					propertyName+="_"+count;
					count++;
				}
			}else{ 
				log.error("Unable to generate XProperty(s). propertyValue(s) not found for ExtendedPropertyType="+extendedProperty);
			}
		}
		return xprops;
	}
	
	
	/**
	 * Convert the {@link String} argument to a mailto {@link URI} if possible.
	 * 
	 * @param emailAddress
	 * @return the email as a URI
	 * @throws IllegalArgumentException
	 *             if conversion failed, or if the argument was empty
	 *
	 * <strong>WARNING</strong >A {@link CalendarItemType} may contain attendees that no longer have a valid email address.
	 * If an event contains an attendee that has been deleted, the email address field takes the value of <legacyDn> example: <t:EmailAddress>/O=EXCHANGELABS/OU=EXCHANGE ADMINISTRATIVE GROUP (FYDIBOHF23SPDLT)/CN=RECIPIENTS/CN=F450764bd9384fd3b7a38722504c8815-Documentati</t:EmailAddress>
	 */
	public URI emailToURI(final String emailAddress) {
		Validate.notEmpty(emailAddress, "emailAddress cannot be null");	 
		URI uri = null;
		try {
			uri = new URI("mailto:" + emailAddress);
		} catch (URISyntaxException e) {
			log.debug("caught URISyntaxException trying to construct mailto URI for "
					+ emailAddress + "\n" + e.getMessage());
		}
		return uri;
	}
	
	/**
	 * This method will return a never null a {@link Pair}<{@link ParameterList},{@link URI}> for a given {@link EmailAddressType}
	 * This method will return a null URI element if the  {@link EmailAddressType} does not contain a valid EmailAddress property
	 * This method will return a never null but possibly empty {@link ParameterList}.  
	 * 
	 * ParamaterList may contain the following {@link Parameter}s: 
	 * {@link Cn}, {@link EmailAddressRoutingType}, {@link EmailAddressMailboxType}
	 * 
	 * @param recipient
	 * @return
	 */
	protected Pair<ParameterList, URI> convertEmailAddressType(EmailAddressType emailAddressType, Role role){
		URI uri = null;
		ParameterList params = new ParameterList();
		if(null != emailAddressType){
			String emailAddress = emailAddressType.getEmailAddress();
			if(StringUtils.isNotBlank(emailAddress)){
				uri = emailToURI(emailAddress);
			}else{
				log.warn("convertEmailAddressType: could not generate URI.");
			}
			
			if(null != role){
				params.add(role);
			}
			
			String name = emailAddressType.getName();
			if(StringUtils.isNotBlank(name)){
				params.add(new Cn(name));
			}else{
				log.debug("convertEmailAddressType: could not generate Cn");
			}
			String routingType = emailAddressType.getRoutingType();
			if(StringUtils.isNotBlank(routingType)){
				params.add(new EmailAddressRoutingType(routingType));
			}else{
				log.debug("convertEmailAddressType: could not generate EmailAddressRoutingType");
			}
			MailboxTypeType mailboxType = emailAddressType.getMailboxType();
			if(null != mailboxType){
				params.add(new EmailAddressMailboxType(mailboxType));
				CuType cuType = convertMailboxTypeTypeToCuType(mailboxType,role);
				params.add(cuType);
			}else{
				log.debug("convertEmailAddressType: could not generate EmailAddressMailboxType");
			}
		}else{
			log.debug("convertEmailAddressType: EmailAddressType = null");
		}
		Pair<ParameterList,URI> pair = Pair.of(params, uri);
		return pair;
	}
	
	
	/**
	 * This method will attempt to generate a {@link Organizer} from a {@link SingleRecipientType} 
	 * 	 * 
	 * This method will add {@link PartStat.ACCEPTED} and {@link net.fortuna.ical4j.model.parameter.Role.CHAIR} when an organizer is found.
	 * {@link com.microsoft.exchange.impl.ExchangeEventConverterImpl.convertEmailAddressType(EmailAddressType)}  for a list of other paramaters that may be included in the response
	 * 
	 * This method will return null if the {@link SingleRecipientType} EmailAddress field  is missing or invalid.
	 * 
	 * @param calendarItemOrganizer
	 * @return
	 */
	public Organizer convertToOrganizer(SingleRecipientType calendarItemOrganizer){
		Organizer organizer = null;
		if(null != calendarItemOrganizer){
			Pair<ParameterList, URI> pair = convertEmailAddressType(calendarItemOrganizer.getMailbox(), Role.CHAIR);
			URI organizerURI = pair.getRight();
			ParameterList organizerParams = pair.getLeft();
			if(null != organizerURI){
				
				organizer = new Organizer(organizerParams, organizerURI);
			
				//organizer is always ACCEPTED
				organizer.getParameters().add(PartStat.ACCEPTED);
				
				
			}else{
				log.debug("convertToOrganizer: organizerURI = null, Organizer = null ");
			}
		}else{
			log.debug("convertToOrganizer: calendarItemOrganizer = null, Organizer = null ");
		}
		return organizer;
	}
	
	/**
	 * This method returns a never null but possibly empty {@link HashSet} of {@link Attendee}s.
	 * This method will attempt to generate a {@link Attendee} for each {@link AttendeeType} contained within @link {@link NonEmptyArrayOfAttendeesType}.
	 * An a {@link Attendee} will not be generated for any {@link AttendeeType} with a missing or invalid {@link EmailAddressType}
	 * 
	 * Attendee Responses are only present if you obtained CalendarItem from Exchange as organizer. @see <a href="http://office.microsoft.com/en-us/outlook-help/organize-meetings-with-outlook-RZ001166003.aspx?section=20">Attendees do not see responses</a>
	 * 
	 * {@link com.microsoft.exchange.impl.ExchangeEventConverterImpl.convertEmailAddressType(EmailAddressType)} for details on how recipient EmailAddressType properties are mapped to {@link Parameter}s.
	 * 
	 * @param attendees
	 * @param myResponseType - a {@link PartStat} parameter will be added to every {@link Attendee} if and only if myResponseType.eqals( {@link ResponseTypeType}.ORGANIZER and the corresponding {@link AttendeeType} contains a valid {@link ResponseTypeType} )
	 * @param requiredAttendees - Indicates which {@link Role} parameter to add to attendees. True indicates that the {@link NonEmptyArrayOfAttendeesType} represent Role.REQ_PARTICIPANT, false indicates the {@link NonEmptyArrayOfAttendeesType} are optional attendees
	 * @return
	 */
	protected Set<Attendee> convertAttendees(NonEmptyArrayOfAttendeesType attendees,ResponseTypeType myResponseType, Role role){
		
		Set<Attendee> attendeeSet = new HashSet<Attendee>();
		
		if(null != attendees && !CollectionUtils.isEmpty(attendees.getAttendees())){
			for(AttendeeType attendeeType : attendees.getAttendees()){
				if(null != attendeeType){
					EmailAddressType mailbox = attendeeType.getMailbox();
					Pair<ParameterList, URI> attendeePair = convertEmailAddressType(mailbox, role);
					
					URI attendeeURI = attendeePair.getRight();
					ParameterList attendeeParams = attendeePair.getLeft();
					
					if(null != attendeeURI){
						Attendee attendee =  new Attendee(attendeeParams, attendeeURI);
						
						if(null != myResponseType && myResponseType.equals(ResponseTypeType.ORGANIZER)){
							//responseType should be present
							ResponseTypeType responseType = attendeeType.getResponseType();
							if(null != responseType){
								//go ahead and add a partstat
								attendee.getParameters().add(convertResponseTypeTypeToPartStat(responseType));
							}
						}
						
						
						if(attendeeSet.add(attendee)){
							log.debug("added Attendee="+attendee);
						}
					}					
				}
			}
		}else{
			log.debug("no attendees");
		}
		
		return attendeeSet;
	}
	
	public Set<Attendee> convertRequiredAttendees(NonEmptyArrayOfAttendeesType attendees,ResponseTypeType myResponseType){
		return convertAttendees(attendees, myResponseType, Role.REQ_PARTICIPANT);
	}
	
	public Set<Attendee> convertOptionalAttendees(NonEmptyArrayOfAttendeesType attendees,ResponseTypeType myResponseType){
		return convertAttendees(attendees, myResponseType, Role.OPT_PARTICIPANT);
	}
	
	public Set<Attendee> convertResourceAttendees(NonEmptyArrayOfAttendeesType attendees,ResponseTypeType myResponseType){
		return convertAttendees(attendees, myResponseType, Role.NON_PARTICIPANT);
	}
	
	/**
	 * Returns a never null {@link PartStat} for a given {@link ResponseTypeType} 
	 * 
	 * ResponseTypeType.ORGANIZER	=> PartStat.ACCEPTED
	 * ResponseTypeType.ACCEPT 		=> PartStat.ACCEPTED
	 * ResponseTypeType.DECLINE		=> PartStat.DECLINED 
	 * ResponseTypeType.TENTATIVE	=> PartStat.TENTATIVE
	 * All other ResponseTypeTypes 	=> PartStat.NEEDS_ACTION
	 * 
	 * @param responseType
	 * @return
	 */
	public static PartStat convertResponseTypeTypeToPartStat(ResponseTypeType responseType) {
		if(null != responseType) {
			if(responseType.equals(ResponseTypeType.ACCEPT) || responseType.equals(ResponseTypeType.ORGANIZER)) {
				return PartStat.ACCEPTED;
			}else if (responseType.equals(ResponseTypeType.DECLINE)) {
				return PartStat.DECLINED;
			}else if (responseType.equals(ResponseTypeType.TENTATIVE)) {
				return PartStat.TENTATIVE;
			}									
		}
		return PartStat.NEEDS_ACTION; 
	}
	
	/**
	 *  Returns a never null {@link ResponseTypeType} for a given {@link PartStat}
	 *  
	 *  PartStat.ACCEPTED 		=> ResponseTypeType.ACCEPT
	 *  PartStat.DECLINED 		=> ResponseTypeType.DECLINE  
	 *  PartStat.TENTATIVE		=> ResponseTypeType.TENTATIVE
	 *  PartStat.NEEDS_ACTION	=> ResponseTypeType.NO_RESPONSE_RECEIVED
	 *  All other PartStats     => ResponseTypeType.UNKNOWN
	 *  
	 * @param partStat
	 * @return
	 */
	public static ResponseTypeType convertPartStatToResponseTypeType(PartStat partStat){
		if(null != partStat){
			if(partStat.equals(PartStat.ACCEPTED)){
				return ResponseTypeType.ACCEPT;
			}else if(partStat.equals(PartStat.DECLINED)){
				return ResponseTypeType.DECLINE;
			}else if(partStat.equals(PartStat.TENTATIVE)){
				return ResponseTypeType.TENTATIVE;
			}else if(partStat.equals(PartStat.NEEDS_ACTION)){
				return ResponseTypeType.NO_RESPONSE_RECEIVED;
			}
		}
		return ResponseTypeType.UNKNOWN;
	}
	
	/**
	 * Return a never null {@link Clazz} for a given {@link SensitivityChoicesType}
	 * 
	 * @see <a href="http://windowsitpro.com/outlook/outlook-using-sensitivity-levels-appointments">Using Sensitivity Levels with Appointments</a>
	 * 
	 * SensitivityChoicesType.CONFIDENTIAL 	=> Clazz.CONFIDENTIAL
	 * SensitivityChoicesType.NORMAL		=> Clazz.PUBLIC
	 * All other SensitivityChoicesType		=> Clazz.PRIVATE
	 * 
	 * @param sensitivity
	 * @return
	 */
	public static Clazz convertSensitivityToClazz(SensitivityChoicesType sensitivity){
		Clazz clazz = Clazz.PRIVATE;
		if(null != sensitivity){
			if(sensitivity.equals(SensitivityChoicesType.CONFIDENTIAL)){
				clazz = Clazz.CONFIDENTIAL;
			}else if(sensitivity.equals(SensitivityChoicesType.NORMAL)){
				clazz = Clazz.PUBLIC;
			}
		}
		return clazz;
	}
	
	/**
	 * Return a never null {@link SensitivityChoicesType} for a given {@link Clazz}
	 * 
	 * Clazz.CONFIDENTIAL 	=> SensitivityChoicesType.CONFIDENTIAL
	 * Clazz.PUBLIC 		=> SensitivityChoicesType.NORMAL
	 * All other Clazz		=> SensitivityChoicesType.PRIVATE
	 * 
	 * @param clazz
	 * @return
	 */
	public static SensitivityChoicesType convertClazzToSensitivityChoicesType(Clazz clazz){
		SensitivityChoicesType sensitivity = SensitivityChoicesType.PRIVATE;
		if(null != clazz){
			if(clazz.equals(Clazz.CONFIDENTIAL)){
				sensitivity = SensitivityChoicesType.CONFIDENTIAL;
			}else if(clazz.equals(Clazz.PUBLIC)){
				sensitivity = SensitivityChoicesType.NORMAL;
			}
		}
		return sensitivity;
	}
	/**
	 * Returns a never null {@link Priority} for a given {@link ImportanceChoicesType}
	 * 
	 * Defaults to Priority.MEDIUM;
	 * 
	 * @param importance
	 * @return
	 */
	public static Priority convertImportanceChoicesTypeToPriority(ImportanceChoicesType importance){
		Priority priority = Priority.MEDIUM;
		if(null != importance){
			if(importance.equals(ImportanceChoicesType.HIGH)){
				priority = Priority.HIGH;
			}else if(importance.equals(ImportanceChoicesType.LOW))
				priority = Priority.LOW;
		}
		return priority;
	}
	
	/** 
	 * Returns a never null {@link ImportanceChoicesType} for a given {@link Priority}
	 * 
	 * Defaults to ImportanceChoicesType.NORMAL
	 * 
	 * Priority (HIGH => 1, NORMAL => 5, LOW => 9) 
	 * 
	 * @param priority
	 * @return
	 */
	public static ImportanceChoicesType convertPriorityToImportanceChoicesType(Priority priority){
		ImportanceChoicesType importance = ImportanceChoicesType.NORMAL;
		if(null != priority ){
			if(priority.equals(Priority.HIGH)){
				importance = ImportanceChoicesType.HIGH;
			}else if(priority.equals(Priority.LOW)){
				importance = ImportanceChoicesType.LOW;
			}
		}
		return importance; 
	}
	
	/**
	 * Return a never null {@link CuType} for a given {@link MailboxTypeType}
	 * 
	 * Defaults to CuType.INDIVIDUAL
	 * 
	 * @see <a href="http://www.kanzaki.com/docs/ical/cutype.html">Calendar User Type</a>
	 * @see <a href="http://msdn.microsoft.com/en-us/library/office/aa563493(v=exchg.140).aspx">MailboxType</a>
	 *  
	 * @param mailboxType
	 * @return
	 */
	public static CuType convertMailboxTypeTypeToCuType(MailboxTypeType mailboxType, Role role){
		//If not specified on a property that allows this parameter, the default is INDIVIDUAL.
		CuType cuType = CuType.INDIVIDUAL;
		if(null != mailboxType){
			if(MailboxTypeType.PRIVATE_DL.equals(mailboxType) || MailboxTypeType.PUBLIC_DL.equals(mailboxType)){
				cuType = CuType.GROUP;
			}
			
			//TODO this is bad hack
			if(Role.NON_PARTICIPANT.equals(role)){
				cuType = CuType.RESOURCE;
			}
		}
		return cuType;
	}
	
	/**
	 * Returns a never null but possibly empty TextList
	 * 
	 * @param strings
	 * @return
	 */
	public static TextList convertArrayOfStringsTypeToTextList(ArrayOfStringsType strings){
		TextList textList = new TextList();
		if(strings != null){
        	List<String> stringList = strings.getStrings();
            if(!CollectionUtils.isEmpty(stringList)){
            	for(String s : stringList){
            		textList.add(s);
                }
            }
        }
		return textList;
	}
	
	/**
	 * Returns a never null {@link Categories} containing one entry for each entry string contained in {@link ArrayOfStringsType}
	 * 
	 * @param categories
	 * @return
	 */
	public static Categories convertCategories(ArrayOfStringsType arrayOfCategories){
		
		TextList textList = convertArrayOfStringsTypeToTextList(arrayOfCategories);
		return new Categories(textList);
		
	}
}
