package org.apache.cassandra.locator;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.net.http.HttpClient;
import java.net.http.HttpResponse.BodyHandlers;
import java.net.http.HttpRequest;
import java.util.function.Supplier;
import java.net.URI;
import org.apache.cassandra.config.Config;
import org.apache.cassandra.config.DatabaseDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fasterxml.jackson.annotation.JsonProperty;

// class JsonBodyHandler<W> implements HttpResponse.BodyHandler<Supplier<W>> {

//     private final Class<W> wClass;

//     public JsonBodyHandler(Class<W> wClass) {
//         this.wClass = wClass;
//     }

//     public static <W> HttpResponse.BodySubscriber<Supplier<W>> asJSON(Class<W> targetType) {
//         HttpResponse.BodySubscriber<InputStream> upstream = HttpResponse.BodySubscribers.ofInputStream();

//         return HttpResponse.BodySubscribers.mapping(
//                 upstream,
//                 inputStream -> toSupplierOfType(inputStream, targetType));
//     }

//     public static <W> Supplier<W> toSupplierOfType(InputStream inputStream, Class<W> targetType) {
//         return () -> {
//             try (InputStream stream = inputStream) {
//                 ObjectMapper objectMapper = new ObjectMapper();
//                 return objectMapper.readValue(stream, targetType);
//             } catch (IOException e) {
//                 throw new UncheckedIOException(e);
//             }
//         };
//     }


//     @Override
//     public HttpResponse.BodySubscriber<Supplier<W>> apply(HttpResponse.ResponseInfo responseInfo) {
//         return asJSON(wClass);
//     }

// }


// class APOD {
//     public final String copyright;
//     public final String date;
//     public final String explanation;
//     public final String hdUrl;
//     public final String mediaType;
//     public final String serviceVersion;
//     public final String title;
//     public final String url;

//     public APOD(@JsonProperty("copyright") String copyright,
//                 @JsonProperty("date") String date,
//                 @JsonProperty("explanation") String explanation,
//                 @JsonProperty("hdurl") String hdUrl,
//                 @JsonProperty("media_type") String mediaType,
//                 @JsonProperty("service_version") String serviceVersion,
//                 @JsonProperty("title") String title,
//                 @JsonProperty("url") String url) {
//         this.copyright = copyright;
//         this.date = date;
//         this.explanation = explanation;
//         this.hdUrl = hdUrl;
//         this.mediaType = mediaType;
//         this.serviceVersion = serviceVersion;
//         this.title = title;
//         this.url = url;
//     }
// }

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
            System.out.println(response.body());

            String[] hosts = response.body().replace("\"", "").split(",", -1);
            seeds = new ArrayList<>(hosts.length);
            for (String host : hosts)
            {
                String hostPortString = host.trim();
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