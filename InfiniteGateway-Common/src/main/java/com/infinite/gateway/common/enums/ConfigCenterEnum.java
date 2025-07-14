package com.infinite.gateway.common.enums;

import lombok.Getter;

@Getter
public enum ConfigCenterEnum {

    NACOS("NACOS"),
    ZOOKEEPER("ZOOKEEPER");

    private final String name;

    ConfigCenterEnum(String name) {
        this.name = name;
    }

}
