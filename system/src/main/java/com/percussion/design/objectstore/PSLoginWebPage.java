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

package com.percussion.design.objectstore;

import com.percussion.error.PSIllegalArgumentException;
import com.percussion.xml.PSXmlDocumentBuilder;
import com.percussion.xml.PSXmlTreeWalker;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.net.URL;
import java.util.List;

/**
 * The PSLoginWebPage class defines the page which will be returned
 * to the requestor when login is required. This can be for server or
 * application login.
 *
 * @author      Tas Giakouminakis
 * @version    1.0
 * @since      1.0
 */
public class PSLoginWebPage extends PSComponent
{
   /**
    * Construct a Java object from its XML representation. See the
    * {@link #toXml(Document) toXml} method for a description of the XML object.
    *
    * @param      sourceNode      the XML element node to construct this
    *                              object from
    *
    * @param      parentDoc      the Java object which is the parent of this
    *                              object
    *
    * @param      parentComponents   the parent objects of this object
    *
    * @exception   PSUnknownNodeTypeException
    *                              if the XML element node is not of the
    *                              appropriate type
    */
   public PSLoginWebPage(org.w3c.dom.Element sourceNode,
      IPSDocument parentDoc, List parentComponents)
      throws PSUnknownNodeTypeException
   {
      this();
      fromXml(sourceNode, parentDoc, parentComponents);
   }

   /**
    * Default construct for fromXml, serialization, etc.
    */
   PSLoginWebPage()
   {
      this(null, false);
   }

   /**
    * Construct a login web page object.
    *
    * @param   url         the URL of the login page
    *
    * @param   isSecure      <code>true</code> if the login page may only be
    *                        sent over secure channels (eg, HTTPS)
    */
   public PSLoginWebPage(
      java.net.URL url, boolean isSecure)
   {
      super();
      m_url = url;
      m_isSecure = isSecure;
   }

   /**
    * Is the login page only sent over secure channels?
    * <P>
    * <EM>Note:</EM> This does not mean the password will be sent using
    * secure channels. If the page uses a form which has a submit button
    * hardcoded to use a protocol such as HTTP, it will be sent unencrypted.
    *
    * @return               <code>true</code> if the login page may only be
    *                        sent over secure channels (eg, HTTPS)
    */
   public boolean isSecure()
   {
      return m_isSecure;
   }
   
   /**
    * Enable or disable sending the login page only over secure channels.
    * <P>
    * <EM>Note:</EM> This does not mean the password will be sent using
    * secure channels. If the page uses a form which has a submit button
    * hardcoded to use a protocol such as HTTP, it will be sent unencrypted.
    *
    * @param   isSecure      <code>true</code> if the login page may only be
    *                        sent over secure channels (eg, HTTPS)
    */
   public void setSecure(boolean isSecure)
   {
      m_isSecure = isSecure;
   }

   /**
    * Get the URL of the login page.
    *
    * @return         the URL of the login page
    */
   public java.net.URL getUrl()
   {
      return m_url;
   }
   
   /**
    * Set the URL of the login page.
    *
    * @param   url   the URL of the login page
    */
   public void setUrl(java.net.URL url)
   {
      m_url = url;
   }
   
   /**
    * Performs a shallow copy of the data in the supplied component to this
    * component. Derived classes should implement this method for their data,
    * calling the base class method first.
    *
    * @param  page a valid PSLoginWebPage. If null, a PSIllegalArgumentException is
    * thrown.
    *
    * @throws PSIllegalArgumentException if page is null
    */
   public void copyFrom( PSLoginWebPage page )
         throws PSIllegalArgumentException
   {
      copyFrom((PSComponent) page );
      // assume object is valid
      m_url = page.getUrl();
      m_isSecure = page.isSecure();
   }

   
   /* **************  IPSComponent Interface Implementation ************** */
   
   /**
    * This method is called to create a PSXLoginWebPage XML element
    * node containing the data described in this object.
    * <p>
    * The structure of the XML document is:
    * <pre><code>
    *    &lt;!--
    *       PSXLoginWebPage defines the page which will be returned to the
    *       requestor when login is required. This can be for server or
    *       application login.
    *    --&gt;
    *    &lt;!ELEMENT PSXLoginWebPage   (url)&gt;
    *
    *    &lt;!--
    *       secure - is the login page only sent over secure channels?
    *         default is no
    *    --&gt;
    *    &lt;!ENTITY % PSXLoginWebPageOutput   "(secure)"&gt;
    *    &lt;!ATTLIST PSXLoginWebPage
    *       secure      (yes|no)        #IMPLIED
    *    &gt;
    *
    *    &lt;!--
    *       the URL of the login page.
    *    --&gt;
    *    &lt;!ELEMENT url               (#PCDATA)&gt;
    * </code></pre>
    *
    * @return     the newly created PSXLoginWebPage XML element node
    */
   public Element toXml(Document doc)
   {
      Element root = doc.createElement(ms_NodeType);
      root.setAttribute("id", String.valueOf(m_id));
      
      root.setAttribute("secure", (m_isSecure ? "yes" : "no"));
      
      if (m_url == null)
         PSXmlDocumentBuilder.addElement(doc, root, "url", "");
      else
         PSXmlDocumentBuilder.addElement(doc, root, "url", m_url.toExternalForm());
      
      return root;
   }
   
