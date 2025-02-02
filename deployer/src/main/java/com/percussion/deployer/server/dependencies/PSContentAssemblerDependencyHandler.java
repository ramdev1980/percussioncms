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
import com.percussion.deployer.objectstore.PSDeployComponentUtils;
import com.percussion.deployer.objectstore.PSDeployableElement;
import com.percussion.deployer.server.PSDependencyDef;
import com.percussion.deployer.server.PSDependencyMap;
import com.percussion.error.PSDeployException;
import com.percussion.security.PSSecurityToken;
import com.percussion.services.assembly.IPSAssemblyTemplate;
import com.percussion.services.error.PSNotFoundException;
import com.percussion.utils.guid.IPSGuid;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;


/**
 * Class to handle packaging and deploying a Content Assembler Element.
 */
public class PSContentAssemblerDependencyHandler 
   extends PSDependencyHandler
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
   public PSContentAssemblerDependencyHandler(PSDependencyDef def,
      PSDependencyMap dependencyMap)
   {
      super(def, dependencyMap);
   }

   /**
    *
    * Get all templates with assembly urls, matching a pattern like:
    *  <code>"../app-name/%"</code>
    * @param tok, the security token, never <code>null</code>
    * @param dep, the dependency, never <code>null</code>
    * @return template guids, never  <code>null</code>.
    */
   private Set<IPSGuid> getTemplateIdsByAssemblyUrl(PSSecurityToken tok,
         PSDependency dep)
   {
      if (tok == null)
         throw new IllegalArgumentException("tok may not be null");
      if (dep == null)
         throw new IllegalArgumentException("Dependency may not be null");
      
      String appPattern = "../" + dep.getDependencyId() + "/%";
      Set<IPSGuid> tmpGuids = new HashSet<>();
      List<IPSAssemblyTemplate> tmps = m_asHelper
            .findTemplatesByAssemblyURL(appPattern);
      for (IPSAssemblyTemplate t : tmps)
         tmpGuids.add(t.getGUID());
      return tmpGuids;
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
      List<PSDependency> childDeps = new ArrayList<>();
      init();
      // 1. get templates(legacy) that have reference to the appname in url
      Set<IPSGuid> tmpIds = getTemplateIdsByAssemblyUrl(tok, dep);
      PSDependencyHandler h = getDependencyHandler(
            PSVariantDefDependencyHandler.DEPENDENCY_TYPE);
      for (IPSGuid g : tmpIds)
      {
         PSDependency d = h.getDependency(tok, String.valueOf(g.longValue()));
         d.setDependencyType(PSDependency.TYPE_LOCAL);
         childDeps.add(d);
      }
            
      // 2. get the LOCAL application child dependencies
      h = getDependencyHandler(PSApplicationDependencyHandler.DEPENDENCY_TYPE);
      PSDependency d = h.getDependency(tok, dep.getDependencyId());
      if ( d != null )
      {
         d.setDependencyType(PSDependency.TYPE_LOCAL);
         childDeps.add(d);     
      }
      return childDeps.iterator();
   }

   /**
    * Util method to initialize AssemblyServiceHelper
    *
    */
   private void init()
   {
      if ( m_asHelper == null)
         m_asHelper = new PSAssemblyServiceHelper();
   }
   
   // see base class
   public Iterator getDependencies(PSSecurityToken tok) throws PSDeployException
   {
      if (tok == null)
         throw new IllegalArgumentException("tok may not be null");
      init();    
      Collection<IPSAssemblyTemplate> tmps = m_asHelper
            .getLegacyTemplatesMap().values();
      //    get a distinct list of app names
      Set<String> appNames = getAppNamesFromAssemblyUrl(tmps);
      
      Iterator names = appNames.iterator();
      // creates the dependencies from the app names
      List<PSDependency> deps = new ArrayList<>();
      PSDependency dep;
      while (names.hasNext())
      {
         String name = (String) names.next();
         dep = new PSDeployableElement(PSDependency.TYPE_SHARED,
               name, m_def.getObjectType(), m_def.getObjectTypeName(), name, 
               m_def.supportsIdTypes(), m_def.supportsIdMapping(), 
               m_def.supportsUserDependencies(), m_def.supportsParentId());
               
         dep.setShouldAutoExpand(m_def.shouldAutoExpand());
         deps.add(dep);
      }
      
      return deps.iterator();
   }

   /**
    * Troll thru all the templates and return a list of application names from
    * assembly url. 
    * @param tmps, the template collection never <code>null</code>
    * @return
    */
   private Set<String> getAppNamesFromAssemblyUrl(
         Collection<IPSAssemblyTemplate> tmps)
   {
      if ( tmps == null )
         throw new IllegalArgumentException("templates may not be null");
      Set<String> appNames = new HashSet<>();
      for (IPSAssemblyTemplate template : tmps)
      {
         String url = template.getAssemblyUrl();
         String appName = PSDeployComponentUtils.getAppName(url);
         appNames.add(appName);
      }
      return appNames;
   }
   
   public boolean doesDependencyExist(PSSecurityToken tok, String id)
         throws PSDeployException
   {
      if (tok == null)
         throw new IllegalArgumentException("tok may not be null");

      if (id == null || id.trim().length() == 0)
         throw new IllegalArgumentException("id may not be null or empty");

      return getDependency(tok, id) != null;
   }

   // see base class
   public PSDependency getDependency(PSSecurityToken tok, String id)
      throws PSDeployException
   {
      if (tok == null)
         throw new IllegalArgumentException("tok may not be null");
      
      init();     
      if (id == null || id.trim().length() == 0)
         throw new IllegalArgumentException("id may not be null or empty");

      // no dep depends on a contentassembler yet  
      return createDeployableElement(m_def, id, id);
   }

   /**
    * Provides the list of child dependency types this class can discover.
    * The child types supported by this handler are:
    * <ol>
    * <li>Application</li>
    * <li>VariantDef</li>
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

   /**
    * Constant for this handler's supported type
    */
   final static String DEPENDENCY_TYPE = "ContentAssembler";
   
   /**
    * An instance of assembly service helper
    */
   private PSAssemblyServiceHelper m_asHelper = null;
   
   /**
    * List of child types supported by this handler, it will never be
    * <code>null</code> or empty.
    */
   private static List<String> ms_childTypes = new ArrayList<>();

   static
   {
      ms_childTypes.add(PSApplicationDependencyHandler.DEPENDENCY_TYPE);
      ms_childTypes.add(PSVariantDefDependencyHandler.DEPENDENCY_TYPE);
      ms_childTypes.add(PSTemplateDefDependencyHandler.DEPENDENCY_TYPE);
   }

}
