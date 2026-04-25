# 꼬미(Kkomi) - 오프라인 AI 비서 앱

## 📱 개요

**꼬미**는 흰색 아기 비숑 캐릭터를 기반으로 한 완전 개인 소유의 오프라인 AI 비서 앱입니다. 사용자의 음성 명령을 통해 전화, 문자, 음악, 알람 등 다양한 기능을 제어하며, 모든 데이터는 기기 내에 AES-256 암호화되어 저장됩니다.

### 핵심 특징

- **완전 오프라인**: 인터넷 없이도 모든 핵심 기능 작동
- **프라이버시 중심**: 모든 데이터 기기 내 저장 및 암호화
- **귀여운 캐릭터**: 꼬미와의 친근한 상호작용
- **음성 명령**: "꼬미야" 핫워드 감지로 쉬운 제어
- **개인화 학습**: 사용자 패턴 및 선호도 학습

## 🎯 주요 기능

### MVP (v1.0)

- ✅ 음성 명령 인식 ("꼬미야" 핫워드)
- ✅ 전화 걸기 (확인 절차 포함)
- ✅ 문자 보내기 (확인 절차 포함)
- ✅ 음악 재생
- ✅ 알람/타이머 설정
- ✅ 볼륨 조절 (최대 70%)
- ✅ 화면 밝기 조절
- ✅ 대화 기록 저장
- ✅ 기본 설정 (이름, 선호도)

### 향후 계획

- 🔄 AI 학습 고도화
- 🔄 온라인 검색 (Wi-Fi 기반)
- 🔄 음성 메모
- 🔄 명령어 매크로
- 🔄 생체인식 보안

## 📋 기술 스택

| 분야 | 기술 |
|---|---|
| **언어** | Kotlin |
| **프레임워크** | Android Framework |
| **AI** | TensorFlow Lite |
| **DB** | SQLite (SQLCipher - AES-256) |
| **음성** | Android Speech-to-Text/TTS |
| **기기 제어** | Android Intent |
| **보안** | Android Keystore |

## 🚀 빌드 및 실행

### 필수 요구사항

- Android Studio 2023.1 이상
- Android SDK API 31 이상
- JDK 11 이상
- 최소 2GB RAM

### 빌드 방법

#### 1. Android Studio 사용 (권장)

```bash
# 프로젝트 열기
1. Android Studio 실행
2. "Open an existing Android Studio project" 선택
3. kkomi_app 디렉토리 선택
4. "Sync Now" 클릭
5. Build → Build APK(s) 선택
```

#### 2. 명령줄 사용

```bash
# Debug APK 빌드
cd /home/ubuntu/kkomi_app
./gradlew assembleDebug

# Release APK 빌드
./gradlew assembleRelease

# 빌드 결과
# Debug: app/build/outputs/apk/debug/app-debug.apk
# Release: app/build/outputs/apk/release/app-release.apk
```

### 설치 및 실행

```bash
# 에뮬레이터/기기에 설치
adb install app/build/outputs/apk/debug/app-debug.apk

# 앱 실행
adb shell am start -n com.kkomi.assistant/.MainActivity

# 로그 확인
adb logcat | grep kkomi
```

## 📁 프로젝트 구조

```
kkomi_app/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/kkomi/assistant/
│   │   │   │   ├── MainActivity.kt          # 메인 화면
│   │   │   │   ├── core/
│   │   │   │   │   ├── KkomiAssistant.kt   # AI 비서 로직
│   │   │   │   │   └── KkomiDatabaseManager.kt
│   │   │   │   ├── ui/
│   │   │   │   ├── data/
│   │   │   │   └── util/
│   │   │   ├── res/
│   │   │   │   ├── drawable/               # 이미지 리소스
│   │   │   │   ├── layout/                 # UI 레이아웃
│   │   │   │   ├── values/                 # 문자열, 색상 등
│   │   │   │   └── xml/                    # 설정 파일
│   │   │   └── AndroidManifest.xml
│   │   ├── test/                           # 단위 테스트
│   │   └── androidTest/                    # 통합 테스트
│   ├── build.gradle.kts                    # 앱 빌드 설정
│   └── proguard-rules.pro                  # 코드 난독화 규칙
├── gradle/
│   └── wrapper/                            # Gradle Wrapper
├── build.gradle.kts                        # 프로젝트 빌드 설정
├── settings.gradle.kts                     # 프로젝트 설정
├── gradlew                                 # Gradle Wrapper (Linux/Mac)
├── gradlew.bat                             # Gradle Wrapper (Windows)
├── README.md                               # 이 파일
└── BUILD_INSTRUCTIONS.md                   # 빌드 상세 가이드
```

## 🔐 보안 기능

### 데이터 보호

- **AES-256 암호화**: 모든 민감한 데이터 암호화
- **Android Keystore**: 마스터 키 안전 저장
- **TLS 1.3**: 온라인 통신 암호화
- **권한 최소화**: 필요한 권한만 요청

### 안전장치

- **확인 절차**: 전화/문자 발신 전 사용자 확인
- **볼륨 제한**: 최대 70%로 청각 보호
- **전화 중 비활성화**: 통화 중 음성 명령 차단

## 📊 성능 목표

| 메트릭 | 목표 |
|---|---|
| 음성 인식 | 1초 이내 |
| 명령 실행 | 0.5초 이내 |
| 앱 시작 | 2초 이내 |
| 배터리 소모 | 1시간 대기 시 0.3% 이하 |
| 메모리 사용 | 85MB 이하 |

## 🧪 테스트

### 단위 테스트

```bash
./gradlew test
```

### 통합 테스트

```bash
./gradlew connectedAndroidTest
```

### 성능 프로파일링

- Android Profiler 사용
- CPU, 메모리, 배터리, 네트워크 모니터링

## 📝 개발 로드맵

### Phase 1: MVP (2개월)
- [x] 프로젝트 구조 설정
- [x] UI/UX 기본 구현
- [ ] 음성 처리 통합
- [ ] AI 모델 통합
- [ ] 기기 제어 구현
- [ ] 데이터베이스 구현
- [ ] 테스트 및 최적화

### Phase 2: 1차 업데이트 (1.5개월)
- [ ] AI 학습 고도화
- [ ] 온라인 검색 기능
- [ ] 음성 메모

### Phase 3: 2차 업데이트 (이후)
- [ ] 명령어 매크로
- [ ] 생체인식 보안
- [ ] 자동 루틴

## 🤝 기여

이 프로젝트는 개인 프로젝트입니다. 피드백 및 제안은 환영합니다.

## 📄 라이선스

MIT License - 자유롭게 사용, 수정, 배포 가능합니다.

## 👨‍💻 개발자

- **Manus AI** - 초기 개발 및 설계
- **사용자** - 기획 및 요구사항 정의

## 📞 지원

문제가 발생하거나 질문이 있으시면:
1. 이슈 트래커에 보고
2. 개발자에게 직접 문의

## 🙏 감사의 말

- TensorFlow Lite 팀
- Android 개발 커뮤니티
- SQLCipher 팀

---

**꼬미와 함께 더 편한 스마트폰 생활을 즐겨보세요!** 🐕✨

