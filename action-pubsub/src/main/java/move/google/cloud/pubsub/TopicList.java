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

import com.google.common.collect.Lists;
import java.util.List;
import java.util.Optional;

public interface TopicList {

  static TopicListBuilder builder() {
    return new TopicListBuilder();
  }

  static TopicList of(Iterable<Topic> topics) {
    return builder().topics(topics).build();
  }

  static TopicList of(Topic... topics) {
    return of(asList(topics));
  }

  List<Topic> topics();

  Optional<String> nextPageToken();

  class TopicListBuilder implements TopicList {

    public List<Topic> topics;
    public Optional<String> nextPageToken;

    public TopicList build() {
      return this;
    }

    @Override
    public List<Topic> topics() {
      return topics;
    }

    @Override
    public Optional<String> nextPageToken() {
      return nextPageToken;
    }

    public TopicListBuilder topics(Topic... topics) {
      this.topics = Lists.newArrayList(topics);
      return this;
    }

    public TopicListBuilder topics(List<Topic> topics) {
      this.topics = topics;
      return this;
    }

    public TopicListBuilder topics(Iterable<Topic> topics) {
      this.topics = Lists.newArrayList(topics);
      return this;
    }

    public TopicListBuilder nextPageToken(Optional<String> nextPageToken) {
      this.nextPageToken = nextPageToken;
      return this;
    }

    public TopicListBuilder nextPageToken(String nextPageToken) {
      this.nextPageToken = Optional.ofNullable(nextPageToken);
      return this;
    }
  }
}
