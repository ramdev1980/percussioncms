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

package com.percussion.ant.install;

import com.percussion.install.Code;
import com.percussion.install.InstallUtil;
import com.percussion.install.PSLogger;

import org.apache.tools.ant.taskdefs.condition.Condition;

/**
 * PSEvalCondition will resolve to <code>true</code> if an eval is installed
 *
 * <br>
 * Example Usage:
 * <br>
 * <pre>
 *
 * First set the typedef:
 *
 *  <code>
 *  &lt;typedef name="evalCondition"
 *              class="com.percussion.ant.install.PSEvalCondition"
 *              classpathref="INSTALL.CLASSPATH"/&gt;
 *  </code>
 *
 * Now use the task to determine if an eval is installed.
 *
 *  <code>
 *  &lt;condition property="EVAL"&gt;
 *     &lt;evalCondition/&gt;
 *  &lt;/condition&gt;
 *  </code>
 *
 * </pre>
 *
 */
public class PSEvalCondition extends PSAction implements Condition
{
   /* (non-Javadoc)
    * @see org.apache.tools.ant.taskdefs.condition.Condition#eval()
    */
   public boolean eval()
   {
      Code code = InstallUtil.fetchBrandCode(getRootDir());
      if (code == null)
      {
         PSLogger.logInfo("PSEvalCondition : Brand code is null");
         return false;
      }
      if(code.isAnEval())
      {
         PSLogger.logInfo("PSEvalCondition : Brand code is eval");
         return true;
      }
      return false;
   }
}
