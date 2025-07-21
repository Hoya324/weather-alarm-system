#!/bin/bash

set -e

# 매개변수 받기
EC2_INSTANCE_ID="$1"

if [ -z "$EC2_INSTANCE_ID" ]; then
    echo "❌ EC2 Instance ID is required as first parameter"
    echo "Usage: $0 <EC2_INSTANCE_ID>"
    exit 1
fi

echo "🚀 Starting deployment to production..."
echo "📦 Using image: ${ECR_REGISTRY}/${ECR_REPOSITORY}:${IMAGE_TAG}"

# Create deployment script
cat > deploy_script.sh << 'DEPLOY_EOF'
#!/bin/bash

set -e

echo "🚀 Starting Outside Weather deployment..."
echo "Environment: production"
echo "Docker Repository: $DOCKER_REPO"
echo "Image Tag: $IMAGE_TAG"

# 디렉토리 이동 또는 생성
cd /home/ec2-user/outside-weather-deploy || {
    mkdir -p /home/ec2-user/outside-weather-deploy && cd /home/ec2-user/outside-weather-deploy;
}

# .env 파일 확인
if [ ! -f ".env" ]; then
    echo "❌ .env file not found! Please create it manually on EC2."
    exit 1
fi

# 동적 환경변수 업데이트
echo "🔄 Updating dynamic environment variables..."

# IMAGE_TAG 업데이트
if grep -q "^IMAGE_TAG=" .env; then
    sed -i "s/^IMAGE_TAG=.*/IMAGE_TAG=$IMAGE_TAG/" .env
else
    echo "IMAGE_TAG=$IMAGE_TAG" >> .env
fi

# ENVIRONMENT 업데이트
if grep -q "^ENVIRONMENT=" .env; then
    sed -i "s/^ENVIRONMENT=.*/ENVIRONMENT=production/" .env
else
    echo "ENVIRONMENT=production" >> .env
fi

# DOCKER_REPO 업데이트
if grep -q "^DOCKER_REPO=" .env; then
    sed -i "s|^DOCKER_REPO=.*|DOCKER_REPO=$DOCKER_REPO|" .env
else
    echo "DOCKER_REPO=$DOCKER_REPO" >> .env
fi

# SPRING_PROFILES_ACTIVE 업데이트
if grep -q "^SPRING_PROFILES_ACTIVE=" .env; then
    sed -i "s/^SPRING_PROFILES_ACTIVE=.*/SPRING_PROFILES_ACTIVE=prod/" .env
else
    echo "SPRING_PROFILES_ACTIVE=prod" >> .env
fi

echo "✅ Dynamic environment variables updated"
echo "📋 Current IMAGE_TAG: $IMAGE_TAG"

# 필요한 디렉토리 생성 및 정리
mkdir -p nginx/conf.d logs

