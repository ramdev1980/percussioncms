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
 * Primary key for transition notification lookup
 */
@Embeddable
public class PSNotificationPK implements Serializable
{
   private static final long serialVersionUID = 1L;
   
   @Column(name = "WORKFLOWAPPID", nullable = false)
   private long workflowId;

   @Column(name = "TRANSITIONID", nullable = false)
   private long transitionId;   
   
   @Column(name = "NOTIFICATIONID", nullable = false)
   private long notificationId;

   @Column(name = "TRANSITIONNOTIFICATIONID", nullable = false)
   private long transNotificationId;
   
   /**
    * Default Ctor
    */
   public PSNotificationPK()
   {
      
   }
   
   /**
    * Ctor to create new primary key with data
    * @param wf the workflow id
    * @param trans the transition id
    * @param notifid the role id
    * @param transNotifId the transition notification Id
    */
   public PSNotificationPK(long wf, long trans, long notifid, long transNotifId)
   {
      workflowId = wf;
      transitionId = trans;
      notificationId = notifid;
      transNotificationId = transNotifId;
   }

   /**
    * @return Returns the notification Id.
    */
   public long getNotificationId()
   {
      return notificationId;
   }

   /**
    * @param notifid The roleid to set.
    */
   public void setNotificationId(long notifid)
   {
      notificationId = notifid;
   }

   public long getTransNotificationId()
   {
      return transNotificationId;
   }
   
   public void setTransNotificationId(long id)
   {
      transNotificationId = id;
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
   

   /**
    * @return Returns the transition id.
    */
   public long getTransitionId()
   {
      return transitionId;
   }

   /**
    * @param transid The transition id to set.
    */
   public void setTransitionId(long transid)
   {
      transitionId = transid;
   }

   @Override
   public boolean equals(Object o) {
      if (this == o) return true;
      if (!(o instanceof PSNotificationPK)) return false;
      PSNotificationPK that = (PSNotificationPK) o;
      return getWorkflowId() == that.getWorkflowId() && getTransitionId() == that.getTransitionId() && getNotificationId() == that.getNotificationId() && getTransNotificationId() == that.getTransNotificationId();
   }

   @Override
   public int hashCode() {
      return Objects.hash(getWorkflowId(), getTransitionId(), getNotificationId(), getTransNotificationId());
   }
}

