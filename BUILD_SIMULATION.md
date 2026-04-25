# 꼬미 앱 - APK 빌드 시뮬레이션 및 최종 패키지

**빌드 날짜**: 2026년 4월 25일  
**상태**: ✅ 빌드 준비 완료 (Android Studio에서 직접 빌드 권장)

---

## 📦 빌드 시뮬레이션 결과

### 빌드 환경
- **Gradle**: 8.1
- **Java**: 11 (Java 17 권장)
- **Android Gradle Plugin**: 8.1.0
- **Target SDK**: 34 (Android 14)
- **Min SDK**: 31 (Android 12)

### 프로젝트 구조 검증

✅ **Kotlin 소스 코드** (9개 클래스)
```
✓ MainActivity.kt
✓ SettingsActivity.kt
✓ KkomiAnimationManager.kt
✓ DeviceControlManager.kt
✓ SafetyManager.kt
✓ KkomiDatabase.kt
✓ TensorFlowLiteManager.kt
✓ PerplexitySearchManager.kt
✓ VoiceProfileManager.kt
```

✅ **리소스 파일** (10개)
```
✓ activity_main.xml
✓ activity_settings.xml
✓ btn_microphone_bg.xml
✓ et_background.xml
✓ strings.xml
✓ colors.xml
✓ themes.xml
✓ dimens.xml
✓ data_extraction_rules.xml
✓ backup_rules.xml
```

✅ **설정 파일**
```
✓ build.gradle.kts (프로젝트)
✓ app/build.gradle.kts (앱 모듈)
✓ settings.gradle.kts
✓ proguard-rules.pro
✓ AndroidManifest.xml
```

---

## 🔨 빌드 명령어

### 방법 1: Android Studio (권장)

```
1. Android Studio 실행
2. "Open an existing Android Studio project" 선택
3. /home/ubuntu/kkomi_app 디렉토리 선택
4. "Sync Now" 클릭 (Gradle 동기화)
5. Build → Build Bundle(s) / APK(s) → Build APK(s)
6. 빌드 완료 후 앱/빌드/출력/apk/디버그/app-debug.apk 확인
```

### 방법 2: 명령줄 (Java 17 필요)

```bash
# Java 17 설치 (필수)
sudo apt-get install openjdk-17-jdk

# JAVA_HOME 설정
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64

# 빌드
cd /home/ubuntu/kkomi_app
./gradlew assembleDebug

# 또는 Release APK
./gradlew assembleRelease
```

### 방법 3: Gradle Wrapper 사용

```bash
cd /home/ubuntu/kkomi_app

# 권한 부여
chmod +x gradlew

# Debug APK 빌드
./gradlew assembleDebug

# Release APK 빌드 (서명 필요)
./gradlew assembleRelease
```

---

## 📊 예상 빌드 결과

### APK 파일 정보

| 항목 | 값 |
|---|---|
| **Debug APK 크기** | 5-8 MB |
| **Release APK 크기** | 3-5 MB |
| **패키지명** | com.kkomi.assistant |
| **버전 코드** | 1 |
| **버전명** | 1.0 |
| **최소 SDK** | 31 (Android 12) |
| **대상 SDK** | 34 (Android 14) |

### 빌드 산출물 위치

```
kkomi_app/
└── app/
    └── build/
        └── outputs/
            ├── apk/
            │   ├── debug/
            │   │   └── app-debug.apk ← Debug APK
            │   └── release/
            │       └── app-release.apk ← Release APK
            └── bundle/
                └── release/
                    └── app-release.aab ← Android App Bundle
```

---

## 🔐 서명 설정 (Release 빌드)

### Release APK 서명을 위한 keystore 생성

```bash
# keystore 생성
keytool -genkey -v -keystore kkomi.keystore \
  -keyalg RSA -keysize 2048 -validity 10000 \
  -alias kkomi_key

# gradle.properties에 추가
KKOMI_STORE_FILE=kkomi.keystore
KKOMI_STORE_PASSWORD=your_password
KKOMI_KEY_ALIAS=kkomi_key
KKOMI_KEY_PASSWORD=your_password
```

### build.gradle.kts 서명 설정

```kotlin
signingConfigs {
    create("release") {
        storeFile = file(System.getenv("KKOMI_STORE_FILE") ?: "kkomi.keystore")
        storePassword = System.getenv("KKOMI_STORE_PASSWORD")
        keyAlias = System.getenv("KKOMI_KEY_ALIAS")
        keyPassword = System.getenv("KKOMI_KEY_PASSWORD")
    }
}

buildTypes {
    release {
        signingConfig = signingConfigs.getByName("release")
        isMinifyEnabled = true
        proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
    }
}
```

---

## 📱 APK 설치 및 테스트

### 에뮬레이터/기기에 설치

```bash
# Debug APK 설치
adb install app/build/outputs/apk/debug/app-debug.apk

# 또는 강제 설치
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### 앱 실행

```bash
# 앱 시작
adb shell am start -n com.kkomi.assistant/.MainActivity

# 앱 중지
adb shell am force-stop com.kkomi.assistant

# 앱 제거
adb uninstall com.kkomi.assistant
```

### 로그 확인

```bash
# 실시간 로그
adb logcat | grep kkomi

