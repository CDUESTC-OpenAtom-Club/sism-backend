# 附件表 (attachment) 完整数据

**生成时间**: 2026-02-10
**数据库**: strategic (PostgreSQL)
**总记录数**: 4

---

## 表结构

| 字段名 | 类型 | 长度 | 必填 | 默认值 | 说明 |
|--------|------|------|------|--------|------|
| id | bigint | - | ✓ | nextval('attachment_ | |
| storage_driver | character varying | 16 | ✓ | 'FILE'::character va | |
| bucket | character varying | 128 | ✗ | - | |
| object_key | text | - | ✓ | - | |
| public_url | text | - | ✗ | - | |
| original_name | text | - | ✓ | - | |
| content_type | character varying | 128 | ✗ | - | |
| file_ext | character varying | 16 | ✗ | - | |
| size_bytes | bigint | - | ✓ | - | |
| sha256 | character | 64 | ✗ | - | |
| etag | text | - | ✗ | - | |
| uploaded_by | bigint | - | ✓ | - | |
| uploaded_at | timestamp with time zone | - | ✓ | now() | |
| remark | text | - | ✗ | - | |
| is_deleted | boolean | - | ✓ | false | |
| deleted_at | timestamp with time zone | - | ✗ | - | |

---

## 示例数据

显示前 4 条记录

| id | storage_driver | bucket | object_key | public_url | original_name | content_type | file_ext | size_bytes | sha256 | etag | uploaded_by | uploaded_at | remark | is_deleted | deleted_at |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| 1001 | FILE | NULL | upload/2026/02/plan/2026教务处指标下发表.xlsx | NULL | 2026年教务处教学质量指标下发表.xlsx | application/vnd.openxmlformats-officedocument.spre | xlsx | 245376 | 9f86d081884c7d659a2feaa0c55ad015a3bf4f1b2b0b822cd1 | NULL | 101 | 2026-02-04 | 指标下发批次附件示例 | ✗ | NULL |
| 1002 | FILE | NULL | upload/2026/02/report/indicator/成果说明.pdf | NULL | 教学改革项目阶段成果说明.pdf | application/pdf | pdf | 1048576 | 3a7bd3e2360a3d29eea436fcfb7e44c735d117c42d1c183542 | NULL | 203 | 2026-02-15 | 月度填报佐证材料示例 | ✗ | NULL |
| 1003 | FILE | NULL | upload/2026/02/report/image/现场照片.png | NULL | 课堂教学现场照片.png | image/png | png | 512348 | b1946ac92492d2347c6235b4d2611184f6c8a3f9e0e9b0c3c2 | NULL | 203 | 2026-02-16 | 图片佐证示例 | ✗ | NULL |
| 1004 | OSS | strategic-system | 2026/02/attachment/科研经费使用情况说明.docx | https://example-oss/strategic-system/2026/02/attac | 科研经费使用情况说明.docx | application/vnd.openxmlformats-officedocument.word | docx | 786432 | e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca4 | 9b2cf535f27731c974343645a3985328 | 305 | 2026-02-18 | 对象存储模式示例 | ✗ | NULL |

---

## 统计信息

- 总记录数: 4
- 字段数: 16
