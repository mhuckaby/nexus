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

// handy grid validation from
// http://www.extjs.com/forum/showthread.php?t=21158
Ext.namespace('Ext.ux', 'Ext.ux.plugins');

/**
 * EditorGrid validation plugin Adds validation functions to the grid
 * 
 * @author Jozef Sakalos, aka Saki
 * @version 0.1 Usage: grid = new Ext.grid.EditorGrid( { plugins:new
 *          Ext.ux.plugins.GridValidator(), ... } )
 */
Ext.ux.plugins.GridValidator = function(config) {
  // initialize plugin
  this.init = function(grid) {
    Ext.apply(grid, {
          /**
           * Checks if a grid cell is valid
           * 
           * @param {Integer}
           *          col Cell column index
           * @param {Integer}
           *          row Cell row index
           * @return {Boolean} true = valid, false = invalid
           */
          isCellValid : function(col, row) {
            if (!this.colModel.isCellEditable(col, row))
            {
              return true;
            }
            var ed = this.colModel.getCellEditor(col, row);
            if (!ed)
            {
              return true;
            }
            var record = this.store.getAt(row);
            if (!record)
            {
              return true;
            }
            var field = this.colModel.getDataIndex(col);
            ed.field.setValue(record.data[field]);
            return ed.field.isValid(true);
          },
          /**
           * Checks if grid has valid data
           * 
           * @param {Boolean}
           *          editInvalid true to automatically start editing of the
           *          first invalid cell
           * @return {Boolean} true = valid, false = invalid
           */
          isValid : function(editInvalid) {
            var cols = this.colModel.getColumnCount();
            var rows = this.store.getCount();
            var r, c;
            var valid = true;
            for (r = 0; r < rows; r++)
            {
              for (c = 0; c < cols; c++)
              {
                valid = this.isCellValid(c, r);
                if (!valid)
                {
                  break;
                }
              }
              if (!valid)
              {
                break;
              }
            }
            if (editInvalid && !valid)
            {
              this.getSelectionModel().selectRow(r);
              this.startEditing(r, c);
            }
            return valid;
          }
        });
  };
};
