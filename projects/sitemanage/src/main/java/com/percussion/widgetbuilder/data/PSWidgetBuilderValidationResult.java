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
package com.percussion.widgetbuilder.data;

import javax.xml.bind.annotation.XmlRootElement;

import org.apache.commons.lang.Validate;

/**
 * @author JaySeletz
 *
 */
@XmlRootElement(name="WidgetBuilderValidationResult")
public class PSWidgetBuilderValidationResult
{
    String category;
    String name;
    String message;
    
    public PSWidgetBuilderValidationResult()
    {
        
    }
    
    /**
     * @param category
     * @param name
     * @param message
     */
    public PSWidgetBuilderValidationResult(String category, String name, String message)
    {
        Validate.notEmpty(category);
        Validate.notEmpty(name);
        Validate.notEmpty(message);
        
        this.category = category;
        this.name = name;
        this.message = message;
    }

    public String getCategory()
    {
        return category;
    }

    public String getName()
    {
        return name;
    }

    public String getMessage()
    {
        return message;
    }

    public void setCategory(String category)
    {
        this.category = category;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public void setMessage(String message)
    {
        this.message = message;
    }

    public enum ValidationCategory
    {
        GENERAL,
        CONTENT,
        RESOURCES,
        DISPLAY
    }
}
