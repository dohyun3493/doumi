# 1) Vue 빌드
FROM node:20 AS build
WORKDIR /app
COPY package*.json ./
RUN npm install            # package-lock.json 없어도 동작
COPY . .
RUN npm run build          # .env.production 적용 → dist 생성

# 2) Nginx로 정적 서빙 + 백엔드 프록시
FROM nginx:alpine
COPY --from=build /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
