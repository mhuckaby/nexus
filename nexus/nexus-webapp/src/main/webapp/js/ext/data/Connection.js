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

Ext.override(Ext.data.Connection, {
      request : function(o) {
        if (this.fireEvent("beforerequest", this, o) !== false)
        {
          var p = o.params;

          if (typeof p == "function")
          {
            p = p.call(o.scope || window, o);
          }
          if (typeof p == "object")
          {
            p = Ext.urlEncode(p);
          }
          if (this.extraParams)
          {
            var extras = Ext.urlEncode(this.extraParams);
            p = p ? (p + '&' + extras) : extras;
          }

          var url = o.url || this.url;
          if (typeof url == 'function')
          {
            url = url.call(o.scope || window, o);
          }

          if (o.form)
          {
            var form = Ext.getDom(o.form);
            url = url || form.action;

            var enctype = form.getAttribute("enctype");
            if (o.isUpload || (enctype && enctype.toLowerCase() == 'multipart/form-data'))
            {
              // hack for IE if a non success response is received, we can't
              // access the response data
              // IE denies access
              if (Ext.isIE)
              {
                if (url.indexOf('?') >= 0)
                {
                  url += '&forceSuccess=true';
                }
                else
                {
                  url += '?forceSuccess=true';
                }
              }

              if (Sonatype.utils.authToken)
              {
                // Add auth header to each request
                return this.doFormUpload(o, p, Sonatype.utils.appendAuth(url));
              }
              else
              {
                return this.doFormUpload(o, p, url);
              }
            }
            var f = Ext.lib.Ajax.serializeForm(form);
            p = p ? (p + '&' + f) : f;
          }

          var hs = o.headers;
          if (this.defaultHeaders)
          {
            // Sonatype: default header fix
            hs = Ext.applyIf(hs || {}, this.defaultHeaders);
            if (!o.headers)
            {
              o.headers = hs;
            }
          }

          if(o.xmlData)
          {
              if (!hs || !hs['Content-Type']){
                  hs['Content-Type'] = 'text/xml; charset=utf-8';
              }
          }
          else if(o.jsonData)
          {
              if (!hs || !hs['Content-Type']){
                  hs['Content-Type'] = 'application/json; charset=utf-8';
              }
          }


          if (Sonatype.utils.authToken)
          {
            // Add auth header to each request
            o.headers.Authorization = 'Basic ' + Sonatype.utils.authToken
          }

          var cb = {
            success : this.handleResponse,
            failure : this.handleFailure,
            scope : this,
            argument : {
              options : o
            },
            timeout : o.timeout || this.timeout
          };

          var method = o.method || this.method || (p ? "POST" : "GET");

          if (method == 'GET' && (this.disableCaching && o.disableCaching !== false) || o.disableCaching === true)
          {
            url += (url.indexOf('?') != -1 ? '&' : '?') + '_dc=' + (new Date().getTime());
          }

          if (typeof o.autoAbort == 'boolean')
          {
            if (o.autoAbort)
            {
              this.abort();
            }
          }
          else if (this.autoAbort !== false)
          {
            this.abort();
          }
          if ((method == 'GET' && p) || o.xmlData || o.jsonData)
          {
            url += (url.indexOf('?') != -1 ? '&' : '?') + p;
            p = '';
          }
          this.transId = Ext.lib.Ajax.request(method, url, cb, p, o);
          return this.transId;
        }
        else
        {
          Ext.callback(o.callback, o.scope, [o, null, null]);
          return null;
        }
      }
    });
