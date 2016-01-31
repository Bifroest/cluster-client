package io.bifroest.bifroest_client.metadata;

import org.json.JSONObject;

public final class DeserializedPortMap implements PortMap {
    // all of these are quasi-final. However, I want to avoid a
    // constructor with 5 int params.
    private int clusterPort;
    private int includeMetricPort;
    private int fastIncludeMetricPort;
    private int getMetricPort;
    private int getMetricSetPort;
    private int getSubmetricPort;

    private DeserializedPortMap() {
    }
 
    @Override
    public int getClusterPort() {
        return clusterPort;
    }

    @Override
    public int getIncludeMetricPort() {
        return includeMetricPort;
    }

    @Override
    public int getFastIncludeMetricPort() {
        return fastIncludeMetricPort;
    }

    @Override
    public int getMetricPort() {
        return getMetricPort;
    }

    @Override
    public int getMetricSetPort() {
        return getMetricSetPort;
    }

    @Override
    public int getSubmetricPort() {
        return getSubmetricPort;
    }

    @Override
    public boolean equals( Object other ) {
        return this.equals_( other );
    }

    @Override
    public int hashCode() {
        return this.hashCode_();
    }

    @Override
    public String toString() {
        return "DeserializedPortMap{" + "clusterPort=" + clusterPort + ", includeMetricPort=" + includeMetricPort + ", fastIncludeMetricPort=" + fastIncludeMetricPort + ", getMetricPort=" + getMetricPort + ", getMetricSetPort=" + getMetricSetPort + ", getSubmetricPort=" + getSubmetricPort + '}';
    }

    public static final PortMap fromJSON( JSONObject json ) {
        DeserializedPortMap result = new DeserializedPortMap();
        result.clusterPort = json.getInt( "cluster" );
        result.includeMetricPort = json.getInt( "include-metric" );
        result.fastIncludeMetricPort = json.getInt( "fast-include-metric" );
        result.getMetricPort = json.getInt( "get-metric" );
        result.getMetricSetPort = json.getInt( "get-metric-set" );
        result.getSubmetricPort = json.getInt( "get-sub-metric" );
        return result;
    }

    public static final PortMap forTest( int port ) {
        DeserializedPortMap result = new DeserializedPortMap();
        result.clusterPort = port;
        result.includeMetricPort = port;
        result.fastIncludeMetricPort = port;
        result.getMetricPort = port;
        result.getMetricSetPort = port;
        result.getSubmetricPort = port;
        return result;
    }
}
