(() => {
    const $  = (s, ctx=document) => ctx.querySelector(s);

    const form   = $('#socialJoinForm');
    const pw     = $('#pw');
    const pw2    = $('#pw2');
    const pwHint = $('#pwHint');
    const pw2Hint= $('#pw2Hint');

    function updatePwHint(){
        if (!pw || !pwHint) return;
        const ok = (pw.value || '').length >= 8;
        pwHint.textContent = ok ? '좋아요! 8자 이상입니다.' : '비밀번호를 8자 이상 입력.';
        pwHint.classList.toggle('success', ok);
        pwHint.classList.toggle('danger', !ok);
    }
    function updatePw2Hint(){
        if (!pw || !pw2 || !pw2Hint) return;
        const a = (pw.value || '');
        const b = (pw2.value || '');
        const touched = b.length >= 1;
        const ok = a.length >= 8 && a === b;
        pw2Hint.textContent = touched ? (ok ? '비밀번호가 일치합니다.' : '비밀번호가 일치하지 않습니다.') : '비밀번호를 확인해주세요.';
        pw2Hint.classList.toggle('success', ok);
        pw2Hint.classList.toggle('danger', !ok);
    }
    pw && pw.addEventListener('input', () => { updatePwHint(); updatePw2Hint(); });
    pw2 && pw2.addEventListener('input', updatePw2Hint);
    updatePwHint(); updatePw2Hint();

    // 휴대폰 숫자만
    const phone = form ? form.querySelector('input[name="phone"]') : null;
    phone && phone.addEventListener('input', () => {
        phone.value = phone.value.replace(/[^0-9]/g, '').slice(0, 13);
    });

    form && form.addEventListener('submit', (e) => {
        const errs = [];
        if (pw && pw.value.length < 8) errs.push('비밀번호는 8자 이상이어야 합니다.');
        if (pw && pw2 && pw.value !== pw2.value) errs.push('비밀번호가 일치하지 않습니다.');

        if (errs.length) {
            e.preventDefault();
            alert(errs[0]);
            return;
        }
        // 그대로 submit → 서버 DTO 검증 + 저장
    });
})();