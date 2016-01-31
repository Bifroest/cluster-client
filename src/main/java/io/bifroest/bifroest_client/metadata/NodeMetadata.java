package io.bifroest.bifroest_client.metadata;

import java.util.Comparator;
import java.util.Objects;
import java.util.UUID;

import org.json.JSONObject;

import io.bifroest.commons.serialize.json.JSONSerializable;

public final class NodeMetadata implements JSONSerializable {
    // Yes I want silly names. We will go mad if we debug this with UUIDs.
    private final String funnyNameForDebugging;
    private final UUID nodeIdentifier;

    private final String address;
    private final PortMap ports;
    
    // Never touch these arrays again.
    // I'm relying that the uuid -> funny name mapping is constant
    private static final String[] adjectives = new String[] {
      "Alien", "Barbaric", "Confusing", "Dazzling", " Energetic", "Fabulous",
      "Green", "Humble", "Interesting", "Jolly", "Krunchy", "Lousy", "Man-eating",
      "Nervous", "Operational", "Protesting", "Quirky", "Ridiculous",
      "Speedy", "Tumbling", "Uniform", "Venomous", "Wacky", "Xenophobic",
      "Yawning", "Zappy"
    };

    // Never touch these arrays again.
    // I'm relying that the uuid -> funny name mapping is constant
    private static final String[] things = new String[] {
      "Anteater", "Bubble", "Cluster", "Dolphin", "Enigma", "Frisbee",
      "Guppy", "Hellhound", "Inuit", "Junker", "Koala", "Mouse", "Norbert",
      "Ozelot", "Penguin", "Quokka", "Rhino", "Shark", "T-Rex", "Unicorn",
      "Veteran", "Wallaby", "Xylophon", "Yak", "Zebra"
    };

    public static final Comparator<NodeMetadata> leaderOrder = Comparator.comparing( NodeMetadata::getAddress ).thenComparing( NodeMetadata::getPorts );

    private NodeMetadata( String funnyNameForDebugging, UUID nodeIdentifier, String address, PortMap ports ) {
        this.funnyNameForDebugging = funnyNameForDebugging;
        this.nodeIdentifier = nodeIdentifier;
        this.address = address;
        this.ports = ports;
    }

    public UUID getNodeIdentifier() {
        return nodeIdentifier;
    }

    public String getAddress() {
        return address;
    }

    public PortMap getPorts() {
        return ports;
    }

    public String getFunnyNameForDebugging() {
        return funnyNameForDebugging;
    }

    public static NodeMetadata fromValues( UUID nodeIdentifier, String address, PortMap ports ) {
        return new NodeMetadata( generateFunnyName( nodeIdentifier ), nodeIdentifier,
                                 address, ports );
    }

    public static NodeMetadata fromAddress( String address, PortMap ports ) {
        UUID myUUID = UUID.randomUUID();
        
        return NodeMetadata.fromValues( myUUID, address, ports );
    }

    public static NodeMetadata forTest() {
        return forTest( null, 0 );
    }

    public static NodeMetadata forTest( String address, int port ) {
        UUID nodeId = UUID.randomUUID();
        return new NodeMetadata( generateFunnyName( nodeId ), nodeId, address, DeserializedPortMap.forTest( port ) );
    }

    private static String generateFunnyName( UUID uuid ) {
        // not lossy, {adjectives,things}.length are far less than Integer.MAX_VALUE
        int hash = uuid.hashCode();
        int adjectiveIndex = (hash % adjectives.length);
        if ( adjectiveIndex < 0 ) {
            adjectiveIndex += adjectives.length;
        }
        int thingIndex = ((hash / adjectives.length) % things.length);
        if ( thingIndex < 0 ) {
            thingIndex += things.length;
        }
        
        return adjectives[adjectiveIndex] + things[thingIndex];
    }
    
    @Override
    public int hashCode() {
        int hash = 3;
        hash = 73 * hash + Objects.hashCode( this.nodeIdentifier );
        hash = 73 * hash + Objects.hashCode( this.address );
        hash = 73 * hash + Objects.hashCode( this.ports );
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
        final NodeMetadata other = (NodeMetadata) obj;
        if ( !Objects.equals( this.nodeIdentifier, other.nodeIdentifier ) ) {
            return false;
        }
        if ( !Objects.equals( this.address, other.address ) ) {
            return false;
        }
        if ( !Objects.equals( this.ports, other.ports ) ) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "NodeMetadata ["
                + "funnyNameForDebugging=" + funnyNameForDebugging + ", "
                + "nodeIdentifier=" + nodeIdentifier + ", "
                + "address=" + address + ", "
                + "ports=" + ports + "]";
    }

    @Override
    public JSONObject toJSON() {
        return new JSONObject()
                   .put( "uuid", getNodeIdentifier().toString() )
                   .put( "host", getAddress() )
                   .put( "ports", this.ports.toJSON() );
    }

    public static NodeMetadata fromJSON( JSONObject json ) {
        return NodeMetadata.fromValues( UUID.fromString( json.getString( "uuid" )),
                                        json.getString( "host" ),
                                        DeserializedPortMap.fromJSON( json.getJSONObject( "ports" ) ) );
    }
}
