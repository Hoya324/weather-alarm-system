-- Enhanced Weather Info Table Migration
-- 초단기실황, 초단기예보 데이터를 위한 컬럼 추가

-- 실시간 온도 정보 (초단기실황)
ALTER TABLE weather_info ADD COLUMN current_temperature DECIMAL(5,2) NULL COMMENT '현재 온도(실시간)';
ALTER TABLE weather_info ADD COLUMN current_humidity INT NULL COMMENT '현재 습도(실시간)';
ALTER TABLE weather_info ADD COLUMN current_wind_speed DECIMAL(5,2) NULL COMMENT '현재 풍속(실시간)';
ALTER TABLE weather_info ADD COLUMN current_wind_direction INT NULL COMMENT '현재 풍향(실시간)';
ALTER TABLE weather_info ADD COLUMN current_precipitation DECIMAL(5,2) NULL COMMENT '현재 강수량(실시간)';
ALTER TABLE weather_info ADD COLUMN current_precipitation_type VARCHAR(10) NULL COMMENT '현재 강수형태(실시간)';

-- 초단기예보 관련 필드
ALTER TABLE weather_info ADD COLUMN sky_condition INT NULL COMMENT '하늘상태(SKY): 맑음(1), 구름많음(3), 흐림(4)';
ALTER TABLE weather_info ADD COLUMN precipitation_type INT NULL COMMENT '강수형태(PTY): 없음(0), 비(1), 비/눈(2), 눈(3), 소나기(4)';
ALTER TABLE weather_info ADD COLUMN lightning DECIMAL(5,2) NULL COMMENT '낙뢰 에너지밀도(LGT)';

-- 데이터 상태 관리 필드
ALTER TABLE weather_info ADD COLUMN has_current_data BOOLEAN NOT NULL DEFAULT FALSE COMMENT '초단기실황 데이터 보유 여부';
ALTER TABLE weather_info ADD COLUMN has_hourly_forecast BOOLEAN NOT NULL DEFAULT FALSE COMMENT '초단기예보 데이터 보유 여부';

-- 인덱스 추가 (성능 최적화)
CREATE INDEX idx_weather_info_current_data ON weather_info(weather_date, has_current_data);
CREATE INDEX idx_weather_info_hourly_forecast ON weather_info(weather_date, has_hourly_forecast);
CREATE INDEX idx_weather_info_updated_at ON weather_info(updated_at);

-- 기존 데이터의 상태 플래그 업데이트
UPDATE weather_info 
SET has_current_data = FALSE, has_hourly_forecast = FALSE 
WHERE has_current_data IS NULL OR has_hourly_forecast IS NULL;
