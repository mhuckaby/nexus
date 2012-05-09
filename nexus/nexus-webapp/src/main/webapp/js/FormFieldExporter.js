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

FormFieldExporter = function(formPanel, panelIdSuffix, formFieldPrefix, customTypes) {
  var outputArr = [];

  var formFieldPanel = formPanel.findById(formPanel.id + panelIdSuffix);
  var i = 0;
  // These are dynamic fields here, so some pretty straightforward generic
  // logic below
  formFieldPanel.getLayout().activeItem.items.each(function(item, i, len) {
        var value;

        if (item.xtype == 'datefield')
        {
          // long representation is used, not actual date
          // force to a string, as that is what the current api requires
          value = '' + item.getValue().getTime();
        }
        else if (item.xtype == 'textfield')
        {
          value = item.getValue();
        }
        else if (item.xtype == 'numberfield')
        {
          // force to a string, as that is what the current api requires
          value = '' + item.getValue();
        }
        else if (item.xtype == 'textarea')
        {
          value = item.getValue();
        }
        else if (item.xtype == 'checkbox')
        {
          value = '' + item.getValue();
        }
        else if (item.xtype == 'combo')
        {
          value = item.getValue();
        }
        else if (customTypes && customTypes[item.xtype])
        {
          value = customTypes[item.xtype].retrieveValue.call(item, item);
        }

        outputArr[i] = {
          key : item.getName().substring(formFieldPrefix.length),
          value : value
        };
        i++;
      }, formFieldPanel.getLayout().activeItem);

  return outputArr;
};