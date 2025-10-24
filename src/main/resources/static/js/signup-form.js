(() => {
    const $  = (s, ctx=document) => ctx.querySelector(s);
    const $$ = (s, ctx=document) => Array.from(ctx.querySelectorAll(s));

    const form   = $('#signupForm');
    const emailLocal = $('#emailLocal');
    const emailDomain= $('#emailDomain');
    const emailSel   = $('#emailDomainSel');
    const emailFull  = $('#emailFull');
    const emailHint  = $('#emailHint');
    const btnEmailCheck = $('#btnEmailCheck');

    const pw     = $('#pw');
    const pw2    = $('#pw2');
    const pwHint = $('#pwHint');
    const pw2Hint= $('#pw2Hint');

    const agreeAll   = $('#agreeAll');
    const agreeItems = $$('.agree-item');

    let emailAvailable = null; // null=미확인, true=사용가능, false=중복

    const composeEmail = () => {
        const l = (emailLocal?.value || "").trim();
        const d = (emailDomain?.value || "").trim();
        const v = (l && d) ? `${l}@${d}`.toLowerCase() : '';
        if (emailFull) emailFull.value = v;
        return v;
    };

    if (emailSel) {
        emailSel.addEventListener('change', () => {
            if (emailSel.value) {
                emailDomain.value = emailSel.value;
                emailDomain.setAttribute('readonly','readonly');
            } else {
                emailDomain.removeAttribute('readonly');
                emailDomain.value = '';
            }
            composeEmail();
            resetEmailCheck();
        });
    }

    [emailLocal, emailDomain].forEach(el => el && el.addEventListener('input', () => {
        composeEmail();
        resetEmailCheck();
    }));

    function resetEmailCheck(){
        emailAvailable = null;
        if (!emailHint) return;
        emailHint.textContent = '';
        emailHint.classList.remove('success','danger');
    }

    async function checkEmailDup() {
        let email = composeEmail();
        if (!email) {
            if (emailHint) {
                emailHint.textContent = '이메일을 입력해 주세요.';
                emailHint.classList.add('danger'); emailHint.classList.remove('success');
            }
            emailAvailable = null;
            return null;
        }
        try {
            // ★ 컨트롤러 매핑: /api/members/email/check → { email: "...", available: true|false }
            const res = await fetch(`/api/members/email/check?email=${encodeURIComponent(email)}`, {
                method: 'GET',
                headers: { 'Accept': 'application/json' }
            });
            if (!res.ok) throw new Error('fail');
            const data = await res.json();

            emailAvailable = !!data.available;

            if (emailHint) {
                if (emailAvailable) {
                    emailHint.textContent = '사용 가능한 이메일입니다.';
                    emailHint.classList.add('success'); emailHint.classList.remove('danger');
                } else {
                    emailHint.textContent = '이미 사용 중인 이메일입니다.';
                    emailHint.classList.add('danger'); emailHint.classList.remove('success');
                }
            }
            return emailAvailable;
        } catch (e) {
            if (emailHint) {
                emailHint.textContent = '중복확인에 실패했습니다. 잠시 후 다시 시도해 주세요.';
                emailHint.classList.add('danger'); emailHint.classList.remove('success');
            }
            emailAvailable = null;
            return null;
        }
    }

    btnEmailCheck && btnEmailCheck.addEventListener('click', checkEmailDup);

    function updatePwHint(){
        if (!pw || !pwHint) return;
        const ok = pw.value.length >= 8;
        pwHint.textContent = ok ? '좋아요! 8자 이상입니다.' : '비밀번호를 8자 이상 입력.';
        pwHint.classList.toggle('success', ok);
        pwHint.classList.toggle('danger', !ok);
    }
    function updatePw2Hint(){
        if (!pw || !pw2 || !pw2Hint) return;
        const hasLen = pw2.value.length >= 1;
        const ok = pw.value.length >= 8 && pw.value === pw2.value;
        pw2Hint.textContent = hasLen ? (ok ? '비밀번호가 일치합니다.' : '비밀번호가 일치하지 않습니다.') : '비밀번호를 확인해주세요.';
        pw2Hint.classList.toggle('success', ok);
        pw2Hint.classList.toggle('danger', !ok);
    }
    pw && pw.addEventListener('input', () => { updatePwHint(); updatePw2Hint(); });
    pw2 && pw2.addEventListener('input', updatePw2Hint);
    updatePwHint(); updatePw2Hint();

    agreeAll && agreeAll.addEventListener('change', () => { agreeItems.forEach(chk => chk.checked = agreeAll.checked); });
    agreeItems.forEach(chk => chk.addEventListener('change', () => {
        if (agreeAll) agreeAll.checked = agreeItems.every(c => c.checked);
    }));

    form && form.addEventListener('submit', async (e) => {
        e.preventDefault(); // JS 검증 후 통과 시 실제 submit()

        const email = composeEmail();
        const errors = [];
        if (!email) errors.push('이메일을 입력해 주세요.');
        if (pw && pw.value.length < 8) errors.push('비밀번호는 8자 이상이어야 합니다.');
        if (pw && pw2 && pw.value !== pw2.value) errors.push('비밀번호가 일치하지 않습니다.');
        if (!$('#agreeTerms')?.checked || !$('#agreePrivacy')?.checked) errors.push('필수 약관에 동의해 주세요.');

        if (emailAvailable === null) await checkEmailDup();
        if (emailAvailable === false) errors.push('이미 사용 중인 이메일입니다.');

        if (errors.length) { alert(errors[0]); return; }

        // 서버로 전송 → /members/join → redirect:/signup/success
        form.submit();
    });
})();