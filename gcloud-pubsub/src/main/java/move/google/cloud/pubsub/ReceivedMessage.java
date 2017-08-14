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

public interface ReceivedMessage {

  static ReceivedMessage of(final String ackId, final Message message) {
    return builder().ackId(ackId).message(message).build();
  }

  static ReceivedMessage of(final String ackId, final String data) {
    return builder().ackId(ackId).message(Message.of(data)).build();
  }

  static ReceivedMessage ofEncoded(final String ackId, final String data) {
    return builder().ackId(ackId).message(Message.ofEncoded(data)).build();
  }

  static Builder builder() {
    return new Builder();
  }

  String ackId();

  Message message();

  class Builder implements ReceivedMessage {

    public String ackId;
    public Message message;

    public ReceivedMessage build() {
      return this;
    }

    public String ackId() {
      return this.ackId;
    }

    public Message message() {
      return this.message;
    }

    public Builder ackId(final String ackId) {
      this.ackId = ackId;
      return this;
    }

    public Builder message(
        final Message message) {
      this.message = message;
      return this;
    }
  }
}
