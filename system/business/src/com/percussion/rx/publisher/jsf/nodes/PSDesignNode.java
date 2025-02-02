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
package com.percussion.rx.publisher.jsf.nodes;

import com.percussion.rx.jsf.PSCategoryNodeBase;
import com.percussion.rx.jsf.PSEditableNode;
import com.percussion.rx.jsf.PSEditableNodeContainer;
import com.percussion.rx.jsf.PSNodeBase;
import com.percussion.services.error.PSNotFoundException;
import com.percussion.utils.guid.IPSGuid;

import java.util.ArrayList;
import java.util.List;

import javax.faces.model.SelectItem;

import org.apache.commons.lang.StringUtils;


/**
 * The node specific to the publishing design tab.
 * 
 * @author dougrand
 */
public abstract class PSDesignNode extends PSEditableNode
{
   /**
    * Ctor.
    * 
    * @param title never <code>null</code> or empty
    * @param guid the GUID of the design object, never <code>null</code>
    */
   public PSDesignNode(String title, IPSGuid guid) {
      super(title, guid);
   }
   
   // see base
   @Override
   protected String getOutcomePrefix()
   {
      return "pub-design-";
   }
   
   @Override
   protected String getCategoryKey()
   {
      return (String) getParent().getKey();
   }
   
   /**
    * Add the new node to the tree and navigate to it for editing.
    * 
    * @param newnode the new node, never <code>null</code>.
    * 
    * @return the outcome of the perform method.
    */
   @Override
   protected String editNewNode(PSDesignNode newnode)
   {
      return editNewNode(getContainer(), newnode);
   }

   /**
    * Add the new node to the given parent node and navigate to it for editing.
    * 
    * @param parent the parent node, never <code>null</code>.
    * @param newnode the new node, never <code>null</code>.
    * 
    * @return the outcome of the perform method.
    */
   protected String editNewNode(PSEditableNodeContainer parent,
         PSDesignNode newnode)
   {
      if (newnode == null)
         throw new IllegalArgumentException("newnode may not be null");
      if (parent == null)
         throw new IllegalArgumentException("parent may not be null");

      parent.addNode(newnode);
      return newnode.perform();
   }

   /**
    * Get the root node.
    * @return the root node, never <code>null</code>.
    */
   private PSNodeBase getRoot()
   {
      PSNodeBase root = getParent();
      while (root.getParent() != null)
         root = root.getParent();

      return root;
   }
   
   /**
    * Get the top level node for the specified node name. It is a child node of 
    * the root node.
    * @param nodeName the node name, assumed not <code>null</code> or empty. 
    * @return the specified node, never <code>null</code>.
    */
   private PSNodeBase getToplevelNode(String nodeName) throws PSNotFoundException {
      PSNodeBase root = getRoot();

      for (PSNodeBase n : root.getChildren())
      {
          if (n.getTitle().equals(nodeName))
             return n;
      }
      throw new IllegalArgumentException(
            "Cannot find a child of root node with the name = '" + nodeName
                  + "'");
   }

   /**
    * Get a list of child name as a selection candidate for a specified 
    * container node name. The container node is a child of root node.
    * @param name the name of the container node. It may not be 
    *    <code>null</code> or empty.
    * @return the list of selectable items, never <code>null</code>, but may
    *    be empty.
    */
   protected List<SelectItem> getSelectionFromContainer(String name) throws PSNotFoundException {
      if (StringUtils.isBlank(name))
         throw new IllegalArgumentException("name may not be null or empty.");
      
      PSCategoryNodeBase container = (PSCategoryNodeBase) getToplevelNode(name);

      // get all delivery type's title/names
      List<SelectItem> rval = new ArrayList<>();
      for (PSNodeBase n : container.getChildren())
      {
         rval.add(new SelectItem(n.getTitle(), n.getTitle()));
      }
      return rval;
   }

}
