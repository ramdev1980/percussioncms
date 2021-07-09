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

package com.percussion.contentmigration.rules;

import org.jsoup.nodes.Document;

public interface IPSContentMigrationRule
{
    /**
     * Finds the matching content based on the rule implementation and returns it, if the content is not found returns <code>null</code>, so that
     * other rules can be applied to match the content.
     * @param widgetId must not be <code>null</code>.
     * @param sourceDoc must not be <code>null</code>, either a rendered page document or rendered template document if the page doesn't exist.
     * @param targetDoc must not be <code>null</code>, the target page document.
     * @return String matched content or <code>null</code> if not found.
     */
    String findMatchingContent(String widgetId, Document sourceDoc, Document targetDoc);
    
    static String ATTR_WIDGET_ID = "widgetid";
    static String CLASS_PERC_REGION = "perc-region";
    static String PERC_CLASS_PREFIX = "perc-";
}