# nginx 설정 디렉토리 완전 정리
echo "🧹 Cleaning nginx configuration directory..."
rm -rf nginx/conf.d/*

# docker-compose.yml 파일 생성
echo "📄 Creating docker-compose.yml..."
echo "$DOCKER_COMPOSE_CONTENT" | base64 -d > docker-compose.yml

# nginx 설정 파일 생성
echo "📄 Creating nginx configuration files..."
echo "$NGINX_CONF_CONTENT" | base64 -d > nginx/conf.d/default.conf.template
echo "$NGINX_NO_SSL_CONTENT" | base64 -d > nginx/conf.d/nginx.no-ssl.conf.template

# 환경변수 치환
export ENVIRONMENT=production
envsubst '${ENVIRONMENT}' < nginx/conf.d/default.conf.template > nginx/conf.d/default.conf
envsubst '${ENVIRONMENT}' < nginx/conf.d/nginx.no-ssl.conf.template > nginx/conf.d/nginx.no-ssl.conf

# SSL 인증서 확인 및 nginx 설정 결정
if [ -f "nginx/certbot/conf/live/git-tree.com/fullchain.pem" ]; then
    echo "✅ SSL certificate found, using HTTPS configuration"
    # SSL 설정을 default.conf로 복사하고 다른 파일들은 제거
    cp nginx/conf.d/default.conf nginx/conf.d/default.conf
    rm -f nginx/conf.d/nginx.no-ssl.conf nginx/conf.d/*.template
else
    echo "⚠️ SSL certificate not found, using HTTP configuration"
    # HTTP 설정을 default.conf로 복사하고 다른 파일들은 제거
    cp nginx/conf.d/nginx.no-ssl.conf nginx/conf.d/default.conf
    rm -f nginx/conf.d/default.conf nginx/conf.d/*.template
fi

echo "📋 Final nginx configuration files:"
ls -la nginx/conf.d/

echo "🔄 Stopping existing containers..."
docker compose down -v 2>/dev/null || docker-compose down -v 2>/dev/null || true

echo "🧹 Cleaning up old Docker images..."
# 현재 실행 중이지 않은 Outside Weather 이미지들 정리 (최근 3개 버전만 유지)
echo "📦 Cleaning up old Outside Weather images (keeping latest 3 versions)..."
docker images "$DOCKER_REPO" --format "{{.Repository}}:{{.Tag}}" | \
grep -E "production-[0-9]+" | \
sort -V -r | \
tail -n +4 | \
xargs -r docker rmi -f 2>/dev/null || true

# Dangling 이미지 정리
echo "🗑️ Removing dangling images..."
docker image prune -f

# 7일 이상된 미사용 이미지 정리
echo "🗑️ Removing unused images older than 7 days..."
docker image prune -a -f --filter "until=168h" || true

# 사용하지 않는 볼륨과 네트워크 정리
echo "🗑️ Cleaning up unused volumes and networks..."
docker volume prune -f
docker network prune -f

# 컨테이너 정리
echo "🗑️ Removing stopped containers..."
docker container prune -f

# 포트 8080을 사용하는 프로세스 확인 및 종료
echo "🔍 Checking for processes using port 8080..."
if lsof -i :8080 > /dev/null 2>&1; then
    echo "⚠️ Port 8080 is in use, attempting to free it..."
    docker ps --format "table {{.ID}}\t{{.Names}}\t{{.Ports}}" | grep -E "(:8080|0.0.0.0:8080)" | awk '{print $1}' | xargs -r docker stop || true
    docker ps -a --format "table {{.ID}}\t{{.Names}}\t{{.Ports}}" | grep -E "(:8080|0.0.0.0:8080)" | awk '{print $1}' | xargs -r docker rm -f || true
    sleep 5
fi

echo "📥 Pulling new images..."
docker pull $DOCKER_REPO:api-$IMAGE_TAG || exit 1
docker pull $DOCKER_REPO:batch-$IMAGE_TAG || exit 1

# 환경변수 로드
echo "🔄 Loading environment variables..."

# .env 파일의 각 라인을 처리
while IFS= read -r line || [ -n "$line" ]; do
    # 주석과 빈 줄 건너뛰기
    [[ $line =~ ^[[:space:]]*# ]] && continue
    [[ -z "${line// }" ]] && continue

    # 변수 export
    if [[ $line =~ ^([^=]+)=(.*)$ ]]; then
        var_name="${BASH_REMATCH[1]}"
        var_value="${BASH_REMATCH[2]}"
        export "$var_name"="$var_value"
    fi
done < .env

# 중요한 환경변수들을 명시적으로 다시 설정
export ENVIRONMENT=production
export DOCKER_REPO
export IMAGE_TAG
export SPRING_PROFILES_ACTIVE=prod

echo "🚀 Starting services..."
echo "📋 Environment variables:"
echo "  ENVIRONMENT: $ENVIRONMENT"
echo "  DOCKER_REPO: $DOCKER_REPO"
echo "  IMAGE_TAG: $IMAGE_TAG"
echo "  SPRING_PROFILES_ACTIVE: $SPRING_PROFILES_ACTIVE"

# docker-compose 실행
if command -v docker-compose &> /dev/null; then
    echo "📦 Using docker-compose..."
    docker-compose up -d
else
    echo "📦 Using docker compose..."
    docker compose up -d
fi

echo "⏳ Waiting for services to stabilize..."
sleep 30

# 서비스 상태 확인
echo "📊 Checking service status..."
docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"

# 컨테이너 로그 확인
for container in outside-weather-mysql-production outside-weather-api-production outside-weather-batch-production; do
    if ! docker ps --filter "name=$container" --filter "status=running" | grep -q $container; then
        echo "⚠️ Container $container is not running. Checking logs..."
        docker logs $container --tail 20 2>&1 || echo "Failed to get logs for $container"
    fi
done

# 최종 디스크 사용량 확인
echo "💾 Current Docker disk usage:"
docker system df

# 헬스체크
echo "🔍 Running health checks..."
sleep 10  # 추가 대기 시간

# MySQL 헬스체크
if docker ps --filter "name=outside-weather-mysql-production" --filter "status=running" | grep -q mysql; then
    echo "✅ MySQL is running"
else
    echo "❌ MySQL failed to start"
    docker logs outside-weather-mysql-production --tail 50 2>&1 || echo "Failed to get MySQL logs"
    exit 1
fi

# API 헬스체크
if docker ps --filter "name=outside-weather-api-production" --filter "status=running" | grep -q outside-weather-api; then
    echo "✅ Outside Weather API application is running"

    # 애플리케이션 헬스체크
    for i in {1..6}; do
        if curl -f -s --max-time 10 "http://localhost:8080/health" > /dev/null; then
            echo "✅ API application health check passed"
            break
        else
            echo "⚠️ API application health check failed (attempt $i/6), retrying in 10 seconds..."
            if [ $i -eq 6 ]; then
                echo "❌ Health check failed after 6 attempts"
                echo "📋 Trying basic connectivity test..."
                if curl -f -s --max-time 5 "http://localhost:8080/" > /dev/null; then
                    echo "✅ Basic connectivity works, but health endpoint may not be available"
                else
                    echo "❌ No response from application"
                    echo "📋 API Container logs:"
                    docker logs outside-weather-api-production --tail 50 2>&1 || echo "Failed to get API logs"
                    exit 1
                fi
            fi
            sleep 10
        fi
    done
else
    echo "❌ Outside Weather API application failed to start"
    echo "📋 API Container logs:"
    docker logs outside-weather-api-production --tail 50 2>&1 || echo "Failed to get API logs"
    exit 1
fi

if docker ps --filter "name=outside-weather-batch-production" --filter "status=running" | grep -q outside-weather-batch; then
    echo "✅ Outside Weather Batch application is running"
else
    echo "❌ Outside Weather Batch application failed to start"
    echo "📋 Batch Container logs:"
    docker logs outside-weather-batch-production --tail 50 2>&1 || echo "Failed to get Batch logs"
    exit 1
fi

# SSL 인증서 자동 획득 (첫 배포 시)
if [ ! -f "nginx/certbot/conf/live/git-tree.com/fullchain.pem" ]; then
    echo "🔐 Attempting to obtain SSL certificate..."
    docker compose run --rm certbot certonly \
        --webroot \
        --webroot-path=/var/www/certbot \
        --email hoyana1225@gmail.com \
        --agree-tos \
        --no-eff-email \
        -d git-tree.com \
        -d www.git-tree.com || echo "⚠️ SSL certificate acquisition failed"
fi

echo "✅ Deployment completed successfully!"
echo "🌐 Application URL: https://git-tree.com"
DEPLOY_EOF

SCRIPT_CONTENT=$(cat deploy_script.sh | base64 -w 0)

# Upload and execute deployment script via SSM
echo "📤 Uploading deployment script to EC2..."
COMMAND_ID=$(aws ssm send-command \
  --region ${EC2_REGION} \
  --instance-ids "${EC2_INSTANCE_ID}" \
  --document-name "AWS-RunShellScript" \
  --parameters "commands=[
    \"echo '$SCRIPT_CONTENT' | base64 -d > /tmp/deploy.sh\",
    \"chmod +x /tmp/deploy.sh\",
    \"export ENVIRONMENT='production'\",
    \"export DOCKER_REPO='${ECR_REGISTRY}/${ECR_REPOSITORY}'\",
    \"export IMAGE_TAG='${IMAGE_TAG}'\",
    \"export DOCKER_COMPOSE_CONTENT='${DOCKER_COMPOSE_CONTENT}'\",
    \"export NGINX_CONF_CONTENT='${NGINX_CONF_CONTENT}'\",
    \"export NGINX_NO_SSL_CONTENT='${NGINX_NO_SSL_CONTENT}'\",
    \"echo 'Starting deployment script execution...' >> /tmp/deployment.log\",
    \"date >> /tmp/deployment.log\",
    \"/tmp/deploy.sh 2>&1 | tee -a /tmp/deployment.log\"
  ]" \
  --timeout-seconds 3600 \
  --query 'Command.CommandId' \
  --output text)

echo "📋 Command ID: $COMMAND_ID"
echo "command-id=$COMMAND_ID" >> $GITHUB_ENV

# 배포 완료까지 대기 (더 유연한 방식)
echo "⏳ Waiting for deployment to complete..."
echo "📋 Command ID: $COMMAND_ID"
echo "📍 Region: ${EC2_REGION}"
echo "🖥️ Instance: ${EC2_INSTANCE_ID}"

# 최대 30분 동안 10초마다 상태 확인
MAX_ATTEMPTS=180
ATTEMPT=0
LAST_OUTPUT_SIZE=0

while [ $ATTEMPT -lt $MAX_ATTEMPTS ]; do
    STATUS=$(aws ssm get-command-invocation \
      --region ${EC2_REGION} \
      --command-id "${COMMAND_ID}" \
      --instance-id "${EC2_INSTANCE_ID}" \
      --query 'Status' \
      --output text 2>/dev/null || echo "InProgress")

    # 10번째 시도마다 현재 출력 확인
    if [ $((ATTEMPT % 10)) -eq 0 ] && [ $ATTEMPT -gt 0 ]; then
        echo "📊 Status check #$((ATTEMPT + 1)): $STATUS"

        # 출력이 있으면 새로운 부분만 표시
        CURRENT_OUTPUT=$(aws ssm get-command-invocation \
          --region ${EC2_REGION} \
          --command-id "${COMMAND_ID}" \
          --instance-id "${EC2_INSTANCE_ID}" \
          --query 'StandardOutputContent' \
          --output text 2>/dev/null || echo "")

        if [ -n "$CURRENT_OUTPUT" ]; then
            CURRENT_OUTPUT_SIZE=$(echo "$CURRENT_OUTPUT" | wc -c)
            if [ $CURRENT_OUTPUT_SIZE -gt $LAST_OUTPUT_SIZE ]; then
                echo "📝 Recent deployment output:"
                echo "$CURRENT_OUTPUT" | tail -5
                LAST_OUTPUT_SIZE=$CURRENT_OUTPUT_SIZE
            fi
        fi
    else
        printf "."
    fi

    if [ "$STATUS" = "Success" ] || [ "$STATUS" = "Failed" ] || [ "$STATUS" = "Cancelled" ] || [ "$STATUS" = "TimedOut" ]; then
        echo ""
        break
    fi

    sleep 10
    ATTEMPT=$((ATTEMPT + 1))
done

echo ""
if [ $ATTEMPT -eq $MAX_ATTEMPTS ]; then
    echo "⚠️ Deployment monitoring timed out after 30 minutes"
    STATUS="TimedOut"
fi

# 실행 결과 확인
EXECUTION_STATUS=$(aws ssm get-command-invocation \
  --region ${EC2_REGION} \
  --command-id "${COMMAND_ID}" \
  --instance-id "${EC2_INSTANCE_ID}" \
  --query 'Status' \
  --output text)

echo "📋 Deployment Status: $EXECUTION_STATUS"

if [ "$EXECUTION_STATUS" = "Success" ]; then
    echo "✅ Deployment completed successfully!"

    # 성공한 경우 출력 확인
    aws ssm get-command-invocation \
      --region ${EC2_REGION} \
      --command-id "${COMMAND_ID}" \
      --instance-id "${EC2_INSTANCE_ID}" \
      --query 'StandardOutputContent' \
      --output text | tail -20
else
    echo "❌ Deployment failed!"

    # 실패한 경우 오류 출력
    echo "📋 Error output:"
    aws ssm get-command-invocation \
      --region ${EC2_REGION} \
      --command-id "${COMMAND_ID}" \
      --instance-id "${EC2_INSTANCE_ID}" \
      --query 'StandardErrorContent' \
      --output text

    echo "📋 Standard output:"
    aws ssm get-command-invocation \
      --region ${EC2_REGION} \
      --command-id "${COMMAND_ID}" \
      --instance-id "${EC2_INSTANCE_ID}" \
      --query 'StandardOutputContent' \
      --output text

    exit 1
fi
