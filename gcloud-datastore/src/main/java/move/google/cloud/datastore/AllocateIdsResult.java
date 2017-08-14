/*
 * Copyright (c) 2011-2015 Spotify AB
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package move.google.cloud.datastore;

import com.google.common.collect.ImmutableList;

import java.util.List;
import java.util.stream.Collectors;

/**
 * A allocate ids result.
 *
 * Returned from a allocate ids operation.
 */
public final class AllocateIdsResult implements Result {

  private final List<Key> keys;

  private AllocateIdsResult() {
    this.keys = ImmutableList.of();
  }

  private AllocateIdsResult(final List<com.google.datastore.v1.Key> keys) {
    this.keys = ImmutableList.copyOf(keys.stream()
        .map(key -> Key.builder(key).build())
        .collect(Collectors.toList()));
  }

  static AllocateIdsResult build(final com.google.datastore.v1.AllocateIdsResponse response) {
    return new AllocateIdsResult(response.getKeysList());
  }

  /**
   * Build an empty result.
   *
   * @return a new empty allocate ids result.
   */
  public static AllocateIdsResult build() {
    return new AllocateIdsResult();
  }

  /**
   * Return all allocated keys that were generated.
   *
   * @return a list of allocated keys.
   */
  public List<Key> getKeys() {
    return keys;
  }
}