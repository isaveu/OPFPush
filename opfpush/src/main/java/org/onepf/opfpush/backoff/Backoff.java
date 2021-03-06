/*
 * Copyright 2012-2014 One Platform Foundation
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

package org.onepf.opfpush.backoff;

/**
 * @author Kirill Rozov
 * @since 05.09.14.
 */
interface Backoff {

    /**
     * Maximum number of try to register push provider.
     * Zero value means that will be no retry.
     */
    boolean hasTries();

    /**
     * Get delay before next attempt to register push provider.
     *
     * @return Period in milliseconds to wait before next try ro register.
     * @throws {@link java.util.NoSuchElementException} if there are no more elements.
     */
    long getTryDelay();

    /**
     * Reset all tries to initial state.
     */
    void reset();
}
