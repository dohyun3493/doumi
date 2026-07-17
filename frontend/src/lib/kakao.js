// 카카오 OAuth(Authorization Code) 프론트 설정
// REST API 키는 인가요청 URL의 client_id로 쓰이며 브라우저에 노출되는 값(공개 식별자)입니다.
// 실제 보호는 카카오 콘솔의 Redirect URI 등록 + 백엔드 토큰교환으로 이뤄집니다.
// → 공개값이라 기본값으로 둬서 팀원이 별도 .env 없이 바로 쓸 수 있게 함. (필요 시 VITE_KAKAO_REST_KEY로 override)
export const KAKAO_REST_KEY = import.meta.env.VITE_KAKAO_REST_KEY || ''

// 현재 접속 origin 기준 콜백 주소 (로컬·배포 자동 대응)
// → 카카오 콘솔의 Redirect URI에 동일하게 등록되어 있어야 함
export function kakaoRedirectUri() {
  return `${window.location.origin}/oauth/kakao/callback`
}

// 카카오 로그인 동의 화면으로 보낼 인가요청 URL
export function getKakaoAuthUrl() {
  const params = new URLSearchParams({
    client_id: KAKAO_REST_KEY,
    redirect_uri: kakaoRedirectUri(),
    response_type: 'code',
  })
  return `https://kauth.kakao.com/oauth/authorize?${params.toString()}`
}
