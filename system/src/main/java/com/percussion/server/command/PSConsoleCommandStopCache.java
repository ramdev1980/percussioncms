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
package com.percussion.server.command;

import com.percussion.error.PSErrorManager;
import com.percussion.server.IPSServerErrors;
import com.percussion.server.PSRequest;
import com.percussion.util.PSCacheException;
import com.percussion.xml.PSXmlDocumentBuilder;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.util.Locale;

/**
 * This class implements the execution of 'stop cache' console command and
 * dumps the results as an XML document to the console.
 */
public class PSConsoleCommandStopCache extends PSConsoleCommandCache
{
   /**
    * The constructor for this class. If the command argument is specified as
    * 'debug' it turns off the cache debugging mode irrespective of cache is
    * running or not when executing this command. If there is no argument it
    * stops the cache. If there is any argument other than 'debug' it is
    * ignored and behaves same as no argument supplied.
    *
    * @param cmdArgs the argument string to use when executing this command, may
    * be <code>null</code> or empty.
    */
   public PSConsoleCommandStopCache(String cmdArgs)
   {
      super(cmdArgs);
   }

   /**
    * Execute the command specified by this object. The results are returned
    * as an XML document of the appropriate structure for the command.
    *   <P>
    * The execution of this command results in the following XML document
    * structure: (includes the cache statistics also).
    * <PRE><CODE>
    *      &lt;ELEMENT PSXConsoleCommandResults   (command, CacheStatistics,
    *    resultCode, resultText)&gt;
    *
    *      &lt;--
    *         the command that was executed (includes the arguments)
    *      --&gt;
    *      &lt;ELEMENT command   (#PCDATA)&gt;
    *
    *      &lt;--
    *    See {@link PSConsoleCommandCache#getCacheStatistics getCacheStatistics}
    *    for description of <code>CacheStatistics</code> element.
    *      --&gt;
    *      &lt;ELEMENT CacheStatistics   (#PCDATA)&gt;
    *
    *      &lt;--
    *         the result code for the command execution
    *      --&gt;
    *      &lt;ELEMENT resultCode   (#PCDATA)&gt;
    *
    *      &lt;--
    *         the message text associated with the result code
    *      --&gt;
    *      &lt;ELEMENT resultText   (#PCDATA)&gt;
    * </CODE></PRE>
    *
    * @param request the requestor object, may be <code>null</code>
    *
    * @return the result document, never <code>null</code>
    *
    * @throws PSConsoleCommandException   if an error occurs during execution
    */
   public Document execute(PSRequest request)
      throws PSConsoleCommandException
   {
      Document respDoc = getResultsDocument();
      Element root = respDoc.getDocumentElement();

      if(m_cmdArgs == null || !m_cmdArgs.equalsIgnoreCase("debug"))
      {
         Locale loc;
         if (request != null)
            loc = request.getPreferredLocale();
         else
            loc = Locale.getDefault();

         Element statistics = getCacheStatistics( respDoc );
         boolean success = false;
         try
         {
            success = stopCache();
         }
         catch(PSCacheException pscex)
         {
            PSXmlDocumentBuilder.addElement(respDoc, root, "resultCode",
               String.valueOf(pscex.getErrorCode()));

            String termMsg = PSErrorManager.createMessage(
                IPSServerErrors.RCONSOLE_UNABLE_TO_EXECUTE_CACHE_COMMAND,
                   new Object[] {pscex.getLocalizedMessage( loc )}, loc);
            PSXmlDocumentBuilder.addElement(respDoc, root, "resultText", termMsg);
            return respDoc;
         }

         if(success)
         {
            root.appendChild( statistics );

            PSXmlDocumentBuilder.addElement(respDoc, root, "resultCode",
               String.valueOf(IPSServerErrors.RCONSOLE_CACHE_STOPPED));

            String termMsg = PSErrorManager.getErrorText(
               IPSServerErrors.RCONSOLE_CACHE_STOPPED, true, loc);
            PSXmlDocumentBuilder.addElement(respDoc, root, "resultText", termMsg);
         }
         else
         {
            PSXmlDocumentBuilder.addElement(respDoc, root, "resultCode",
               String.valueOf(IPSServerErrors.RCONSOLE_CACHE_ALREADY_STOPPED));

            String termMsg = PSErrorManager.getErrorText(
               IPSServerErrors.RCONSOLE_CACHE_ALREADY_STOPPED, true, loc);
            PSXmlDocumentBuilder.addElement(respDoc, root, "resultText", termMsg);
         }
      }
      else
      {
         setCacheDebugLogging( false );
         PSXmlDocumentBuilder.addElement(respDoc, root, "resultCode", "0");
         PSXmlDocumentBuilder.addElement(respDoc, root, "resultText",
            "Stopped logging cache debug messages.");
      }

      return respDoc;
   }

   //see super class method for description.
   public String getCommandName()
   {
      return ms_cmdName;
   }

   /**
    * The command executed by this class.
    */
   public final static String ms_cmdName = "stop cache";
}
