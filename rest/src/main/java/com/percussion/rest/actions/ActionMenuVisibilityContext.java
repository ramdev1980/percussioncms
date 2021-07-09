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

import io.swagger.annotations.ApiModel;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "ActionMenuVisibilityContext")
@ApiModel(description = "Represents a Visibility Context for an Action Menu")
public class ActionMenuVisibilityContext {

    private String name;
    private String description;
    private String values;
    private UIContext uiContext;


    public ActionMenuVisibilityContext(){}

    public UIContext getUiContext() {
        return uiContext;
    }

    public void setUiContext(UIContext uiContext) {
        this.uiContext = uiContext;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getValue() {
        return values;
    }

    public void setValue(String values) {
        this.values = values;
    }
}
