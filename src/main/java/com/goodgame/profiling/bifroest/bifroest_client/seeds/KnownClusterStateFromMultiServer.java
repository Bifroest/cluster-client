package com.goodgame.profiling.bifroest.bifroest_client.seeds;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;

import com.goodgame.profiling.bifroest.bifroest_client.metadata.ClusterState;

public final class KnownClusterStateFromMultiServer implements KnownClusterStateRequester {
    private static final Logger log = LogManager.getLogger();
    
    @Override
    public Optional<ClusterState> request( HostPortPair target ) {
        try {
            try ( Socket requestSocket = new Socket( target.host(), target.port() ) ) {
                BufferedWriter writer = new BufferedWriter( new OutputStreamWriter( requestSocket.getOutputStream() ));
                writer.write( new JSONObject().put( "command", "get-known-cluster-state" ).toString() );
                writer.write( '\n' );
                writer.flush();

                BufferedReader reader = new BufferedReader( new InputStreamReader( requestSocket.getInputStream() ));
                String rawResponse = reader.readLine();
                log.debug( "Response from {} is: {}", target, rawResponse );
                JSONObject response = new JSONObject( rawResponse );
                if ( response.getBoolean( "answered" ) ) {
                    return Optional.of( ClusterState.fromJSON( response.getJSONObject( "cluster-state" ) ) );
                } else {
                    return Optional.empty();
                }
            }
        } catch ( IOException e ) {
            log.warn( "Cannot communicate with {}: {}", target.toString(), e.getMessage() );
            return Optional.empty();
        }
    }
}
