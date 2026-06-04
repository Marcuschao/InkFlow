# 压测脚本（k6）

需安装 [k6](https://k6.io/docs/get-started/installation/)。

压测前请用 **`local,benchmark`** 启动后端（关闭限流，避免 429 干扰读路径指标）：

```powershell
$env:JAVA_HOME="C:\Program Files\Java\jdk-17"
$env:SPRING_PROFILES_ACTIVE="local,benchmark"
$env:SERVER_PORT="8082"
mvn -f personal-blog-backend spring-boot:run '-Dmaven.test.skip=true'
$env:BASE_URL="http://127.0.0.1:8082"
```

## 文章详情

```powershell
$env:BASE_URL="http://127.0.0.1:8080"
$env:ARTICLE_ID="1"
$env:VUS="80"
$env:DISTINCT_IP="1"
k6 run scripts/benchmark/k6/article-detail.js
```

## 文章列表

```powershell
$env:DISTINCT_IP="1"
k6 run scripts/benchmark/k6/article-list.js
```

## 统计上报（限流）

```powershell
$env:VUS="120"
k6 run scripts/benchmark/k6/stat-view.js
```

指标说明见 [docs/benchmark.md](../../docs/benchmark.md)。
