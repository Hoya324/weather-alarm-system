# 7시 알림 누락 문제 해결 가이드

## 🚨 발견된 문제점

### 1. WeatherInfo 데이터 불완전
```sql
-- 현재 상태
id=3, weather_date=NULL, temperature=NULL, user_id=1
id=4, weather_date=NULL, temperature=NULL, user_id=1

-- 문제: 필수 필드들이 비어있음
```

### 2. NotificationInfo 설정
```sql
-- 알림 설정: 07:00:00, 날씨 조건 알림 (WEATHER)
-- 조건: "비" 감지시 알림
-- 좌표: (37.544800839, 126.952017415) → 격자(58, 125)
```

## 🔍 디버깅 단계

### Step 1: WeatherInfo 데이터 확인
```sql
-- 7월 22일 해당 좌표의 날씨 데이터 확인
SELECT * FROM weather_info 
WHERE weather_date = '2025-07-22' 
AND nx = 58 AND ny = 125;

-- 만약 데이터가 없다면
SELECT * FROM weather_info 
WHERE user_id = 1 
ORDER BY created_at DESC LIMIT 5;
```

### Step 2: 배치 실행 로그 확인
```bash
# 7월 22일 02:15, 05:15 배치 실행 로그
grep "2025-07-22.*02:15\|05:15" logs/weather-batch.log

# 좌표 (58, 125) 처리 로그
grep "nx=58.*ny=125" logs/weather-batch.log

# 에러 발생 여부
grep -E "(ERROR|WARN)" logs/weather-batch.log | grep "2025-07-22"
```

### Step 3: KMA API 응답 확인
```bash
# 해당 좌표로 API 테스트
curl "http://localhost:8080/api/batch/test/kma-api?nx=58&ny=125&apiType=FORECAST"
```

## 🛠️ 수동 해결 방법

### 1. 즉시 데이터 수집
```bash
# 종합 데이터 수집 실행
curl -X POST "http://localhost:8080/api/batch/execute/comprehensive"

# 결과 확인
curl "http://localhost:8080/api/batch/status"
```

### 2. 수동 알림 발송
```bash
# 현재 시간 기준 알림 발송
curl -X POST "http://localhost:8080/api/batch/execute/notification"
```

### 3. WeatherInfo 데이터 수동 보정 (임시)
```sql
-- 임시로 weather_date 업데이트
UPDATE weather_info 
SET weather_date = '2025-07-22',
    temperature = 25.0,
    weather_condition = 'CLEAR'
WHERE id IN (3, 4);
```

## 🔧 근본 원인 분석

### 가능한 원인들:

1. **배치 처리 실패**
   ```kotlin
   // WeatherDataFetchTasklet에서 데이터 파싱 실패
   // parseAndSaveWeatherData() 메소드 확인 필요
   ```

2. **KMA API 응답 문제**
   ```json
   // API 응답에서 필수 데이터 누락
   // "resultCode": "03" (NODATA_ERROR) 가능성
   ```

3. **데이터베이스 제약조건**
   ```sql
   -- weather_date가 NOT NULL이어야 하는데 저장 실패
   -- 트랜잭션 롤백 발생 가능성
   ```

4. **좌표 변환 오류**
   ```kotlin
   // 주소 → 좌표 → 격자 변환 과정에서 오류
   // (37.544800839, 126.952017415) → (58, 125) 검증 필요
   ```

## 🚀 영구 해결책

### 1. WeatherDataFetchTasklet 개선
```kotlin
private fun parseAndSaveWeatherData(): Boolean {
    return try {
        // 필수 필드 검증 추가
        if (targetDate == null || coordinate == null) {
            logger.error("필수 데이터 누락: targetDate=$targetDate, coordinate=$coordinate")
            return false
        }
        
        // 기존 로직...
        true
    } catch (e: Exception) {
        logger.error("데이터 저장 실패", e)
        false
    }
}
```

### 2. 알림 조건 로직 개선
```kotlin
// NotificationItemReader에서 조건 확인 강화
fun shouldSendNotification(): Boolean {
    // weather_date, temperature 등 필수 데이터 확인
    if (weatherInfo.weatherDate == null || weatherInfo.temperature == null) {
        logger.warn("날씨 데이터 불완전으로 알림 건너뜀: userId=${user.id}")
        return false
    }
    
    // 기존 조건 로직...
}
```

### 3. 모니터링 강화
```kotlin
@Scheduled(cron = "0 5 7 * * *") // 7시 5분에 확인
fun checkMorningNotificationStatus() {
    val today = LocalDate.now()
    val missingNotifications = notificationInfoRepository
        .findByNotificationEnabledTrueAndNotificationTime(LocalTime.of(7, 0))
        .filter { 
            // 해당 시간대 WeatherInfo가 없는 경우 체크
            weatherInfoRepository.findByWeatherDateAndGrid(today, it.nx, it.ny) == null
        }
    
    if (missingNotifications.isNotEmpty()) {
        logger.error("7시 알림용 날씨 데이터 누락: ${missingNotifications.size}건")
        // 긴급 데이터 수집 또는 알림 발송
    }
}
```

## ⚡ 즉시 조치사항

1. **로그 확인**: 7시 전후 배치 실행 로그 분석
2. **API 테스트**: 해당 좌표 KMA API 응답 확인  
3. **데이터 보정**: weather_date 등 필수 필드 업데이트
4. **알림 재발송**: 수동으로 알림 발송 테스트

## 📞 긴급 연락처

문제가 지속되면:
- 개발팀 슬랙: #weather-alert
- 긴급 전화: 개발팀 on-call
- 로그 파일: `/logs/weather-batch.log`
