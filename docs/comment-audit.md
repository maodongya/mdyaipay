# Java 中文注释审计（2026-09-17）

依据 [`方便人理解代码规约.md`](方便人理解代码规约.md) §4、§12 对 **`src/main/java`** 做自动化扫描 + 人工补全。

## 扫描口径

| 项 | 规则 |
|----|------|
| 类级 Javadoc | 每个 public `class` / `interface` / `enum` / `record` 声明前须有 `/** … */`（可在注解之上） |
| `package-info.java` | 含至少一个业务/技术 `.java` 的目录应有包说明 |
| 对外 public 方法 | Facade、ApplicationService、RestController 等须方法 Javadoc 或 `{@inheritDoc}` / 指向 Facade |

复跑命令（CI 可挂非零退出码）：

```bash
python3 scripts/java-comment-audit.py
```

## 本轮结果

| 指标 | 审计前 | 补全后 |
|------|--------|--------|
| 缺类级 Javadoc（main） | 41 | **0** |
| 缺 `package-info.java` | 37 | **0** |
| 已补类注释 | — | payment 28、user DAO 12、tools 1 |
| 新增 `package-info` | — | 37 个包 |

### 已重点补全的模块

- `mdyaipay-payment`：domain / service / repository / mybatis / gateway / config
- `mdyaipay-user`：MyBatis Mapper 与 Row
- `mdyaipay-payment-api`、`mdyaipay-gateway`（前序提交）
- `mdyaipay-tools-loadtest-core` 各子包说明

### 方法级补充

- `PaymentApplicationService` / `WithholdApplicationService` / `PayoutApplicationService` 对外入口
- `MerchantFacadeImpl`：`{@inheritDoc}`
- `MerchantRestController`：指向 `MerchantFacade` 各方法

## 未纳入自动扫描（后续可按文件改动补）

- `src/test/java` 中的 public 测试辅助类
- 仅 package-private 的类型
- 复杂方法体内部 **§3.1 功能块**（需人工读代码，非脚本可完全判定）

## 强制规约

见 [`.cursor/rules/java-chinese-readable-docs.mdc`](../.cursor/rules/java-chinese-readable-docs.mdc) 与规约文首 **MANDATORY** 段落。
