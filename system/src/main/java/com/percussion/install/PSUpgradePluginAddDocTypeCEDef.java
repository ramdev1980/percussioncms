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
package com.percussion.install;

import org.w3c.dom.Element;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
/**
 * This plugin has been written to add the DocType to the ContentEditorSystemDef
 * XML file. The upgrade process updates this file and by doing so, it is
 * removing the DocType and xml processing instruction from the xml file.
 */

public class PSUpgradePluginAddDocTypeCEDef implements IPSUpgradePlugin
{
   /**
    * Default constructor
    */
   public PSUpgradePluginAddDocTypeCEDef()
   {
   }

   /**
    * Implements the process function of IPSUpgardePlugin.
    * @param config PSUpgradeModule object.
    * @param elemData We do not use this element in this function.
    * @return <code>null</code>
    */
   public PSPluginResponse process(IPSUpgradeModule config, Element elemData)
   {

      config.getLogStream().println("Adding DOCTYPE to file " +
         "ContentEditorSystemdef.xml...");

      File file = null;
      PrintWriter pw = null;
      ByteArrayOutputStream bos = null;
      FileInputStream fis = null;
      try
      {
         file = new File(RxUpgrade.getRxRoot() +
            "rxconfig/Server/ContentEditors/ContentEditorSystemDef.xml");

         fis = new FileInputStream(file);
         bos = new ByteArrayOutputStream();
         copyStream(fis, bos);
         String docStr = bos.toString();
         
         docStr = InstallUtil.addDocType(docStr, "ContentEditorSystemDef",
               "SYSTEM", "sys_ContentEditorSystemDef.dtd");
         pw = new PrintWriter(new FileOutputStream(file));
         pw.write(docStr);
         file =  new File(RxUpgrade.getRxRoot() +
         "rxconfig/Server/ContentEditors/DTD/sys_ContentEditorSystemDef.dtd");
         if(file.exists())
         {
            file.delete();
         }
         file =  new File(RxUpgrade.getRxRoot() +
            "rxconfig/Server/ContentEditors/DTD/sys_BasicObjects.dtd");
         if(file.exists())
         {
            file.delete();
         }
      }
      catch(Exception e)
      {
         e.printStackTrace(config.getLogStream());
      }
      finally
      {
         try
         {
            if(pw != null)
            {
               pw.close();
               pw =null;
            }
         }
         catch(Throwable t)
         {
         }
      }
      config.getLogStream().println("leaving the process() of the plugin...");
      return null;
   }
   
   /**
   * Method to copy Java InputStream to OutputStream.
   *
   * @param in Input stream tp copy from, never <code>null</code>.
   *
   * @param out Output stream to copy to, never <code>null</code>.
   *
   * @return number bytes copied
   *
   * @throws IOException in case of any error while copying.
   *
   */
   public static long copyStream(InputStream in, OutputStream out)
      throws IOException
   {
      int nCopied = 0;
      final byte[] buffer = new byte[ DEFAULT_BUFFER_SIZE ];
      int n = 0;
      while( -1 != (n = in.read( buffer )) )
      {
          out.write( buffer, 0, n );
          nCopied += n;
      }
      return nCopied;
   }
   /**
    * default buffer size
    */
   private static final int DEFAULT_BUFFER_SIZE = 1024 * 4;
}
