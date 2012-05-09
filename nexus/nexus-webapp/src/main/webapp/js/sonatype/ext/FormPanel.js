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
 * Ext.form.js Sonatype specific Ext Form overrides and extensions
 * Ext.form.Field override to provide help text quick tip Sonatype
 * implementations of form Actions Note: Ext namespace is maintained
 */

/*
 * Generic object editor (intended to be subclassed). When used with
 * Sonatype.panels.GridViewer, instanced of this editor panel will never be
 * reused. When the form is submitted, all child panels related to this grid
 * record are re-created, so the editor does not have to worry about altering
 * its state after submit (as opposed to having to disable certain fields after
 * the new record is saved, which we had to do previously). Config options:
 * cancelButton: if set to "true", the form will display a "Cancel" button, so
 * the invoker can subscribe to a "cancel" event and do the necessary cleanup
 * (e.g. close the panel). By default, the form will display a "Reset" button
 * instead, which reloads the form when clicked. Note a special dataModifier of
 * 'rootData' will work with entire contents of json response rather than on a
 * field by field basis. Using this modifier will then render other modifiers
 * useless dataModifiers: { // data modifiers on form submit/load load: { attr1:
 * func1, attr2: func2 }, save: { ... } } dataStores: an array of data stores
 * this editor depends on. It will make sure all stores are loaded before the
 * form load request is sent. The stores should be configured with auto load
 * off. listeners: { // custom events offered by the editor panel cancel: { fn:
 * function( panel ) { // do cleanup, remove the form from the container, //
 * delete the temporary grid record, etc. }, scope: this }, load: { fn:
 * function( form, action, receivedData ) { // do extra work for data load if
 * needed }, scope: this }, submit: { fn: function( form, action, receivedData ) { //
 * update the grid record and do other stuff if needed var rec = this.payload;
 * rec.beginEdit(); rec.set( 'attr1', receivedData.attr1 ); rec.set( 'attr2',
 * receivedData.attr2 ); rec.commit(); rec.endEdit(); }, scope: this } }
 * payload: the grid record being edited referenceData: a reference data object
 * that's used as a template on form submit uri: base URL for JSON requests. It
 * will be used for POST requests when creating a new object, or for PUT (with
 * payload.id appended) if the record does not a resourceURI attribute
 */
Sonatype.ext.FormPanel = function(config) {
  var config = config || {};
  var defaultConfig = {
    region : 'center',
    width : '100%',
    height : '100%',
    autoScroll : true,
    border : false,
    frame : true,
    collapsible : false,
    collapsed : false,
    labelWidth : 200,
    layoutConfig : {
      labelSeparator : ''
    }
  };
  Ext.apply(this, config, defaultConfig);

  this.checkPayload();
  if (this.isNew && this.cancelButton == null)
  {
    this.cancelButton = true;
  }

  Sonatype.ext.FormPanel.superclass.constructor.call(this, {
        buttons : (config.readOnly || this.readOnly) ? [] : [{
              text : 'Save',
              handler : this.saveHandler,
              scope : this
            }, {
              handler : this.cancelButton ? this.cancelHandler : this.resetHandler,
              scope : this,
              text : this.cancelButton ? 'Cancel' : 'Reset'
            }]
      });

  this.on('afterlayout', this.initData, this, {
        single : true
      });
  this.on('afterlayout', this.registerRequiredQuicktips, this, {
        single : true
      });
  this.form.on('actioncomplete', this.actionCompleteHandler, this);
  this.form.on('actionfailed', this.actionFailedHandler, this);
  this.addEvents({
        cancel : true,
        load : true,
        submit : true
      });
};

