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
package com.percussion.services.workflow.data;

import java.io.Serializable;
import java.util.Objects;


import javax.persistence.Column;
import javax.persistence.Embeddable;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

/**
 * Primary key for workflow role lookup
 */
@Embeddable
public class PSWorkflowRolePK implements Serializable
{
   private static final long serialVersionUID = 1L;

   @Column(name = "WORKFLOWAPPID", nullable = false)
   private long workflowId;
   
   @Column(name = "ROLEID", nullable = false)
   private long roleId;

   /**
    * Default Ctor
    */
   public PSWorkflowRolePK()
   {
      
   }
   
   /**
    * Ctor to create new primary key with data
    * @param wf the workflow id
    * @param roleid the role id
    */
   public PSWorkflowRolePK(long wf, long roleid)
   {
      workflowId = wf;
      roleId = roleid;
   }

   /**
    * @return Returns the role Id.
    */
   public long getRoleId()
   {
      return roleId;
   }

   /**
    * @param roleid The roleid to set.
    */
   public void setRoleId(long roleid)
   {
      roleId = roleid;
   }

   /**
    * @return Returns the workflowid.
    */
   public long getWorkflowId()
   {
      return workflowId;
   }

   /**
    * @param workflowid The workflowid to set.
    */
   public void setWorkflowId(long workflowid)
   {
      workflowId = workflowid;
   }

   @Override
   public boolean equals(Object o) {
      if (this == o) return true;
      if (!(o instanceof PSWorkflowRolePK)) return false;
      PSWorkflowRolePK that = (PSWorkflowRolePK) o;
      return getWorkflowId() == that.getWorkflowId() && getRoleId() == that.getRoleId();
   }

   @Override
   public int hashCode() {
      return Objects.hash(getWorkflowId(), getRoleId());
   }
}

