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
package com.percussion.cms.handlers;

import com.percussion.cms.IPSCmsErrors;
import com.percussion.cms.PSCmsException;
import com.percussion.cms.objectstore.PSComponentSummary;
import com.percussion.cms.objectstore.PSRelationshipFilter;
import com.percussion.cms.objectstore.server.PSCloneFactory;
import com.percussion.cms.objectstore.server.PSInlineLinkProcessor;
import com.percussion.cms.objectstore.server.PSRelationshipProcessor;
import com.percussion.data.PSDataExtractionException;
import com.percussion.data.PSExecutionData;
import com.percussion.data.PSInternalRequestCallException;
import com.percussion.design.objectstore.IPSObjectStoreErrors;
import com.percussion.design.objectstore.PSLocator;
import com.percussion.design.objectstore.PSObjectException;
import com.percussion.design.objectstore.PSProcessCheck;
import com.percussion.design.objectstore.PSRelationship;
import com.percussion.design.objectstore.PSRelationshipConfig;
import com.percussion.design.objectstore.PSRelationshipSet;
import com.percussion.design.objectstore.PSSystemValidationException;
import com.percussion.design.objectstore.PSUnknownNodeTypeException;
import com.percussion.error.PSException;
import com.percussion.error.PSExceptionUtils;
import com.percussion.error.PSNotFoundException;
import com.percussion.error.PSRelationshipException;
import com.percussion.extension.PSExtensionException;
import com.percussion.relationship.IPSExecutionContext;
import com.percussion.relationship.PSCloneAlreadyExistsException;
import com.percussion.relationship.PSCloneLocator;
import com.percussion.relationship.PSExecutionContext;
import com.percussion.relationship.effect.PSEffectTestRunner;
import com.percussion.security.PSAuthenticationFailedException;
import com.percussion.security.PSAuthorizationException;
import com.percussion.server.IPSRequestContext;
import com.percussion.server.PSRequest;
import com.percussion.server.PSRequestContext;
import com.percussion.server.PSRequestValidationException;
import com.percussion.server.PSServer;
import com.percussion.server.config.PSServerConfigException;
import com.percussion.server.webservices.PSServerFolderProcessor;
import com.percussion.services.guidmgr.IPSGuidManager;
import com.percussion.services.guidmgr.PSGuidManagerLocator;
import com.percussion.services.purge.IPSSqlPurgeHelper;
import com.percussion.services.purge.PSSqlPurgeHelperLocator;
import com.percussion.services.sitemgr.IPSSite;
import com.percussion.services.sitemgr.IPSSiteManager;
import com.percussion.services.sitemgr.PSSiteManagerException;
import com.percussion.services.sitemgr.PSSiteManagerLocator;
import com.percussion.share.service.exception.PSValidationException;
import com.percussion.util.IPSHtmlParameters;
import com.percussion.util.PSCms;
import com.percussion.util.PSRelationshipUtils;
import com.percussion.utils.guid.IPSGuid;
import com.percussion.utils.types.PSPair;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.Serializable;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Creates clones based on conditions specified in a clone handler
 * configuration.
 */
public class PSConditionalCloneHandler extends PSCloneHandler
{
   /**
    * Constructs a new clone handler for the supplied copy handler. See base
    * class for additional description.
    */
   public PSConditionalCloneHandler(IPSCopyHandler copyHandler)
      throws PSServerConfigException, PSUnknownNodeTypeException
   {
      super(copyHandler);
   }

