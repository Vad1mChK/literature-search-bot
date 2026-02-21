# ---- Stage 1: Build ----
FROM gradle:8.7-jdk17 AS build
WORKDIR /app

# Copy ONLY the Gradle descriptors first (improves cache)
COPY build.gradle.kts settings.gradle.kts gradle.properties ./
COPY gradle ./gradle

# Download dependencies
RUN gradle dependencies || true

# Copy the actual source code
COPY src ./src

# Build the application
RUN gradle installDist --no-daemon

# ---- Stage 2: Runtime ----
FROM eclipse-temurin:17-jre
WORKDIR /app

# Copy built distribution
COPY --from=build /app/build/install/* ./app/

# Create a non-root user for security
RUN useradd -ms /bin/bash botuser
USER botuser

# Run the application
ENTRYPOINT ["./app/bin/literature-search-bot"]