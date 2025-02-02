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
package com.percussion.services.guidmgr.impl;

import com.percussion.design.objectstore.PSLocator;
import com.percussion.services.catalog.PSTypeEnum;
import com.percussion.services.guidmgr.IPSGuidManager;
import com.percussion.services.guidmgr.PSGuidManagerLocator;
import com.percussion.services.guidmgr.data.PSGuid;
import com.percussion.services.guidmgr.data.PSGuidGeneratorData;
import com.percussion.services.guidmgr.data.PSLegacyGuid;
import com.percussion.services.guidmgr.data.PSNextNumber;
import com.percussion.util.PSBaseBean;
import com.percussion.utils.guid.IPSGuid;
import com.percussion.utils.types.PSConversions;
import org.apache.commons.lang.StringUtils;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Guid manager implementation. Allocates new ids in groups, updating the
 * database as each group is allocated. Each type has its own pool of ids.
 * <P>
 * The site id is stored under the -1 id.
 * <P>
 * Ids are allocated in blocks. The code then allocates new ids from the block
 * until the block is exhausted. At that point the database is read for the next
 * block start and a new block is allocated.
 *
 * @author dougrand
 */
@PSBaseBean("sys_guidmanager")
@Transactional(propagation = Propagation.REQUIRES_NEW, noRollbackFor = IllegalArgumentException.class)
public class PSGuidManager implements IPSGuidManager
{

   @PersistenceContext
   private EntityManager entityManager;

   private Session getSession(){
      return entityManager.unwrap(Session.class);
   }

   /**
    * The key for the GUID data table where the host information is stored
    */
   static final Integer HOST_KEY = -1;

   /**
    * These keys are used to store the host IP address. The IP address is always
    * buffered out to 128 bits for comparison purposes.
    */
   static final Integer IP_KEY1 = -2;

   /**
    * The second part of the host IP address
    */
   static final Integer IP_KEY2 = -3;

   /**
    * This is the range of IDs created before allocating a new block.
    */
   static final int BLOCK_SIZE = 10;

   /**
    * This stores the host id, this initial value here is replaced by the real
    * host id on initialization.
    */
   static volatile long ms_hostId = -1;

   /**
    * A map of allocations. Each allocation allows the code to allocate IDs in a
    * range. When the range is exceeded, a new allocation is created and
    * replaces the current allocation. Data in the allocation is not persisted
    * because only the range limits matter to other processes allocating IDs.
    * <p>
    * The keys of the map are one of the following:
    * <ul>
    * <li>The ordinal of the type enumeration
    * <li>The ordinal + an offset used to separate each repository's IDs
    * <li>The string name of the next number column to use
    * </ul>
    */
   static ConcurrentHashMap<Object, Allocation> ms_allocation = new ConcurrentHashMap<>(8, 0.9f, 1);

   /**
    * Ctor - this object will be configured as a singleton in each running
    * container.
    */
   public PSGuidManager()
   {
      super();
   }

   /**
    * This must be called from synchronized code. Generates the host id if not
    * present, or if the ip address of the host has changed.
    */
   public void loadHostId()
   {

      Session sess =  getSession();
      PSGuidGeneratorData host = null;

      try
      {
         // Must get values with an upgrade key to avoid multiple writers
         host =  sess.get(PSGuidGeneratorData.class, HOST_KEY);
         PSGuidGeneratorData ip1 = sess.get(PSGuidGeneratorData.class, IP_KEY1);
         PSGuidGeneratorData ip2 =  sess.get(PSGuidGeneratorData.class, IP_KEY2);

         byte[] hostip = null;

         try
         {
            hostip = InetAddress.getLocalHost().getAddress();
            if (hostip.length < 16)
            {
               byte[] temp = new byte[16];
               System.arraycopy(hostip, 0, temp, 0, hostip.length);
               hostip = temp;
            }
         }
         catch (UnknownHostException e)
         {
            hostip = new byte[16]; // Initialized to zeros
         }

         byte[] storedip = new byte[16];
         if (ip1 != null && ip2 != null)
         {
            System.arraycopy(PSConversions.longToByteArray(ip1.getValue()), 0, storedip, 0, 8);
            System.arraycopy(PSConversions.longToByteArray(ip2.getValue()), 0, storedip, 8, 8);
         }

         if (host == null || host.getValue() == 0 || !Arrays.equals(storedip, hostip))
         {
            SecureRandom rand = new SecureRandom();
            if (host == null)
            {
               host = new PSGuidGeneratorData(HOST_KEY, 0);
            }
            int hostid = 0;
            while (hostid == 0)
            {
               hostid = rand.nextInt() & 0x00FFFFFF;
            }
            host.setValue(hostid);
            sess.saveOrUpdate(host);
            if (ip1 == null)
            {
               ip1 = new PSGuidGeneratorData(IP_KEY1, 0);
            }
            if (ip2 == null)
            {
               ip2 = new PSGuidGeneratorData(IP_KEY2, 0);
            }
            ip1.setValue(PSConversions.byteArrayToLong(hostip, 0));
            ip2.setValue(PSConversions.byteArrayToLong(hostip, 8));
            sess.saveOrUpdate(ip1);
            sess.saveOrUpdate(ip2);
         }

         ms_hostId = host.getValue();
      }
      catch (HibernateException e)
      {
         // Site unknown
      }

   }

