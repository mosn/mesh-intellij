/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.mosn.coder.common;

import java.util.regex.Pattern;

/**
 * Constants
 */
public class Constants {

    public static final String PROVIDER = "provider";

    public static final String CONSUMER = "consumer";

    public static final String REGISTER = "register";

    public static final String UNREGISTER = "unregister";

    public static final String SUBSCRIBE = "subscribe";

    public static final String UNSUBSCRIBE = "unsubscribe";

    public static final String CATEGORY_KEY = "category";

    public static final String PROVIDERS_CATEGORY = "providers";

    public static final String CONSUMERS_CATEGORY = "consumers";

    public static final String DEFAULT_CATEGORY = PROVIDERS_CATEGORY;

    public static final String REGISTRY_PROTOCOL = "registry";

    public static final int DEFAULT_IO_THREADS = Math.min(Runtime.getRuntime().availableProcessors() + 1, 32);

    public static final String DEFAULT_KEY_PREFIX = "default.";

    public static final String BACKUP_KEY = "backup";

    public static final String ANYHOST_KEY = "anyhost";

    public static final String ANYHOST_VALUE = "0.0.0.0";

    public static final String LOCALHOST_KEY = "localhost";

    public static final String APPLICATION_KEY = "application";

    public static final String REGISTER_KEY = "register";

    public static final String SUBSCRIBE_KEY = "subscribe";

    public static final String GROUP_KEY = "group";

    public static final String PATH_KEY = "path";

    public static final String INTERFACE_KEY = "interface";

    public static final String VERSION_KEY = "version";

    public static final String DATACENTER_KEY = "datacenter";

    public static final String ZONE_KEY = "zone";

    public static final String ENDPOINT_KEY = "endpoint";

    public static final String INSTANCE_ID_KEY = "instanceId";

    public static final String ACCESS_KEY = "accessKey";

    public static final String SECRET_KEY = "secretKey";

    public static final String ENV_KEY = "env";
    public static final String SHARED = "shared";

    public static final String DEBUG_TIMEOUT = "DEBUG_TIMEOUT";

    public static final Pattern COMMA_SPLIT_PATTERN = Pattern
            .compile("\\s*[,]+\\s*");

}
