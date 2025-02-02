﻿/*
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

tinymce.PluginManager.add('percadvlink', function(editor) {

    function createLinkList(callback) {
        return function() {
            var linkList = editor.settings.link_list;

            if (typeof(linkList) === 'string') {
                tinymce.util.XHR.send({
                    url: linkList,
                    success: function(text) {
                        callback(tinymce.util.JSON.parse(text));
                    }
                });
            } else {
                callback(linkList);
            }
        };
    }

    function showDialog(linkList) {

        var data = {}, cm1LinkData = {}, selection = editor.selection, dom = editor.dom, selectedElm, anchorElm, initialText;
        var win, linkListCtrl, relListCtrl, targetListCtrl;
        var mainEditor = editor.contentWindow.parent;
        var topFrJQ = mainEditor.jQuery.topFrameJQuery;


        selectedElm = selection.getNode();
        anchorElm = dom.getParent(selectedElm, 'a[href]');
        if (anchorElm) {
            selection.select(anchorElm);
        }

        data.text = initialText = selection.getContent({format: 'text'});
	    data.target = anchorElm ? dom.getAttrib(anchorElm, 'target') : '';
        data.url = anchorElm ? dom.getAttrib(anchorElm, 'href') : '';
		if(data.url){

		data.href = {value: data.url};
		}
        data.title = anchorElm ? dom.getAttrib(anchorElm, 'title') : '';

        data.rel = anchorElm ? dom.getAttrib(anchorElm, 'rel') : '';
        cm1LinkData.sys_dependentvariantid = anchorElm ? dom.getAttrib(anchorElm, 'sys_dependentvariantid') : '';
        cm1LinkData.rxinlineslot = anchorElm ? dom.getAttrib(anchorElm, 'rxinlineslot') : '';
        cm1LinkData.sys_dependentid = anchorElm ? dom.getAttrib(anchorElm, 'sys_dependentid') : '';
        cm1LinkData.inlinetype = anchorElm ? dom.getAttrib(anchorElm, 'inlinetype') : '';

        if (editor.settings.rel_list) {
            relListCtrl = {
                name: 'rel',
                type: 'listbox',
                label: 'Rel',
                values: buildRelList(data.rel)
            };
        }

        win = editor.windowManager.open({
            title: I18N.message("perc.ui.widget.tinymce@Insert link"),
            data: data,
            body: {
                type: 'panel', // root body panel
                items: [
                    { name: 'href',type: 'urlinput',filetype: 'file',size: 40,label: 'Url' },
                    { name: 'title', type: 'input',label: I18N.message("perc.ui.widget.tinymce@Title"),inputMode: 'text'},
                    { name: 'target', type: 'listbox',label: I18N.message("perc.ui.widget.tinymce@Target"),
                        items: [
                            { text: 'Same Window', value: '_self' },
                            { text: 'New Window', value: '_blank' },
                            { text: 'Parent Window', value: '_parent' },
                            { text: 'Top Window', value: '_top' }
                        ]
                    }

                ]
            },
            buttons: [
                { type: 'cancel', text: 'Close' },
                { type: 'submit', text: 'Save', primary: true}
            ],
			initialData: data,
			onChange: function(api, changeData){
				if(changeData.name === 'target'){
						data.target = api.getData().target;
				}
				if(changeData.name === 'href'){
						data.href = api.getData().href;
				}
				if(changeData.name === 'title'){
						data.title = api.getData().title;
				}
            },

            onSubmit: function(e) {
                var linkPath = data.url;
                if (!linkPath) {
					linkPath = data.href.value;
					if (!linkPath) {
                        editor.execCommand('unlink');
                        return;
                    }
                }

                //Resolve manually entered internal links
                if(linkPath.match('^//Sites/') || linkPath.match('^//Assets/') || linkPath.match('^/Sites/') || linkPath.match('^/Assets/') || linkPath.match('^Sites/') || linkPath.match('^Assets/')){
                    if(linkPath.match('^Sites/') || linkPath.match('^Assets/')) {
                        linkPath = '/' + linkPath;
                    }
                    else if(linkPath.match('^//Sites/') || linkPath.match('^//Assets/')) {
                        linkPath = linkPath.substring(1);
                    }
                    topFrJQ.PercPathService.getPathItemForPath(linkPath, function(status, result){
                        if(status === topFrJQ.PercServiceUtils.STATUS_ERROR){
                            topFrJQ.perc_utils.alert_dialog({"title":I18N.message("perc.ui.widget.tinymce@Error"), "content":I18N.message("perc.ui.widget.tinymce@Invalid Link Message")});
                        }
                        else{
                            updateLinkData(result.PathItem);
                        }
                    });
                }
                else{
                    if(linkPath.match('^//Rhythmyx/') || linkPath.match('^/Rhythmyx/')){
                        addLink('no');
                    } else {
                        addLink('yes');
                    }
                }
				win.close();
            }
        });

        function updateLinkData(pathItem)
        {
            //Save the path to cookie
            topFrJQ.cookie('perc-inlinelink-path', pathItem.path);
            topFrJQ.PercPathService.getInlineRenderLink(pathItem.id, function(status, retData){

                if(!status)
                {
                    topFrJQ.perc_utils.info(retData);
                    topFrJQ.perc_utils.alert_dialog({"title":I18N.message("perc.ui.widget.tincymce@Error"), "content":I18N.message("perc.ui.widget.tinymce@Could not get item details")});
                    return;
                }
                var renderLink = retData.InlineRenderLink;
                cm1LinkData.sys_dependentvariantid = renderLink.sys_dependentvariantid;
                cm1LinkData.stateClass = renderLink.stateClass;
                cm1LinkData.rxinlineslot = '103';
                cm1LinkData.sys_dependentid = renderLink.sys_dependentid;
                cm1LinkData.inlinetype = 'rxhyperlink';
                cm1LinkData.jcrPath = pathItem.path;
                data.url = renderLink.url;
				data.href = {value: data.url};
                data.title= renderLink.title;
                addLink('no');
                win.setData(data);
            });
        }

        //Inner function that adds the link.
        function addLink(extLink){
            var anchorAttrs = {

                target: data.target ? data.target : null,
				href:data.href?data.href.value :data.url,
                title: data.title ? data.title : null,
                rel: data.rel ? data.rel : null,
                sys_dependentvariantid : cm1LinkData.sys_dependentvariantid,
                rxinlineslot : cm1LinkData.rxinlineslot,
                sys_relationshipid : '',
                sys_dependentid : cm1LinkData.sys_dependentid,
                inlinetype : cm1LinkData.inlinetype,
                'class': cm1LinkData.stateClass,
                'data-jcrpath': cm1LinkData.jcrPath
            };
            var extAnchorAttrs = {
                target: data.target ? data.target : null,
				href:data.href? data.href.value :data.url,
                title: data.title ? data.title : null
            };
            if (anchorElm) {
                editor.focus();
                if(selectedElm.nodeName !== 'IMG'){
                    anchorElm.innerHTML = data.text;
                }

                if(extLink === 'yes'){
                    dom.setAttribs(anchorElm, extAnchorAttrs);
                } else {
                    dom.setAttribs(anchorElm, anchorAttrs);
                }

                if(anchorElm.target!=null && anchorElm.target!="" ){
                    anchorElm.rel="noopener noreferrer";
                }
                selection.select(anchorElm);
            } else {
                if(anchorAttrs.target!=null && anchorAttrs.target!="" ){
                    anchorAttrs.rel="noopener noreferrer";
                }
                editor.execCommand('mceInsertLink', !1, anchorAttrs);
            }
        }

        editor.addCommand('updateFileSelection', function (ui, selectedItem) {
            updateLinkData(selectedItem,null);
        });
    }

    /**
     * This method checks if TinyMCE is part of ContentEditor or CMS UI
     * In Case it is CMS UI, it enables buttons and menu items applicable to CMS and
     * disables buttons and menuitems applicable to Rhythmyx Objects and vice a versa.
     * @returns {boolean}
     */
	function isRXEditor(){
		 var isRxEdr = false;
		if(typeof contentEditor !== 'undefined' && "yes" === contentEditor){
			isRxEdr = true;
		}
		return isRxEdr;
	}

    editor.ui.registry.addButton('link', {
        icon: 'link',
        type: 'button',
        tooltip: I18N.message("perc.ui.widget.tinymce@Insert Edit Link"),
        shortcut: 'Ctrl+K',
        onAction: createLinkList(showDialog),
        stateSelector: 'a[href]',
		onSetup: function (buttonApi) {
            var editorEventCallback = function (eventApi) {
              buttonApi.setDisabled(isRXEditor() === true );
            };
            editor.on('NodeChange', editorEventCallback);

            /* onSetup should always return the unbind handlers */
            return function (buttonApi) {
              editor.off('NodeChange', editorEventCallback);
            };
      }
    });

    editor.ui.registry.addButton('unlink', {
        icon: 'unlink',
        type: 'button',
        tooltip: I18N.message("perc.ui.widget.tinymce@Remove links"),
        onAction: function () {
            editor.execCommand('unlink');
        },
        stateSelector: 'a[href]',
		onSetup: function (buttonApi) {
            var editorEventCallback = function (eventApi) {
              buttonApi.setDisabled(isRXEditor() === true );
            };
            editor.on('NodeChange', editorEventCallback);

            /* onSetup should always return the unbind handlers */
            return function (buttonApi) {
              editor.off('NodeChange', editorEventCallback);
            };
      }
    });
	 editor.ui.registry.addButton('openlink', {
        icon: 'new-tab',
        type: 'button',
		onAction: function () {
            editor.execCommand('mceLink');
        },
        stateSelector: 'a[href]',
		 selector: 'textarea',
		 default_link_target: '_blank',
		 onSetup: function (buttonApi) {
            var editorEventCallback = function (eventApi) {
              buttonApi.setDisabled(isRXEditor() === true );
            };
            editor.on('NodeChange', editorEventCallback);

            /* onSetup should always return the unbind handlers */
            return function (buttonApi) {
              editor.off('NodeChange', editorEventCallback);
            };
      }
    });

    editor.shortcuts.add('ctrl+k', '', createLinkList(showDialog));

    this.showDialog = showDialog;

    editor.ui.registry.addMenuItem('openlink', {
        icon: 'new-tab',
        text: I18N.message("perc.ui.widget.tinymce@Open link"),
        onAction: function () {
            editor.execCommand('mceLink');
        },
        stateSelector: 'a[href]',
        context: 'insert',
        prependToContext: true,
        onSetup: function (buttonApi) {
            if (isRXEditor() === false) {
                buttonApi.setDisabled(false);
            } else {
                buttonApi.setDisabled(true);
            }
        }
    });

    editor.ui.registry.addMenuItem('link', {
        icon: 'link',
        text: I18N.message("perc.ui.widget.tinymce@Insert link"),
        shortcut: 'Ctrl+K',
        onAction: createLinkList(showDialog),
        stateSelector: 'a[href]',
        context: 'insert',
        prependToContext: true,
        onSetup: function (buttonApi) {
            if (isRXEditor() === false) {
                buttonApi.setDisabled(false);
            } else {
                buttonApi.setDisabled(true);
            }
        }
    });

	editor.ui.registry.addMenuItem('unlink', {
        icon: 'unlink',
        text: I18N.message("perc.ui.widget.tinymce@Remove links"),
        onAction: function () {
            editor.execCommand('unlink');
        },
        stateSelector: 'a[href]',
        context: 'insert',
        prependToContext: true,
        onSetup: function (buttonApi) {
            if (isRXEditor() === false) {
                buttonApi.setDisabled(false);
            } else {
                buttonApi.setDisabled(true);
            }
        }
    });
});
