/*
* Copyright (c) 2014 Spotify AB.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing,
* software distributed under the License is distributed on an
* "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
* KIND, either express or implied. See the License for the
* specific language governing permissions and limitations
* under the License.
*/
package org.jenkinsci.plugins.dockerbuildstep.log.container;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.io.ByteStreams.copy;
import static com.google.common.io.ByteStreams.nullOutputStream;

import java.io.Closeable;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;

import com.google.common.io.ByteStreams;

/**
 * Stream support should be rolled into docker-java, see https://github.com/docker-java/docker-java/issues/42.
 * 
 * As a workaround we borrowed the decoding magic from https://github.com/spotify/docker-client.
 * 
 * @see https://github.com/spotify/docker-client/blob/master/src/main/java/com/spotify/docker/client/LogReader.java
 */
public class DockerLogStreamReader implements Closeable {
	private final InputStream stream;
	public static final int HEADER_SIZE = 8;
	public static final int FRAME_SIZE_OFFSET = 4;

	private volatile boolean closed;

	public DockerLogStreamReader(InputStream is) {
		this.stream = is;
	}

	public DockerLogMessage nextMessage() throws IOException {
		// Read header
		final byte[] headerBytes = new byte[HEADER_SIZE];
		final int n = ByteStreams.read(stream, headerBytes, 0, HEADER_SIZE);
		if (n == 0) {
			return null;
		}
		if (n != HEADER_SIZE) {
			throw new EOFException();
		}
		final ByteBuffer header = ByteBuffer.wrap(headerBytes);
		final int streamId = header.get();
		header.position(FRAME_SIZE_OFFSET);
		final int frameSize = header.getInt();
		// Read frame
		final byte[] frame = new byte[frameSize];
		ByteStreams.readFully(stream, frame);
		return new DockerLogMessage(streamId, ByteBuffer.wrap(frame));
	}

	@Override
	protected void finalize() throws Throwable {
		super.finalize();
		if (!closed) {
			close();
		}
	}

	public void close() throws IOException {
		closed = true;
		// Jersey will close the stream and release the connection after we read
		// all the data.
		// We cannot call the stream's close method because it an instance of
		// UncloseableInputStream,
		// where close is a no-op.
		copy(stream, new OutputStream() {
			/** Discards the specified byte. */
			@Override
			public void write(int b) {
			}

			/** Discards the specified byte array. */
			@Override
			public void write(byte[] b) {
				checkNotNull(b);
			}

			/** Discards the specified byte array. */
			@Override
			public void write(byte[] b, int off, int len) {
				checkNotNull(b);
			}

			@Override
			public String toString() {
				return "ByteStreams.nullOutputStream()";
			};
		});
	}

}
