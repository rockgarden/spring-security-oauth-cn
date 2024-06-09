# Spring Security OAuth

探索 Spring Security 5 中的新 OAuth2 堆栈 - 学习 Spring Security OAuth

## Build the Project

```bash
mvn clean install
```

## Projects/Modules

该项目包含多个模块，以下是每个模块包含的内容的简要说明：

- `oauth-rest` - 基于新 Spring Security 5 堆栈的授权服务器（Keycloak）、资源服务器和 Angular 应用程序
- `oauth-jwt` - 基于新 Spring Security 5 堆栈的授权服务器（Keycloak）、资源服务器和 Angular 应用程序，专注于 JWT 支持
- `oauth-jws-jwk-legacy` - Spring Security OAuth2 应用程序中 JWS + JWK 的授权服务器和资源服务器
- `oauth-legacy` - 用于传统 Spring Security OAuth2 的授权服务器、资源服务器、Angular 和 AngularJS 应用程序

## Run the Modules

使用命令行运行任何子模块：

```bash
mvn spring-boot:run
```

如果您使用的是 Spring STS，您还可以通过 Boot Dashboard 直接导入并运行它们

然后，您可以访问 UI 应用程序 - 例如使用密码授予的模块 - 如下所示：

`http://localhost:8084/`

可以使用这些凭据登录，用户名：john 和密码：123

## Run the Angular 7 Modules

- 要运行任何 Angular7 前端模块（_spring-security-oauth-ui-implicit-angular_、_spring-security-oauth-ui-password-angular_ 和 _oauth-ui-authorization-code-angular_），我们需要构建首先是应用程序：
  
```bash
mvn clean install
```

- 然后我们导航到我们的 Angular 应用程序目录：

```bash
cd src/main/resources
```

并运行命令下载依赖项：

```bash
npm install
```

- 最后，我们将启动我们的应用程序：

```bash
npm start
```

> 注意：Angular7 模块被注释掉了，因为它们不是在 Jenkins 上构建的，因为它们需要安装 npm，但它们在本地正确构建。
> Angular 版本 < 4.3.0 的注意事项：您应该注释掉 app.module 和 app.service.ts 中的 HttpClient 和 HttpClientModule 导入。这些版本依赖于 HttpModule。

## Using the JS-only SPA OAuth Client

The main purpose of these projects are to analyze how OAuth should be carried out on Javascript-only Single-Page-Applications, using the authorization_code flow with PKCE.

这些项目的主要目的是分析如何使用 PKCE 的授权代码流在仅 Javascript 的单页应用程序上执行 OAuth。

_clients-SPA-legacy/clients-js-only-react-legacy_ 项目包括一个非常简单的 Spring Boot 应用程序，服务于在 React 中开发的几个单独的单页应用程序。

包括两个页面：

- “逐步”指南，我们在其中明确分析获取访问令牌和请求安全资源所需执行的每个步骤
- 一个“真实案例”场景，我们可以在其中登录、获取或使用安全端点（由 Auth 服务器和我们设置的自定义服务器提供）
- 文章的示例页面，与相关文章中显示的代码完全相同

分步指南支持使用不同的提供者（授权服务器），只需在 `static/*spa*/js/configs.js` 中添加（或取消注释）相应条目。

### 带有 PKCE 页面的“逐步”OAuth 客户端

运行 Spring Boot 应用程序后（一个简单的 _mvn spring-boot:run_ 命令就足够了），我们可以浏览到 _<http://localhost:8080/pkce-stepbystep/index.html>_ 并按照步骤找到了解使用带有 PKCE 流的授权代码获取访问令牌所需的内容。

当提示登录表单时，我们可能需要首先为我们的应用程序创建一个用户。

### 带有 PKCE 页面的“真实案例”OAuth 客户端

要使用 _<http://localhost:8080/pkce-realcase/index.html>_ 页面中包含的所有功能，我们需要首先启动资源服务器（clients-SPA-legacy/oauth-resource-server-auth0-legacy）。

在这个页面中，我们可以：

- 列出我们资源服务器中的资源（公开，无需权限）
- 添加资源（我们在登录时被请求执行此操作的权限。为简单起见，我们只请求现有的 'profile' scope）
- 移除资源（我们实际上无法完成这个任务，因为资源服务器要求应用程序拥有现有范围内未包含的权限）