   /*
    * (non-Javadoc)
    *
    * @see
    * com.percussion.guidmgr.IPSGuidManager#createGuids(com.percussion.utils
    * .guid.PSGuid.Type, int)
    */
   public List<IPSGuid> createGuids(PSTypeEnum type, int count)
   {
      return createGuids((byte) 0, type, count);
   }

   /*
    * (non-Javadoc)
    *
    * @see com.percussion.guidmgr.IPSGuidManager#createGuids(byte,
    * com.percussion.utils.guid.PSGuid.Type, int)
    */
   public List<IPSGuid> createGuids(byte repositoryId, PSTypeEnum type, int count)
   {
      List<IPSGuid> rval = new ArrayList<>();
      for (int i = 0; i < count; i++)
      {
         rval.add(createGuid(repositoryId, type));
      }
      return rval;
   }

   /*
    * (non-Javadoc)
    *
    * @see
    * com.percussion.guidmgr.IPSGuidManager#createGuid(com.percussion.util.guid
    * .PSGuid.Type)
    */
  public IPSGuid createGuid(PSTypeEnum type)
   {
      return createGuid((byte) 0, type);
   }

   /*
    * (non-Javadoc)
    *
    * @see com.percussion.guidmgr.IPSGuidManager#createGuid(byte,
    * com.percussion.utils.guid.PSGuid.Type)
    */
   public IPSGuid createGuid(byte repositoryId, PSTypeEnum type)
   {
      if (repositoryId < 0)
      {
         throw new IllegalArgumentException("Repository id must not be negative");
      }
      if (type == null)
      {
         throw new IllegalArgumentException("type must never be null");
      }

      if (type.getKey() == null)
         return createStandardGuid(repositoryId, type);
      else
      {
         int id = createNextNumberId(type.getKey(), BLOCK_SIZE);
         return new PSGuid(0, type, id);
      }
   }

   /*
    * (non-Javadoc)
    *
    * @see com.percussion.services.guidmgr.IPSGuidManager#convertToGuid(long,
    * com.percussion.services.catalog.PSTypeEnum)
    */
   public IPSGuid makeGuid(long raw, PSTypeEnum type, boolean forceType)
   {
      if (type == PSTypeEnum.LEGACY_CONTENT || type == PSTypeEnum.LEGACY_CHILD)
         return new PSLegacyGuid(raw);
      return new PSGuid(type, raw,forceType);
   }

    /**
     * Recreates a guid instance from a value originally obtained from
     * {@link IPSGuid#longValue()} or from a uuid.
     *
     * @param raw  This value may or may not contain the type id. If it does,
     *             then it must match the supplied <code>type</code>, otherwise, the supplied
     *             type is used as the type for the new guid.
     * @param type the type, never <code>null</code>
     * @return a guid of the specified type built from the specified raw value,
     * never <code>null</code>.
     */
    @Override
    public IPSGuid makeGuid(long raw, PSTypeEnum type) {
        if (type == PSTypeEnum.LEGACY_CONTENT || type == PSTypeEnum.LEGACY_CHILD)
            return new PSLegacyGuid(raw);
        return new PSGuid(type, raw);
    }

    /*
    * (non-Javadoc)
    *
    * @see
    * com.percussion.services.guidmgr.IPSGuidManager#makeGuid(com.percussion
    * .design.objectstore.PSLocator)
    */
   public IPSGuid makeGuid(PSLocator loc)
   {
      return new PSLegacyGuid(loc);
   }

   // see interface
   public PSLocator makeLocator(IPSGuid guid)
   {
      if (guid.getType() != PSTypeEnum.LEGACY_CONTENT.getOrdinal())
      {
         throw new IllegalArgumentException("Guid must be of type LEGACY_CONTENT.");
      }

      PSLegacyGuid legacyGuid;
      if (!(guid instanceof PSLegacyGuid))
         legacyGuid = new PSLegacyGuid(guid.longValue());
      else
         legacyGuid = (PSLegacyGuid) guid;

      return legacyGuid.getLocator();
   }

