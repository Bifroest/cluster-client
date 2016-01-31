package io.bifroest.bifroest_client;

import com.goodgame.profiling.bifroest.balancing.BucketMapping;
import com.goodgame.profiling.bifroest.bifroest_client.metadata.NodeMetadata;

public interface ClientCommands {
    void reactToJoinSoon( NodeMetadata joinedNodeMetadata );
    void reactToMappingSoon( BucketMapping<NodeMetadata> mapping );
    void reactToLeaveSoon( NodeMetadata leavingNodeMetadata );
    void reactToNewNodeSoon( NodeMetadata newNode );
}
