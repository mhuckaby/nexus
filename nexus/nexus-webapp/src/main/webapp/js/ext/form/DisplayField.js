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

/*
 * ! Ext JS Library 3.2.1 Copyright(c) 2006-2010 Ext JS, Inc.
 * licensing@extjs.com http://www.extjs.com/license
 */
/**
 * @class Ext.form.DisplayField
 * @extends Ext.form.Field A display-only text field which is not validated and
 *          not submitted.
 * @constructor Creates a new DisplayField.
 * @param {Object}
 *          config Configuration options
 * @xtype displayfield
 */
Ext.form.DisplayField = Ext.extend(Ext.form.Field, {
  validationEvent : false,
  validateOnBlur : false,
  defaultAutoCreate : {
    tag : "div"
  },
  /**
   * @cfg {String} fieldClass The default CSS class for the field (defaults to
   *      <tt>"x-form-display-field"</tt>)
   */
  fieldClass : "x-form-display-field",
  /**
   * @cfg {Boolean} htmlEncode <tt>false</tt> to skip HTML-encoding the text
   *      when rendering it (defaults to <tt>false</tt>). This might be
   *      useful if you want to include tags in the field's innerHTML rather
   *      than rendering them as string literals per the default logic.
   */
  htmlEncode : false,

  // private
  initEvents : Ext.emptyFn,

  isValid : function() {
    return true;
  },

  validate : function() {
    return true;
  },

  getRawValue : function() {
    var v = this.rendered ? this.el.dom.innerHTML : Ext.value(this.value, '');
    if (v === this.emptyText)
    {
      v = '';
    }
    if (this.htmlEncode)
    {
      v = Ext.util.Format.htmlDecode(v);
    }
    return v;
  },

  getValue : function() {
    return this.getRawValue();
  },

  getName : function() {
    return this.name;
  },

  setRawValue : function(v) {
    if (this.htmlEncode)
    {
      v = Ext.util.Format.htmlEncode(v);
    }
    return this.rendered ? (this.el.dom.innerHTML = (Ext.isEmpty(v) ? '' : v)) : (this.value = v);
  },

  setValue : function(v) {
    this.setRawValue(v);
    return this;
  }
    /**
     * @cfg {String} inputType
     * @hide
     */
    /**
     * @cfg {Boolean} disabled
     * @hide
     */
    /**
     * @cfg {Boolean} readOnly
     * @hide
     */
    /**
     * @cfg {Boolean} validateOnBlur
     * @hide
     */
    /**
     * @cfg {Number} validationDelay
     * @hide
     */
    /**
     * @cfg {String/Boolean} validationEvent
     * @hide
     */
  });

Ext.reg('displayfield', Ext.form.DisplayField);

