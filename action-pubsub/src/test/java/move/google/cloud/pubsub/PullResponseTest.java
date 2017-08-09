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

import move.google.cloud.pubsub.PullResponse;
import move.google.cloud.pubsub.ReceivedMessage;
import org.junit.Test;

import static org.hamcrest.Matchers.contains;
import static org.junit.Assert.assertThat;

public class PullResponseTest {

  @Test
  public void testOf() throws Exception {
    final ReceivedMessage[] receivedMessages = {ReceivedMessage.ofEncoded("a1", "m1"),
                                                ReceivedMessage.ofEncoded("a2", "m2")};
    final PullResponse pullResponse = PullResponse.of(receivedMessages);
    assertThat(pullResponse.receivedMessages(), contains(receivedMessages));
  }
}