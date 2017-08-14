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

interface PullRequest {

  static Builder builder() {
    return new Builder();
  }

  boolean returnImmediately();

  int maxMessages();

  class Builder implements PullRequest {

    public boolean returnImmediately;
    public int maxMessages;

    public PullRequest build() {
      return this;
    }

    public boolean returnImmediately() {
      return this.returnImmediately;
    }

    public int maxMessages() {
      return this.maxMessages;
    }

    public Builder returnImmediately(final boolean returnImmediately) {
      this.returnImmediately = returnImmediately;
      return this;
    }

    public Builder maxMessages(final int maxMessages) {
      this.maxMessages = maxMessages;
      return this;
    }
  }
}
