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
//See https://code.google.com/p/exchangeling/source/browse/trunk/src/main/java/ExchangeEventConverter/iCal4j.java

package com.microsoft.exchange;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.StringTokenizer;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.Date;
import net.fortuna.ical4j.model.DateList;
import net.fortuna.ical4j.model.DateTime;
import net.fortuna.ical4j.model.Property;
import net.fortuna.ical4j.model.Recur;
import net.fortuna.ical4j.model.TextList;
import net.fortuna.ical4j.model.TimeZoneRegistry;
import net.fortuna.ical4j.model.TimeZoneRegistryFactory;
import net.fortuna.ical4j.model.WeekDay;
import net.fortuna.ical4j.model.component.VEvent;
import net.fortuna.ical4j.model.component.VTimeZone;
import net.fortuna.ical4j.model.parameter.Cn;
import net.fortuna.ical4j.model.parameter.CuType;
import net.fortuna.ical4j.model.parameter.PartStat;
import net.fortuna.ical4j.model.parameter.Value;
import net.fortuna.ical4j.model.property.Attendee;
import net.fortuna.ical4j.model.property.CalScale;
import net.fortuna.ical4j.model.property.Categories;
import net.fortuna.ical4j.model.property.Clazz;
import net.fortuna.ical4j.model.property.Description;
import net.fortuna.ical4j.model.property.ExDate;
import net.fortuna.ical4j.model.property.Location;
import net.fortuna.ical4j.model.property.Organizer;
import net.fortuna.ical4j.model.property.Priority;
import net.fortuna.ical4j.model.property.ProdId;
import net.fortuna.ical4j.model.property.RRule;
import net.fortuna.ical4j.model.property.RecurrenceId;
import net.fortuna.ical4j.model.property.Transp;
import net.fortuna.ical4j.model.property.Uid;
import net.fortuna.ical4j.model.property.Version;
import net.fortuna.ical4j.model.property.XProperty;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import com.microsoft.exchange.types.ArrayOfStringsType;
import com.microsoft.exchange.types.AttendeeType;
import com.microsoft.exchange.types.CalendarItemType;
import com.microsoft.exchange.types.CalendarItemTypeType;
import com.microsoft.exchange.types.DayOfWeekIndexType;
import com.microsoft.exchange.types.DayOfWeekType;
import com.microsoft.exchange.types.DeletedOccurrenceInfoType;
import com.microsoft.exchange.types.ImportanceChoicesType;
import com.microsoft.exchange.types.ItemIdType;
import com.microsoft.exchange.types.LegacyFreeBusyType;
import com.microsoft.exchange.types.RecurrenceType;
import com.microsoft.exchange.types.RelativeMonthlyRecurrencePatternType;
import com.microsoft.exchange.types.SensitivityChoicesType;
import com.microsoft.exchange.types.TimeZoneType;



@Deprecated
public class ExchangeEventConverterOLD {
	public Calendar ical;

	protected final static Log log = LogFactory.getLog(ExchangeEventConverterOLD.class);
	public HashMap<String, String> timeZoneMap;
	private List<VTimeZone> calendarTimeZones;
	
	public ExchangeEventConverterOLD() {
		
		log.debug("ExchangeEventConverter instantiated");
		
		ApplicationContext context = new ClassPathXmlApplicationContext("classpath:com/microsoft/exchange/exchangeContext-ical4jTimeZoneMap.xml");
		timeZoneMap = (HashMap<String, String>) context.getBean("timeZoneMap");
		calendarTimeZones = new ArrayList<VTimeZone>();
		
        ical   = new Calendar();
        ical.getProperties().add(new ProdId("-//UWExchange ICS Converter//ExchangeEventConverter 1.0//EN"));
        ical.getProperties().add(Version.VERSION_2_0);
        ical.getProperties().add(CalScale.GREGORIAN);
        //ical.getComponents().add( convertToVTimeZone(TimeZone.getDefault()) );
    }
    
    
    public void add( CalendarItemType item ) {
        ical.getComponents().add( convertExchangeCalendarItemToiCal(item) );
    }
    
    @Override
    public String toString() {
        return ical.toString();
    }
   
