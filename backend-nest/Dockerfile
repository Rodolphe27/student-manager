# Stage 1 — compile TypeScript
FROM node:22-slim AS build
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci
COPY tsconfig.json tsconfig.build.json ./
COPY src ./src
RUN npm run build

# Stage 2 — run it
FROM node:22-slim
WORKDIR /app
ENV NODE_ENV=production
COPY package.json package-lock.json ./
RUN npm ci --omit=dev
COPY --from=build /app/dist ./dist
COPY schema ./schema
# Same port as the Spring backend (Render: PORT=5030).
EXPOSE 5030
USER node
CMD ["node", "dist/main.js"]
