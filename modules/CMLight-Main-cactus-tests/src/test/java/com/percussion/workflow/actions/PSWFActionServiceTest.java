/******************************************************************************
 *
 * [ PSWorkflowActionsTest.java ]
 * 
 * COPYRIGHT (c) 1999 - 2011 by Percussion Software, Inc., Woburn, MA USA.
 * All rights reserved. This material contains unpublished, copyrighted
 * work including confidential and proprietary information of Percussion.
 *
 *****************************************************************************/
package com.percussion.workflow.actions;

import com.percussion.design.objectstore.PSNotFoundException;
import com.percussion.extension.IPSWorkflowAction;
import com.percussion.extension.PSExtensionException;
import com.percussion.utils.testing.IntegrationTest;
import org.apache.cactus.ServletTestCase;
import org.junit.experimental.categories.Category;

import java.util.List;

/**
 * Unit Tests for PSWFActionService which is used by the workflow transition 
 * action dispatcher extension.  
 * 
 * @author BillLanglais
 */
@Category(IntegrationTest.class)
public class PSWFActionServiceTest extends ServletTestCase
{
   /**
    * Test method for
    * {@link PSWFActionService#getActions(int, int)}
    * .
    * 
    * @throws Exception
    */
   public void testGetActions() throws Exception
   {
      IPSWFActionService wfActionService = PSWFActionServiceLocator
            .getPSWFActionService();

      List<IPSWorkflowAction> actionList = wfActionService.getActions(
            STANDARD_WORKFLOW, RETURN_TO_PUBLIC_TRANSITION);

      assertEquals(
            "Found the wrong number of actions on Standard Workflow " +
            "\"Return To Public\" Transition\n" + "Found " + 
            actionList.size() + " expected 1",
            actionList.size(), 1);

      String name = actionList.get(0).getClass().getSimpleName();

      assertEquals("Action is " + name + " not " + PS_TOUCH_PARENT_ITEMS,
            name, PS_TOUCH_PARENT_ITEMS);

      actionList = wfActionService.getActions(STANDARD_WORKFLOW,
            MOVE_TO_QUICK_EDIT_TRANSITION);

      assertEquals(
            "Found the wrong number of actions on Standard Workflow " + 
            "\"Move To Quick Edit\" Transition\n" + "Found " +
            actionList.size() + " expected 1",
            actionList.size(), 0);
   }

   /**
    * Test method for
    * {@link PSWFActionService#getWorkflowAction(String)}
    * .
    * 
    * @throws PSNotFoundException
    * @throws PSExtensionException
    */
   public void testGetWorkflowAction()
      throws PSExtensionException, PSNotFoundException
   {
      IPSWFActionService wfActionService = PSWFActionServiceLocator
            .getPSWFActionService();

      IPSWorkflowAction action = wfActionService
            .getWorkflowAction(SYS_TOUCH_PARENT_ITEMS);

      assertNotNull(PS_TOUCH_PARENT_ITEMS + " was not found!", action);

      String name = action.getClass().getSimpleName();

      assertEquals("Action is " + name + " not " + PS_TOUCH_PARENT_ITEMS,
            name, PS_TOUCH_PARENT_ITEMS);

      action = wfActionService.getWorkflowAction(SYS_WORKFLOW_ACTION_DISPATCHER);

      assertNotNull(PS_SPRING_WORKFLOW_ACTION_DISPATCHER + " was not found!", action);

      name = action.getClass().getSimpleName();

      assertEquals("Action is " + name + " not " + PS_SPRING_WORKFLOW_ACTION_DISPATCHER,
            name, PS_SPRING_WORKFLOW_ACTION_DISPATCHER);
   }

   private static final int STANDARD_WORKFLOW = 5;

   private static final int MOVE_TO_QUICK_EDIT_TRANSITION = 9;

   private static final int RETURN_TO_PUBLIC_TRANSITION = 11;

   private static final String PS_TOUCH_PARENT_ITEMS = "PSTouchParentItems";

   private static final String SYS_TOUCH_PARENT_ITEMS = "sys_TouchParentItems";
   
   private static final String PS_SPRING_WORKFLOW_ACTION_DISPATCHER = "PSSpringWorkflowActionDispatcher";

   private static final String SYS_WORKFLOW_ACTION_DISPATCHER = "sys_WorkflowActionDispatcher";
}
