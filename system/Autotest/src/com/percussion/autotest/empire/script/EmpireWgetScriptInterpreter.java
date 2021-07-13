/******************************************************************************
 *
 * [ EmpireWgetScriptInterpreter.java ]
 *
 * COPYRIGHT (c) 1999 - 2006 by Percussion Software, Inc., Woburn, MA USA.
 * All rights reserved. This material contains unpublished, copyrighted
 * work including confidential and proprietary information of Percussion.
 *
 *****************************************************************************/
package com.percussion.autotest.empire.script;

import com.percussion.autotest.empire.EmpireTestClient;
import com.percussion.autotest.framework.QAScriptDocument;
import com.percussion.autotest.framework.QATestResults;
import com.percussion.test.http.HttpHeaders;
import com.percussion.util.PSStringOperation;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Collection;
import java.util.Iterator;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class EmpireWgetScriptInterpreter extends EmpireScriptInterpreter
{
   public EmpireWgetScriptInterpreter(
      QAScriptDocument doc,
      EmpireTestClient client,
      QATestResults results,
      boolean logToScreen,
      File wgetScriptDir)
   {
      super(doc, client, results, logToScreen);
      m_scriptDir = wgetScriptDir;
   }

   public void run()
   {
      try
      {
         if (!m_scriptDir.exists())
            m_scriptDir.mkdirs();

         m_name = m_qadoc.getName();
         int lastSlash = m_name.lastIndexOf('/');
         int lastPeriod = m_name.lastIndexOf('.');
         if (lastSlash < 0)
            lastSlash = 0;
         if (lastPeriod < 0)
            lastPeriod = m_name.length();

         m_wgetScript = new File(m_scriptDir, m_name.substring(lastSlash + 1, lastPeriod) + "_wget.bat");
         FileOutputStream fOut = new FileOutputStream(m_wgetScript);
         m_out = new BufferedWriter(new OutputStreamWriter(fOut));
         m_out.write("@echo off\r\n");
         m_out.write("REM Autogenerated\r\n");

         File idxFile = new File(m_scriptDir, m_name.substring(lastSlash + 1, lastPeriod) + "_idx.html");
         FileOutputStream idxOut = new FileOutputStream(idxFile);
         m_idxOut = new BufferedWriter(new OutputStreamWriter(idxOut));

         m_idxOut.write("<HTML>\r\n<HEAD><TITLE>" + m_name + " : test results index</TITLE>\r\n");
         String baseHref = ("file:///" + m_scriptDir.getAbsolutePath()).replace('\\', '/');
         if (!baseHref.endsWith("/"))
            baseHref = baseHref + "/";

         m_idxOut.write("<BASE HREF=\"" + baseHref + "\" TARGET=\"body\"></BASE></HEAD>\r\n");
         m_idxOut.write("<BODY>\r\n<FONT SIZE=\"-1\"><UL>\r\n");

         try
         {
            super.run();
            m_idxOut.write("\r\n</UL></FONT></BODY>\r\n</HTML>");
         }
         finally
         {
            try { m_idxOut.close(); } catch (Throwable t) { t.printStackTrace(); }
            try { idxOut.close(); } catch (Throwable t) { t.printStackTrace(); }
            try { m_out.close(); } catch (Throwable t) { t.printStackTrace(); }
            try { fOut.close(); } catch (Throwable t) { t.printStackTrace(); }
         }

         log("Finished script...");
      }
      catch (Throwable t)
      {
         t.printStackTrace();
         log(t);
      }
   }


   protected void handleGetOrPostRequest(Element requestElem)
      throws Exception
   {
      try
      {
         String elementType = requestElem.getNodeName();
         String requestType = null;
         if (elementType.equals("PostRequest"))
            requestType = "POST";
         else
            requestType = "GET";

         String href = getAttribute(requestElem, "href");
         String originalHref = href;

         String skipBecause = getAttribute(requestElem, "skipBecause");

         if (skipBecause != null && skipBecause.length() > 0)
         {
            // this test is disabled for some reason
            String message = "Skipped test because " + skipBecause;
            log(message);
            m_out.write("REM " + message);
            m_out.write("ECHO Skipped test because " + skipBecause);
            return;
         }

         String expectHref = null;
         String dir = null;
         String outFile = null;
         String dosHref = null;
         String dosExpectHref = null;

         dosHref = DOSify(href);

         {// // // // //
            String modHref = PSStringOperation.replace(
               PSStringOperation.replace(href,"?","-Q-"),"%","-H-");
            String fileHref = DOSify(modHref);

            File file = new File(new URL(fileHref).getFile()) ;

            dir = file.getParent();

            if (dir.startsWith("\\") || dir.startsWith("/"))
               dir = dir.substring(1, dir.length());

            outFile = file.getName();
         }// // // // //

         {// // // // //
            // get the expected headers/values and the URL of the expected data
            Element expectEl = null;
            NodeList children = requestElem.getChildNodes();
            for (int i = 0; i < children.getLength(); i++)
            {
               Node kid = children.item(i);
               if ((kid instanceof Element) && kid.getNodeName().equals("Expect"))
               {
                  expectEl = (Element)kid;
                  break;
               }
            }
            if (expectEl != null)
            {
               expectHref = getAttribute(expectEl, "href");
            }
         }// // // // //

         if (expectHref != null && expectHref.length() > 0)
         {
            log("Expect: " + expectHref);

            expectHref = PSStringOperation.replace(
               PSStringOperation.replace(expectHref,"?","-Q-"),"%","-H-");

            outFile = new File(new URL(expectHref).getFile()).getName();
            dosExpectHref = DOSify(expectHref);
            outFile = new File(new URL(dosExpectHref).getFile()).getName();
         }

         HttpHeaders reqHeaders = getChildHeaders(requestElem);

         String type = (requestType.equals("POST")) ? "POST : " : "" ;

         if (requestType.equals("POST"))
         {
            m_out.write("REM We don't support auto-downloading of POST requests\r\n");
            m_out.write("ECHO ***WARNING*** skipping POST request to \"" + href + "\"\r\n");
         }
         else
         {
            String logFileName = "\"wget_log.txt@" + outFile + "\"";

            m_out.write("IF NOT EXIST " + dir + " MKDIR " + dir + "\r\n");
            m_out.write("PUSHD " + dir + "\r\n");
            m_out.write("ECHO Getting URL \"" + dosHref + "\"\r\n");
            m_out.write("ECHO Retrieving URL \"" + dosHref + "\" >> " + logFileName + "\r\n");
            m_out.write("DATE /T >> " + logFileName + "\r\n");
            m_out.write("TIME /T >> " + logFileName+ "\r\n");
            m_out.write("wget --server-response --cache=off -a " + logFileName);

            sendHeaderOptions(reqHeaders, m_out);

            if (expectHref != null)
            {
               m_out.write(" --output-document=\"" + outFile + "\"");
            }

            m_out.write(" \"" + dosHref + "\"\r\n");
            m_out.write("IF ERRORLEVEL 1 COPY " + logFileName + " \"" + outFile + "\"\r\n");
            m_out.write("POPD\r\n");
         }

         if (expectHref != null && expectHref.length() > 0)
         {
            m_idxOut.write("<LI> " + type + "<A HREF=\"" + dir + "/"
               + URLEncoder.encode(new File(new URL(expectHref).getFile()).getName()) + "\">"
               + originalHref + "</A>");

            sendHeaderIndex(reqHeaders, m_idxOut);
            m_idxOut.write("</LI>\r\n");
         }

         m_idxOut.flush();
         m_out.flush();
      }
      catch (Exception e)
      {
         e.printStackTrace();
      }
   }

   protected void sendHeaderOptions(HttpHeaders headers, Writer out) throws Exception
   {
      Collection keySet = headers.getHeaderNames();
      for (Iterator i = keySet.iterator(); i.hasNext(); )
      {
         String headerName = i.next().toString();
         for (Iterator j = headers.getHeaders(headerName); j.hasNext(); )
         {
            String headerVal = j.next().toString();
            out.write(" --header=\"" + headerName + ": " + headerVal + "\"");
         }
      }
   }

   protected void sendHeaderIndex(HttpHeaders headers, Writer out) throws Exception
   {
      Collection keySet = headers.getHeaderNames();
      for (Iterator i = keySet.iterator(); i.hasNext(); )
      {
         String headerName = i.next().toString();
         for (Iterator j = headers.getHeaders(headerName); j.hasNext(); )
         {
            String headerVal = j.next().toString();
            out.write("<BR><B>" + headerName + "</B>: <I>" + headerVal + "</I></A>");
         }
      }
   }

   protected String DOSify(String in)
   {
      // escape parens in the expect URL
      if (in.indexOf('%') >= 0)
      {
         // wget converts percent when it shouldn't
         StringBuilder inBuf = new StringBuilder(in.length());
         for (int i = 0; i < in.length(); i++)
         {
            char c = in.charAt(i);
            if (c == '%')
            {
               // DOS forces us to put quotes around these bloody things
               inBuf.append("\"%%\"");
            }
            else
            {
               inBuf.append(c);
            }
         }

         in = inBuf.toString();
      }
      return in;
   }


   private String m_name;
   private File m_scriptDir;
   private File m_wgetScript;
   private Writer m_out;
   private Writer m_idxOut;
}
