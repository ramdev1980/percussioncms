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

package com.percussion.security;

import com.percussion.error.PSException;


/**
 * PSRoleNotDefinedException is thrown to indicate that a role reference
 * was made to a role the role security rovider does not know about.
 *
 * @author      Tas Giakouminakis
 * @version      1.0
 * @since      1.0
 */
public class PSRoleNotDefinedException extends PSException
{
   /**
    * Constructs a role not defined exception with the default
    * message.
    *
    * @param   appName            the app name (local prefix)
    *
    * @param   roleName            the name of the role
    */
   public PSRoleNotDefinedException(   java.lang.String appName,
                                    java.lang.String roleName)
   {
      super(IPSSecurityErrors.LOCAL_ROLE_NOT_DEFINED,
            new Object[] { roleName, appName });
   }

   /**
    * Constructs a role not defined exception for non-application ACLs
    * with the default message. If an application ACL is throwing this
    * exception, be sure to use the constructor which takes the name
    * of the application.
    *
    * @param   roleName            the name of the role
    */
   public PSRoleNotDefinedException(java.lang.String roleName)
   {
      super(IPSSecurityErrors.GLOBAL_ROLE_NOT_DEFINED,
            new Object[] { roleName });
   }
}

