package com.microsoft.exchange;

import java.util.Collection;

import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.component.VEvent;
import net.fortuna.ical4j.model.component.VTimeZone;
import net.fortuna.ical4j.model.component.VToDo;
import net.fortuna.ical4j.model.property.ProdId;
import net.fortuna.ical4j.model.property.Version;

import com.microsoft.exchange.types.CalendarItemType;
import com.microsoft.exchange.types.ItemType;
import com.microsoft.exchange.types.TimeZoneDefinitionType;

public interface ExchangeEventConverter {
	/**
	 * @see <a href="http://www.kanzaki.com/docs/ical/prodid.html">Product Identifier</a>
	 */
	public static final ProdId PROD_ID = new ProdId( "-//ExchangeEventConverter//ExchangeEventConverter 1.1//EN");
	
	/**
	 * @see <a href="http://www.kanzaki.com/docs/ical/version.html">Version</a>
	 */
	public static final Version VERSION = Version.VERSION_2_0;
	
	
	/**
	 * Return a never null {@link Calendar} containing a corresponding
	 * {@link VEvent} for each {@link CalendarItemType} passed in via the items
	 * parameter, and a {@link VToDo} for each {@link TaskType} passed in via
	 * the items parameter.
	 * 
	 * No conversion will be attempted for any other {@link ItemType} implementation.  
	 * 
	 * The {@link Calendar} returned will also contain {@link VTimeZone}
	 * components corresponding to any {@link TimeZoneDefinitionType} that can be accurately mapped to a {@link VTimeZone}.
	 * 
	 * More info on what a succesful timezone mapping is...
	 * 
	 * @param items
	 * @param upn
	 * @return
	 */
	Calendar convertToCalendar(Collection<ItemType> items, String upn);
	
}
