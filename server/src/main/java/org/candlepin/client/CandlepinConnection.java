/**
 * Copyright (c) 2009 - 2012 Red Hat, Inc.
 *
 * This software is licensed to you under the GNU General Public License,
 * version 2 (GPLv2). There is NO WARRANTY for this software, express or
 * implied, including the implied warranties of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. You should have received a copy of GPLv2
 * along with this software; if not, see
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.txt.
 *
 * Red Hat trademarks are not licensed under GPLv2. No permission is
 * granted to use or replicate Red Hat trademarks that are incorporated
 * in this software or its documentation.
 */
package org.candlepin.client;

import org.candlepin.common.config.Configuration;
import org.candlepin.resteasy.JsonProvider;

import com.google.inject.Inject;

import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.impl.client.DefaultHttpClient;
import org.jboss.resteasy.client.ClientExecutor;
import org.jboss.resteasy.client.ProxyFactory;
import org.jboss.resteasy.client.core.executors.ApacheHttpClient4Executor;
import org.jboss.resteasy.plugins.providers.RegisterBuiltin;
import org.jboss.resteasy.spi.ResteasyProviderFactory;

/**
 * CandlepinConnection sets up the remote connection to another Candlepin
 * instance. It takes in the stand Config object to configure the JsonProvider
 * then takes in some Credentials to login via the connect method.
 */
public class CandlepinConnection {

    @Inject
    public CandlepinConnection(Configuration config) {
        ResteasyProviderFactory rpf = ResteasyProviderFactory.getInstance();
        JsonProvider jsonprovider = new JsonProvider(config);
        rpf.registerProviderInstance(jsonprovider);
        RegisterBuiltin.register(rpf);
    }

    /**
     * Connects to another Candlepin instance located at the given uri.
     * @param clazz the client class to create.
     * @param creds authentication credentials for the given uri.
     * @param uri the Candlepin instance to connect to
     * @return Client proxy used to interact with Candlepin via REST API.
     */
    public <T> T connect(Class<T> clazz, Credentials creds, String uri) {
        DefaultHttpClient httpclient = new DefaultHttpClient();
        httpclient.getCredentialsProvider().setCredentials(AuthScope.ANY, creds);
        ClientExecutor clientExecutor = new ApacheHttpClient4Executor(httpclient);
        return ProxyFactory.create(clazz, uri,
            clientExecutor);
    }
}
