package io.bifroest.bifroest_client;

import io.bifroest.commons.boot.interfaces.Environment;

public interface EnvironmentWithBifroestClient extends Environment {
    BifroestClient bifroestClient();
}
