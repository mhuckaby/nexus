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

/**
 * A RoleManager is used to display assigned roles and privileges (optional) in a grid, with a toolbar
 * that has options to add roles/privileges (which will open up a new window that will show a specialized role/privilege list
 * grid that will allow for filtering/paging the lists and selection for addition) and to remove roles/privileges ( again opening
 * up a new window, where the user can simply deselect available items ) 
 *  
 * usePrivileges - boolean flag, if true will use the privileges along with the roles 
 * selectedRoleIds - role ids that are already assigned 
 * selectedPrivilegeIds - privilege ids that are already assigned
 * userId - userId which is used to properly retrieve the data for UI (most specifically the external roles)
 */
RoleManager = function(config) {
  var config = config || {};

  var defaultConfig = {
    header : false,
    trackMouseOver : false,
    loadMask : true,
    usePrivileges : true,
    selectedRoleIds : [],
    selectedPrivilegeIds : [],
    onlySelected : true,
    userId : null,
    hideHeaders : true,
    doValidation : true,
    style : 'border:1px solid #B5B8C8',
    readOnly : false,
    viewConfig : {
      forceFit : true
    }
  };
  //apply the config and defaults to 'this'
  Ext.apply(this, config, defaultConfig);

  //proxy for the store, to assign jsonData
  this.storeProxy = new Ext.data.HttpProxy({
        method : 'POST',
        url : Sonatype.config.servicePath + '/rolesAndPrivs',
        jsonData : {
          data : {
            onlySelected : this.onlySelected,
            noPrivileges : !this.usePrivileges,
            noRoles : false,
            selectedRoleIds : this.selectedRoleIds,
            selectedPrivilegeIds : this.selectedPrivilegeIds,
            userId : this.userId
          }
        }
      });

  //our remote resource, that will be supplying the paginated content
  this.store = new Ext.data.JsonStore({
        root : 'data',
        id : 'id',
        totalProperty : 'totalCount',
        remoteSort : true,
        proxy : this.storeProxy,
        fields : [{
              name : 'id'
            }, {
              name : 'type'
            }, {
              name : 'name',
              sortType : Ext.data.SortTypes.asUCString
            }, {
              name : 'description'
            }, {
              name : 'external'
            }]
      });

  this.store.setDefaultSort('name', 'asc');

  //setup the columns in the grid
  this.columns = [];

  this.columns.push({
        id : 'name',
        width : 100,
        header : 'Name',
        dataIndex : 'name',
        sortable : true,
        renderer : function(value, metaData, record, rowIndex, colIndex, store) {
          switch (record.get('type'))
          {
            case 'role' :
              metaData.css = 'roleRow';
              break;
            case 'privilege' :
              metaData.css = 'privilegeRow';
              break;
          }
          return String.format('<img class="placeholder" src="{0}"/>', Ext.BLANK_IMAGE_URL) + (record.get('external') ? ('<b>' + value + '</b>') : value);
        }
      });

  this.addButton = new Ext.Toolbar.Button({
        text : 'Add',
        handler : this.addHandler,
        scope : this,
        disabled : this.readOnly
      });

  this.removeButton = new Ext.Toolbar.Button({
        text : 'Remove',
        handler : this.removeHandler,
        scope : this,
        disabled : true
      });

  this.tbar = ['<b>Role' + (this.usePrivileges ? '/Privilege' : '') + ' Management</b>', '->', '-', this.addButton, '-', this.removeButton];

  RoleManager.superclass.constructor.call(this, {});

  this.getView().scrollOffset = 1;

  this.getSelectionModel().on('selectionchange', this.selectionChangeHandler, this);
};

