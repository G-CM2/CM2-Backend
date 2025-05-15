# Docker Container Management Dashboard Backend 

Docker 컨테이너 모니터링 및 관리를 위한 웹 API 서버입니다.

## 기술 스택

- **Spring boot**: 서버 구축
- **Lombok**: Annotation기반 Syntax sugar용
- **Gradle**: 의존성 관리 및 빌드 도구
- **Github Actions**: 자동 Release 버전 배포

## 주요 기능

- 컨테이너 명령어 최적화 조작
- 컨테이너 이벤트 파싱
- 컨테이너 목록 조회
- 컨테이너 상세 정보 조회
- 컨테이너 장애 이력 조회
- 컨테이너 시작/중지/재시작 등 제어
- 시스템 리소스 모니터링
- 자동 스케일링 정책 관리

## 프로젝트 구조

Frontend → Spring 
           | RestController → Service → Repository → Memory

```
src
├─main                      # 소스 코드
│  ├─java
│  │  └─com
│  │      └─cm2
│  │          ├─collector   # Native Docker 통신 및 데이터 수집 담당 클래스
│  │          │  └─dto      # 통신시 필요한 DTO 클래스
│  │          ├─config      # Spring 설정을 위한 클래스
│  │          ├─controller  # 프론트와 통신을 위한 컨트롤러 클래스
│  │          ├─entity      # 실제 가지고 있을 엔티티 클래스
│  │          │  └─dto      # 프론트와 통신을 위한 DTO 클래스
│  │          ├─repository  # In-memory Repository 클래스
│  │          ├─service     # 서비스 계층 클래스
│  │          └─util        # 단위 계산 등 유틸리티 클래스
│  └─resources              # 필요 리소스
│      ├─static
│      └─templates
└─test
    └─java
        └─com
            └─cm2           # 테스트 코드
```

## 주요 파일 및 역할

- **src/main/resources/application.yml**: Spring Application 설정
- **src/main/java/com/cm2/config/AsyncConfig.java**: 비동기 쓰레드 풀 설정
- **build.gradle**: 의존성 설정

## 설치 및 실행

### 자바 설치 (JDK 17+)
https://openjdk.org


### 실행하고자 하는 CM2 버전 다운로드
curl -LO https://github.com/G-CM2/CM2-Backend/releases/download/[version]/CM2-Backend.jar


### 개발 서버 실행
```bash
java -jar CM2-Backend.jar
```
