package io.bifroest.bifroest_client;

import com.goodgame.profiling.commons.boot.interfaces.Environment;

public interface EnvironmentWithBifroestClient extends Environment {
    BifroestClient bifroestClient();
}
