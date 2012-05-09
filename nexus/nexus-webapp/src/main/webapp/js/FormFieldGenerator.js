/*
 * Sonatype Nexus (TM) Open Source Version
 * Copyright (c) 2007-2012 Sonatype, Inc.
 * All rights reserved. Includes the third-party code listed at http://links.sonatype.com/products/nexus/oss/attributions.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
 * which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Sonatype Nexus (TM) Professional Version is available from Sonatype, Inc. "Sonatype" and "Sonatype Nexus" are trademarks
 * of Sonatype, Inc. Apache Maven is a trademark of the Apache Software Foundation. M2eclipse is a trademark of the
 * Eclipse Foundation. All other trademarks are the property of their respective owners.
 */

// FIXME: Namespace!!!

FormFieldGenerator = function(panelId, fieldSetName, fieldNamePrefix, typeStore, repoStore, groupStore, repoOrGroupStore, customTypes, width) {
  var allTypes = [];

  if (!width)
  {
    width = 300;
  }

  allTypes[0] = {
    xtype : 'fieldset',
    id : panelId + '_emptyItem',
    checkboxToggle : false,
    title : fieldSetName,
    anchor : Sonatype.view.FIELDSET_OFFSET,
    collapsible : false,
    autoHeight : true,
    layoutConfig : {
      labelSeparator : ''
    }
  };

  // Now add the dynamic content
  typeStore.each(function(item, i, len) {
        var items = [];
        if (item.data.formFields.length > 0)
        {
          for (var j = 0; j < item.data.formFields.length; j++)
          {
            var curRec = item.data.formFields[j];
            // Note that each item is disabled initially, this is because
            // the select handler for the capabilityType
            // combo box handles enabling/disabling as necessary, so each
            // inactive card isn't also included in the form
            if (curRec.type == 'string')
            {
              items[j] = {
                xtype : 'textfield',
                htmlDecode : true,
                fieldLabel : curRec.label,
                itemCls : curRec.required ? 'required-field' : '',
                helpText : curRec.helpText,
                name : fieldNamePrefix + curRec.id,
                allowBlank : curRec.required ? false : true,
                disabled : true,
                width : width,
                regex : curRec.regexValidation ? new RegExp(curRec.regexValidation) : null
              };
            }
            else if (curRec.type == 'number')
            {
              items[j] = {
                xtype : 'numberfield',
                fieldLabel : curRec.label,
                itemCls : curRec.required ? 'required-field' : '',
                helpText : curRec.helpText,
                name : fieldNamePrefix + curRec.id,
                allowBlank : curRec.required ? false : true,
                disabled : true,
                width : width,
                regex : curRec.regexValidation ? new RegExp(curRec.regexValidation) : null
              };
            }
            else if (curRec.type == 'text-area')
            {
              items[j] = {
                xtype : 'textarea',
                htmlDecode : true,
                fieldLabel : curRec.label,
                itemCls : curRec.required ? 'required-field' : '',
                helpText : curRec.helpText,
                name : fieldNamePrefix + curRec.id,
                allowBlank : curRec.required ? false : true,
                disabled : true,
                anchor : '-20',
                height : '138',
                regex : curRec.regexValidation ? new RegExp(curRec.regexValidation) : null
              };
            }
            else if (curRec.type == 'checkbox')
            {
              items[j] = {
                xtype : 'checkbox',
                fieldLabel : curRec.label,
                helpText : curRec.helpText,
                name : fieldNamePrefix + curRec.id,
                disabled : true
              };
            }
            else if (curRec.type == 'date')
            {
              items[j] = {
                xtype : 'datefield',
                fieldLabel : curRec.label,
                itemCls : curRec.required ? 'required-field' : '',
                helpText : curRec.helpText,
                name : fieldNamePrefix + curRec.id,
                allowBlank : curRec.required ? false : true,
                disabled : true,
                value : new Date()
              };
            }
            else if (curRec.type == 'repo')
            {
              items[j] = {
                xtype : 'combo',
                fieldLabel : curRec.label,
                itemCls : curRec.required ? 'required-field' : '',
                helpText : curRec.helpText,
                name : fieldNamePrefix + curRec.id,
                store : repoStore,
                displayField : 'name',
                valueField : 'id',
                editable : false,
                forceSelection : true,
                mode : 'local',
                triggerAction : 'all',
                emptyText : 'Select...',
                selectOnFocus : true,
                allowBlank : curRec.required ? false : true,
                disabled : true,
                width : width,
                minListWidth : width
              };
            }
            else if (curRec.type == 'group')
            {
              items[j] = {
                xtype : 'combo',
                fieldLabel : curRec.label,
                itemCls : curRec.required ? 'required-field' : '',
                helpText : curRec.helpText,
                name : fieldNamePrefix + curRec.id,
                store : groupStore,
                displayField : 'name',
                valueField : 'id',
                editable : false,
                forceSelection : true,
                mode : 'local',
                triggerAction : 'all',
                emptyText : 'Select...',
                selectOnFocus : true,
                allowBlank : curRec.required ? false : true,
                disabled : true,
                width : width,
                minListWidth : width
              };
            }
            else if (curRec.type == 'repo-or-group')
            {
              items[j] = {
                xtype : 'combo',
                fieldLabel : curRec.label,
                itemCls : curRec.required ? 'required-field' : '',
                helpText : curRec.helpText,
                name : fieldNamePrefix + curRec.id,
                store : repoOrGroupStore,
                displayField : 'name',
                valueField : 'id',
                editable : false,
                forceSelection : true,
                mode : 'local',
                triggerAction : 'all',
                emptyText : 'Select...',
                selectOnFocus : true,
                allowBlank : curRec.required ? false : true,
                disabled : true,
                width : width,
                minListWidth : width
              };
            }
            else if (customTypes && customTypes[curRec.type])
            {
              items[j] = customTypes[curRec.type].createItem.call(this, curRec, fieldNamePrefix, width);
            }

            allTypes[allTypes.length] = {
              xtype : 'fieldset',
              id : panelId + '_' + item.data.id,
              checkboxToggle : false,
              title : fieldSetName,
              anchor : Sonatype.view.FIELDSET_OFFSET,
              collapsible : false,
              autoHeight : true,
              labelWidth : 175,
              layoutConfig : {
                labelSeparator : ''
              },
              items : items
            };
          }
        }
        else
        {
          allTypes[allTypes.length] = {
            xtype : 'fieldset',
            id : panelId + '_' + item.data.id,
            checkboxToggle : false,
            title : fieldSetName,
            anchor : Sonatype.view.FIELDSET_OFFSET,
            collapsible : false,
            autoHeight : true,
            labelWidth : 175,
            layoutConfig : {
              labelSeparator : ''
            }
          };
        }
      }, this);

  return allTypes;
};
