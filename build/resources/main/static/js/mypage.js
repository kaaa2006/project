// mypage.js — 마이페이지 상호작용 (확정판)

console.debug('[mypage] mypage.js loaded');

document.addEventListener('DOMContentLoaded', () => {
    const btn   = document.getElementById('btnGradeBenefit');
    const panel = document.getElementById('grade-benefit-panel');

    if (!btn || !panel) {
        console.warn('[mypage] button/panel not found', { btn: !!btn, panel: !!panel });
        return;
    }

    // 1) 초기 상태 보장 (display:block 강제 → scrollHeight 측정 가능)
    panel.style.display  = 'block';
    panel.style.overflow = 'hidden';
    if (!panel.hasAttribute('aria-hidden')) panel.setAttribute('aria-hidden', 'true');
    if (panel.getAttribute('aria-hidden') === 'true') {
        panel.style.maxHeight = '0px';
        panel.style.opacity   = '0';
    }

    // 2) 전환 종료 시 열린 상태라면 max-height 풀기(auto 효과)
    function onTransitionEnd(e) {
        if (e.propertyName !== 'max-height') return;
        if (panel.getAttribute('aria-hidden') === 'false') {
            panel.style.maxHeight = 'none';
        }
    }
    panel.removeEventListener('transitionend', onTransitionEnd);
    panel.addEventListener('transitionend', onTransitionEnd);

    // 3) 단일 클릭 핸들러(간섭 차단)
    btn.addEventListener('click', (e) => {
        e.preventDefault();
        e.stopPropagation();
        if (typeof e.stopImmediatePropagation === 'function') e.stopImmediatePropagation();

        const opened = panel.getAttribute('aria-hidden') === 'false';

        if (!opened) {
            // 열기
            panel.style.display = 'block'; // 다른 CSS가 숨겼더라도 노출
            panel.setAttribute('aria-hidden', 'false');
            btn.setAttribute('aria-expanded', 'true');

            // 이미 none(=auto)로 풀려 있었으면 픽셀로 전환하여 애니메이션 시작
            if (panel.style.maxHeight === '' || panel.style.maxHeight === 'none') {
                panel.style.maxHeight = '0px';
            }

            // 높이 측정(0 방지용 fallback 400)
            let h = panel.scrollHeight;
            if (!h || h === 0) {
                // 혹시라도 0이면, 자식 높이로 한 번 더 시도
                h = panel.firstElementChild ? panel.firstElementChild.scrollHeight : 0;
            }
            panel.style.maxHeight = (h > 0 ? h : 400) + 'px';
            panel.style.opacity   = '1';
            return;
        }

        // 닫기
        if (panel.style.maxHeight === '' || panel.style.maxHeight === 'none') {
            panel.style.maxHeight = panel.scrollHeight + 'px';
        }
        requestAnimationFrame(() => {
            panel.setAttribute('aria-hidden', 'true');
            btn.setAttribute('aria-expanded', 'false');
            panel.style.maxHeight = '0px';
            panel.style.opacity   = '0';
            // 닫힌 다음에도 display는 block 유지(다음 열림을 위해). 필요 시 아래 주석 해제:
            // panel.style.display = 'block';
        });
    });

    // 4) 리사이즈 시 열린 상태면 높이 재계산
    window.addEventListener('resize', () => {
        if (panel.getAttribute('aria-hidden') === 'false' && panel.style.maxHeight !== 'none') {
            panel.style.maxHeight = panel.scrollHeight + 'px';
        }
    });
});
