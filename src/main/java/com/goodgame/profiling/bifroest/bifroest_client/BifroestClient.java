package com.goodgame.profiling.bifroest.bifroest_client;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;

import com.goodgame.profiling.bifroest.bifroest_client.BasicClient.MetricState;
import com.goodgame.profiling.commons.model.Interval;
import com.goodgame.profiling.commons.model.Metric;
import com.goodgame.profiling.graphite_retentions.MetricSet;


public interface BifroestClient {
    MetricSet getMetrics( String metricName, Interval interval );
    void sendMetrics( Collection<Metric> metrics ) throws IOException;
    Map<String, MetricState> getSubMetrics( String query );
    void shutdown();
}
