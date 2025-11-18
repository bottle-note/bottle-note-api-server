# 운영 환경 GitOps 마이그레이션 계획

> **업데이트**: 2025-11-18 - Dev와 완전히 동일한 구조로 마이그레이션 완료

## ✅ 수정 완료 사항

### 해결된 주요 문제들
1. **빌드 아규먼트 누락 문제 해결**
   - GIT_COMMIT, GIT_BRANCH, BUILD_TIME 추가
   - `/api/v1/app-info` 엔드포인트가 정상적으로 커밋 정보 반환

2. **ECR Token 네임스페이스 문제 해결**
   - base/ecr-token-renew-cronjob.yaml의 namespace 하드코딩 제거
   - Kustomize overlay가 자동으로 네임스페이스 적용

3. **Dev와 완전 동일한 구조로 통일**
   - GitHub Secrets 직접 사용 방식으로 변경
   - Short SHA 사용 (7자리)
   - 동일한 워크플로우 구조

### 수정된 파일 목록
- ✅ `.github/workflows/deploy_production.yml.bak` (백업 생성)
- ✅ `.github/workflows/deploy_production.yml` (전면 재작성)
- ✅ `git.environment-variables/deploy/overlays/production/kustomization.yaml` (images 섹션 추가)
- ✅ `git.environment-variables/deploy/overlays/production/product-api-patch.yaml` (하드코딩 이미지 제거)
- ✅ `git.environment-variables/deploy/base/ecr-token-renew-cronjob.yaml` (네임스페이스 제거)

---

## 📊 현황 분석

### 개발 환경 (현재 상태)
```
✅ GitOps 방식 (ArgoCD 기반)

PR 머지 (dev 브랜치)
    ↓
GitHub Actions: deploy_development.yml
    ├─ Job 1: build-and-push
    │   ├─ Docker 이미지 빌드
    │   ├─ ECR 푸시 (development_{short-sha}, development_latest)
    │   └─ GitHub Actions 캐시 활용
    │
    └─ Job 2: update-kustomize-tag
        ├─ environment-variables 서브모듈로 이동
        ├─ Kustomize 이미지 태그 업데이트
        └─ Git 커밋 및 푸시 (main 브랜치)
    ↓
environment-variables 저장소 업데이트
    ↓
ArgoCD 자동 감지 (3분 이내)
    ↓
Kubernetes 클러스터 자동 배포
```

### 운영 환경 (현재 상태)
```
⚠️  레거시 직접 배포 방식

PR 머지 (prod 브랜치)
    ↓
GitHub Actions: deploy_production.yml
    ├─ Job 1: build-and-push-ecr
    │   ├─ Docker 이미지 빌드
    │   └─ ECR 푸시 (production_{full-sha}, production_latest)
    │
    └─ Job 2: rolling-deploy
        ├─ SSH로 Oracle 서버 접속 (2대)
        ├─ Docker Compose로 직접 배포
        └─ Health Check
    ↓
Oracle 서버에 Docker 컨테이너 실행

문제점:
1. ArgoCD 인프라는 구축되어 있으나 활용되지 않음
2. Kustomize 설정이 development_latest를 잘못 참조
3. GitOps 패턴이 적용되지 않음
4. 수동 개입 가능성이 높음
```

---

## 🎯 마이그레이션 목표

### 운영 환경을 개발 환경과 동일한 GitOps 방식으로 전환

**기대 효과**:
- 배포 프로세스 일관성 확보
- GitOps 선언적 배포로 안정성 향상
- ArgoCD Self-Healing으로 자동 복구
- 배포 이력 추적 용이 (Git 기반)
- 수동 개입 최소화

---

## 📝 작업 포인트 상세

### 1. GitHub Actions 워크플로우 수정

#### 파일: `.github/workflows/deploy_production.yml`

**1-1. 기존 파일 백업**
```bash
# 레거시 배포 방식 보존
cp .github/workflows/deploy_production.yml .github/workflows/deploy_production.yml.bak
```

**1-2. build-and-push-ecr job 수정**

**변경 전**:
```yaml
tags: |
  ${{ steps.login-ecr.outputs.registry }}/${{ env.ECR_REPOSITORY }}:production_${{ github.sha }}
  ${{ steps.login-ecr.outputs.registry }}/${{ env.ECR_REPOSITORY }}:production_latest
```

**변경 후**:
```yaml
# 개발 환경과 동일하게 short SHA 사용
- name: generate short commit sha
  id: short-sha
  run: echo "sha=${GITHUB_SHA:0:7}" >> $GITHUB_OUTPUT

- name: build and push prod image to ecr
  uses: docker/build-push-action@v6
  with:
    tags: |
      ${{ steps.login-ecr.outputs.registry }}/${{ env.ECR_REPOSITORY }}:production_${{ steps.short-sha.outputs.sha }}
      ${{ steps.login-ecr.outputs.registry }}/${{ env.ECR_REPOSITORY }}:production_latest
```

