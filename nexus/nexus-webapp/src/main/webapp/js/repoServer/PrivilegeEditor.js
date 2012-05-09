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

Sonatype.repoServer.PrivilegeEditor = function(config) {
  var config = config || {};
  var defaultConfig = {
    uri : Sonatype.config.repos.urls.privileges + '_target',
    dataModifiers : {
      load : {
        properties : function(value, parent, fpanel) {
          for (var i = 0; i < value.length; i++)
          {
            var field = fpanel.form.findField(value[i].key);
            field.setValue(fpanel.propertyTypeStore.getAt(fpanel.propertyTypeStore.find('type', field.fieldConverterType)).data.converter(value[i].value, value));
          }
        },
        type : function(value, parent, fpanel) {
          return fpanel.convertDataValue(value, fpanel.privilegeTypeStore, 'id', 'name');
        }
      },
      submit : {
        method : function(val, fpanel) {
          return ['create', 'read', 'update', 'delete'];
        },
        repositoryId : function(val, fpanel) {
          var v = fpanel.form.findField('repositoryOrGroup').getValue();
          return v.indexOf('repo_') == 0 ? v.substring('repo_'.length) : '';
        },
        repositoryGroupId : function(val, fpanel) {
          var v = fpanel.form.findField('repositoryOrGroup').getValue();
          return v.indexOf('group_') == 0 ? v.substring('group_'.length) : '';
        },
        type : function(val, fpanel) {
          return 'target';
        }
      }
    },
    validationModifiers : {
      repositoryId : "repositoryOrGroup",
      repositoryGroupId : "repositoryOrGroup"
    },
    referenceData : Sonatype.repoServer.referenceData.privileges.target
  };
  Ext.apply(this, config, defaultConfig);

  var ht = Sonatype.repoServer.resources.help.privileges;
  this.COMBO_WIDTH = 300;
  this.sp = Sonatype.lib.Permissions;

  this.combinedStore = new Ext.data.JsonStore({
        root : 'data',
        id : 'id',
        fields : [{
              name : 'id'
            }, {
              name : 'format'
            }, {
              name : 'name',
              sortType : Ext.data.SortTypes.asUCString
            }],
        url : Sonatype.config.repos.urls.repositories
      });
  this.initCombinedStore();

  this.checkPayload();
  if (!(this.sp.checkPermission('security:privileges', this.sp.UPDATE) || this.isNew))
  {
    this.readOnly = true;
  }

  var items = [{
        xtype : 'hidden',
        name : 'id'
      }, {
        xtype : 'textfield',
        fieldLabel : 'Name',
        itemCls : this.readOnly ? '' : 'required-field',
        helpText : ht.name,
        name : 'name',
        allowBlank : false,
        width : this.COMBO_WIDTH,
        disabled : this.readOnly
      }, {
        xtype : 'textfield',
        fieldLabel : 'Description',
        itemCls : this.readOnly ? '' : 'required-field',
        helpText : ht.description,
        name : 'description',
        allowBlank : false,
        width : this.COMBO_WIDTH,
        disabled : this.readOnly
      }];

  if (this.isNew)
  {
    // clone the target store
    var targetStore2 = new Ext.data.JsonStore({
          root : 'data',
          id : 'id',
          fields : [{
                name : 'id'
              }, {
                name : 'contentClass'
              }, {
                name : 'name',
                sortType : Ext.data.SortTypes.asUCString
              }],
          url : Sonatype.config.repos.urls.repoTargets
        });
    targetStore2.add(this.targetStore.getRange());
    this.targetStore = targetStore2;

    items.push({
          xtype : 'combo',
          fieldLabel : 'Repository',
          itemCls : 'required-field',
          helpText : ht.repositoryOrGroup,
          name : 'repositoryOrGroup',
          store : this.combinedStore,
          displayField : 'name',
          valueField : 'id',
          editable : false,
          forceSelection : true,
          mode : 'local',
          triggerAction : 'all',
          emptyText : 'Select...',
          selectOnFocus : true,
          allowBlank : false,
          width : this.COMBO_WIDTH,
          minListWidth : this.COMBO_WIDTH,
          value : "all_repo",
          listeners : {
            select : {
              fn : this.repositorySelectHandler,
              scope : this
            }
          }
        });
    items.push({
          xtype : 'combo',
          fieldLabel : 'Repository Target',
          itemCls : 'required-field',
          helpText : ht.repositoryTarget,
          name : 'repositoryTargetId',
          store : this.targetStore,
          displayField : 'name',
          valueField : 'id',
          editable : false,
          forceSelection : true,
          mode : 'local',
          triggerAction : 'all',
          emptyText : 'Select...',
          selectOnFocus : true,
          width : this.COMBO_WIDTH
        });
  }
  else
  {
    items.push({
          xtype : 'textfield',
          fieldLabel : 'Type',
          helpText : ht.type,
          name : 'type',
          width : this.COMBO_WIDTH,
          disabled : true
        });

    var typeRec = this.privilegeTypeStore.getById(this.payload.data.type);

    if (!Ext.isEmpty(typeRec))
    {
      for (var i = 0; i < typeRec.data.properties.length; i++)
      {
        items.push({
              xtype : 'textfield',
              fieldConverterType : typeRec.data.properties[i].type,
              fieldLabel : typeRec.data.properties[i].name,
              helpText : typeRec.data.properties[i].helpText,
              name : typeRec.data.properties[i].id,
              width : this.COMBO_WIDTH,
              disabled : true
            });
      }
    }
  }

  Sonatype.repoServer.PrivilegeEditor.superclass.constructor.call(this, {
        labelWidth : 120,
        items : items
      });
};

Ext.extend(Sonatype.repoServer.PrivilegeEditor, Sonatype.ext.FormPanel, {
      initCombinedRecord : function(rec) {
        var isGroup = rec.data.repoType == null;
        return {
          id : (isGroup ? 'group_' : 'repo_') + rec.data.id,
          name : rec.data.name + (isGroup ? ' (Group)' : ' (Repo)'),
          format : rec.data.format
        };
      },

      initCombinedStore : function() {
        var data = [{
              id : 'all_repo',
              name : 'All Repositories'
            }];

        this.repoStore.each(function(rec) {
              data.push(this.initCombinedRecord(rec));
            }, this);
        this.groupStore.each(function(rec) {
              data.push(this.initCombinedRecord(rec));
            }, this);

        this.combinedStore.loadData({
              data : data
            });
      },

      repositorySelectHandler : function(combo, rec, index) {
        var targetCombo = this.form.findField('repositoryTargetId');
        var previousValue = targetCombo.getValue();
        targetCombo.setValue(null);
        targetCombo.store.clearFilter();

        var filterValue = rec.data.format;
        if (filterValue)
        {
          targetCombo.store.filter('contentClass', filterValue);
        }

        if (targetCombo.store.getById(previousValue))
        {
          targetCombo.setValue(previousValue);
        }
      }
    });
