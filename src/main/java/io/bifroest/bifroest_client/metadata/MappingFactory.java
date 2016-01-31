package io.bifroest.bifroest_client.metadata;

import org.json.JSONObject;

import io.bifroest.balancing.BucketMapping;
import io.bifroest.balancing.KeepNeighboursMapping;

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
