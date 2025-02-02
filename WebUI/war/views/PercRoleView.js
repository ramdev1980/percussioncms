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

/**
 * PercRoleView.js
 * @author Luis Mendez
 *
 */
(function($) {

    var dirtyController = $.PercDirtyController;

    $.PercRoleView = function() {

        var viewApi = {
            init                   : init,
            updateListOfRoles      : updateListOfRoles,
            updateRoleNameField    : updateRoleNameField,
            updateAssignedUsers    : updateAssignedUsers,
            updateDescriptionField : updateDescriptionField,
            updateHomepageField    : updateHomepageField,
            resetRoleDetails       : resetRoleDetails,
            selectRole             : selectRole,
            showSelectedRoleEditor : showSelectedRoleEditor,
            showNewRoleEditor      : showNewRoleEditor,
            alertDialog            : alertDialog,
            getDescription         : getDescription
        }

        // A snippet to adjust the frame size on resizing the window.
        $(window).on("resize",function() {
            fixIframeHeight();
            fixTemplateHeight();
        });

        var container = $("#perc-roles-list");
        var editingRole = false;
        var addingRole = false;
        var deletingRole = false;
        var controller = $.PercRoleController;
        controller.init(viewApi);

        /**
         * Confirm dialog to delete Role
         */
        function deleteRole(rolename){
            // retrieve assigned users
            var assignedUsers = new Array();
            $(".perc-roles-assigned-users-list span").each(function() {
                assignedUsers.push($(this).html());
            });
            var roleObj = {"Role":{"name":rolename, "description":getDescription, "users":assignedUsers}};
            controller.validateAndDeleteRole(roleObj);
        }

        function init() {
            resetRoleDetails();
            $("#perc-roles-edit-role-button").off("click").on("click", function(evt){
                editingRole = true;
                controller.editSelectedRole();
                disableButtons();
                unhighlightAllUsers();
            });
			$("#perc-roles-edit-role-button").off("keydown").on("keydown", function(evt){
               if(evt.code == "Enter" || evt.code == "Space"){
						document.activeElement.click();
				}
            });

            //Bind Add Users to Role event
			 $(".perc-roles-addusers-button").off("keydown").on("keydown",
                function(evt){
                    if(evt.code == "Enter" || evt.code == "Space"){
						document.activeElement.click();
					}
                });

            $(".perc-roles-addusers-button").off("click").on("click",
                function(evt){
                    addUsers(evt);
                });
            //Bind remove Users from Role event
            $(".perc-roles-removeusers-button").off("click").on("click",
                function(evt){
                    removeUsers(evt);
                });
			$(".perc-roles-removeusers-button").off("keydown").on("keydown",
                function(evt){
                    if(evt.code == "Enter" || evt.code == "Space"){
						document.activeElement.click();
					}
                });

            //Bind Save event
            $("#perc-roles-save").off("click").on("click", function(evt){
                save();
            });

            //Bind Cancel event
            $("#perc-roles-cancel").off().on("click", function(evt){
                controller.cancel();
            });

            var config = {
                listItem: [],
                title: I18N.message("perc.ui.perc.role.view@Roles"),
                addTitle: I18N.message("perc.ui.perc.role.view@Add Role"),
                deleteTitle: I18N.message("perc.ui.perc.role.view@Delete Role"),
                //enableDelete: false,
                createItem: addNewRole,
                deleteItem: deleteRole,
                selectedItem: controller.selectRole
            }

            $.PercDataList.init(container, config);
        }

        function addUsers(event){
            var assignedUsers = [];
            $(".perc-roles-assigned-users-list span").each(function() {
                assignedUsers.push($(this).html());
            });
            controller.getAvailableUsers(assignedUsers);
        }

        function removeUsers(event){
            if (!addingRole){
                if (!deletingRole) {
                    deletingRole = true;
                    var remainUsers = [];
                    var selectedUsers = [];
                    //Get a list of selected users and the remaining.
                    $(".perc-roles-assigned-users-list li").each(function() {
                        var userRow = $(this);
                        var user = userRow.find("span").html();
                        if (userRow.is(".perc-assigned-user-selected"))
                            selectedUsers.push(user); // users to be validated before delete
                        else
                            remainUsers.push(user);
                    });
                    controller.validateAndRemoveUsers(selectedUsers, remainUsers);
                }
            }
            else{
                $(".perc-roles-assigned-users-list .perc-assigned-user-selected").remove();
                disableRemoveUsers();
            }
        }

        function addNewRole(){
            addingRole = true;
            controller.addNewRole();
            disableButtons();
            disableRemoveUsers();
            $(".perc-required-label").show();
            $("#perc-roles-edit-role-button").hide();
        }

        function resetRoleDetails() {
            $("#perc-orig-roles-name-field").val("");
            $("#perc-roles-name-field").val("");
            $("#perc-roles-description-field").val("");
            $("#perc-roles-homepage-field").val("");
            $(".perc-roles-assigned-users-list").html("");

            $("#perc-roles-name-field")
                .addClass("perc-roles-field-readonly")
                .attr("readonly","readonly");
            $("#perc-roles-description-label").hide();
            $("#perc-role-save-cancel-block").hide();
            $("#perc-roles-edit-role-button").show();
        }

        function updateListOfRoles(roleArray) {
            $.PercDataList.updateList(container, roleArray);
        }

        function showSelectedRoleEditor() {
            if (controller.getSelectedRole() !== "Admin" && controller.getSelectedRole() !== "Designer"){
                $("#perc-roles-name-field")
                    .removeClass("perc-roles-field-readonly")
                    .prop("readonly",false)
                    .on("change",function() {
                        dirtyController.setDirty(true, "role");
                    });
            }

            $("#perc-roles-description-field")
                .removeClass("perc-roles-field-readonly")
                .prop("readonly",false)
                .attr("style", "height: 55px;width: 100%;")
                .on("change",function() {
                    dirtyController.setDirty(true, "role");
                });
            $("#perc-roles-description-label").show();

            $("#perc-roles-homepage-field")
                .show()
                .on("change",function() {
                    dirtyController.setDirty(true, "role");
                });
            $("#perc-roles-homepage-label").show();
            $("#perc-roles-homepage-field-readonly").hide();
            $("#perc-roles-edit-role-button").hide();
            $("#perc-role-save-cancel-block").show();
            $("#perc-roles-description-field").trigger("focus");
        }

        function showNewRoleEditor() {
            $("#perc-roles-name-field")
                .removeClass("perc-roles-field-readonly")
                .prop("readonly",false)
                .val("")
                .trigger("focus")
                .on("change",function() {
                    dirtyController.setDirty(true, "role");
                });
            $("#perc-roles-name-label").addClass("perc-required-field");
            $("#perc-roles-description-field")
                .removeClass("perc-roles-field-readonly")
                .prop("readonly",false)
                .attr("style", "height: 55px;width: 100%;")
                .val("")
                .on("change",function() {
                    dirtyController.setDirty(true, "role");
                });
            $("#perc-roles-description-label").show();

            $("#perc-roles-homepage-field")
                .show()
                .on("change",function() {
                    dirtyController.setDirty(true, "role");
                }).val("Home");
            $("#perc-roles-homepage-label").show();
            $("#perc-roles-homepage-field-readonly").hide();
            unhighlightAllRoles();
            $("#perc-roles-edit-role-button").hide();
            $("#perc-role-save-cancel-block").show();
        }

        function updateRoleNameField(name) {
            var roleOrigNameField = $("#perc-orig-roles-name-field");
            roleOrigNameField.val(name);
            var roleNameField = $("#perc-roles-name-field");
            roleNameField.val(name);
        }

        function updateDescriptionField(description) {
            var descriptionField = $("#perc-roles-description-field");
            descriptionField.val(description);
        }

        function updateHomepageField(homepage) {
            $("#perc-roles-homepage-field").val(homepage);
            $("#perc-roles-homepage-field-readonly").text(homepage);
        }

        /**
         * Update the List of Assigned user of a role
         */
        function updateAssignedUsers(assignedUsersArray) {
            var $assignedUsers = $(".perc-roles-assigned-users-list");
            var currentUser = controller.getCurrentUser();
            var ulUsers = $("<ul class='perc-assigned-users' />");
            $assignedUsers.html("");

            // iterate over the list of users and add it to the user option
            for(i in assignedUsersArray) {
                var userName = assignedUsersArray[i];
                var liUser = $("<li tabindex='0' class='perc-assigned-user-entry'/>")
                    .append(
                        $("<span />")
                            .html(userName)
                            .attr("title", userName)
                    )
                //When Admin role is selected, if the list of users will contain the logged in user, then that user name will be disabled.
                //Note: This ensures there is always at least one user in the Admin role.
                if (controller.getSelectedRole() == "Admin" && (currentUser == userName)){
                    liUser.find("span").css("color", "#000000");
                }
                else{
                    liUser.on("click",
                        function(evt){
                            selectUser.call(this,[evt]);
                        });
					liUser.on("keydown",
                        function(eventHandler){
                            if(eventHandler.code == "Enter" || eventHandler.code == "Space"){
								document.activeElement.click();
							}

                        });
                }
                ulUsers.append(liUser);
            }
            // append html to DOM
            $assignedUsers.append(ulUsers);
        }

        function selectUser(event){
            if (!editingRole){
                var currentUser = controller.getCurrentUser();
                var userRow = $(this);
                var userSelected = userRow.find("span").html();
                if (userRow.is(".perc-assigned-user-selected"))
                    userRow.removeClass("perc-assigned-user-selected");
                else
                    userRow.addClass("perc-assigned-user-selected");
                if($(".perc-roles-assigned-users-list .perc-assigned-user-selected span").length > 0)
                    enableRemoveUsers();
                else
                    disableRemoveUsers();
            }
        }

        /**
         * Unhighlight all roles and
         * Highlight just the current role
         */
        function selectRole(rolename) {
            deletingRole = false;
            if (editingRole || addingRole){
                editingRole = false;
                addingRole = false;
                enableButtons();
                $(".perc-required-label").hide();
                $("#perc-roles-edit-role-button").show();
            }
            if (rolename == "Admin" || rolename == "Designer"){
                $.PercDataList.disableDeleteButton(container);
            }
            else{
                $.PercDataList.enableDeleteButton(container);
            }
            disableRemoveUsers();

            $.PercDataList.selectItem(container, rolename);
            $("#perc-roles-name-field")
                .addClass("perc-roles-field-readonly")
                .attr("readonly","readonly");
            var descriptionLabel = $("#perc-roles-description-label");
            var descriptionField = $("#perc-roles-description-field");
            descriptionField
                .addClass("perc-roles-field-readonly")
                .attr("readonly","readonly")
            if (descriptionField.val() == ""){
                descriptionLabel.hide();
            }else{
                descriptionLabel.show();
            }

            $("#perc-roles-homepage-field").hide();
            $("#perc-roles-homepage-field-readonly").show();
            $("#perc-roles-homepage-label").show();

            $("#perc-roles-name-label").removeClass("perc-required-field");
            $("#perc-roles-edit-role-button").show();
            $("#perc-role-save-cancel-block").hide();
        }

        function unhighlightAllRoles() {
            // unhighlight all roles
            $("#perc-rolename-list ul li")
                .css("background-color","")
                .removeClass("perc-roles-selected");
            $("#perc-rolename-list ul li div").css("background-color","");
        }

        function unhighlightAllUsers() {
            // unhighlight all users
            $(".perc-roles-assigned-users-list .perc-assigned-user-selected")
                .removeClass("perc-assigned-user-selected")
        }

        /**
         * On Edit or Create we disable add/delete Role and add/remove users.
         */
        function disableButtons(){
            $.PercDataList.disableButtons(container);
            if (editingRole){
                $(".perc-roles-addusers-button")
                    .addClass("perc-item-disabled")
					.attr("aria-disabled","true")
                    .off();
                disableRemoveUsers();
            }
        }

        function enableButtons(){
            $.PercDataList.enableButtons(container);
            $(".perc-roles-addusers-button")
                .removeClass("perc-item-disabled")
				.attr("aria-disabled","false")
                .off("click")
                .on("click", function(evt){
                    addUsers(evt);
                });
			$(".perc-roles-addusers-button")
                .removeClass("perc-item-disabled")
				.attr("aria-disabled","false")
                .off("keydown")
                .on("keydown", function(evt){
                    if(evt.code == "Enter" || evt.code == "Space"){
						document.activeElement.click();
				}
                });
        }

        function enableRemoveUsers(){
            $(".perc-roles-removeusers-button")
                .removeClass("perc-item-disabled")
				.attr("aria-disabled","false")
                .off("click")
                .on("click", function(evt){
                    removeUsers(evt);
                });
			$(".perc-roles-removeusers-button")
                .removeClass("perc-item-disabled")
				.attr("aria-disabled","false")
                .off("click")
                .on("click", function(evt){
                    if(event.code == "keydown" || event.code == "keydown"){
						document.activeElement.click();
					}
                });
        }

        function disableRemoveUsers(){
            $(".perc-roles-removeusers-button")
                .addClass("perc-item-disabled")
				.attr("aria-disabled","true")
                .off();
        }

        function save() {
            // retrieve assigned users
            var assignedUsers = new Array();
            $(".perc-roles-assigned-users-list span").each(function() {
                assignedUsers.push($(this).html());
            });

            var rolename  = $("#perc-roles-name-field").val().trim();
            var origRoleName  = $("#perc-orig-roles-name-field").val().trim();

            var description = $("#perc-roles-description-field").val();
            var homepage = $("#perc-roles-homepage-field").val();
            controller.save(rolename, origRoleName, description, homepage, assignedUsers);
        }

        function alertDialog(title, message, w) {
            if(w == null || w == undefined || w == "" || w < 1)
                w = 400;
            $.perc_utils.alert_dialog({
                title: title,
                content: message,
                width: w,
                okCallBack: function()
                {
                    controller.updateListOfRoles(function()
                    {
                        $("#perc-roles-list [data-id='perc-item-id-0']").trigger("click");
                    });
                }
            });
        }

        function getDescription(){
            return $("#perc-roles-description-field").val();
        }

    };
})(jQuery);