   /**
    * This method is called to populate a PSLoginWebPage Java object
    * from a PSXLoginWebPage XML element node. See the
    * {@link #toXml(Document) toXml} method for a description of the XML object.
    *
    * @exception   PSUnknownNodeTypeException if the XML element node is not
    *                                        of type PSXLoginWebPage
    */
   public void fromXml(Element sourceNode, IPSDocument parentDoc, 
                        List parentComponents)
      throws PSUnknownNodeTypeException
   {
      if (sourceNode == null)
         throw new PSUnknownNodeTypeException(
            IPSObjectStoreErrors.XML_ELEMENT_NULL, ms_NodeType);
      
      if (!ms_NodeType.equals(sourceNode.getNodeName()))
      {
         Object[] args = { ms_NodeType, sourceNode.getNodeName() };
         throw new PSUnknownNodeTypeException(
            IPSObjectStoreErrors.XML_ELEMENT_WRONG_TYPE, args);
      }
      
      PSXmlTreeWalker   tree = new PSXmlTreeWalker(sourceNode);
      
      String sTemp = tree.getElementData("id");
      try {
         m_id = Integer.parseInt(sTemp);
      } catch (Exception e) {
         Object[] args = { ms_NodeType, ((sTemp == null) ? "null" : sTemp) };
         throw new PSUnknownNodeTypeException(
            IPSObjectStoreErrors.XML_ELEMENT_INVALID_ID, args);
      }
      
      sTemp = tree.getElementData("secure");
      m_isSecure = (sTemp != null) && sTemp.equalsIgnoreCase("yes");

      sTemp = tree.getElementData("url");
      if ((sTemp == null) || (sTemp.length() == 0)) {
         // this is no longer true. if no URL is specified, we use our
         // default login page
         // Object[] args = { ms_NodeType, "url", "" };
         // throw new PSUnknownNodeTypeException(
         //    IPSObjectStoreErrors.XML_ELEMENT_INVALID_CHILD, args);
         m_url = null;
      }
      else {
         try {
            m_url = new URL(sTemp);
         } catch(java.net.MalformedURLException e) {
            Object[] args = { ms_NodeType, "url",
                              "(URL: " + sTemp + ") " + e.getMessage() };
            throw new PSUnknownNodeTypeException(
               IPSObjectStoreErrors.XML_ELEMENT_INVALID_CHILD, args);
         }
      }
   }

   /**
    * Validates this object within the given validation context. The method
    * signature declares that it throws PSSystemValidationException, but the
    * implementation must not directly throw any exceptions. Instead, it
    * should register any errors with the validation context, which will
    * decide whether to throw the exception (in which case the implementation
    * of <CODE>validate</CODE> should not catch it unless it is to be
    * rethrown).
    * 
    * @param   cxt The validation context.
    * 
    * @throws PSSystemValidationException According to the implementation of the
    * validation context (on warnings and/or errors).
    */
   public void validate(IPSValidationContext cxt) throws PSSystemValidationException
   {
      if (!cxt.startValidation(this, null))
         return;

      // this is no longer true. if no URL is specified, we use our
      // default login page
      // if (m_url == null)
      //    cxt.validationError(this, IPSObjectStoreErrors.LOGIN_WEBPAGE_URL_EMPTY, null);
   }
   
   @Override
   public boolean equals(Object o)
   {
      if (!(o instanceof PSLoginWebPage))
         return false;

      PSLoginWebPage other = (PSLoginWebPage)o;

      if (m_isSecure != other.m_isSecure)
         return false;

      if (!compare(m_url, other.m_url))
         return false;

      return true;

   }
   
   
   @Override
   public int hashCode()
   {
      return Boolean.valueOf(isSecure()).hashCode()
            + (getUrl() == null ? 0 : getUrl().hashCode());
   }

   private java.net.URL                m_url;
   private boolean                    m_isSecure = false;
   
   /* package access on this so they may reference each other in fromXml */
   static final String   ms_NodeType            = "PSXLoginWebPage";
}

