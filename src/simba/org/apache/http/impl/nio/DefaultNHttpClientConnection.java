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

package simba.org.apache.http.impl.nio;

import java.io.IOException;
import java.nio.channels.SelectionKey;

import simba.org.apache.http.HttpEntity;
import simba.org.apache.http.HttpEntityEnclosingRequest;
import simba.org.apache.http.HttpException;
import simba.org.apache.http.HttpRequest;
import simba.org.apache.http.HttpResponse;
import simba.org.apache.http.HttpResponseFactory;
import simba.org.apache.http.annotation.NotThreadSafe;
import simba.org.apache.http.impl.nio.codecs.DefaultHttpRequestWriter;
import simba.org.apache.http.impl.nio.codecs.DefaultHttpResponseParser;
import simba.org.apache.http.nio.NHttpClientConnection;
import simba.org.apache.http.nio.NHttpClientEventHandler;
import simba.org.apache.http.nio.NHttpClientHandler;
import simba.org.apache.http.nio.NHttpClientIOTarget;
import simba.org.apache.http.nio.NHttpMessageParser;
import simba.org.apache.http.nio.NHttpMessageWriter;
import simba.org.apache.http.nio.reactor.EventMask;
import simba.org.apache.http.nio.reactor.IOSession;
import simba.org.apache.http.nio.reactor.SessionInputBuffer;
import simba.org.apache.http.nio.reactor.SessionOutputBuffer;
import simba.org.apache.http.nio.util.ByteBufferAllocator;
import simba.org.apache.http.params.HttpParams;

/**
 * Default implementation of the {@link NHttpClientConnection} interface.
 * <p>
 * The following parameters can be used to customize the behavior of this
 * class:
 * <ul>
 *  <li>{@link simba.org.apache.http.params.CoreProtocolPNames#HTTP_ELEMENT_CHARSET}</li>
 *  <li>{@link simba.org.apache.http.params.CoreConnectionPNames#SOCKET_BUFFER_SIZE}</li>
 *  <li>{@link simba.org.apache.http.params.CoreConnectionPNames#MAX_HEADER_COUNT}</li>
 *  <li>{@link simba.org.apache.http.params.CoreConnectionPNames#MAX_LINE_LENGTH}</li>
 * </ul>
 *
 * @since 4.0
 */