   /** see IPSCloneHandler for description */
   @SuppressWarnings("unchecked")
   public PSLocator clone(PSLocator source, Iterator relationships,
      PSExecutionData data, PSCommandHandler ch,
      IPSRelationshipHandlerCallback cb)
      throws SQLException, PSObjectException, IOException
   {
      if (source == null || relationships == null || data == null)
         throw new IllegalArgumentException(
            "source, relationship and data cannot be null");

      try
      {
         PSRequest request = data.getRequest();

         String relationshipType = request.getParameter(
            IPSHtmlParameters.SYS_RELATIONSHIPTYPE);

         PSRelationshipConfig requestedConfig = PSRelationshipCommandHandler
            .getRelationshipConfig(relationshipType);

         PSLocator clone = null;
         boolean cloneExists = false;
         try
         {
            PSExecutionContext execCxt = new PSExecutionContext(
               IPSExecutionContext.RS_PRE_CLONE, data);
            PSEffectTestRunner.run(execCxt, requestedConfig.getEffects(), data);
         }
         catch (PSCloneAlreadyExistsException ce)
         {
            cloneExists = true;
            clone = new PSCloneLocator(ce.getDependent(), true);
            data.setOriginatingRelationship(new PSRelationship(-1,
               (PSLocator) source.clone(), (PSLocator) clone.clone(),
               requestedConfig));
         }
         log.debug("Creating {}, for the item with content id = {}, and revision = {} ", relationshipType, source.getId(), source.getRevision());


         /*
          * No clone exists based on the user effect(s) attached to the 
          * relationship type, create clone now.
          */
         Map childRowMappings = new HashMap();
         Iterator<PSRelationship> dedupedRels = relationships;
         if (!cloneExists)
         {
            // see if the target folder for creating the item is present
            String folderid = request.getParameter(
               IPSHtmlParameters.SYS_FOLDERID, null);
            if (folderid != null && folderid.trim().length() > 0)
            {
                if (!PSCms.canWriteToFolders(new PSRequestContext(
                   data.getRequest())))
                {
                    // user must have write access
                    throw new PSAuthorizationException(
                       IPSCmsErrors.FOLDER_PERMISSION_DENIED, new String[]{});
                }
            }
            
            if (!canClone(data))
            {
               // the current object does not allow cloning
               throw new PSObjectException(
                  IPSObjectStoreErrors.OBJECT_CLONING_NOT_ALLOWED, "item");
            }

            /*
             * Create the new relationship and add it to the execution data so
             * that it is available for conditionals and effects. Effects can
             * change this before it is persisted at the end of the recursion.
             * Note: the supplied locator is just a dummy placeholder which is
             * never used.
             */
            PSRelationship relationship = new PSRelationship(-1,
               (PSLocator) source.clone(), new PSLocator(Integer.MAX_VALUE), 
               requestedConfig);

            data.setOriginatingRelationship(relationship);
            
            clone = PSCloneFactory.createClone(request, source, 
               childRowMappings);

            // update the list of handled objects
            request.addClone(source.getId(), clone);
         }
         else {
            /*
             * In order to prevent creating duplicated (AA) relationships (fix
             * (both RX-12522 and RX-16302) while deep clone AA relationships
             * during "create translation" process, we decided to simply
             * stop the deep clone processing at all already cloned items.
             * For example,
             *   Pre-condition
             *      A -> B
             *      A -> C
             *   Translate and deep clone A, we got
             *        A   ->   A'
             *       / \      / \
             *      B   C    B'  C'
             *      (where B -> B'  and  C -> C')
             *   Add "E" to "A", create "D" and add "A" into a slot in "D"
             *      D ->   A      ->   A'
             *           /  / \       / \
             *          E  B  C      B' C'
             *      (where B -> B'  and  C -> C')
             *   Translate "D", we should gotten:
             *             D      ->   D'
             *             |           |
             *             A      ->   A'
             *           /  / \       / \
             *          E  B  C      B' C'
             *       (where B -> B'  and  C -> C')
             *       NOTE, there is no E' created in the above last step
             *             because "A'" already exists.
             *
             *   Note, we reverted changes for Rx-12522 here.
             */


            dedupedRels = Collections.EMPTY_LIST.iterator();
         }


//         // update the list of handled objects
//         request.addClone(source.getId(), clone);

         /*
          * We need to save the request parameters before we start recreating
          * the relationships. The request parameters will be restored with
          * this once the relationships are processed.
          */
         Map<String, Object> paramsBackup = request.getParameters();

         try
         {
            List psRelationships = new ArrayList();

            /*  Sort
             * RX-16430,RX-13380 Create Translation - Fails With Errors and Leaves Orphaned English Locale Content
             */
            ArrayList<PSRelationship> sortedRels = new ArrayList<PSRelationship>();
            while (dedupedRels.hasNext())
            {
               sortedRels.add(dedupedRels.next());
            }

            Collections.sort(sortedRels,FOLDER_REL_SORT);


            /*
             * Walk all relationships provided and recreate them base on the
             * configuration settings.
             */
            for (PSRelationship relationship : sortedRels)
            {
               // first check if the relationship allows cloning
               if (!relationship.getConfig().isCloningAllowed())
                  continue;
   
               PSRelationshipConfig config = relationship.getConfig();
   
               /*
                * We create new parameter map from the original and then 
                * override the existing parameter values so that we propagate 
                * all other parameters.
                */
               HashMap params = new HashMap(paramsBackup);
               request.setParameters(params);
               params.put(IPSHtmlParameters.SYS_COMMAND,
                  PSRelationshipCommandHandler.COMMAND_NAME + "/" +
                  PSRelationshipCommandHandler.COMMAND_INSERT);
               params.put(IPSHtmlParameters.SYS_CONTENTID,
                  Integer.toString(relationship.getDependent().getId()));
   
               PSExecutionData tempData = null;
               try
               {
                  if (allowsShallowCloning(relationship.getConfig(), data))
                  {
                     psRelationships.add(new ProcessedRelationship(relationship,
                        true));
      
                     // prepare the request to create a shallow clone
                     addPropertyParameters(params, relationship);
      
                     params.put(IPSHtmlParameters.SYS_RELATIONSHIPTYPE,
                        config.getName());
                     params.put(IPSHtmlParameters.SYS_CONTENTID,
                        Integer.toString(clone.getId()));
                     if (relationship.getConfig().useOwnerRevision())
                        params.put(IPSHtmlParameters.SYS_REVISION,
                           Integer.toString(clone.getRevision()));
                     params.put(IPSHtmlParameters.SYS_DEPENDENTID,
                        Integer.toString(relationship.getDependent().getId()));
                     if (relationship.getConfig().useDependentRevision())
                        params.put(IPSHtmlParameters.SYS_DEPENDENTREVISION,
                           Integer.toString(
                              relationship.getDependent().getRevision()));
      
                     // call the relationship command handler to do this for us
                     tempData = ch.makeInternalRequest(request);
                  }
                  else if (allowsDeepCloning(relationship.getConfig(), data))
                  {
                     psRelationships.add(new ProcessedRelationship(relationship,
                        false));
      
                     // prepare the request to create a deep clone
                     addPropertyParameters(params, requestedConfig);
      
                     params.put(IPSHtmlParameters.SYS_RELATIONSHIPTYPE,
                        relationshipType);
                     params.put(IPSHtmlParameters.SYS_CONTENTID,
                        Integer.toString(relationship.getDependent().getId()));
                     if (relationship.getDependent().useRevision())
                        params.put(IPSHtmlParameters.SYS_REVISION,
                           Integer.toString(
                              relationship.getDependent().getRevision()));
                     else
                     {
                        Object revision = null;
                        try
                        {
                           revision = m_currentRevisionExtractor.extract(data);
                        }
                        catch (PSDataExtractionException e)
                        {
                           // this should never happen
                           revision = "1";
                        }
      
                        params.put(IPSHtmlParameters.SYS_REVISION, 
                           revision.toString());
                     }
      
                     // call the relationship command handler to do this for us
                     tempData = ch.makeInternalRequest(request);

                     addPropertyParameters(params, relationship);
      
                     // and relate current and new clone if requested
                     if (cb != null)
                     {
                        PSLocator dependent = getDependent(request,
                           relationship.getConfig().useDependentRevision());
                        
                        // if folder relationship check if item is already related to a parent folder.
                        // if already related to clone then do nothing.  If related to other folder
                        // e.g. translation source then move.  This will happen if translating an ancestor
                        // of an already translated folder.  We want to move the folder to its newly translated parent.
                        if (config.getCategory().equals(
                              PSRelationshipConfig.CATEGORY_FOLDER)) {
                              preventDuplicateFolderRelationships(source, data, cb, clone, relationship, config,
                                    dependent);
                        } else {
                        cb.relate(config.getName(), clone, dependent, data);
                     }
                  }
                  }
                  else
                     params.put(IPSHtmlParameters.SYS_RELATIONSHIPTYPE,
                        relationshipType);
               }
               finally
               {
                  if (tempData != null)
                  {
                     tempData.release();
                     tempData = null;
                  }
               }
            }
   
            if (!cloneExists)
            {
               fixupRelationships(request, clone, psRelationships,
                     childRowMappings);
            }
         }
         finally
         {
            /*
             * Restore the request parameters and set contentid and revision 
             * from the new clone.
             */
            request.setParameters(paramsBackup);
            request.setParameter(IPSHtmlParameters.SYS_CONTENTID,
               Integer.toString(clone.getId()));
            request.setParameter(IPSHtmlParameters.SYS_REVISION,
               Integer.toString(clone.getRevision()));
         }

         return clone;
      }
      catch (PSException e)
      {

            Iterator<Serializable> clones = data.getRequest().getClones();
            List<PSLocator> cloneIds = new ArrayList<PSLocator>();
            log.error("Cloning failed rolling back clone items");
            IPSSqlPurgeHelper purgeHelper = PSSqlPurgeHelperLocator.getPurgeHelper();
            while (clones.hasNext())
            {
                PSLocator loc = (PSLocator) clones.next();
                cloneIds.add(loc);
            }
            try
            {
               purgeHelper.purgeAll(cloneIds);
            }
            catch (PSException | PSValidationException e1)
            {
                log.error("Failed to remove cloned items.  There will probably be orphaned items", e);
            }

         throw new PSObjectException(e.getErrorCode(), e.getErrorArguments());
      }
   }

