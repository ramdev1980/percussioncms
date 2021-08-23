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
package com.percussion.delivery.caching.tests.fakeProviders;

import com.percussion.delivery.caching.IPSCacheProviderPlugin;
import com.percussion.delivery.caching.PSCacheManagerException;
import com.percussion.delivery.caching.data.PSCacheConfig;
import com.percussion.delivery.caching.data.PSInvalidateRequest;

import org.springframework.context.ApplicationContext;

/**
 * @author miltonpividori
 *
 */
public class PSFakeProviderPlugin7 implements IPSCacheProviderPlugin
{
    public PSFakeProviderPlugin7()
    {
        
    }
    
    /* (non-Javadoc)
     * @see com.percussion.delivery.caching.IPSCacheProviderPlugin#invalidate(com.percussion.delivery.caching.data.PSInvalidateRequest)
     */
    public void invalidate(PSInvalidateRequest request) throws PSCacheManagerException
    {
        // FIXME Auto-generated method stub
        
    }

    /* (non-Javadoc)
     * @see com.percussion.delivery.caching.IPSCacheProviderPlugin#initialize(com.percussion.delivery.caching.data.PSCacheConfig)
     */
    public void initialize(PSCacheConfig config, ApplicationContext appContext)
    {
        
    }

}
