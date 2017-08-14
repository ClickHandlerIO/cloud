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

public interface SubscriptionList {

  static Builder builder() {
    return new Builder();
  }

  static SubscriptionList of(Iterable<Subscription> subscriptions) {
    return builder().subscriptions(subscriptions).build();
  }

  static SubscriptionList of(Subscription... subscriptions) {
    return of(asList(subscriptions));
  }

  List<Subscription> subscriptions();

  Optional<String> nextPageToken();

  class Builder implements SubscriptionList {

    public List<Subscription> subscriptions;
    public Optional<String> nextPageToken;

    public SubscriptionList build() {
      return this;
    }

    public List<Subscription> subscriptions() {
      return this.subscriptions;
    }

    public Optional<String> nextPageToken() {
      return this.nextPageToken;
    }

    public Builder subscriptions(
        final Iterable<Subscription> subscriptions) {
      this.subscriptions = Lists.newArrayList(subscriptions);
      return this;
    }

    public Builder subscriptions(
        final Subscription... subscriptions) {
      this.subscriptions = Lists.newArrayList(subscriptions);
      return this;
    }

    public Builder subscriptions(
        final List<Subscription> subscriptions) {
      this.subscriptions = subscriptions;
      return this;
    }

    public Builder nextPageToken(
        final Optional<String> nextPageToken) {
      this.nextPageToken = nextPageToken;
      return this;
    }

    public Builder nextPageToken(
        final String nextPageToken) {
      this.nextPageToken = Optional.ofNullable(nextPageToken);
      return this;
    }
  }
}
