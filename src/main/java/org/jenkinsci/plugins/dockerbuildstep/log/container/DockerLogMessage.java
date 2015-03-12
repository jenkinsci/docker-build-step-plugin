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

import java.nio.ByteBuffer;

/**
 * Stream support should be rolled into docker-java, see https://github.com/docker-java/docker-java/issues/42.
 * 
 * As a workaround we borrowed the decoding magic from https://github.com/spotify/docker-client.
 * 
 * @see https://github.com/spotify/docker-client/blob/master/src/main/java/com/spotify/docker/client/LogMessage.java
 */
public class DockerLogMessage {
	final DockerLogStreamType stream;
	final ByteBuffer content;

	public DockerLogMessage(final int streamId, final ByteBuffer content) {
		this(DockerLogStreamType.of(streamId), content);
	}

	public DockerLogMessage(final DockerLogStreamType stream, final ByteBuffer content) {
		this.stream = checkNotNull(stream, "stream");
		this.content = checkNotNull(content, "content");
	}

	public DockerLogStreamType stream() {
		return stream;
	}

	public ByteBuffer content() {
		return content.asReadOnlyBuffer();
	}
}
