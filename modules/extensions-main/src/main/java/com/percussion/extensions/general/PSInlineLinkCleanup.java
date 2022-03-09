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
package com.percussion.extensions.general;

import com.percussion.cms.PSInlineLinkField;
import com.percussion.cms.objectstore.PSInvalidContentTypeException;
import com.percussion.cms.objectstore.PSItemDefinition;
import com.percussion.cms.objectstore.server.PSItemDefManager;
import com.percussion.deployer.server.PSDbmsHelper;
import com.percussion.design.objectstore.IPSBackEndMapping;
import com.percussion.design.objectstore.PSBackEndColumn;
import com.percussion.design.objectstore.PSContainerLocator;
import com.percussion.design.objectstore.PSContentEditorMapper;
import com.percussion.design.objectstore.PSContentEditorPipe;
import com.percussion.design.objectstore.PSDisplayMapper;
import com.percussion.design.objectstore.PSDisplayMapping;
import com.percussion.design.objectstore.PSDisplayText;
import com.percussion.design.objectstore.PSField;
import com.percussion.design.objectstore.PSFieldSet;
import com.percussion.design.objectstore.PSLocator;
import com.percussion.design.objectstore.PSRelationshipSet;
import com.percussion.design.objectstore.PSUISet;
import com.percussion.error.PSDeployException;
import com.percussion.error.PSExceptionUtils;
import com.percussion.extension.IPSResultDocumentProcessor;
import com.percussion.extension.PSDefaultExtension;
import com.percussion.extension.PSExtensionProcessingException;
import com.percussion.extension.PSParameterMismatchException;
import com.percussion.server.IPSRequestContext;
import com.percussion.server.PSConsole;
import com.percussion.tablefactory.PSJdbcColumnData;
import com.percussion.tablefactory.PSJdbcFilterContainer;
import com.percussion.tablefactory.PSJdbcRowData;
import com.percussion.tablefactory.PSJdbcSelectFilter;
import com.percussion.tablefactory.PSJdbcTableData;
import com.percussion.tablefactory.PSJdbcTableSchema;
import com.percussion.util.IPSHtmlParameters;
import com.percussion.xml.PSXmlDocumentBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.sql.Types;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * This class is a Rhythmyx ResultDocumentProcessor extension.
 * It is used to cleanup the orphan inline links for all contents.
 * It is expecting one (option) parameter, which is either "preview" or 
 * "cleanup".
 * <p> 
 * If the option parameter is "preview", then it processes all 
 * items, report status to the console as well as to the trace if the trace 
 * is on, report the summary of the result at the end, but it does not write
 * to the database. 
 * <p>If the option parameter is not "preview", it will do the 
 * same as "preview" does, plus write to the database for the processed 
 * result.   
 */
