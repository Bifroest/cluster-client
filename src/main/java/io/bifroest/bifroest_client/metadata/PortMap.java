package com.goodgame.profiling.bifroest.bifroest_client.metadata;

import java.util.Comparator;

import org.json.JSONObject;

import com.goodgame.profiling.commons.serialize.json.JSONSerializable;

public interface PortMap extends Comparable<PortMap>, JSONSerializable {
    int getClusterPort();
    int getIncludeMetricPort();
    int getFastIncludeMetricPort();
    int getMetricPort();
    int getMetricSetPort();
    int getSubmetricPort();

    @Override
    default int compareTo( PortMap otherMap ) {
        return Comparator.comparingInt( PortMap::getClusterPort )
                         .thenComparingInt( PortMap::getIncludeMetricPort )
                         .thenComparingInt( PortMap::getFastIncludeMetricPort )
                         .thenComparingInt( PortMap::getMetricPort )
                         .thenComparingInt( PortMap::getMetricSetPort )
                         .thenComparingInt( PortMap::getSubmetricPort )
                         .compare( this, otherMap );
    }

    default boolean equals_( Object obj ) {
        if ( obj == null ) {
            return false;
        }
        if ( obj == this ) {
            return true;
        }
        if ( !( obj instanceof PortMap ) ) {
            return false;
        }
        final PortMap other = (PortMap) obj;

        if ( this.getClusterPort() != other.getClusterPort() ) {
            return false;
        }
        if ( this.getIncludeMetricPort() != other.getIncludeMetricPort() ) {
            return false;
        }
        if ( this.getFastIncludeMetricPort() != other.getFastIncludeMetricPort() ) {
            return false;
        }
        if ( this.getMetricPort() != other.getMetricPort() ) {
            return false;
        }
        if ( this.getMetricSetPort() != other.getMetricSetPort() ) {
            return false;
        }
        if ( this.getSubmetricPort() != other.getSubmetricPort() ) {
            return false;
        }
        
        return true;
    }

    default int hashCode_() {
        int hash = 3;
        hash = 73 * hash + this.getClusterPort();
        hash = 73 * hash + this.getIncludeMetricPort();
        hash = 73 * hash + this.getFastIncludeMetricPort();
        hash = 73 * hash + this.getMetricPort();
        hash = 73 * hash + this.getMetricSetPort();
        hash = 73 * hash + this.getSubmetricPort();
        return hash;
    }

    @Override
    default JSONObject toJSON() {
        return new JSONObject().put( "cluster", getClusterPort() )
                               .put( "include-metric", getIncludeMetricPort() )
                               .put( "fast-include-metric", getFastIncludeMetricPort() )
                               .put( "get-metric", getMetricPort() )
                               .put( "get-metric-set", getMetricSetPort() )
                               .put( "get-sub-metric", getSubmetricPort() );
    }
}
