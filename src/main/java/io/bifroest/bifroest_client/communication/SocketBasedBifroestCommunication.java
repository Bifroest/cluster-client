package com.goodgame.profiling.bifroest.bifroest_client.communication;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.Socket;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;

import com.goodgame.profiling.bifroest.balancing.BucketMapping;
import com.goodgame.profiling.bifroest.bifroest_client.ClientCommands;
import com.goodgame.profiling.bifroest.bifroest_client.metadata.MappingFactory;
import com.goodgame.profiling.bifroest.bifroest_client.metadata.NodeMetadata;
import com.goodgame.profiling.commons.systems.cron.TaskRunner;

public class SocketBasedBifroestCommunication implements BifroestCommunication {
    private static final Logger log = LogManager.getLogger();

    private final ClientCommands client;
    private final Duration pingFrequency;

    private final Map<NodeMetadata, Remote> remotes;

    private final Object theLock = new Object();

    public SocketBasedBifroestCommunication( ClientCommands client, Duration pingFrequency ) {
        this.client = client;
        this.pingFrequency = pingFrequency;
        this.remotes = new HashMap<>();
    }

    @Override
    public void sendToNode( NodeMetadata target, JSONObject message ) throws IOException {
        remotes.get( target ).sendMessage( message );
    }

    @Override
    public void connectAndIntroduceTo( NodeMetadata newNode ) throws IOException {
        synchronized ( theLock ) {
            log.debug( "Creating connection to {}", newNode );
            Socket toNewNode = new Socket( newNode.getAddress(), newNode.getPorts().getClusterPort() );
            Remote remote = new Remote( toNewNode, newNode );
            remotes.put( newNode, remote );
            remote.writeIntroduction();
        }
    }

    @Override
    public void disconnectFrom(NodeMetadata leavingNode) throws IOException {
        remotes.get( leavingNode ).disconnect();
    }

    @Override
    public void shutdown() {
        for ( Remote remote : remotes.values() ) {
            remote.disconnect();
        }
    }

    private final class Remote {
        private final NodeMetadata remoteMetadata;
        private final Writer toNode;
        private final ReaderThread readerThread;
        private final TaskRunner.TaskID pingTask;

        private long sequenceIdSent;
        private long sequenceIdAcked;

        public Remote( Socket socket, NodeMetadata newNode ) throws IOException {
            this.remoteMetadata = newNode;
            this.toNode = new BufferedWriter( new OutputStreamWriter( socket.getOutputStream() ) );
            this.readerThread = new ReaderThread( newNode, new BufferedReader( new InputStreamReader( socket.getInputStream() ) ) );

            this.readerThread.start();

            this.sequenceIdSent = 0;
            this.sequenceIdAcked = 0;
            this.pingTask = startPinging();
        }

        private TaskRunner.TaskID startPinging() {
            return TaskRunner.runRepeated(
                    () -> {
                        if ( sequenceIdAcked != sequenceIdSent ) {
                            log.warn( "Last ping wasn't acknowledged for remote {}. Connection is stale, disconnecting!", remoteMetadata );
                            log.warn( "sequenceIdAcked: {}, sequenceIdSent: {}", sequenceIdAcked, sequenceIdSent );
                            client.reactToLeaveSoon( remoteMetadata );
                        }
                        sendMessage( new JSONObject().put( "command", "ping" )
                                                     .put( "sequence-id", ++sequenceIdSent ) );
                    },
                    "ping-" + remoteMetadata.getFunnyNameForDebugging(),
                    pingFrequency, pingFrequency,
                    true );
        }

        public void writeIntroduction() {
            sendMessage( new JSONObject().put( "command", "hello-i-am-a-client") );
        }

        public void disconnect() {
            TaskRunner.stopTask( pingTask );

            readerThread.dontRunAnymore();
            readerThread.interrupt();

            try {
                toNode.close();
            } catch ( IOException e ) {
                log.warn( "Exception while disconnecting {}: {}", remoteMetadata, e.getMessage() );
            }
        }

        public void sendMessage( JSONObject message ) {
            log.debug( "Sending to {}: {}", remoteMetadata, message.toString() );
            try {
                toNode.write( message.toString() + "\n" );
                toNode.flush();
            } catch ( IOException e ) {
                log.info( "Couldn't send message - connection to {} lost: {}", remoteMetadata, e.getMessage() );
                client.reactToLeaveSoon( remoteMetadata );
            }
        }

        public void onMessage( JSONObject message ) {
            switch( message.getString( "command" ) ) {
            case "pong":
                onPong( message );
                break;
            case "update-mapping":
                onUpdateMapping( message );
                break;
            case "new-node":
                onNewNode( message );
                break;
            default:
                log.warn( "Cannot handle command {}", message.getString( "command" ) );
            }
        }

        private void onPong( JSONObject message ) {
            long got = message.getLong( "sequence-id" );
            long expected = sequenceIdSent;
            if ( got != expected ) {
                log.warn( "Got wrong sequence ID from {} - got {}, expected {}", remoteMetadata, got, expected );
            }
            sequenceIdAcked = got;
        }

        private void onUpdateMapping( JSONObject message ) {
            BucketMapping<NodeMetadata> buckets = MappingFactory.fromJSON( message.getJSONObject( "mapping" ) );
            client.reactToMappingSoon( buckets );
        }

        private void onNewNode( JSONObject message ) {
            NodeMetadata newNode = NodeMetadata.fromJSON( message.getJSONObject( "new-node" ) );
            client.reactToNewNodeSoon( newNode );
        }
    }

    private final class ReaderThread extends Thread {
        private final NodeMetadata source;
        private final BufferedReader fromNode;
        private volatile boolean running;

        public ReaderThread( NodeMetadata source, BufferedReader fromNode ) {
            this.source = source;
            this.fromNode = fromNode;
        }

        public void dontRunAnymore() {
            running = false;
        }

        @Override
        public void run() {
            running = true;
            try {
                while( running ) {
                    log.trace( "Hello world from reader thread" );
                    String input = fromNode.readLine();
                    log.debug( "Incoming from {}: {}", source, input );
                    if ( input == null ) {
                        log.info( "Terminating Reader thread for {}", source );
                        client.reactToLeaveSoon( source );
                        return;
                    }
                    JSONObject command = new JSONObject( input );
                    remotes.get( source ).onMessage( command );
                }
            } catch( IOException e ) {
                log.info( "Couldn't receive message - connection to {} lost: {}", source, e.getMessage() );
                client.reactToLeaveSoon( source );
            }
        }
    }

}