Ext.extend(RoleManager, Ext.grid.GridPanel, {
      selectionChangeHandler : function(selectionModel) {
        if (selectionModel.getCount() > 0 && !this.readOnly)
        {
          this.removeButton.enable();
        }
        else
        {
          this.removeButton.disable();
        }
      },
      addHandler : function() {
        this.roleSelectorWindow = new Ext.Window({
              modal : true,
              layout : 'fit',
              width : 800,
              items : [{
                    xtype : 'roleselector',
                    name : 'roleSelector',
                    height : 400,
                    width : 750,
                    usePrivileges : this.usePrivileges,
                    hiddenRoleIds : this.getHiddenRoleIds(),
                    hiddenPrivilegeIds : this.getSelectedPrivilegeIds(),
                    title : 'Add Roles' + (this.usePrivileges ? ' and Privileges' : '')
                  }],
              buttonAlign : 'center',
              buttons : [{
                    handler : this.addOk,
                    text : 'OK',
                    scope : this
                  }, {
                    handler : this.addCancel,
                    text : 'Cancel',
                    scope : this
                  }]
            });
        this.roleSelectorWindow.show();
      },
      removeHandler : function() {
        var records = this.getSelectionModel().getSelections();

        if (records && records.length > 0)
        {
          for (var i = 0; i < records.length; i++)
          {
            if (records[i].get('type') == 'role')
            {
              this.selectedRoleIds.remove(records[i].get('id'));
            }
            else if (records[i].get('type') == 'privilege')
            {
              this.selectedPrivilegeIds.remove(records[i].get('id'));
            }
          }
          this.reloadStore();
          this.validate();
        }
      },
      addOk : function() {
        var roleSelector = this.roleSelectorWindow.find('name', 'roleSelector')[0];
        this.addSelectedRoleId(roleSelector.getSelectedRoleIds());
        this.addSelectedPrivilegeId(roleSelector.getSelectedPrivilegeIds());
        this.roleSelectorWindow.close();
        this.reloadStore();
      },
      addCancel : function() {
        this.roleSelectorWindow.close();
      },
      getIdFromObject : function(object) {
        if (typeof(object) != 'string' && object.source == 'default')
        {
          if (object.id)
          {
            return object.id;
          }
          else if (object.roleId)
          {
            return object.roleId;
          }
        }
        else if (typeof(object) == 'string')
        {
          return object;
        }
        return null;
      },
      append : function (roleIds, to) {
        if (roleIds != null)
        {
          if (!Ext.isArray(roleIds))
          {
            roleIds = [roleIds];
          }

          for (var i = 0; i < roleIds.length; i++)
          {
            var roleId = this.getIdFromObject(roleIds[i]);
            if (roleId != null)
            {
              to.push(roleId);
            }
          }
        }
      },
      setHiddenRoleIds : function(roleIds, reload) {
        this.hiddenRoleIds = [];
        
        this.append(roleIds, this.hiddenRoleIds);
        
        this.validate();

        if (reload)
        {
          this.reloadStore();
        }
      },
      getHiddenRoleIds : function() {
        // NEXUS-4371: merge selected and explicitly hidden roles
        var hidden = [];
        this.append(this.hiddenRoleIds, hidden);
        this.append(this.getSelectedRoleIds(), hidden);
        return hidden;
      },
      setSelectedRoleIds : function(roleIds, reload) {
        this.selectedRoleIds = [];
        
        this.append(roleIds, this.selectedRoleIds);

        if (this.selectedRoleIds.length == 0)
        {
          this.noRolesOnStart = true;
        }
        else
        {
          this.noRolesOnStart = false;
        }

        this.validate();

        if (reload)
        {
          this.reloadStore();
        }
      },
      setSelectedPrivilegeIds : function(privilegeIds, reload) {
        this.selectedPrivilegeIds = [];
        
        this.append(privilegeIds, this.selectedPrivilegeIds);

        this.validate();

        if (reload)
        {
          this.reloadStore();
        }
      },
      addSelectedRoleId : function(roleIds, reload) {
        if (roleIds)
        {
          if (!Ext.isArray(roleIds))
          {
            roleIds = [roleIds];
          }

          for (var i = 0; i < roleIds.length; i++)
          {
            var roleId = this.getIdFromObject(roleIds[i]);
            if (roleId != null)
            {
              this.selectedRoleIds.push(roleId);
            }
          }

          this.validate();

          if (reload)
          {
            this.reloadStore();
          }
        }
      },
      addSelectedPrivilegeId : function(privilegeIds, reload) {
        if (privilegeIds)
        {
          if (!Ext.isArray(privilegeIds))
          {
            privilegeIds = [privilegeIds];
          }

          for (var i = 0; i < privilegeIds.length; i++)
          {
            var privilegeId = this.getIdFromObject(privilegeIds[i]);
            if (privilegeId != null)
            {
              this.selectedPrivilegeIds.push(privilegeId);
            }
          }

          this.validate();

          if (reload)
          {
            this.reloadStore();
          }
        }
      },
      reloadStore : function() {
        this.storeProxy.conn.jsonData.data.selectedRoleIds = this.selectedRoleIds;
        this.storeProxy.conn.jsonData.data.selectedPrivilegeIds = this.selectedPrivilegeIds;
        this.storeProxy.conn.jsonData.data.userId = this.userId;
        this.store.load({
              params : {
                start : 0
              }
            });
      },
      getSelectedRoleIds : function() {
        return this.selectedRoleIds;
      },
      getSelectedPrivilegeIds : function() {
        return this.selectedPrivilegeIds;
      },
      getRoleNameFromId : function(id) {
        var rec = this.store.getById(id);

        if (rec)
        {
          return rec.get('name');
        }

        return id;
      },
      showErrorMarker: function(msg) {
        var elp = this.getEl();
        if (!this.errorEl) {
            this.errorEl = elp.createChild({
                cls: "x-form-invalid-msg"
            });
            this.errorEl.setWidth(elp.getWidth(true));
            this.errorEl.setStyle("border: 0 solid #fff")
        }
        this.errorEl.update(msg);
        elp.setStyle({
            "background-color": "#fee",
            border: "1px solid #dd7870"
        });
        Ext.form.Field.msgFx.normal.show(this.errorEl, this)
      },
      markInvalid: function(msg) {
       this.showErrorMarker(msg);
      },
      validate : function() {
        if (this.doValidation && !this.userId && this.selectedRoleIds.length == 0 && this.selectedPrivilegeIds.length == 0)
        {
          var msg = "You must select at least 1 role" + (this.usePrivileges ? " or privilege" : "");
          this.showErrorMarker(msg);
          return false;
        }

        this.clearValidation();
        return true;
      },
      clearValidation : function() {
        if (this.errorEl)
        {
          this.getEl().setStyle({
                'background-color' : '#FFFFFF',
                border : '1px solid #B5B8C8'
              });
          Ext.form.Field.msgFx['normal'].hide(this.errorEl, this);
        }
      },
      setUserId : function(userId) {
        this.userId = userId;
        this.storeProxy.conn.jsonData.data.userId = this.userId;
      },
      disable : function() {
        this.readOnly = true;
        this.addButton.disable();
        this.removeButton.disable();
      },
      enable : function() {
        this.readOnly = false;
        this.addButton.enable();
        //dont blatantly enable, unless something is selected
        this.selectionChangeHandler(this.getSelectionModel());
      }
    });

Ext.reg('rolemanager', RoleManager);
