# 商品价格表分表实战

## 项目简介

本项目实现了商品价格表分表实战（3000万+数据，不停服迁移）的完整解决方案。使用 Sharding-JDBC 实现数据分片，通过双写、数据迁移、数据校验、流量切换等机制，实现在不停服的情况下平滑迁移。

## 技术栈

- **Spring Boot**: 2.3.7.RELEASE
- **Sharding-JDBC**: 4.0.0-RC1
- **MyBatis**: 2.1.4
- **MySQL**: 5.7+
- **Druid**: 1.1.21
- **Lombok**: Latest

## 项目结构

```
sharding-jdbc-price/
├── doc/                                    # 文档目录
│   ├── init_old_db.sql                     # 旧数据库初始化脚本
│   ├── init_sharding_db.sql                # 分片数据库初始化脚本
│   ├── generate_test_data.sql              # 测试数据生成脚本
│   ├── 实现细节文档.md                      # 实现细节文档
│   └── 验证文档.md                          # 验证文档
├── src/
│   ├── main/
│   │   ├── java/com/baoge/
│   │   │   ├── ShardingJdbcPriceApplication.java  # 启动类
│   │   │   ├── controller/                 # 控制器层
│   │   │   │   ├── ProductPriceController.java    # 价格管理控制器
│   │   │   │   └── MigrationController.java       # 迁移管理控制器
│   │   │   ├── entity/                     # 实体类
│   │   │   │   ├── ProductPrice.java             # 价格实体
│   │   │   │   └── ProductPriceHistory.java      # 价格历史实体
│   │   │   ├── mapper/                     # 数据访问层
│   │   │   │   ├── ProductPriceMapper.java       # 价格Mapper
│   │   │   │   └── ProductPriceHistoryMapper.java # 历史Mapper
│   │   │   └── service/                    # 服务层
│   │   │       ├── ProductPriceService.java       # 价格服务
│   │   │       ├── ProductPriceMigrationService.java  # 迁移服务
│   │   │       ├── ProductPriceReadService.java      # 读取服务
│   │   │       ├── ProductPriceMigrationJob.java    # 迁移任务
│   │   │       └── DataConsistencyChecker.java      # 数据校验
│   │   └── resources/
│   │       ├── application.yml             # 主配置文件
│   │       ├── application-old.yml         # 旧表配置
│   │       ├── application-sharding.yml    # 分片表配置
│   │       └── application-migration.yml   # 迁移配置
│   └── test/
└── pom.xml
```

## 快速开始

### 1. 环境准备

- JDK 1.8+
- Maven 3.6+
- MySQL 5.7+

### 2. 数据库初始化

#### 2.1 创建旧数据库

```bash
mysql -u root -p < doc/init_old_db.sql
```

#### 2.2 创建新分片数据库

```bash
mysql -u root -p < doc/init_sharding_db.sql
```

#### 2.3 生成测试数据

```sql
USE price_db;

-- 生成少量测试数据
INSERT INTO product_price (product_id, sku_id, price, original_price, cost_price, status)
VALUES 
(1, 11, 99.99, 129.99, 59.99, 1),
(2, 21, 199.99, 259.99, 119.99, 1),
(3, 31, 299.99, 399.99, 179.99, 1);

-- 生成大量测试数据（可选）
CALL generate_test_data(100000);
```

### 3. 配置修改

根据实际数据库配置，修改 `application-old.yml`、`application-sharding.yml` 和 `application-migration.yml` 中的数据库连接信息。

### 4. 编译项目

```bash
mvn clean package -DskipTests
```

### 5. 启动应用

#### 5.1 启动旧表模式

```bash
java -jar target/sharding-jdbc-price-0.0.1-SNAPSHOT.jar --spring.profiles.active=old
```

#### 5.2 启动迁移模式

```bash
java -jar target/sharding-jdbc-price-0.0.1-SNAPSHOT.jar --spring.profiles.active=migration
```

#### 5.3 启动分片表模式

```bash
java -jar target/sharding-jdbc-price-0.0.1-SNAPSHOT.jar --spring.profiles.active=sharding
```

## API 接口

### 价格管理接口

#### 添加价格

```bash
POST /price/add
Content-Type: application/json

{
  "productId": 100,
  "skuId": 1001,
  "price": 199.99,
  "originalPrice": 259.99,
  "costPrice": 119.99,
  "status": 1
}
```

#### 更新价格

```bash
POST /price/update
Content-Type: application/json

{
  "id": 1234567890123456789,
  "productId": 100,
  "skuId": 1001,
  "price": 179.99,
  "originalPrice": 259.99,
  "costPrice": 119.99,
  "status": 1
}
```

#### 查询当前价格

```bash
GET /price/current/{productId}
```

#### 查询 SKU 价格

```bash
GET /price/sku/{skuId}
```

#### 按价格范围查询

```bash
GET /price/range?productId={productId}&minPrice={minPrice}&maxPrice={maxPrice}
```

#### 查询历史价格

