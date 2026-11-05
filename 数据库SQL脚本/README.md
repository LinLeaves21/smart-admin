# 数据库脚本

本目录提供 SmartAdmin 的 MySQL 8 初始化脚本和版本增量脚本。

## 首次部署

创建数据库后执行：

```bash
mysql -uroot -p smart_admin_v3 < mysql/smart_admin_v3.sql
```

## 版本升级

按版本号从小到大执行 `mysql/sql-update-log/` 目录下的增量脚本。

新增表或字段时，需要同时补充对应的增量脚本。