   /**
    * When deep cloning folder relationships for translations we may find a folder and its translated folder.  We want to move the translated
    * folder to its new translated folder but prevent duplicate folder relationships being created.
    *
    * @param source
    * @param data
    * @param cb
    * @param clone
    * @param relationship
    * @param config
    * @param dependent
    * @throws PSCmsException
    * @throws PSRelationshipException
    */
   private void preventDuplicateFolderRelationships(PSLocator source, PSExecutionData data,
         IPSRelationshipHandlerCallback cb, PSLocator clone, PSRelationship relationship, PSRelationshipConfig config,
         PSLocator dependent) throws PSCmsException, PSRelationshipException, com.percussion.services.error.PSNotFoundException, PSRelationshipException {
      if (dependent.getId() != relationship.getDependent().getId())
      {
         PSServerFolderProcessor proc = PSServerFolderProcessor.getInstance();
         PSComponentSummary[] summaries = proc.getParentSummaries(dependent);
         boolean move = false;
         boolean foundExisting = false;
         List<Integer> removeRels = new ArrayList<Integer>();
         if (summaries.length > 0)
         {
            for (int i = 0; i < summaries.length; i++)
            {
               int id = summaries[i].getContentId();
               if (id == source.getId() && !foundExisting)
               {
                  move = true;
                  log.debug("Found existing relationship to source parent Item setting to move "
                        + dependent.getId());
               }
               else if (id == clone.getId())
               {
                  log.debug("Dependent " + dependent.getId() + " is already related to clone parent folder "
                        + clone.getId());
                  foundExisting = true;
                  if (move = true)
                  {
                     removeRels.add(source.getId());
                     move = false;
                  }
               }
               else
               {
                  log.debug("Dependent " + dependent.getId() + " is related to unknown parent folder " + id
                        + " removing relationship to clean up");
                  removeRels.add(id);
               }
            }
         }

         for (int removeRel : removeRels)
         {
            proc.removeChildren(new PSLocator(removeRel), Collections.singletonList(dependent));
         }

         if (move)
         {
            proc.move(PSRelationshipConfig.TYPE_FOLDER_CONTENT, source, Collections.singletonList(dependent), new PSLocator(clone.getId()));
         }
         else if (!foundExisting)
         {
            log.debug("Only found and cleaned up existing folder relationships creating new folder relationship");
            cb.relate(config.getName(), clone, dependent, data);
         }

      }
      else
      {
         log.debug("Folder relationshp clone same as source dependent not processing" + source.getId() + " : "
               + dependent.getId());
      }
   }

   /**
    * Initialize fix up relationships. This must be called before the cloning
    * processing started, then calls
    * {@link #fixupRelationships(IPSRequestContext)} to fix up the created
    * relationships.
    * 
    * @param reqCtx the request context, used to set private objects, not
    * <code>null</code>.
    */
   public static void initFixupRelationships(IPSRequestContext reqCtx)
   {
      if (reqCtx == null)
         throw new IllegalArgumentException("reqCtx must not be null.");
      
      reqCtx.setPrivateObject(PSConditionalCloneHandler.FIXUP_RELS,
            new ArrayList<FixupRelationships>());
   }
   
   /**
    * Fixes the relationships that are created in the cloning process. This 
    * must be called after the end of cloning processing. Must call
    * {@link #initFixupRelationships(IPSRequestContext)} before the cloning
    * processing.
    * 
    * 
    * @param reqCtx the request context, which contains the relationships
    * in question. It may not be <code>null</code>.
    * 
    * @throws PSException if an error occurs.
    */
   @SuppressWarnings("unchecked")
   public static void fixupRelationships(IPSRequestContext reqCtx) throws PSException
   {
      if (reqCtx == null)
         throw new IllegalArgumentException("reqCtx must not be null.");

      PSRequest request = PSServer.getRequest(reqCtx);
      List<FixupRelationships> fixupRels = (List<FixupRelationships>) request
            .getPrivateObject(FIXUP_RELS);
      for (FixupRelationships fixup : fixupRels)
      {
         fixupSiteFolderIds(request, fixup.mi_relationshipMap);
         PSInlineLinkProcessor.processInlineLinkItem(request, fixup.mi_clone,
               fixup.mi_relationshipMap);
         fixupInlineRelationshipIds(request, fixup.mi_createdRels.iterator(),
               fixup.mi_psRelationships, fixup.mi_childRowMappings);
      }
   }
   
   /**
    * This container class contains all necessary information for fixing up
    * site/folder IDs, inline links, ...etc for the stored relationships. 
    */
   private class FixupRelationships
   {
      /**
       * The cloned items
       */
      PSLocator mi_clone;
      
      /**
       * The original relationships that were cloned from.
       */
      List<ProcessedRelationship> mi_psRelationships;
      
