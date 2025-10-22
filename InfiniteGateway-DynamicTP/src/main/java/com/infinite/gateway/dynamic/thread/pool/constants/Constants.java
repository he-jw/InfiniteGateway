/*
 * 动态线程池（oneThread）基础组件项目
 *
 * 版权所有 (C) [2024-至今] [山东流年网络科技有限公司]
 *
 * 保留所有权利。
 *
 * 1. 定义和解释
 *    本文件（包括其任何修改、更新和衍生内容）是由[山东流年网络科技有限公司]及相关人员开发的。
 *    "软件"指的是与本文件相关的任何代码、脚本、文档和相关的资源。
 *
 * 2. 使用许可
 *    本软件的使用、分发和解释均受中华人民共和国法律的管辖。只有在遵守以下条件的前提下，才允许使用和分发本软件：
 *    a. 未经[山东流年网络科技有限公司]的明确书面许可，不得对本软件进行修改、复制、分发、出售或出租。
 *    b. 任何未授权的复制、分发或修改都将被视为侵犯[山东流年网络科技有限公司]的知识产权。
 *
 * 3. 免责声明
 *    本软件按"原样"提供，没有任何明示或暗示的保证，包括但不限于适销性、特定用途的适用性和非侵权性的保证。
 *    在任何情况下，[山东流年网络科技有限公司]均不对任何直接、间接、偶然、特殊、典型或间接的损害（包括但不限于采购替代商品或服务；使用、数据或利润损失）承担责任。
 *
 * 4. 侵权通知与处理
 *    a. 如果[山东流年网络科技有限公司]发现或收到第三方通知，表明存在可能侵犯其知识产权的行为，公司将采取必要的措施以保护其权利。
 *    b. 对于任何涉嫌侵犯知识产权的行为，[山东流年网络科技有限公司]可能要求侵权方立即停止侵权行为，并采取补救措施，包括但不限于删除侵权内容、停止侵权产品的分发等。
 *    c. 如果侵权行为持续存在或未能得到妥善解决，[山东流年网络科技有限公司]保留采取进一步法律行动的权利，包括但不限于发出警告信、提起民事诉讼或刑事诉讼。
 *
 * 5. 其他条款
 *    a. [山东流年网络科技有限公司]保留随时修改这些条款的权利。
 *    b. 如果您不同意这些条款，请勿使用本软件。
 *
 * 未经[山东流年网络科技有限公司]的明确书面许可，不得使用此文件的任何部分。
 *
 * 本软件受到[山东流年网络科技有限公司]及其许可人的版权保护。
 */

package com.infinite.gateway.dynamic.thread.pool.constants;

/**
 * 动态线程池基础常量类
 * <p>
 * 作者：马丁
 * 加项目群：早加入就是优势！500人内部项目群，分享的知识总有你需要的 <a href="https://t.zsxq.com/cw7b9" />
 * 开发时间：2025-04-30
 */
public class Constants {

    /**
     * 线程池参数变更日志打印常量
     */
    public static final String CHANGE_THREAD_POOL_TEXT = "[{}] Dynamic thread pool parameter changed:"
            + "\n    corePoolSize: {}"
            + "\n    maximumPoolSize: {}"
            + "\n    capacity: {}"
            + "\n    keepAliveTime: {}"
            + "\n    rejectedType: {}"
            + "\n    allowCoreThreadTimeOut: {}";

    /**
     * 线程池参数变更前后分隔符常量
     */
    public static final String CHANGE_DELIMITER = "%s => %s";

    /**
     * 钉钉配置变更消息文本
     */
    public static final String DING_CONFIG_CHANGE_MESSAGE_TEXT = """
            **<font color=#2a9d8f>[通知] </font>%s - 动态线程池参数变更**
            
             ---
            
            <font color='#708090' size=2>线程池ID：%s</font>\s
            
            <font color='#708090' size=2>应用实例：%s</font>\s
            
            <font color='#708090' size=2>核心线程数：%s</font>\s
            
            <font color='#708090' size=2>最大线程数：%s</font>\s
            
            <font color='#708090' size=2>线程存活时间：%s</font>\s
            
            <font color='#708090' size=2>队列类型：%s</font>\s
            
            <font color='#708090' size=2>队列容量：%s</font>\s
            
            <font color='#708090' size=2>旧拒绝策略：%s</font>\s
            
            <font color='#708090' size=2>新拒绝策略：%s</font>\s
            
            <font color='#708090' size=2>OWNER：@%s</font>\s
            
            <font color='#708090' size=2>提示：动态线程池配置变更实时通知（无限制）</font>\s
            
             ---
            
            **变更时间：%s**
            """;

    /**
     * 钉钉配置 Web 线程池变更消息文本
     */
    public static final String DING_CONFIG_WEB_CHANGE_MESSAGE_TEXT = """
            **<font color=#2a9d8f>[通知] </font>%s - %s线程池参数变更**
            
             ---
            
            <font color='#708090' size=2>应用实例：%s</font>\s
            
            <font color='#708090' size=2>核心线程数：%s</font>\s
            
            <font color='#708090' size=2>最大线程数：%s</font>\s
            
            <font color='#708090' size=2>线程存活时间：%s</font>\s
            
            <font color='#708090' size=2>OWNER：@%s</font>\s
            
            <font color='#708090' size=2>提示：%s线程池配置变更实时通知（无限制）</font>\s
            
             ---
            
            **变更时间：%s**
            """;

    /**
     * 钉钉配置报警消息文本
     */
    public static final String DING_ALARM_NOTIFY_MESSAGE_TEXT = """
            **<font color=#FF0000>[警报] </font>%s - 动态线程池运行告警**
            
             ---
            
            <font color='#708090' size=2>线程池ID：%s</font>\s
            
            <font color='#708090' size=2>应用实例：%s</font>\s
            
            <font color='#708090' size=2>告警类型：%s</font>\s
            
             ---
            
            <font color='#708090' size=2>核心线程数：%d</font>\s
            
            <font color='#708090' size=2>最大线程数：%d</font>\s
            
            <font color='#708090' size=2>当前线程数：%d</font>\s
            
            <font color='#708090' size=2>活跃线程数：%d</font>\s
            
            <font color='#708090' size=2>同存最大线程数：%d</font>\s
            
            <font color='#708090' size=2>线程池任务总量：%d</font>\s
            
             ---
            
            <font color='#708090' size=2>队列类型：%s</font>\s
            
            <font color='#708090' size=2>队列容量：%d</font>\s
            
            <font color='#708090' size=2>队列元素个数：%d</font>\s
            
            <font color='#708090' size=2>队列剩余个数：%d</font>\s
            
             ---
            
            <font color='#708090' size=2>拒绝策略：%s</font>\s
            
            <font color='#708090' size=2>拒绝策略执行次数：</font><font color='#FF0000' size=2>%d</font>\s
            
            <font color='#708090' size=2>OWNER：@%s</font>\s
            
            <font color='#708090' size=2>提示：%d分钟内此线程池不会重复告警（可配置）</font>\s
            
             ---
            
            **告警时间：%s**
            """;
}
