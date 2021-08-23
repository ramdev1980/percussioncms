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

package com.percussion.rest.actions;


import com.percussion.rest.Guid;
import com.percussion.util.PSSiteManageBean;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriInfo;
import java.util.Arrays;
import java.util.List;

@PSSiteManageBean(value="restActionMenuResource")
@Path("/actions")
@Tag(name = "Action Menu", description = "Action Menu operations")
public class ActionMenuResource {

    private static final Logger log = LogManager.getLogger(ActionMenuResource.class);

    @Autowired
    private IActionMenuAdaptor adaptor;

    @Context
    private UriInfo uriInfo;

    public ActionMenuResource(){}

    @GET
    @Path("/find")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Finds action menus", description = "Returns a list of Action Menus that matches the criteria."
            , responses= {
            @ApiResponse(responseCode="200", description = "OK"
                    , content = @Content(array = @ArraySchema(schema=@Schema(implementation = ActionMenu.class)))),
            @ApiResponse(responseCode="404", description = "No Action Menu's found"),
            @ApiResponse(responseCode = "500", description = "Error searching for Action Menu")
    })
    public List<ActionMenu> findActions(@Parameter(name = "name",required = false) @QueryParam("name") String name,
                                        @Parameter(name="label", required= false) @QueryParam("label") String label,
                                        @Parameter(name="dynamic", required=false) @QueryParam("dynamic") Boolean dynamic,
                                        @Parameter(name="item", required=false) @QueryParam("item") Boolean item,
                                        @Parameter(name="cascading", required=false) @QueryParam("cascading") Boolean cascading
                                        ) {

        if(StringUtils.isEmpty(name))
            name=null;

        if(StringUtils.isEmpty(label))
            label=null;
        List<ActionMenu> ret=null;

        try {
           ret = adaptor.findMenus(name, label, item, dynamic, cascading);
        }catch(Exception e){
            log.error(e);
        }
       return ret;
    }

    public ActionMenu[] loadActions(List<Guid> guids){
        return null;
    }

    @POST
    @Operation(description = "Accepts an object with an array of contentid's and assignment types and returns the allowed menus")
    public ActionMenuList getAllowedTransitions(AllowedWorkflowTransitionsRequest request){
        return null;
    }


    @POST
    @Path("/find/types")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Finds action menus by content type", description = "Returns a list of Action Menus that matches the criteria. ActionId should be ignored for these menus."
            , responses= {
            @ApiResponse(responseCode="200", description = "OK"
            , content = @Content(array = @ArraySchema(schema=@Schema(implementation = ActionMenu.class)))),
            @ApiResponse(responseCode="404", description = "No Action Menu's found"),
            @ApiResponse(responseCode = "500", description = "Error searching for Action Menu")
    })
    public ActionMenuList getAllowedContentTypeMenus(AllowedContentTypeMenusRequest request){
        return new ActionMenuList(adaptor.findAllowedContentTypes( Arrays.stream( request.getContentIds() ).boxed().toArray( Integer[]::new )));
    }

    @GET
    @Path("/find/templates/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary="Returns a list of allowed Templates for the given content id",
            description="ActionId should be ignored for these menus." ,
            responses ={
                    @ApiResponse(responseCode="200", description = "Success",
                    content=@Content(array = @ArraySchema(schema=@Schema(implementation = ActionMenu.class)))),
                    @ApiResponse(responseCode="404", description = "No Action Menu's found"),
                    @ApiResponse(responseCode = "500", description = "Error searching for Action Menu")
    })
    public ActionMenuList getAllowedTemplateMenus(    @Parameter(required = true, description = "The content id to retrieve template URLS for.")
                                                      @PathParam(value = "id") int contentId,
                                                      @Parameter(description="Set to true to include AA menus.")
                                                      @QueryParam(value="isAA") boolean isAA){
        return new ActionMenuList(adaptor.findAllowedTemplates(contentId, isAA));

    }
}
