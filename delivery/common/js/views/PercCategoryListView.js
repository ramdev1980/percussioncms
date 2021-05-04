
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
 *      https://www.percusssion.com
 *
 *     You should have received a copy of the GNU Affero General Public License along with this program.  If not, see <https://www.gnu.org/licenses/>
 */

/**
 * The delivery side category list view class. Makes a call to the service to get page list entries
 * and renders them in the list.
 * On document ready loops through all perc-category-list elements on the page and finds the query from the data attribute
 * on them. Passes the query and gets the entries from the service. If service returns an error, logs the error and
 * does nothing.
 */
var strJSON;
var pageResult;
var baseURL;
(function($)
{
    $(document).ready(function(){
        $.PercCategoryListView.updateCategoryList();
    });
    $.PercCategoryListView = {
        updateCategoryList : updateCategoryList
    };
    let pageResult;
    let baseURL;
    let strJSON;
    let nRow;
    function updateCategoryList()
    {
        $(".perc-category-list").each(function(){
            let currentCategoryList = $(this);
            if ("" === currentCategoryList.attr("data-query")) {return;}
            let queryString = JSON.parse( currentCategoryList.attr("data-query"));
            //Get the result page to display the pages for each category
            pageResult = queryString.category_page_result;
            delete queryString.category_page_result;
            //Get the display option (Expanded hierarchy = "expanded", Collapsible = "collapsible")
            let display = queryString.display_option;
            delete queryString.display_option;
            //Get the max categories option
            //set the tree pre-expanded level for Collapsible option or the row number to show for the Expanded hierarchy
            let maxCat = queryString.max_categories;
            delete queryString.max_categories;
            let isEditMode = queryString.isEditMode;
            //Set the base URL to create the href for each item then
            baseURL = "";
            if(isEditMode==="true"){
                let paths = window.location.pathname.split("/");
                baseURL = "/" + paths[1] + "/" + paths[2];
            }else{
                baseURL = window.location.protocol + '//' + window.location.host;
            }
            strJSON = JSON.stringify(queryString);
            nRow = 0;

            $.PercCategoryListService.getCategories(queryString, function(status, categoryEntries){

                categoryEntries = $.PercServiceUtils.toJSON(categoryEntries);


                if(status && 0 < categoryEntries.length)
                {
                    //Clean the HTML generated by CMS
                    currentCategoryList.find(".perc-category-list-expanded,.perc-category-list-collapsible").empty();
                    //Destroy previous element of Dynatree.
                    currentCategoryList.find(".perc-category-list-collapsible").dynatree("destroy");

                    var ul;
                    if ("collapsible" === display)
                    {
                        //Collapsible Tree
                        ul = createNestedUlLiTreeCollapsible(categoryEntries, "", maxCat, 0);
                        currentCategoryList.find(".perc-category-list-collapsible").html(ul);
                        currentCategoryList.find(".perc-category-list-collapsible").dynatree({
                            minExpandLevel: 2,
                            onActivate : function(node){
                                var href = $(node.data.title).attr("href");
                                var count = $(node.data.title).attr("data-count");
                                if(0 < count) {
                                    window.location.href = href;
                                }
                            }
                        });
                    }
                    else{
                        //Full expanded Tree
                        ul = createNestedUlLiTreeExpanded(categoryEntries, "", maxCat, 0);
                        currentCategoryList.find(".perc-category-list-expanded").html(ul);
                    }

                    //currentCategoryList.find(".perc-category-list-expanded,.perc-category-list-collapsible").show();
                }
                else if(status && 0 === categoryEntries.length)
                {
                    /*
                    $(".perc-category-list-static ul")
                        .dynatree({onActivate : function(node){
                            var href = $(node.data.title).attr("href");
                            window.location.href = href;
                        }});
                    */
                }
                else
                {
                    //Log the error and leave the original list entries as is
                    console.error(status);
                }
            });
        });
    }

    //Generate the structure for the Full expanded tree using UL and LI
    function createNestedUlLiTreeExpanded(treeNodes, parentPath, maxRow, nLevel){
        var ul = "";
        if (maxRow > nRow || 0 == maxRow){
            ul = $("<ul>")
                .addClass("perc-category-elements")
                .addClass("perc-category-level" + nLevel);
            for(var n=0; n<treeNodes.length; n++){
                if (maxRow > nRow || 0 == maxRow){
                    var node = treeNodes[n];
                    node.path = parentPath + "/" + node.category;
                    var li = parseNode(node, nLevel);
                    ul.append(li);
                    nRow++;
                    if(node.children && 0 < node.children.length){
                        li.append(createNestedUlLiTreeExpanded(node.children, node.path, maxRow, nLevel + 1));
                    }
                }
            }
        }
        return ul;
    }

    //Generate the structure for the Collapsible Tree using dynaTree plugin
    function createNestedUlLiTreeCollapsible(treeNodes, parentPath, maxRow, nLevel){
        var ul;
        ul = $("<ul>")
            .addClass("perc-category-elements")
            .addClass("perc-category-level" + nLevel);

        for(var n=0; n<treeNodes.length; n++){
            if ((1 !== nLevel || n < maxRow) || 0 == maxRow){
                var node = treeNodes[n];
                node.path = parentPath + "/" + node.category;
                var li = parseNode(node);
                ul.append(li);
                if(node.children && 0 < node.children.length){
                    li.append(createNestedUlLiTreeCollapsible(node.children, node.path, maxRow, nLevel + 1));
                }
            }
        }
        return ul;
    }

    //Create the LI node from a Category.
    function parseNode(node) {
        let countTotal = node.count.first;
        let nodeStr = node.category;
        if (countTotal > 0)
            nodeStr = node.category + " (" + countTotal + ")";

        let query = JSON.parse(strJSON );
        query.criteria.push("perc:category LIKE '" + node.path + "%'");
        let encodedQuery = "&query=" + encodeURIComponent(JSON.stringify(query));

        let href = "#";
        if ("undefined" !== typeof (pageResult) && "" !== pageResult) {
            href = baseURL + pageResult + "?filter=" + node.category + encodedQuery;
        }

        let a = $("<a>")
            .attr("href", href)
            .attr("data-count", countTotal)
            .attr("title", nodeStr)
            .addClass("perc-node")
            .append(nodeStr);

        return $("<li>")
            .addClass("perc-category-element")
            .append(a);
    }

})(jQuery);