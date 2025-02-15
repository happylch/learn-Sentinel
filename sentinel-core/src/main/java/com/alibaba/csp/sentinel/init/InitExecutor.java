/*
 * Copyright 1999-2018 Alibaba Group Holding Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.csp.sentinel.init;

import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;
import java.util.concurrent.atomic.AtomicBoolean;

import com.alibaba.csp.sentinel.log.RecordLog;

/**
 * Load registered init functions and execute in order.
 *
 *  核心流程，初始化注册 函数
 *
 * @author Eric Zhao
 */
public final class InitExecutor {

    private static AtomicBoolean initialized = new AtomicBoolean(false);

    /**
     * If one {@link InitFunc} throws an exception, the init process
     * will immediately be interrupted and the application will exit.
     *
     * The initialization will be executed only once.
     */
    public static void doInit() {
        //保证只初始化一次
        if (!initialized.compareAndSet(false, true)) {
            return;
        }
        try {
            //基于JVM原生的ServiceLoader（一个服务提供加载工具类，一个服务是一组接口类，服务提供者是服务的具体实现）
            //提供者的类通常实现接口并对服务本身定义的类进行子类化，
            //在 资源目录 classpath:/ resources/META-INF/services/接口全路径名作为filename;
            //Sentinel一共在5个子工程中配置了(不包括demo)，具体参看学习笔记
            ServiceLoader<InitFunc> loader = ServiceLoader.load(InitFunc.class);
            List<OrderWrapper> initList = new ArrayList<OrderWrapper>();
            //对load出来的所有InitFunc类型的对象进行排序，此处的排序主要依赖与在InitFunc具体子类上的@InitOrder注解value值从小到大进行排序
            for (InitFunc initFunc : loader) {
                RecordLog.info("[InitExecutor] Found init func: " + initFunc.getClass().getCanonicalName());
                //进行排序并进行一次OrderWrapper包装
                insertSorted(initList, initFunc);
            }
            //对排序后的InitFunc进行初始化 init执行
            for (OrderWrapper w : initList) {
                //执行每一个InitFunc具体子类的init方法
                w.func.init();
                RecordLog.info(String.format("[InitExecutor] Executing %s with order %d",
                    w.func.getClass().getCanonicalName(), w.order));
            }
        } catch (Exception ex) {
            RecordLog.warn("[InitExecutor] WARN: Initialization failed", ex);
            ex.printStackTrace();
        } catch (Error error) {
            RecordLog.warn("[InitExecutor] ERROR: Initialization failed with fatal error", error);
            error.printStackTrace();
        }
    }

    private static void insertSorted(List<OrderWrapper> list, InitFunc func) {
        int order = resolveOrder(func);
        int idx = 0;
        for (; idx < list.size(); idx++) {
            if (list.get(idx).getOrder() > order) {
                break;
            }
        }
        list.add(idx, new OrderWrapper(order, func));
    }

    private static int resolveOrder(InitFunc func) {
        if (!func.getClass().isAnnotationPresent(InitOrder.class)) {
            return InitOrder.LOWEST_PRECEDENCE;
        } else {
            return func.getClass().getAnnotation(InitOrder.class).value();
        }
    }

    private InitExecutor() {}

    private static class OrderWrapper {
        private final int order;
        private final InitFunc func;

        OrderWrapper(int order, InitFunc func) {
            this.order = order;
            this.func = func;
        }

        int getOrder() {
            return order;
        }

        InitFunc getFunc() {
            return func;
        }
    }
}
