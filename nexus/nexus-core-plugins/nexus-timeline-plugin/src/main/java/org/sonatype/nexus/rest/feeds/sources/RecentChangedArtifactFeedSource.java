/**
 * Copyright (c) 2008-2011 Sonatype, Inc.
 * All rights reserved. Includes the third-party code listed at http://links.sonatype.com/products/nexus/oss/attributions
 *
 * This program is free software: you can redistribute it and/or modify it only under the terms of the GNU Affero General
 * Public License Version 3 as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Affero General Public License Version 3
 * for more details.
 *
 * You should have received a copy of the GNU Affero General Public License Version 3 along with this program.  If not, see
 * http://www.gnu.org/licenses.
 *
 * Sonatype Nexus (TM) Open Source Version is available from Sonatype, Inc. Sonatype and Sonatype Nexus are trademarks of
 * Sonatype, Inc. Apache Maven is a trademark of the Apache Foundation. M2Eclipse is a trademark of the Eclipse Foundation.
 * All other trademarks are the property of their respective owners.
 */
package org.sonatype.nexus.rest.feeds.sources;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.sonatype.nexus.feeds.NexusArtifactEvent;
import org.sonatype.nexus.feeds.RepositoryIdTimelineFilter;
import org.sonatype.nexus.timeline.Entry;

import com.google.common.base.Predicate;

/**
 * @author Juven Xu
 */
@Component( role = FeedSource.class, hint = "recentlyChangedArtifacts" )
public class RecentChangedArtifactFeedSource
    extends AbstractNexusItemEventFeedSource
{
    @Requirement( hint = "artifact" )
    private SyndEntryBuilder<NexusArtifactEvent> entryBuilder;

    public static final String CHANNEL_KEY = "recentlyChangedArtifacts";

    public String getFeedKey()
    {
        return CHANNEL_KEY;
    }

    public String getFeedName()
    {
        return getDescription();
    }

    @Override
    public String getDescription()
    {
        return "Recent artifact storage changes in all Nexus repositories (caches, deployments, deletions).";
    }

    @Override
    public List<NexusArtifactEvent> getEventList( Integer from, Integer count, Map<String, String> params )
    {
        final Set<String> repositoryIds = getRepoIdsFromParams( params );
        final Predicate<Entry> filter =
            ( repositoryIds == null || repositoryIds.isEmpty() ) ? null
                : new RepositoryIdTimelineFilter( repositoryIds );

        return getFeedRecorder().getNexusArtifectEvents(
            new HashSet<String>( Arrays.asList( NexusArtifactEvent.ACTION_CACHED, NexusArtifactEvent.ACTION_DEPLOYED,
                NexusArtifactEvent.ACTION_DELETED ) ), from, count, filter );
    }

    @Override
    public String getTitle()
    {
        return "Recent artifact storage changes";
    }

    @Override
    public SyndEntryBuilder<NexusArtifactEvent> getSyndEntryBuilder( NexusArtifactEvent event )
    {
        return entryBuilder;
    }

}