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

package com.ibm.cadf.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.text.MessageFormat;

import org.junit.Test;

import com.ibm.cadf.Messages;
import com.ibm.cadf.exception.CADFException;

public class CredentialTest
{

    @Test
    public void testCredentialPositive() throws CADFException, IOException
    {
        Credential credential = new Credential("auth token");
        assertEquals(true, credential.isValid());
    }

    @Test
    public void testCredentialNegative() throws CADFException, IOException
    {

        try
        {
            Credential credential = new Credential(null);
            credential.isValid();
            fail("Credential object creation should fail as mandatory field token is not passed");
        }
        catch (CADFException ex)
        {
            String message = MessageFormat.format(Messages.MISSING_MANDATORY_FIELDS, "token");
            assertEquals(message, ex.getMessage());
        }

        try
        {
            Credential credential = new Credential("");
            credential.isValid();
            fail("Credential object creation should fail as mandatory field token is can not be empty");
        }
        catch (CADFException ex)
        {
            String message = MessageFormat.format(Messages.MISSING_MANDATORY_FIELDS, "token");
            assertEquals(message, ex.getMessage());
        }
    }

}
