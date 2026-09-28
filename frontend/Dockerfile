# ============================================================
# 本地生活探店平台 - 前端 Dockerfile（多阶段构建）
# 阶段1：Node 构建 dist；阶段2：Nginx 托管静态资源 + 反代 /api
# ============================================================

# ---------- 阶段1：构建 ----------
FROM node:22 AS build
WORKDIR /app

# 先拷贝依赖清单，利用 Docker 层缓存
COPY package.json package-lock.json ./
RUN npm ci

# 拷贝源码并构建，产物为 dist/
COPY . .
RUN npm run build

# ---------- 阶段2：运行 ----------
FROM nginx:alpine

# 自定义 Nginx 配置：SPA history 路由 fallback + /api 反代到 backend:8080
COPY nginx.conf /etc/nginx/conf.d/default.conf

# 拷贝构建产物
COPY --from=build /app/dist /usr/share/nginx/html

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