      /**
       * The created/cloned relationships
       */
      List<PSRelationship> mi_createdRels;
      
      /**
       * It maps original relationship Id to the created/cloned relationship.
       */
      Map<Integer, PSRelationship> mi_relationshipMap;

      /**
       * a map of inline relationship id's created during this request, assumed
       * not <code>null</code>, may be empty. See
       * {@link PSCloneFactory#CHILD_ROW_MAPPINGS_PRIVATE_OBJECT} for details of
       * the map.
       */
      Map mi_childRowMappings;
   }
   
   /**
    * Fixes up the created relationships, which is stored in
    * {@link PSRequest#getRelationships()}. However, it skips fixup process,
    * but only collects the fixing up information if
    * {@link PSConditionalCloneHandler#initFixupRelationships(IPSRequestContext)}
    * is called.
    * 
    * @param request the request contains the created relationships, assumed not
    * <code>null</code>.
    * @param clone the locator of the cloned item, assumed not <code>null</code>.
    * @param psRelationships the original relationships that were used to
    * create/clone the new relationships, assumed not <code>null</code>.
    * @param childRowMappings a map of inline relationship id's created during
    * this request, assumed not <code>null</code>, may be empty. See
    * {@link PSCloneFactory#CHILD_ROW_MAPPINGS_PRIVATE_OBJECT} for details of
    * the map.
    * 
    * @throws PSException if an error occurs.
    */
   @SuppressWarnings("unchecked")
   private void fixupRelationships(PSRequest request, PSLocator clone,
         List<ProcessedRelationship> psRelationships, Map childRowMappings) throws PSException
   {
      List<PSRelationship> createdRels = new ArrayList<>();
      CollectionUtils.addAll(createdRels, request.getRelationships());
      psRelationships = removeFolderRelationships(psRelationships);
      if (psRelationships.isEmpty())
         return;
      
      Map<Integer, PSRelationship> relationshipMap = getRelationshipMap(request,
            createdRels.iterator(), psRelationships);
      
      List<FixupRelationships> fixupRels = (List<FixupRelationships>) request
            .getPrivateObject(FIXUP_RELS);
      if (fixupRels != null)
      {
         FixupRelationships fixup = new FixupRelationships();
         fixup.mi_clone = (PSLocator) clone.clone();
         fixup.mi_psRelationships = psRelationships;
         fixup.mi_childRowMappings = childRowMappings;
         fixup.mi_relationshipMap = relationshipMap;
         fixup.mi_createdRels = createdRels;
         fixupRels.add(fixup);         
      }
      else
      {
         fixupSiteFolderIds(request, relationshipMap);
         PSInlineLinkProcessor.processInlineLinkItem(request, clone,
               relationshipMap);
         fixupInlineRelationshipIds(request, createdRels.iterator(),
               psRelationships, childRowMappings);
      }
   }
   
   /**
    * Removes the folder relationships from the supplied relationships.
    * 
    * @param psRelationships the relationships in question, assumed not
    * <code>null</code>.
    * 
    * @return the relationships that do not contain folder relationships,
    * never <code>null</code>, but may be empty.
    */
   private List<ProcessedRelationship> removeFolderRelationships(
         List<ProcessedRelationship> psRelationships)
   {
      List<ProcessedRelationship> result = new ArrayList<>();
      for (ProcessedRelationship r : psRelationships)
      {
         if (r.m_relationship.getConfig().getCategory().equals(
               PSRelationshipConfig.CATEGORY_FOLDER))
            continue;

         result.add(r);
      }

      return result;
   }
   
   /**
    * Fixes up the site and/or folder IDs for the given relationships.
    * 
    * @param req the request, assumed not <code>null</code>.
    * @param relMap the map that maps the original relationship ID to the cloned
    * relationships, assumed not <code>null</code>.
    * 
    * @throws PSException if an error occurs.
    */
   private static void fixupSiteFolderIds(PSRequest req,
         Map<Integer, PSRelationship> relMap) throws PSException
   {
      PSRelationshipSet rels = new PSRelationshipSet();
      PSRelationshipProcessor processor = PSRelationshipProcessor.getInstance();
      for (Integer rid : relMap.keySet())
      {
         PSRelationship rel = relMap.get(rid);
         rel = updateSiteFolderId(req, rel, processor);
         if (rel != null)
         {
            rels.add(rel);
            relMap.put(rid, rel);
         }
      }
      if (!rels.isEmpty())
      {
         processor.save(rels);
      }
   }

   /**
    * Updates the site and/or folder IDs for the given relationship if the
    * relationship contains either site or folder IDs.
    * 
    * @param req the request, assumed not <code>null</code>.
    * @param rel the relationship in question, assumed not <code>null</code>.
    * @param processor the relationship processor, assumed not <code>null</code>.
    * 
    * @return the updated relationship. It may be <code>null</code> if the
    * relationship does not need to be updated. 
    * 
    * @throws PSException if an error occurs.
    */
   private static PSRelationship updateSiteFolderId(PSRequest req,
         PSRelationship rel, 
         PSRelationshipProcessor processor) throws PSException
   {
      Map<String, String> relProps = rel.getAllProperties();
      
      boolean hasSiteId = StringUtils.isNotBlank(relProps
            .get(PSRelationshipConfig.PDU_SITEID));
      boolean hasFolderId = StringUtils.isNotBlank(relProps
            .get(PSRelationshipConfig.PDU_FOLDERID));
      if ((!hasSiteId) && (!hasFolderId))
      {
         return null;
      }

      // reload the relationship to keep it in sync with repository;
      // otherwise owner_revision may be -1
      PSRelationshipFilter filter = new PSRelationshipFilter();
      filter.setRelationshipId(rel.getId());
      List<PSRelationship> rels = processor.getRelationshipList(filter);
      if (rels.size() != 1)
      {
         throw new IllegalStateException("Cannot find relationship ID = "
               + rel.getId());
      }
      rel = rels.get(0);
      String siteId = "";
      String folderId = "";
      PSPair<Integer, Integer> ids = getSiteFolderIds(rel.getDependent(), rel,
            req);
      if (ids != null && ids.getFirst() != null )
         siteId = String.valueOf(ids.getFirst());
      if (ids != null && ids.getSecond() != null )
         folderId = String.valueOf(ids.getSecond());

      if (hasSiteId)
         rel.setProperty(PSRelationshipConfig.PDU_SITEID, siteId);
      if (hasFolderId)
         rel.setProperty(PSRelationshipConfig.PDU_FOLDERID, folderId);
      
      return rel;
   }