    public static RRule convertRecurrence( CalendarItemType item ) {
        RRule   rule    = null;
        Recur recur     = null;
        if( item.getCalendarItemType() != null && item.getCalendarItemType().equals( CalendarItemTypeType.RECURRING_MASTER )) {
            if( item.getRecurrence() != null ) {
                    // exchange recurrences are non-exclusive, ie it is possible to get different recurrence types
                RecurrenceType recurrenceType = item.getRecurrence();
                log.info("RECR: "+item.getSubject());
                if( recurrenceType.getAbsoluteMonthlyRecurrence() != null ) {
                    log.info("RECR: getAbsoluteMonthlyRecurrence");
                    recur               = new Recur( Recur.MONTHLY, item.getAdjacentMeetingCount() );
                    recur.setInterval( recurrenceType.getAbsoluteMonthlyRecurrence().getInterval() );
                    if( recurrenceType.getNumberedRecurrence() != null ) {
                        recur.setCount( recurrenceType.getNumberedRecurrence().getNumberOfOccurrences() );
                    }
                }
                
                if( recurrenceType.getAbsoluteYearlyRecurrence() != null ) {
                    log.info("RECR: getAbsoluteYearlyRecurrence  [not implemented]");
                    
                }
                
                if( recurrenceType.getDailyRecurrence() != null ) {
                    log.info("RECR: getDailyRecurrence");
                    recur               = new Recur( Recur.DAILY, item.getAdjacentMeetingCount() );
                    recur.setInterval( recurrenceType.getDailyRecurrence().getInterval() );
                }
                
                
                if( recurrenceType.getRelativeMonthlyRecurrence() != null ) {
                    // to do
                    log.info("RECR: getRelativeMonthlyRecurrence");
                    RelativeMonthlyRecurrencePatternType rrpt = recurrenceType.getRelativeMonthlyRecurrence();
                    recur               = new Recur( Recur.MONTHLY, item.getAdjacentMeetingCount() );
                    if( rrpt.getDayOfWeekIndex().equals( DayOfWeekIndexType.FIRST ) ) {
                        recur.getSetPosList().add( new Integer (1));
                    } else if(  rrpt.getDayOfWeekIndex().equals( DayOfWeekIndexType.SECOND ) ) {
                        recur.getSetPosList().add( new Integer (2));
                    } else if(  rrpt.getDayOfWeekIndex().equals( DayOfWeekIndexType.THIRD ) ) {
                        recur.getSetPosList().add( new Integer (3));
                    } else if(  rrpt.getDayOfWeekIndex().equals( DayOfWeekIndexType.FOURTH ) ) {
                        recur.getSetPosList().add( new Integer (4));
                    } else if(  rrpt.getDayOfWeekIndex().equals( DayOfWeekIndexType.LAST ) ) {
                        recur.getSetPosList().add( new Integer (5));
                    }
                    WeekDay weekDay = convertDayOfWeek( rrpt.getDaysOfWeek() );
                    if (weekDay != null) {
                        recur.getDayList().add(weekDay);
                    }
                    recur.setInterval( rrpt.getInterval() );
                }
                if( recurrenceType.getRelativeYearlyRecurrence() != null ) {
                    // to do
                    log.info("RECR: getRelativeYearlyRecurrence  [not implemented]");
                }
                
                if( recurrenceType.getWeeklyRecurrence() != null ) {
                    log.info("RECR: getWeeklyRecurrence (interval="+recurrenceType.getWeeklyRecurrence().getInterval()+")");
                    recur               = new Recur( Recur.WEEKLY, item.getAdjacentMeetingCount() );
                    
                    recur.setInterval( recurrenceType.getWeeklyRecurrence().getInterval() );
                    for( DayOfWeekType dayOfWeek : recurrenceType.getWeeklyRecurrence().getDaysOfWeek() ) {
                        WeekDay weekDay = convertDayOfWeek( dayOfWeek );
                        if( weekDay != null ) {
                            recur.getDayList().add( weekDay );
                        }
                    }
                }

                if( recur != null && recurrenceType.getEndDateRecurrence() != null ) {
                    DateTime endTime = new DateTime(recurrenceType.getEndDateRecurrence().getEndDate().toGregorianCalendar().getTime());
                    java.util.GregorianCalendar cal = new java.util.GregorianCalendar();
                    cal.setTime(endTime);
                    cal.add(java.util.GregorianCalendar.DATE, 1);  // add 1 day to the end time to include the last day
                    endTime = new DateTime(cal.getTime());
                    recur.setUntil( endTime );
                }
                
                if( recur != null && recurrenceType.getNoEndRecurrence() != null ) {
                    log.info("RECR: getNoEndRecurrence");
                    recur.setUntil(null);
                }
                
                if( recur != null && recurrenceType.getNumberedRecurrence() != null ) {
                    log.info("RECR: getNumberedRecurrence");
                    recur.setCount( recurrenceType.getNumberedRecurrence().getNumberOfOccurrences() );
                }

            }
        }
        
        if( recur != null ) {
            rule = new RRule( recur );
        }
        
        return rule;
    }

