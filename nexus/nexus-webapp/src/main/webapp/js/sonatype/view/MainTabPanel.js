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

Sonatype.view.MainTabPanel = Ext.extend(Ext.TabPanel, {
      addOrShowTab : function(id, panelClass, panelClassParams) {
        var panelClassParams = panelClassParams || {};
        var tab = this.getComponent(id);
        if (tab)
        {
          this.setActiveTab(tab);
        }
        else
        {
          tab = new panelClass(Ext.apply({
                id : id
              }, panelClassParams));
          this.add(tab);
        }

        this.setActiveTab(tab);

        return tab;
      }
    });

Sonatype.Events.addListener('nexusNavigationInit', function(panel) {
      Sonatype.view.viewport.doLayout();
    });

Sonatype.utils.updateGlobalTimeout();
