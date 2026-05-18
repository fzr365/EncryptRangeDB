# EncryptRangeDB（支持密文范围查询的安全数据库管理系统）

支持密文范围查询的安全数据库管理系统。项目采用前后端分离架构，后端基于 Spring Boot、MyBatis 和 MySQL，前端基于 Vue 3、Vite 和 View UI Plus，实现了密文存储、可编程 OPE 范围索引、EAFS 有序链式索引、完整性校验、权限控制和审计日志。

## 项目概述

本项目是基于 Spring Boot + Vue 3 + MySQL 的 B/S 架构安全数据库系统，实现对敏感数值字段的密文存储与密文范围查询。系统采用 AES-GCM 进行字段级加密与完整性校验，设计基于确定性有界扰动值的改进型 OPE 索引，并根据 EAFS 思想实现 Ordered EAFS 链式索引结构，通过锚点定位与顺序扫描完成密文区间检索。支持数据录入、SQL 导入、密文查询、策略配置、审计日志和实验统计等功能，可用于政务、金融、医疗等场景中的敏感数据加密存储与安全检索。

## 核心功能

- AES-GCM 字段级加密存储
- HMAC-SHA256 记录完整性校验
- 可编程 OPE 范围索引 `rindex`
- EAFS 有序链式索引维护与扫描
- SQL 范围条件解析与索引区间转换
- 用户认证、角色权限控制和审计日志

## 项目结构