    public static WeekDay convertDayOfWeek( DayOfWeekType dayOfWeek ) {
        if (dayOfWeek.equals(DayOfWeekType.MONDAY)) {
            return WeekDay.MO;
        } else if (dayOfWeek.equals(DayOfWeekType.TUESDAY)) {
            return WeekDay.TU;
        } else if (dayOfWeek.equals(DayOfWeekType.WEDNESDAY)) {
            return WeekDay.WE;
        } else if (dayOfWeek.equals(DayOfWeekType.THURSDAY)) {
            return WeekDay.TH;
        } else if (dayOfWeek.equals(DayOfWeekType.FRIDAY) ) {
            return WeekDay.FR;
        } else if( dayOfWeek.equals(DayOfWeekType.SATURDAY) ) {
            return WeekDay.SA;
        } else if( dayOfWeek.equals(DayOfWeekType.SUNDAY) ) {
            return WeekDay.SU;
        }
        return null;
    }

    public VEvent convertExchangeCalendarItemToiCal( CalendarItemType item ) {
        VEvent event = null;
        net.fortuna.ical4j.model.TimeZoneRegistry registry = TimeZoneRegistryFactory.getInstance().createRegistry();
        net.fortuna.ical4j.model.TimeZone timezone = registry.getTimeZone( TimeZone.getDefault().getID() );
        
        log.debug("processing CalendarItemType: "+item.getSubject());
        String msCalendarTimeZone = item.getTimeZone();
        TimeZoneType msMeetingTimeZone = item.getMeetingTimeZone();
        
        if(msMeetingTimeZone!=null){
        	log.debug("meetingTimeZone found" + msMeetingTimeZone.getTimeZoneName());
        }else{
        	log.debug("item.getMeetingTimeZone() is null");
        }
        
        //set timezone
        log.debug("TimeZone set (msZoneID="+msCalendarTimeZone+", ical4jZoneID="+timeZoneMap.get(msCalendarTimeZone)+")");
        timezone = registry.getTimeZone( timeZoneMap.get(msCalendarTimeZone) );
        
        
        //if the VCALENDAR does not contain the current time zone, then add it
        if(!calendarTimeZones.contains(timezone.getVTimeZone())){
        	log.debug("Added timeZone to VCALENDAR");
        	timezone = registry.getTimeZone( timeZoneMap.get(msCalendarTimeZone) );
        	calendarTimeZones.add(timezone.getVTimeZone());
        	this.ical.getComponents().add( timezone.getVTimeZone());
        }

        if( item.isIsAllDayEvent() ) {
            Date start      = null;
            Date end        = null;
            if (item.getStart() != null) {
                start       = new Date(item.getStart().toGregorianCalendar().getTime());
            }
            if (item.getEnd() != null) {
                end         = new Date(item.getEnd().toGregorianCalendar().getTime());
            }
            event           = new VEvent(start, end, item.getSubject());
            event.getProperties().getProperty( Property.DTSTART ).getParameters().add( Value.DATE );
        } else {
            DateTime start  = null;
            DateTime end    = null;
        
            if (item.getStart() != null) {
                start       = new DateTime(item.getStart().toGregorianCalendar().getTime());
                start.setTimeZone(timezone);
                //start.setTimeZone((net.fortuna.ical4j.model.TimeZone) t);
            }
            if (item.getEnd() != null) {
                end         = new DateTime(item.getEnd().toGregorianCalendar().getTime());
                end.setTimeZone(timezone);
                //end.setTimeZone((net.fortuna.ical4j.model.TimeZone) t);
            }
            event           = new VEvent(start, end, item.getSubject());
            event.getProperties().add(timezone.getVTimeZone().getTimeZoneId());
        }
        
        
        RRule rrule = convertRecurrence( item );

        if( rrule != null )
            event.getProperties().add( rrule );

        Uid uid = convertUid( item );
        if( uid != null )
            event.getProperties().add(uid);

        if( item.getLocation() != null )
            event.getProperties().add( new Location( item.getLocation() ) );
        
        if( item.getBody() != null )
            event.getProperties().add( new Description( convertHtmlToText( item.getBody().getValue() )) );
        
        
        //FindItemRequest only returns display names, must use getItem for additonal properties, 
        //see remarks here: http://msdn.microsoft.com/en-us/library/exchange/aa566107(v=exchg.140).aspx
        
        if( item.getOrganizer() != null) {
            try {
                String name = item.getOrganizer().getMailbox().getName();
                String email = item.getOrganizer().getMailbox().getEmailAddress();
                Organizer organizer = new Organizer();
                if ( email != null ) {
                    organizer.setValue("mailto:" + email);
                }
                organizer.getParameters().add(new Cn(name));
                event.getProperties().add(organizer);
            } catch (URISyntaxException ex) {
                log.error("event organizer: " + ex);
            }
        }
        
        if ( item.getResources() != null ) {
            log.debug("resource attendees");
            for (AttendeeType attendee : item.getResources().getAttendees() ) {
                // probably need to do something with these too
                Attendee participant = convertAttendee( attendee );

                // to get the real CuType requires an extra SOAP request so this is just a hack
                // should use GetUserAvailabilityRequest to retrieve getAttendeeType() and see if it is "ROOM"
                participant.getParameters().add( new CuType( "ROOM" ) );
                
                event.getProperties().add(participant);
                //ATTENDEE;CUTYPE=GROUP:MAILTO:ietf-calsch@imc.org
            }
        }

        if ( item.getOptionalAttendees() != null ) {
            for (AttendeeType attendee : item.getOptionalAttendees().getAttendees() ) {
                Attendee participant = convertAttendee( attendee );
                if (participant != null) {
                    event.getProperties().add(participant);
                }
            }
        }
        
        if ( item.getRequiredAttendees() != null ) {
            for (AttendeeType attendee : item.getRequiredAttendees().getAttendees() ) {
                Attendee participant = convertAttendee( attendee );
                if (participant != null) {
                    event.getProperties().add(participant);
                }
            }
        }

        if( item.getCalendarItemType().equals( CalendarItemTypeType.EXCEPTION) ) {
            DateTime originalStart = new DateTime( item.getOriginalStart().toGregorianCalendar().getTime() );
            originalStart.setTimeZone((net.fortuna.ical4j.model.TimeZone) timezone);
            event.getProperties().add( new RecurrenceId( originalStart ));
        }
        
        if( item.getCalendarItemType().equals( CalendarItemTypeType.RECURRING_MASTER) && item.getDeletedOccurrences() != null ) {
            DateList dateList = new DateList();
            for( DeletedOccurrenceInfoType doit : item.getDeletedOccurrences().getDeletedOccurrences()) {
                DateTime deletedOccurrence = new DateTime( doit.getStart().toGregorianCalendar().getTime() );
                dateList.add(deletedOccurrence);
            }
            event.getProperties().add( new ExDate(dateList) );
        }
        
        //LegacyFreeBusyStatus determines TRANSP.
        //Possible values are (Busy => OPAQUE, NoData => OPAQUE, Tentative => OPAQUE,OoF => OPAQUE, Free => TRANSPARENT) 
        LegacyFreeBusyType freeBusy = item.getLegacyFreeBusyStatus();
        String freeBusyString = freeBusy.toString();
        String transpValue = "OPAQUE";
        
        if(freeBusyString.equalsIgnoreCase("Free")){
        	transpValue = "TRANSPARENT";
        }
        event.getProperties().add( new Transp(transpValue));
        
        //SENSITIVITY => CLASS
        //Possible values are (Normal => PUBLIC, Confidential => CONFIDENTIAL, Personal => PRIVATE, Private => PRIVATE
        SensitivityChoicesType sensitivity = item.getSensitivity();
        String sensitivityString=  sensitivity.value();
        String sensitivityValue = "PUBLIC";
        if(sensitivityString.equalsIgnoreCase("Confidential")){
        	sensitivityValue = "CONFIDENTIAL";
        }else if(sensitivityString.equalsIgnoreCase("Private") || sensitivityString.equalsIgnoreCase("Personal")){
        	sensitivityValue = "PRIVATE";
        }
        event.getProperties().add( new Clazz(sensitivityValue));
        
        //IMPORTANCE => PRIORIRY
        //Possible values are (HIGH => 1, NORMAL => 5, LOW => 9) 
        ImportanceChoicesType importance = item.getImportance();
        int importanceInt = 5;
        if(importance.equals(ImportanceChoicesType.HIGH)){
        	importanceInt = 1;
        }else if(importance.equals(ImportanceChoicesType.NORMAL)){
        	importanceInt =5;
        }else if(importance.equals(ImportanceChoicesType.LOW)){
        	importanceInt = 9;
        }else{
        	if(log != null) log.debug("Importance not recognized.  Using default (NORMAL => PRIORITY:5)");
        }
        event.getProperties().add( new Priority(importanceInt));	
        
        //CATEGORIES
        ArrayOfStringsType categories = item.getCategories();
        if(categories != null){
        	TextList categoriesTextList = new TextList();
            List<String> categoriesStringList = categories.getStrings();
            if(!categoriesStringList.isEmpty()){
            	for(String c : categoriesStringList){
            		categoriesTextList.add(c);
                }
                event.getProperties().add( new Categories(categoriesTextList));
            }
        }

        //might be handy to set the itemId and changeKey...
        event.getProperties().add(new XProperty("X-ItemID", item.getItemId().getId()));
        event.getProperties().add(new XProperty("X-ChangeKey",item.getItemId().getChangeKey()));
        
        return event;
    }
    
