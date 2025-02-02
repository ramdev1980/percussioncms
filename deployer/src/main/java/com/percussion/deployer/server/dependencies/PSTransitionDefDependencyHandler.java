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

package com.percussion.deployer.server.dependencies;

import com.percussion.deployer.objectstore.PSDependency;
import com.percussion.deployer.objectstore.PSIdMap;
import com.percussion.deployer.server.PSArchiveHandler;
import com.percussion.deployer.server.PSDbmsHelper;
import com.percussion.deployer.server.PSDependencyDef;
import com.percussion.deployer.server.PSDependencyMap;
import com.percussion.deployer.server.PSImportCtx;
import com.percussion.error.PSDeployException;
import com.percussion.security.PSSecurityToken;
import com.percussion.services.error.PSNotFoundException;
import com.percussion.tablefactory.PSJdbcFilterContainer;
import com.percussion.tablefactory.PSJdbcRowData;
import com.percussion.tablefactory.PSJdbcSelectFilter;
import com.percussion.tablefactory.PSJdbcTableData;
import com.percussion.utils.collections.PSIteratorUtils;

import java.sql.Types;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * Class to handle discovery of transition dependencies.  
 * The <code>PSWorkflowDefDependencyHandler</code> class handles the packaging
 * and installation of a transition.
 */
