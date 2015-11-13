package com.goodgame.profiling.bifroest.bifroest_client;


import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.json.JSONArray;
import org.json.JSONObject;
import org.kohsuke.MetaInfServices;

import com.goodgame.profiling.bifroest.bifroest_client.metadata.ClusterState;
import com.goodgame.profiling.bifroest.bifroest_client.seeds.HostPortPair;
import com.goodgame.profiling.bifroest.bifroest_client.seeds.KnownClusterStateFromMultiServer;
import com.goodgame.profiling.bifroest.bifroest_client.seeds.KnownClusterStateRequester;
import com.goodgame.profiling.commons.boot.interfaces.Subsystem;
import com.goodgame.profiling.commons.statistics.units.parse.DurationParser;

@MetaInfServices
public class BifroestClientSystem<E extends EnvironmentWithMutableBifroestClient> implements Subsystem<E> {
    
    public static final String IDENTIFIER = "bifroest.bifroest-client";
    
    private List<HostPortPair> seeds;

    private Duration pingFrequency;
    private Duration waitAfterReject = Duration.ofMillis( 500 );
    private int retriesAfterReject = 1;

    @Override
    public String getSystemIdentifier() {
        return IDENTIFIER;
    }

    @Override
    public Collection<String> getRequiredSystems() {
        return Collections.emptyList();
    }

    @Override
    public void configure( JSONObject configuration ) {
        JSONObject clientConfig = configuration.getJSONObject( "bifroest-client" );

        seeds = new ArrayList<>();
        JSONArray configSeeds = clientConfig.getJSONArray( "seeds" );
        for ( int i = 0; i < configSeeds.length(); i++ ) {
            JSONObject configSeed = configSeeds.getJSONObject( i );
            seeds.add( HostPortPair.of( configSeed.getString( "host" ),
                                        configSeed.getInt( "port" ) ) );
        }

        pingFrequency = new DurationParser().parse( clientConfig.getString( "ping-frequency" ) );
        if ( clientConfig.has( "wait-after-reject" ) ) {
            waitAfterReject = new DurationParser().parse( clientConfig.getString( "wait-after-reject" ) );
        }
        if ( clientConfig.has( "retries-after-reject" ) ) {
            retriesAfterReject = clientConfig.getInt( "retries-after-reject" );
        }
    }

    @Override
    public void boot( E environment ) throws Exception {
        KnownClusterStateRequester requester = new KnownClusterStateFromMultiServer();

        Optional<ClusterState> clusterState = Optional.empty();
        for ( HostPortPair seed : seeds ) {
            clusterState = requester.request( seed );
            if ( clusterState.isPresent() ) {
                break;
            }
        }

        if ( !clusterState.isPresent() ) {
            throw new IllegalStateException( "No cluster state was received!" );
        }

        BifroestClient client = new BasicClient( pingFrequency, clusterState.get().getKnownNodes(), waitAfterReject, retriesAfterReject );
        environment.setBifroestClient( client );
    }

    @Override
    public void shutdown( E environment ) {
        environment.bifroestClient().shutdown();
    }

}
