document.addEventListener('DOMContentLoaded', function () {
    const form = document.getElementById('withdrawForm');
    if (!form) return;

    form.addEventListener('submit', function (e) {
        const pw = form.querySelector('input[name="password"]').value.trim();
        const agree = form.querySelector('#agreeCheck').checked;
        const confirmText = document.getElementById('confirmText').value.trim();

        if (!pw) {
            alert('현재 비밀번호를 입력하세요.');
            e.preventDefault(); // ❗ 잘못된 경우만 막음
            return;
        }
        if (!agree) {
            alert('안내사항 동의가 필요합니다.');
            e.preventDefault();
            return;
        }
        if (confirmText !== '회원탈퇴') {
            alert('확인 문구를 정확히 입력하세요: 회원탈퇴');
            e.preventDefault();
            return;
        }

        // ✅ 조건 다 맞으면 그냥 서버로 submit (컨트롤러 실행됨)
        // e.preventDefault() 안 걸리니까 form.submit() 자동 실행됨
    });
});