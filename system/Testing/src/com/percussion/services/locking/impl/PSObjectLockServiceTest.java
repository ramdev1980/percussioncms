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
package com.percussion.services.locking.impl;

import com.percussion.services.locking.IPSObjectLockService;
import com.percussion.services.locking.PSObjectLockServiceLocator;
import com.percussion.services.locking.data.PSObjectLock;
import com.percussion.services.system.IPSSystemService;
import com.percussion.services.system.PSSystemServiceLocator;
import com.percussion.services.system.data.PSSharedProperty;
import com.percussion.utils.guid.IPSGuid;
import com.percussion.utils.testing.IntegrationTest;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertTrue;


/**
 * Unit tests for the {@link PSObjectLockService} class.
 */
@Category(IntegrationTest.class)
public class PSObjectLockServiceTest
{
   /**
    * Tests all public lock services. 
    */
   @Test
   public void testLockManager() throws Exception
   {
      String session = "session";
      String locker = "locker";
      
      IPSSystemService systemService = 
         PSSystemServiceLocator.getSystemService();

      // create some objects to lock
      int count = 3;
      List<PSSharedProperty> properties = new ArrayList<PSSharedProperty>();
      for (int i=0; i<count; i++)
      {
         String name = "name_" + i;
         String value = "value_" + 1;
         PSSharedProperty property = new PSSharedProperty(name, value);

         systemService.saveSharedProperty(property);

         properties.add(property);
      }
      
      List<IPSGuid> propertyIds = new ArrayList<IPSGuid>();
      for (PSSharedProperty property : properties)
         propertyIds.add(property.getGUID());
      
      IPSObjectLockService service = 
         PSObjectLockServiceLocator.getLockingService();
      
      // create locks
      createLocks(properties, session, locker);
      
      // test that the locks are created
      List<PSObjectLock> createdLocks = service.findLocksByObjectIds(
         propertyIds, session, locker);
      assertTrue(createdLocks != null);
      assertTrue(createdLocks.size() == 3);
      
      // update lock 1 so that it expires in 1000ms
      service.extendLock(properties.get(1).getGUID(), session, locker, 
         properties.get(1).getVersion(), 1000);
      
      // make sure that lock 1 expires
      Thread.sleep(5000);
      
      // test that lock 1 has expired
      service.isLockedFor(properties.get(1).getGUID(), session, locker);
      List<PSObjectLock> remainingLocks = service.findLocksByObjectIds(
         propertyIds, session, locker);
      assertTrue(remainingLocks != null);
      assertTrue(remainingLocks.size() == 2);
      
      // update lock 2 so that it expires in 1000ms
      service.extendLock(properties.get(2).getGUID(), session, locker, 
         properties.get(1).getVersion(), 1000);
      
      // make sure that lock 2 expires
      Thread.sleep(5000);
      
      // test that lock 2 has expired and create a new one for session2/locker2
      service.createLock(properties.get(2).getGUID(), "session2", "locker2", 
         properties.get(0).getVersion(), false);
      remainingLocks = service.findLocksByObjectIds(propertyIds, null, null);
      assertTrue(remainingLocks != null);
      assertTrue(remainingLocks.size() == 2);
      
      // release remaining locks
      service.releaseLocks(remainingLocks);
      remainingLocks = service.findLocksByObjectIds(propertyIds, null, null);
      assertTrue(remainingLocks != null);
      assertTrue(remainingLocks.size() == 0);
      
      // remove shared properties
      for (PSSharedProperty property : properties)
         systemService.deleteSharedProperty(property.getGUID());
   }
   
   /**
    * lock all supplied properties.
    * 
    * @param properties the properties to lock, assumed not <code>null</code>.
    * @param session the current session, assumed not <code>null</code> or
    *    empty.
    * @param locker the user for which to lock, assumed not <code>null</code>
    *    or empty.
    * @return all created locks, never <code>null</code>, may be empty.
    * @throws Exception for any error locking the supplied properties.
    */
   private List<PSObjectLock> createLocks(List<PSSharedProperty> properties, 
      String session, String locker) throws Exception
   {
      IPSObjectLockService service = 
         PSObjectLockServiceLocator.getLockingService();

      List<PSObjectLock> locks = new ArrayList<PSObjectLock>();
      
      locks.add(service.createLock(properties.get(0).getGUID(), session, 
         locker, properties.get(0).getVersion(), false));
      locks.add(service.createLock(properties.get(1).getGUID(), session, 
         locker, properties.get(1).getVersion(), false));
      locks.add(service.createLock(properties.get(2).getGUID(), session, 
         locker, properties.get(2).getVersion(), false));
      
      return locks;
   }
}