**1-3. update-kustomize-tag job 추가**

```yaml
update-kustomize-tag:
  needs: build-and-push-ecr
  runs-on: ubuntu-latest
  steps:
    - name: checkout code with submodules
      uses: actions/checkout@v4
      with:
        submodules: true
        token: ${{ secrets.GIT_ACCESS_TOKEN }}

    - name: setup kustomize
      uses: imranismail/setup-kustomize@v2
      with:
        github-token: ${{ secrets.GITHUB_TOKEN }}

    - name: update image tag with kustomize
      run: |
        COMMIT_SHA="${{ github.sha }}"
        IMAGE_TAG="production_${COMMIT_SHA:0:7}"

        echo "📦 업데이트할 이미지 태그: $IMAGE_TAG"

        cd git.environment-variables

        # detached HEAD 해결: main 브랜치로 전환
        git checkout main

        cd deploy/overlays/production

        # Kustomize로 이미지 태그 업데이트
        kustomize edit set image \
          536697247604.dkr.ecr.ap-northeast-2.amazonaws.com/ecr-bottle-note-api=536697247604.dkr.ecr.ap-northeast-2.amazonaws.com/ecr-bottle-note-api:${IMAGE_TAG}

        echo "✅ kustomization.yaml 업데이트 완료"
        git diff kustomization.yaml

    - name: commit and push to environment-variables
      run: |
        cd git.environment-variables

        git config user.name "github-actions[bot]"
        git config user.email "github-actions[bot]@users.noreply.github.com"

        git add deploy/overlays/production/kustomization.yaml

        git commit -m "chore(prod): update image tag to ${GITHUB_SHA:0:7}

        Updated by GitHub Actions
        Commit: ${{ github.sha }}
        Branch: ${{ github.ref_name }}
        Workflow: ${{ github.workflow }}"

        git push origin main

        echo "✅ Git 커밋 및 푸시 완료"
        echo "🔄 ArgoCD가 변경사항을 감지하여 자동 배포를 시작합니다"
```

**1-4. rolling-deploy job 제거**

```yaml
# 레거시 방식 제거 (ArgoCD로 완전 전환)
# 이전 rolling-deploy job은 .bak 파일 참조
```

**1-5. 환경 변수 간소화**

**변경 전**:
```yaml
env:
  ORACLE_SSH_KEY_FILE: git.environment-variables/deploy/oracle/keys/product-cluster-ssh-key.pem
  AWS_ECR_ENV_FILE: git.environment-variables/deploy/aws/ecr-properties.env
  APP_ENV_FILE: git.environment-variables/application.springboot/prod.env
  ECR_REPOSITORY: ecr-bottle-note-api
```

**변경 후**:
```yaml
env:
  ECR_REGION: ap-northeast-2
  ECR_REGISTRY: 536697247604.dkr.ecr.ap-northeast-2.amazonaws.com
  ECR_REPOSITORY: ecr-bottle-note-api
```

**1-6. AWS 인증 간소화**

```yaml
- name: configure aws credentials
  uses: aws-actions/configure-aws-credentials@v2
  with:
    aws-access-key-id: ${{ secrets.AWS_ACCESS_KEY_ID }}
    aws-secret-access-key: ${{ secrets.AWS_SECRET_ACCESS_KEY }}
    aws-region: ${{ env.ECR_REGION }}
```

---

### 2. Kustomize 설정 수정

#### 파일: `git.environment-variables/deploy/overlays/production/product-api-patch.yaml`

**2-1. 잘못된 이미지 태그 수정**

**변경 전** (Line 26-27):
```yaml
# TODO: production_latest 이미지 빌드 후 태그 변경 필요
image: 536697247604.dkr.ecr.ap-northeast-2.amazonaws.com/ecr-bottle-note-api:development_latest
```

**변경 후**:
```yaml
# GitHub Actions에서 Kustomize로 자동 업데이트됨
imagePullPolicy: Always
```

> 주의: image 필드는 base에 정의되어 있으므로 patch에서는 제거

---

#### 파일: `git.environment-variables/deploy/overlays/production/kustomization.yaml`

**2-2. images 섹션 추가**

**변경 전**:
```yaml
apiVersion: kustomize.config.k8s.io/v1beta1
kind: Kustomization

namespace: bottlenote-production

resources:
  - ../../base

patches:
  - path: product-api-patch.yaml
  - path: redis-patch.yaml

generators:
  - secrets-generator.yaml
```

