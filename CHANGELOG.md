#  (2026-03-05)


### Bug Fixes

* 恢复V5迁移脚本的正确UTF-8编码并统一使用双美元符号 ([8a4a04a](https://github.com/CDUESTC-OpenAtom-Club/sism-backend/commit/8a4a04af015944abfd368317fc8ea73b8d29a73b))
* 使用rsync替代scp，增加重试和文件完整性验证 ([e6b7b97](https://github.com/CDUESTC-OpenAtom-Club/sism-backend/commit/e6b7b97a6aa30c748733a3314fddb1bc128403a6))
* 统一V5迁移脚本中的DO块分隔符为双美元符号 ([76f9185](https://github.com/CDUESTC-OpenAtom-Club/sism-backend/commit/76f9185b85df59351318f7f9547e6222e726fe11))
* 修复环境变量加载路径，支持config/.env ([6c1e11b](https://github.com/CDUESTC-OpenAtom-Club/sism-backend/commit/6c1e11b1f33e45f2743c61c9ecf2a1deb2e485d4))
* 修复审批通过后进度字段处理逻辑 ([954feba](https://github.com/CDUESTC-OpenAtom-Club/sism-backend/commit/954febaa58bc353f0c4f6a368c9ac5518055ebef))
* 修复战略发展部页面显示问题 - 修正isStrategic判断逻辑 ([42c0578](https://github.com/CDUESTC-OpenAtom-Club/sism-backend/commit/42c05788db937c0b844fe15dd96b7fcb5a67019a))
* 修复指标创建时ownerOrgId的映射问题 ([3f7b991](https://github.com/CDUESTC-OpenAtom-Club/sism-backend/commit/3f7b991f9bd056018f3eab64915fc925ced3d2fa))
* 修复Flyway V5迁移脚本中的RAISE NOTICE语法错误 ([abb7497](https://github.com/CDUESTC-OpenAtom-Club/sism-backend/commit/abb74971a790fdf3e607d181b572fd17919fbc5f))
* 修复V5迁移脚本末尾的END语句分隔符 ([94f88e7](https://github.com/CDUESTC-OpenAtom-Club/sism-backend/commit/94f88e76a0ae78a45de49906e831ce15f44e0ad1))
* 修复V6迁移脚本中的列名错误(org_name->name)并统一使用双美元符号 ([c974a09](https://github.com/CDUESTC-OpenAtom-Club/sism-backend/commit/c974a091965cf88f3bce57a25948e35abf3063da))
* 移除SSH配置中不支持的CompressionLevel选项 ([89966ca](https://github.com/CDUESTC-OpenAtom-Club/sism-backend/commit/89966ca6b399f20df644ede5853603cf14895a38))
* 增加健康检查等待时间到60秒 ([bed6068](https://github.com/CDUESTC-OpenAtom-Club/sism-backend/commit/bed606854083cf120f1acf5d1716885ceaae111c))


### Features

* 后端字段映射对齐 - 从sys_task表查询真实任务名称 ([4cdb82b](https://github.com/CDUESTC-OpenAtom-Club/sism-backend/commit/4cdb82bf4ebf818b3cb101677003e54a2fb391ef))
* 简化配置文件并修复环境变量加载 ([62448c6](https://github.com/CDUESTC-OpenAtom-Club/sism-backend/commit/62448c634eb6b855a371599c12c9a5ab7696e917))
* 切换到self-hosted runner进行本地部署 ([87bfcec](https://github.com/CDUESTC-OpenAtom-Club/sism-backend/commit/87bfcece30f6867a742d4e8d4cd887823eb6b8f1))
* **approval:** 两级主管审批流程完整实现（后端） ([77a07fd](https://github.com/CDUESTC-OpenAtom-Club/sism-backend/commit/77a07fd9877c9f70c8219ed0684f352c7bbad4db))


### Performance Improvements

* 优化JAR上传，使用tar.gz压缩传输 ([41821bb](https://github.com/CDUESTC-OpenAtom-Club/sism-backend/commit/41821bb5a5a4dbb949081670df5142ed2e92ed82))
