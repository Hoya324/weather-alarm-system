rootProject.name = "weather-alarm-system"

// Global Utils 모듈
include("modules:global-utils:common-utils")

// Domain 모듈
include("modules:domain:weather-domain")
include("modules:domain:user-domain")

// Infrastructure 모듈  
include("modules:infrastructure:weather-client")
include("modules:infrastructure:geocoding-client")
include("modules:infrastructure:slack-client")

// Application 모듈
include("modules:application:weather-batch")