   /**
    * Gets the site and folder IDs for the given item.
    * 
    * @param loc the locator of the item, which is also the dependent of the
    *    given relationship, assumed not <code>null</code>.
    * @param rel the relationship that contains site and/or folder ID
    *    properties, assumed not <code>null</code>.
    * @param req the request, assumed not <code>null</code>.
    * 
    * @return a pair IDs, where the 1st is site ID, 2nd is folder ID. It may be
    * <code>null</code> if there is no site and folder IDs for this item; or
    * one of the pair is <code>null</code> if there is only folder ID, but no
    * site ID.
    */
   private static PSPair<Integer, Integer> getSiteFolderIds(PSLocator loc,
         PSRelationship rel, PSRequest req)
   {
      try
      {
         // get the original site and/or folder IDs if there is any
         String sSiteId = rel.getProperty(PSRelationshipConfig.PDU_SITEID);
         String sFolderId = rel.getProperty(PSRelationshipConfig.PDU_FOLDERID);
         Integer origSiteId = StringUtils.isBlank(sSiteId) ? null
               : new Integer(sSiteId);
         Integer origFolderId = StringUtils.isBlank(sFolderId) ? null
               : new Integer(sFolderId);
         
         IPSSiteManager smgr = PSSiteManagerLocator.getSiteManager();
         IPSGuidManager mgr = PSGuidManagerLocator.getGuidMgr();
         IPSGuid guid = mgr.makeGuid(loc);
         List<IPSSite> itemSites = smgr.getItemSites(guid);

         // are there both site & folder IDs?
         if (!itemSites.isEmpty())
         {
            return selectSiteFolderIds(itemSites, guid, origSiteId, smgr);
         }

         // is there folder path
         PSServerFolderProcessor proc = PSServerFolderProcessor.getInstance();
         String[] paths = proc.getFolderPaths(loc);
         if (paths.length == 0)
            return null; // no site & folder IDs

         // folder ID only, pick the original folder ID if possible
         if (origFolderId == null)
         {
            int folderId = proc.getIdByPath(paths[0]);
            return new PSPair<>(null, folderId);
         }
            
         Integer folderId = null;
         for (String path : paths)
         {
            folderId = proc.getIdByPath(path);
            if (folderId == origFolderId.intValue())
               break;
         }
         return new PSPair<>(null, folderId);
      }
      catch (Exception e)
      {

         log.error("Failed to get site and folder IDs for item: {}, Error: {}", loc.toString(),PSExceptionUtils.getMessageForLog(e));
         log.debug(PSExceptionUtils.getDebugMessageForLog(e));

         throw new RuntimeException(e);
      }
   }
   
   /**
    * Selects the site and/or folder IDs from the given sites, which the given
    * item resides in.
    * 
    * @param sites the list of sites to select from, assumed not
    * <code>null</code> or empty.
    * @param itemId the item ID, assumed not <code>null</code>.
    * @param origSiteId the original site ID, it may be <code>null</code>.
    * @param smgr the site manager service, assumed not <code>null</code>.
    * 
    * @return a pair IDs, where the 1st is site ID, 2nd is folder ID. It never
    * <code>null</code>.
    * 
    * @throws PSSiteManagerException if error occurs.
    */
   private static PSPair<Integer, Integer> selectSiteFolderIds(
         List<IPSSite> sites, IPSGuid itemId, Integer origSiteId,
         IPSSiteManager smgr)
           throws PSSiteManagerException, com.percussion.services.error.PSNotFoundException {
      if (origSiteId != null)
      {
         // use the original site ID if it is one of the sites
         for (IPSSite s : sites)
         {
            if (s.getGUID().getUUID() == origSiteId)
            {
               IPSGuid folderId = smgr.getSiteFolderId(s.getGUID(), itemId);
               return new PSPair<>(s.getGUID().getUUID(),
                     folderId.getUUID());
            }
         }
      }

      // just pick the 1st site (if the original site ID is not in the "sites"
      IPSGuid siteId = sites.get(0).getGUID();
      IPSGuid folderId = smgr.getSiteFolderId(siteId, itemId);
      return new PSPair<>(siteId.getUUID(), folderId
            .getUUID());
   }
   
   /**
    * Highly specialized comparison function. Compares for equality, but ignores
    * known properties that apply to the owner or the dependent, properties that
    * are related to persistent storage or folderId or siteId.
    * 
    * @param rel1 Assumed not <code>null</code>.
    * 
    * @param rel2 Assumed not <code>null</code>.
    * 
    * @return <code>true</code> if <code>rel1</code> is equal to
    * <code>rel2</code> sans owner, dependent, folderId and siteId, otherwise
    * <code>false</code>.
    */
   public boolean compareRelationships(PSRelationship rel1, PSRelationship rel2)
   {
      EqualsBuilder bldr = new EqualsBuilder()
         .append(rel1.getConfig().getName(), rel2.getConfig().getName());

      Map<String, String> userProps1 = rel1.getUserProperties();
      Map<String, String> tmpProps1 = new HashMap<>();
      for (String propName : userProps1.keySet())
      {
         if (!ms_ignorePropNames.contains(propName))
            tmpProps1.put(propName, userProps1.get(propName));
      }

      Map<String, String> userProps2 = rel2.getUserProperties();
      Map<String, String> tmpProps2 = new HashMap<>();
      for (String propName : userProps2.keySet())
      {
         if (!ms_ignorePropNames.contains(propName))
            tmpProps2.put(propName, userProps2.get(propName));
      }
      
      bldr.append(tmpProps1, tmpProps2);
      
      return bldr.isEquals();
   }

