package com.goodgame.profiling.bifroest.bifroest_client.seeds;

import java.util.Optional;

import com.goodgame.profiling.bifroest.bifroest_client.metadata.ClusterState;

/**
 * This interface exists to make some node states easier, and more
 * testable
 */
public interface KnownClusterStateRequester {
    Optional<ClusterState> request( HostPortPair target );
}
