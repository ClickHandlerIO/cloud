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

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_EMPTY;
import static com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleAbstractTypeResolver;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.google.api.client.repackaged.com.google.common.base.Throwables;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

class Json {

  private static final ObjectMapper MAPPER;

  static {
    MAPPER = new ObjectMapper()
        .setSerializationInclusion(NON_EMPTY)
        .configure(FAIL_ON_UNKNOWN_PROPERTIES, false)
        .registerModule(new Jdk8Module())
        .registerModule(new JavaTimeModule())
        .registerModule(new GuavaModule());

    final SimpleModule module = new SimpleModule("PubSub", Version.unknownVersion());
    final SimpleAbstractTypeResolver resolver = new SimpleAbstractTypeResolver();
    resolver.addMapping(AcknowledgeRequest.class, AcknowledgeRequest.Builder.class);
    resolver.addMapping(Message.class, Message.Builder.class);
    resolver.addMapping(ModifyAckDeadlineRequest.class, ModifyAckDeadlineRequest.Builder.class);
    resolver.addMapping(PublishRequest.class, PublishRequest.Builder.class);
    resolver.addMapping(PublishResponse.class, PublishResponse.Builder.class);
    resolver.addMapping(PullRequest.class, PullRequest.Builder.class);
    resolver.addMapping(PullResponse.class, PullResponse.Builder.class);
    resolver.addMapping(PushConfig.class, PushConfig.Builder.class);
    resolver.addMapping(ReceivedMessage.class, ReceivedMessage.Builder.class);
    resolver.addMapping(RequestInfo.class, RequestInfo.Builder.class);
    resolver.addMapping(Subscription.class, Subscription.Builder.class);
    resolver.addMapping(SubscriptionList.class, SubscriptionList.Builder.class);
    resolver.addMapping(Topic.class, Topic.Builder.class);
    resolver.addMapping(TopicList.class, TopicList.Builder.class);

    module.setAbstractTypes(resolver);

    MAPPER.registerModule(module);
  }

  static <T> T read(final byte[] src, final Class<T> cls) throws IOException {
    return MAPPER.readValue(src, cls);
  }

  static <T> T read(final InputStream src, final Class<T> cls) throws IOException {
    return MAPPER.readValue(src, cls);
  }

  static byte[] write(final Object value) {
    try {
      return MAPPER.writeValueAsBytes(value);
    } catch (JsonProcessingException e) {
      throw Throwables.propagate(e);
    }
  }

  static void write(final OutputStream stream, final Object value) throws IOException {
    try {
      MAPPER.writeValue(stream, value);
    } catch (JsonProcessingException e) {
      throw Throwables.propagate(e);
    }
  }
}
