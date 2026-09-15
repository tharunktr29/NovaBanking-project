FROM node:22-alpine AS build
WORKDIR /app
COPY package*.json ./
RUN npm ci
COPY . .
RUN npm run build

FROM nginx:1.27-alpine
LABEL org.opencontainers.image.title="NovaBank web" org.opencontainers.image.source="https://github.com/tharunktr29/NovaBanking-project"
RUN rm -rf /usr/share/nginx/html/* && chown -R nginx:nginx /usr/share/nginx/html /var/cache/nginx /run
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html
USER nginx
EXPOSE 8080
