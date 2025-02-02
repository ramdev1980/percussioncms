/*
 * Copyright 1999-2023 Percussion Software, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.percussion.cms.objectstore;

import com.percussion.i18n.PSI18nUtils;
import com.percussion.util.PSDataTypeConverter;
import org.apache.commons.lang3.time.FastDateFormat;

import java.util.Date;
import java.util.Locale;

/**
 * The value of an <code>PSItemField</code> that is treated as a
 * date value.
 */
public class PSDateValue extends PSFieldValue
{
   /**
    * Constructs a new object with the date as the value. Using the default
    * format ("yyyy-MM-dd") and default <code>Locale</code> "en-us".  This is
    * a convenience method that is equivalent to calling
    * {@link #PSDateValue(Date,FastDateFormat) PSDateValue(date,null)}.
    *
    * @param date {@link #PSDateValue(Date,FastDateFormat) PSDateValue(date,null)}
    */
   public PSDateValue(Date date)
   {
      setValue(date, null);
   }

   /**
    * Constructs a new object with the date as the value with the proper format.
    *
    * @param date that will be the value.  <code>null</code> will result in
    * creating a date value for now.
    * @param format if <code>null</code> the default format will be used.
    */
   public PSDateValue(Date date, FastDateFormat format)
   {
      setValue(date, format);
   }

   /**
    * Sets the supplied Date as the content of this value.
    *
    * @param date that will be the value.  <code>null</code> will result in
    * creating a date value for now.
    * @param format if <code>null</code> the default format will be used.
    */
   public void setValue(Date date, FastDateFormat format)
   {
      if(date == null)
         date = new Date();

      if(format == null)
         format = FastDateFormat.getInstance(DEFAULT_FORMAT, DEFAULT_LOCALE);

      m_date = date;
      m_format = format;
   }

   /**
    * Constructs a new object with the date as the value. Using the default
    * format ("yyyy-MM-dd") and default <code>Locale</code> "en-us".
    *
    * @param dateString a string representation of the date.
    * See {@link 
    * com.percussion.util.PSDataTypeConverter#parseStringToDate(String)} for 
    * more information. Must not be <code>null</code> or empty.
    *
    * @return - new PSDateValue.  Not <code>null</code>.
    */
   public static PSDateValue getDateValueFromString(String dateString)
   {
      if(dateString == null || dateString.trim().length() == 0)
         throw new IllegalArgumentException(
            "dateString must not be null or empty");

      Date tDate = PSDataTypeConverter.parseStringToDate(dateString);

      if(tDate == null)
         throw new IllegalArgumentException( "dateString - invalid format");

      return new PSDateValue(tDate);
   }

   /**
    * Implements the interface.  Gets the <code>Date</code> value.
    *
    * @return The <code>Date</code>, may be <code>null</code>.
    */
   public Object getValue()
   {
      return m_date;
   }

   /**
    * Clones this objects.  Makes a deep copy.
    *
    * @return deep copy of this object.
    */
   public Object clone()
   {
      PSDateValue copy = null;

      copy = (PSDateValue)super.clone();

      copy.m_date = (Date)m_date.clone();
      copy.m_format = (FastDateFormat) m_format.clone();

      return copy;
   }

      /** @see IPSFieldValue */
   public boolean equals(Object obj)
   {
      if(obj == null || !(getClass().isInstance(obj)))
         return false;

      PSDateValue comp = (PSDateValue) obj;
      if (!compare(m_date, comp.m_date))
         return false;
      if (!compare(m_format, comp.m_format))
         return false;

      return true;
   }

   /** @see IPSFieldValue */
   public int hashCode()
   {
      int hash = 0;

      // super is abtract, don't call
      hash += hashBuilder(m_date);
      hash += hashBuilder(m_format);

      return hash;
   }

   /**
    * This returns the <code>Date</code> as a <code>String</code>.
    *
    * @return the <code>Date</code> as a <code>String</code>.  Never
    * <code>null</code> or empty.
    */
   public String getValueAsString()
   {
      return m_format.format(m_date);
   }

   /**
    * The content of this value,  set by <code>setValue()</code>,
    * will not be empty or <code>null</code>, if attempt is made to set to
    * <code>null</code> will be set to now.
    */
   private Date m_date;

   /**
    * The format to use for the date, set by <code>setValue()</code>
    * will not be empty or <code>null</code>, defaults to
    * <code>SimpleDateFormat(DEFAULT_FORMAT, DEFAULT_LOCALE)</code>
    */
   private FastDateFormat m_format;

   /**
    * Default date format that will be used to format the date.
    */
   private static final String DEFAULT_FORMAT =  "yyyy-MM-dd HH:mm:ss";

   /**
    * Default Locale created from the default language which is the default
    * language of our internationalization system.
    */
   private static final Locale DEFAULT_LOCALE =
      PSI18nUtils.getLocaleFromString(PSI18nUtils.DEFAULT_LANG);
}