public class PSTransitionDefDependencyHandler 
   extends PSDataObjectDependencyHandler
{
   /**
    * Construct a dependency handler.
    *
    * @param def The def for the type supported by this handler.  May not be
    * <code>null</code> and must be of the type supported by this class.  See
    * {@link #getType()} for more info.
    * @param dependencyMap The full dependency map.  May not be
    * <code>null</code>.
    *
    * @throws IllegalArgumentException if any param is invalid.
    */
   public PSTransitionDefDependencyHandler(PSDependencyDef def,
      PSDependencyMap dependencyMap)
   {
      super(def, dependencyMap);
   }

   // see base class
   public Iterator getChildDependencies(PSSecurityToken tok, PSDependency dep)
           throws PSDeployException, PSNotFoundException {
      if (tok == null)
         throw new IllegalArgumentException("tok may not be null");
      if (dep == null)
         throw new IllegalArgumentException("dep may not be null");
      if (! dep.getObjectType().equals(DEPENDENCY_TYPE))
         throw new IllegalArgumentException("dep wrong type");

      Iterator childIDs = getExitDependencies(dep);

      return getDepsFromIds(childIDs, 
         PSExitDefDependencyHandler.DEPENDENCY_TYPE, tok).iterator();
    }

   // see base class
   public Iterator getDependencies(PSSecurityToken tok) throws PSDeployException
   {
      if (tok == null)
         throw new IllegalArgumentException("tok may not be null");
      
      List deps = new ArrayList<>();
      Iterator ids = getChildPairIdsFromTable(TRANSITIONS_TABLE, TRANSITION_ID, 
         WORKFLOW_ID, null);
      while (ids.hasNext())
      {
         String id = (String)ids.next();
         PSPairDependencyId pairId = new PSPairDependencyId(id);
         PSDependency dep = getDependency(tok, pairId.getChildId(), 
            PSWorkflowDefDependencyHandler.DEPENDENCY_TYPE, 
            pairId.getParentId());
         if (dep != null)
            deps.add(dep);
      }
      
      return deps.iterator();
   }
   
   // see base class
   public Iterator getDependencies(PSSecurityToken tok, String parentType, 
      String parentId) throws PSDeployException
   {
      if (tok == null)
         throw new IllegalArgumentException("tok may not be null");

      if (parentType == null || parentType.trim().length() == 0)
         throw new IllegalArgumentException(
            "parentType may not be null or empty");
            
      if (parentId == null || parentId.trim().length() == 0)
         throw new IllegalArgumentException(
            "parentId may not be null or empty");

      List deps = new ArrayList();
      Iterator ids = getChildPairIdsFromTable(TRANSITIONS_TABLE, TRANSITION_ID, 
         WORKFLOW_ID, parentId);
      while (ids.hasNext())
      {
         String id = (String)ids.next();
         PSPairDependencyId pairId = new PSPairDependencyId(id);
         PSDependency dep = getDependency(tok, pairId.getChildId(), 
            PSWorkflowDefDependencyHandler.DEPENDENCY_TYPE, 
            pairId.getParentId());
         if (dep != null)
            deps.add(dep);
      }
      
      return deps.iterator();
      
   }
   
   // see base class
   public PSDependency getDependency(PSSecurityToken tok, String id, 
      String parentType, String parentId)
         throws PSDeployException
   {
      if (tok == null)
         throw new IllegalArgumentException("tok may not be null");

      if (id == null || id.trim().length() == 0)
         throw new IllegalArgumentException("id may not be null or empty");
      
      if (parentType == null || parentType.trim().length() == 0)
         throw new IllegalArgumentException(
            "parentType may not be null or empty");
            
      if (parentId == null || parentId.trim().length() == 0)
         throw new IllegalArgumentException(
            "parentId may not be null or empty");

      if (!parentType.equals(PSWorkflowDefDependencyHandler.DEPENDENCY_TYPE))
         throw new IllegalArgumentException("parentType wrong type");
            
      PSDependency transDep = null;
      
      // create the filter from the id of the state dependency
      PSJdbcSelectFilter fltTransId = new PSJdbcSelectFilter(TRANSITION_ID,
         PSJdbcSelectFilter.EQUALS, id, Types.INTEGER);
      PSJdbcSelectFilter fltWorklowId = new PSJdbcSelectFilter(WORKFLOW_ID,
         PSJdbcSelectFilter.EQUALS, parentId, 
         Types.INTEGER);

      PSJdbcFilterContainer fltWhere = new PSJdbcFilterContainer();
      fltWhere.doAND(fltTransId);
      fltWhere.doAND(fltWorklowId);
      
      // get the result set from the database
      String[] columns = {TRANSITION_NAME};  
      PSDbmsHelper dbmsHelper = PSDbmsHelper.getInstance(); 
      
      PSJdbcTableData data = dbmsHelper.catalogTableData(
         TRANSITIONS_TABLE, columns, fltWhere);

      // should only get back one, take the first if found
      if (data != null && data.getRows().hasNext())
      {
         Iterator rows = data.getRows();
         PSJdbcRowData row = (PSJdbcRowData) rows.next();
         String transName = dbmsHelper.getColumnString(TRANSITIONS_TABLE, 
            TRANSITION_NAME, row);
         transDep = createDependency(m_def, id, transName);
         transDep.setParent(parentId, parentType);
      }

      return transDep;
   }
    

   // see base class
   public boolean doesDependencyExist(PSSecurityToken tok, String id, 
      String parentId) throws PSDeployException
   {
      if (tok == null)
         throw new IllegalArgumentException("tok may not be null");

      if (id == null || id.trim().length() == 0)
         throw new IllegalArgumentException("id may not be null or empty");
      
      if (parentId == null || parentId.trim().length() == 0)
         throw new IllegalArgumentException(
            "parentId may not be null or empty");
      
      return getDependency(tok, id, 
         PSWorkflowDefDependencyHandler.DEPENDENCY_TYPE, parentId) != null;
   }

   
   /**
    * Provides the list of child dependency types this class can discover.
    * The child types supported by this handler are:
    * <ol>
    * <li>Extension</li>
    * </ol>
    *
    * @return An iterator over zero or more types as <code>String</code>
    * objects, never <code>null</code>, does not contain <code>null</code> or
    * empty entries.
    */
   public Iterator getChildTypes()
   {
      return ms_childTypes.iterator();
   }

   // see base class
   public String getType()
   {
      return DEPENDENCY_TYPE;
   }

   // see base class
   public String getParentType()
   {
      return PSWorkflowDefDependencyHandler.DEPENDENCY_TYPE;
   }
   
   // see base class
   public void reserveNewId(PSDependency dep, PSIdMap idMap)
      throws PSDeployException
   {
      if (dep == null)
         throw new IllegalArgumentException("dep may not be null");

      if (!dep.getObjectType().equals(DEPENDENCY_TYPE))
         throw new IllegalArgumentException("dep wrong type");

      if (idMap == null)
         throw new IllegalArgumentException("idMap may not be null");

      reserveNewId(dep, idMap, TRANSITIONS_TABLE, DEPENDENCY_TYPE);
   }

   // see base class
   public Iterator getDependencyFiles(PSSecurityToken tok, PSDependency dep)
      throws PSDeployException
   {
      if (tok == null)
         throw new IllegalArgumentException("tok may not be null");

      if (dep == null)
         throw new IllegalArgumentException("dep may not be null");

      if (!dep.getObjectType().equals(DEPENDENCY_TYPE))
         throw new IllegalArgumentException("dep wrong type");

      // nothing to deploy, assume it has been handled by the workflow handler
      return PSIteratorUtils.emptyIterator();
   }

   // see base class
   public void installDependencyFiles(PSSecurityToken tok,
      PSArchiveHandler archive, PSDependency dep, PSImportCtx ctx)
         throws PSDeployException
   {
      // nothing to install, assume it has been handled by the workflow handler
   }
   
   /**
    * Override the method from super class, but this is to get the next id 
    * specifically for <code>TRANSITION_ID</code> in the 
    * <code>TRANSITIONS_TABLE</code>.
    */
   protected String getNextId(String table, PSDependency dep, 
      String tgtParentId) throws PSDeployException
   {
      if (table == null || table.trim().length() == 0)
         throw new IllegalArgumentException("table may not be null or empty");
         
      if (dep == null)
         throw new IllegalArgumentException("dep may not be null");
      
      if (!dep.getObjectType().equals(DEPENDENCY_TYPE))
         throw new IllegalArgumentException("dep wrong type");

      if (tgtParentId == null || tgtParentId.trim().length() == 0)
         throw new IllegalArgumentException(
            "tgtParentId may not be null or empty");
      
      int id = PSDbmsHelper.getInstance().getNextIdInMemory(TRANSITIONS_TABLE, 
         TRANSITION_ID, WORKFLOW_ID, tgtParentId);
         
      return String.valueOf(id);
   }
   
    /**
     * Get a list of child exit dependencies for a given transition 
     * deployable object.
     *
     * @param dep The transition deployable object, assumed not 
     * <code>null</code>.
     *
     * @return An iterator over zero or more extension refs of child exit
     * dependencies as <code>String</code> objects. Never <code>null</code>, but 
     * may be empty.
     *
     * @throws PSDeployException if there are any errors.
     */
    private Iterator getExitDependencies(PSDependency dep)
      throws PSDeployException
    {
      PSDbmsHelper dbmsHelper = PSDbmsHelper.getInstance();
      String[] columns = {TRANSITION_ACTION};
      PSJdbcSelectFilter fltTrans;
      PSJdbcSelectFilter fltWorkflowId;
      
      String transId = dep.getDependencyId();
      String workflowId = dep.getParentId();

      // create filter:
      // WHERE (WORKFLOW_ID = workflowId) AND
      //       (TRANSITIONID = transId)
      
      fltTrans = new PSJdbcSelectFilter(TRANSITION_ID,
         PSJdbcSelectFilter.EQUALS, transId, Types.INTEGER);
      fltWorkflowId = new PSJdbcSelectFilter(WORKFLOW_ID,
         PSJdbcSelectFilter.EQUALS, workflowId, Types.INTEGER);

      PSJdbcFilterContainer fltWhere = new PSJdbcFilterContainer();
      fltWhere.doAND(fltTrans);
      fltWhere.doAND(fltWorkflowId);

      PSJdbcTableData data = dbmsHelper.catalogTableData(
         TRANSITIONS_TABLE, columns, fltWhere);

      // use "Set" to make sure it is a distinct list
      Set ids = new HashSet();

      if (data != null && data.getRows().hasNext())
      {
         Iterator rows = data.getRows();
         String id;
         PSJdbcRowData row;
         while (rows.hasNext())
         {
            row = (PSJdbcRowData)rows.next();
            
            // this is a nullable column
            id = getColumnValueNullable(TRANSITIONS_TABLE, TRANSITION_ACTION, 
               row);
            if (id != null && id.trim().length() != 0)
               ids.add(id);
         }
      }
      return ids.iterator();
    }

   
   /**
    * Constant for this handler's supported type
    */
   final static String DEPENDENCY_TYPE = "TransitionDef";

   // private table and column names
   private static final String TRANSITIONS_TABLE = "TRANSITIONS";
   private static final String TRANSITION_ID = "TRANSITIONID";
   private static final String TRANSITION_NAME = "TRANSITIONLABEL";
   private static final String TRANSITION_ACTION = "TRANSITIONACTIONS";
   private static final String WORKFLOW_ID = "WORKFLOWAPPID";

   /**
    * List of child types supported by this handler, it will never be
    * <code>null</code> or empty.
    */
   private static List ms_childTypes = new ArrayList();

   static
   {
      ms_childTypes.add(PSExitDefDependencyHandler.DEPENDENCY_TYPE);
   }

}
