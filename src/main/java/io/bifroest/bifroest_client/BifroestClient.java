package io.bifroest.bifroest_client;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;

import io.bifroest.bifroest_client.BasicClient.MetricState;
import io.bifroest.commons.model.Interval;
import io.bifroest.commons.model.Metric;
import io.bifroest.retentions.MetricSet;


public interface BifroestClient {
    MetricSet getMetrics( String metricName, Interval interval );
    void sendMetrics( Collection<Metric> metrics ) throws IOException;
    Map<String, MetricState> getSubMetrics( String query );
    void shutdown();
}