@SuppressWarnings("deprecation")
@NotThreadSafe
public class DefaultNHttpClientConnection
    extends NHttpConnectionBase implements NHttpClientIOTarget {

    protected final NHttpMessageParser<HttpResponse> responseParser;
    protected final NHttpMessageWriter<HttpRequest> requestWriter;

    /**
     * Creates a new instance of this class given the underlying I/O session.
     *
     * @param session the underlying I/O session.
     * @param responseFactory HTTP response factory.
     * @param allocator byte buffer allocator.
     * @param params HTTP parameters.
     */
    public DefaultNHttpClientConnection(
            final IOSession session,
            final HttpResponseFactory responseFactory,
            final ByteBufferAllocator allocator,
            final HttpParams params) {
        super(session, allocator, params);
        if (responseFactory == null) {
            throw new IllegalArgumentException("Response factory may not be null");
        }
        this.responseParser = createResponseParser(this.inbuf, responseFactory, params);
        this.requestWriter = createRequestWriter(this.outbuf, params);
        this.hasBufferedInput = false;
        this.hasBufferedOutput = false;
        this.session.setBufferStatus(this);
    }

    /**
     * Creates an instance of {@link NHttpMessageParser} to be used
     * by this connection for parsing incoming {@link HttpResponse} messages.
     * <p>
     * This method can be overridden in a super class in order to provide
     * a different implementation of the {@link NHttpMessageParser} interface.
     *
     * @return HTTP response parser.
     */
    protected NHttpMessageParser<HttpResponse> createResponseParser(
            final SessionInputBuffer buffer,
            final HttpResponseFactory responseFactory,
            final HttpParams params) {
        // override in derived class to specify a line parser
        return new DefaultHttpResponseParser(buffer, null, responseFactory, params);
    }

    /**
     * Creates an instance of {@link NHttpMessageWriter} to be used
     * by this connection for writing out outgoing {@link HttpRequest} messages.
     * <p>
     * This method can be overridden by a super class in order to provide
     * a different implementation of the {@link NHttpMessageWriter} interface.
     *
     * @return HTTP response parser.
     */
    protected NHttpMessageWriter<HttpRequest> createRequestWriter(
            final SessionOutputBuffer buffer,
            final HttpParams params) {
        // override in derived class to specify a line formatter
        return new DefaultHttpRequestWriter(buffer, null, params);
    }

    /**
     * @since 4.2
     */
    protected void onResponseReceived(final HttpResponse response) {
    }
    
    /**
     * @since 4.2
     */
    protected void onRequestSubmitted(final HttpRequest request) {
    }
    
    public void resetInput() {
        this.response = null;
        this.contentDecoder = null;
        this.responseParser.reset();
    }

    public void resetOutput() {
        this.request = null;
        this.contentEncoder = null;
        this.requestWriter.reset();
    }

    public void consumeInput(final NHttpClientEventHandler handler) {
        if (this.status != ACTIVE) {
            this.session.clearEvent(EventMask.READ);
            return;
        }
        try {
            if (this.response == null) {
                int bytesRead;
                do {
                    bytesRead = this.responseParser.fillBuffer(this.session.channel());
                    if (bytesRead > 0) {
                        this.inTransportMetrics.incrementBytesTransferred(bytesRead);
                    }
                    this.response = this.responseParser.parse();
                } while (bytesRead > 0 && this.response == null);
                if (this.response != null) {
                    if (this.response.getStatusLine().getStatusCode() >= 200) {
                        HttpEntity entity = prepareDecoder(this.response);
                        this.response.setEntity(entity);
                        this.connMetrics.incrementResponseCount();
                    }
                    onResponseReceived(this.response);
                    handler.responseReceived(this);
                    if (this.contentDecoder == null) {
                        resetInput();
                    }
                }
                if (bytesRead == -1) {
                    handler.endOfInput(this);
                }
            }
            if (this.contentDecoder != null && (this.session.getEventMask() & SelectionKey.OP_READ) > 0) {
                handler.inputReady(this, this.contentDecoder);
                if (this.contentDecoder.isCompleted()) {
                    // Response entity received
                    // Ready to receive a new response
                    resetInput();
                }
            }
        } catch (HttpException ex) {
            resetInput();
            handler.exception(this, ex);
        } catch (Exception ex) {
            handler.exception(this, ex);
        } finally {
            // Finally set buffered input flag
            this.hasBufferedInput = this.inbuf.hasData();
        }
    }

    public void produceOutput(final NHttpClientEventHandler handler) {
        try {
            if (this.outbuf.hasData()) {
                int bytesWritten = this.outbuf.flush(this.session.channel());
                if (bytesWritten > 0) {
                    this.outTransportMetrics.incrementBytesTransferred(bytesWritten);
                }
            }
            if (!this.outbuf.hasData()) {
                if (this.status == CLOSING) {
                    this.session.close();
                    this.status = CLOSED;
                    resetOutput();
                    return;
                } else {
                    if (this.contentEncoder != null) {
                        handler.outputReady(this, this.contentEncoder);
                        if (this.contentEncoder.isCompleted()) {
                            resetOutput();
                        }
                    }
                }

                if (this.contentEncoder == null && !this.outbuf.hasData()) {
                    if (this.status == CLOSING) {
                        this.session.close();
                        this.status = CLOSED;
                    }
                    if (this.status != CLOSED) {
                        this.session.clearEvent(EventMask.WRITE);
                        handler.requestReady(this);
                    }
                }
            }
        } catch (Exception ex) {
            handler.exception(this, ex);
        } finally {
            // Finally set buffered output flag
            this.hasBufferedOutput = this.outbuf.hasData();
        }
    }

    public void submitRequest(final HttpRequest request) throws IOException, HttpException {
        if (request == null) {
            throw new IllegalArgumentException("HTTP request may not be null");
        }
        assertNotClosed();
        if (this.request != null) {
            throw new HttpException("Request already submitted");
        }
        onRequestSubmitted(request);
        this.requestWriter.write(request);
        this.hasBufferedOutput = this.outbuf.hasData();

        if (request instanceof HttpEntityEnclosingRequest
                && ((HttpEntityEnclosingRequest) request).getEntity() != null) {
            prepareEncoder(request);
            this.request = request;
        }
        this.connMetrics.incrementRequestCount();
        this.session.setEvent(EventMask.WRITE);
    }

    public boolean isRequestSubmitted() {
        return this.request != null;
    }

    public void consumeInput(final NHttpClientHandler handler) {
        consumeInput(new NHttpClientEventHandlerAdaptor(handler));
    }

    public void produceOutput(final NHttpClientHandler handler) {
        produceOutput(new NHttpClientEventHandlerAdaptor(handler));
    }

}
