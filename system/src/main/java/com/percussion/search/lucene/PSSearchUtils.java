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
package com.percussion.search.lucene;

import com.percussion.cms.objectstore.PSInvalidContentTypeException;
import com.percussion.cms.objectstore.PSItemDefinition;
import com.percussion.cms.objectstore.server.PSItemDefManager;
import com.percussion.design.objectstore.PSContentEditorMapper;
import com.percussion.design.objectstore.PSField;
import com.percussion.design.objectstore.PSFieldSet;
import com.percussion.search.PSSearchKey;
import org.apache.commons.lang.StringUtils;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;


/**
 * A collection of generically useful methods that are specific to the RW 
 * search engine implementation.
 *
 * @author paulhoward
 */
public class PSSearchUtils
{

   /**
    * A utility method to get the node text. Gets the text from the nodes
    * recursively and appends the data to the supplied StringBuilder.
    * 
    * @param sb Object of StringBuilder must not be <code>null</code>.
    * @param node Node from which text needs to be extracted. Must not be
    * <code>null</code>.
    */
   public static void getNodeText(StringBuilder sb, Node node)
   {
      if(sb == null)
         throw new IllegalArgumentException("sb must not be null");
      if(node == null)
         throw new IllegalArgumentException("node must not be null");
         
      if (node.getNodeType() == Node.TEXT_NODE)
      {
         if (!StringUtils.isBlank(node.getNodeValue()))
            sb.append(StringUtils.trim(node.getNodeValue()) + " ");
      }
      NodeList children = node.getChildNodes();
      if (children != null)
      {
         int len = children.getLength();
         for (int i = 0; i < len; i++)
         {
            getNodeText(sb, children.item(i));
         }
      }
   }
   
   /**
    * Find all fields for a parent or one of its complex children. These 
    * include sdmp and mdsp, but not complex child fields. The field list is
    * based on the most recent item def passed to the search admin handler for
    * updating.
    * <p>Assumes the search engine has already been initialized.
    * 
    * @param key Never <code>null</code>. Content type id and child
    * content type id (if present) expected to be numeric values.
    * 
    * @return An iteration over 1 or more PSFields. If a child id is present 
    * in the key, then the fields for that child are returned, otherwise the 
    * fields for the main item are returned.
    * @throws PSInvalidContentTypeException 
    */
   public static Iterator getFields(PSSearchKey key)
      throws PSInvalidContentTypeException
   {
      if (null == key)
      {
         throw new IllegalArgumentException("key cannot be null");
      }
      try
      {
         PSItemDefinition def = PSItemDefManager.getInstance().getItemDef(
               key.getContentTypeKey().getPartAsInt(), 
               PSItemDefManager.COMMUNITY_ANY);
         Iterator results;
         if (null != key.getChildId())
         {
            int id = Integer.parseInt(key.getChildId().getChildContentType());
            results = def.getChildFields(id);
         }
         else
            results = def.getParentFields();
         return results;
      }
      catch (NumberFormatException nfe)
      {
         //should not happen
         throw new RuntimeException(
               "Expected numeric value for child mapper id, but got :" 
               + key.getChildId().getChildContentType());
      } 
   }
   
   /**
    * Find all fields for a given item definition which are searchable.
    * 
    * @param itemDef the item definition.  Never <code>null</code>.
    * 
    * @return A collection of PSFields.  Never <code>null</code>, may be empty.
    */
   public static Collection<PSField> getSearchableFields(PSItemDefinition itemDef)
   {
      if (itemDef == null)
      {
         throw new IllegalArgumentException("itemDef may not be null");
      }
      
      Collection<PSField> searchableFields = new HashSet<PSField>();
      
      PSContentEditorMapper mapper = itemDef.getContentEditorMapper();
      PSFieldSet fieldSet = mapper.getFieldSet();
      PSField[] fields = fieldSet.getAllFields();
      for (PSField field : fields)
      {
         if (field.isUserSearchable())
         {
            searchableFields.add(field);
         }
      }
      
      return searchableFields;
   }

   /**
    * A convenient method to find the Java Locale object corresponding to the
    * supplied languageString  .Defaults to US English if not supplied.
    *
    * @param languageString in the form of two letter language code hyphen two
    * letter country code for which the Java Locale needs to be found.
    * @return Locale object corresponding to the supplied language string or
    * <code>US English</code> if not found.
    */
   public static Locale getJavaLocale(String languageString)
   {
      if (StringUtils.isEmpty(languageString))
         languageString = "en-us";

      String[] strSplit = languageString.split("-");
      String langStr = strSplit[0];
      String cntStr = strSplit.length >= 2 ? strSplit[1] : "";
      Locale[] locales = Locale.getAvailableLocales();
      Locale defaultLocale = Locale.getDefault();
      for (Locale loc : locales)
      {
         if (loc.getLanguage().equalsIgnoreCase(langStr)
                 && loc.getCountry().equalsIgnoreCase(cntStr))
            return loc;
      }

      return defaultLocale;
   }
}
