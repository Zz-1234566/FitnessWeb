# 前端容器只负责提供静态文件，HTTPS 和反向代理交给全局 nginx
FROM nginx:alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
