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

FormFieldImporter = function(jsonObject, formPanel, formFieldPrefix, customTypes) {
  // Maps the incoming json properties to the generic component
  for (var i = 0; i < jsonObject.properties.length; i++)
  {
    var formFields = formPanel.find('name', formFieldPrefix + jsonObject.properties[i].key);
    for (var j = 0; j < formFields.length; j++)
    {
      var formField = formFields[j];

      if (formField != null)
      {
        if (!formField.disabled && !Ext.isEmpty(jsonObject.properties[i].value))
        {
          if (formField.xtype == 'datefield')
          {
            formField.setValue(new Date(Number(jsonObject.properties[i].value)));
          }
          else if (formField.xtype == 'textfield')
          {
            formField.setValue(jsonObject.properties[i].value);
          }
          else if (formField.xtype == 'numberfield')
          {
            formField.setValue(Number(jsonObject.properties[i].value));
          }
          else if (formField.xtype == 'textarea')
          {
            formField.setValue(jsonObject.properties[i].value);
          }
          else if (formField.xtype == 'checkbox')
          {
            formField.setValue(Boolean('true' == jsonObject.properties[i].value));
          }
          else if (formField.xtype == 'combo')
          {
            formField.setValue(jsonObject.properties[i].value);
          }
          else if (customTypes && customTypes[formField.xtype])
          {
            customTypes[formField.xtype].setValue.call(formField, formField, jsonObject.properties[i].value);
          }
          break;
        }
      }
    }
  }
};