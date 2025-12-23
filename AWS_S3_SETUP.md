# AWS S3 자격 증명 설정 가이드

## 1. AWS S3 버킷 생성 및 IAM 사용자 설정

### 1-1. AWS 콘솔에서 S3 버킷 생성
1. AWS 콘솔 (https://console.aws.amazon.com) 로그인
2. S3 서비스로 이동
3. "버킷 만들기" 클릭
4. 버킷 이름 입력 (예: `green-university-uploads`)
5. 리전 선택: `아시아 태평양(서울) ap-northeast-2`
6. 버킷 만들기 완료

### 1-2. IAM 사용자 생성 및 Access Key 발급
1. AWS 콘솔 → IAM 서비스로 이동
2. "사용자" → "사용자 추가" 클릭
3. 사용자 이름 입력 (예: `s3-upload-user`)
4. "액세스 유형" 선택:
   - ✅ 프로그래밍 방식 액세스 체크
5. "권한 설정" → "기존 정책 직접 연결"
6. 정책 검색 및 선택:
   - `AmazonS3FullAccess` (또는 필요한 권한만 가진 커스텀 정책)
7. 사용자 생성 완료
8. **중요**: Access Key ID와 Secret Access Key를 안전하게 저장
   - ⚠️ Secret Access Key는 이 창을 닫으면 다시 볼 수 없습니다!

## 2. 자격 증명 설정 방법

### 방법 1: application.yml에 직접 입력 (로컬 개발용)

`GreenUniversity_Backend/src/main/resources/application.yml` 파일을 열고:

```yaml
aws:
  s3:
    bucket-name: green-university-uploads  # 생성한 버킷 이름
    region: ap-northeast-2
    access-key: AKIAIOSFODNN7EXAMPLE        # 발급받은 Access Key ID
    secret-key: wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY  # 발급받은 Secret Access Key
    base-url: https://green-university-uploads.s3.ap-northeast-2.amazonaws.com
```

⚠️ **주의사항**:
- 이 방법은 **로컬 개발 환경에서만** 사용하세요
- Git에 커밋하면 안 됩니다! (`.gitignore`에 추가 권장)
- 프로덕션 환경에서는 절대 사용하지 마세요

### 방법 2: 환경 변수 사용 (권장)

#### Windows (PowerShell)
```powershell
$env:AWS_S3_BUCKET_NAME="green-university-uploads"
$env:AWS_REGION="ap-northeast-2"
$env:AWS_ACCESS_KEY="AKIAIOSFODNN7EXAMPLE"
$env:AWS_SECRET_KEY="wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY"
```

#### Windows (CMD)
```cmd
set AWS_S3_BUCKET_NAME=green-university-uploads
set AWS_REGION=ap-northeast-2
set AWS_ACCESS_KEY=AKIAIOSFODNN7EXAMPLE
set AWS_SECRET_KEY=wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY
```

#### Linux/Mac
```bash
export AWS_S3_BUCKET_NAME=green-university-uploads
export AWS_REGION=ap-northeast-2
export AWS_ACCESS_KEY=AKIAIOSFODNN7EXAMPLE
export AWS_SECRET_KEY=wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY
```

#### IntelliJ IDEA에서 환경 변수 설정
1. Run → Edit Configurations
2. Application → Your Application 선택
3. Environment variables 클릭
4. 다음 변수 추가:
   - `AWS_S3_BUCKET_NAME=green-university-uploads`
   - `AWS_REGION=ap-northeast-2`
   - `AWS_ACCESS_KEY=your-access-key`
   - `AWS_SECRET_KEY=your-secret-key`

### 방법 3: EC2 배포 시 환경 변수 설정

#### EC2 인스턴스에 환경 변수 설정
```bash
# ~/.bashrc 또는 ~/.profile에 추가
export AWS_S3_BUCKET_NAME=green-university-uploads
export AWS_REGION=ap-northeast-2
export AWS_ACCESS_KEY=your-access-key
export AWS_SECRET_KEY=your-secret-key
```

#### 또는 systemd 서비스 파일에 추가
```ini
[Service]
Environment="AWS_S3_BUCKET_NAME=green-university-uploads"
Environment="AWS_REGION=ap-northeast-2"
Environment="AWS_ACCESS_KEY=your-access-key"
Environment="AWS_SECRET_KEY=your-secret-key"
```

## 3. 보안 권장 사항

1. **Access Key는 절대 Git에 커밋하지 마세요**
   - `.gitignore`에 `application-local.yml` 추가
   - 환경 변수 사용 권장

2. **최소 권한 원칙**
   - 전체 S3 접근 권한 대신, 특정 버킷만 접근 가능한 정책 생성 권장
   ```json
   {
     "Version": "2012-10-17",
     "Statement": [
       {
         "Effect": "Allow",
         "Action": [
           "s3:PutObject",
           "s3:GetObject",
           "s3:DeleteObject"
         ],
         "Resource": "arn:aws:s3:::green-university-uploads/*"
       }
     ]
   }
   ```

3. **정기적으로 Access Key 로테이션**
   - 90일마다 새로운 Access Key 발급 및 교체

## 4. 테스트

애플리케이션 실행 후 다음 로그가 나오면 성공:
- "파일 업로드 성공: ..." (S3Service 로그)

에러가 발생하면:
- Access Key와 Secret Key가 올바른지 확인
- 버킷 이름과 리전이 올바른지 확인
- IAM 사용자에게 S3 권한이 있는지 확인

