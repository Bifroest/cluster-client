package io.bifroest.bifroest_client;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.time.Duration;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.stream.Collectors;

import org.apache.commons.collections4.map.LazyMap;
import org.apache.commons.lang3.concurrent.BasicThreadFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import io.bifroest.balancing.BucketMapping;
import io.bifroest.bifroest_client.communication.BifroestCommunication;
import io.bifroest.bifroest_client.communication.SocketBasedBifroestCommunication;
import io.bifroest.bifroest_client.metadata.ClusterState;
import io.bifroest.bifroest_client.metadata.ClusterStateWithVersionedMapping;
import io.bifroest.bifroest_client.metadata.ClusterStateWithVersionedMapping.MappingWithVersion;
import io.bifroest.bifroest_client.metadata.NodeMetadata;
import io.bifroest.bifroest_client.util.JSONClient;
import io.bifroest.commons.model.Interval;
import io.bifroest.commons.model.Metric;
import io.bifroest.retentions.MetricSet;

public class BasicClient implements BifroestClient, ClientCommands {
    private static final Logger log = LogManager.getLogger();

    private final ClusterStateWithVersionedMapping clusterState;
    private final BifroestCommunication communication;

    private final ExecutorService clientBrain;

    private final Duration waitAfterReject;
    private final int retriesAfterReject;

    public BasicClient( Duration pingFrequency, Collection<NodeMetadata> initialNodes, Duration waitAfterReject, int retriesAfterReject ) throws IOException {
        this.clusterState = new ClusterStateWithVersionedMapping( new ClusterState(), waitAfterReject );
        this.communication = new SocketBasedBifroestCommunication( this, pingFrequency );
        ThreadFactory factory = new BasicThreadFactory.Builder().namingPattern( "ClientBrain-%d" ).build();
        this.clientBrain = Executors.newFixedThreadPool( 1, factory );

        log.info( "Connecting to initial nodes {}", initialNodes );
        this.clusterState.addAll( initialNodes );
        this.communication.connectAndIntroduceToAll( initialNodes );
        this.waitAfterReject = waitAfterReject;
        this.retriesAfterReject = retriesAfterReject;
    }

    @Override
    public void reactToJoinSoon( NodeMetadata joinedNodeMetadata ) {
        clientBrain.execute( () -> {
            log.info( "Node joined: {}", joinedNodeMetadata );
            clusterState.addNode( joinedNodeMetadata );
        });
    }

    @Override
    public void reactToMappingSoon( BucketMapping<NodeMetadata> mapping ) {
        clientBrain.execute( () -> {
            log.info( "New mapping: {}", mapping );
            clusterState.setBucketMapping( mapping );
        });
    }

    @Override
    public void reactToLeaveSoon( NodeMetadata leavingNodeMetadata ) {
        clientBrain.execute( () -> {
            log.info( "Node left: {}", leavingNodeMetadata );
            clusterState.removeNode( leavingNodeMetadata );
            try {
                communication.disconnectFrom( leavingNodeMetadata );
            } catch ( IOException e ) {
                log.warn( "Error when disconnecting from {}:", leavingNodeMetadata, e );
            }
        });
    }

    @Override
    public void reactToNewNodeSoon( NodeMetadata newNode ) {
        clientBrain.execute( () -> {
            log.info( "Node joined: {}", newNode );
            clusterState.addNode( newNode );
            try {
                communication.connectAndIntroduceTo(newNode );
            } catch ( IOException e ) {
                log.warn( "Error when connecting to {}:", newNode, e );
            }
        });
    }


    @Override
    public void sendMetrics( Collection<Metric> metrics ) throws IOException {
        BucketMapping<NodeMetadata> currentMapping = clusterState.getMappingWithVersion().getMapping();
        Map<NodeMetadata, List<Metric>> metricsByTarget = metrics.stream()
                                                                 .collect( Collectors.groupingBy( m -> currentMapping.getNodeFor( m.name().hashCode() )));
        for( Map.Entry<NodeMetadata, List<Metric>> targetAndMetrics : metricsByTarget.entrySet() ) {
            NodeMetadata target = targetAndMetrics.getKey();
            List<Metric> metricsForTarget = targetAndMetrics.getValue();

            try ( Socket toTarget = new Socket( target.getAddress(), target.getPorts().getFastIncludeMetricPort() ) ) {
                try (BufferedWriter writerToTarget = new BufferedWriter( new OutputStreamWriter( toTarget.getOutputStream() ) )) {
                    for ( Metric metric : metricsForTarget ) {
                        writerToTarget.write( String.format("%s %f %d\n", metric.name(), metric.value(), metric.timestamp() ) );
                    }
                }
            }
        }
    }

