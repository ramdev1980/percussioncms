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

package com.percussion.rest.extensions;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "ExtensionParameter")
@JsonInclude(JsonInclude.Include.NON_NULL)
@ApiModel(description="Represents an Extension Parameter with value")
public class ExtensionParameter {

    @ApiModelProperty(value="name", required=true,notes="The name of the Extension Parameter")
    private      String          name = "";

    @ApiModelProperty(value="description", required=false,notes="The description of the Extension Parameter")
    private      String          description = "";

    @ApiModelProperty(value="dataType", required=false,notes="The Data Type of the Extension Parameter")
    private      String          dataType = "";

    @ApiModelProperty(value="required", required=false,notes="When true, indicates that this is a required parameter for the Extension")
    private boolean required;

    @ApiModelProperty(value="value", required=false,notes="The current value of the type in String form. Use the data type for client side type conversion. May be null or empty.")
    private String value;

    public ExtensionParameter(){}

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

    public String getDataType() {
        return dataType;
    }

    public void setDataType(String dataType) {
        this.dataType = dataType;
    }

    public String getValue(){
        return value;
    }

    public void setValue(String value){
        this.value = value;
    }
}
