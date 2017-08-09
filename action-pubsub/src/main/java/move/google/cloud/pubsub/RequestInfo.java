/*
 * Copyright (c) 2011-2016 Spotify AB
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package move.google.cloud.pubsub;

public interface RequestInfo {

  String operation();

  String method();

  String uri();

  long payloadSize();

  static RequestInfoBuilder builder() {
    return new RequestInfoBuilder();
  }

  class RequestInfoBuilder implements RequestInfo {

    public String operation;
    public String method;
    public String uri;
    public long payloadSize;

    public RequestInfo build() {
      return this;
    }

    public String operation() {
      return this.operation;
    }

    public String method() {
      return this.method;
    }

    public String uri() {
      return this.uri;
    }

    public long payloadSize() {
      return this.payloadSize;
    }

    public RequestInfoBuilder operation(final String operation) {
      this.operation = operation;
      return this;
    }

    public RequestInfoBuilder method(final String method) {
      this.method = method;
      return this;
    }

    public RequestInfoBuilder uri(final String uri) {
      this.uri = uri;
      return this;
    }

    public RequestInfoBuilder payloadSize(final long payloadSize) {
      this.payloadSize = payloadSize;
      return this;
    }
  }
}
