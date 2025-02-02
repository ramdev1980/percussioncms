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

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.ibm.cadf.cfg.Config;
import com.ibm.cadf.util.Constants;

public class IdentifierTest
{

    @Before
    public void setUp()
    {
        System.setProperty(Constants.API_AUDIT_MAP, "/com/ibm/cadf/cfg/api_audit_map.conf");
    }

    @Test
    public void generateUniqueId()
    {
        Config.getInstance().registerProperty("namespace", "jcloud");
        String uid = Identifier.generateUniqueId();
        boolean b = uid.startsWith("jcloud");
        Assert.assertTrue("Wrong prefix", b);
    }

}
