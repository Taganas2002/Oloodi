# Frontend — Vite build + nginx (proxies /api including SSE)
FROM node:22-alpine AS build
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci
COPY index.html vite.config.ts tsconfig*.json ./
COPY public ./public
COPY src ./src
# Same-origin /api via nginx — leave VITE_API_BASE_URL empty at build time
RUN npm run build

FROM nginx:1.27-alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html
EXPOSE 80
HEALTHCHECK --interval=10s --timeout=3s --start-period=10s --retries=6 \
  CMD wget -qO- http://127.0.0.1/ >/dev/null || exit 1
