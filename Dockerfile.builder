FROM node:20-alpine
WORKDIR /workspace
RUN apk add --no-cache git bash
COPY entrypoint.sh /entrypoint.sh
RUN chmod +x /entrypoint.sh
ENTRYPOINT ["/entrypoint.sh"]
