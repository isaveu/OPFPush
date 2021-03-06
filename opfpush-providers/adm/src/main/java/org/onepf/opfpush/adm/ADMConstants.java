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

package org.onepf.opfpush.adm;

/**
 * @author Roman Savin
 * @since 17.12.14
 */
public final class ADMConstants {

    public static final String PROVIDER_NAME = "Amazon Device Messaging";

    static final String AMAZON_MANUFACTURER = "Amazon";
    static final String KINDLE_STORE_APP_PACKAGE = "com.amazon.venezia";
    static final String ACCOUNT_TYPE = "com.amazon.account";

    private ADMConstants() {
        throw new UnsupportedOperationException();
    }
}