    @Override
    public MetricSet getMetrics( String metricName, Interval interval ) {
        return getMetrics( metricName, interval, retriesAfterReject );
    }

    private MetricSet getMetrics( String metricName, Interval interval, int retriesRemaining ) {
        MappingWithVersion currentMapping = clusterState.getMappingWithVersion();
        NodeMetadata target = currentMapping.getMapping().getNodeFor( metricName.hashCode() );
        log.debug( "Will request metric '{}' from node: {}", metricName, target.toString() );
        JSONObject command = new JSONObject()
                .put( "command", "get-metric-set" )
                .put( "name", metricName )
                .put( "startTimestamp", interval.start() )
                .put( "endTimestamp", interval.end() );

        JSONObject answer = JSONClient.request( target.getAddress(), target.getPorts().getMetricSetPort(), command );

        boolean nodeDidntWantToAnswer = answer.has( "answered" ) && ! answer.getBoolean( "answered" );
        boolean nodeDoesntThinkItCaches = answer.has( "cached-here" ) && ! answer.getBoolean( "cached-here" );
        boolean nodeIsCurrentlyPreloading = answer.has( "preloading-metric" ) && answer.getBoolean( "preloading-metric" );

        if ( nodeDidntWantToAnswer || nodeIsCurrentlyPreloading ) {
            if ( retriesRemaining > 0 ) {
                try {
                    Thread.sleep( waitAfterReject.toMillis() );
                } catch (InterruptedException e) {
                    log.warn( "Interrupted while waiting after rejection", e );
                }
                log.debug( "Node did not answer ({}) or node is currently preloading ({}). Retrying again ({} times)",
                           nodeDidntWantToAnswer, nodeIsCurrentlyPreloading, retriesRemaining );
                return getMetrics( metricName, interval, retriesRemaining - 1 );
            }
            log.debug( "Node did not answer ({}) or node is currently preloading ({}). Will NOT retry again",
                       nodeDidntWantToAnswer, nodeIsCurrentlyPreloading );
            return null;
        } else if ( nodeDoesntThinkItCaches ) {
            if ( retriesRemaining > 0 ) {
                log.debug( "Node does not think it caches. Will wait for a new mapping and retry again ({} times)", retriesRemaining );
                clusterState.waitForMappingNewerThan( currentMapping );
                return getMetrics( metricName, interval, retriesRemaining - 1 );
            }
            log.debug( "Node does not think it caches. Will NOT retry again" );
            return null;
        } else {
            log.debug( "Got metric '{}' from {}", metricName, target.toString() );
            return MetricSet.fromJSON( answer );
        }
   }


    public enum MetricState {
        BRANCH, LEAF;

        static MetricState ofIsLeaf( boolean isLeaf ) {
            return isLeaf ? LEAF : BRANCH;
        }
        
        static MetricState combine( MetricState left, MetricState right ) {
            if ( left == BRANCH || right == BRANCH ) {
                // if any bifroest thinks a metric is a branch, we need to tell
                // graphite there are more metrics to get.
                return BRANCH;
            } else {
                return LEAF;
            }
        }
    }
    @Override
    public Map<String, MetricState> getSubMetrics( String query ) {
        JSONObject command = new JSONObject()
                .put( "command", "get-sub-metrics" )
                .put( "query", query );

        Map<String, MetricState> result = LazyMap.lazyMap( new HashMap<>(), k -> MetricState.LEAF );
        
        for ( NodeMetadata target : clusterState.getKnownNodes() ) {
            JSONObject answer = JSONClient.request( target.getAddress(), target.getPorts().getSubmetricPort(), command );
            JSONArray answerResults = answer.getJSONArray( "results" );
            
            for (int i = 0; i < answerResults.length(); i++) {
                JSONObject subMetric = answerResults.getJSONObject(i);
                String path = subMetric.getString( "path" );
                boolean isLeaf = subMetric.getBoolean( "isLeaf" );
                
                result.put( path, MetricState.combine( result.get( path ), MetricState.ofIsLeaf( isLeaf ) ) );
            }
        }
        return result;
    }

    @Override
    public void shutdown() {
        communication.shutdown();
    }
}
