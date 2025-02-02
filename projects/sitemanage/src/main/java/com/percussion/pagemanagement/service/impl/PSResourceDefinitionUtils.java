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
package com.percussion.pagemanagement.service.impl;

import static org.apache.commons.collections.CollectionUtils.intersection;
import static org.apache.commons.collections.CollectionUtils.isSubCollection;
import static org.apache.commons.lang.Validate.isTrue;
import static org.apache.commons.lang.Validate.notEmpty;
import static org.apache.commons.lang.Validate.notNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.percussion.pagemanagement.data.PSResourceDefinitionGroup.PSResourceDefinition;
import com.percussion.pagemanagement.data.PSResourceDefinitionGroup.PSResourceDependency;

public class PSResourceDefinitionUtils
{
    
    
    /**
     * Sorts a list of resources by its dependencies such that if
     * resource 'b' is depends on resource 'a', resource 'b' will be
     * somewhere after resource 'a' in the returned list.
     * <strong>Note: If a resource depends on another resource it is not guarenteed to be directly
     * after the resource it depends on.</strong>
     * @param <T> Resource Definition.
     * @param resources never <code>null</code> and will not be modified.
     * @return a new list in dependency order, never <code>null</code>.
     * @throws PSResourceDefinitionDependencyCycleException if there is a cycle in the dependencies.
     */
    @SuppressWarnings("unchecked")
    public static  <T extends PSResourceDefinition> List<T> sortByDependencies(List<T> resources) throws PSResourceDefinitionDependencyCycleException {
        notNull(resources, "resources");
        int size = resources.size();
        
        if (size == 0) return new ArrayList<>();
        
        /*
         * It should take less than n*n times to sort the deps.
         * If it takes longer we have cycle(s).
         */
        int cutOff = (size - 1) * (size - 1) + 4;
        
        List<T> resourceBag = new ArrayList<>();
        List<T> sortedResources = new ArrayList<>();
        List<String> allIds = getResourceIds(resources);
        resourceBag.addAll(resources);
        while( ! resourceBag.isEmpty()  && cutOff != 0) {
            cutOff--;
            T r = resourceBag.remove(0);
            Collection<String> depIds = getResourceDependeeIds(r.getDependencies());
            depIds = intersection(depIds, allIds);
            List<String> sortedIds = getResourceIds(sortedResources);
            if (isSubCollection(depIds, sortedIds)) {
                sortedResources.add(r);
            }
            else {
                resourceBag.add(r);
            }
        }
        
        if (cutOff == 0) {
            isTrue( ! resourceBag.isEmpty(), "Should have some cycles");
            throw new PSResourceDefinitionDependencyCycleException(resourceBag);
        }
        
        return sortedResources;
    }
    
    /**
     * 
     * @param <T> resource
     * @param deps never <code>null</code>.
     * @return a list of the dependee ids, never <code>null</code>.
     */
    public static <T extends PSResourceDependency> List<String> getResourceDependeeIds(Collection<T> deps) {
        notNull(deps, "deps");
        List<String> ids = new ArrayList<>();
        for (T d : deps) {
            ids.add(d.getDependeeId());
        }
        return ids;
    }
    
    /**
     * Thrown when there is a cyle.
     * Example would be 'a' depends on 'b' which depends on 'c' which depends on 'a'.
     * @author adamgent
     *
     */
    public static class PSResourceDefinitionDependencyCycleException extends RuntimeException
    {

        private static final long serialVersionUID = 1L;
        private List<? extends PSResourceDefinition> cyclicalResources;
        public PSResourceDefinitionDependencyCycleException(List<? extends PSResourceDefinition> resources)
        {
            super();
            cyclicalResources = resources;
        }
        
        
        @Override
        public String getMessage()
        {
            return "Cycle detected with the following resources: " + getResourceIds(getCyclicalResources());
        }


        public final List<? extends PSResourceDefinition> getCyclicalResources()
        {
            return cyclicalResources;
        }
        
    }

    
    /**
     * 
     * @param <T> resource
     * @param resources never <code>null</code>.
     * @param uniqueId never <code>null</code>, empty, or blank.
     * @return <code>true</code> if it contains the resource with the unique id.
     */
    public static <T extends PSResourceDefinition> boolean containsResource(Collection<T> resources, String uniqueId) {
        notNull(resources, "resources");
        notEmpty(uniqueId, "uniqueId");
        return getResourceIds(resources).contains(uniqueId);
    }
    
    /**
     * Gets a list of resource ids from a collection of resources
     * @param <T> resource
     * @param resources never <code>null</code> will not be modified.
     * @return a new list of ids.
     */
    public static <T extends PSResourceDefinition> List<String> getResourceIds(Collection<T> resources) {
        notNull(resources, "resources");
        List<String> ids = new ArrayList<>();
        for (T r : resources) {
            ids.add(r.getUniqueId());
        }
        return ids;
    }

}

