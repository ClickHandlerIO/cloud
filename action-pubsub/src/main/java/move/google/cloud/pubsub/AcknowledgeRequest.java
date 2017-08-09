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

import java.util.List;

interface AcknowledgeRequest {

  static AcknowledgeRequestBuilder builder() {
    return new AcknowledgeRequestBuilder();
  }

  List<String> ackIds();

  class AcknowledgeRequestBuilder implements AcknowledgeRequest {

    public List<String> ackIds;

    public AcknowledgeRequest build() {
      return this;
    }

    public List<String> ackIds() {
      return this.ackIds;
    }

    public AcknowledgeRequestBuilder ackIds(final List<String> ackIds) {
      this.ackIds = ackIds;
      return this;
    }
  }
}
