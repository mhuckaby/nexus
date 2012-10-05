/**
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

package org.sonatype.nexus.proxy.storage.remote.httpclient;

import com.google.common.base.Throwables;
import org.apache.http.client.HttpClient;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.impl.conn.PoolingClientConnectionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.InstanceAlreadyExistsException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;
import java.lang.management.ManagementFactory;
import java.util.Hashtable;

/**
 * ???
 *
 * @since 2.2
 */
public class HttpClientMBeanInstaller
{
    private static final Logger log = LoggerFactory.getLogger(HttpClientMBeanInstaller.class);

    public static final String DOMAIN = "org.sonatype.nexus.proxy.storage.remote.httpclient";

    private static ObjectName constructClientName(final String id) {
        Hashtable<String, String> properties = new Hashtable<String, String>();
        properties.put("repoId", id);
        properties.put("type", "client");
        try {
            return ObjectName.getInstance(DOMAIN, properties);
        }
        catch (MalformedObjectNameException e) {
            throw Throwables.propagate(e);
        }
    }

    private static ObjectName constructPoolName(final String id) {
        Hashtable<String, String> properties = new Hashtable<String, String>();
        properties.put("repoId", id);
        properties.put("type", "pool");
        try {
            return ObjectName.getInstance(DOMAIN, properties);
        }
        catch (MalformedObjectNameException e) {
            throw Throwables.propagate(e);
        }
    }

    public static void install(final String id, final HttpClient client) {
        register(new HttpClientMBeanImpl(client), constructClientName(id));

        ClientConnectionManager connectionManager = client.getConnectionManager();
        if (connectionManager instanceof PoolingClientConnectionManager) {
            register(new PoolingClientConnectionManagerMBeanImpl((PoolingClientConnectionManager)connectionManager), constructPoolName(id));
        }
        else {
            log.warn("No supported connection manager mbean for: {}", connectionManager.getClass().getName());
        }
    }

    public static void uninstall(final String id) {
        unregister(constructPoolName(id));
        unregister(constructClientName(id));
    }

    private static MBeanServer getServer() {
        return ManagementFactory.getPlatformMBeanServer();
    }

    private static void unregister(final ObjectName objectName) {
        try {
            MBeanServer server = getServer();
            if (server.isRegistered(objectName)) {
                server.unregisterMBean(objectName);
            }
        }
        catch (Exception e) {
            log.debug("Failed to unregister mbean: {}", objectName);
        }
    }

    private static void register(final Object object, final ObjectName objectName) {
        try {
            MBeanServer server = getServer();
            server.registerMBean(object, objectName);
        }
        catch (Exception e) {
            log.warn("Failed to register mbean: {}", objectName);
        }
    }
}
