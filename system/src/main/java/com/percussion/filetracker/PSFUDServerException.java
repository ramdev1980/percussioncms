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

/**
 * This exception is thrown when application fails to load the content list
 * document from the server for a reason other than authentication failure.
 */
package com.percussion.filetracker;

public class PSFUDServerException extends Exception
{
   /**
    * Default constructor
    */
   public PSFUDServerException()
   {
   }

   /**
    * Constructore that takes the message as a parameter.
    *
    * @param msg as String
    *
    */
   public PSFUDServerException(String msg)
   {
      super(msg);
   }
}
