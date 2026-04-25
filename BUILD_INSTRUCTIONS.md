# 꼬미(Kkomi) 앱 - APK 빌드 가이드

## 1. 빌드 환경 준비

### 1.1. 필수 도구 설치

**Windows/Mac/Linux**:
1. **Android Studio** 다운로드 및 설치
   - https://developer.android.com/studio
   - API 31 이상의 Android SDK 설치

2. **Java Development Kit (JDK)**
   - JDK 11 이상 설치
   - JAVA_HOME 환경변수 설정

### 1.2. 프로젝트 구조 확인

```
kkomi_app/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/kkomi/assistant/
│   │   │   │   └── MainActivity.kt
│   │   │   ├── res/
│   │   │   │   ├── drawable/
│   │   │   │   ├── layout/
│   │   │   │   ├── values/
│   │   │   │   └── xml/
│   │   │   └── AndroidManifest.xml
│   │   ├── test/
│   │   └── androidTest/
│   ├── build.gradle.kts
│   └── proguard-rules.pro
├── gradle/
│   └── wrapper/
│       └── gradle-wrapper.properties
├── build.gradle.kts
├── settings.gradle.kts
├── gradlew
└── gradlew.bat
```

## 2. APK 빌드 방법

### 2.1. Android Studio를 사용한 빌드 (권장)

1. **프로젝트 열기**
   - Android Studio 실행
   - "Open an existing Android Studio project" 선택
   - `/home/ubuntu/kkomi_app` 디렉토리 선택

2. **Gradle 동기화**
   - 프로젝트 로드 후 "Sync Now" 클릭
   - 모든 의존성 다운로드 완료 대기

3. **APK 빌드**
   - 메뉴: `Build` → `Build Bundle(s) / APK(s)` → `Build APK(s)`
   - 빌드 완료 후 `app/build/outputs/apk/debug/` 디렉토리에서 APK 확인

### 2.2. 명령줄을 사용한 빌드

```bash
# 프로젝트 디렉토리로 이동
cd /home/ubuntu/kkomi_app

# Debug APK 빌드
./gradlew assembleDebug

# Release APK 빌드 (서명 필요)
./gradlew assembleRelease

# 빌드 완료 후 APK 위치
# Debug: app/build/outputs/apk/debug/app-debug.apk
# Release: app/build/outputs/apk/release/app-release-unsigned.apk
```

### 2.3. Release APK 서명

```bash
# 1. 키스토어 생성 (처음 한 번만)
keytool -genkey -v -keystore kkomi_keystore.jks -keyalg RSA -keysize 2048 -validity 10000 -alias kkomi_key

# 2. Release APK 서명
jarsigner -verbose -sigalg SHA256withRSA -digestalg SHA-256 \
  -keystore kkomi_keystore.jks \
  app/build/outputs/apk/release/app-release-unsigned.apk \
  kkomi_key

# 3. APK 최적화 (선택사항)
zipalign -v 4 app/build/outputs/apk/release/app-release-unsigned.apk \
  app/build/outputs/apk/release/app-release.apk
```

## 3. 빌드 결과 확인

### 3.1. APK 파일 정보

```bash
# APK 파일 크기 확인
ls -lh app/build/outputs/apk/debug/app-debug.apk

# APK 내용 확인
unzip -l app/build/outputs/apk/debug/app-debug.apk
```

### 3.2. 빌드 로그 확인

```bash
# 빌드 로그 저장
./gradlew assembleDebug > build.log 2>&1

# 에러 확인
grep -i error build.log
```

## 4. 테스트 및 배포

### 4.1. 에뮬레이터에서 테스트

```bash
# 에뮬레이터 실행
emulator -avd Pixel_4_API_31

# APK 설치
adb install app/build/outputs/apk/debug/app-debug.apk

# 앱 실행
adb shell am start -n com.kkomi.assistant/.MainActivity

# 로그 확인
adb logcat | grep kkomi
```

### 4.2. 실제 기기에서 테스트

1. **USB 디버깅 활성화**
   - 기기 설정 → 개발자 옵션 → USB 디버깅 활성화

2. **APK 설치**
   ```bash
   adb install app/build/outputs/apk/debug/app-debug.apk
   ```

3. **앱 실행 및 테스트**
   - 기기에서 "꼬미 AI 비서" 앱 실행
   - 음성 명령 테스트
   - 기능 동작 확인

### 4.3. Google Play Store 배포

1. **Google Play Developer 계정 생성**
   - https://play.google.com/console

2. **Release APK 준비**
   - Release APK 서명 완료
   - 버전 코드 및 버전명 확인

3. **앱 정보 작성**
   - 앱 이름, 설명, 스크린샷, 아이콘 등
   - 개인정보 보호정책 링크

4. **배포**
   - Release APK 업로드
   - 검수 대기 (일반적으로 몇 시간 ~ 며칠)

## 5. 빌드 최적화

### 5.1. 빌드 속도 개선

```gradle
// build.gradle.kts
android {
    // 병렬 빌드 활성화
    gradle.startParameter.maxWorkers = Runtime.getRuntime().availableProcessors()
}
```

### 5.2. APK 크기 최소화

```gradle
android {
    buildTypes {
        release {
            // 코드 축소
            isMinifyEnabled = true
            // 리소스 축소
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
}
```

## 6. 문제 해결

### 6.1. 빌드 실패

**문제**: `Could not find android.jar`
```bash
# 해결: SDK 업데이트
sdkmanager --update
sdkmanager "platforms;android-34"
```

**문제**: `Gradle sync failed`
```bash
# 해결: Gradle 캐시 삭제
./gradlew clean
./gradlew sync
```

### 6.2. 런타임 에러

**문제**: `ClassNotFoundException`
```bash
# 해결: ProGuard 규칙 확인
# proguard-rules.pro에서 필요한 클래스 keep 추가
```

## 7. 최종 체크리스트

- [ ] 모든 권한 설정 확인
- [ ] AndroidManifest.xml 검증
- [ ] 리소스 파일 완성도 확인
- [ ] 코드 컴파일 오류 없음
- [ ] ProGuard 규칙 적용 확인
- [ ] 서명 설정 완료
- [ ] APK 크기 최적화
- [ ] 에뮬레이터 테스트 완료
- [ ] 실제 기기 테스트 완료
- [ ] 배포 준비 완료

