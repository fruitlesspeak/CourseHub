# build stage
FROM node:22-alpine AS build
WORKDIR /app
COPY package*.json ./
RUN npm install
COPY . .
RUN npm run build

# production stage
FROM nginx:1.27-alpine
COPY --from=build /app/dist /usr/share/nginx/html
RUN rm /etc/nginx/conf.d/default.conf
COPY nginx.conf.template /etc/nginx/templates/default.conf.template
EXPOSE 80
CMD ["sh", "-c", "NAMESERVER=$(awk '/^nameserver / {print $2; exit}' /etc/resolv.conf); case \"$NAMESERVER\" in *:*) export NAMESERVER=\"[$NAMESERVER]\" ;; *) export NAMESERVER ;; esac; envsubst '$NAMESERVER' < /etc/nginx/templates/default.conf.template > /etc/nginx/conf.d/default.conf && exec nginx -g 'daemon off;'"]
