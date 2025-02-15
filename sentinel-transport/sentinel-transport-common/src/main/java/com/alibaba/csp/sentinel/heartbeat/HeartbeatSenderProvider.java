/*
 * Copyright 1999-2019 Alibaba Group Holding Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.csp.sentinel.heartbeat;

import com.alibaba.csp.sentinel.log.RecordLog;
import com.alibaba.csp.sentinel.transport.HeartbeatSender;
import com.alibaba.csp.sentinel.util.SpiLoader;

/**
 * @author Eric Zhao
 * @since 1.6.0
 */
public final class HeartbeatSenderProvider {

    private static HeartbeatSender heartbeatSender = null;

    static {
        resolveInstance();
    }

    private static void resolveInstance() {
         /*
          此处获取的是 sentinel-transport-simple-http中的com.alibaba.csp.sentinel.transport.heartbeat.SimpleHttpHeartbeatSender
         */
        HeartbeatSender resolved = SpiLoader.loadHighestPriorityInstance(HeartbeatSender.class);
        if (resolved == null) {
            RecordLog.warn("[HeartbeatSenderProvider] WARN: No existing HeartbeatSender found");
        } else {
            heartbeatSender = resolved;
            RecordLog.info("[HeartbeatSenderProvider] HeartbeatSender activated: " + resolved.getClass()
                .getCanonicalName());
        }
    }

    /**
     * Get resolved {@link HeartbeatSender} instance.
     *
     * @return resolved {@code HeartbeatSender} instance
     */
    public static HeartbeatSender getHeartbeatSender() {
        return heartbeatSender;
    }

    private HeartbeatSenderProvider() {}
}