   /*
    * (non-Javadoc)
    *
    * @see
    * com.percussion.services.guidmgr.IPSGuidManager#makeGuid(java.lang.String,
    * com.percussion.services.catalog.PSTypeEnum)
    */
   public IPSGuid makeGuid(String raw, PSTypeEnum type, boolean forceType)
   {
      if (type == PSTypeEnum.LEGACY_CONTENT || type == PSTypeEnum.LEGACY_CHILD)
      {
         return makeGuid(Long.parseLong(raw), type,forceType);
      }
      return new PSGuid(type, raw, forceType);
   }

    /**
     * Recreates a guid instance from a human readable form of the guid.
     *
     * @param raw  Never <code>null</code> or empty. The generic format of the
     *             supplied string is of the form: hostid-typeid-uuid (e.g. 10-103-125). A
     *             single long value that is supported by {@link #makeGuid(long, PSTypeEnum)}
     *             can also be supplied, in which case, the rules defined in that method must
     *             be followed. Two different represenations are allowed: hostid-uuid,
     *             hostid-typeid-uuid. If a typeid is supplied, it must match that of the
     *             <code>type</code> param, otherwise, the supplied type is used. If the
     *             type is {@link PSTypeEnum#LEGACY_CONTENT} or
     *             {@link PSTypeEnum#LEGACY_CHILD}, the human-readable forms are not
     *             supported.
     * @param type the type, never <code>null</code>
     *             *
     * @return a guid of the specified type built from the specified raw value,
     * never <code>null</code>.
     */
    @Override
    public IPSGuid makeGuid(String raw, PSTypeEnum type) {
        if (type == PSTypeEnum.LEGACY_CONTENT || type == PSTypeEnum.LEGACY_CHILD)
        {
            return makeGuid(Long.parseLong(raw), type,false);
        }
        return new PSGuid(type, raw, false);
    }

    /*
    * //see base interface method for details
    */
   public IPSGuid makeGuid(String raw)
   {
      if (StringUtils.isBlank(raw))
         throw new IllegalArgumentException("raw may not be blank.");

      PSGuid guid = new PSGuid(raw);
      if (guid.getType() == PSTypeEnum.LEGACY_CONTENT.getOrdinal()
              || guid.getType() == PSTypeEnum.LEGACY_CHILD.getOrdinal())
      {
         return new PSLegacyGuid(guid);
      }
      return guid;
   }

   /**
    * Get the next number for the given key from the next number table. This
    * method first looks for an allocation to use or reuse. If the allocation is
    * missing, then it creates a new allocation.
    *
    * @param key the key to use, see the next number table for existing keys,
    *           assumed never <code>null</code> or empty
    * @param blocksize the number of ids to allocate per allocation block
    * @return the next available id
    */
   private int createNextNumberId(String key, int blocksize)
   {
      Allocation allocation = createNextNumberAllocation(key,blocksize);

      return (int)allocation.next();

   }


   private Allocation createNextNumberAllocation(final String key, final int blocksize)
   {
      // A lot easier with computeIfAbsent in java 8.
      return ms_allocation.computeIfAbsent(key, k -> new Allocation(blocksize,(bs,sv) -> (long) PSGuidManagerLocator.getGuidMgr().updateNextNumber(key, bs, sv)));
   }

   @Override
   public int fixNextNumber(String key, int value)
   {
      Allocation allocation = createNextNumberAllocation(key,BLOCK_SIZE);
      return allocation.fix(value);
   }

   @Override
   public int peekNextNumber(String key)
   {
      Allocation allocation = createNextNumberAllocation(key,BLOCK_SIZE);
      return (int)allocation.peek();
   }

   public int updateNextNumber(String key, int blocksize, long setValue)
   {

      int current = -1;

      Session s =  getSession();
      PSNextNumber data;

      data = s.get(PSNextNumber.class, key);
      if (data == null)
      {
         data = new PSNextNumber(key, 100);
         s.persist(data);
      }

      current = data.getNext();


      if (setValue>0) {
         current = (int) setValue-1;
      }

      if (blocksize>0 || setValue > 0) {
         int next = current + blocksize;
         data.setNext(next);
         try {
            s.update(data);
            s.flush();
         } catch (HibernateException e1) {
            throw new RuntimeException("Could not create or save next number info");
         }
      }

      return current+1;

   }