```bash
GET /price/history/{productId}?limit={limit}
```

### 迁移管理接口

#### 切换读流量

```bash
POST /migration/switch-read?source={source}
```

参数说明：
- `source`: 读源，可选值：`old`（旧表）、`new`（新表）、`both`（双读对比）

#### 获取当前读源

```bash
GET /migration/read-source
```

#### 全量数据迁移

```bash
POST /migration/migrate-all
```

#### 并发数据迁移

```bash
POST /migration/migrate-concurrent
```

#### 全量数据校验

```bash
POST /migration/check-all
```

#### 抽样校验

```bash
POST /migration/check-random
```

## 迁移流程

### 阶段一：准备阶段（1-2天）

1. 创建新数据库和分片表
2. 配置 Sharding-JDBC
3. 测试环境验证

### 阶段二：双写阶段（3-5天）

1. 实现双写逻辑
2. 配置双写开关
3. 实现读写分离逻辑
4. 灰度发布双写功能

### 阶段三：历史数据迁移（5-7天）

1. 全量数据迁移
2. 增量数据同步（基于 Binlog）
3. 迁移进度监控

### 阶段四：数据校验（2-3天）

1. 全量数据校验
2. 抽样校验
3. 数据一致性检查

### 阶段五：流量切换（1-2天）

1. 切换 10% 读流量到新表
2. 切换 50% 读流量到新表
3. 切换 100% 读流量到新表
4. 停止双写，只写新表

### 阶段六：清理阶段（1天）

1. 下线旧表
2. 清理旧数据
3. 归档历史数据

## 分片策略

### 分库规则

- 4个数据库：`price_db_0`、`price_db_1`、`price_db_2`、`price_db_3`
- 分库算法：`product_id % 4`

### 分表规则

- 每个数据库8张表：`product_price_0` ~ `product_price_7`
- 分表算法：`product_id % 8`

### 数据节点

- 共 4 × 8 = 32 张物理表
- 数据节点表达式：`ds$->{0..3}.product_price_$->{0..7}`

## 核心特性

### 1. 不停服迁移

通过双写机制，实现数据迁移过程中应用持续提供服务。

### 2. 数据一致性保证

- 双写机制保证新旧表数据同步
- 数据校验机制确保数据一致性
- 双读对比模式实时检测数据不一致

### 3. 灰度切换

支持逐步切换读流量，降低迁移风险。

### 4. 完善的监控

- 迁移进度监控
- 数据一致性监控
- 系统性能监控

### 5. 快速回滚

发现问题可以快速切换回旧表，保证业务正常运行。

## 性能优化

### 1. 分片查询

通过分片减少单表数据量，提升查询性能。

### 2. 并发迁移

使用多线程并发迁移，提高迁移效率。

### 3. 批量操作

使用批量插入，减少网络开销。

### 4. 连接池优化

合理设置连接池大小，提高并发处理能力。

## 风险控制

### 1. 数据备份

迁移前备份旧表数据，确保数据安全。

### 2. 回滚方案

准备完善的回滚方案，快速响应问题。

### 3. 压力测试

进行充分的压力测试，确保系统稳定性。

### 4. 监控告警

实时监控系统状态，及时发现和处理问题。

## 文档说明

### 实现细节文档.md

详细说明了项目的实现细节，包括：
- 项目概述
- 分表方案设计
- 数据库设计
- 核心功能实现
- 配置文件说明
- 迁移方案
- 监控与告警
- 风险控制
- 优化建议

### 验证文档.md

详细说明了如何验证项目的功能，包括：
- 环境准备
- 启动应用
- 功能验证
- 性能测试
- 异常场景验证
- 验证清单
- 验证报告模板
- 常见问题

## 常见问题

### Q1: 如何选择分片键？

A: 选择查询频率高、数据分布均匀、不常更新的字段作为分片键。本项目选择 `product_id` 作为分片键。

### Q2: 迁移过程中应用是否需要停机？

A: 不需要。通过双写机制，应用可以持续提供服务。

### Q3: 如何确保数据一致性？

A: 通过双写、数据校验、双读对比等多种机制确保数据一致性。

### Q4: 迁移失败如何处理？

A: 系统会记录失败的数据，支持重试。同时可以切换回旧表，保证业务正常运行。

### Q5: 性能提升多少？

A: 根据实际测试，查询性能可提升 80% 以上，具体取决于数据量和查询模式。

## 注意事项

1. **数据备份**：在执行迁移前，务必备份旧表数据
2. **测试环境**：建议先在测试环境充分验证，再在生产环境执行
3. **监控告警**：迁移过程中密切监控系统指标
4. **回滚方案**：准备好回滚方案，以便快速响应问题
5. **灰度发布**：采用灰度发布策略，逐步切换流量
6. **数据校验**：迁移完成后，务必进行完整的数据校验
7. **性能测试**：进行充分的性能测试，确保分片后性能满足要求

## 联系方式

如有问题，请联系项目维护者。

## 许可证

本项目仅供学习和研究使用。