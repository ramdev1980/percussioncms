/*
 *     Percussion CMS
 *     Copyright (C) 1999-2020 Percussion Software, Inc.
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Affero General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Affero General Public License for more details.
 *
 *     Mailing Address:
 *
 *      Percussion Software, Inc.
 *      PO Box 767
 *      Burlington, MA 01803, USA
 *      +01-781-438-9900
 *      support@percussion.com
 *      https://www.percussion.com
 *
 *     You should have received a copy of the GNU Affero General Public License along with this program.  If not, see <https://www.gnu.org/licenses/>
 */
package com.percussion.extensions.i18n;

import com.percussion.data.PSConversionException;
import com.percussion.extension.PSSimpleJavaUdfExtension;
import com.percussion.i18n.PSI18nUtils;
import com.percussion.server.IPSRequestContext;

import java.util.Date;

/**
 * This is a generic UDF that converts a a given date object to given format and
 * locale.
 *
 * The first parameter is the date to format. This can be a java.util.Date
 * object or a date string. If it is <code>null</code> or <code>empty</code>,
 * current date is assumed.
 *
 * The second parameter is the input date pattern such as "MM dd yyyy HH:mm:ss".
 * This will be ignored if the first parameter is a java.util.Date object.
 * If the first parameter is a date string, this pattern must match with that of
 * the date string.
 *
 * Third parameter is input locale string such as "en-us" or "ja-jp". If
 * <code>null</code> or <code>empty</code>, system default locale is assumed.
 *
 * Fourth parameter is the required output date pattern.
 *
 * Fifth parameter is the output locale. If <code>null</code> or <code>empty</code>,
 * locale is taken from the user context information.
 *
 * The return value is the formatted date string. Never <code>null</code>.
 *
 * @throws PSSimpleJavaUdfExtension
 *
 * @see com.percussion.i18n.PSI18nUtils#formatDate
 * @see com.percussion.i18n.PSI18nUtils#getLocaleFromString
 * @see java.util.Date
 */

public class PSConvertDate
   extends PSSimpleJavaUdfExtension
{
   /* ************ IPSUdfProcessor Interface Implementation ************ */
   public Object processUdf(Object[] params, IPSRequestContext request)
      throws com.percussion.data.PSConversionException
   {
      Object date = params[0];
      if(date == null || date.toString().length() < 1)
      {
         date = new Date();
      }

      String inPattern = "";
      String inLang = "";
      String outPattern = "";
      String outLang = "";

      Object obj = null;

      if(params.length > 1)
      {
         obj = params[1];
         if(obj != null)
            inPattern = obj.toString();
      }

      if(params.length > 2)
      {
         obj = params[2];
         if(obj != null)
            inLang = obj.toString();
      }
      if(params.length > 3)
      {
         obj = params[3];
         if(obj != null)
            outPattern = obj.toString();
      }

      if(params.length > 4)
      {
         obj = params[4];
         if(obj != null)
            outLang = obj.toString();
      }

      String result = null;
      try
      {
         if(outLang == null || outLang.trim().length() < 1)
         {
            outLang = request.getUserContextInformation(
               PSI18nUtils.USER_CONTEXT_VAR_SYS_LANG,
               PSI18nUtils.DEFAULT_LANG).toString();
         }
         if(date instanceof java.util.Date)
         {
            result = PSI18nUtils.formatDate(
               (Date)date, outPattern, PSI18nUtils.getLocaleFromString(outLang));
         }
         else
         {
            result = PSI18nUtils.formatDate(
               date.toString(), inPattern, inLang, outPattern, outLang);
         }

      }
      catch(Exception e)
      {
         int errCode = 0;
         Object[] args = { e.toString(), "PSConvertDate/processUdf" };
         throw new PSConversionException(errCode, args);
      }
      return result;
   }
}
