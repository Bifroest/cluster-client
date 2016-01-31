package com.goodgame.profiling.bifroest.bifroest_client.metadata;

import com.goodgame.profiling.bifroest.balancing.BucketMapping;
import com.goodgame.profiling.bifroest.balancing.KeepNeighboursMapping;
import com.goodgame.profiling.bifroest.bifroest_client.metadata.NodeMetadata;

import org.json.JSONObject;


public final class MappingFactory {

    private MappingFactory() {
        // Utility class. Avoid instantiation!
    }

    public static BucketMapping<NodeMetadata> fromJSON( JSONObject input ) {
        return KeepNeighboursMapping.fromJSON( input, NodeMetadata::fromJSON );
    }

    public static BucketMapping<NodeMetadata> newMapping( NodeMetadata initialNode) {
        return new KeepNeighboursMapping<>( initialNode );
    }
}