   /**
    * Adds all non-null, non-empty properties of the supplied relationship to
    * the provided parameters map. If the relationship does not specify a value
    * for a parameter (<code>null</code> or empty) and the parameter exists
    * in the supplied map, this will remove that parameter.
    * <p>
    * Since this method may modify the supplied parameter map, the caller must
    * be careful and may need to restore a previously made backup after
    * processing to make sure the original request still has all the parameters
    * for the original call.
    * 
    * @param htmlParameters the parameters map to which to add the relationship
    * properties or from which to remove them, not <code>null</code>, may be
    * empty, may be changed by this method.
    * @param relationship the relationship for which to add/remove the
    * properties, not <code>null</code>.
    */
   private void addPropertyParameters(Map<String, Object> htmlParameters,
         PSRelationship relationship)
      {
         if (htmlParameters == null || relationship == null)
            throw new IllegalArgumentException("parameters cannot be null");

         for (String name : relationship.getAllProperties().keySet())
         {
            String value = relationship.getProperty(name);
            if (value != null && value.trim().length() > 0)
               htmlParameters.put(name, value);
            else
               htmlParameters.remove(name);
         }
      }

   /**
    * Adds all non-null, non-empty user properties of the supplied
    * relationship configuration to the provided parameters map. This also
    * adds all newly added user properties to avoid upgrades on the relationship
    * configuration on older versions. If the relationship config does not 
    * specify a value for a parameter (<code>null</code> or empty) and the 
    * parameter exists in the supplied map, this will remove that parameter.
    * <p>
    * Since this method may modify the supplied parameter map, the caller
    * must be careful and may need to restore a previously made backup after
    * processing to make sure the original request still has all the parameters
    * for the original call.
    *
    * @param htmlParameters the parameters map to which to add the relationship
    *    config properties or from which to remove them, not <code>null</code>, 
    *    may be empty, may be changed by this method.
    * @param config the relationship config for which to add the
    *    properties, not <code>null</code>.
    */
   private void addPropertyParameters(Map<String, Object> htmlParameters,
      PSRelationshipConfig config)
   {
      if (htmlParameters == null || config == null)
         throw new IllegalArgumentException("parameters cannot be null");

      // add or remove user properties
      for (String name : config.getUserProperties().keySet())
      {
         String value = config.getUserProperty(name);
         if (value != null && value.trim().length() > 0)
            htmlParameters.put(name, value);
         else
            htmlParameters.remove(name);
      }
   }

   /**
    * Is shallow cloning allowed?
    *
    * @param config the relationship configuration object, assumed not 
    *   <code>null</code>.
    * @param data the execution data to operate on, assumed not 
    *   <code>null</code>.
    *   
    * @return <code>true</code> if shallow cloning is allowed,
    *    <code>false</code> otherwise.
    * @throws PSNotFoundException if an extension used as part of the process
    *    check cannot be found.
    * @throws PSExtensionException if an extension executed as part of the
    *    process check failed.
    */
   private boolean allowsShallowCloning(PSRelationshipConfig config,
      PSExecutionData data) throws PSNotFoundException, PSExtensionException
   {
      PSProcessCheck check = config.getProcessCheck(
         PSCloneHandler.RS_CLONESHALLOW);
      return PSCloneHandler.doProcessCheck(check, data, false);
   }

   /**
    * Is deep cloning allowed?
    *
    * @param config the relationship configuration object, assumed not 
    *   <code>null</code>..
    * @param data the execution data to operate on, assumed not 
    *   <code>null</code>.
    *   
    * @return <code>true</code> if deep cloning is allowed, <code>false</code>
    *    otherwise.
    * @throws PSNotFoundException if an extension used as part of the process
    *    check cannot be found.
    * @throws PSExtensionException if an extension executed as part of the
    *    process check failed.
    */
   private boolean allowsDeepCloning(PSRelationshipConfig config,
      PSExecutionData data) throws PSNotFoundException, PSExtensionException
   {
      PSProcessCheck check = config.getProcessCheck(
         PSCloneHandler.RS_CLONEDEEP);
      return PSCloneHandler.doProcessCheck(check, data, false);
   }

   /**
    * Fixup all inline relationships id's for the supplied list of processed 
    * relationships and the child row mappings.
    * 
    * @param request the request used to perform the modifications, assumed
    *    not <code>null</code>.
    * @param relationships the created/cloned relationships, assumed not
    *    <code>null</code>.
    * @param processedRelationships a list with all processed relationships with
    *    this request, assumed not <code>null</code>, may be empty.
    * @param childRowMappings a map of inline relationship id's created during 
    *    this request, assumed not <code>null</code>, may be empty. See
    *    {@link PSCloneFactory#CHILD_ROW_MAPPINGS_PRIVATE_OBJECT} for details
    *    of the map.
    * @throws PSCmsException for any error making the relationship 
    *    modifications.
    */
   private static void fixupInlineRelationshipIds(PSRequest request,
         Iterator<PSRelationship> relationships,
         List<ProcessedRelationship> processedRelationships,
         Map childRowMappings) throws PSCmsException
   {
      if (childRowMappings.isEmpty())
         return;

      PSRelationshipSet needModification = new PSRelationshipSet();
         
      for (int i=0; i<processedRelationships.size(); i++)
      {
         ProcessedRelationship processedRelationship = processedRelationships
               .get(i);
         String originalInlineRsId = 
            processedRelationship.m_relationship.getProperty(
               PSRelationshipConfig.PDU_INLINERELATIONSHIP);
         if (originalInlineRsId != null && 
            originalInlineRsId.trim().length() > 0)
         {
            while (relationships.hasNext())
            {
               PSRelationship relationship = relationships.next();
               String test = relationship.getProperty(
                  PSRelationshipConfig.PDU_INLINERELATIONSHIP);
               if (test != null && test.trim().length() > 0 &&
                  originalInlineRsId.equals(test))
               {
                  String newInlineRsId = (String) childRowMappings.get(
                     originalInlineRsId);
                  if (newInlineRsId != null)
                  {
                     relationship.setProperty(
                        PSRelationshipConfig.PDU_INLINERELATIONSHIP, 
                           newInlineRsId);
                     needModification.add(relationship);
                  }
               }
            }
         }
         
         if (!needModification.isEmpty())
         {
            PSRelationshipProcessor processor = PSRelationshipProcessor.getInstance();
            processor.save(needModification);
            //need to clear relationship Cache in graph as well
         }
      }
   }

