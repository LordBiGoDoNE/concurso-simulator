FROM node@sha256:44fce696f38ba8c987c4db89954051000bbd05ffd8d8659ba15ed4a8602e937f AS spa
WORKDIR /source/frontend
COPY frontend/package*.json ./
RUN npm ci
COPY frontend/ ./
ARG LOCAL_APP_ORIGIN=http://127.0.0.1:8088
ENV VITE_API_URL=/
ENV VITE_MATERIAL_URL=${LOCAL_APP_ORIGIN}/material/
RUN node -e "const u=new URL(process.env.VITE_MATERIAL_URL);if(u.protocol!=='http:'||u.hostname!=='127.0.0.1'||u.username||u.password||u.search||u.hash||u.pathname!=='/material/')process.exit(1)" && npm run build

FROM python@sha256:f6a589d43c42b9e7f7dc67a12d37132491f362859a5d750607710cc56da3bc72 AS material
WORKDIR /source
COPY . ./
RUN python scripts/build_pages.py --branch main --stored /empty --output /study

FROM nginxinc/nginx-unprivileged@sha256:782bb89768cc87b7f95c18b95baa1af250e5d451f26794f59363540161147c01
COPY infra/local-app/nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=spa /source/frontend/dist/ /usr/share/nginx/html/
COPY --from=material /study/ /usr/share/nginx/html/material/
USER 101:101
EXPOSE 8080
HEALTHCHECK --interval=10s --timeout=3s --retries=3 CMD ["wget", "-q", "-O", "/dev/null", "http://127.0.0.1:8080/"]