**변경 후**:
```yaml
apiVersion: kustomize.config.k8s.io/v1beta1
kind: Kustomization

namespace: bottlenote-production

resources:
  - ../../base

patches:
  - path: product-api-patch.yaml
  - path: redis-patch.yaml

generators:
  - secrets-generator.yaml

# 이미지 태그 관리 (GitHub Actions에서 자동 업데이트)
images:
  - name: 536697247604.dkr.ecr.ap-northeast-2.amazonaws.com/ecr-bottle-note-api
    newName: 536697247604.dkr.ecr.ap-northeast-2.amazonaws.com/ecr-bottle-note-api
    newTag: production_latest
```

---

### 3. ArgoCD Application 확인

#### 파일: `git.environment-variables/deploy/overlays/production/bottlenote-production.yaml`

**현재 상태 확인** (수정 불필요 - 이미 올바르게 설정됨):
```yaml
apiVersion: argoproj.io/v1alpha1
kind: Application
metadata:
  name: bottlenote-production
  namespace: argocd
spec:
  project: bottlenote
  source:
    repoURL: https://github.com/bottle-note/environment-variables
    targetRevision: main
    path: deploy/overlays/production
  destination:
    server: https://kubernetes.default.svc
    namespace: bottlenote-production
  syncPolicy:
    automated:
      prune: true        # ✅ 삭제된 리소스 자동 정리
      selfHeal: true     # ✅ 수동 변경 자동 복구
    syncOptions:
      - CreateNamespace=true
```

---

## 🔄 마이그레이션 실행 순서

### Phase 1: 준비 작업

1. **백업 생성**
   ```bash
   # GitHub Actions 워크플로우 백업
   cp .github/workflows/deploy_production.yml \
      .github/workflows/deploy_production.yml.bak
   ```

2. **현재 운영 환경 상태 기록**
   - 현재 실행 중인 컨테이너 정보 저장
   - Docker 이미지 태그 기록
   - 환경 변수 백업

### Phase 2: 코드 수정

3. **파일 수정 (순서대로)**
   1. `git.environment-variables/deploy/overlays/production/kustomization.yaml`
      - images 섹션 추가

   2. `git.environment-variables/deploy/overlays/production/product-api-patch.yaml`
      - 잘못된 이미지 태그 수정

   3. `.github/workflows/deploy_production.yml`
      - build-and-push-ecr job 수정
      - update-kustomize-tag job 추가
      - rolling-deploy job 제거

4. **커밋 및 푸시**
   ```bash
   git add .
   git commit -m "feat: migrate production deployment to GitOps

   - Add Kustomize image tag update to production workflow
   - Remove legacy SSH deployment
   - Align production deployment with development environment
   - Enable ArgoCD automated sync for production"

   git push origin main
   ```

### Phase 3: 검증 및 배포

5. **Kustomize 설정 검증**
   ```bash
   cd git.environment-variables/deploy/overlays/production
   kustomize build .
   ```

6. **워크플로우 수동 실행**
   - GitHub Actions에서 `deploy_production.yml` 수동 트리거
   - 이미지 빌드 및 푸시 확인
   - Kustomize 태그 업데이트 확인

7. **ArgoCD 동기화 확인**
   - ArgoCD UI에서 bottlenote-production 상태 확인
   - 자동 동기화 시작 여부 확인
   - 배포 진행 상황 모니터링

8. **Health Check**
   - Kubernetes Pod 상태 확인
   - Ingress 트래픽 확인
   - 애플리케이션 Health Endpoint 확인

### Phase 4: 모니터링

9. **배포 후 모니터링 (최소 1시간)**
   - Pod 로그 확인
   - 에러 발생 여부 확인
   - 성능 지표 확인

10. **Oracle 서버 정리 (배포 안정화 후)**
    - Docker Compose 컨테이너 중지
    - 불필요한 이미지 정리
    - 디스크 공간 확보

---

## ⚠️ 롤백 계획

### 문제 발생 시 롤백 절차

**시나리오 1: GitHub Actions 단계에서 실패**
```bash
# 백업 파일로 복원
cp .github/workflows/deploy_production.yml.bak \
   .github/workflows/deploy_production.yml

git add .github/workflows/deploy_production.yml
git commit -m "revert: rollback production deployment to legacy method"
git push origin main
```

**시나리오 2: ArgoCD 동기화 실패**
```bash
# ArgoCD에서 수동으로 이전 버전으로 롤백
kubectl -n argocd get application bottlenote-production
argocd app rollback bottlenote-production {이전 리비전 번호}
```

