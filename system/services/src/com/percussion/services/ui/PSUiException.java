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
package com.percussion.services.ui;

import com.percussion.utils.exceptions.PSBaseException;

public class PSUiException extends PSBaseException
{
   /**
    * Compiler generated serial version ID used for serialization.
    */
   private static final long serialVersionUID = -6886987313402394369L;

   /* (non-Javadoc)
    * @see PSBaseException#PSBaseException(int)
    */
   public PSUiException(int msgCode)
   {
      super(msgCode);
   }

   /* (non-Javadoc)
    * @see PSBaseException#PSBaseException(int, Object...)
    */
   public PSUiException(int msgCode, Object... arrayArgs)
   {
      super(msgCode, arrayArgs);
   }

   /* (non-Javadoc)
    * @see PSBaseException#PSBaseException(int, throwable, Object...)
    */
   public PSUiException(int msgCode, Throwable cause, Object... arrayArgs)
   {
      super(msgCode, cause, arrayArgs);
   }

   @Override
   protected String getResourceBundleBaseName()
   {
      return "com.percussion.services.ui.PSUiErrorStringBundle";
   }
}