   /**
    * Maps the old relationships to the new relationships.
    *
    * @param request The request that contains the new relationships.
    *    assume not <code>null</code>.
    *
    * @param createdRels the created / cloned relationships, assumed not
    *    <code>null</code>.
    *
    * @param psRelationships A list of old relationships, assume it is one or
    *    more <code>ProcessedRelationship</code> objects.
    *
    * @return The map that maps the old relationship id
    *    (in <code>Integer</code>) to the new relationship
    *    (in <code>PSRelationship</code>). Never <code>null</code>, may be
    *    empty.
    */
   private Map<Integer, PSRelationship> getRelationshipMap(PSRequest request,
         Iterator<PSRelationship> createdRels, List<ProcessedRelationship> psRelationships)
   {
      Map<Integer, PSRelationship> relateMap = 
         new HashMap<>();

      List<ProcessedRelationship> origRels = new ArrayList<ProcessedRelationship>();
      origRels.addAll(psRelationships);


      while (createdRels.hasNext())
      {
         PSRelationship relationship = createdRels.next();

         ProcessedRelationship origRel = findOrigRelationship(request,
                 relationship, origRels.iterator());

         if (origRel != null)
         {
            relateMap.put(new Integer(origRel.m_relationship.getId()), relationship);
            origRels.remove(origRel);
         }
      }

      return relateMap;
   }

   /**
    * Finds the original or old relationship from the supplied processed
    * relationships which was the base for creating the given new relationship.
    * In finding so it follows the following sequence of steps while iterating
    * through the processed relationships:
    * <p>
    * <ol>
    * <li>ignore a relationship if its relationship config type does not match</li>
    * <li>ignore a relationship if its owner locator does not match</li>
    * <li>ignore a relationship if its dependent locator does not match</li>
    * <li>ignore a relationship if its properties (name-value pairs) do not
    * match</li>
    * </ol>
    * 
    * @param request The request for this process. It contains the mapper
    * between of orignal locator and the cloned locator, assume not
    * <code>null</code>.
    * 
    * @param newRelationship The created relationship, assume not
    * <code>null</code>.
    * 
    * @param origRels A list of old relationships, assume it is one or
    * more <code>ProcessedRelationship</code> objects.
    * 
    * @return The original relationship. It may be <code>null</code> if cannot
    * find one from the <code>psRelationships</code>.
    */
   private ProcessedRelationship findOrigRelationship(PSRequest request,
      PSRelationship newRelationship, Iterator<ProcessedRelationship> origRels)
   {
      PSLocator owner = newRelationship.getOwner();
      PSLocator depent = newRelationship.getDependent();
      PSRelationshipConfig config = newRelationship.getConfig();
      boolean useOwnerRev = newRelationship.getConfig().useOwnerRevision();
      boolean userDepentRev = newRelationship.getConfig().useDependentRevision();

      ProcessedRelationship psRelationship;
      PSRelationship origRelationship;
      PSLocator clonedOwner, clonedDepent;
      PSRelationshipConfig origConfig;

      while (origRels.hasNext())
      {
         psRelationship = origRels.next();
         origRelationship = psRelationship.m_relationship;
         origConfig = psRelationship.m_relationship.getConfig();

         //====================================
         // compare the type of the relationship
         //====================================
         if (! origConfig.getName().equals(config.getName()))
            continue;

         //==============
         // compare owner
         //==============
         clonedOwner = request.getClonedLocator(
            origRelationship.getOwner().getId());
         if (useOwnerRev)  // compare both id and rev
         {
            if (!owner.equals(clonedOwner))
               continue;
         }
         else              // compare id only
         {
            if (owner.getId() != clonedOwner.getId())
               continue;
         }

         //==================
         // compare dependent
         //==================
         if (psRelationship.m_isShallowCloning)
         {
            clonedDepent = origRelationship.getDependent();
         }
         else
         {
            clonedDepent = request.getClonedLocator(
               origRelationship.getDependent().getId());
            
            if(clonedDepent==null)
               continue;
         }
         
         if(clonedDepent==null)
            continue;
         
         if (userDepentRev)   // compare both id and rev
         {
            if (! depent.equals(clonedDepent))
               continue;
         }
         else                 // compare id only
         {
            if (depent.getId() != clonedDepent.getId())
               continue;
         }

         //==================
         // Compare the relationship properties
         //==================
         Map<String, String> props = origRelationship.getProperties();
         Map<String, String> newProps = newRelationship.getProperties();
         
         if(!doesRelPropertiesMatch(props,newProps))
            continue;

         return psRelationship;
      }
      return null;
   }

   /**
    * Compares the supplied relationship property maps and returns true or false
    * based on whether they are same or not. There may be some optional
    * parameters that present in either new or original properties which we need
    * not to consider for the comparison. For example, sys_siteid and
    * sys_folderid properties may be different between the original and the
    * cloned (new) relationships. These properties will not be considered in the
    * comparison process; otherwise we will not find the original relationship
    * for corresponding to the new relationship.
    * 
    * @param origProps Map of original properties assumed not <code>null</code>.
    * @param newProps Map of new properties assumed not <code>null</code>.
    * 
    * @return <code>true</code>, if the supplied maps match based on the
    * above said description or <code>false</code> otherwise.
    */
   private boolean doesRelPropertiesMatch(Map<String, String> origProps,
         Map<String, String> newProps)
   {
      Map<String,String> op = new HashMap<>();
      Map<String,String> np = new HashMap<>();

      op.putAll(origProps);
      np.putAll(newProps);
      // remove unknown properties that may be different between original & new
      op.remove(PSRelationshipConfig.PDU_SITEID);
      op.remove(PSRelationshipConfig.PDU_FOLDERID);
      np.remove(PSRelationshipConfig.PDU_SITEID);
      np.remove(PSRelationshipConfig.PDU_FOLDERID);
      
      //Create maps with common properties
      op.keySet().retainAll(newProps.keySet());
      np.keySet().retainAll(origProps.keySet());
      //Compare the common props if they are not same return false
      if(!op.equals(np))
         return false;
      
      //Clear the maps and refill
      op.clear();
      np.clear();
      op.putAll(origProps);
      np.putAll(newProps);
      
      //This time take the uncommon ones
      op.keySet().removeAll(newProps.keySet());
      np.keySet().removeAll(origProps.keySet());
      
      //Loop through the remaining original properties and if any property is
      // not null or empty return false
      for (String key : op.keySet())
      {
         String val = op.get(key);
         if(val != null && val.length() > 0)
            return false;
         
      }
      
      //Loop through the remaining new properties and if any property is
      // not null or empty return false
      for(String key : np.keySet())
      {
         String val = np.get(key);
         if(val != null && val.length() > 0)
            return false;
      }
     
      return true;
   }


