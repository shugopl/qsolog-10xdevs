# Stage 1: Build
FROM node:22-alpine AS builder

WORKDIR /build

# Copy package files and install dependencies
COPY package*.json ./
RUN npm ci

# Copy source code and build
COPY . .
RUN npm run build

# Stage 2: Runtime with nginx
FROM nginx:1.27-alpine

# Copy custom nginx configuration
COPY nginx.conf /etc/nginx/conf.d/default.conf

# Copy built application from builder stage
COPY --from=builder /build/dist/frontend/browser /usr/share/nginx/html

# Create non-root user (nginx runs as nginx user by default in alpine)
RUN chown -R nginx:nginx /usr/share/nginx/html && \
    chmod -R 755 /usr/share/nginx/html

# Expose port
EXPOSE 80

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost/ || exit 1

# nginx runs as non-root by default in alpine image
CMD ["nginx", "-g", "daemon off;"]
