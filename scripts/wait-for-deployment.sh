#!/bin/bash

# AWS SSM 명령 실행 대기 및 모니터링 스크립트

set -e

COMMAND_ID="$1"
INSTANCE_ID="$2"
EC2_REGION="${EC2_REGION:-ap-northeast-2}"

# Color codes
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

log() {
    echo -e "${BLUE}[$(date '+%H:%M:%S')]${NC} $1"
}

success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

if [ -z "$COMMAND_ID" ] || [ -z "$INSTANCE_ID" ]; then
    error "사용법: $0 <COMMAND_ID> <INSTANCE_ID>"
    error "예시: $0 12345678-1234-1234-1234-123456789012 i-1234567890abcdef0"
    exit 1
fi

log "🚀 SSM 배포 명령 모니터링 시작"
log "Command ID: $COMMAND_ID"
log "Instance ID: $INSTANCE_ID"
log "Region: $EC2_REGION"

# 최대 대기 시간 (30분)
MAX_WAIT_TIME=1800
WAIT_INTERVAL=15
ELAPSED_TIME=0
LAST_OUTPUT_CHECK=0

while [ $ELAPSED_TIME -lt $MAX_WAIT_TIME ]; do
    # 명령 상태 확인
    STATUS=$(aws ssm get-command-invocation \
        --region "$EC2_REGION" \
        --command-id "$COMMAND_ID" \
        --instance-id "$INSTANCE_ID" \
        --query 'Status' \
        --output text 2>/dev/null || echo "Unknown")

    case "$STATUS" in
        "Success")
            success "🎉 배포가 성공적으로 완료되었습니다!"

            # 최종 출력 내용 표시
            echo ""
            echo "=== 배포 완료 로그 ==="
            aws ssm get-command-invocation \
                --region "$EC2_REGION" \
                --command-id "$COMMAND_ID" \
                --instance-id "$INSTANCE_ID" \
                --query 'StandardOutputContent' \
                --output text 2>/dev/null | tail -30

            echo ""
            success "✅ https://git-tree.com 에서 배포 결과를 확인하세요!"
            exit 0
            ;;
        "Failed")
            error "❌ 배포가 실패했습니다!"

            # 실패 시 에러 내용 표시
            echo ""
            echo "=== 에러 로그 ==="
            aws ssm get-command-invocation \
                --region "$EC2_REGION" \
                --command-id "$COMMAND_ID" \
                --instance-id "$INSTANCE_ID" \
                --query 'StandardErrorContent' \
                --output text 2>/dev/null | tail -50 || echo "에러 로그를 가져올 수 없습니다."

            echo ""
            echo "=== 표준 출력 (마지막 50줄) ==="
            aws ssm get-command-invocation \
                --region "$EC2_REGION" \
                --command-id "$COMMAND_ID" \
                --instance-id "$INSTANCE_ID" \
                --query 'StandardOutputContent' \
                --output text 2>/dev/null | tail -50

            exit 1
            ;;
        "InProgress")
            minutes=$((ELAPSED_TIME / 60))
            seconds=$((ELAPSED_TIME % 60))
            log "🔄 배포 진행 중... (${minutes}m ${seconds}s)"

            # 1분마다 진행 상황 확인
            if [ $((ELAPSED_TIME - LAST_OUTPUT_CHECK)) -ge 60 ]; then
                echo ""
                echo "=== 현재 진행 상황 (최근 15줄) ==="
                aws ssm get-command-invocation \
                    --region "$EC2_REGION" \
                    --command-id "$COMMAND_ID" \
                    --instance-id "$INSTANCE_ID" \
                    --query 'StandardOutputContent' \
                    --output text 2>/dev/null | tail -15 | sed 's/^/  /' || echo "  진행 상황을 가져올 수 없습니다."
                echo ""

                LAST_OUTPUT_CHECK=$ELAPSED_TIME
            fi
            ;;
        "Cancelled")
            warning "⚠️ 배포가 취소되었습니다."
            exit 1
            ;;
        "TimedOut")
            error "⏰ 배포가 AWS SSM 시간 초과되었습니다."
            exit 1
            ;;
        "Cancelling")
            warning "🛑 배포 취소 중..."
            ;;
        "Unknown")
            warning "❓ 명령 상태를 확인할 수 없습니다. AWS 연결을 확인하세요."
            # Unknown 상태가 계속되면 종료
            if [ $ELAPSED_TIME -gt 300 ]; then  # 5분
                error "명령 상태 확인 실패로 종료합니다."
                exit 1
            fi
            ;;
        *)
            log "📋 상태: $STATUS"
            ;;
    esac

    sleep $WAIT_INTERVAL
    ELAPSED_TIME=$((ELAPSED_TIME + WAIT_INTERVAL))

    # 진행률 표시
    progress=$((ELAPSED_TIME * 100 / MAX_WAIT_TIME))
    if [ $((ELAPSED_TIME % 120)) -eq 0 ] && [ $ELAPSED_TIME -gt 0 ]; then  # 2분마다
        log "⏳ 진행률: ${progress}% (${ELAPSED_TIME}/${MAX_WAIT_TIME}초)"
    fi
done

error "⏰ 최대 대기 시간(${MAX_WAIT_TIME}초)을 초과했습니다."
echo ""
warning "배포가 아직 진행 중일 수 있습니다. 다음 명령어로 수동 확인하세요:"
echo ""
echo "aws ssm get-command-invocation \\"
echo "  --region $EC2_REGION \\"
echo "  --command-id $COMMAND_ID \\"
echo "  --instance-id $INSTANCE_ID"
echo ""
warning "또는 EC2 인스턴스에 직접 접속하여 상태를 확인하세요:"
echo "ssh ec2-user@your-server-ip"
echo "cd /home/ec2-user/outside-weather-deploy && docker ps"

exit 1
