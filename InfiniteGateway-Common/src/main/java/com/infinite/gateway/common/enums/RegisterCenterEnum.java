package com.infinite.gateway.common.enums;

import lombok.Getter;

@Getter
public enum RegisterCenterEnum {

    NACOS("NACOS"),
    ZOOKEEPER("ZOOKEEPER");

    private final String name;

    RegisterCenterEnum(String name) {
        this.name = name;
    }

}
