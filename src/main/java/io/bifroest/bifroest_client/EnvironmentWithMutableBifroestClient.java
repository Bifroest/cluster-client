package io.bifroest.bifroest_client;

public interface EnvironmentWithMutableBifroestClient extends EnvironmentWithBifroestClient {
    void setBifroestClient( BifroestClient client );
}
