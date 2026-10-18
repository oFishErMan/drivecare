# 개발 환경 저장 위치

Docker의 컨테이너, 이미지, 빌드 캐시와 MariaDB·Redis 볼륨은 D 드라이브에 저장합니다.

- 실제 데이터 파일: `D:\DockerDesktop\disk\docker_data.vhdx`
- Docker가 사용하는 경로: `C:\Users\WooK\AppData\Local\Docker\wsl\disk`
- 위 C 경로는 실제 D 경로를 가리키는 Windows 디렉터리 정션입니다.
- 프로젝트 소스는 `C:\Spring\_Project\drivecare`에 있습니다.

Compose 설정이나 실행 명령은 바뀌지 않습니다.

```powershell
cd C:\Spring\_Project\drivecare
docker compose up -d
```

Docker Desktop 설정에는 C 경로가 보일 수 있지만 데이터 파일의 실제 저장 위치는 D 드라이브입니다. D 드라이브와 해당 폴더를 삭제하거나 이름을 변경하지 마세요. Docker Desktop의 데이터 초기화나 재설치 후에는 정션과 저장 위치를 다시 확인해야 합니다.

데이터 파일을 백업하거나 이동할 때는 먼저 Compose 서비스를 중지하고 Docker Desktop을 종료해야 합니다. 실행 중인 VHDX를 직접 복사하거나 수정하지 마세요.

```powershell
Get-Item -LiteralPath C:\Users\WooK\AppData\Local\Docker\wsl\disk |
    Select-Object FullName, LinkType, Target
```
