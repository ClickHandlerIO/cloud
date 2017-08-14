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

interface PullResponse {

  static Builder builder() {
    return new Builder();
  }

  static PullResponse of(List<ReceivedMessage> messages) {
    return builder().receivedMessages(messages).build();
  }

  static PullResponse of(ReceivedMessage... messages) {
    return of(asList(messages));
  }

  List<ReceivedMessage> receivedMessages();

  class Builder implements PullResponse {

    public List<ReceivedMessage> receivedMessages;

    public PullResponse build() {
      return this;
    }

    public List<ReceivedMessage> receivedMessages() {
      return this.receivedMessages;
    }

    public Builder receivedMessages(
        final List<ReceivedMessage> receivedMessages) {
      this.receivedMessages = receivedMessages;
      return this;
    }
  }
}
