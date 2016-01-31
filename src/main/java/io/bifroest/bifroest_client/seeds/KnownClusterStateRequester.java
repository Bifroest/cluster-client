package io.bifroest.bifroest_client.seeds;

import java.util.Optional;

import io.bifroest.bifroest_client.metadata.ClusterState;

/**
 * This interface exists to make some node states easier, and more
 * testable
 */
public interface KnownClusterStateRequester {
    Optional<ClusterState> request( HostPortPair target );
}