public class PSInlineLinkCleanup
   extends PSDefaultExtension
   implements IPSResultDocumentProcessor
{
   private static final Logger log = LogManager.getLogger(PSInlineLinkCleanup.class);
   /**
    *
    * @param request the current request context
    *
    * @param resultDoc  the result document generated by the calling resource
    *
    * @return the modified result document
    *
    * @throws PSParameterMismatchException when the &lt;filelist&gt; is not
    * specified
    **/
   public Document processResultDocument(
      Object[] params,
      IPSRequestContext request,
      Document resultDoc)
      throws PSParameterMismatchException, PSExtensionProcessingException
   {
      if (params.length < 1
         || null == params[0]
         || params[0].toString().trim().length() == 0)
      {
         throw new PSParameterMismatchException(1, 0);
      }
      String command = params[0].toString();
      ProcessResultDoc(request, resultDoc, command);
      return resultDoc;
   }
   
   /**
    * This function is required by the IPSResultDocumentProcessor interface.
    * PSFormatFileTree will never modify the stylesheet.
    *
    * @return false
    *
    **/
   public boolean canModifyStyleSheet()
   {
      return false;
   }
   
   /**
    * ProcessResultDoc handles the entire result document
    *
    * @param resultDoc
    *
    * @param pt parameter tracker contains parameters for this request.
    **/
   private void ProcessResultDoc(
      IPSRequestContext request,
      Document resultDoc,
      String command)
   {
      long startTime = System.currentTimeMillis();
      Object[] args = {command};
      traceMessage(request, "Start the cleanup process, command=\"{0}\"", args);
      
      boolean isPreview = command.equalsIgnoreCase("preview");
      
      try
      {
         ContentType[] ctypes = getInlineLinkFields();
         
         for (int i = 0; i < ctypes.length; i++)
         {
            ctypes[i].processAllContents(request, isPreview);
         }
         
         reportProcessedResult(request, resultDoc, ctypes);
      }
      catch (Exception e)
      {
         traceMessage(request, e);
         log.error(PSExceptionUtils.getMessageForLog(e));
         log.debug(PSExceptionUtils.getDebugMessageForLog(e));
      }
      
      args[0] = new Long((System.currentTimeMillis()-startTime)/1000);
      traceMessage(request, "Completed the cleanup process in {0} seconds", 
         args);
   }

   /**
    * Get inline link fields for all visible content types.
    * 
    * @return array of content types, never <code>null</code>.
    * 
    * @throws PSInvalidContentTypeException if encounter an invalid content
    *    type.
    */   
   private ContentType[] getInlineLinkFields() 
      throws PSInvalidContentTypeException
   {
      PSItemDefManager mgr = PSItemDefManager.getInstance();
      long[] ctypeIds = mgr.getContentTypeIds(PSItemDefManager.COMMUNITY_ANY);
      PSItemDefinition itemDef = null;
      PSContentEditorMapper mapper = null;
      PSContentEditorPipe pipe = null;
      PSDisplayMapper dispMapper = null;
      PSFieldSet fieldSet = null;
      PSContainerLocator clocator = null;
      ContentType[] ctypes = new ContentType[ctypeIds.length];
      for (int i = 0; i < ctypes.length; i++)
      {
         itemDef =
            mgr.getItemDef(ctypeIds[i], PSItemDefManager.COMMUNITY_ANY);
         ctypes[i] = new ContentType(itemDef);
         pipe = (PSContentEditorPipe) itemDef.getContentEditor().getPipe();
         mapper = pipe.getMapper();
         clocator = pipe.getLocator();
         dispMapper = mapper.getUIDefinition().getDisplayMapper();
         addInlineFields(dispMapper, mapper, ctypes[i]);
      }
      
      return ctypes;
   }
   
   /**
    * Recurse through each mapper within a mapping to look for inline link
    * fields and add them into the given list.
    *
    * @param dispMapper The display mapper, assumed to be <code>null</code>.
    * 
    * @param editMapper The editor mapper, assumed to be <code>null</code>.
    * 
    * @param inlineFields  This is used to collect all inline link fields,
    *       a set of <code>PSField</code> objects. Assume not <code>null</code>,
    *       it may be empty.
    */
   private void addInlineFields(
      PSDisplayMapper dispMapper,
      PSContentEditorMapper editMapper,
      ContentType ctype)
   {
      PSFieldSet fieldSet = editMapper.getFieldSet(dispMapper.getFieldSetRef());
      Iterator mappings = dispMapper.iterator();
      PSDisplayMapping mapping = null;
      PSUISet uiSet = null;
      PSDisplayText label = null;
      String text = null;
      if (mappings != null)
      {
         while (mappings.hasNext())
         {
            mapping = (PSDisplayMapping) mappings.next();
            PSDisplayMapper displayMapper = mapping.getDisplayMapper();
            if (displayMapper != null)
            {
               addInlineFields(displayMapper, editMapper, ctype);
            }
            Object o = fieldSet.get(mapping.getFieldRef());
            // check for sdmp fields in child fieldset
            if (o == null)
            {
               o =
                  fieldSet.getChildField(
                     mapping.getFieldRef(),
                     PSFieldSet.TYPE_MULTI_PROPERTY_SIMPLE_CHILD);
            }
            if (o != null && o instanceof PSField)
            {
               PSField field = (PSField) o;
               if (field.isLocalField())
                  ctype.setLocalTable(field);
               Object backendObj = field.getLocator();
               if (field.mayHaveInlineLinks()
                  && (backendObj instanceof PSBackEndColumn))
               {
                  ctype.addInlineField(field);
               }
            }
         }
      }
   }

   /**
    * Utility method, get the schema for the supplied (backend) table name.
    * 
    * @param table The table name, assume not <code>null</code>.
    * 
    * @return The schema object, never <code>null</code>.
    */
   private PSJdbcTableSchema getSchema(String table)
   {
      PSJdbcTableSchema schema = (PSJdbcTableSchema) ms_tableSchemas.get(table);
      if (schema == null)
      {
         try
         {
            schema = PSDbmsHelper.getInstance().catalogTable(table, false);
         }
         catch (PSDeployException e)
         {
            throw new RuntimeException(e);
         }
         ms_tableSchemas.put(table, schema);
      }
      return schema;
   }
   
   /**
    * Gets the backend table name from the supplied field.
    * 
    * @param field the field object, assume not <code>null</code>.
    * 
    * @return The table name, never <code>null</code>.
    */
   private String getTableFromField(PSField field)
   {
      IPSBackEndMapping columnLocator = field.getLocator();
      if (! (columnLocator instanceof PSBackEndColumn))
         throw new IllegalStateException("field.getLocator() must return " +
            "a PSBackEndColumn object");
         
      PSBackEndColumn backendColumn = (PSBackEndColumn) columnLocator;
      String column = backendColumn.getColumn();
      return backendColumn.getTable().getTable();
   }
   
   /**
    * Gets the backend column name of the supplied field.
    * 
    * @param field The field object, assume not <code>null</code>.
    * 
    * @return The column name, never <code>null</code>.
    */
   private String getColumnFromField(PSField field)
   {
      IPSBackEndMapping columnLocator = field.getLocator();
      if (! (columnLocator instanceof PSBackEndColumn))
         throw new IllegalStateException("field.getLocator() must return " +
            "a PSBackEndColumn object");
            
      PSBackEndColumn backendColumn = (PSBackEndColumn) field.getLocator();
      return backendColumn.getColumn();
   }
   
   /**
    * Writes the processed summary result into the supplied document.
    *
    * @param request the request object, assume not <code>null</code>.
    *   
    * @param resultDoc The returned document that will contain the
    *    summary of the processed result, assume not <code>null</code>.
    * 
    * @param ctypes All visible content types that contains the summary
    *    of the processed result, assume not <code>null</code>.
    */
   private void reportProcessedResult(
      IPSRequestContext request,
      Document resultDoc,
      ContentType[] ctypes)
   {
      Element root = resultDoc.createElement(XML_ROOT);
      int totalDeletes = 0;
      int totalModifies = 0;
      int totalItems = 0;
      
      for (int i=0; i<ctypes.length; i++)
      {
         ContentType ctype = ctypes[i];
         if (! ctype.hasInlineFields())
            continue;

         // create the content type summary         
         String contentType = ctype.m_itemdef.getName();
         Element ctElement = resultDoc.createElement("ContentType");
         ctElement.setAttribute("name", contentType);
         ctElement.setAttribute(
            "deletes",
            String.valueOf(ctype.m_totalDeletes));
         ctElement.setAttribute(
            "modifies",
            String.valueOf(ctype.m_totalModifies));
         ctElement.setAttribute(
            "processedItems",
            String.valueOf(ctype.m_totalProcessedItems));
            
         root.appendChild(ctElement);
         
         totalDeletes += ctype.m_totalDeletes;
         totalModifies += ctype.m_totalModifies;
         totalItems += ctype.m_totalProcessedItems;
      }
      
      root.setAttribute("deletes", String.valueOf(totalDeletes));
      root.setAttribute("modifies", String.valueOf(totalModifies));
      root.setAttribute("processedItems", String.valueOf(totalItems));
      
      PSXmlDocumentBuilder.replaceRoot(resultDoc, root);
      
      Object[] args = {PSXmlDocumentBuilder.toString(resultDoc)};
      traceMessage(request, "Cleanup inline link summary: {0}", args);
   }

   /**
    * Print the supplied message to trace and console
    *  
    * @param request The request, assume not <code>null</code>.
    * @param msg The trace message, assume not <code>null</code>.
    */   
   private void traceMessage(
      IPSRequestContext request,
      String msg,
      Object[] args)
   {
      if (args != null)
         msg = MessageFormat.format(msg, args);

      request.printTraceMessage(msg);
      PSConsole.printMsg("Cleanup inlinks", msg);
   }
   
   /**
    * Convenient method, calls {@link #traceMessage(IPSRequestContext, 
    * String, null)}.
    */
   private void traceMessage(IPSRequestContext request, String msg)
   {
      traceMessage(request, msg, null);
   }

   /**
    * Convenient method, calls {@link #traceMessage(IPSRequestContext, 
    * String, null)}.
    */
   private void traceMessage(IPSRequestContext request, Exception e)
   {
      Object[] args = {e.getLocalizedMessage()};
      traceMessage(request, "Caught exception: {0}", args);
   }

   /**
    * The root node name
    */   
   private static String XML_ROOT = "InlineLinkCleanup";

   /**
    * Inner class used to process an visible content type.
    */
   private class ContentType
   {
      /**
       * Constructs the object with the supplied item definition.
       * 
       * @param itemdef The item definition of the content type, assume not
       *    <code>null</code>.
       */
      private ContentType(PSItemDefinition itemdef)
      {
         m_itemdef = itemdef;
      }
      
      /**
       * Adds the given field to the list of inline link fields.
       * 
       * @param field The inline link field, asume not <code>null</code>.
       */
      private void addInlineField(PSField field)
      {
         m_inlineFields.add(field);
      }
      
      /**
       * Determines whether the current content type contains inline link fields
       * 
       * @return <code>true</code> if it contains inline link fields; 
       *    <code>false</code> otherwise.
       */
      private boolean hasInlineFields()
      {
         return (! m_inlineFields.isEmpty());
      }
      
      /**
       * Get all fields that may contain inline links.
       *  
       * @return A list over zero or more <code>PSField</code> objects, 
       *    never <code>null</code>, but may not empty.
       */
      private List getInlineFields()
      {
         return m_inlineFields;
      }
      
      /**
       * Sets the table name for the local fields of this content type.
       * 
       * @param field one of the local field, assume not <code>null</code>.
       */
      private void setLocalTable(PSField field)
      {
         if (m_localTable == null)
         {
            if (!field.isLocalField())
               throw new IllegalArgumentException("field must be a local field");
               
            m_localTable = getTableFromField(field);
         }
      }
      
      /**
       * Get the table name for the local fields of this content type. 
       * {@link setLocalTable(PSField)} must be called first. 
       * 
       * @return The table name of this content type, never <code>null</code>.
       */
      private String getLocalTable()
      {
         if (m_localTable == null)
            throw new IllegalStateException(
               "setLocalTable must be called first");
               
         return m_localTable;
      }
      
      /**
       * Get the locators for all contents in the current content type.
       * 
       * @param request The current request, assume not <code>null</code>.
       * 
       * @return A list over zero or more <code>PSLocator</code> objects,
       *    never <code>null</code>, but may be empty.
       * 
       * @throws Exception if an error occurs.
       */
      private List getContentLocators(IPSRequestContext request)
         throws Exception
      {
         List result = new ArrayList();
         String table = getLocalTable();
         PSDbmsHelper dbmsHelper = PSDbmsHelper.getInstance();  
         PSJdbcTableData data = dbmsHelper.catalogTableData(table, null, null);
         
         if (data != null && data.getRows().hasNext())
         {
            Iterator rows = data.getRows();
            while (rows.hasNext())
            {
               PSJdbcRowData row = (PSJdbcRowData) rows.next();
               int id = dbmsHelper.getColumnInt(table, CONTENTID, row);
               int rev = dbmsHelper.getColumnInt(table, REVISIONID, row);
               PSLocator locator = new PSLocator(id, rev);
               result.add(locator);
            }
         }
         
         // trace message
         Object[] args = {new Integer(result.size()), table};
         traceMessage(request, "Retrieved {0} items from table \"{1}\"", args);
         
         return result;
      }
      
      /**
       * Processes all contents for the current content type.
       * 
       * @param request The request, assume not <code>null</code>.
       * @param isPreview <code>true</code> if the request is for preview only.
       */
      public void processAllContents(
         IPSRequestContext request,
         boolean isPreview)
      {
         Object[] args = {m_itemdef.getName()};
         traceMessage(request, "Processing content type: \"{0}\"", args);
         
         List result = new ArrayList();
         if (! hasInlineFields())
            return;
            
         try
         {
            // process one content type at a time
            Iterator locators = getContentLocators(request).iterator();
            while (locators.hasNext())
            {
               PSLocator locator = (PSLocator) locators.next();
               try
               {
                  processContents(request, locator, isPreview);
               }
               catch (Exception e)
               {
                  traceMessage(request, e);
                  log.error(PSExceptionUtils.getMessageForLog(e));
                  log.debug(PSExceptionUtils.getDebugMessageForLog(e));
                  
                  Object[] args1 =
                     {
                        new Integer(locator.getId()),
                        new Integer(locator.getRevision())};

                  traceMessage(
                     request,
                     "Failed process item (id={0}, rev={1}).",
                     args1);
               }
               m_totalProcessedItems++;
               
            }
         }
         catch (Exception e)
         {
            traceMessage(request, e);
            log.error(PSExceptionUtils.getMessageForLog(e));
            log.debug(PSExceptionUtils.getDebugMessageForLog(e));
         }
      }
      
      /**
       * Processes the content that is contained by the specified locator.
       * 
       * @param request The request object, assume not <code>null</code>.
       * @param locator The locator of the to be processed content, assume not
       *    <code>null</code>.
       * @param isPreview <code>true</code> if the request is for preview only;
       *    no modification to the database.
       * 
       * @throws Exception if an error occurs.
       */
      private void processContents(
         IPSRequestContext request,
         PSLocator locator,
         boolean isPreview)
         throws Exception
      {
         FieldContent fieldContent = null;
         PSInlineLinkField inlineField = null;
         PSField field = null;
         
         // retrieve contents for all inline link fields
         Iterator fields = getInlineFields().iterator();
         List contents = new ArrayList();
         while (fields.hasNext())
         {
            field = (PSField) fields.next();
            contents.addAll(getContents(locator, field));
         }
         
         // pre-processing 
         request.setParameter(
            IPSHtmlParameters.SYS_CONTENTID,
            String.valueOf(locator.getId()));
         request.setParameter(
            IPSHtmlParameters.SYS_REVISION,
            String.valueOf(locator.getRevision()));
         PSRelationshipSet deletes =
            PSInlineLinkField.getInlineRelationships(request);
         PSRelationshipSet modifies = new PSRelationshipSet();
         Iterator fieldContents = contents.iterator();
         while (fieldContents.hasNext())
         {
            fieldContent = (FieldContent) fieldContents.next();
            fieldContent.preProcess(request, deletes, modifies);
         }
         
         // post-process          
         if ((! isPreview) && (deletes.size() > 0 || modifies.size() > 0))
         {
            fieldContents = contents.iterator();
            while (fieldContents.hasNext())
            {
               fieldContent = (FieldContent) fieldContents.next();
               fieldContent.postProcess(request);
            }
         
            PSInlineLinkField.postProcess(request, deletes, modifies);
         }
         m_totalDeletes += deletes.size();
         m_totalModifies += modifies.size();
         
         // trace message
         if (deletes.size() > 0 || modifies.size() > 0)
         {
            Object[] args = {
               String.valueOf(m_totalProcessedItems),
               String.valueOf(locator.getId()), 
               String.valueOf(locator.getRevision()), 
               String.valueOf(deletes.size()),
               String.valueOf(modifies.size())};
            traceMessage(request, "processed {0} item (id={1}, rev={2}), " +
               "deleted ({3}) and added/updated ({4}) relationships", args);
         }
      }
      
      /**
       * Get contents for the specified item locator and field
       * 
       * @param locator The item locator, assume not <code>null</code>.
       * @param field The field of the item, assume not <code>null</code>.
       * 
       * @return A list over <code>FieldContent</code> objects, never 
       *    <code>null</code>, but may be empty.
       * 
       * @throws Exception if an error occurs.
       */
      private List getContents(PSLocator locator, PSField field)
         throws Exception
      {
         List result = new ArrayList();
         String column = getColumnFromField(field);
         String table = getTableFromField(field);
         PSJdbcTableSchema schema = getSchema(table);
         boolean hasSysid = false;
         if (schema.getColumn(SYSID) != null)
            hasSysid = true;
            
         // retrieve data
         PSDbmsHelper dbmsHelper = PSDbmsHelper.getInstance();
         PSJdbcSelectFilter filter = getFilter(locator);
         PSJdbcTableData data =
            dbmsHelper.catalogTableData(table, null, filter);
         if (data != null && data.getRows().hasNext())
         {
            Iterator rows = data.getRows();
            FieldContent fieldContent;
            int sysid = -1;
            while (rows.hasNext())
            {
               PSJdbcRowData row = (PSJdbcRowData) rows.next();
               PSJdbcColumnData cdata= row.getColumn(column);
               String content = cdata.getValue();               
               if (hasSysid)
                  sysid = dbmsHelper.getColumnInt(table, SYSID, row);
               fieldContent = new FieldContent(field, content, locator, sysid);
               result.add(fieldContent);
            }
         }
         return result;
      }
      
      /**
       * Creates a filter for the supplied item locator. The created filter
       * is used for the where clause as (CONTENTID=id AND REVISIONID=rev) 
       * 
       * @param locator The item locator, assume not <code>null</code>.
       * 
       * @return The created filter, never <code>null</code>.
       */
      private PSJdbcSelectFilter getFilter(PSLocator locator)
      {
         // create where clause: "CONTENTID=# AND REVISIONID=#"
         PSJdbcSelectFilter idFilter =
            new PSJdbcSelectFilter(
               CONTENTID,
               PSJdbcSelectFilter.EQUALS,
               String.valueOf(locator.getId()),
               Types.INTEGER);
         PSJdbcSelectFilter revFilter =
            new PSJdbcSelectFilter(
               REVISIONID,
               PSJdbcSelectFilter.EQUALS,
               String.valueOf(locator.getRevision()),
               Types.INTEGER);
            
         PSJdbcFilterContainer filter = new PSJdbcFilterContainer();
         filter.doAND(idFilter);
         filter.doAND(revFilter);
      
         return filter;
      }
   
      /*
       * The item definition for the content type. Init by ctor, 
       * never <code>null</code>.
       */
      private PSItemDefinition m_itemdef;
      
      /*
       * A list of inline link fields, never <code>null</code>, may be empty.
       */
      private List m_inlineFields = new ArrayList();
      
      /**
       * The local (DB) table name for the current content type. Set by 
       * {@link setLocalTable(PSField)}, never <code>null</code> after that.
       */
      private String m_localTable = null;
      
      /**
       * The total deleted inline link relationships
       */
      private int m_totalDeletes = 0;
      
      /**
       * The total modified inline link relationships
       */
      private int m_totalModifies = 0;
      
      /**
       * The total item that have been processed.
       */
      private int m_totalProcessedItems = 0;
   }
   
   /**
    * The inner class is used to process an inline link field.
    */
   private class FieldContent
   {
      /**
       * Constructs a processed field.
       * 
       * @param field The to be processed field, assume not <code>null</code>.
       * @param value The value of the field, it may be <code>null</code>, 
       *    which will be treated as an empty string.
       * @param locator The locator of the processed item, assume not 
       *    <code>null</code>.
       * @param sysid The value from the SYSID column, it may be <code>-1</code>
       *    if there is no SYSID column for the table of the field.
       */
      FieldContent(
         PSField field,
         String value,
         PSLocator locator,
         int sysid)
      {
         m_field = field;
         m_value = (value == null) ? "" : value;
         m_locator = locator;
         m_sysid = sysid;
      }

      /**
       * Sets the field value.
       * 
       * @param value The new value of the field, it may be <code>null</code>.
       *    A <code>null</code> value will be treated as empty string.
       */      
      void setValue(String value)
      {
         if (value == null)
            value = "";
            
         m_value = value;
      }
      
      /**
       * Performs pre-process for the current content. It updates the
       * supplied deletes and modified list afterwards.
       * 
       * @param request The current request, assume not <code>null</code>.
       * @param deletes It contains to be deleted relationships, assume not 
       *    <code>null</code>.
       * @param modifies It contains to be modified (updated or inserted) 
       *    relationships, assume not <code>null</code>.
       * 
       * @throws Exception if an erorr occurs.
       */
      void preProcess(
         IPSRequestContext request,
         PSRelationshipSet deletes,
         PSRelationshipSet modifies)
         throws Exception
      {
         request.setParameter(m_field.getSubmitName(), m_value);
         request.setParameter(
            IPSHtmlParameters.SYS_CONTENTID,
            String.valueOf(m_locator.getId()));
         request.setParameter(
            IPSHtmlParameters.SYS_REVISION,
            String.valueOf(m_locator.getRevision()));
            
         m_inlineField = new PSInlineLinkField(m_field);
         m_inlineField.preProcess(request, deletes, modifies);
         
         // set the processed new value         
         m_value = request.getParameter(m_field.getSubmitName());
      }
      
      /**
       * Performs post process for this object. This can only be called after 
       * the {@link #preProcess(IPSRequestContext, PSRelationshipSet, 
       * PSRelationshipSet)} has been called.
       *  
       * @param request The current request, assume not <code>null</code>.
       * 
       * @throws Exception if an erorr occurs.
       */
      void postProcess(IPSRequestContext request)
         throws Exception
      {
         if (m_inlineField == null)
            throw new IllegalStateException(
            "The postProcess can only be called after calling the preProcess");
            
         String table = getTableFromField(m_field);
         String column = getColumnFromField(m_field);
         // creates the updated row
         List columns = new ArrayList();
         PSJdbcColumnData id =
            new PSJdbcColumnData(CONTENTID, String.valueOf(m_locator.getId()));
         columns.add(id);
         PSJdbcColumnData rev =
            new PSJdbcColumnData(
               REVISIONID,
               String.valueOf(m_locator.getRevision()));
         columns.add(rev);
         PSJdbcColumnData content = new PSJdbcColumnData(column, m_value);
         columns.add(content);
         if (m_sysid != -1)
         {
            PSJdbcColumnData sysid =
               new PSJdbcColumnData(SYSID, String.valueOf(m_sysid));
            columns.add(sysid);
         }
         PSJdbcRowData row =
            new PSJdbcRowData(columns.iterator(), PSJdbcRowData.ACTION_UPDATE);
         List rows = new ArrayList();
         rows.add(row);
         PSJdbcTableData data = new PSJdbcTableData(table, rows.iterator());
         data.setOnCreateOnly(false);
         PSDbmsHelper dbmsHelper = PSDbmsHelper.getInstance();
         PSJdbcTableSchema schema = getSchema(table);
         dbmsHelper.processTable(schema, data);
      }
      
      /**
       * The processed field, init by ctor, never <code>null</code> after that.
       */
      private PSField m_field;
      
      /**
       * The value of the field, which may be the original or updated value.
       * Initialized by ctor, never <code>null</code> after that.
       */
      private String m_value;
      
      /**
       * The locator of the processed item, init by ctor, never 
       * <code>null</code> after thatn.
       */
      private PSLocator m_locator;
      
      /**
       * See ctor for its description
       */
      private int m_sysid = -1;
      
      /**
       * The inline link field object, it is set by {@link preProcess()},
       * never <code>null</code> after that.
       */
      private PSInlineLinkField m_inlineField = null;
   }
   
   // constant strings for pre-defined column
   private static String CONTENTID = "CONTENTID";
   private static String REVISIONID = "REVISIONID";
   private static String SYSID = "SYSID";
   
   /**
    * It maps database's table name to its schema. The map key is the name of 
    * the table as a <code>String</code> object; the map value is the schema
    * of the table as a <code>PSJdbcTableSchema</code> object. 
    */
   private static Map ms_tableSchemas = new HashMap();
}
