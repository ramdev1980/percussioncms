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
 * Actions split button
 */
(function ($)
{
    /**
     * Constructs the actions button.
     *
     * @param finder
     * @param contentViewer
     */
    $.perc_build_actions_button = function (finder, contentViewer)
    {
        // Create the action elements
        var cp = $.perc_build_copy_page_button( finder, contentViewer );
        var fp = $.perc_build_folderproperties_button( finder, contentViewer );
        var db = $.perc_build_download_button( finder, contentViewer );
        var ub = $.perc_build_upload_button( finder, contentViewer );
        var ri = $.perc_build_restore_button( finder, contentViewer );

        var menuEntries = [cp, fp, db, ub, ri];
        // Create the menu and the button
        var menu = createMenuHTML(menuEntries)
            .on("mouseenter",preventHide)
            .on("mouseleave",hideOnMouseOut);

        var btnHtml ='<div id="perc-finder-actions" >'
            + '<a id="perc-finder-actions-button" class="perc-font-icon" title="' +I18N.message("perc.ui.actions.button@Select An Action") + '" href="#"><span class="icon-cog fas fa-cog"></span><span class="icon-caret-down fas fa-caret-down"></span></a>'
            + '</div>';
        var btn = $(btnHtml)
            //.perc_button()
            .append(menu)
            .on("mouseenter",preventHide)
            .on("mouseleave",hideOnMouseOut);

        // This flag, hideOnMouseOut and preventHide prevent an unnatural hiding of the menu
        var flag_show = false;

        /**
         * Binds the hiding behavior to the menu once the cursor left it.
         */
        function hideOnMouseOut()
        {
            flag_show = false;
            setTimeout(function() {
                if (!flag_show) {
                    showMenu(false);
                }
            },500);
        }

        /**
         * Prevents the menu hiding if the cursor returns to the hover the menu or the button.
         */
        function preventHide(event)
        {
            var target = $(event.target);

            if (target.attr('id') === btn.attr('id'))
            {
                flag_show = true;
                return;
            }

            if (target.is("#perc-finder-actions *"))
            {
                flag_show = true;
            }
        }

        /**
         * Handler that get called when the button is clicked
         */
        function clickHandler()
        {
            if ($('#perc-finder-actions-button').hasClass('ui-disabled'))
            {
                return false;
            }
            else
            {
                if (menu.css('display') === 'none')
                {
                    showMenu(true);
                }
                else
                {
                    showMenu(false);
                }
                return false;
            }
        }

        /**
         * Makes the menu visible/invisible.
         * @param boolean flag If true, makes the menu visible
         */
        function showMenu(flag)
        {
            if (flag)
            {
                var menuX = btn.position().left + btn.outerWidth() - menu.outerWidth() - 1;
                var menuY = btn.position().top + btn.outerHeight() + 9;
                menu
                    .css("top", menuY)
                    .css("left", menuX)
                    .css("display", "block");
            }
            else
            {
                menu.hide();
            }
        }

        /**
         * Helper function to enable or disable the button in the finder.
         * @param flag(boolean) if <code>true</code> the button is enabled, otherwise the button is disabled.
         */
        function enableButton(flag)
        {
            var anchor = $('#perc-finder-actions-button');
            if (flag)
            {
                // We perform an "unbind" first, in case clickHandler has been bound several times by error
                // (same thing in the else)
                anchor.removeClass('ui-disabled').addClass('ui-enabled').off('click').on("click",
                    clickHandler);
            }
            else
            {
                anchor.addClass('ui-disabled').removeClass('ui-enabled').off('click');
            }
        }

        /**
         * Creates the base HTML and adds the menu entries.
         * @param array of menuentries (former button elements)
         */
        function createMenuHTML(menuentries)
        {
            var dropdown = $("<ul class=\"perc-actions-menu box_shadow_with_padding\">");
            var option = $("<li class=\"perc-actions-menu-item\">");

            for(l = 0; l < menuentries.length; l++){
                option.clone().append(menuentries[l]).appendTo(dropdown);
            }

            return dropdown;
        }

        /**
         * Binds the enable/disable change events of the menu entries with the actions button.
         * If all the menu entries are disabled, the button must be disabled too.
         */
        function bindChangeEnabledStateListener()
        {
            // We will declare a variable and an internal method first, then we will bind this internal
            // function to the buttons
            var entriesListenedLeft = menuEntries.length;
            var entriesDisabled = 0;

            /**
             * Callback function that is called whenever an 'actions-change-enabled-state' event
             * is triggered. It uses closure to take advantage of storing state between asynchronous
             * calls and maintain state to finally enable/disable the actions button.
             * NOTE: To debug this function I recommend using console.log()
             */
            function entryChangeEnabledStateListener()
            {
                // In this case, "this" represents the menu entry
                var state_enabled = $(this).hasClass("ui-enabled");
                if (entriesListenedLeft === 1 && entriesDisabled === menuEntries.length - 1)
                {
                    // If there only 1 entry left to trigger the event, and the previous ones were
                    // all disabled, then its states determines the state of the button
                    enableButton(state_enabled);
                    entriesListenedLeft = menuEntries.length;
                    entriesDisabled = 0;
                }
                else if (entriesListenedLeft == 1 && entriesDisabled < menuEntries.length - 1)
                {
                    enableButton(true);
                    entriesListenedLeft = menuEntries.length;
                    entriesDisabled = 0;
                }
                else
                {
                    // The entry is not the last, if is disabled count it
                    if (!state_enabled)
                    {
                        entriesDisabled++;
                    }
                    entriesListenedLeft--;
                }
            }

            // Bind the declared function to the buttons in the array menuEntries
            for (m = 0; m < menuEntries.length; m++)
            {
                menuEntries[m].on('actions-change-enabled-state', entryChangeEnabledStateListener);
            }
        }

        // By default, the button will be disabled, and will get enabled whenever one of the menu
        // entries gets enabled. Check the bindChangeEnabledStateListener() function.
        bindChangeEnabledStateListener();
        return btn;
    };
})(jQuery);
