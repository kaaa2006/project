// /js/faqBoard.js
document.addEventListener('DOMContentLoaded', () => {
    const registerBtn = document.querySelector('.btn-register');
    if (!registerBtn) return;

    // 관리자만 버튼이 보이므로 추가 검증이 필요 없지만,
    // 혹시 서버에서 노출 제어가 바뀔 경우 대비
    registerBtn.addEventListener('click', (e) => {
        // 필요 시 로그인 체크/권한 체크 로직 추가
        // e.preventDefault(); alert('권한이 없습니다.'); return;
    });
});
