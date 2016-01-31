package io.bifroest.bifroest_client.metadata;

import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.bifroest.balancing.BucketMapping;

public class ClusterStateWithVersionedMapping {
    private static final Logger log = LogManager.getLogger();

    private final ClusterState innerState;
    private final Duration maxNewMappingWaitTime;

    private final ReentrantLock mappingLock = new ReentrantLock();
    private final Condition newMappingReceived = mappingLock.newCondition();

    private MappingWithVersion currentVersionedMapping;
    
    public ClusterStateWithVersionedMapping( ClusterState existingState, Duration maxNewMappingWaitTime ) {
        this.innerState = existingState;
        this.maxNewMappingWaitTime = maxNewMappingWaitTime;
    }

    public synchronized void setBucketMapping( BucketMapping<NodeMetadata> newMapping ) {
        mappingLock.lock();
        try {
            currentVersionedMapping = new MappingWithVersion( Instant.now(), newMapping );
            newMappingReceived.signalAll();
        } finally {
            mappingLock.unlock();
        }
    }
    
    public MappingWithVersion getMappingWithVersion() {
        mappingLock.lock(); // I think this lock is necessary due to visibility of updates of currentVErsionedMapping
        try {
            return currentVersionedMapping;
        } finally {
            mappingLock.unlock();
        }
    }

    public void waitForMappingNewerThan( MappingWithVersion mappingTheClientHas ) {
        mappingLock.lock();
        try {
            if ( ! currentVersionedMapping.isNewerThan( mappingTheClientHas ) ) {
                newMappingReceived.await( maxNewMappingWaitTime.toMillis(), TimeUnit.MILLISECONDS );
            }
        } catch (InterruptedException e) {
            log.warn( "Interrupted while waiting on condition", e );
        } finally {
            mappingLock.unlock();
        }
    }

    public void addAll( Collection<NodeMetadata> initialNodes ) {
        innerState.addAll( initialNodes );
    }

    public void addNode( NodeMetadata joinedNodeMetadata ) {
        innerState.addNode( joinedNodeMetadata );
    }

    public void removeNode( NodeMetadata leavingNodeMetadata ) {
        innerState.removeNode( leavingNodeMetadata );
    }

    public Iterable<NodeMetadata> getKnownNodes() {
        return innerState.getKnownNodes();
    }

    public static final class MappingWithVersion {
        private final Instant whenWasThisMappingReceived;
        private final BucketMapping<NodeMetadata> mappingAtThatTime;

        public MappingWithVersion( Instant whenWasThisMappingReceived, BucketMapping<NodeMetadata> mappingAtThatTime ) {
            this.whenWasThisMappingReceived = whenWasThisMappingReceived;
            this.mappingAtThatTime = mappingAtThatTime;
        }

        public BucketMapping<NodeMetadata> getMapping() {
            return mappingAtThatTime;
        }
        
        private boolean isNewerThan( MappingWithVersion otherMapping ) {
            return whenWasThisMappingReceived.compareTo( otherMapping.whenWasThisMappingReceived ) > 0;
        }
    }
}
