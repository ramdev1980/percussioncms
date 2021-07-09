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

package com.percussion.install;

import java.io.IOException;
import java.sql.Connection;
import java.util.ArrayList;

/**
 * The IRxConvert interface must be implemented by any class which
 * can be used to convert installed files.
 *
 */
public interface IRxConvert {
  /**
   * Converts the installed files.
   * 
   * @param files - The list of files to convert.
   * @param conn - The connection pointing to the backend database
   *              specified by rxrepository.properties.
   */
   public abstract void convert(ArrayList files, Connection conn) throws IOException;
}

