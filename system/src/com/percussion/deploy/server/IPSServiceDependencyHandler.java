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
 *      https://www.percusssion.com
 *
 *     You should have received a copy of the GNU Affero General Public License along with this program.  If not, see <https://www.gnu.org/licenses/>
 */
package com.percussion.deploy.server;

import com.percussion.deploy.error.PSDeployException;
import com.percussion.deploy.objectstore.PSDependency;
import com.percussion.deploy.server.PSArchiveHandler;
import com.percussion.deploy.server.PSImportCtx;
import com.percussion.deploy.server.dependencies.PSDependencyHandler;
import com.percussion.security.PSSecurityToken;
import com.percussion.services.error.PSNotFoundException;


/**
 * Interface for dependency handlers that utilize services.
 */
public interface IPSServiceDependencyHandler
{
   /**
    * Performs the task of installing dependency files as described by 
    * {@link PSDependencyHandler#installDependencyFiles(PSSecurityToken,
    * PSArchiveHandler, PSDependency, PSImportCtx)}.
    *
    * @param tok The security token to use if objectstore access is required,
    * may not be <code>null</code>.
    * @param archive The archive handler to use to retrieve the required files
    * from the archive.  May not be <code>null</code>.
    * @param dep The dependency for which files are to be installed.  May not be
    * <code>null</code> and must be of the type supported by the handler.
    * @param ctx The import context to aid in the installation.  May not be
    * <code>null</code>.
    *
    * @throws IllegalArgumentException if any param is invalid.
    * @throws PSDeployException if there are any errors.
    */
   public void doInstallDependencyFiles(PSSecurityToken tok,
         PSArchiveHandler archive, PSDependency dep, PSImportCtx ctx)
           throws PSDeployException, PSNotFoundException;
}
