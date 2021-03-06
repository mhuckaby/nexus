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
package org.sonatype.nexus.gwt.client.services;

import org.sonatype.gwt.client.handler.EntityResponseHandler;
import org.sonatype.gwt.client.handler.StatusResponseHandler;
import org.sonatype.gwt.client.resource.Representation;

/**
 * Nexus Repository Service.
 * 
 * @author cstamas
 */
public interface RepositoryService
{
    /**
     * Creates a repository based on representation.
     * 
     * @param representation
     */
    void create( Representation representation, StatusResponseHandler handler );

    /**
     * Reads the repository State Object.
     * 
     * @param handler
     */
    void read( EntityResponseHandler handler );

    /**
     * Updates the repository state with representation.
     * 
     * @param representation
     */
    void update( Representation representation, StatusResponseHandler handler );

    /**
     * Deletes this repository.
     * 
     * @param handler
     */
    void delete( StatusResponseHandler handler );

    /**
     * Reads this repository meta data.
     * 
     * @param handler
     */
    void readRepositoryMeta( EntityResponseHandler handler );

    /**
     * Reads the current status of this repository.
     * 
     * @param handler
     */
    void readRepositoryStatus( EntityResponseHandler handler );

    /**
     * Updates the status of this repository and returns the new status.
     * 
     * @param representation
     * @param handler
     */
    void updateRepositoryStatus( Representation representation, EntityResponseHandler handler );
}
