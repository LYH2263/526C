FROM node:20-alpine AS deps
WORKDIR /app
COPY package.json ./
RUN npm config set registry https://registry.npmmirror.com
RUN --mount=type=cache,target=/root/.npm \
    npm install --no-audit --no-fund

FROM node:20-alpine AS builder
WORKDIR /app
COPY --from=deps /app/node_modules ./node_modules
COPY package.json vite.config.js windi.config.js index.html ./
COPY src ./src
RUN npm run build

FROM nginx:alpine AS runtime
RUN apk add --no-cache shadow && \
    groupadd -r app && useradd -r -g app app && \
    mkdir -p /var/cache/nginx /var/run /var/log/nginx && \
    chown -R app:app /var/cache/nginx /var/run /var/log/nginx /usr/share/nginx/html /etc/nginx/conf.d && \
    chmod -R 755 /var/cache/nginx /var/run /var/log/nginx && \
    apk del shadow
COPY --from=builder /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
USER app
EXPOSE 8080
CMD ["nginx", "-g", "daemon off;"]