**시나리오 3: 배포 후 애플리케이션 오류**
```bash
# 1. ArgoCD 자동 동기화 일시 중지
argocd app set bottlenote-production --sync-policy none

# 2. Kustomize 이미지 태그를 이전 버전으로 수동 변경
cd git.environment-variables/deploy/overlays/production
kustomize edit set image \
  536697247604.dkr.ecr.ap-northeast-2.amazonaws.com/ecr-bottle-note-api=\
  536697247604.dkr.ecr.ap-northeast-2.amazonaws.com/ecr-bottle-note-api:{이전태그}

# 3. 커밋 및 푸시
git add kustomization.yaml
git commit -m "revert: rollback to previous image tag"
git push origin main

# 4. ArgoCD 동기화 재개
argocd app set bottlenote-production --sync-policy automated
```

**긴급 상황: 레거시 방식으로 즉시 복귀**
```bash
# 1. 백업 워크플로우 복원
cp .github/workflows/deploy_production.yml.bak \
   .github/workflows/deploy_production.yml

# 2. 수동으로 워크플로우 실행
# GitHub Actions UI에서 deploy_production.yml 실행

# 3. Oracle 서버에 직접 배포됨
```

---

## 📊 수정 파일 체크리스트

### bottle-note-api-server 저장소

- [ ] `.github/workflows/deploy_production.yml` 수정
  - [ ] 환경 변수 간소화
  - [ ] build-and-push-ecr job 수정 (short SHA)
  - [ ] update-kustomize-tag job 추가
  - [ ] rolling-deploy job 제거
- [ ] `.github/workflows/deploy_production.yml.bak` 생성 (백업)

### environment-variables 저장소 (서브모듈)

- [ ] `deploy/overlays/production/kustomization.yaml` 수정
  - [ ] images 섹션 추가
  - [ ] 초기 태그를 production_latest로 설정
- [ ] `deploy/overlays/production/product-api-patch.yaml` 수정
  - [ ] 잘못된 development_latest 이미지 태그 제거
  - [ ] TODO 주석 제거

---

## 🎯 성공 지표

마이그레이션이 성공적으로 완료되었는지 확인하는 지표:

1. **GitHub Actions**
   - ✅ PR 머지 시 워크플로우 자동 실행
   - ✅ Docker 이미지 빌드 및 ECR 푸시 성공
   - ✅ Kustomize 태그 업데이트 및 커밋 성공

2. **Git 저장소**
   - ✅ environment-variables의 kustomization.yaml 자동 업데이트됨
   - ✅ 커밋 메시지에 github-actions[bot] 표시

3. **ArgoCD**
   - ✅ 변경사항 자동 감지 (3분 이내)
   - ✅ Synced 상태 유지
   - ✅ Healthy 상태 유지

4. **Kubernetes**
   - ✅ Pod가 새 이미지로 업데이트됨
   - ✅ Rolling Update 성공
   - ✅ Health Check 통과

5. **애플리케이션**
   - ✅ /api/v1/app-info 엔드포인트 정상 응답
   - ✅ 배포된 커밋 해시 일치
   - ✅ 서비스 정상 동작

---

## 📚 참고 자료

### 개발 환경 워크플로우
- 파일: `.github/workflows/deploy_development.yml`
- 이 워크플로우를 참고하여 운영 환경 수정

### Kustomize 문서
- 공식 문서: https://kustomize.io/
- Image Transformer: https://kubectl.docs.kubernetes.io/references/kustomize/kustomization/images/

### ArgoCD 문서
- 공식 문서: https://argo-cd.readthedocs.io/
- GitOps: https://argo-cd.readthedocs.io/en/stable/user-guide/best_practices/

---

## 📞 문제 해결

### 자주 발생할 수 있는 문제

**Q: Kustomize가 이미지 태그를 업데이트하지 못함**
```bash
# 원인: kustomization.yaml에 images 섹션이 없음
# 해결: images 섹션 추가 후 재시도
```

**Q: ArgoCD가 변경사항을 감지하지 못함**
```bash
# 원인: 동기화 주기 문제
# 해결: 수동으로 Refresh 클릭 또는 다음 명령 실행
argocd app get bottlenote-production --refresh
```

**Q: Pod가 새 이미지를 Pull하지 못함**
```bash
# 원인: ECR 인증 만료
# 해결: ecr-registry-secret 갱신
kubectl delete secret ecr-registry-secret -n bottlenote-production
# ECR token renew cronjob이 자동으로 재생성
```

---

## ✅ 완료 후 확인사항

- [ ] 개발 환경과 운영 환경의 배포 프로세스가 동일함
- [ ] GitOps 패턴이 두 환경 모두에 적용됨
- [ ] Oracle 서버의 레거시 배포 방식 제거됨
- [ ] 모든 배포가 ArgoCD를 통해 이루어짐
- [ ] 배포 히스토리가 Git에 기록됨
- [ ] Self-Healing이 정상 동작함

---

**작성일**: 2025-11-18
**버전**: 1.0
**담당자**: DevOps Team
