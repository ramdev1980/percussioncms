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

package com.percussion.rest.errors;

import javax.ws.rs.core.Response;

/**
 * @author stephenbolton
 * 
 */
public class FolderNotFoundException extends RestExceptionBase
{
    /**
	 * 
	 */
	private static final long serialVersionUID = -4398063672305185319L;

	public FolderNotFoundException()
    {
        super(RestErrorCode.FOLDER_NOT_FOUND, null, null, Response.Status.NOT_FOUND);
    }

    public FolderNotFoundException(Throwable cause){
	    super(cause);
    }
}