    public static String convertHtmlToText( String html ) {
        String convertedString = html;
        convertedString = convertedString.replaceAll("<.*?>", "");  // strip all HTML tags
        convertedString = convertedString.replaceAll("&nbsp;", " ");  // convert &nbsp;
        convertedString = convertedString.replaceAll("&amp;", "&");  // convert &amp;
        convertedString = convertedString.trim(); // strip leading and trailing whitespace
        convertedString = convertedString.replaceAll("\n{2,}", "\n"); // collapse multipe empty lines
        return convertedString;
    }
    
    public static Attendee convertAttendee( AttendeeType attendee ) {
        Attendee icalAttendee = null;
        try {
            icalAttendee = new Attendee();
            String name = attendee.getMailbox().getName();
            String email = attendee.getMailbox().getEmailAddress();
            String response = attendee.getResponseType().value();

            if ( response.equals("Unknown") ) {
                response = null;
            } else if ( response.equals("Accept") ) {
                response = "ACCEPTED";
            } else if ( response.equals("Decline") ) {
                response = "DECLINED";
            } else if ( response.equals("Tentative") ) {
                response = "TENTATIVE";
            } else if ( response.equals("Organizer") ) {
                response = "ACCEPTED";
            } else if ( response.equals("NoResponseReceived") ) {
                response = null;
            }
//            if ( email != null ) {
            if ( email != null && validateEmail(email) ) {
                icalAttendee.setValue("mailto:" + email);
            }
            icalAttendee.getParameters().add( new Cn(name) );
            if ( response != null ) {
                icalAttendee.getParameters().add( new PartStat(response) );
            }
        } catch (URISyntaxException ex) {
            log.error("convertAttendee: " + ex);
        }
        return icalAttendee;
    }
    