```text
EncryptRangeDB/
├─ .gitignore
├─ README.md
├─ package.json
├─ package-lock.json
├─ backend/
│  ├─ pom.xml
│  ├─ .mvn/
│  │  └─ maven.config
│  └─ src/
│     ├─ main/
│     │  ├─ java/
│     │  │  └─ com/encryprangedb/
│     │  │     ├─ EncrypRangeDbApplication.java
│     │  │     ├─ auth/
│     │  │     │  ├─ AccessDeniedException.java
│     │  │     │  ├─ AuthContext.java
│     │  │     │  ├─ AuthException.java
│     │  │     │  ├─ AuthInterceptor.java
│     │  │     │  ├─ AuthService.java
│     │  │     │  ├─ AuthenticatedUser.java
│     │  │     │  ├─ PasswordUtil.java
│     │  │     │  └─ UserRole.java
│     │  │     ├─ config/
│     │  │     │  ├─ AdminSchemaInitializer.java
│     │  │     │  ├─ CryptoBeans.java
│     │  │     │  ├─ CryptoProperties.java
│     │  │     │  ├─ GlobalExceptionHandler.java
│     │  │     │  └─ WebConfig.java
│     │  │     ├─ controller/
│     │  │     │  ├─ AdminController.java
│     │  │     │  ├─ AuthController.java
│     │  │     │  ├─ RecordController.java
│     │  │     │  └─ SqlController.java
│     │  │     ├─ crypto/
│     │  │     │  ├─ AESUtil.java
│     │  │     │  ├─ HashUtil.java
│     │  │     │  └─ HmacUtil.java
│     │  │     ├─ mapper/
│     │  │     │  ├─ AnalyticsMapper.java
│     │  │     │  ├─ EafsAnchorMapper.java
│     │  │     │  ├─ EafsOrderedNodeMapper.java
│     │  │     │  └─ RecordMapper.java
│     │  │     ├─ model/
│     │  │     │  ├─ AuditLogResponse.java
│     │  │     │  ├─ DecryptAuditRequest.java
│     │  │     │  ├─ EncryptedField.java
│     │  │     │  ├─ EncryptedInsertRequest.java
│     │  │     │  ├─ EncryptedRecord.java
│     │  │     │  ├─ ExperimentStatsResponse.java
│     │  │     │  ├─ LoginRequest.java
│     │  │     │  ├─ OpePolicyRequest.java
│     │  │     │  ├─ OpePolicyResponse.java
│     │  │     │  ├─ PlainInsertRequest.java
│     │  │     │  ├─ RangeQueryRequest.java
│     │  │     │  ├─ RangeQueryResponse.java
│     │  │     │  ├─ RebuildEafsRequest.java
│     │  │     │  ├─ RebuildEafsResponse.java
│     │  │     │  ├─ SqlImportResult.java
│     │  │     │  └─ entity/
│     │  │     │     ├─ EafsAnchorEntity.java
│     │  │     │     ├─ EafsOrderedNodeEntity.java
│     │  │     │     ├─ EncryptedIndexEntity.java
│     │  │     │     ├─ EncryptedRecordEntity.java
│     │  │     │     ├─ OpePolicyEntity.java
│     │  │     │     └─ QueryAuditLogEntity.java
│     │  │     ├─ ope/
│     │  │     │  └─ ProgrammableOPE.java
│     │  │     └─ service/
│     │  │        ├─ AuditLogService.java
│     │  │        ├─ CryptoService.java
│     │  │        ├─ EafsOrderedIndexService.java
│     │  │        ├─ ExperimentStatsService.java
│     │  │        ├─ IntegrityService.java
│     │  │        ├─ KeyManagementService.java
│     │  │        ├─ OpePolicyService.java
│     │  │        ├─ RecordService.java
│     │  │        ├─ SqlImportService.java
│     │  │        └─ SqlRewriteService.java
│     │  └─ resources/
│     │     ├─ application.yml
│     │     └─ mapper/
│     │        ├─ AnalyticsMapper.xml
│     │        ├─ EafsAnchorMapper.xml
│     │        ├─ EafsOrderedNodeMapper.xml
│     │        └─ RecordMapper.xml
│     └─ test/
│        └─ java/com/encryprangedb/
│           ├─ crypto/
│           │  └─ AESUtilTest.java
│           ├─ ope/
│           │  └─ ProgrammableOPETest.java
│           └─ service/
│              ├─ EafsOrderedIndexServiceTest.java
│              ├─ OpePolicyServiceTest.java
│              ├─ RecordServiceTest.java
│              ├─ SqlImportServiceTest.java
│              └─ SqlRewriteServiceTest.java
├─ frontend/
│  ├─ index.html
│  ├─ package.json
│  ├─ package-lock.json
│  ├─ tsconfig.json
│  ├─ vite.config.ts
│  └─ src/
│     ├─ App.vue
│     ├─ main.ts
│     ├─ api/
│     │  ├─ admin.ts
│     │  ├─ auth.ts
│     │  ├─ http.ts
│     │  ├─ records.ts
│     │  ├─ recordsEncrypted.ts
│     │  ├─ sql.ts
│     │  └─ sqlImport.ts
│     ├─ components/
│     │  ├─ AuditLogPanel.vue
│     │  ├─ ExperimentStatsPanel.vue
│     │  ├─ InsertClientForm.vue
│     │  ├─ InsertPlainForm.vue
│     │  ├─ KeyManagementPanel.vue
│     │  ├─ LoginPanel.vue
│     │  ├─ OpePolicyPanel.vue
│     │  ├─ ResultsTable.vue
│     │  ├─ SqlConsole.vue
│     │  └─ SqlImportPanel.vue
│     ├─ crypto/
│     │  ├─ aesgcm.ts
│     │  ├─ demoKey.ts
│     │  ├─ encryptClient.ts
│     │  └─ ope.ts
│     ├─ store/
│     │  └─ useResults.ts
│     └─ styles/
│        └─ theme.css
└─ scripts/
   ├─ init-db.ps1
   └─ start-mysql-local.ps1
```


## 本地运行

### 1. 启动后端

```bash
cd backend
mvn spring-boot:run
```

默认端口：`8090`

### 2. 启动前端

```bash
cd frontend
npm install
npm run dev
```

### 3. 初始化数据库

项目附带本地脚本：

- [init-db.ps1](E:/EncryptRangeDB/scripts/init-db.ps1)
- [start-mysql-local.ps1](E:/EncryptRangeDB/scripts/start-mysql-local.ps1)

## 项目声明

- 项目名称：支持密文范围查询的安全数据库管理系统
- 英文名称：EncryptRangeDB: Secure Database Management System Supporting Ciphertext Range Query
- 项目作者：Jinqi Lai
- 作者单位：暨南大学网络空间安全学院
- 开发语言：Java、TypeScript、Vue、SQL、PowerShell
- 框架：Spring Boot、MyBatis、Vue 3、Vite
- 数据库：MySQL
- 核心技术：AES-GCM、HMAC、OPE、EAFS