   /**
    * Create the next long for the given type. Longs are allocated from the
    * current allocation block if possible. If not then a new allocation block
    * is allocated first.
    *
    * @param repositoryId if provided then the repository id is folded into the
    *           type's id before allocating. The algorithm allows for
    *           <code>1000</code> types.
    * @param type the type, assumed never <code>null</code>
    * @return the next long value from the allocation
    */
   private IPSGuid createStandardGuid(byte repositoryId, PSTypeEnum type)
   {

      long hostValue = 0;
      Integer key = null;
      boolean useRepository = repositoryId > 0;

      if (useRepository)
      {
         hostValue = repositoryId | 0xFFFF00L;
         // Each repository has its own allocation
         // To make this easy to interpret in the database, multiply
         // by a power of 10.
         key = type.getOrdinal() + repositoryId * 1000;
      }
      else
      {
         if (ms_hostId < 0)
         {
             PSGuidManagerLocator.getGuidMgr().loadHostId();
         }
         hostValue = ms_hostId;
         key = (int) type.getOrdinal();
      }

      try
      {
         return new PSGuid(hostValue, type, createNextLong(key));
      }
      catch (Exception e)
      {
         throw new RuntimeException("Logic problem in next calculation",e);
      }

   }

   /**
    * The allocation is looked up and if it is exhausted then the database is
    * used to get the next id, which is updated using an upgrade lock.
    *
    * @param key the key to look up the allocation, assumed never
    *           <code>null</code>.
    * @return the allocation, never <code>null</code>.
    */
   private long createNextLong(Integer key)
   {

      return getNextLongAllocation(key).next();
   }

   private Allocation getNextLongAllocation(final Integer key)
   {
      // Use the following like in java 1.8
      return ms_allocation.computeIfAbsent(key, k -> new Allocation(BLOCK_SIZE,(bs,sv) -> PSGuidManagerLocator.getGuidMgr().updateNextLong(key)));
   }

   public long updateNextLong(Integer key)
   {
       Session s = getSession();
       PSGuidGeneratorData data;
       long current = -1L;

      data = s.get(PSGuidGeneratorData.class, key);
      if (data == null)
      {
         data = new PSGuidGeneratorData(key, 1);
         data.setVersion(0);
         s.persist(data);

      }
      current = data.getValue();

      long next = current + BLOCK_SIZE;

      data.setValue(next);
      try
      {
         s.update(data);
      }
      catch (HibernateException e1)
      {
         throw new RuntimeException("Could not create or save guid info");
      }

      return current+1;
   }

   /*
    * (non-Javadoc)
    *
    * @see com.percussion.guidmgr.IPSGuidManager#getSiteId()
    */
   public long getHostId()
   {
      return ms_hostId;
   }

   /*
    * (non-Javadoc)
    *
    * @see
    * com.percussion.services.guidmgr.IPSGuidManager#createId(java.lang.String)
    */
    public int createId(String key)
   {
      return createNextNumberId(key, BLOCK_SIZE);
   }

   /*
    * (non-Javadoc)
    *
    * @see
    * com.percussion.services.guidmgr.IPSGuidManager#createIdBlock(java.lang
    * .String, int)
    */
   public int[] createIdBlock(String key, int blocksize)
   {
      if (StringUtils.isBlank(key))
      {
         throw new IllegalArgumentException("key may not be null or empty");
      }
      if (blocksize < 1)
      {
         throw new IllegalArgumentException("Blocksize must be positive");
      }

      int[] rval = new int[blocksize];

      for (int i = 0; i < blocksize; i++)
      {
         rval[i] = createNextNumberId(key, blocksize);
      }

      return rval;
   }

   public List<Integer> extractContentIds(List<IPSGuid> guids)
   {
      if (guids == null || guids.isEmpty())
      {
         throw new IllegalArgumentException("guids may not be null or empty");
      }
      for (IPSGuid g : guids)
      {
         if (!(g instanceof PSLegacyGuid))
         {
            throw new IllegalArgumentException("guids must be content guids");
         }
      }

      if (guids.size() == 1)
      {
         PSLegacyGuid g = (PSLegacyGuid) guids.get(0);
         return Collections.singletonList(g.getContentId());
      }
      else
      {
         List<Integer> rval = new ArrayList<>();
         for (IPSGuid g : guids)
         {
            PSLegacyGuid lg = (PSLegacyGuid) g;
            rval.add(lg.getContentId());
         }
         return rval;
      }
   }

   /**
    * Create next id for the given type.
    *
    * @param type the type to use for looking up the next value, never
    *           <code>null</code>.
    * @return the next value.
    */
    public long createLongId(PSTypeEnum type)
   {
      if (type == null)
      {
         throw new IllegalArgumentException("type may not be null");
      }
      Integer key = (int) type.getOrdinal();

      long longVal = createNextLong(key);

      if (longVal < 0)
         throw new RuntimeException("Next Id returned < 0");

      return longVal;

   }
}
