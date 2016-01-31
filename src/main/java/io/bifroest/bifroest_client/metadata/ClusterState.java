package io.bifroest.bifroest_client.metadata;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

import org.json.JSONArray;
import org.json.JSONObject;

import com.goodgame.profiling.bifroest.balancing.BucketMapping;
import com.goodgame.profiling.commons.serialize.json.JSONSerializable;

public final class ClusterState implements JSONSerializable {
    private final List<NodeMetadata> knownNodes = new ArrayList<>();
    private NodeMetadata leader;

    private BucketMapping<NodeMetadata> bucketMapping;

    public Collection<NodeMetadata> getKnownNodes() {
        return knownNodes;
    }

    public void addAll( Iterable<NodeMetadata> manyNodes ) {
        manyNodes.forEach( this::addNode );
    }

    public void addNode( NodeMetadata metadata ) {
        knownNodes.add(  metadata );
    }

    public void removeNode( NodeMetadata metadata ) {
        knownNodes.remove( metadata );
    }

    public void setLeader( NodeMetadata newLeader ) {
        leader = newLeader;
    }

    public NodeMetadata getLeader() {
        return Objects.requireNonNull( leader, "No leader decided yet" );
    }

    public void setBucketMapping( BucketMapping<NodeMetadata> newBucketMapping ) {
        bucketMapping = newBucketMapping;
    }

    public BucketMapping<NodeMetadata> getBucketMapping() {
        return bucketMapping;
    }

    @Override
    public JSONObject toJSON() {
        JSONArray nodes = new JSONArray();
        getKnownNodes().forEach( node -> nodes.put( node.toJSON() ) );
        return new JSONObject()
                .put( "nodes", nodes )
                .put( "leader", getLeader().toJSON() )
                .put( "mapping", getBucketMapping().toJSON() );
    }

    public static ClusterState fromJSON( JSONObject input ) {
        ClusterState result = new ClusterState();

        JSONArray nodes = input.getJSONArray( "nodes" );
        for( int i = 0; i < nodes.length(); i++ ) {
            result.addNode( NodeMetadata.fromJSON( nodes.getJSONObject( i ) ) );
        }

        result.setLeader( NodeMetadata.fromJSON( input.getJSONObject( "leader" ) ) );
        result.setBucketMapping( MappingFactory.fromJSON( input.getJSONObject( "mapping" ) ) );

        return result;
    }
}
