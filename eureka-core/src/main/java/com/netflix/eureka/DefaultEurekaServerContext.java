/*
 * Copyright 2015 Netflix, Inc.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.netflix.eureka;

import com.netflix.appinfo.ApplicationInfoManager;
import com.netflix.discovery.DiscoveryManager;
import com.netflix.eureka.cluster.PeerEurekaNodes;
import com.netflix.eureka.registry.PeerAwareInstanceRegistry;
import com.netflix.eureka.resources.ServerCodecs;
import com.netflix.eureka.util.EurekaMonitors;
import com.netflix.eureka.util.ServoControl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Represent the local server context and exposes getters to components of the
 * local server such as the registry.
 *
 * Eureka-Server 上下文接口，提供Eureka-Server 内部各组件对象的初始化、关闭、获取等方法。
 * @author David Liu
 */
@Singleton
public class DefaultEurekaServerContext implements EurekaServerContext {
    private static final Logger logger = LoggerFactory.getLogger(DefaultEurekaServerContext.class);

    /**
     * Eureka-Server 配置
     */
    private final EurekaServerConfig serverConfig;
    /**
     * Eureka-Server 请求和响应编解码器
     */
    private final ServerCodecs serverCodecs;
    /**
     * 应用实例信息的注册表
     */
    private final PeerAwareInstanceRegistry registry;
    /**
     * Eureka-Server 集群节点集合
     */
    private final PeerEurekaNodes peerEurekaNodes;
    /**
     * 应用实例信息管理器
     */
    private final ApplicationInfoManager applicationInfoManager;

    @Inject
    public DefaultEurekaServerContext(EurekaServerConfig serverConfig,
                               ServerCodecs serverCodecs,
                               PeerAwareInstanceRegistry registry,
                               PeerEurekaNodes peerEurekaNodes,
                               ApplicationInfoManager applicationInfoManager) {
        this.serverConfig = serverConfig;
        this.serverCodecs = serverCodecs;
        this.registry = registry;
        this.peerEurekaNodes = peerEurekaNodes;
        this.applicationInfoManager = applicationInfoManager;
    }

    @PostConstruct
    @Override
    public void initialize() {
        logger.info("Initializing ...");
        // 将 eureka server 集群给启动起来，更新一下 eureka server 集群的信息，让当前的 eureka 感知到所有的其他的 eureka server
        // 然后搞一个定时任务，就一个后台线程，每隔一段时间，更新集群信息
        peerEurekaNodes.start();
        try {
            // 将 eureka server 集群中所有的 eureka server 的注册表的信息都抓取过来，放到自己本地的注册表里
            registry.init(peerEurekaNodes);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        logger.info("Initialized");
    }

    @PreDestroy
    @Override
    public void shutdown() {
        logger.info("Shutting down ...");
        registry.shutdown();
        peerEurekaNodes.shutdown();
        ServoControl.shutdown();
        EurekaMonitors.shutdown();
        logger.info("Shut down");
    }

    @Override
    public EurekaServerConfig getServerConfig() {
        return serverConfig;
    }

    @Override
    public PeerEurekaNodes getPeerEurekaNodes() {
        return peerEurekaNodes;
    }

    @Override
    public ServerCodecs getServerCodecs() {
        return serverCodecs;
    }

    @Override
    public PeerAwareInstanceRegistry getRegistry() {
        return registry;
    }

    @Override
    public ApplicationInfoManager getApplicationInfoManager() {
        return applicationInfoManager;
    }

}
