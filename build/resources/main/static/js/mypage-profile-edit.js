// /static/js/mypage-profile-edit.js
document.addEventListener('DOMContentLoaded', function () {
    const form = document.getElementById('profileEditForm');

    // ✅ 휴대폰 입력 보정
    const phone =
        (form && form.querySelector('input[name="phone"]')) ||
        document.querySelector('input[name="phone"]');
    if (phone) {
        phone.addEventListener('input', () => {
            phone.value = phone.value.replace(/[^\d-]/g, '').slice(0, 20);
        });
    }

    // ✅ 비밀번호 일치 힌트(긍정만 표시)
    const pwNew =
        (form && form.querySelector('input[name="newPassword"]')) ||
        document.querySelector('input[name="newPassword"]');
    const pwConfirm =
        (form && form.querySelector('input[name="newPasswordConfirm"]')) ||
        document.querySelector('input[name="newPasswordConfirm"]');

    if (pwNew && pwConfirm) {
        // 이미 만들어둔 힌트가 있으면 재사용
        let hint =
            pwConfirm.parentElement.querySelector('.pf-help') ||
            document.createElement('div');
        if (!hint.classList.contains('pf-help')) {
            hint.className = 'pf-help mt-1';
            hint.setAttribute('aria-live', 'polite');
            pwConfirm.parentElement.appendChild(hint);
        }

        const showPositiveOnly = () => {
            const a = (pwNew.value || '').trim();
            const b = (pwConfirm.value || '').trim();

            // 둘 다 비었거나, 확인란이 비어있으면 힌트 숨김
            if (!a && !b) {
                hint.textContent = '';
                hint.classList.remove('text-success');
                return;
            }
            if (!b) {
                hint.textContent = '';
                hint.classList.remove('text-success');
                return;
            }

            // 일치할 때만 긍정 힌트 표시, 불일치면 아무 것도 표시하지 않음(서버 에러에 맡김)
            if (a && b && a === b) {
                hint.textContent = '새 비밀번호가 일치합니다.';
                hint.classList.add('text-success');
            } else {
                hint.textContent = '';
                hint.classList.remove('text-success');
            }
        };

        pwNew.addEventListener('input', showPositiveOnly);
        pwConfirm.addEventListener('input', showPositiveOnly);

        // 제출 시 힌트 제거 → 서버 검증 메시지가 유일하게 보이도록
        if (form) {
            form.addEventListener('submit', () => {
                hint.textContent = '';
                hint.classList.remove('text-success');
            });
        }
    }
});
