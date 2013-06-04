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

package simba.org.apache.http.impl.nio.codecs;

import simba.org.apache.http.HttpException;
import simba.org.apache.http.HttpRequest;
import simba.org.apache.http.HttpRequestFactory;
import simba.org.apache.http.ParseException;
import simba.org.apache.http.RequestLine;
import simba.org.apache.http.annotation.NotThreadSafe;
import simba.org.apache.http.message.LineParser;
import simba.org.apache.http.message.ParserCursor;
import simba.org.apache.http.nio.NHttpMessageParser;
import simba.org.apache.http.nio.reactor.SessionInputBuffer;
import simba.org.apache.http.params.HttpParams;
import simba.org.apache.http.util.CharArrayBuffer;

/**
 * Default {@link NHttpMessageParser} implementation for {@link HttpRequest}s.
 * <p>
 * The following parameters can be used to customize the behavior of this
 * class:
 * <ul>
 *  <li>{@link simba.org.apache.http.params.CoreConnectionPNames#MAX_HEADER_COUNT}</li>
 *  <li>{@link simba.org.apache.http.params.CoreConnectionPNames#MAX_LINE_LENGTH}</li>
 * </ul>
 *
 * @since 4.1
 */
@NotThreadSafe
public class DefaultHttpRequestParser extends AbstractMessageParser<HttpRequest> {

    private final HttpRequestFactory requestFactory;

    public DefaultHttpRequestParser(
            final SessionInputBuffer buffer,
            final LineParser parser,
            final HttpRequestFactory requestFactory,
            final HttpParams params) {
        super(buffer, parser, params);
        if (requestFactory == null) {
            throw new IllegalArgumentException("Request factory may not be null");
        }
        this.requestFactory = requestFactory;
    }

    @Override
    protected HttpRequest createMessage(final CharArrayBuffer buffer)
            throws HttpException, ParseException {
        ParserCursor cursor = new ParserCursor(0, buffer.length());
        RequestLine requestLine = lineParser.parseRequestLine(buffer, cursor);
        return this.requestFactory.newHttpRequest(requestLine);
    }

}
