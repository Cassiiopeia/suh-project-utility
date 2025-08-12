# GitHub 이슈 코멘트 자동화 가이드

## 개요
이 문서는 GitHub 레포지토리에 이슈가 생성되거나 재오픈될 때 자동으로 브랜치명과 커밋 메시지 가이드를 이슈 댓글로 작성하는 워크플로우를 설명합니다.

## 작동 방식
1. 이슈가 생성되거나 재오픈되면 GitHub Actions 워크플로우가 실행됩니다.
2. 워크플로우는 이슈 정보를 Suh Project Utility API로 전송합니다.
3. API는 레포지토리 허용 여부를 확인하고, 허용된 경우 브랜치명과 커밋 메시지를 생성합니다.
4. 워크플로우는 API 응답의 마크다운 댓글을 이슈에 게시합니다.

## 사전 요구사항
1. 레포지토리가 Suh Project Utility 시스템에 등록되어 있어야 합니다.
   - 적어도 한 번 Suh Project Utility 웹 UI에서 해당 레포지토리의 이슈 URL을 사용하여 브랜치/커밋 가이드를 생성해본 적이 있어야 합니다.
2. GitHub Actions가 레포지토리에서 활성화되어 있어야 합니다.

## 워크플로우 파일 설정 방법

`.github/workflows/PROJECT-ISSUE-COMMENT.yaml` 파일을 다음과 같이 생성하세요:

```yaml
name: 이슈 브랜치/커밋 가이드 자동 댓글

on:
  issues:
    types: [opened, reopened]

jobs:
  add-comment:
    runs-on: ubuntu-latest
    permissions:
      issues: write
    
    steps:
      - name: Checkout repository
        uses: actions/checkout@v3
        
      - name: Get Issue information
        id: issue-info
        run: |
          echo "ISSUE_URL=${{ github.event.issue.html_url }}" >> $GITHUB_ENV
      
      - name: Call Suh Project Utility API
        id: api-call
        uses: fjogeleit/http-request-action@v1
        with:
          url: 'https://lab.suhsaechan.me/api/issue-helper/create/commit-branch/github-workflow'
          method: 'POST'
          contentType: 'multipart/form-data'
          customHeaders: '{"Accept": "application/json"}'
          data: |
            {
              "issueUrl": "${{ env.ISSUE_URL }}"
            }
            
      - name: Check API response
        id: check-response
        run: |
          if [ "${{ steps.api-call.outputs.status }}" == "200" ]; then
            echo "API call successful"
            # 응답에서 commentMarkdown 필드 추출
            MARKDOWN=$(echo '${{ steps.api-call.outputs.response }}' | jq -r '.commentMarkdown // empty')
            
            if [ -n "$MARKDOWN" ]; then
              echo "COMMENT_BODY=$MARKDOWN" >> $GITHUB_ENV
            else
              # fallback - 필요한 정보를 수동으로 구성
              BRANCH=$(echo '${{ steps.api-call.outputs.response }}' | jq -r '.branchName // "브랜치명을 가져올 수 없습니다"')
              COMMIT=$(echo '${{ steps.api-call.outputs.response }}' | jq -r '.commitMessage // "커밋 메시지를 가져올 수 없습니다"')
              
              COMMENT_MD="<!-- 이 댓글은 SUH Project Utility에 의해 자동으로 생성되었습니다. - https://lab.suhsaechan.me -->\n\n## 🛠️ 브랜치/커밋 가이드\n\n### 브랜치\n\`\`\`\n$BRANCH\n\`\`\`\n\n### 커밋 메시지\n\`\`\`\n$COMMIT\n\`\`\`\n\n<!-- 이 댓글은 SUH Project Utility에 의해 자동으로 생성되었습니다. - https://lab.suhsaechan.me -->"
              
              echo "COMMENT_BODY<<EOF" >> $GITHUB_ENV
              echo -e "$COMMENT_MD" >> $GITHUB_ENV
              echo "EOF" >> $GITHUB_ENV
            fi
          else
            echo "API call failed with status ${{ steps.api-call.outputs.status }}"
            echo "COMMENT_BODY=API 호출에 실패했습니다. 레포지토리가 허용 목록에 있는지 확인하세요." >> $GITHUB_ENV
          fi
      
      - name: Add comment to Issue
        uses: actions/github-script@v6
        with:
          github-token: ${{ secrets.GITHUB_TOKEN }}
          script: |
            const commentBody = process.env.COMMENT_BODY;
            if (!commentBody) {
              core.setFailed('댓글 내용이 없습니다.');
              return;
            }
            
            github.rest.issues.createComment({
              issue_number: context.issue.number,
              owner: context.repo.owner,
              repo: context.repo.repo,
              body: commentBody
            });
```

## 보안 및 허용 정책
- 워크플로우 API는 허용된 레포지토리만 처리합니다.
- 레포지토리 허용 여부는 다음과 같이 결정됩니다:
  1. 웹 UI에서 최소한 한 번 이상 해당 레포지토리의 이슈에 대한 브랜치/커밋 가이드를 생성하면 자동으로 허용 목록에 추가됩니다.
  2. 시스템 관리자가 명시적으로 레포지토리를 허용/차단할 수 있습니다.

## 문제 해결
- 워크플로우가 `403 Forbidden` 오류를 반환하는 경우:
  - 해당 레포지토리가 허용 목록에 등록되어 있지 않습니다.
  - 최소한 한 번 웹 UI(https://lab.suhsaechan.me)에서 해당 레포지토리의 이슈 URL을 사용하여 브랜치/커밋 가이드를 생성해보세요.
  
- 워크플로우가 실행되지만 이슈에 댓글이 추가되지 않는 경우:
  - GitHub 워크플로우 로그를 확인하세요.
  - API 응답 형식을 확인하세요.
  - API가 commentMarkdown 필드를 반환하는지 확인하세요.

## 디버깅 팁
워크플로우에 문제가 있을 경우 다음과 같은 방법으로 디버깅할 수 있습니다:

1. GitHub Actions 로그를 확인합니다.
2. `Debug API Response` 단계에서 API 응답을 확인합니다.
3. 필요한 경우 워크플로우 파일에 추가적인 디버깅 단계를 추가합니다.

## 향후 개선 계획
- 웹 UI를 통한 레포지토리 허용 관리 화면 추가
- 커스텀 댓글 템플릿 지원
- 이슈 라벨에 따라 다른 메시지 템플릿 제공
