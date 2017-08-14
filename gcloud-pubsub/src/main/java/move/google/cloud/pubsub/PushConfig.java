/*
 * Copyright (c) 2011-2015 Spotify AB
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

import java.util.Map;
import java.util.Optional;

public interface PushConfig {

  static Builder builder() {
    return new Builder();
  }

  static PushConfig of(String pushEndpoint) {
    return builder().pushEndpoint(pushEndpoint).build();
  }

  static PushConfig of() {
    return builder().build();
  }

  Optional<String> pushEndpoint();

  class Builder implements PushConfig {

    public Optional<String> pushEndpoint;
    public Map<String, String> attributes;

    public PushConfig build() {
      return this;
    }

    public Optional<String> pushEndpoint() {
      return this.pushEndpoint;
    }

    public Builder pushEndpoint(final String pushEndpoint) {
      this.pushEndpoint = Optional.ofNullable(pushEndpoint);
      return this;
    }

    public Builder pushEndpoint(final Optional<String> pushEndpoint) {
      this.pushEndpoint = pushEndpoint;
      return this;
    }
  }
}
