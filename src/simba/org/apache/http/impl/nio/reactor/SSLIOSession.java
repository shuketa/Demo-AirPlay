/*
 * ====================================================================
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 *
 */

package simba.org.apache.http.impl.nio.reactor;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;

import simba.org.apache.http.nio.reactor.IOSession;
import simba.org.apache.http.params.HttpParams;

/**
 * A decorator class intended to transparently extend an {@link IOSession}
 * with transport layer security capabilities based on the SSL/TLS protocol.
 *
 * @since 4.0
 *
 * @deprecated (4.2) use {@link simba.org.apache.http.nio.reactor.ssl.SSLIOSession}
 */
@Deprecated
public class SSLIOSession extends simba.org.apache.http.nio.reactor.ssl.SSLIOSession {

    /**
     * @since 4.1
     */
    public SSLIOSession(
            final IOSession session,
            final SSLContext sslContext,
            final SSLSetupHandler handler) {
        super(session, simba.org.apache.http.nio.reactor.ssl.SSLMode.CLIENT,
                sslContext, handler != null ? new SSLSetupHandlerAdaptor(handler) : null);
    }

    public SSLIOSession(
            final IOSession session,
            final SSLContext sslContext,
            final SSLIOSessionHandler handler) {
        super(session, simba.org.apache.http.nio.reactor.ssl.SSLMode.CLIENT,
                sslContext, handler != null ? new SSLIOSessionHandlerAdaptor(handler) : null);
    }

    public synchronized void bind(
            final SSLMode mode,
            final HttpParams params) throws SSLException {
        simba.org.apache.http.nio.reactor.ssl.SSLSetupHandler handler = getSSLSetupHandler();
        if (handler instanceof SSLIOSessionHandlerAdaptor) {
            ((SSLIOSessionHandlerAdaptor) handler).setParams(params);
        } else if (handler instanceof SSLSetupHandlerAdaptor) {
            ((SSLSetupHandlerAdaptor) handler).setParams(params);
        }
        initialize(convert(mode));
    }

    private simba.org.apache.http.nio.reactor.ssl.SSLMode convert(final SSLMode mode) {
        switch(mode) {
        case CLIENT:
            return simba.org.apache.http.nio.reactor.ssl.SSLMode.CLIENT;
        case SERVER:
            return simba.org.apache.http.nio.reactor.ssl.SSLMode.SERVER;
        }
        return null;
    }

}
