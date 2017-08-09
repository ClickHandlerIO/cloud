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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Strings.isNullOrEmpty;
import static move.google.cloud.pubsub.Topic.canonicalTopic;
import static move.google.cloud.pubsub.Topic.validateCanonicalTopic;

import java.util.Optional;
import java.util.regex.Pattern;


public interface Subscription {

  Pattern PATTERN = Pattern.compile("^projects/[^/]*/subscriptions/[^/]*$");

  String PROJECTS = "projects";
  String SUBSCRIPTIONS = "subscriptions";

  static SubscriptionBuilder builder() {
    return new SubscriptionBuilder();
  }

  static Subscription of(String project, String name, String topic) {
    return of(canonicalSubscription(project, name), canonicalTopic(project, topic));
  }

  static Subscription of(String canonicalSubscription, String canonicalTopic) {
    validateCanonicalSubscription(canonicalSubscription);
    validateCanonicalTopic(canonicalTopic);
    return builder().name(canonicalSubscription).topic(canonicalTopic).build();
  }

  static String canonicalSubscription(final String project, final String subscription) {
    checkArgument(!isNullOrEmpty(project) && !project.contains("/"), "illegal project: %s",
        project);
    checkArgument(!isNullOrEmpty(subscription) && !subscription.contains("/") && !subscription
            .startsWith("goog"),
        "illegal subscription: %s", subscription);
    return PROJECTS + '/' + project + '/' + SUBSCRIPTIONS + '/' + subscription;
  }

  static void validateCanonicalSubscription(final String canonicalSubscription) {
    checkArgument(PATTERN.matcher(canonicalSubscription).matches(), "malformed subscription: %s",
        canonicalSubscription);
  }

  String name();

  String topic();

  Optional<PushConfig> pushConfig();

  Optional<Integer> ackDeadlineSeconds();

  Create forCreate();

  class SubscriptionBuilder implements Subscription {
    public String name;
    public String topic;
    public Optional<PushConfig> pushConfig;
    public Optional<Integer> ackDeadlineSeconds;

    public Subscription build() {
      return this;
    }

    @Override
    public Create forCreate() {
      final Create create = new Create();
      create.topic = topic;
      create.pushConfig = pushConfig;
      create.ackDeadlineSeconds = ackDeadlineSeconds;
      return create;
    }

    public String name() {
      return this.name;
    }

    public String topic() {
      return this.topic;
    }

    public Optional<PushConfig> pushConfig() {
      return this.pushConfig;
    }

    public Optional<Integer> ackDeadlineSeconds() {
      return this.ackDeadlineSeconds;
    }

    public SubscriptionBuilder name(final String name) {
      this.name = name;
      return this;
    }

    public SubscriptionBuilder topic(final String topic) {
      this.topic = topic;
      return this;
    }

    public SubscriptionBuilder pushConfig(
        final Optional<PushConfig> pushConfig) {
      this.pushConfig = pushConfig;
      return this;
    }

    public SubscriptionBuilder ackDeadlineSeconds(
        final Optional<Integer> ackDeadlineSeconds) {
      this.ackDeadlineSeconds = ackDeadlineSeconds;
      return this;
    }
  }

  class Create {
    public String topic;
    public Optional<PushConfig> pushConfig;
    public Optional<Integer> ackDeadlineSeconds;
  }
}