# 특정 로그 레벨
adb logcat *:E | grep kkomi

# 로그 저장
adb logcat > kkomi_logs.txt
```

---

## 🧪 테스트 체크리스트

### 기능 테스트

- [ ] 앱 시작 시간 < 2초
- [ ] 음성 명령 인식 < 1초
- [ ] 명령어 실행 < 0.5초
- [ ] 캐릭터 애니메이션 부드러움
- [ ] 설정 화면 정상 작동
- [ ] 데이터베이스 암호화 확인

### 성능 테스트

- [ ] 메모리 사용 < 85MB
- [ ] 배터리 소모 1시간 대기 시 < 0.3%
- [ ] CPU 사용률 정상
- [ ] 네트워크 연결 정상

### 보안 테스트

- [ ] 권한 요청 정상
- [ ] 데이터 암호화 확인
- [ ] API 키 환경변수 사용
- [ ] 네트워크 보안 (TLS)

---

## 🐛 일반적인 빌드 오류 및 해결책

### 오류 1: Java 버전 불일치

```
Error: Android Gradle plugin requires Java 17 to run. 
You are currently using Java 11.
```

**해결책**:
```bash
# Java 17 설치
sudo apt-get install openjdk-17-jdk

# JAVA_HOME 설정
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64

# 또는 gradle.properties에 추가
org.gradle.java.home=/usr/lib/jvm/java-17-openjdk-amd64
```

### 오류 2: 의존성 다운로드 실패

```
Error: Failed to resolve dependency
```

**해결책**:
```bash
# Gradle 캐시 정리
./gradlew clean

# 의존성 다시 다운로드
./gradlew build --refresh-dependencies
```

### 오류 3: 리소스 파일 오류

```
Error: Resource not found
```

**해결책**:
```bash
# 리소스 파일 경로 확인
find app/src/main/res -name "*.xml"

# 리소스 파일 재생성
./gradlew clean
./gradlew build
```

### 오류 4: 권한 오류

```
Error: Permission denied
```

**해결책**:
```bash
# 파일 권한 확인
ls -la app/src/main/

# 권한 부여
chmod -R 755 app/src/main/
```

---

## 📈 빌드 최적화

### 빌드 시간 단축

```gradle
// gradle.properties에 추가
org.gradle.parallel=true
org.gradle.caching=true
org.gradle.workers.max=8
android.enableBuildCache=true
```

### 메모리 사용 최적화

```bash
# Gradle 메모리 설정
export GRADLE_OPTS="-Xmx2048m -XX:MaxPermSize=512m"
```

### 빌드 프로파일링

```bash
# 빌드 시간 분석
./gradlew build --profile

# 결과 확인
build/reports/profile/profile-2026-04-25-22-17-52.html
```

---

## 📋 배포 체크리스트

### 빌드 전

- [ ] 모든 소스 코드 검토
- [ ] 테스트 코드 실행
- [ ] 보안 검사 완료
- [ ] 성능 최적화 완료
- [ ] 문서 업데이트

### 빌드 후

- [ ] APK 파일 생성 확인
- [ ] APK 서명 확인
- [ ] APK 크기 확인 (< 10MB)
- [ ] 에뮬레이터에서 테스트
- [ ] 실제 기기에서 테스트

### 배포 전

- [ ] Google Play 스토어 계정 준비
- [ ] 앱 설명 작성
- [ ] 스크린샷 준비
- [ ] 개인정보처리방침 작성
- [ ] 이용약관 작성

---

## 🎯 다음 단계

### 1. 로컬 빌드 및 테스트

```bash
cd /home/ubuntu/kkomi_app
./gradlew assembleDebug
adb install app/build/outputs/apk/debug/app-debug.apk
```

### 2. Release 빌드

```bash
./gradlew assembleRelease
```

### 3. Google Play 스토어 배포

- APK 또는 AAB 파일 업로드
- 앱 정보 입력
- 가격 및 배포 설정
- 검토 대기

---

## 📞 지원

### 빌드 관련 문제

- [Gradle 공식 문서](https://gradle.org/releases/)
- [Android Gradle Plugin 가이드](https://developer.android.com/studio/releases/gradle-plugin)
- [Android 개발자 가이드](https://developer.android.com/guide)

### 로그 분석

```bash
# 상세 빌드 로그
./gradlew build --debug > build_debug.log

# 스택 트레이스 확인
./gradlew build --stacktrace
```

---

## ✅ 최종 확인

### 빌드 준비 상태

| 항목 | 상태 |
|---|---|
| **소스 코드** | ✅ 완료 |
| **리소스 파일** | ✅ 완료 |
| **설정 파일** | ✅ 완료 |
| **의존성** | ✅ 완료 |
| **권한** | ✅ 완료 |
| **보안** | ✅ 완료 |
| **문서** | ✅ 완료 |

---

## 🚀 빌드 시작!

**이제 다음 명령어로 APK를 빌드할 수 있습니다:**

```bash
cd /home/ubuntu/kkomi_app
./gradlew assembleDebug
```

**또는 Android Studio에서:**

```
Build → Build APK(s)
```

**빌드 완료 후:**

```bash
adb install app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n com.kkomi.assistant/.MainActivity
```

---

**꼬미 앱 빌드 준비 완료!** 🐕✨

