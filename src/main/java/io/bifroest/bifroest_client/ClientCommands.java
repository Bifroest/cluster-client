package io.bifroest.bifroest_client;

import io.bifroest.balancing.BucketMapping;
import io.bifroest.bifroest_client.metadata.NodeMetadata;

public interface ClientCommands {
    void reactToJoinSoon( NodeMetadata joinedNodeMetadata );
    void reactToMappingSoon( BucketMapping<NodeMetadata> mapping );
    void reactToLeaveSoon( NodeMetadata leavingNodeMetadata );
    void reactToNewNodeSoon( NodeMetadata newNode );
}
