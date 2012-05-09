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

Sonatype.ext.TwinPanelController = function(config) {
  var config = config || {};
  var defaultConfig = {};
  Ext.apply(this, config, defaultConfig);

  this.addOneButton = new Ext.Button({
        xtype : 'button',
        handler : this.addOne,
        scope : this,
        tooltip : 'Add',
        icon : Sonatype.config.extPath + '/images/default/grid/page-prev.gif',
        cls : 'x-btn-icon'
      });

  this.addAllButton = new Ext.Button({
        xtype : 'button',
        handler : this.addAll,
        scope : this,
        tooltip : 'Add All',
        icon : Sonatype.config.extPath + '/images/default/grid/page-first.gif',
        cls : 'x-btn-icon'
      });

  this.removeOneButton = new Ext.Button({
        xtype : 'button',
        handler : this.removeOne,
        scope : this,
        tooltip : 'Remove',
        icon : Sonatype.config.extPath + '/images/default/grid/page-next.gif',
        cls : 'x-btn-icon'
      });

  this.removeAllButton = new Ext.Button({
        xtype : 'button',
        handler : this.removeAll,
        scope : this,
        tooltip : 'Remove All',
        icon : Sonatype.config.extPath + '/images/default/grid/page-last.gif',
        cls : 'x-btn-icon'
      });

  Sonatype.ext.TwinPanelController.superclass.constructor.call(this, {
        layout : 'table',
        style : 'padding-top: ' + (this.halfSize ? 40 : 100) + 'px; padding-right: 10px; padding-left: 10px',
        width : 45,
        defaults : {
          style : 'margin-bottom: 3px'
        },
        layoutConfig : {
          columns : 1
        },
        items : [this.addOneButton, this.addAllButton, this.removeOneButton, this.removeAllButton]
      });

};

Ext.extend(Sonatype.ext.TwinPanelController, Ext.Panel, {
      disable : function() {
        this.addOneButton.disable();
        this.addAllButton.disable();
        this.removeOneButton.disable();
        this.removeAllButton.disable();
      },
      enable : function() {
        this.addOneButton.enable();
        this.addAllButton.enable();
        this.removeOneButton.enable();
        this.removeAllButton.enable();
      },
      addOne : function() {
        this.moveItems(2, 0, false);
      },

      addAll : function() {
        this.moveItems(2, 0, true);
      },

      removeOne : function() {
        this.moveItems(0, 2, false);
      },

      removeAll : function() {
        this.moveItems(0, 2, true);
      },

      moveItems : function(fromIndex, toIndex, moveAll) {
        var fromPanel = this.ownerCt.getComponent(fromIndex);
        var toPanel = this.ownerCt.getComponent(toIndex);

        var dragZone = fromPanel.dragZone;
        var dropZone = toPanel.dropZone;
        var fn = toPanel.dropConfig.onContainerOver.createDelegate(dropZone, [dragZone, null], 0);
        var checkIfDragAllowed = function(node) {
          return (!node.disabled) && fn({
                node : node
              }) == dropZone.dropAllowed;
        }

        if (fromPanel && toPanel)
        {
          if (toPanel.sorter && toPanel.sorter.disableSort)
          {
            toPanel.sorter.disableSort(toPanel);
          }
          if (fromPanel.sorter && fromPanel.sorter.disableSort)
          {
            fromPanel.sorter.disableSort(fromPanel);
          }
          var fromRoot = fromPanel.root;
          var toRoot = toPanel.root;
          if (moveAll)
          {
            for (var i = 0; i < fromRoot.childNodes.length; i++)
            {
              var node = fromRoot.childNodes[i];
              if (checkIfDragAllowed(node))
              {
                toRoot.appendChild(node);
                i--;
              }
            }
          }
          else
          {
            var selectedNodes = fromPanel.getSelectionModel().getSelectedNodes();
            if (selectedNodes)
            {
              for (var i = 0; i < selectedNodes.length; i++)
              {
                var node = selectedNodes[i];
                if (checkIfDragAllowed(node))
                {
                  toRoot.appendChild(node);
                  i--;
                }
              }
            }
          }
          if (toPanel.sorter && toPanel.sorter.enableSort)
          {
            toPanel.sorter.enableSort(toPanel);
          }
          if (fromPanel.sorter && fromPanel.sorter.enableSort)
          {
            fromPanel.sorter.enableSort(fromPanel);
          }
        }
      }
    });

Ext.reg('twinpanelcontroller', Sonatype.ext.TwinPanelController);
