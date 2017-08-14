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

import static java.util.Arrays.asList;

import java.util.List;

interface PublishResponse {

  static Builder builder() {
    return new Builder();
  }

  static PublishResponse of(List<String> messageIds) {
    return builder().messageIds(messageIds).build();
  }

  static PublishResponse of(String... messageIds) {
    return of(asList(messageIds));
  }

  List<String> messageIds();

  class Builder implements PublishResponse {

    public List<String> messageIds;

    public PublishResponse build() {
      return this;
    }

    public List<String> messageIds() {
      return this.messageIds;
    }

    public Builder messageIds(final List<String> messageIds) {
      this.messageIds = messageIds;
      return this;
    }
  }
}
