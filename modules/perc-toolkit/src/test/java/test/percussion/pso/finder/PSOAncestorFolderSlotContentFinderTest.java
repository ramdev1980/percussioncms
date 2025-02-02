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
package test.percussion.pso.finder;

import static java.util.Arrays.*;
import static org.junit.Assert.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JMock;
import org.jmock.integration.junit4.JUnit4Mockery;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.percussion.extension.PSExtensionProcessingException;
import com.percussion.pso.finder.PSOAncestorFolderSlotContentFinder;
import com.percussion.pso.jexl.PSOFolderTools;
import com.percussion.services.assembly.IPSAssemblyItem;
import com.percussion.services.assembly.IPSTemplateSlot;
import com.percussion.services.assembly.impl.finder.PSBaseSlotContentFinder.SlotItem;
import com.percussion.services.content.data.PSItemSummary;
import com.percussion.services.guidmgr.IPSGuidManager;
import com.percussion.utils.guid.IPSGuid;
import com.percussion.webservices.PSErrorException;
import com.percussion.webservices.PSErrorResultsException;
import com.percussion.webservices.content.IPSContentWs;

//import static java.util.Arrays.*;
//import static org.hamcrest.CoreMatchers.*;
//import static org.junit.matchers.JUnitMatchers.*;

/**
 * Scenario description: 
 * @author adamgent, Nov 25, 2008
 */
@RunWith(JMock.class)
public class PSOAncestorFolderSlotContentFinderTest {

    Mockery context = new JUnit4Mockery();

    PSOAncestorFolderSlotContentFinder finder;

    IPSContentWs contentWs;
    
    StubFolderTools folderTools;
    
    IPSGuidManager guidManager;
    
    IPSAssemblyItem assemblyItem;
    
    IPSTemplateSlot slot;
    
    Map<String, Object> params;
    
    protected static class StubFolderTools extends PSOFolderTools {

        String expectedFolderPath = "//path";
        public String getExpectedFolderPath() {
            return expectedFolderPath;
        }
        public void setExpectedFolderPath(String expectedFolderPath) {
            this.expectedFolderPath = expectedFolderPath;
        }
        @Override
        public String getParentFolderPath(IPSAssemblyItem assemblyItem)
                throws PSErrorResultsException, PSExtensionProcessingException,
                PSErrorException {
            return getExpectedFolderPath();
        }
        
    }
    
    @Before
    public void setUp() throws Exception {
        
        contentWs = context.mock(IPSContentWs.class, "contentWs");
        guidManager = context.mock(IPSGuidManager.class, "guidManager");
        assemblyItem = context.mock(IPSAssemblyItem.class, "assemblyItem");
        slot = context.mock(IPSTemplateSlot.class, "slot");
        
        folderTools = new StubFolderTools();
        finder = new PSOAncestorFolderSlotContentFinder(contentWs, folderTools);
        params = new HashMap<String,Object>();

        context.checking(new Expectations() {{
            allowing(slot).getFinderArguments();
            will(returnValue(new HashMap<String, String>()));

        }});
    }

    
    
    @Test
    public void shouldGetSlotItemOfContentTypeInParentFolder() throws Exception {
        /*
         * Given: we pass in the content type name of generic.
         * The assembly item is in folder path below.
         * The desired item slot item of type generic is in folder b.
         * 
         */
        
        params.put(PSOAncestorFolderSlotContentFinder.PARAM_CONTENTTYPE, "generic");
        final String path = "//a/b/c";
        
        /* 
         * Expect: 
         *  To get the folder path of the assembly item.
         *  Load that folder path using the content ws.
         *  For each folder in the path check to see if it has a
         *  child of the given content type. Return that child.
         * 
         */
        
        context.checking(new Expectations() {{
            
            allowing(assemblyItem).getId();
            
            IPSGuid a = context.mock(IPSGuid.class, "a");
            IPSGuid b = context.mock(IPSGuid.class, "b");
            IPSGuid c = context.mock(IPSGuid.class, "c");
            
            //PSOFolderTools expect.
            
            folderTools.setExpectedFolderPath(path);
            one(contentWs).findPathIds(path);
            will(returnValue(asList(a,b,c)));
            
            /*
             * c has no children.
             */
            one(contentWs).findFolderChildren(c, false);
            one(contentWs).findFolderChildren(b, false);
            
            /*
             * b has two children.
             */
            PSItemSummary sumYes = new PSItemSummary(1, 1, "yes", 300, "generic",false);
            PSItemSummary sumNo = new PSItemSummary(2, 1, "yes", 302, "blah",false);
            will(returnValue(asList(sumNo, sumYes)));
            
            /*
             * We should not need to load folder a's children since 
             * b already has an item that is generic.
             */
            never(contentWs).findFolderChildren(a, false);
            
            
            
        }});

        /*
         * When: we call getSlotItems from the finder.
         */

        Set<SlotItem> slotItems = finder.getSlotItems(assemblyItem, slot, params);
        /*
         * Then: We should only have one slot item.
         */
        
        assertEquals(1, slotItems.size());
    }
    
    
    @Test
    public void shouldGetZeroSlotItemsFromAncestorFoldersIfNoItemOfDesiredTypeExistInAncestorFolders() throws Exception {
        /*
         * Given: we pass in the content type name of generic.
         * The assembly item is in folder path below.
         * The desired item slot item of type generic is in folder b.
         * 
         */
        
        params.put(PSOAncestorFolderSlotContentFinder.PARAM_CONTENTTYPE, "generic");
        final String path = "//a/b/c";
        
        /* 
         * Expect: findFolderChildren to happen on 
         * ALL ancestor folders since none of the folders have
         * an item with the desired content type.
         */
        
        context.checking(new Expectations() {{
            
            allowing(assemblyItem).getId();
            
            IPSGuid a = context.mock(IPSGuid.class, "a");
            IPSGuid b = context.mock(IPSGuid.class, "b");
            IPSGuid c = context.mock(IPSGuid.class, "c");
            
            //PSOFolderTools expect.
            
            folderTools.setExpectedFolderPath(path);
            one(contentWs).findPathIds(path);
            will(returnValue(asList(a,b,c)));
            
            /*
             * c has no children.
             */
            one(contentWs).findFolderChildren(c, false);
            one(contentWs).findFolderChildren(b, false);
            
            /*
             * b has two children.
             */
            PSItemSummary sumNotGeneric = new PSItemSummary(1, 1, "yes", 300, "NOT_GENERIC",false);
            PSItemSummary sumNo = new PSItemSummary(2, 1, "yes", 302, "blah",false);
            will(returnValue(asList(sumNo, sumNotGeneric)));
            
            /*
             * folder a has no children.
             */
            one(contentWs).findFolderChildren(a, false);
            
            
            
        }});

        /*
         * When: we call getSlotItems from the finder.
         */

        Set<SlotItem> slotItems = finder.getSlotItems(assemblyItem, slot, params);
        /*
         * Then: We should have zero items
         */
        
        assertEquals(0, slotItems.size());
    }


}