   /**
    * Gets the dependent to be used for the supplied request. We assume first
    * that the current item is going to be the dependent. In cases of circular
    * references, this might be the original instead of the clone. Therefore we
    * test if there was a clone built for the current object. If there was one
    * built, that has to be our dependent we were looking for.
    * 
    * @param request the request to get the dependent for, assumed not
    * <code>null</code>.
    * @param useDependentRevision <code>true</code> to use the dependent
    * revision, <code>false</code> otherwise.
    * 
    * @return the dependent locator and a flag to indicate if the dependent is a
    * previously cloned within the same request (or thread), never
    * <code>null</code>.
    * 
    * @throws PSAuthorizationException if the user is not authorized.
    * @throws PSInternalRequestCallException if any error occurs processing the
    * internal request call.
    * @throws PSAuthenticationFailedException if the user failed to
    * authenticate.
    * @throws PSNotFoundException for any file not found.
    * @throws SQLException for any failed SQL operation.
    * @throws PSRequestValidationException if the supplied request is invalid.
    * @throws PSSystemValidationException for any validation failed.
    * @throws IOException for any IO operation that failed.
    * @throws PSUnknownNodeTypeException for objectstore XML parsing errors.
    */
   private PSLocator getDependent(PSRequest request,
      boolean useDependentRevision)
      throws PSAuthorizationException, PSInternalRequestCallException,
         PSAuthenticationFailedException, PSNotFoundException, SQLException,
         PSRequestValidationException, PSSystemValidationException, IOException,
         PSUnknownNodeTypeException
   {
      // assume the current item is our dependent
      PSLocator dependent = new PSLocator(
         request.getParameter(IPSHtmlParameters.SYS_CONTENTID),
         request.getParameter(IPSHtmlParameters.SYS_REVISION));

      Boolean isCloned = false;
      
      // if the current item was cloned previously we must use that one
      PSLocator previousClone = request.getClonedLocator(dependent.getId());
      if (previousClone != null)
      {
         dependent = (PSLocator) previousClone.clone();
         isCloned = true;
      }
      else
      {
         Iterator clones = request.getClones();
         while (clones.hasNext())
         {
            PSLocator loc = (PSLocator) clones.next();
            if (loc.getId() == dependent.getId())
            {
               dependent = (PSLocator) loc.clone();
               isCloned = true;
               break;
            }
         }
      }
      if (!isCloned)
      {
         /**
          * There might be a clone from a previous translation. We need to
          * lookup that from the repository.
          */
         PSRequestContext req = new PSRequestContext(request);
         previousClone = PSRelationshipUtils.getExistingTranslation(req);
         if (previousClone != null)
            dependent = (PSLocator) previousClone.clone();
      }

      // fix the revision
      if (!useDependentRevision)
         dependent.setRevision(-1);

      return dependent;
   }

   /**
    * The container class for a processed relationship
    */
   private class ProcessedRelationship
   {
      /**
       * Constructs an instance of this class from the given parameters.
       *
       * @param relationship The processed relationship, assume not
       *    <code>null</code>.
       *
       * @param isShallowCloning <code>true</code> if the relationship
       *    allows shallow cloning; <code>false</code> otherwise.
       */
      ProcessedRelationship(PSRelationship relationship,
         boolean isShallowCloning)
      {
         m_relationship = relationship;
         m_isShallowCloning = isShallowCloning;
      }

      @Override
      public boolean equals(Object o)
      {
         if (!(o instanceof ProcessedRelationship))
            return false;
         ProcessedRelationship other = (ProcessedRelationship) o;
         return other.m_relationship.equals(m_relationship) && other.m_isShallowCloning == m_isShallowCloning;
      }

      @Override
      public int hashCode()
      {
         return m_relationship.hashCode() + (m_isShallowCloning ? 1 : 0);
      }

      /**
       * The processed relationship, init by ctor, never <code>null</code>
       * after that.
       */
      PSRelationship m_relationship;

      /**
       * <code>true</code> if the relationship allows shallow cloning;
       * <code>false</code> otherwise. Initialized by ctor.
       */
      boolean m_isShallowCloning;
   }

   /**
    * The name of a private object stored in the request. The private object is
    * a list of {@link FixupRelationships}, which is used to fix up the created
    * relationships after the cloning process.
    */
   private final static String FIXUP_RELS = "FIXUP_RELATIONSHIPS";
   
   /**
    * The log4j logger for this class.
    */

   private static final Logger log = LogManager.getLogger(PSConditionalCloneHandler.class);

   /**
    * This is used in
    * {@link #compareRelationships(PSRelationship, PSRelationship)}. Each entry
    * is the name of a user relationship property that should be ignored when
    * performing a comparison.
    */
   static Set<String> ms_ignorePropNames = new HashSet<>();
   {
      ms_ignorePropNames.add(PSRelationshipConfig.PDU_FOLDERID);
      ms_ignorePropNames.add(PSRelationshipConfig.PDU_SITEID);
   }


   /*
    * A comparator used to order folder relationshps first for processing to solve
    * RX-16430,RX-13380 Create Translation - Fails With Errors and Leaves Orphaned English Locale Content
    */
   static final Comparator<PSRelationship> FOLDER_REL_SORT =
           new Comparator<PSRelationship>() {
              public int compare(PSRelationship o1, PSRelationship o2) {
                 int foldero1 = o1.getConfig().getCategory().equals(PSRelationshipConfig.CATEGORY_FOLDER) ? 0 : 1;
                 int foldero2 = o2.getConfig().getCategory().equals(PSRelationshipConfig.CATEGORY_FOLDER) ? 0 : 1;
                 return foldero1-foldero2;
              }
           };
}
