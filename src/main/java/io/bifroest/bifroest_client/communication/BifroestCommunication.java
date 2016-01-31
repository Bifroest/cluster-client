package io.bifroest.bifroest_client.communication;

import java.io.IOException;
import java.util.Collection;

import org.json.JSONObject;

import com.goodgame.profiling.bifroest.bifroest_client.metadata.NodeMetadata;

public interface BifroestCommunication {
    void sendToNode( NodeMetadata target, JSONObject message ) throws IOException;
    void connectAndIntroduceTo( NodeMetadata newNode ) throws IOException;
    void disconnectFrom( NodeMetadata leavingNode ) throws IOException;
    void shutdown();

    default void connectAndIntroduceToAll( Collection<NodeMetadata> allNodes ) throws IOException {
        for ( NodeMetadata node : allNodes ) {
            connectAndIntroduceTo( node );
        }
    }
}
