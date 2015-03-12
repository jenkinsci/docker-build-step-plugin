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

/**
 * Stream support should be rolled into docker-java, see https://github.com/docker-java/docker-java/issues/42.
 * 
 * As a workaround we borrowed the decoding magic from https://github.com/spotify/docker-client.
 * 
 * @see https://github.com/spotify/docker-client/blob/master/src/main/java/com/spotify/docker/client/LogMessage.java
 */
public enum DockerLogStreamType {
	STDIN(0),
	STDOUT(1),
	STDERR(2);
	
	private final int id;

	DockerLogStreamType(int id) {
		this.id = id;
	}

	public int id() {
		return id;
	}

	public static DockerLogStreamType of(final int id) {
		switch (id) {
		case 0:
			return STDIN;
		case 1:
			return STDOUT;
		case 2:
			return STDERR;
		default:
			throw new IllegalArgumentException();
		}
	}
}
