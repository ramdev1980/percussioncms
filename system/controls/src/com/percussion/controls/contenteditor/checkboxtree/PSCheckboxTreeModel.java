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
 *      https://www.percusssion.com
 *
 *     You should have received a copy of the GNU Affero General Public License along with this program.  If not, see <https://www.gnu.org/licenses/>
 */
package com.percussion.controls.contenteditor.checkboxtree;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeModel;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.w3c.dom.Document;

/**
 * Tree model with extensions for loading the model data from an xml document.
 */
public class PSCheckboxTreeModel extends DefaultTreeModel implements TreeModel
{

   private static final Logger log = LogManager.getLogger(PSCheckboxTreeModel.class);

   /**
    * Construct and populate the tree model.
    * 
    * @param baseURL the base URL of the Applet, not <code>null</code>.
    * @param docUrlStr the URL string where the xml definition document is 
    *    found, not <code>null</code> or empty.
    * @param psSessionId the value of "pssessionid" applet parameter. It may
    * be <code>null</code> or empty if the parameter is not defined. 
    */
   public PSCheckboxTreeModel(URL baseURL, String docUrlStr, String psSessionId)
   {
      super(new PSCheckboxTreeRootNode());
      
      setPsSessionId(psSessionId);
      loadModel(baseURL, docUrlStr);
   }

   /**
    * Load the tree model.
    * 
    * @param baseURL the base URL of the applet, not <code>null</code>.
    * @param docUrlStr the URL string where the xml definition document is 
    *    found, not <code>null</code> or empty.
    */
   private void loadModel(URL baseURL, String docUrlStr)
   {
      if (baseURL == null)
         throw new IllegalArgumentException(
            "baseURL cannot be null");
      
      if (docUrlStr == null || docUrlStr.trim().length() == 0)
         throw new IllegalArgumentException(
            "docUrlStr cannot be null or empty");

      Document doc = null;


      try
      {
         URL docUrl = new URL(baseURL, docUrlStr);
         docUrl = appendPsSessionId(docUrl);

         URLConnection conn = docUrl.openConnection();
         try(InputStream is = conn.getInputStream()) {

            DocumentBuilder builder =
                    DocumentBuilderFactory.newInstance().newDocumentBuilder();
            doc = builder.parse(is);
            PSCheckboxTreeRootNode rootNode = (PSCheckboxTreeRootNode) getRoot();
            rootNode.loadDocument(doc);
         }
      }
      catch (Exception e)
      {
         throw new RuntimeException(e);
      }
   }

   /**
    * Append PS Session ID parameter to the query string of the specified URL.
    * Do nothing if the URL is used to request the PS Session ID.
    * This is needed for Firefox in the following scenario:
    * <UL>
    * <LI> Bring up Firefox, login CX
    * <LI> JSESSIONID and/or PS Session expired (due to either server 
    *      restart or session expired (more than 2 hours, for example)
    * <LI> Bring a new (after close down the current) Firefox session or refresh
    *      the same Firefox window, login CX, get new JSESSIONID (and 
    *      pssessionid)
    * <LI> perform some operation in CX (for example, bring up an Content Editor
    * <LI> Some of the request from java applet may not have the
    *      authenticated cookie, which is retained from step [3].
    * </UL>
    * <p>
    * Note, this will not be called when requesting the PS Session ID for the
    * current applet session.
    * 
    * @param url the URL in question, assumed not <code>null</code>.
    * 
    * @return the URL that has appended PS Session ID parameter. It can never 
    * be <code>null</code>.
    */
   private URL appendPsSessionId(URL url)
   {
      String sUrl = url.toString();
      if (url.toString().endsWith(GET_SESSIONID_URL))
         return url;

      if (sUrl.indexOf("pssessionid=") != -1)
         return url;
      
      if (sUrl.indexOf('?') == -1)
         sUrl = sUrl + "?pssessionid=" + getPsSessionId(url);
      else
         sUrl = sUrl + "&pssessionid=" + getPsSessionId(url);
      
      try
      {
         return new URL(sUrl);
      }
      catch (MalformedURLException e)
      {
         log.error(e.getMessage());
         log.debug(e.getMessage(), e);
         throw new RuntimeException(e);
      }
   }
   
   /**
    * Sets the PS Session ID if the specified session ID is not
    * <code>null</code> or empty.
    * 
    * @param psSessionId the possible new session ID. It may be
    * <code>null</code> or empty.
    */
   private void setPsSessionId(String psSessionId)
   {
      // set the PS Session ID if there is one.
      if (psSessionId != null && psSessionId.trim().length() > 0)
      {
         m_psSessionId = psSessionId;
         //System.out.println("Applet Param - PSSessionId: " + psSessionId);
      }
   }
   
   /**
    * Gets the PS Session ID of the current applet session.
    * 
    * @return the PS Session ID, never <code>null</code> or empty.
    */
   private String getPsSessionId(URL baseUrl)
   {
      if (m_psSessionId != null)
         return m_psSessionId;
      
      try
      {
         URL url = new URL(baseUrl, GET_SESSIONID_URL);
         String response = getResponse(url);
         // the response is in the following format:
         //
         //     var pssessionid = "30b30e2603585e0fc198aef6207d1c9d116226a3";
         int beginIndex = response.indexOf("\"");
         int lastIndex = response.lastIndexOf("\"");
         if (beginIndex >= lastIndex)
         {
            String errorMsg = "Failed to get pssessionid. The response data is: " + response;
            System.out.println(errorMsg);
            throw new RuntimeException(errorMsg);
         }

         String sId = response.substring(beginIndex+1, lastIndex);
         //System.out.println("Session ID: " + sId);
         m_psSessionId = sId;
         return m_psSessionId;
      }
      catch (Exception e)
      {
         throw new RuntimeException(e.toString());
      }
   }

   /**
    * Sends a request to server with the specified URL and return the
    * the response from byte array to string.
    * 
    * @param url the request URL, assumed not <code>null</code>.
    * 
    * @return the response of the request, never <code>null</code>.
    */
   private String getResponse(URL url)
   {


      try
      {
         URLConnection conn = url.openConnection();
         try(InputStream is = conn.getInputStream()) {
            byte[] buffer = new byte[1000];
            int byteRead = is.read(buffer);
            String response = new String(buffer, 0, byteRead);
            return response;
         }
      }
      catch (Exception e)
      {
         throw new RuntimeException(e);
      }
   }
   
   /**
    * The current PS Session ID. Default to <code>null</code> if has not been
    * set for current applet session.
    */
   private String m_psSessionId = null;
   
   /**
    * The URL used to retrieve the PS Session ID from server.
    */
   private static String GET_SESSIONID_URL = "../util/getPSSessionID.jsp";
   

   /**
    * Generated serial version id.
    */
   private static final long serialVersionUID = 6374466631198834418L;
}
