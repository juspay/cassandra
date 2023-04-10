package org.apache.cassandra.locator;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.net.http.HttpClient;
import java.net.http.HttpResponse.BodyHandlers;
import java.net.http.HttpRequest;
import java.net.URI;
import org.apache.cassandra.config.Config;
import org.apache.cassandra.config.DatabaseDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HttpSeedProvider implements SeedProvider
{
    private static final Logger logger = LoggerFactory.getLogger(HttpSeedProvider.class);
    public HttpClient client = HttpClient.newHttpClient();

    public HttpSeedProvider(Map<String, String> args) {}

    public List<InetAddressAndPort> getSeeds()
    {
        Config conf;
        List<InetAddressAndPort> seeds = new ArrayList<>(0);
        try
        {
            conf = DatabaseDescriptor.loadConfig();
            var request = HttpRequest.newBuilder(
            URI.create(conf.seed_provider.parameters.get("seedsUrl")))
            .header("accept", "application/json")
            .build();
            var response = client.send(request, BodyHandlers.ofString());
            String[] hosts = response.body().replace("\"", "").split(",", -1);
            seeds = new ArrayList<>(hosts.length);
            for (String host : hosts)
            {
                String hostPortString = host.trim();
                if (hostPortString.split(":").length != 2){
                    hostPortString = hostPortString + ":" + conf.seed_provider.parameters.get("defaultPort");
                }
                try
                {
                    if(!hostPortString.isEmpty()) {
                        seeds.add(InetAddressAndPort.getByName(hostPortString));
                    }
                }
                catch (UnknownHostException ex)
                {
                logger.warn("Seed provider couldn't lookup host {}", host);
                }
            }
        }
        catch (Exception e)
        {
            throw new AssertionError(e);
        }
        return Collections.unmodifiableList(seeds);
    }
}
