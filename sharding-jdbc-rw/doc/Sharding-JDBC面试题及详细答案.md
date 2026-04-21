# Sharding-JDBC 面试题及详细答案

## 目录

1. [基础概念篇](#基础概念篇)
2. [分片策略篇](#分片策略篇)
3. [读写分离篇](#读写分离篇)
4. [核心原理篇](#核心原理篇)
5. [性能优化篇](#性能优化篇)
6. [实战应用篇](#实战应用篇)
7. [高级特性篇](#高级特性篇)
8. [常见问题篇](#常见问题篇)

---

## 基础概念篇

### 1. 什么是 Sharding-JDBC？它的核心特点是什么？

**答案：**

Sharding-JDBC 是 Apache ShardingSphere 的第一个产品，定位为轻量级 Java 框架，在 Java 的 JDBC 层提供的额外服务。它使用客户端直连数据库，以 jar 包形式提供服务，无需额外部署和依赖，可理解为增强版的 JDBC 驱动。

**核心特点：**

1. **轻量级**：无需额外部署，以 jar 包形式提供服务
2. **零侵入**：对应用代码零侵入，仅修改配置即可
3. **兼容性好**：兼容各种 ORM 框架（MyBatis、Hibernate、JPA 等）
4. **功能丰富**：支持分库分表、读写分离、分布式事务等
5. **性能优异**：采用客户端直连，减少网络开销
6. **灵活配置**：支持多种配置方式（YAML、Spring Boot、Java API）

### 2. Sharding-JDBC 与 Sharding-Proxy、Sharding-Sidecar 的区别是什么？

**答案：**

| 特性 | Sharding-JDBC | Sharding-Proxy | Sharding-Sidecar |
|------|---------------|----------------|------------------|
| **定位** | 客户端分片 | 服务端分片 | 云原生分片 |
| **部署方式** | Jar 包嵌入应用 | 独立部署服务 | Sidecar 模式 |
| **网络开销** | 低（直连数据库） | 中（通过 Proxy） | 低 |
| **语言支持** | Java | 多语言（通过协议） | 多语言 |
| **复杂度** | 低 | 中 | 高 |
| **适用场景** | Java 应用 | 多语言混合应用 | Kubernetes 环境 |

**选择建议：**
- 纯 Java 应用：选择 Sharding-JDBC
- 多语言混合应用：选择 Sharding-Proxy
- Kubernetes 云原生环境：选择 Sharding-Sidecar

### 3. Sharding-JDBC 支持哪些数据库？

**答案：**

Sharding-JDBC 支持主流的关系型数据库：

1. **MySQL**：5.6.5+ 版本
2. **PostgreSQL**：8.4+ 版本
3. **SQLServer**：2005+ 版本
4. **Oracle**：10g+ 版本
5. **DB2**：9.7+ 版本
6. **H2**：1.4.193+ 版本

此外，还支持一些国产数据库如达梦、人大金仓等。

### 4. 什么是逻辑表、物理表、绑定表、广播表？

**答案：**

**1. 逻辑表（Logic Table）**
- 应用层感知的表，即分片规则中的表名
- 例如：`ksd_user` 是逻辑表名

**2. 物理表（Physical Table）**
- 实际存储在数据库中的表
- 例如：`ds0.ksd_user_0`、`ds0.ksd_user_1` 是物理表

**3. 绑定表（Binding Table）**
- 指分片规则一致的主表和子表
- 例如：订单表和订单详情表使用相同的分片键和分片算法
- 优势：避免跨库 JOIN，提高查询性能

**4. 广播表（Broadcast Table）**
- 在所有分片数据源中都存在的表
- 表结构和数据完全相同
- 例如：字典表、配置表等
- 优势：避免跨库查询，提高查询效率

---

## 分片策略篇

### 5. Sharding-JDBC 支持哪些分片算法？

**答案：**

Sharding-JDBC 提供了 5 种分片算法：

**1. 精确分片算法（PreciseShardingAlgorithm）**
- 用于处理 = 和 IN 的分片
- 返回单个目标数据源或表
- 示例：按用户 ID 精确分片

```java
public class UserIdShardingAlgorithm implements PreciseShardingAlgorithm<Long> {
    @Override
    public String doSharding(Collection<String> availableTargetNames, 
                           PreciseShardingValue<Long> shardingValue) {
        Long userId = shardingValue.getValue();
        return "ds" + (userId % 2);
    }
}
```

**2. 范围分片算法（RangeShardingAlgorithm）**
- 用于处理 BETWEEN、>、<、>=、<= 的分片
- 返回多个目标数据源或表
- 示例：按时间范围分片

```java
public class DateRangeShardingAlgorithm implements RangeShardingAlgorithm<Date> {
    @Override
    public Collection<String> doSharding(Collection<String> availableTargetNames,
                                         RangeShardingValue<Date> shardingValue) {
        // 返回符合时间范围的所有表
        return availableTargetNames;
    }
}
```

**3. 复合分片算法（ComplexKeysShardingAlgorithm）**
- 用于处理多分片键的分片
- 支持多个字段的组合分片
- 示例：按用户 ID 和订单 ID 组合分片

**4. Hint 分片算法（HintShardingAlgorithm）**
- 通过 Hint 指定分片值
- 不依赖 SQL 中的分片键
- 示例：强制路由到指定数据源

**5. 行表达式分片算法（Inline）**
- 使用 Groovy 表达式语法
- 适合简单的取模、范围分片
- 配置简单，无需编写代码
- 示例：`ds$->{user_id % 2}`、`table_$->{age % 2}`

### 6. 如何选择分片键？有哪些原则？

**答案：**

**分片键选择原则：**

1. **查询频率高**
   - 选择经常作为查询条件的字段
   - 例如：用户表的 user_id、订单表的 order_id

2. **数据分布均匀**
   - 避免数据倾斜
   - 例如：避免使用性别、状态等基数小的字段

3. **避免频繁更新**
   - 分片键更新会导致数据迁移
   - 例如：避免使用状态、等级等经常变化的字段

4. **基数足够大**
   - 确保分片效果明显
   - 例如：用户 ID、订单 ID 等唯一标识

5. **业务关联性**
   - 考虑业务查询模式
   - 例如：按用户 ID 分片，便于查询用户的所有订单

**常见分片键选择：**
- 用户表：user_id
- 订单表：user_id 或 order_id
- 日志表：create_time
- 商品表：category_id 或 product_id

### 7. 什么是分片策略？Sharding-JDBC 支持哪些分片策略？

**答案：**

**分片策略**是指数据如何分配到不同的数据库和表中的规则。Sharding-JDBC 支持以下分片策略：

**1. 标准分片策略（StandardStrategy）**
- 支持精确分片和范围分片
- 适用于单分片键场景
- 配置示例：

```yaml
table-strategy:
  standard:
    sharding-column: user_id
    precise-algorithm-class-name: com.example.UserIdAlgorithm
    range-algorithm-class-name: com.example.UserIdRangeAlgorithm
```

**2. 复合分片策略（ComplexStrategy）**
- 支持多分片键组合
- 适用于多字段组合分片场景
- 配置示例：

```yaml
table-strategy:
  complex:
    sharding-columns: user_id, order_id
    algorithm-class-name: com.example.ComplexAlgorithm
```

**3. Hint 分片策略（HintStrategy）**
- 通过 Hint 指定分片值
- 不依赖 SQL 中的分片键
- 配置示例：

```yaml
table-strategy:
  hint:
    algorithm-class-name: com.example.HintAlgorithm
```

**4. 行表达式分片策略（InlineStrategy）**
- 使用 Groovy 表达式
- 配置简单，适合简单场景
- 配置示例：

```yaml
table-strategy:
  inline:
    sharding-column: user_id
    algorithm-expression: table_$->{user_id % 2}
```

**5. 不分片策略（NoneStrategy）**
- 不进行分片
- 适用于广播表或不需要分片的表

### 8. 如何配置分库分表？请给出一个完整的配置示例。

**答案：**

**完整配置示例（基于 Spring Boot）：**

```yaml
spring:
  shardingsphere:
    # 显示 SQL
    props:
      sql:
        show: true
    
    # 数据源配置
    datasource:
      names: ds0,ds1
      ds0:
        type: com.alibaba.druid.pool.DruidDataSource
        driver-class-name: com.mysql.cj.jdbc.Driver
        url: jdbc:mysql://localhost:3306/demo_ds_0
        username: root
        password: 123456
      ds1:
        type: com.alibaba.druid.pool.DruidDataSource
        driver-class-name: com.mysql.cj.jdbc.Driver
        url: jdbc:mysql://localhost:3306/demo_ds_1
        username: root
        password: 123456
    
    # 分片规则配置
    sharding:
      # 默认数据源
      default-data-source-name: ds0
      
      # 分表配置
      tables:
        # 逻辑表名
        t_order:
          # 数据节点
          actual-data-nodes: ds$->{0..1}.t_order_$->{0..1}
          
          # 主键生成策略
          key-generator:
            column: order_id
            type: SNOWFLAKE
          
          # 分库策略
          database-strategy:
            inline:
              sharding-column: user_id
              algorithm-expression: ds$->{user_id % 2}
          
          # 分表策略
          table-strategy:
            inline:
              sharding-column: order_id
              algorithm-expression: t_order_$->{order_id % 2}
      
      # 绑定表配置
      binding-tables:
        - t_order,t_order_item
      
      # 广播表配置
      broadcast-tables:
        - t_config
```

**配置说明：**

1. **actual-data-nodes**：定义数据节点，格式为 `数据源$->{范围}.表名$->{范围}`
2. **key-generator**：主键生成策略，支持 SNOWFLAKE、UUID 等
3. **database-strategy**：分库策略
4. **table-strategy**：分表策略
5. **binding-tables**：绑定表配置，避免跨库 JOIN
6. **broadcast-tables**：广播表配置，在所有数据源中存在

### 9. 什么是数据节点（Data Node）？如何配置？

**答案：**

**数据节点（Data Node）**是数据源和物理表的组合，表示数据实际存储的位置。

**数据节点格式：**
```
数据源$->{范围}.逻辑表名$->{范围}
```

**配置示例：**

```yaml
# 示例1：2个数据库，每个数据库2张表
actual-data-nodes: ds$->{0..1}.t_order_$->{0..1}
# 结果：ds0.t_order_0, ds0.t_order_1, ds1.t_order_0, ds1.t_order_1

# 示例2：3个数据库，每个数据库3张表
actual-data-nodes: ds$->{0..2}.t_order_$->{0..2}
# 结果：ds0.t_order_0, ds0.t_order_1, ds0.t_order_2, ds1.t_order_0, ...

# 示例3：按年月分表
actual-data-nodes: ds0.t_order_$->{2021..2022}${(1..12).collect{t -> t.toString().padLeft(2,'0')}}
# 结果：ds0.t_order_202101, ds0.t_order_202102, ..., ds0.t_order_202212

# 示例4：单库多表
actual-data-nodes: ds0.t_order_$->{0..3}
# 结果：ds0.t_order_0, ds0.t_order_1, ds0.t_order_2, ds0.t_order_3
```

**Groovy 表达式说明：**
- `$->{0..1}`：生成 0 和 1
- `$->{2021..2022}`：生成 2021 和 2022
- `${(1..12).collect{t -> t.toString().padLeft(2,'0')}}`：生成 01 到 12

---

## 读写分离篇

### 10. Sharding-JDBC 如何实现读写分离？有哪些负载均衡策略？

**答案：**

**读写分离实现原理：**

1. **配置主从数据源**
   - 主库：负责写操作（INSERT、UPDATE、DELETE）
   - 从库：负责读操作（SELECT）

2. **SQL 路由**
   - 写操作自动路由到主库
   - 读操作路由到从库

3. **负载均衡**
   - 支持多种负载均衡策略

**配置示例：**

```yaml
spring:
  shardingsphere:
    datasource:
      names: master,slave0,slave1
      master:
        type: com.alibaba.druid.pool.DruidDataSource
        driver-class-name: com.mysql.cj.jdbc.Driver
        url: jdbc:mysql://master:3306/demo
        username: root
        password: 123456
      slave0:
        type: com.alibaba.druid.pool.DruidDataSource
        driver-class-name: com.mysql.cj.jdbc.Driver
        url: jdbc:mysql://slave0:3306/demo
        username: root
        password: 123456
      slave1:
        type: com.alibaba.druid.pool.DruidDataSource
        driver-class-name: com.mysql.cj.jdbc.Driver
        url: jdbc:mysql://slave1:3306/demo
        username: root
        password: 123456
    
    masterslave:
      name: ms
      master-data-source-name: master
      slave-data-source-names: slave0,slave1
      load-balance-algorithm-type: round_robin
```

**负载均衡策略：**

1. **round_robin（轮询）**
   - 按顺序轮询分配到各个从库
   - 适用于从库性能相近的场景

2. **random（随机）**
   - 随机选择从库
   - 适用于从库性能相近的场景

3. **weight（权重）**
   - 根据权重分配请求
   - 适用于从库性能不均的场景
   - 需要自定义实现

### 11. 读写分离中如何保证数据一致性？

**答案：**

**数据一致性挑战：**

1. **主从延迟**
   - 主库写入后，从库可能还未同步
   - 导致读操作可能读到旧数据

2. **最终一致性**
   - MySQL 主从复制是异步的
   - 存在短暂的数据不一致

**解决方案：**

**1. 强制读主库**
```java
// 使用 Hint 强制读主库
HintManager.getInstance().setMasterRouteOnly();
try {
    // 执行查询
    List<Order> orders = orderMapper.selectAll();
} finally {
    HintManager.clear();
}
```

**2. 延迟双删**
```java
// 第一次删除
orderMapper.delete(orderId);

// 延迟一段时间
Thread.sleep(500);

// 第二次删除
orderMapper.delete(orderId);
```

**3. 消息队列补偿**
- 使用消息队列保证最终一致性
- 主库写入后发送消息
- 消费者处理数据同步

**4. 业务容忍**
- 对于非关键业务，容忍短暂不一致
- 例如：评论数、点赞数等

**5. 监控主从延迟**
```sql
-- 查看主从延迟
SHOW SLAVE STATUS;
-- 查看 Seconds_Behind_Master
```

### 12. 读写分离和分库分表可以同时使用吗？

**答案：**

**可以同时使用。**

Sharding-JDBC 支持读写分离和分库分表的组合使用。

**配置示例：**

```yaml
spring:
  shardingsphere:
    datasource:
      names: master0,slave0_0,slave0_1,master1,slave1_0,slave1_1
      master0:
        url: jdbc:mysql://master0:3306/demo
      slave0_0:
        url: jdbc:mysql://slave0_0:3306/demo
      slave0_1:
        url: jdbc:mysql://slave0_1:3306/demo
      master1:
        url: jdbc:mysql://master1:3306/demo
      slave1_0:
        url: jdbc:mysql://slave1_0:3306/demo
      slave1_1:
        url: jdbc:mysql://slave1_1:3306/demo
    
    sharding:
      tables:
        t_order:
          actual-data-nodes: ds$->{0..1}.t_order_$->{0..1}
          database-strategy:
            inline:
              sharding-column: user_id
              algorithm-expression: ds$->{user_id % 2}
    
    # 读写分离配置
    masterslave:
      # 配置多个主从组
      - name: ms0
        master-data-source-name: master0
        slave-data-source-names: slave0_0,slave0_1
        load-balance-algorithm-type: round_robin
      - name: ms1
        master-data-source-name: master1
        slave-data-source-names: slave1_0,slave1_1
        load-balance-algorithm-type: round_robin
```

**使用场景：**
- 分库分表解决数据量大、性能问题
- 读写分离解决读压力大问题
- 两者结合可以同时解决读写性能问题

---

## 核心原理篇

### 13. Sharding-JDBC 的核心执行流程是什么？

**答案：**

Sharding-JDBC 的核心执行流程分为以下几个阶段：

**1. SQL 解析（SQL Parsing）**
- 将 SQL 解析为抽象语法树（AST）
- 识别 SQL 的各个组成部分（SELECT、FROM、WHERE 等）
- 提取表名、字段、条件等信息

**2. SQL 路由（SQL Routing）**
- 根据分片规则确定目标数据源和表
- 支持单路由、多路由、广播路由
- 考虑分片键、分片算法等因素

**3. SQL 改写（SQL Rewriting）**
- 将逻辑表名改写为物理表名
- 添加分片键条件
- 处理分页、排序等
- 生成可执行的 SQL

**4. SQL 执行（SQL Execution）**
- 在目标数据源上执行 SQL
- 支持并发执行
- 处理执行结果

**5. 结果归并（Result Merging）**
- 合并多个数据源的查询结果
- 支持流式归并、内存归并
- 处理排序、分页、聚合等

**流程图：**
```
原始 SQL
    ↓
SQL 解析 → AST
    ↓
SQL 路由 → 目标数据源
    ↓
SQL 改写 → 物理SQL
    ↓
SQL 执行 → 结果集
    ↓
结果归并 → 最终结果
```

### 14. Sharding-JDBC 如何实现 SQL 解析？

**答案：**

Sharding-JDBC 使用 ANTLR（Another Tool for Language Recognition）作为 SQL 解析器。

**SQL 解析过程：**

**1. 词法分析（Lexical Analysis）**
- 将 SQL 字符串分解为 Token 序列
- 识别关键字、标识符、运算符等

**2. 语法分析（Syntax Analysis）**
- 根据 Token 序列构建抽象语法树（AST）
- 验证 SQL 语法是否正确

**3. 语义分析（Semantic Analysis）**
- 提取表名、字段、条件等信息
- 识别分片键、分片算法等

**支持的 SQL 类型：**
- SELECT
- INSERT
- UPDATE
- DELETE
- DDL（CREATE、ALTER、DROP）

**解析器配置：**
```yaml
spring:
  shardingsphere:
    props:
      sql:
        show: true  # 显示解析后的 SQL
```

### 15. Sharding-JDBC 如何实现结果归并？

**答案：**

**结果归并（Result Merging）**是将多个数据源的查询结果合并为最终结果的过程。

**归并类型：**

**1. 流式归并（Stream Merging）**
- 边获取边归并，减少内存占用
- 适用于大数据量场景
- 支持 ORDER BY、LIMIT 等

**2. 内存归并（Memory Merging）**
- 将所有结果加载到内存后归并
- 适用于小数据量场景
- 支持 GROUP BY、聚合函数等

**归并策略：**

**1. 排序归并（Order By Merging）**
- 合并多个有序结果集
- 使用归并排序算法
- 支持多字段排序

**2. 分页归并（Limit Merging）**
- 处理分页查询
- 支持 OFFSET、LIMIT
- 避免全量查询

**3. 聚合归并（Aggregation Merging）**
- 合并聚合函数结果
- 支持 COUNT、SUM、AVG、MAX、MIN
- 处理 GROUP BY

**4. 分组归并（Group By Merging）**
- 合并分组结果
- 支持 GROUP BY
- 结合聚合归并

**归并配置：**
```yaml
spring:
  shardingsphere:
    props:
      executor:
        size: 16  # 执行线程池大小
```

### 16. Sharding-JDBC 如何处理分布式事务？

**答案：**

Sharding-JDBC 提供了多种分布式事务解决方案：

**1. 本地事务（Local Transaction）**
- 适用于单分片场景
- 使用数据库本地事务
- 无法保证跨分片事务一致性

**2. 两阶段提交（2PC）**
- 强一致性事务
- 性能较差，不推荐使用
- 需要事务管理器支持

**3. 柔性事务（Best Efforts Delivery）**
- 最终一致性
- 性能较好
- 适用于对一致性要求不高的场景

**4. TCC 事务（Try-Confirm-Cancel）**
- 应用层事务补偿
- 需要业务代码支持
- 性能和一致性平衡

**5. Saga 事务**
- 长事务解决方案
- 通过补偿操作保证一致性
- 适用于业务流程长的场景

**配置示例（柔性事务）：**

```yaml
spring:
  shardingsphere:
    props:
      transaction:
        type: BASE  # 使用柔性事务
```

**代码示例（使用 @Transactional）：**

```java
@Transactional
public void createOrder(Order order) {
    // 插入订单
    orderMapper.insert(order);
    
    // 扣减库存
    inventoryMapper.decrement(order.getProductId(), order.getQuantity());
    
    // 更新用户积分
    userMapper.addPoints(order.getUserId(), order.getAmount());
}
```

### 17. Sharding-JDBC 如何实现分布式主键生成？

**答案：**

Sharding-JDBC 提供了多种分布式主键生成策略：

**1. SNOWFLAKE（雪花算法）**
- Twitter 开源的分布式 ID 生成算法
- 生成 64 位长整型 ID
- 结构：时间戳（41位）+ 机器ID（10位）+ 序列号（12位）
- 优势：全局唯一、趋势递增、高性能

**配置示例：**
```yaml
key-generator:
  column: order_id
  type: SNOWFLAKE
  props:
    worker:
      id: 123  # 机器 ID
```

**2. UUID**
- 生成 32 位字符串
- 优势：简单、全局唯一
- 缺点：无序、占用空间大

**配置示例：**
```yaml
key-generator:
  column: order_id
  type: UUID
```

**3. LEAF（美团开源）**
- 基于 ZooKeeper 的分布式 ID 生成
- 支持号段模式、雪花模式
- 适合高并发场景

**4. 自定义主键生成**
- 实现 KeyGenerator 接口
- 根据业务需求自定义

**代码示例：**
```java
public class CustomKeyGenerator implements KeyGenerator {
    @Override
    public Comparable<?> generateKey() {
        // 自定义主键生成逻辑
        return System.currentTimeMillis();
    }
}
```

**配置示例：**
```yaml
key-generator:
  column: order_id
  type: CUSTOM
  props:
    custom:
      class-name: com.example.CustomKeyGenerator
```

---

## 性能优化篇

### 18. 如何优化 Sharding-JDBC 的性能？

**答案：**

**性能优化策略：**

**1. 合理选择分片键**
- 选择查询频率高的字段
- 确保数据分布均匀
- 避免跨分片查询

**2. 优化分片算法**
- 使用简单的取模算法
- 避免复杂的计算逻辑
- 考虑使用 Inline 表达式

**3. 使用绑定表**
- 避免跨库 JOIN
- 提高查询性能
- 配置示例：

```yaml
binding-tables:
  - t_order,t_order_item
```

**4. 使用广播表**
- 减少跨库查询
- 提高查询效率
- 配置示例：

```yaml
broadcast-tables:
  - t_config
  - t_dict
```

**5. 优化 SQL 语句**
- 避免使用 SELECT *
- 避免使用复杂的 JOIN
- 使用索引优化查询

**6. 调整连接池参数**
```yaml
datasource:
  ds0:
    maxPoolSize: 100
    minPoolSize: 10
    initialSize: 10
```

**7. 调整执行线程池**
```yaml
props:
  executor:
    size: 16  # 根据实际情况调整
```

**8. 启用 SQL 缓存**
```yaml
props:
  sql:
    show: true
  cache:
    enabled: true
```

**9. 使用读写分离**
- 读操作路由到从库
- 减轻主库压力
- 提高读性能

**10. 监控和调优**
- 使用 Sharding-Proxy 的监控功能
- 分析慢查询日志
- 定期优化分片策略

### 19. Sharding-JDBC 有哪些性能瓶颈？如何解决？

**答案：**

**常见性能瓶颈及解决方案：**

**1. 跨分片查询**
- **问题**：查询多个分片，性能下降
- **解决**：
  - 优化分片键选择
  - 使用绑定表
  - 避免跨分片 JOIN

**2. 结果归并**
- **问题**：大量数据归并占用内存
- **解决**：
  - 使用流式归并
  - 限制查询结果数量
  - 优化分页查询

**3. SQL 解析**
- **问题**：复杂 SQL 解析耗时
- **解决**：
  - 简化 SQL 语句
  - 使用预编译 SQL
  - 启用 SQL 缓存

**4. 网络开销**
- **问题**：多次网络请求增加延迟
- **解决**：
  - 使用连接池
  - 优化网络配置
  - 使用读写分离

**5. 主从延迟**
- **问题**：读操作可能读到旧数据
- **解决**：
  - 监控主从延迟
  - 强制读主库
  - 使用消息队列补偿

**6. 分片键选择不当**
- **问题**：数据倾斜，负载不均
- **解决**：
  - 重新选择分片键
  - 优化分片算法
  - 数据重新分布

### 20. 如何监控 Sharding-JDBC 的性能？

**答案：**

**监控方案：**

**1. SQL 日志监控**
```yaml
props:
  sql:
    show: true  # 显示 SQL 日志
```

**2. 慢查询监控**
- 分析慢查询日志
- 优化慢查询 SQL
- 添加合适的索引

**3. 使用 Sharding-Proxy 监控**
- Sharding-Proxy 提供监控接口
- 可以查看 SQL 执行情况
- 监控分片路由情况

**4. 集成 Prometheus + Grafana**
- 收集 Sharding-JDBC 指标
- 可视化监控
- 告警通知

**5. 使用 APM 工具**
- SkyWalking
- Pinpoint
- Zipkin

**6. 自定义监控**
- 实现监听器
- 记录 SQL 执行时间
- 统计分片路由情况

**代码示例（自定义监听器）：**
```java
public class ShardingExecuteListener implements ExecuteListener {
    @Override
    public void eventExecute(ExecuteEvent event) {
        long startTime = System.currentTimeMillis();
        // 记录开始时间
    }
    
    @Override
    public void eventExecuteSuccess(ExecuteEvent event) {
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        // 记录执行时间
    }
}
```

---

## 实战应用篇

### 21. 如何设计一个订单表的分片策略？

**答案：**

**订单表分片策略设计：**

**1. 分片键选择**
- **user_id**：按用户分片，便于查询用户的所有订单
- **order_id**：按订单 ID 分片，便于按订单查询
- **create_time**：按时间分片，便于按时间范围查询

**2. 分片策略**

**方案一：按用户 ID 分片**
```yaml
t_order:
  actual-data-nodes: ds$->{0..3}.t_order_$->{0..3}
  key-generator:
    column: order_id
    type: SNOWFLAKE
  database-strategy:
    inline:
      sharding-column: user_id
      algorithm-expression: ds$->{user_id % 4}
  table-strategy:
    inline:
      sharding-column: user_id
      algorithm-expression: t_order_$->{user_id % 4}
```

**优势**：
- 查询用户订单效率高
- 数据分布均匀
- 避免跨分片查询

**劣势**：
- 按订单 ID 查询需要扫描所有分片
- 按时间范围查询需要扫描所有分片

**方案二：按时间分片**
```yaml
t_order:
  actual-data-nodes: ds0.t_order_$->{2021..2023}${(1..12).collect{t -> t.toString().padLeft(2,'0')}}
  key-generator:
    column: order_id
    type: SNOWFLAKE
  table-strategy:
    standard:
      sharding-column: create_time
      precise-algorithm-class-name: com.example.DateShardingAlgorithm
```

**优势**：
- 按时间范围查询效率高
- 便于数据归档
- 便于历史数据清理

**劣势**：
- 按用户查询需要扫描多个分片
- 可能存在数据倾斜

**方案三：混合分片**
```yaml
t_order:
  actual-data-nodes: ds$->{0..3}.t_order_$->{2021..2023}${(1..12).collect{t -> t.toString().padLeft(2,'0')}}
  key-generator:
    column: order_id
    type: SNOWFLAKE
  database-strategy:
    inline:
      sharding-column: user_id
      algorithm-expression: ds$->{user_id % 4}
  table-strategy:
    standard:
      sharding-column: create_time
      precise-algorithm-class-name: com.example.DateShardingAlgorithm
```

**优势**：
- 兼顾用户查询和时间查询
- 数据分布均匀
- 灵活性高

**劣势**：
- 配置复杂
- 维护成本高

**3. 订单详情表分片**
```yaml
t_order_item:
  actual-data-nodes: ds$->{0..3}.t_order_item_$->{0..3}
  key-generator:
    column: item_id
    type: SNOWFLAKE
  database-strategy:
    inline:
      sharding-column: user_id
      algorithm-expression: ds$->{user_id % 4}
  table-strategy:
    inline:
      sharding-column: order_id
      algorithm-expression: t_order_item_$->{order_id % 4}

# 绑定表配置
binding-tables:
  - t_order,t_order_item
```

### 22. 如何设计一个用户表的分片策略？

**答案：**

**用户表分片策略设计：**

**1. 分片键选择**
- **user_id**：按用户 ID 分片，最常用
- **region_id**：按地区分片，便于按地区查询
- **register_time**：按注册时间分片，便于按时间查询

**2. 分片策略**

**方案一：按用户 ID 分片**
```yaml
t_user:
  actual-data-nodes: ds$->{0..3}.t_user_$->{0..3}
  key-generator:
    column: user_id
    type: SNOWFLAKE
  database-strategy:
    inline:
      sharding-column: user_id
      algorithm-expression: ds$->{user_id % 4}
  table-strategy:
    inline:
      sharding-column: user_id
      algorithm-expression: t_user_$->{user_id % 4}
```

**优势**：
- 查询用户效率高
- 数据分布均匀
- 配置简单

**方案二：按地区分片**
```yaml
t_user:
  actual-data-nodes: ds$->{0..3}.t_user_$->{0..3}
  key-generator:
    column: user_id
    type: SNOWFLAKE
  database-strategy:
    inline:
      sharding-column: region_id
      algorithm-expression: ds$->{region_id % 4}
  table-strategy:
    inline:
      sharding-column: user_id
      algorithm-expression: t_user_$->{user_id % 4}
```

**优势**：
- 按地区查询效率高
- 便于地区数据分析

**劣势**：
- 可能存在数据倾斜
- 某些地区用户过多

**3. 用户扩展表分片**
```yaml
t_user_profile:
  actual-data-nodes: ds$->{0..3}.t_user_profile_$->{0..3}
  key-generator:
    column: profile_id
    type: SNOWFLAKE
  database-strategy:
    inline:
      sharding-column: user_id
      algorithm-expression: ds$->{user_id % 4}
  table-strategy:
    inline:
      sharding-column: user_id
      algorithm-expression: t_user_profile_$->{user_id % 4}

# 绑定表配置
binding-tables:
  - t_user,t_user_profile
```

### 23. 如何处理分片后的数据迁移？

**答案：**

**数据迁移方案：**

**1. 在线迁移（Online Migration）**
- 使用双写方案
- 逐步切换流量
- 保证数据一致性

**步骤：**
1. 搭建新的分片环境
2. 应用同时写入新旧环境
3. 使用数据同步工具同步历史数据
4. 验证数据一致性
5. 逐步切换读流量
6. 下线旧环境

**2. 离线迁移（Offline Migration）**
- 停止应用
- 导出数据
- 数据转换
- 导入新环境
- 启动应用

**适用场景**：
- 数据量不大
- 可以接受停机

**3. 增量迁移（Incremental Migration）**
- 先迁移历史数据
- 再迁移增量数据
- 使用 binlog 同步

**工具推荐：**
- DataX
- Canal
- Maxwell
- Debezium

**代码示例（双写）：**
```java
public void insertUser(User user) {
    // 写入旧环境
    oldUserMapper.insert(user);
    
    // 写入新环境
    newUserMapper.insert(user);
}
```

**注意事项：**
- 确保数据一致性
- 监控迁移进度
- 准备回滚方案
- 测试验证

---

## 高级特性篇

### 24. Sharding-JDBC 支持哪些分布式 ID 生成算法？

**答案：**

Sharding-JDBC 支持以下分布式 ID 生成算法：

**1. SNOWFLAKE（雪花算法）**
- 64 位长整型 ID
- 结构：时间戳（41位）+ 机器ID（10位）+ 序列号（12位）
- 优势：全局唯一、趋势递增、高性能

**2. UUID**
- 32 位字符串 ID
- 优势：简单、全局唯一
- 缺点：无序、占用空间大

**3. LEAF（美团开源）**
- 基于 ZooKeeper 的分布式 ID 生成
- 支持号段模式、雪花模式
- 适合高并发场景

**4. 自定义 ID 生成**
- 实现 KeyGenerator 接口
- 根据业务需求自定义

**配置示例：**
```yaml
key-generator:
  column: order_id
  type: SNOWFLAKE
  props:
    worker:
      id: 123
```

### 25. 如何实现 Sharding-JDBC 的动态分片？

**答案：**

**动态分片**是指根据业务需求动态调整分片策略。

**实现方案：**

**1. 使用配置中心**
- 将分片配置存储在配置中心
- 动态加载配置
- 支持配置热更新

**2. 使用数据库存储配置**
- 将分片规则存储在数据库
- 应用启动时加载配置
- 支持动态修改

**3. 使用 Sharding-Proxy**
- Sharding-Proxy 支持动态配置
- 通过管理接口修改配置
- 无需重启应用

**代码示例（动态加载配置）：**
```java
@Configuration
public class DynamicShardingConfig {
    
    @Autowired
    private DataSource dataSource;
    
    @Bean
    public DataSource shardingDataSource() {
        // 从配置中心加载配置
        ShardingRuleConfiguration shardingRuleConfig = loadShardingConfig();
        
        // 创建 ShardingDataSource
        return ShardingDataSourceFactory.createDataSource(dataSource, shardingRuleConfig, new Properties());
    }
    
    private ShardingRuleConfiguration loadShardingConfig() {
        // 从配置中心加载配置
        ShardingRuleConfiguration config = new ShardingRuleConfiguration();
        // ... 配置分片规则
        return config;
    }
}
```

### 26. Sharding-JDBC 如何支持多租户？

**答案：**

**多租户方案：**

**1. 基于分片的多租户**
- 按租户 ID 分片
- 每个租户独立的数据源或表
- 数据隔离性好

**配置示例：**
```yaml
t_order:
  actual-data-nodes: ds$->{0..9}.t_order
  database-strategy:
    inline:
      sharding-column: tenant_id
      algorithm-expression: ds$->{tenant_id % 10}
```

**2. 基于字段的多租户**
- 在表中添加租户 ID 字段
- 查询时自动过滤租户数据
- 配置简单

**代码示例（租户过滤器）：**
```java
@Intercepts({
    @Signature(type = StatementHandler.class, method = "prepare", args = {Connection.class, Integer.class})
})
public class TenantInterceptor implements Interceptor {
    
    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        StatementHandler statementHandler = (StatementHandler) invocation.getTarget();
        BoundSql boundSql = statementHandler.getBoundSql();
        String sql = boundSql.getSql();
        
        // 添加租户过滤条件
        sql = addTenantCondition(sql);
        
        // 修改 SQL
        Field field = boundSql.getClass().getDeclaredField("sql");
        field.setAccessible(true);
        field.set(boundSql, sql);
        
        return invocation.proceed();
    }
    
    private String addTenantCondition(String sql) {
        Long tenantId = TenantContext.getTenantId();
        return sql + " AND tenant_id = " + tenantId;
    }
}
```

**3. 基于数据源的多租户**
- 每个租户独立的数据源
- 完全隔离
- 成本较高

**配置示例：**
```yaml
spring:
  shardingsphere:
    datasource:
      names: tenant1,tenant2,tenant3
      tenant1:
        url: jdbc:mysql://localhost:3306/tenant1
      tenant2:
        url: jdbc:mysql://localhost:3306/tenant2
      tenant3:
        url: jdbc:mysql://localhost:3306/tenant3
```

---

## 常见问题篇

### 27. Sharding-JDBC 不支持哪些 SQL 操作？

**答案：**

**不支持的 SQL 操作：**

**1. 跨分片的 JOIN**
- 不支持跨分片的表 JOIN
- 解决方案：
  - 使用绑定表
  - 在应用层组装数据
  - 使用冗余字段

**2. 复杂的子查询**
- 不支持复杂的子查询
- 解决方案：
  - 拆分为多个查询
  - 在应用层处理

**3. 跨分片的事务**
- 不支持强一致性分布式事务
- 解决方案：
  - 使用柔性事务
  - 使用 TCC 事务
  - 使用 Saga 事务

**4. 某些聚合函数**
- 不支持某些聚合函数
- 解决方案：
  - 在应用层计算
  - 使用流式归并

**5. 存储过程和函数**
- 不支持存储过程和函数
- 解决方案：
  - 改用应用层逻辑
  - 使用自定义函数

**6. 触发器**
- 不支持触发器
- 解决方案：
  - 使用消息队列
  - 使用应用层逻辑

### 28. 如何排查 Sharding-JDBC 的问题？

**答案：**

**问题排查步骤：**

**1. 启用 SQL 日志**
```yaml
props:
  sql:
    show: true
```

**2. 检查分片配置**
- 检查数据源配置
- 检查分片规则配置
- 检查分片算法配置

**3. 检查 SQL 语句**
- 检查 SQL 语法
- 检查分片键是否正确
- 检查是否使用不支持的 SQL

**4. 检查数据源连接**
- 检查数据库连接是否正常
- 检查连接池配置
- 检查网络连接

**5. 检查日志**
- 查看应用日志
- 查看数据库日志
- 查看系统日志

**6. 使用调试工具**
- 使用 IDE 调试
- 使用 Sharding-Proxy 监控
- 使用 APM 工具

**常见问题及解决方案：**

**问题1：数据路由错误**
- 检查分片键是否正确
- 检查分片算法是否正确
- 检查分片规则配置

**问题2：查询结果不正确**
- 检查结果归并配置
- 检查 SQL 语句是否正确
- 检查数据是否一致

**问题3：性能问题**
- 检查是否存在跨分片查询
- 检查连接池配置
- 检查分片算法是否优化

**问题4：事务问题**
- 检查事务配置
- 检查是否使用分布式事务
- 检查事务传播行为

### 29. Sharding-JDBC 与 Mycat 的区别是什么？

**答案：**

| 特性 | Sharding-JDBC | Mycat |
|------|---------------|-------|
| **架构** | 客户端分片 | 服务端分片 |
| **部署方式** | Jar 包嵌入应用 | 独立部署服务 |
| **性能** | 高（直连数据库） | 中（通过 Proxy） |
| **复杂度** | 低 | 中 |
| **语言支持** | Java | 多语言 |
| **功能** | 分库分表、读写分离 | 分库分表、读写分离、数据迁移 |
| **监控** | 基础监控 | 丰富的监控 |
| **管理** | 配置文件 | Web 管理界面 |

**选择建议：**
- 纯 Java 应用：选择 Sharding-JDBC
- 多语言混合应用：选择 Mycat
- 需要数据迁移：选择 Mycat
- 需要丰富的管理功能：选择 Mycat

### 30. Sharding-JDBC 的未来发展趋势是什么？

**答案：**

**发展趋势：**

**1. 云原生**
- 支持 Kubernetes
- 支持 Service Mesh
- 支持 Serverless

**2. 多语言支持**
- 支持 Go、Python 等语言
- 统一的 SQL 解析引擎
- 统一的分片规则

**3. 智能化**
- 智能分片策略
- 自动性能优化
- 自动故障转移

**4. 生态完善**
- 丰富的插件
- 完善的监控
- 完善的文档

**5. 性能提升**
- 优化 SQL 解析
- 优化结果归并
- 优化并发执行

**6. 功能增强**
- 支持更多数据库
- 支持更多 SQL 操作
- 支持更多分布式事务

---

## 总结

Sharding-JDBC 是一个功能强大、性能优异的数据库分片中间件，适用于各种分库分表场景。掌握 Sharding-JDBC 的核心概念、原理和最佳实践，对于解决数据库性能瓶颈、提高系统可扩展性具有重要意义。

**学习建议：**
1. 理解核心概念和原理
2. 熟悉配置和使用方法
3. 掌握性能优化技巧
4. 了解常见问题和解决方案
5. 关注最新发展和趋势

**实践建议：**
1. 从简单场景开始实践
2. 逐步深入复杂场景
3. 关注性能监控和优化
4. 积累实战经验
5. 参与社区交流和贡献

---

## 实战场景篇

### 31. 商品价格表分表实战（3000万+数据，不停服迁移）

**场景描述：**

某电商平台商品价格系统，商品价格表（product_price）已有 3000 万+ 数据，单表查询性能下降严重，需要使用 Sharding-JDBC 进行分表优化。要求：

1. 价格系统不能停服，需要在线迁移
2. 查询场景：按商品 ID 查询价格、按 SKU ID 查询价格、按价格范围查询
3. 写入场景：价格更新频繁，需要保证数据一致性
4. 历史数据查询：需要查询历史价格记录

**表结构：**

```sql
CREATE TABLE product_price (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    product_id BIGINT NOT NULL COMMENT '商品ID',
    sku_id BIGINT NOT NULL COMMENT 'SKU ID',
    price DECIMAL(10,2) NOT NULL COMMENT '价格',
    original_price DECIMAL(10,2) COMMENT '原价',
    cost_price DECIMAL(10,2) COMMENT '成本价',
    start_time DATETIME COMMENT '生效开始时间',
    end_time DATETIME COMMENT '生效结束时间',
    status TINYINT DEFAULT 1 COMMENT '状态：1-有效，0-无效',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_product_id (product_id),
    INDEX idx_sku_id (sku_id),
    INDEX idx_price (price),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品价格表';
```

**答案：**

#### 一、分表方案设计

**1. 分片策略选择**

根据业务场景分析：

- **主要查询场景**：按商品 ID 查询、按 SKU ID 查询（高频）
- **次要查询场景**：按价格范围查询（中频）
- **写入场景**：价格更新频繁

**推荐分片策略：**

**方案一：按商品 ID 分片（推荐）**

```yaml
spring:
  shardingsphere:
    datasource:
      names: ds0,ds1,ds2,ds3
      ds0:
        type: com.alibaba.druid.pool.DruidDataSource
        driver-class-name: com.mysql.cj.jdbc.Driver
        url: jdbc:mysql://192.168.1.10:3306/price_db_0
        username: root
        password: 123456
        maxPoolSize: 50
        minPoolSize: 10
      ds1:
        type: com.alibaba.druid.pool.DruidDataSource
        driver-class-name: com.mysql.cj.jdbc.Driver
        url: jdbc:mysql://192.168.1.11:3306/price_db_1
        username: root
        password: 123456
        maxPoolSize: 50
        minPoolSize: 10
      ds2:
        type: com.alibaba.druid.pool.DruidDataSource
        driver-class-name: com.mysql.cj.jdbc.Driver
        url: jdbc:mysql://192.168.1.12:3306/price_db_2
        username: root
        password: 123456
        maxPoolSize: 50
        minPoolSize: 10
      ds3:
        type: com.alibaba.druid.pool.DruidDataSource
        driver-class-name: com.mysql.cj.jdbc.Driver
        url: jdbc:mysql://192.168.1.13:3306/price_db_3
        username: root
        password: 123456
        maxPoolSize: 50
        minPoolSize: 10
    
    sharding:
      default-data-source-name: ds0
      
      tables:
        product_price:
          # 数据节点：4个数据库，每个数据库8张表，共32张表
          actual-data-nodes: ds$->{0..3}.product_price_$->{0..7}
          
          # 主键生成策略
          key-generator:
            column: id
            type: SNOWFLAKE
            props:
              worker:
                id: 1
          
          # 分库策略：按商品 ID 取模
          database-strategy:
            inline:
              sharding-column: product_id
              algorithm-expression: ds$->{product_id % 4}
          
          # 分表策略：按商品 ID 取模
          table-strategy:
            inline:
              sharding-column: product_id
              algorithm-expression: product_price_$->{product_id % 8}
      
      # 绑定表：如果有商品表，可以配置绑定表
      binding-tables:
        - product,product_price
      
      # 广播表：配置表、字典表
      broadcast-tables:
        - product_config
        - price_config
      
      # 默认分片策略
      default-table-strategy:
        none:
      
      # 默认数据库策略
      default-database-strategy:
        none
    
    props:
      sql:
        show: true
      executor:
        size: 20
```

**方案二：按时间分片（适用于历史数据归档）**

```yaml
tables:
  product_price:
    # 按年月分表
    actual-data-nodes: ds0.product_price_$->{2023..2025}${(1..12).collect{t -> t.toString().padLeft(2,'0')}}
    
    key-generator:
      column: id
      type: SNOWFLAKE
    
    # 分表策略：按创建时间分片
    table-strategy:
      standard:
        sharding-column: create_time
        precise-algorithm-class-name: com.example.DateShardingAlgorithm
```

**方案三：混合分片（分库 + 分表）**

```yaml
tables:
  product_price:
    # 4个数据库，每个数据库按年月分表
    actual-data-nodes: ds$->{0..3}.product_price_$->{2023..2025}${(1..12).collect{t -> t.toString().padLeft(2,'0')}}
    
    key-generator:
      column: id
      type: SNOWFLAKE
    
    # 分库策略：按商品 ID 取模
    database-strategy:
      inline:
        sharding-column: product_id
        algorithm-expression: ds$->{product_id % 4}
    
    # 分表策略：按时间分片
    table-strategy:
      standard:
        sharding-column: create_time
        precise-algorithm-class-name: com.example.DateShardingAlgorithm
```

**推荐方案：方案一（按商品 ID 分片）**

**理由：**
1. 按商品 ID 查询是最常见的场景，性能最优
2. 数据分布均匀，避免数据倾斜
3. 配置简单，维护成本低
4. 适合价格更新频繁的场景

**2. 分片算法实现**

**商品 ID 分片算法（使用 Inline 表达式即可）：**

```yaml
# 分库算法
algorithm-expression: ds$->{product_id % 4}

# 分表算法
algorithm-expression: product_price_$->{product_id % 8}
```

**如果需要自定义算法：**

```java
public class ProductPriceShardingAlgorithm implements PreciseShardingAlgorithm<Long> {
    
    @Override
    public String doSharding(Collection<String> availableTargetNames, 
                           PreciseShardingValue<Long> shardingValue) {
        Long productId = shardingValue.getValue();
        
        // 计算分片索引
        int tableIndex = (int) (productId % 8);
        
        // 返回表名
        return "product_price_" + tableIndex;
    }
}
```

**3. 代码实现**

**实体类：**

```java
@Data
@TableName("product_price")
public class ProductPrice {
    
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    
    private Long productId;
    private Long skuId;
    private BigDecimal price;
    private BigDecimal originalPrice;
    private BigDecimal costPrice;
    private Date startTime;
    private Date endTime;
    private Integer status;
    private Date createTime;
    private Date updateTime;
}
```

**Mapper 接口：**

```java
@Mapper
public interface ProductPriceMapper {
    
    @Insert("INSERT INTO product_price (product_id, sku_id, price, original_price, cost_price, start_time, end_time, status) " +
            "VALUES (#{productId}, #{skuId}, #{price}, #{originalPrice}, #{costPrice}, #{startTime}, #{endTime}, #{status})")
    @Options(useGeneratedKeys = true, keyColumn = "id", keyProperty = "id")
    int insert(ProductPrice productPrice);
    
    @Update("UPDATE product_price SET price = #{price}, original_price = #{originalPrice}, " +
            "cost_price = #{costPrice}, start_time = #{startTime}, end_time = #{endTime}, " +
            "status = #{status}, update_time = NOW() WHERE id = #{id}")
    int update(ProductPrice productPrice);
    
    @Select("SELECT * FROM product_price WHERE product_id = #{productId} AND status = 1 ORDER BY create_time DESC LIMIT 1")
    ProductPrice selectByProductId(Long productId);
    
    @Select("SELECT * FROM product_price WHERE sku_id = #{skuId} AND status = 1 ORDER BY create_time DESC LIMIT 1")
    ProductPrice selectBySkuId(Long skuId);
    
    @Select("SELECT * FROM product_price WHERE product_id = #{productId} AND price BETWEEN #{minPrice} AND #{maxPrice} AND status = 1")
    List<ProductPrice> selectByPriceRange(@Param("productId") Long productId, 
                                          @Param("minPrice") BigDecimal minPrice, 
                                          @Param("maxPrice") BigDecimal maxPrice);
    
    @Select("SELECT * FROM product_price WHERE product_id = #{productId} ORDER BY create_time DESC LIMIT #{limit}")
    List<ProductPrice> selectHistoryByProductId(@Param("productId") Long productId, @Param("limit") int limit);
}
```

**Service 层：**

```java
@Service
public class ProductPriceService {
    
    @Autowired
    private ProductPriceMapper productPriceMapper;
    
    @Autowired
    private ProductPriceHistoryMapper historyMapper;
    
    /**
     * 添加价格
     */
    @Transactional
    public void addPrice(ProductPrice productPrice) {
        productPrice.setCreateTime(new Date());
        productPrice.setUpdateTime(new Date());
        productPrice.setStatus(1);
        
        // 插入当前价格
        productPriceMapper.insert(productPrice);
        
        // 同时插入历史价格记录
        ProductPriceHistory history = new ProductPriceHistory();
        BeanUtils.copyProperties(productPrice, history);
        historyMapper.insert(history);
    }
    
    /**
     * 更新价格
     */
    @Transactional
    public void updatePrice(ProductPrice productPrice) {
        // 先查询旧价格
        ProductPrice oldPrice = productPriceMapper.selectById(productPrice.getId());
        
        // 更新当前价格
        productPrice.setUpdateTime(new Date());
        productPriceMapper.update(productPrice);
        
        // 保存历史价格
        ProductPriceHistory history = new ProductPriceHistory();
        BeanUtils.copyProperties(oldPrice, history);
        historyMapper.insert(history);
    }
    
    /**
     * 查询商品当前价格
     */
    public ProductPrice getCurrentPrice(Long productId) {
        return productPriceMapper.selectByProductId(productId);
    }
    
    /**
     * 查询 SKU 当前价格
     */
    public ProductPrice getSkuPrice(Long skuId) {
        return productPriceMapper.selectBySkuId(skuId);
    }
    
    /**
     * 按价格范围查询
     */
    public List<ProductPrice> queryByPriceRange(Long productId, BigDecimal minPrice, BigDecimal maxPrice) {
        return productPriceMapper.selectByPriceRange(productId, minPrice, maxPrice);
    }
    
    /**
     * 查询历史价格
     */
    public List<ProductPrice> queryHistoryPrice(Long productId, int limit) {
        return productPriceMapper.selectHistoryByProductId(productId, limit);
    }
}
```

#### 二、历史数据迁移方案（不停服）

**迁移方案：双写 + 增量同步 + 数据校验 + 流量切换**

**阶段一：准备阶段（1-2天）**

1. **创建新表结构**

```sql
-- 在新的数据库中创建分片表
-- ds0 数据库
CREATE TABLE product_price_0 LIKE product_price;
CREATE TABLE product_price_1 LIKE product_price;
CREATE TABLE product_price_2 LIKE product_price;
CREATE TABLE product_price_3 LIKE product_price;
CREATE TABLE product_price_4 LIKE product_price;
CREATE TABLE product_price_5 LIKE product_price;
CREATE TABLE product_price_6 LIKE product_price;
CREATE TABLE product_price_7 LIKE product_price;

-- ds1、ds2、ds3 数据库执行相同操作
```

2. **创建历史价格表**

```sql
CREATE TABLE product_price_history (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    price_id BIGINT NOT NULL COMMENT '原价格表ID',
    product_id BIGINT NOT NULL COMMENT '商品ID',
    sku_id BIGINT NOT NULL COMMENT 'SKU ID',
    price DECIMAL(10,2) NOT NULL COMMENT '价格',
    original_price DECIMAL(10,2) COMMENT '原价',
    cost_price DECIMAL(10,2) COMMENT '成本价',
    start_time DATETIME COMMENT '生效开始时间',
    end_time DATETIME COMMENT '生效结束时间',
    status TINYINT DEFAULT 1 COMMENT '状态',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_product_id (product_id),
    INDEX idx_sku_id (sku_id),
    INDEX idx_price_id (price_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品价格历史表';
```

3. **配置 Sharding-JDBC**

```yaml
# application-sharding.yml
spring:
  shardingsphere:
    datasource:
      names: old-ds,ds0,ds1,ds2,ds3
      old-ds:
        type: com.alibaba.druid.pool.DruidDataSource
        driver-class-name: com.mysql.cj.jdbc.Driver
        url: jdbc:mysql://old-db:3306/price_db
        username: root
        password: 123456
      # ds0-ds3 配置如上
    
    sharding:
      default-data-source-name: old-ds
      
      tables:
        product_price:
          actual-data-nodes: ds$->{0..3}.product_price_$->{0..7}
          key-generator:
            column: id
            type: SNOWFLAKE
          database-strategy:
            inline:
              sharding-column: product_id
              algorithm-expression: ds$->{product_id % 4}
          table-strategy:
            inline:
              sharding-column: product_id
              algorithm-expression: product_price_$->{product_id % 8}
```

**阶段二：双写阶段（3-5天）**

1. **实现双写逻辑**

```java
@Service
public class ProductPriceMigrationService {
    
    @Autowired
    private ProductPriceMapper oldPriceMapper;
    
    @Autowired
    private ProductPriceMapper newPriceMapper;
    
    @Autowired
    private ProductPriceHistoryMapper historyMapper;
    
    /**
     * 双写：同时写入旧表和新表
     */
    @Transactional
    public void insertWithDualWrite(ProductPrice productPrice) {
        productPrice.setCreateTime(new Date());
        productPrice.setUpdateTime(new Date());
        productPrice.setStatus(1);
        
        // 写入旧表
        oldPriceMapper.insert(productPrice);
        
        // 写入新表（分片表）
        newPriceMapper.insert(productPrice);
        
        // 写入历史表
        ProductPriceHistory history = new ProductPriceHistory();
        BeanUtils.copyProperties(productPrice, history);
        historyMapper.insert(history);
    }
    
    /**
     * 双写：同时更新旧表和新表
     */
    @Transactional
    public void updateWithDualWrite(ProductPrice productPrice) {
        // 更新旧表
        oldPriceMapper.update(productPrice);
        
        // 更新新表
        newPriceMapper.update(productPrice);
        
        // 保存历史记录
        ProductPriceHistory history = new ProductPriceHistory();
        BeanUtils.copyProperties(productPrice, history);
        historyMapper.insert(history);
    }
}
```

2. **配置双写开关**

```yaml
migration:
  dual-write:
    enabled: true
    old-table-write: true
    new-table-write: true
  read:
    source: old  # old: 读旧表, new: 读新表, both: 双读对比
```

3. **实现读写分离逻辑**

```java
@Service
public class ProductPriceReadService {
    
    @Value("${migration.read.source:old}")
    private String readSource;
    
    @Autowired
    private ProductPriceMapper oldPriceMapper;
    
    @Autowired
    private ProductPriceMapper newPriceMapper;
    
    public ProductPrice selectByProductId(Long productId) {
        if ("old".equals(readSource)) {
            return oldPriceMapper.selectByProductId(productId);
        } else if ("new".equals(readSource)) {
            return newPriceMapper.selectByProductId(productId);
        } else {
            // 双读对比
            ProductPrice oldPrice = oldPriceMapper.selectByProductId(productId);
            ProductPrice newPrice = newPriceMapper.selectByProductId(productId);
            
            // 数据校验
            if (!Objects.equals(oldPrice, newPrice)) {
                log.error("数据不一致！productId: {}, oldPrice: {}, newPrice: {}", 
                         productId, oldPrice, newPrice);
                // 发送告警
                alertService.sendAlert("数据不一致", productId);
            }
            
            return newPrice != null ? newPrice : oldPrice;
        }
    }
}
```

**阶段三：历史数据迁移（5-7天）**

1. **全量数据迁移脚本**

```java
@Component
public class ProductPriceMigrationJob {
    
    @Autowired
    private ProductPriceMapper oldPriceMapper;
    
    @Autowired
    private ProductPriceMapper newPriceMapper;
    
    @Autowired
    private ProductPriceHistoryMapper historyMapper;
    
    private static final int BATCH_SIZE = 1000;
    private static final int THREAD_COUNT = 4;
    
    /**
     * 全量数据迁移
     */
    public void migrateAllData() {
        long totalCount = oldPriceMapper.count();
        long migratedCount = 0;
        
        log.info("开始全量数据迁移，总数据量：{}", totalCount);
        
        // 分页查询旧表数据
        int pageNum = 1;
        int pageSize = BATCH_SIZE;
        
        while (true) {
            List<ProductPrice> priceList = oldPriceMapper.selectByPage(pageNum, pageSize);
            
            if (priceList.isEmpty()) {
                break;
            }
            
            // 批量插入新表
            for (ProductPrice price : priceList) {
                try {
                    // 插入新表
                    newPriceMapper.insert(price);
                    
                    // 插入历史表
                    ProductPriceHistory history = new ProductPriceHistory();
                    BeanUtils.copyProperties(price, history);
                    historyMapper.insert(history);
                    
                    migratedCount++;
                    
                } catch (Exception e) {
                    log.error("数据迁移失败，id: {}", price.getId(), e);
                    // 记录失败数据，后续重试
                    saveFailedRecord(price);
                }
            }
            
            log.info("已迁移：{}/{}", migratedCount, totalCount);
            pageNum++;
        }
        
        log.info("全量数据迁移完成，共迁移：{} 条", migratedCount);
    }
    
    /**
     * 多线程并发迁移
     */
    public void migrateDataConcurrently() {
        long totalCount = oldPriceMapper.count();
        long pageSize = BATCH_SIZE;
        long totalPages = (totalCount + pageSize - 1) / pageSize;
        
        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
        
        for (long i = 0; i < totalPages; i++) {
            final long pageNum = i + 1;
            executor.submit(() -> {
                List<ProductPrice> priceList = oldPriceMapper.selectByPage((int) pageNum, (int) pageSize);
                
                for (ProductPrice price : priceList) {
                    try {
                        newPriceMapper.insert(price);
                        
                        ProductPriceHistory history = new ProductPriceHistory();
                        BeanUtils.copyProperties(price, history);
                        historyMapper.insert(history);
                        
                    } catch (Exception e) {
                        log.error("数据迁移失败，id: {}", price.getId(), e);
                        saveFailedRecord(price);
                    }
                }
            });
        }
        
        executor.shutdown();
        try {
            executor.awaitTermination(1, TimeUnit.DAYS);
        } catch (InterruptedException e) {
            log.error("数据迁移被中断", e);
        }
    }
}
```

2. **增量数据同步（基于 Binlog）**

使用 Canal 或 Maxwell 监听旧表的 Binlog，实时同步到新表：

```java
@Component
public class CanalSyncListener {
    
    @Autowired
    private ProductPriceMapper newPriceMapper;
    
    @Autowired
    private ProductPriceHistoryMapper historyMapper;
    
    @KafkaListener(topics = "canal-product-price")
    public void handleCanalMessage(String message) {
        CanalEntry.Entry entry = JSON.parseObject(message, CanalEntry.Entry.class);
        
        if (entry.getEntryType() == CanalEntry.EntryType.ROWDATA) {
            CanalEntry.RowChange rowChange = CanalEntry.RowChange.parseFrom(entry.getStoreValue());
            
            for (CanalEntry.RowData rowData : rowChange.getRowDatasList()) {
                if (rowChange.getEventType() == CanalEntry.EventType.INSERT) {
                    handleInsert(rowData.getAfterColumnsList());
                } else if (rowChange.getEventType() == CanalEntry.EventType.UPDATE) {
                    handleUpdate(rowData.getAfterColumnsList());
                } else if (rowChange.getEventType() == CanalEntry.EventType.DELETE) {
                    handleDelete(rowData.getBeforeColumnsList());
                }
            }
        }
    }
    
    private void handleInsert(List<CanalEntry.Column> columns) {
        ProductPrice price = convertToProductPrice(columns);
        newPriceMapper.insert(price);
        
        ProductPriceHistory history = new ProductPriceHistory();
        BeanUtils.copyProperties(price, history);
        historyMapper.insert(history);
    }
    
    private void handleUpdate(List<CanalEntry.Column> columns) {
        ProductPrice price = convertToProductPrice(columns);
        newPriceMapper.update(price);
        
        ProductPriceHistory history = new ProductPriceHistory();
        BeanUtils.copyProperties(price, history);
        historyMapper.insert(history);
    }
    
    private void handleDelete(List<CanalEntry.Column> columns) {
        Long id = Long.valueOf(getColumnValue(columns, "id"));
        newPriceMapper.deleteById(id);
    }
    
    private ProductPrice convertToProductPrice(List<CanalEntry.Column> columns) {
        ProductPrice price = new ProductPrice();
        price.setId(Long.valueOf(getColumnValue(columns, "id")));
        price.setProductId(Long.valueOf(getColumnValue(columns, "product_id")));
        price.setSkuId(Long.valueOf(getColumnValue(columns, "sku_id")));
        price.setPrice(new BigDecimal(getColumnValue(columns, "price")));
        // ... 其他字段
        return price;
    }
    
    private String getColumnValue(List<CanalEntry.Column> columns, String name) {
        return columns.stream()
                .filter(c -> c.getName().equals(name))
                .findFirst()
                .map(CanalEntry.Column::getValue)
                .orElse(null);
    }
}
```

**阶段四：数据校验（2-3天）**

1. **数据一致性校验**

```java
@Component
public class DataConsistencyChecker {
    
    @Autowired
    private ProductPriceMapper oldPriceMapper;
    
    @Autowired
    private ProductPriceMapper newPriceMapper;
    
    /**
     * 全量数据校验
     */
    public void checkAllData() {
        long totalCount = oldPriceMapper.count();
        long checkedCount = 0;
        long errorCount = 0;
        
        int pageNum = 1;
        int pageSize = 1000;
        
        while (true) {
            List<ProductPrice> oldPriceList = oldPriceMapper.selectByPage(pageNum, pageSize);
            
            if (oldPriceList.isEmpty()) {
                break;
            }
            
            for (ProductPrice oldPrice : oldPriceList) {
                ProductPrice newPrice = newPriceMapper.selectById(oldPrice.getId());
                
                if (newPrice == null) {
                    log.error("数据缺失！id: {}", oldPrice.getId());
                    errorCount++;
                    saveErrorRecord(oldPrice, "数据缺失");
                } else if (!isDataEqual(oldPrice, newPrice)) {
                    log.error("数据不一致！id: {}, oldPrice: {}, newPrice: {}", 
                             oldPrice.getId(), oldPrice, newPrice);
                    errorCount++;
                    saveErrorRecord(oldPrice, "数据不一致");
                }
                
                checkedCount++;
            }
            
            log.info("已校验：{}/{}，错误数：{}", checkedCount, totalCount, errorCount);
            pageNum++;
        }
        
        log.info("数据校验完成，共校验：{} 条，错误数：{}", checkedCount, errorCount);
    }
    
    /**
     * 抽样校验
     */
    public void randomCheck() {
        Random random = new Random();
        int sampleSize = 10000;
        
        for (int i = 0; i < sampleSize; i++) {
            Long id = (long) random.nextInt(30000000);
            
            ProductPrice oldPrice = oldPriceMapper.selectById(id);
            ProductPrice newPrice = newPriceMapper.selectById(id);
            
            if (oldPrice != null && newPrice != null) {
                if (!isDataEqual(oldPrice, newPrice)) {
                    log.error("抽样校验失败！id: {}", id);
                }
            }
        }
    }
    
    private boolean isDataEqual(ProductPrice oldPrice, ProductPrice newPrice) {
        return Objects.equals(oldPrice.getProductId(), newPrice.getProductId()) &&
               Objects.equals(oldPrice.getSkuId(), newPrice.getSkuId()) &&
               Objects.equals(oldPrice.getPrice(), newPrice.getPrice()) &&
               Objects.equals(oldPrice.getOriginalPrice(), newPrice.getOriginalPrice()) &&
               Objects.equals(oldPrice.getCostPrice(), newPrice.getCostPrice()) &&
               Objects.equals(oldPrice.getStatus(), newPrice.getStatus());
    }
}
```

**阶段五：流量切换（1-2天）**

1. **灰度切换**

```java
@RestController
@RequestMapping("/migration")
public class MigrationController {
    
    @Value("${migration.read.source:old}")
    private String readSource;
    
    @Autowired
    private ProductPriceReadService readService;
    
    /**
     * 切换读流量
     */
    @PostMapping("/switch-read")
    public String switchReadSource(@RequestParam String source) {
        if (!Arrays.asList("old", "new", "both").contains(source)) {
            return "error: invalid source";
        }
        
        // 更新配置
        readSource = source;
        
        // 记录切换日志
        log.info("读流量切换：{}", source);
        
        return "success";
    }
    
    /**
     * 获取当前读源
     */
    @GetMapping("/read-source")
    public String getReadSource() {
        return readSource;
    }
}
```

2. **切换步骤**

**Day 1：**
- 切换 10% 读流量到新表
- 监控错误率和性能指标
- 如有问题，立即回滚

**Day 2：**
- 切换 50% 读流量到新表
- 继续监控

**Day 3：**
- 切换 100% 读流量到新表
- 全面监控

**Day 4：**
- 停止双写，只写新表
- 验证数据一致性

**Day 5：**
- 下线旧表
- 清理旧数据

**阶段六：监控与告警**

1. **监控指标**

```java
@Component
public class MigrationMonitor {
    
    @Autowired
    private MeterRegistry meterRegistry;
    
    /**
     * 记录迁移进度
     */
    public void recordMigrationProgress(long totalCount, long migratedCount) {
        double progress = (double) migratedCount / totalCount * 100;
        meterRegistry.gauge("migration.progress", progress);
    }
    
    /**
     * 记录数据不一致数
     */
    public void recordInconsistency(long count) {
        meterRegistry.counter("migration.inconsistency").increment(count);
    }
    
    /**
     * 记录查询延迟
     */
    public void recordQueryLatency(long latency) {
        meterRegistry.timer("migration.query.latency").record(latency, TimeUnit.MILLISECONDS);
    }
}
```

2. **告警配置**

```yaml
# Prometheus 告警规则
groups:
  - name: migration_alerts
    rules:
      - alert: MigrationProgressStuck
        expr: rate(migration_progress[5m]) == 0
        for: 10m
        labels:
          severity: warning
        annotations:
          summary: "迁移进度停滞"
          description: "迁移进度在过去10分钟内没有变化"
      
      - alert: DataInconsistencyHigh
        expr: rate(migration_inconsistency_total[5m]) > 100
        for: 5m
        labels:
          severity: critical
        annotations:
          summary: "数据不一致率高"
          description: "数据不一致率超过阈值"
      
      - alert: QueryLatencyHigh
        expr: histogram_quantile(0.95, migration_query_latency_seconds) > 1
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "查询延迟高"
          description: "95%的查询延迟超过1秒"
```

#### 三、迁移时间表

| 阶段 | 任务 | 时间 | 负责人 |
|------|------|------|--------|
| 准备阶段 | 创建新表结构、配置 Sharding-JDBC | 1-2天 | DBA + 开发 |
| 双写阶段 | 实现双写逻辑、测试验证 | 3-5天 | 开发 + 测试 |
| 数据迁移 | 全量迁移 + 增量同步 | 5-7天 | 开发 + 运维 |
| 数据校验 | 全量校验 + 抽样校验 | 2-3天 | 开发 + 测试 |
| 流量切换 | 灰度切换 + 监控 | 1-2天 | 运维 + 开发 |
| 清理阶段 | 下线旧表、清理数据 | 1天 | DBA + 运维 |

**总计：13-20天**

#### 四、风险控制

**1. 回滚方案**

```java
@Component
public class RollbackService {
    
    @Value("${migration.read.source:old}")
    private String readSource;
    
    /**
     * 紧急回滚
     */
    public void emergencyRollback() {
        // 切换读流量到旧表
        readSource = "old";
        
        // 停止双写
        migrationConfig.setDualWriteEnabled(false);
        
        // 发送告警
        alertService.sendEmergencyAlert("迁移回滚", "已紧急回滚到旧表");
        
        log.error("已执行紧急回滚！");
    }
}
```

**2. 数据备份**

```bash
# 迁移前备份旧表
mysqldump -u root -p price_db product_price > product_price_backup_$(date +%Y%m%d).sql

# 备份 binlog
mysqlbinlog --start-datetime="2024-01-01 00:00:00" --stop-datetime="2024-01-31 23:59:59" \
  /var/lib/mysql/mysql-bin.000001 > binlog_backup.sql
```

**3. 压力测试**

```java
@SpringBootTest
public class MigrationStressTest {
    
    @Autowired
    private ProductPriceService priceService;
    
    @Test
    public void stressTest() {
        int threadCount = 100;
        int requestCount = 10000;
        
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        
        long startTime = System.currentTimeMillis();
        
        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                for (int j = 0; j < requestCount / threadCount; j++) {
                    Long productId = ThreadLocalRandom.current().nextLong(1, 1000000);
                    priceService.getCurrentPrice(productId);
                }
                latch.countDown();
            });
        }
        
        try {
            latch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        double qps = (double) requestCount / (duration / 1000.0);
        
        System.out.println("总请求数：" + requestCount);
        System.out.println("总耗时：" + duration + "ms");
        System.out.println("QPS：" + qps);
    }
}
```

#### 五、优化建议

**1. 性能优化**

- 使用批量插入，减少网络开销
- 合理设置连接池大小
- 使用读写分离，减轻主库压力
- 添加合适的索引，提高查询性能

**2. 监控优化**

- 实时监控迁移进度
- 监控数据一致性
- 监控系统性能指标
- 设置合理的告警阈值

**3. 安全优化**

- 数据备份
- 回滚方案
- 灰度发布
- 压力测试

**4. 运维优化**

- 自动化脚本
- 监控告警
- 文档完善
- 团队培训

#### 六、总结

**核心要点：**

1. **分片策略**：按商品 ID 分片，适合高频查询场景
2. **迁移方案**：双写 + 增量同步 + 数据校验 + 流量切换
3. **不停服**：通过双写和灰度切换实现平滑迁移
4. **数据一致性**：通过 Binlog 同步和数据校验保证
5. **风险控制**：完善的回滚方案和监控告警

**注意事项：**

- 充分测试，确保方案可行
- 监控到位，及时发现问题
- 准备回滚方案，降低风险
- 灰度发布，逐步切换流量
- 文档完善，便于后续维护

这个方案可以在不停服的情况下，安全、高效地完成 3000 万+ 数据的商品价格表分表迁移。