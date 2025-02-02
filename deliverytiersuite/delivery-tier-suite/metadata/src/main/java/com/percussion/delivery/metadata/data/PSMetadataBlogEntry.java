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
package com.percussion.delivery.metadata.data;

import java.util.HashSet;
import java.util.Set;

/**
 * Represents a hashSet of {@link PSMetadataBlogYear} instances.
 *
 * @author leonardohildt
 * 
 */
public class PSMetadataBlogEntry
{
    private Set<PSMetadataBlogYear> years;

    public PSMetadataBlogEntry()
    {
        super();
        this.years = new HashSet<>();
    }

    /**
     * @return the years
     */
    public Set<PSMetadataBlogYear> getYears()
    {
        return years;
    }

    /**
     * @param years the years to set
     */
    public void setYears(Set<PSMetadataBlogYear> years)
    {
        this.years = years;
    }

}
