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

package com.percussion.utils.jexl;

import org.junit.Assert;
import org.junit.Test;

public class JexlScriptFixesTest {

    @Test
    public void fixScript() {

        String testScript = "sdfgsdfg foreach($item in list ) sdfgsdfg";
        String result = JexlScriptFixes.fixScript(testScript,"Unit Test", "fixScript");
        Assert.assertEquals("sdfgsdfg for($item : list) sdfgsdfg", result);
        System.out.println(testScript +" ----> "+result);

        testScript = "if ( !$test )";
        result = JexlScriptFixes.fixScript(testScript,"Unit Test", "fixScript");
        System.out.println(testScript +" ----> "+result);
        Assert.assertEquals("if ( ! $test )", result);


        testScript = "if ( $ref1=$ref2 )";
        result = JexlScriptFixes.fixScript(testScript,"Unit Test", "fixScript");
        System.out.println(testScript +" ----> "+result);
        Assert.assertEquals("if ( $ref1 = $ref2 )", result);


        testScript = "$params=$rx.string.stringToMap(null);";
        result = JexlScriptFixes.fixScript(testScript,"Unit Test", "fixScript");
        System.out.println(testScript +" ----> "+result);
        Assert.assertEquals("$params = $rx.string.stringToMap(null);", result);


        testScript = "sdfgsdfg foreach($item in list ) sdfgsdfg sdfgsdfg foreach($item in list ) sdfgsdfg";
        result = JexlScriptFixes.fixScript(testScript,"Unit Test", "fixScript");
        System.out.println(testScript +" ----> "+result);
        Assert.assertEquals("sdfgsdfg for($item : list) sdfgsdfg sdfgsdfg for($item : list) sdfgsdfg", result);
    }
}