    public static VTimeZone convertToVTimeZone( TimeZone tz ) {
        TimeZoneRegistry registry = TimeZoneRegistryFactory.getInstance().createRegistry();
        net.fortuna.ical4j.model.TimeZone timezone = registry.getTimeZone( tz.getID() );
        return timezone.getVTimeZone();
    }

    public static Uid convertUid( CalendarItemType item ) {
        Uid uid = null;
        if( item.getUID() != null ) {
            uid = new Uid( item.getUID() );
        } else if( item.getItemId() != null ) {
            ItemIdType id = item.getItemId();
            uid = new Uid( id.getChangeKey() );
        }
        return uid;
    }
    
    public static String convertCalendarItemType( ArrayList<CalendarItemType> calendarItemType ) {
        ExchangeEventConverterOLD ical4j = new ExchangeEventConverterOLD();
        
        for( CalendarItemType item : calendarItemType ) {
            ical4j.add(item);
        }
        return ical4j.toString();
    }

    public static boolean validateEmail( String email ){
         Pattern p = Pattern.compile(".+@.+\\.[a-z]+");
         Matcher m = p.matcher(email);
         boolean matchFound = m.matches();
         StringTokenizer st = new StringTokenizer(email, ".");
         String lastToken = null;
         while (st.hasMoreTokens()) {
             lastToken = st.nextToken();
         }
         if (matchFound && lastToken.length() >= 2
             && email.length() - 1 != lastToken.length()) {
             return true;
         }
         else return false;
     }
    
	
	
    
    

}
