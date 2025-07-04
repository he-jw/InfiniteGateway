package com.infinite.gateway.config.utils;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import java.io.IOException;
import java.io.InputStream;

/**
 * 加载配置文件工具类
 */
public class ConfigLoadUtil {

    // 使用 Jackson 的 YAMLFactory 初始化 ObjectMapper，用于解析 YAML 文件
    private static final ObjectMapper mapper = new ObjectMapper(new YAMLFactory());

    static {
        // 忽略反序列化时未知字段的错误（即 YAML 文件中存在但 Java 类中不存在的字段）
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    /**
     * 从指定路径加载 YAML 文件，并将其映射为指定类型的 Java 对象。
     *
     * @param filePath 配置文件路径（相对于 resources 目录）
     * @param clazz    要映射的目标类类型
     * @param prefix   可选前缀，用于提取配置文件中的某个子节点
     * @param <T>      泛型类型
     * @return 映射后的对象实例，若文件未找到或解析失败则抛出异常
     */
    public static <T> T loadConfigFromYaml(String filePath, Class<T> clazz, String prefix) {
        try (InputStream inputStream = ConfigLoadUtil.class.getClassLoader().getResourceAsStream(filePath)) {
            if (inputStream == null) return null;

            // 将 YAML 文件读取为 Jackson 的 ObjectNode 树结构
            ObjectNode rootNode = (ObjectNode) mapper.readTree(inputStream);

            // 获取指定前缀对应的子节点
            ObjectNode subNode = getSubNode(rootNode, prefix);

            // 将子节点树转换为目标类的对象
            return mapper.treeToValue(subNode, clazz);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 获取配置树中指定前缀对应的子节点。
     *
     * @param node   当前节点（通常是根节点）
     * @param prefix 点分格式的路径前缀（例如 "server.http"）
     * @return 子节点对象，如果路径无效则返回 null
     */
    private static ObjectNode getSubNode(ObjectNode node, String prefix) {
        if (prefix == null || prefix.isEmpty()) return node;

        // 按照点号拆分前缀字符串
        String[] keys = prefix.split("\\.");

        for (String key : keys) {
            if (node == null || node.isMissingNode() || node.isNull()) {
                return null;
            }
            node = (ObjectNode) node.get(key); // 进入下一层节点
        }

        return node;
    }

}
