package com.infinite.gateway.common.constant;

public interface NacosConstant {

    String NACOS_DEFAULT_NAMESPACE = ""; // nacos默认命名空间，为空，代表Public

    String NACOS_DEFAULT_DATA_ID = "Infinite-Gateway"; // nacos默认Data Id

    String NACOS_DEFAULT_DYNAMIC_THREAD_POOL_DATA_ID = "Infinite-Gateway-ThreadPoolParams"; // nacos默认Data Id

    String NACOS_DEFAULT_GROUP = "DEFAULT_GROUP"; // nacos默认Group

    int NACOS_DEFAULT_TIMEOUT = 5000;  // nacos默认超时时长

}