Ext.extend(Sonatype.ext.FormPanel, Ext.FormPanel, {
      convertDataValue : function(value, store, idProperty, nameProperty) {
        if (value)
        {
          var rec = store.getAt(store.find(idProperty, value));
          if (rec)
          {
            return rec.data[nameProperty];
          }
        }
        return '';
      },
      checkPayload : function() {
        this.isNew = false;
        if (this.payload)
        {
          if (this.payload.id.substring(0, 4) == 'new_')
          {
            this.isNew = true;
          }
        }
      },

      checkStores : function() {
        if (this.dataStores)
        {
          for (var i = 0; i < this.dataStores.length; i++)
          {
            var store = this.dataStores[i];
            if (store.lastOptions == null)
            {
              return false;
            }
          }
        }
        return true;
      },

      dataStoreLoadHandler : function(store, records, options) {
        if (this.checkStores())
        {
          this.loadData();
        }
      },

      registerRequiredQuicktips : function(formPanel, fLayout) {
        // register required field quicktip, but have to wait for elements to
        // show up in DOM
        var temp = function() {
          var els = Ext.select('.required-field .x-form-item-label, .required-field .x-panel-header-text', this.getEl());
          els.each(function(el, els, i) {
                Ext.QuickTips.register({
                      target : el,
                      cls : 'required-field',
                      title : '',
                      text : 'Required Field',
                      enabled : true
                    });
              });
        }.defer(300, formPanel);
      },

      cancelHandler : function(button, event) {
        this.fireEvent('cancel', this);
      },

      resetHandler : function(button, event) {
        this.loadData();
      },

      initData : function() {
        if (this.dataStores)
        {
          for (var i = 0; i < this.dataStores.length; i++)
          {
            var store = this.dataStores[i];
            store.on('load', this.dataStoreLoadHandler, this);
            if (store.autoLoad != true)
            {
              store.load();
            }
          }
        }
        else
        {
          this.loadData();
        }
      },

      loadData : function() {
        if (this.isNew)
        {
          this.form.reset();
        }
        else
        {
          this.form.doAction('sonatypeLoad', {
                url : this.getActionURL(),
                method : 'GET',
                fpanel : this,
                dataModifiers : this.dataModifiers.load,
                scope : this
              });
        }
      },

      isValid : function() {
        return this.form.isValid();
      },

      saveHandler : function(button, event) {
        if (this.isValid())
        {
          this.form.doAction('sonatypeSubmit', {
            method : this.getSaveMethod(),
            url : this.getActionURL(),
            waitMsg : this.isNew ? 'Creating a new record...' : 'Updating records...',
            fpanel : this,
            validationModifiers : this.validationModifiers,
            dataModifiers : this.dataModifiers.submit,
            serviceDataObj : this.referenceData,
            isNew : this.isNew
              // extra option to send to callback, instead of conditioning on
              // method
            });
        }
      },

      actionFailedHandler : function(form, action) {
        if (action.failureType == Ext.form.Action.CLIENT_INVALID)
        {
          Sonatype.MessageBox.alert('Missing or Invalid Fields', 'Please change the missing or invalid fields.').setIcon(Sonatype.MessageBox.WARNING);
        }
        else if (action.failureType == Ext.form.Action.CONNECT_FAILURE || action.response)
        {
          Sonatype.utils.connectionError(action.response, 'There is an error communicating with the server.');
        }
        else if (action.failureType == Ext.form.Action.LOAD_FAILURE)
        {
          Sonatype.MessageBox.alert('Load Failure', 'The data failed to load from the server.').setIcon(Sonatype.MessageBox.ERROR);
        }
      },

      // (Ext.form.BasicForm, Ext.form.Action)
      actionCompleteHandler : function(form, action) {
        var receivedData = action.handleResponse(action.response).data;
        if (receivedData == null)
        {
          receivedData = {};
        }
        if (action.type == 'sonatypeSubmit')
        {
          this.fireEvent('submit', form, action, receivedData);

          if (this.isNew && this.payload.autoCreateNewRecord)
          {
            var store = this.payload.store;
            store.remove(this.payload);

            if (Ext.isArray(receivedData))
            {
              for (var i = 0; i < receivedData.length; i++)
              {
                var r = receivedData[i];
                var rec = new store.reader.recordType(r, r.resourceURI);
                this.addSorted(store, rec);
              }
            }
            else
            {
              var rec = new store.reader.recordType(receivedData, receivedData.resourceURI);
              rec.autoCreateNewRecord = true;
              this.addSorted(store, rec);
            }
          }
          this.isNew = false;
          this.payload.autoCreateNewRecord = false;
        }
        else if (action.type == 'sonatypeLoad')
        {
          this.fireEvent('load', form, action, receivedData);
        }
      },

      addSorted : function(store, rec) {
        store.addSorted(rec);
      },

      getActionURL : function() {
        return this.isNew ? this.uri : // if new, return the uri
            (this.payload.data.resourceURI ? // if resouceURI is supplied,
                // return it
                this.payload.data.resourceURI
                : this.uri + '/' + this.payload.id); // otherwise construct a
        // uri
      },

      getSaveMethod : function() {
        return this.isNew ? 'POST' : 'PUT';
      },

      optionalFieldsetExpandHandler : function(panel) {
        panel.items.each(function(item, i, len) {
              if (item.getEl().up('div.required-field', 3))
              {
                item.allowBlank = false;
              }
              else if (item.isXType('fieldset', true))
              {
                this.optionalFieldsetExpandHandler(item);
              }
            }, this);
      },

      optionalFieldsetCollapseHandler : function(panel) {
        panel.items.each(function(item, i, len) {
              if (item.getEl().up('div.required-field', 3))
              {
                item.allowBlank = true;
              }
              else if (item.isXType('fieldset', true))
              {
                this.optionalFieldsetCollapseHandler(item);
              }
            }, this);
      }
    });

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

