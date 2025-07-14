package com.infinite.gateway.register.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.api.naming.NamingFactory;
import com.alibaba.nacos.api.naming.NamingMaintainFactory;
import com.alibaba.nacos.api.naming.NamingMaintainService;
import com.alibaba.nacos.api.naming.NamingService;
import com.alibaba.nacos.api.naming.listener.Event;
import com.alibaba.nacos.api.naming.listener.EventListener;
import com.alibaba.nacos.api.naming.listener.NamingEvent;
import com.alibaba.nacos.api.naming.pojo.Instance;
import com.alibaba.nacos.api.naming.pojo.Service;
import com.alibaba.nacos.api.naming.pojo.ServiceInfo;
import com.alibaba.nacos.common.executor.NameThreadFactory;
import com.alibaba.nacos.common.utils.CollectionUtils;
import com.infinite.gateway.common.constant.GatewayConst;
import com.infinite.gateway.common.pojo.ServiceDefinition;
import com.infinite.gateway.common.pojo.ServiceInstance;
import com.infinite.gateway.config.config.Config;
import com.infinite.gateway.register.listener.RegisterCenterListener;
import com.infinite.gateway.register.service.RegisterCenterService;
import lombok.Builder;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
public class NacosRegisterCenterServiceImpl implements RegisterCenterService {

    /**
     * 静态配置信息
     */
    private Config config;

    /**
     * 用于维护nacos服务实例
     */
    private NamingService namingService;

    /**
     * 用于维护nacos服务定义
     */
    private NamingMaintainService namingMaintainService;

    /**
     * 监听器列表
     * 这里由于监听器可能变更 会出现线程安全问题
     */
    private List<RegisterCenterListener> registerCenterListeners;

    @SneakyThrows(NacosException.class)
    @Override
    public void init(Config config){
        this.config = config;
        namingService = NamingFactory.createNamingService(config.getRegisterCenter().getAddress());
        namingMaintainService = NamingMaintainFactory.createMaintainService(config.getRegisterCenter().getAddress());
        registerCenterListeners = new CopyOnWriteArrayList<>();
    }

    @SneakyThrows(NacosException.class)
    @Override
    public void register(ServiceDefinition serviceDefinition, ServiceInstance serviceInstance) {
        // 把网关实例注册到注册中心
        Instance instance = BeanUtil.toBean(serviceInstance, Instance.class);
        namingService.registerInstance(serviceInstance.getServiceName(), serviceDefinition.getEnv(), instance);

        // 更新服务定义
        namingMaintainService.updateService(
                serviceDefinition.getServiceName(),
                serviceDefinition.getEnv(),
                0,
                Map.of(GatewayConst.META_DATA_KEY, JSON.toJSONString(serviceDefinition))
        );
        log.info("register {} {}", serviceDefinition, serviceInstance);
    }

    @SneakyThrows(NacosException.class)
    @Override
    public void deregister(ServiceDefinition serviceDefinition, ServiceInstance serviceInstance) {
        namingService.deregisterInstance(
                serviceInstance.getServiceName(),
                serviceDefinition.getEnv(),
                serviceInstance.getIp(),
                serviceInstance.getPort()
        );
    }

    @Override
    public void subscribeAllServices(RegisterCenterListener registerCenterListener) {
        registerCenterListeners.add(registerCenterListener);
        Executors.newScheduledThreadPool(1, new NameThreadFactory("NacosRegisterCenterService-doSubscribeAllServices"))
                .scheduleAtFixedRate(this::doSubscribeAllServices, 0, 10, TimeUnit.SECONDS);
    }

    private void doSubscribeAllServices() {
        try {
            //得到当前服务已经订阅的服务
            //这里其实已经在init的时候初始化过 namingService 了，所以这里可以直接拿到当前服务已经订阅的服务
            Set<String> subscribeServiceSet =
                    namingService.getSubscribeServices().stream().map(ServiceInfo::getName).collect(Collectors.toSet());

            int pageNo = 1;
            int pageSize = 100;

            //分页从nacos拿到所有的服务列表
            // TODO 这里的groupName目前是默认按环境隔离，同一个服务名下有多个环境，比如：dev prod
            List<String> serviseList = namingService.getServicesOfServer(pageNo, pageSize, config.getEnv()).getData();

            //拿到所有的服务名称后进行遍历
            while (CollectionUtils.isNotEmpty(serviseList)) {
                log.info("service list size {}", serviseList.size());

                for (String service : serviseList) {
                    //判断是否已经订阅了当前服务
                    if (subscribeServiceSet.contains(service)) {
                        continue;
                    }

                    //nacos事件监听器 订阅当前服务
                    //这里我们需要自己实现一个nacos的事件订阅类 来具体执行订阅执行时的操作
                    EventListener eventListener = new NacosEventListener();
                    //当前服务之前不存在，手动触发事件监听器的回调方法
                    eventListener.onEvent(new NamingEvent(service, null));
                    //为指定的服务和环境注册一个事件监听器
                    namingService.subscribe(service, config.getEnv(), eventListener);
                    log.info("subscribe a service，ServiceName {} Env {}", service, config.getEnv());
                }
                //遍历下一页的服务列表
                serviseList = namingService.getServicesOfServer(++pageNo, pageSize, config.getEnv()).getData();
            }

        } catch (NacosException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 实现对nacos事件的监听器 这个事件监听器会在Nacos发生事件变化的时候进行回调
     * NamingEvent 是一个事件对象，用于表示与服务命名空间（Naming）相关的事件。
     * NamingEvent 的作用是用于监听和处理命名空间中的服务实例（Service Instance）的变化，
     * 以便应用程序可以根据这些变化来动态地更新服务实例列表，以保持与注册中心的同步。
     */
    public class NacosEventListener implements EventListener {
        @Override
        public void onEvent(Event event) {
            //先判断是否是注册中心事件
            if (event instanceof NamingEvent) {
                log.info("the triggered event info is：{}", JSON.toJSON(event));
                NamingEvent namingEvent = (NamingEvent) event;
                //获取当前变更的服务名
                String serviceName = namingEvent.getServiceName();

                try {
                    //获取服务定义信息
                    Service service = namingMaintainService.queryService(serviceName, config.getEnv());
                    //得到服务定义信息
                    ServiceDefinition serviceDefinition = ServiceDefinition.builder()
                            .serviceName(serviceName)
                            .env(service.getGroupName())
                            .enabled(true)
                            .build();

                    //获取服务实例信息
                    List<Instance> allInstances = namingService.getAllInstances(service.getName(), serviceDefinition.getEnv());
                    Set<ServiceInstance> set = new HashSet<>();
                    for (Instance instance : allInstances) {
                        ServiceInstance serviceInstance = BeanUtil.toBean(instance, ServiceInstance.class);
                        serviceInstance.setInstanceId(instance.getIp() + ":" + instance.getPort());
                        if (instance.getServiceName().contains("@@")) {
                            serviceInstance.setServiceName(instance.getServiceName().split("@@")[1]);
                        }
                        set.add(serviceInstance);
                    }
                    //调用我们自己的订阅监听器
                    registerCenterListeners.forEach(registerCenterListener ->
                            registerCenterListener.onChange(serviceDefinition, set));
                } catch (NacosException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }



}
