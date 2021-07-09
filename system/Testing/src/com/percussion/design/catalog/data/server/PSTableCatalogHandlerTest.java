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

package com.percussion.design.catalog.data.server;

import junit.framework.TestCase;

public class PSTableCatalogHandlerTest extends TestCase
{
   public void testIsOracleRecycleBinObject()
   {
      assertTrue(isOracleRecycleBinObject("BIN$KGHNSSICRgW807Nul9jzZA==$0"));
      assertTrue(isOracleRecycleBinObject("BIN$/f2GYNKuTTadpSywb4pxaw==$0"));
      
      //lesser length
      assertFalse(isOracleRecycleBinObject("BIN$/f2GYNKuTTadpSywb4pxaw==$"));
      
      //no last $
      assertFalse(isOracleRecycleBinObject("BIN$/f2GYNKuTTadpSywb4pxaw==00"));
      
      //no first $
      assertFalse(isOracleRecycleBinObject("BIN0/f2GYNKuTTadpSywb4pxaw==$0"));
   }
   
   /**
    * Convenience method to access
    * {@link PSTableCatalogHandler#isOracleRecycleBinObject(String)}.
    */
   private boolean isOracleRecycleBinObject(String name)
   {
      return new PSTableCatalogHandler().isOracleRecycleBinObject(name);
   }
}
