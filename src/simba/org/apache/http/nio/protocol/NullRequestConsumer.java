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

package simba.org.apache.http.nio.protocol;

import java.io.IOException;
import java.nio.ByteBuffer;

import simba.org.apache.http.HttpRequest;
import simba.org.apache.http.nio.ContentDecoder;
import simba.org.apache.http.nio.IOControl;
import simba.org.apache.http.protocol.HttpContext;

class NullRequestConsumer implements HttpAsyncRequestConsumer<Object> {

    private final ByteBuffer buffer;
    private volatile boolean completed;

    NullRequestConsumer() {
        super();
        this.buffer = ByteBuffer.allocate(2048);
    }

    public void requestReceived(final HttpRequest request) {
    }

    public void consumeContent(
            final ContentDecoder decoder, final IOControl ioctrl) throws IOException {
        int lastRead;
        do {
            this.buffer.clear();
            lastRead = decoder.read(this.buffer);
        } while (lastRead > 0);
    }

    public void requestCompleted(final HttpContext context) {
        this.completed = true;
    }

    public void failed(final Exception ex) {
        this.completed = true;
    }

    public Object getResult() {
        return this.completed;
    }

    public Exception getException() {
        return null;
    }

    public void close() throws IOException {
        this.completed = true;
    }

    public boolean isDone() {
        return this.completed;
    }

}
