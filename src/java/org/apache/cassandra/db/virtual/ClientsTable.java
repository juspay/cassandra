/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.cassandra.db.virtual;

import java.net.InetSocketAddress;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.apache.cassandra.db.marshal.BooleanType;
import org.apache.cassandra.db.marshal.InetAddressType;
import org.apache.cassandra.db.marshal.Int32Type;
import org.apache.cassandra.db.marshal.LongType;
import org.apache.cassandra.db.marshal.MapType;
import org.apache.cassandra.db.marshal.UTF8Type;
import org.apache.cassandra.dht.LocalPartitioner;
import org.apache.cassandra.metrics.ClientMetrics;
import org.apache.cassandra.schema.TableMetadata;
import org.apache.cassandra.transport.ConnectedClient;

import static org.apache.cassandra.utils.LocalizeString.toLowerCaseLocalized;

final class ClientsTable extends AbstractVirtualTable
{
    private static final String ADDRESS = "address";
    private static final String PORT = "port";
    private static final String HOSTNAME = "hostname";
    private static final String USERNAME = "username";
    private static final String CONNECTION_STAGE = "connection_stage";
    private static final String PROTOCOL_VERSION = "protocol_version";
    private static final String CLIENT_OPTIONS = "client_options";
    private static final String DRIVER_NAME = "driver_name";
    private static final String DRIVER_VERSION = "driver_version";
    private static final String REQUEST_COUNT = "request_count";
    private static final String SSL_ENABLED = "ssl_enabled";
    private static final String SSL_PROTOCOL = "ssl_protocol";
    private static final String SSL_CIPHER_SUITE = "ssl_cipher_suite";
    private static final String KEYSPACE_NAME = "keyspace_name";
    private static final String AUTHENTICATION_MODE = "authentication_mode";
    private static final String AUTHENTICATION_METADATA = "authentication_metadata";

    ClientsTable(String keyspace)
    {
        super(TableMetadata.builder(keyspace, "clients")
                           .comment("currently connected clients")
                           .kind(TableMetadata.Kind.VIRTUAL)
                           .partitioner(new LocalPartitioner(InetAddressType.instance))
                           .addPartitionKeyColumn(ADDRESS, InetAddressType.instance)
                           .addClusteringColumn(PORT, Int32Type.instance)
                           .addRegularColumn(HOSTNAME, UTF8Type.instance)
                           .addRegularColumn(USERNAME, UTF8Type.instance)
                           .addRegularColumn(CONNECTION_STAGE, UTF8Type.instance)
                           .addRegularColumn(PROTOCOL_VERSION, Int32Type.instance)
                           .addRegularColumn(CLIENT_OPTIONS, MapType.getInstance(UTF8Type.instance, UTF8Type.instance, false))
                           .addRegularColumn(DRIVER_NAME, UTF8Type.instance)
                           .addRegularColumn(DRIVER_VERSION, UTF8Type.instance)
                           .addRegularColumn(REQUEST_COUNT, LongType.instance)
                           .addRegularColumn(SSL_ENABLED, BooleanType.instance)
                           .addRegularColumn(SSL_PROTOCOL, UTF8Type.instance)
                           .addRegularColumn(SSL_CIPHER_SUITE, UTF8Type.instance)
                           .addRegularColumn(KEYSPACE_NAME, UTF8Type.instance)
                           .addRegularColumn(AUTHENTICATION_MODE, UTF8Type.instance)
                           .addRegularColumn(AUTHENTICATION_METADATA, MapType.getInstance(UTF8Type.instance, UTF8Type.instance, false))
                           .build());
    }

    @Override
    public DataSet data()
    {
        SimpleDataSet result = new SimpleDataSet(metadata());

        for (ConnectedClient client : ClientMetrics.instance.allConnectedClients())
        {
            InetSocketAddress remoteAddress = client.remoteAddress();

            result.row(remoteAddress.getAddress(), remoteAddress.getPort())
                  .column(HOSTNAME, remoteAddress.getHostName())
                  .column(USERNAME, client.username().orElse(null))
                  .column(CONNECTION_STAGE, toLowerCaseLocalized(client.stage().toString()))
                  .column(PROTOCOL_VERSION, client.protocolVersion())
                  .column(CLIENT_OPTIONS, client.clientOptions().orElse(null))
                  .column(DRIVER_NAME, client.driverName().orElse(null))
                  .column(DRIVER_VERSION, client.driverVersion().orElse(null))
                  .column(REQUEST_COUNT, client.requestCount())
                  .column(SSL_ENABLED, client.sslEnabled())
                  .column(SSL_PROTOCOL, client.sslProtocol().orElse(null))
                  .column(SSL_CIPHER_SUITE, client.sslCipherSuite().orElse(null))
                  .column(KEYSPACE_NAME, client.keyspace().orElse(null))
                  .column(AUTHENTICATION_MODE, client.authenticationMode().toString())
                  .column(AUTHENTICATION_METADATA, client.authenticationMetadata()
                                                         .entrySet().stream()
                                                         .collect(Collectors.toMap(Entry::getKey, entry -> String.valueOf(entry.getValue()))));
        }

        return result;
    }
}
