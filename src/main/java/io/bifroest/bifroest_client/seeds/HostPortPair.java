package io.bifroest.bifroest_client.seeds;

import java.util.Objects;

public final class HostPortPair {
    private final String host;
    private final int port;

    private HostPortPair( String host, int port ) {
        this.host = host;
        this.port = port;
    }

    public static HostPortPair of( String host, int port ) {
        return new HostPortPair( host, port );
    }

    public String host() {
        return host;
    }

    public int port() {
        return port;
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 43 * hash + Objects.hashCode( this.host );
        hash = 43 * hash + this.port;
        return hash;
    }

    @Override
    public boolean equals( Object obj ) {
        if ( obj == null ) {
            return false;
        }
        if ( getClass() != obj.getClass() ) {
            return false;
        }
        final HostPortPair other = (HostPortPair) obj;
        if ( !Objects.equals( this.host, other.host ) ) {
            return false;
        }
        if ( this.port != other.port ) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "HostPortPair{" + "host=" + host + ", port=" + port + '}';
    }
}
