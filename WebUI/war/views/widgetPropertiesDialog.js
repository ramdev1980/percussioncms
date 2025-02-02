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

//Note; this view was created to edit user properties initially, but has been changed it for editing any properties that
// are of type AbstractUserPref, some of the variable and function names in this file may refer to user pref, treat it as any pref.
//@TODO rename the variable and method names.
function countProperties(obj) {
    var count = 0;
    for(var prop in obj) {
        if(obj.hasOwnProperty(prop))
            ++count;
    }
    return count;
}

(function($, P) {
    P.widgetPropertiesDialog = function( setWidgetProperty, widgetProperties, widgetDefinitionId, postCallback, propertyType, getWidgetByName ) {
        $.perc_widget_definition_client.restGetWidgetDefinition(widgetDefinitionId, propertyType, function(widgetDef) {
            if (widgetProperties){
                if (typeof(widgetProperties.sys_perc_name) !== "undefined" && typeof(widgetProperties.sys_perc_description) !== "undefined" ){
                    widgetDef.userPrefDef.sys_perc_name = (new $.perc_sys_pref("perc_sys_name","","Name","sys_perc_name"));
                    widgetDef.userPrefDef.sys_perc_description = (new $.perc_sys_pref("perc_sys_description","","Description","sys_perc_description"));
                }
                widgetDef.setValuesFromWidgetProperties( widgetProperties );
            }

            var dialogOptions = {
                modal : true,
                width : 500,
                title : I18N.message("perc.ui.widget.properties.dialog@Configure Widget Properties"),
                zIndex : 100000,
                percButtons : {
                    'Ok' : {click: saveProperties, id: 'perc-widget-properties-ok'},
                    'Cancel': {click: function(){ $(this).remove(); } , id:'perc-widget-properties-cancel'}
                },
                id: "perc_edit_widget_properties"
            };
            var dialog = $('#perc_edit_widget_properties');
            //If there are no field groups then add height accordingly.
            var numOfFields = countProperties(widgetDef.userPrefDef);
            var dlgHeight = "auto";
            if (numOfFields > 10)
                dlgHeight = "700";
            if ($('#perc_edit_widget_properties > #perc-section-system-container').length === 0)
                dialogOptions.height = dlgHeight;
            $("<div/>").append( widgetDef.render() ).perc_dialog(dialogOptions);
            _addFieldGroups();
        });

        function saveProperties(){

            var requiredFieldsValid = _checkRequiredFields(this);

            // Only process the form if all required fields are entered
            if(requiredFieldsValid === true) {
                //Check the uniqueness of the widget name.
                var widget = $(this).find('[name=sys_perc_name]');
                var widgetName = widget.val();

                if(typeof widgetName !== "undefined")
                    widgetName = widgetName.trim().toUpperCase();

                var originalName = widget.attr('data-perc-original-value');
                if(typeof originalName !== "undefined")
                    originalName = originalName.trim().toUpperCase();

                if (typeof(widgetName) !== "undefined" && widgetName !== "" && widgetName !== originalName && getWidgetByName(widgetName) !== null ){
                    $.perc_utils.alert_dialog({title: I18N.message("perc.ui.publish.title@Error"), content: I18N.message("perc.ui.widget.properties.dialog@Widget Name") + widgetName + I18N.message("perc.ui.widget.properties.dialog@Widget Name Already Used")});
                    return;
                }

                $(this).find('input').each( function(){
                    var value = $(this).val().trim();
                    if($(this).attr('type')==='checkbox')
                    {
                        value = $(this).is(':checked')?'true':'false';
                    }
                    setWidgetProperty( $(this).attr('name'), value );
                });
                $(this).find('select').each(function(){
                    setWidgetProperty( $(this).attr('name'), $(this).val() );
                });
                $(this).find('textarea').each(function(){
                    setWidgetProperty( $(this).attr('name'), $(this).val() );
                });

                postCallback();
                $(this).remove();
            }

            else {
                $('#percRequiredFieldWarning').text(I18N.message("perc.ui.general@Required Fields Warning"));
            }

        }

        // A private helper method to check required fields
        function _checkRequiredFields(formObject) {
            var isValid = true;
            $(formObject).find('input,textarea,select').each(function(){
                if($(this).prop('required') && $(this).val() === ''){
                    isValid = false;
                    return false;
                }
            });
            return isValid;
        }

        // A private helper method to group the fields and create collapsible sections
        function _addFieldGroups() {
            var dialog = $('#perc_edit_widget_properties');

            //Identify is we will use grouping style or not.
            if (dialog.find('#perc-section-system-container').length > 0)
                dialog.find('.ui-dialog-content.ui-widget-content').addClass('group-style');

            var fieldGroups = [
                { groupName : "perc-section-system-container", groupLabel : I18N.message("perc.ui.widget.properties.dialog@Widget Summary")},
                { groupName : "perc-section-properties-container", groupLabel : I18N.message("perc.ui.widget.properties.dialog@Properties")}
            ];
            $.each(fieldGroups, function(index) {
                // Create HTML markup with the groupName minimizer/maximizer and
                // insert it before the 1st field in each group
                var minmaxClass = (index === 0) ? "perc-items-minimizer" : "perc-items-maximizer";
                var groupHtml =
                    "<div class='perc-section-header'>" +
                    "<div class='perc-section-label' data-perc-group-name='" + this.groupName + "'>" +
                    "<span  class='perc-min-max " + minmaxClass + "' ></span>" + this.groupLabel +
                    "</div>" +
                    "</div>";
                dialog.find('#' + this.groupName).before(groupHtml);
                // The first group will be the only one expanded (hide all others)
                if(index !== 0){
                    dialog.find('#' + this.groupName).hide();
                }
            });

            // Bind collapsible event
            dialog.find(".perc-section-label").off("click").on("click",function() {
                var self = $(this);
                self.find(".perc-min-max")
                    .toggleClass('perc-items-minimizer')
                    .toggleClass('perc-items-maximizer');
                dialog.find('#' + self.attr('data-perc-group-name')).toggle();
            });
        }
    };
    /**
     *  Widget Definition Model.
     *  Represents a set of widget user preference properties
     */
    $.perc_widget_definition_model = function()
    {
        // original xml coming from service
        // will be parsed out into an array of user preferences
        this.widgetDefinitionXml = null;
        // contains all the user preferences for this type of widget
        this.userPrefDef = null;

        this.setValuesFromWidgetProperties = function(widgetProperties)
        {
            for(let p in this.userPrefDef)
            {
                var propertyName = this.userPrefDef[p].name;
                this.userPrefDef[p].realValue = widgetProperties[propertyName];
            }
        };

        // getter for userPrefDef
        this.getUserPrefDef = function()
        {
            return this.userPrefDef;
        };

        // initialize widget definition model
        this.init = function(xml, propertyType)
        {
            var self = this;
            // store original xml
            this.widgetDefinitionXml = xml;
            // convert xml to a jQuery object so we can traverse
            var $widgetDefinitionXml = $(this.widgetDefinitionXml);
            // get all the User Pref elements
            var $userPreferences = $widgetDefinitionXml.find(propertyType);
            //Initialice userPref Object
            this.userPrefDef = {};

            //finish if we dont have userPref to process
            if($userPreferences.length === 0)
                return;

            // iterate over all the User Pref elements
            $userPreferences.each(function()
            {
                // get the attributes common to all User Prefs
                var datatype = $(this).attr('datatype');
                var default_value = $(this).attr('default_value');
                var display_name = $(this).attr('display_name');
                var name = $(this).attr('name');
                var required_field = $(this).attr('required');
                var enumValues = null;

                // if its enum get the Enum Values
                if(datatype === '' || datatype == null)
                {
                    datatype = 'string';
                }
                else if(datatype === 'enum')
                {
                    enumValues = {};
                    // iterate over all the Enum Values
                    $(this).find('EnumValue').each(function()
                    {
                        // get the attributes for each enumerated value
                        var display_value = $(this).attr('display_value');
                        var value = $(this).attr('value');
                        enumValues[value] = display_value;
                    });
                }

                // create an instance of the user preferences and add them to the model in an array
                self.userPrefDef[name] = (new $.perc_user_pref
                (
                    datatype,
                    default_value,
                    display_name,
                    name,
                    required_field,
                    enumValues
                ));
            });
        };

        // generates an HTML table with property names and input fields
        // for the user prefs depending on their datatype
        this.render = function()
        {
            var html = $("<div/>");
            var systemContainer = $("<div id='perc-section-system-container' />");
            var propertiesContainer = $("<div id='perc-section-properties-container' />");
            var sysProperties = "";
            var properties = "";
            for(let u in this.userPrefDef)
            {
                if((this.userPrefDef[u].name === "sys_perc_name" || this.userPrefDef[u].name === "sys_perc_description") &&
                    (this.userPrefDef[u].datatype === "perc_sys_name" || this.userPrefDef[u].datatype === "perc_sys_description"))
                    sysProperties += "\t"+this.userPrefDef[u].render();
                else
                    properties += "\t"+this.userPrefDef[u].render();
            }
            if (sysProperties !== "" && properties !== ""){
                systemContainer.append($(sysProperties));
                html.append($("<div class='fieldGroup' />").append(systemContainer));
                propertiesContainer.append($(properties));
                html.append($("<div class='fieldGroup' />").append(propertiesContainer));
            }
            else if (properties === "")
                html.append($(sysProperties));
            else
                html.append($(properties));

            // Prepend required field warning
            html.prepend('<div id="percRequiredFieldWarning"><p>&nbsp;</p></div>');

            // Append required field key only if there is a required field in the widget
            if($(html).find('input,textarea,select').filter('[required]').length > 0) {
                html.append('<div><p>' + I18N.message("perc.ui.general@Denotes Required Field") + '</p></div>');
            }

            $.perc_filterField(html.find('[name=sys_perc_name]'), $.perc_textFilters.ID_WITH_SPACE);
            $.perc_filterField(html.find('[name=sys_perc_description]'), $.perc_textFilters.DESCRIPTION);
            return html;
        };
    };
    /**
     *  Widget Definition Client
     *  Retrieves widget definition from REST Service and creates a widget definition model
     */

    $.perc_widget_definition_client = new function()
    {
        this.restGetWidgetDefinition = function(widgetDefinitionId, propertyType, callback)
        {
            $.ajax(
                {
                    headers: {
                        'Accept': 'application/xml',
                        'Content-Type': 'application/xml'
                    },
                    url: $.perc_paths.WIDGET_FULL + "/" + widgetDefinitionId,
                    type: "GET",
                    success: function(xml, textstatus)
                    {
                        var model = new $.perc_widget_definition_model(xml);
                        model.init(xml, propertyType);
                        callback(model);
                    },
                    error : function()
                    {
                        alert(I18N.message("perc.ui.widget.properties.dialog@Unable To Retrieve Widget Definition") + widgetDefinitionId);
                    }
                });
        };

        this.restGetWidgetPrefs = function(widgetDefinitionId, callback)
        {
            $.ajax(
                {
                    headers: {
                        'Accept': 'application/xml',
                        'Content-Type': 'application/xml'
                    },
                    url: $.perc_paths.WIDGET_FULL + "/" + widgetDefinitionId,
                    type: "GET",
                    success: function(xml, textstatus)
                    {
                        var $widgetDefinitionXml = $(xml);
                        var $widgetPrefs = $widgetDefinitionXml.find("WidgetPrefs");
                        callback($widgetPrefs);
                    },
                    error : function()
                    {
                        alert(I18N.message("perc.ui.widget.properties.dialog@Unable To Retrieve Widget Definition") + widgetDefinitionId);
                    }
                });
        };
    };

    $.perc_sys_pref = function(datatype, default_value, display_name, name, required_field)
    {
        this.datatype = datatype;
        this.default_value = default_value;
        this.display_name = display_name;
        this.name = name;
        this.required_field = required_field;
        this.realValue = null;

        this.render = function()
        {
            var buff = "";
            if(datatype === 'perc_sys_name'){
                var value = (typeof(this.realValue) != "undefined") ? this.realValue : this.default_value;
                buff =  '<tr>\n';
                buff += '   <td><label for="' + this.name + '">' + this.display_name+'</label>: </td>\n'+ '</tr>\n';
                buff += '   <td><input class="perc-widget-property" name="'+this.name+'" type="text" value="'+value+'" data-perc-original-value="'+value+'" maxlength="30"></td>\n'+ '</tr>\n';
                buff += '</tr>\n';
                return buff;
            }

            if(datatype === 'perc_sys_description'){
                let value2 = (typeof(this.realValue) != "undefined") ? this.realValue : this.default_value;
                buff =  '<tr>\n';
                buff += '   <td><label for="' + this.name + '">' + this.display_name+'</label>: </td>\n'+ '</tr>\n';
                buff += '   <td><textarea style="resize: none; border: 1px inset #F0F0F0; padding: 2px; height: 40px" class="perc-widget-property" name="'+this.name+'" type="text" maxlength="100">' + value2 + '</textarea></td>\n'+ '</tr>\n';
                buff += '</tr>\n';
                return buff;
            }

            return buff;
        };
    };

    $.perc_user_pref = function(datatype,default_value,display_name,name,required_field,enumValues)
    {
        this.datatype = datatype;
        this.default_value = default_value;
        this.display_name = display_name;
        this.name = name;
        this.required_field = required_field;
        this.enumValues = enumValues;
        this.realValue = null;

        if(this.required_field === 'true') {
            this.display_name_prepend = '* ';
            this.required_attr = 'aria-required="true" required';
            this.required_class = ' class="perc-required-field" ';
        }
        else {
            this.display_name_prepend = '';
            this.required_attr = '';
            this.required_class = '';
        }

        // generates HTML table row with property name and input field
        // for this user pref's datatype
        this.render = function()
        {

            var buff = '<tr>\n';
            let datatype2 = this.datatype;
            let value2;

            if(typeof(this.realValue)!== 'boolean')
            {
                if(this.realValue === undefined)
                {
                    value2 = this.default_value;
                } else
                {
                    value2 = (this.realValue === "0") ? this.default_value : this.realValue;
                }
            } else
            {
                if(this.realValue === undefined)
                {
                    value2 = this.default_value;
                } else
                {
                    value2 = this.realValue;
                }
            }

            if (value2 === undefined)
                value2 = "";

            if(datatype2 === 'string' || datatype2 === 'number' )
            {
                buff += '   <td><label' + this.required_class + 'for="' + this.name + '">' + this.display_name+'</label>: </td>\n'+ '</tr>\n';
                buff += '   <td><input class="perc-widget-property" name="'+this.name+'" type="text" value="'+value2+'"' + this.required_attr +'></td>\n'+ '</tr>\n';
            }
            else if(datatype2 === 'bool' )
            {
                var checked = (value2 === true || value2 === 'true' || value2 === 'on')?'CHECKED ':'';
                buff += '   <td class = "checkbox-size"><input class="perc-widget-property" name="'+this.name+'" type="checkbox" '+ checked +this.required_attr+'> <label for="' + this.name + '">' + this.display_name+'</label></td>\n'+ '</tr>\n';

            }
            else if(datatype2 === 'enum')
            {
                buff += '   <td><label' + this.required_class + 'for="' + this.name + '">' + this.display_name+'</label>: </td>\n'+ '</tr>\n';
                buff += '   <td>\n';
                buff += '   <select class="perc-widget-property" name="'+this.name+ '" ' +this.required_attr+'>\n';
                var selected='';
                var pickDefault = false;
                if(!enumValues.hasOwnProperty(value2)) {
                    pickDefault = true;
                }
                for (let v in enumValues) {

                    if(pickDefault){
                        selected = (v===this.default_value) ? 'selected' :'';
                    }else{
                        selected = (v === value2) ? 'selected' : '';
                    }

                    buff += '       <option value="' + v + '" ' + selected + '> ' + enumValues[v] + '   </option>\n';
                }

                //If no option was selected and value2 has a value then select the default value
                if(selected === '' && value2 != ''){

                }
                buff += '   </select>\n';
                buff += '</td>\n'+ '</tr>\n';
            }
            else if(datatype2 === 'list')
            {
                var values = JSON.parse(value);

                buff += '   <td><label' + this.required_class + 'for="' + this.name + '">' + this.display_name+'</label>: </td>\n'+ '</tr>\n';
                buff += '   <td><textarea class="perc-widget-property" name="'+this.name+'">\n';
                for(let v in values)
                {
                    buff += values[v]+'\n';
                }
                buff += '</textarea>\n';
            }
            buff += '</tr>\n';
            return buff;

        };
    };

})(jQuery, jQuery.Percussion);