Ext.form.TimestampDisplayField = Ext.extend(Ext.form.DisplayField, {
      setValue : function(v) {
        // java give the timestamp in miliseconds, extjs consumes it in seconds
        var toSecs = Math.round(v / 1000);
        v = new Date.parseDate(toSecs, 'U').toString();
        this.setRawValue(v);
        return this;
      }
    });

Ext.reg('timestampDisplayField', Ext.form.TimestampDisplayField);

Ext.form.ByteDisplayField = Ext.extend(Ext.form.DisplayField, {
      setValue : function(v) {
        if (v < 1024)
        {
          v = v + ' Bytes';
        }
        else if (v < 1048576)
        {
          v = (v / 1024).toFixed(2) + ' KB';
        }
        else if (v < 1073741824)
        {
          v = (v / 1048576).toFixed(2) + ' MB';
        }
        else
        {
          v = (v / 1073741824).toFixed(2) + ' GB';
        }
        this.setRawValue(v);
        return this;
      }
    });

Ext.reg('byteDisplayField', Ext.form.ByteDisplayField);

Ext.override(Ext.form.TextField, {
  /**
   * @cfg {Boolean} htmlDecode
   * <tt>true</tt> to decode html entities in the value given to
   * Ext.form.ByteDisplayField.setValue and Ext.form.ByteDisplayField.setRawValue
   * before setting the actual value.
   * <p/>
   * This is needed for displaying the 'literal' value in the text field when it was received by the server,
   * for example in the repository name. The REST layer will encode to html entities, which will be correct
   * for html rendering, but text fields without this configuration will display '&quot;test&quot;' instead
   * of the originally sent '"test"'.
   */
  htmlDecode : false,

  /**
   * @cfg {Boolean} htmlConvert
   * <tt>true</tt> to decode html entities in the value given to
   * Ext.form.TextField.set(Raw)Value
   * before setting the actual value, and encode html entities again
   * in the call to Ext.form.TextField.get(Raw)Value.
   * <p/>
   * This is needed for displaying the 'literal' value in the text field when it was received by the server
   * (see htmlDecode configuration doc), and display to the user correctly before round-tripping to the server again
   * (e.g. in a grid field).
   * <p/>
   * when this config is set, the value has to be html-decoded again before sending it to the server, because the REST layer
   * will encode the string again.
   */
  htmlConvert : false,

  setRawValue : function(value) {
    if ( this.htmlDecode || this.htmlConvert )
    {
      value = Ext.util.Format.htmlDecode(value);
    }
    Ext.form.TextField.superclass.setRawValue.call(this, value);
  },
  setValue : function(value) {
    if ( this.htmlDecode || this.htmlConvert )
    {
      value = Ext.util.Format.htmlDecode(value);
    }
    Ext.form.TextField.superclass.setValue.call(this, value);
  },
  getRawValue : function() {
    var value = Ext.form.TextField.superclass.getRawValue.call(this);
    if ( this.htmlConvert )
    {
      value = Ext.util.Format.htmlEncode(value);
    }
    return value;
  },
  getValue : function() {
    var value = Ext.form.TextField.superclass.getValue.call(this);
    if ( this.htmlConvert )
    {
      value = Ext.util.Format.htmlEncode(value);
    }
    return value;
  }
});


