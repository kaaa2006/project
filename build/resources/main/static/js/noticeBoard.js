document.addEventListener('DOMContentLoaded', () => {
    // 글쓰기 버튼 로그인/권한 체크가 서버에서 처리되므로 별도 JS 없음.
    // (필요 시 dataset.loggedin 등을 붙여 팝업 후 리다이렉트 로직 추가 가능)

    // 행 클릭 시 링크 이동(접근성 고려: a 태그 우선, li 클릭은 보조)
    const list = document.getElementById('notice-list');
    if (list) {
        list.addEventListener('click', (e) => {
            const row = e.target.closest('.notice-item');
            if (!row) return;
            // a 태그 클릭이 이미 발생한 경우는 기본 동작
            if (e.target.tagName.toLowerCase() === 'a') return;

            const link = row.querySelector('.col-title a');
            if (link && link.href) {
                window.location.href = link.href;
            }
        });
    }

    // 페이지네이션 바인딩
    const pager = document.getElementById('notice-pagination');
    if (!pager) return;

    const curr  = parseInt(pager.dataset.currentPage || '1', 10) || 1;
    const total = parseInt(pager.dataset.totalPages || '1', 10) || 1;
    const size  = parseInt(pager.dataset.size || '20', 10) || 20;    const type  = pager.dataset.type || '';
    const keyword = pager.dataset.keyword || '';
    const boardType = 'NOTICE';

    // 이동 함수
    const go = (page) => {
        const params = new URLSearchParams({
            boardType, page: String(page), size: String(size),
            type, keyword
        });
        window.location.href = `/board/list?${params.toString()}`;
    };

    // 1) 공용 renderPagination가 있으면 재사용
    if (typeof window.renderPagination === 'function') {
        window.renderPagination({
            container: pager,
            totalPages: total,
            currentPage: curr,
            onPageClick: (toPage) => {
                if (toPage === curr) return;
                go(toPage);
            }
        });
        return;
    }

    // 2) 폴백 렌더러 (팁/이벤트 페이지와 동일 UX)
    const build = (currentPage, totalPages) => {
        pager.innerHTML = '';
        if (totalPages <= 1) return;

        const liBtn = (label, page, { disabled=false, active=false }={}) => {
            const li = document.createElement('li');
            const btn = document.createElement('button');
            btn.type = 'button';
            btn.className = 'page-btn';
            btn.innerHTML = label;
            if (disabled) btn.classList.add('disabled');
            if (active)   btn.classList.add('active');
            btn.addEventListener('click', () => {
                if (!disabled && !active) go(page);
            });
            li.appendChild(btn);
            return li;
        };
        const liEll = () => {
            const li = document.createElement('li');
            li.className = 'ellipsis';
            li.textContent = '…';
            return li;
        };

        const maxBtns = 7;
        let start = Math.max(1, currentPage - Math.floor(maxBtns / 2));
        let end   = Math.min(totalPages, start + maxBtns - 1);
        start     = Math.max(1, end - maxBtns + 1);

        pager.appendChild(liBtn('‹ 이전', currentPage - 1, { disabled: currentPage === 1 }));

        if (start > 1) {
            pager.appendChild(liBtn('1', 1));
            if (start > 2) pager.appendChild(liEll());
        }

        for (let p = start; p <= end; p++) {
            pager.appendChild(liBtn(String(p), p, { active: p === currentPage }));
        }

        if (end < totalPages) {
            if (end < totalPages - 1) pager.appendChild(liEll());
            pager.appendChild(liBtn(String(totalPages), totalPages));
        }

        pager.appendChild(liBtn('다음 ›', currentPage + 1, { disabled: currentPage === totalPages }));
    };

    build(curr, total);
});
