// ========================================================
// TIP Board — 목록/등록/수정/상세 공용 JS
// - 목록: 클라이언트 페이지네이션(1p=20)
// - 페이지네이션: /js/pagination.js 시그니처에 맞춰 호출(객체 인자)
// - 데이터가 JSON이 아닐 때(로그인 리다이렉트 등) 콘솔에 원문을 로깅
// ========================================================

(function () {
    // ---------------------------
    // 공용 유틸
    // ---------------------------

    // 한국어 날짜+시간 포맷 (브라우저/형식 호환)
    function formatKoreanDateTime(input) {
        if (!input) return '';
        let d;
        if (typeof input === 'number') {           // epoch(ms) 지원
            d = new Date(input);
        } else {
            // "yyyy-MM-dd HH:mm:ss" 같은 문자열은 Safari가 못 읽으므로 공백→T 치환
            const s = String(input).replace(' ', 'T');
            d = new Date(s);
        }
        if (isNaN(d)) return String(input);        // 파싱 실패 시 원문 노출
        // 24시간제. 오전/오후 표기를 원하면 hour12:true로 바꾸세요.
        return d.toLocaleString('ko-KR', {
            year: 'numeric',
            month: '2-digit',
            day: '2-digit',
            hour: '2-digit',
            minute: '2-digit',
            hour12: false
        });
    }

    function escapeHtml(s) {
        return String(s)
            .replaceAll('&', '&amp;')
            .replaceAll('<', '&lt;')
            .replaceAll('>', '&gt;')
            .replaceAll('"', '&quot;')
            .replaceAll("'", '&#39;');
    }

    // ---------------------------
    // 목록 + 페이지네이션
    // ---------------------------
    const PAGE_SIZE = 20;
    const ulList   = document.getElementById('tip-list');
    const ulPaging = document.getElementById('tip-pagination');
    const btnWrite = document.querySelector('.btn-register');

    // CSRF
    const csrfToken  = document.querySelector('meta[name="_csrf"]')?.content || null;
    const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.content || null;

    let ALL = [];
    let totalPages = 1;

    if (ulList && ulPaging) {
        // 글쓰기 버튼: 로그인 체크
        if (btnWrite) {
            btnWrite.addEventListener('click', (e) => {
                const isLoggedIn = btnWrite.dataset.loggedin === 'true';
                if (!isLoggedIn) {
                    e.preventDefault();
                    alert('로그인이 필요합니다.');
                    location.href = '/login';
                }
            });
        }

        // 제목 클릭 시 조회수 증가 후 이동
        ulList.addEventListener('click', async (e) => {
            const link = e.target.closest('.title-link');
            if (!link) return;
            e.preventDefault();
            try {
                const headers = (csrfHeader && csrfToken) ? { [csrfHeader]: csrfToken } : {};
                await fetch(`/board/tip/increment-view/${link.dataset.bno}`, { method: 'POST', headers });
            } catch (err) {
                // ignore
            } finally {
                location.href = link.href;
            }
        });

        // 전체 목록 가져오기
        async function fetchAllTips() {
            const res = await fetch('/board/tip/list-json', { credentials: 'same-origin' });
            if (!res.ok) throw new Error(`HTTP ${res.status}`);

            const text = await res.text();
            let parsed;
            try {
                parsed = JSON.parse(text);
            } catch (e) {
                console.error('[TIP] list-json 응답이 JSON이 아닙니다. (아마도 로그인/오류 페이지)', { preview: text.slice(0, 300) });
                throw e;
            }

            if (!Array.isArray(parsed)) {
                console.warn('[TIP] list-json이 배열이 아닙니다. 서버 포맷 확인 필요', parsed);
            }
            ALL = Array.isArray(parsed) ? parsed : [];
            totalPages = Math.max(1, Math.ceil(ALL.length / PAGE_SIZE));
        }

        // 쿼리스트링 page
        function getPageFromQuery() {
            const p = new URLSearchParams(location.search).get('page');
            const n = parseInt(p, 10);
            return (Number.isFinite(n) && n > 0) ? n : 1;
        }
        function setPageInQuery(nextPage) {
            const params = new URLSearchParams(location.search);
            params.set('page', String(nextPage));
            history.replaceState(null, '', `?${params}`);
        }

        // 한 페이지 렌더
        function renderPage(page) {
            ulList.querySelectorAll('.tip-item, .tip-empty-row').forEach(el => el.remove());

            if (ALL.length === 0) {
                const empty = document.createElement('li');
                empty.className = 'tip-empty-row';
                empty.innerHTML = `<span class="col-title">등록된 TIP이 없습니다.</span>`;
                ulList.appendChild(empty);
                ulPaging.innerHTML = '';
                return;
            }

            const start = (page - 1) * PAGE_SIZE;
            const rows = ALL.slice(start, start + PAGE_SIZE);

            rows.forEach((item, idx) => {
                const displayNo = ALL.length - (start + idx);

                const li = document.createElement('li');
                li.className = item.writer === '관리자' ? 'tip-item admin-tip' : 'tip-item';

                // ✅ 날짜+시간 표시
                const regDateDisplay = formatKoreanDateTime(item.regDate);

                li.innerHTML = `
          <span class="col-no">${displayNo}</span>
          <span class="col-title">
            <a href="/board/tip/read?bno=${item.bno}" class="title-link" data-bno="${item.bno}">
              ${escapeHtml(item.title || '')}
              ${item.writer === '관리자' ? '<span style="color:#c77600;font-weight:600;">[관리자 글]</span>' : ''}
            </a>
          </span>
          <span class="col-writer">${escapeHtml(item.writer || '')}</span>
          <span class="col-views">${item.viewCount ?? 0}</span>
          <span class="col-like">${item.likeCount ?? 0}</span>
          <span class="col-date">${regDateDisplay}</span>
        `;
                ulList.appendChild(li);
            });

            applyPagination(page, totalPages);
        }

        // 페이지네이션 (공용 pagination.js 시그니처에 맞춤)
        function applyPagination(curr, total) {
            // 공용 함수가 있으면 우선 사용
            if (typeof window.renderPagination === 'function') {
                window.renderPagination({
                    container: ulPaging,
                    totalPages: total,
                    currentPage: curr,
                    onPageClick: (toPage) => {
                        if (toPage === curr) return;
                        setPageInQuery(toPage);
                        renderPage(toPage);
                    }
                });
                return;
            }

            // 없으면 fallback (동일 톤)
            ulPaging.innerHTML = buildFallback(curr, total);
            ulPaging.onclick = (e) => {
                const btn = e.target.closest('a[data-page]');
                if (!btn) return;
                e.preventDefault();
                const to = parseInt(btn.dataset.page, 10);
                if (!Number.isFinite(to) || to === curr) return;
                setPageInQuery(to);
                renderPage(to);
            };
        }

        function buildFallback(curr, total) {
            const maxWindow = 2;
            const items = [];
            const liA = (enabled, page, label, active=false) => {
                const cls = ['page-item']; if (!enabled) cls.push('disabled'); if (active) cls.push('active');
                return `<li class="${cls.join(' ')}"><a class="page-link" href="#" data-page="${page}">${label}</a></li>`;
            };
            const liEll = () => `<li class="page-item disabled"><span class="page-link">…</span></li>`;

            items.push(liA(curr > 1, curr - 1, '&laquo;'));
            if (curr - maxWindow > 1) {
                items.push(liA(true, 1, '1'));
                if (curr - maxWindow > 2) items.push(liEll());
            }
            for (let p = Math.max(1, curr - maxWindow); p <= Math.min(total, curr + maxWindow); p++) {
                items.push(liA(true, p, String(p), p === curr));
            }
            if (curr + maxWindow < total) {
                if (curr + maxWindow < total - 1) items.push(liEll());
                items.push(liA(true, total, String(total)));
            }
            items.push(liA(curr < total, curr + 1, '&raquo;'));
            return items.join('');
        }

        // 초기화
        (async function initList() {
            try {
                await fetchAllTips();
                const initPage = Math.min(Math.max(1, getPageFromQuery()), totalPages);
                setPageInQuery(initPage);
                renderPage(initPage);
            } catch (err) {
                console.error('[TIP] 목록 로드 실패:', err);
                // 테이블에 메시지 표시
                ulList.querySelectorAll('.tip-item').forEach(el => el.remove());
                const li = document.createElement('li');
                li.className = 'tip-empty-row';
                li.innerHTML = `<span class="col-title">목록을 불러오는 중 오류가 발생했습니다.</span>`;
                ulList.appendChild(li);
            }
        })();
    }

    // ---------------------------
    // 등록/수정 — 기존 로직 유지
    // (필요 페이지에 요소가 있을 때만 동작)
    // ---------------------------
    const registerForm = document.getElementById('tip-form');
    if (registerForm && registerForm.dataset.mode === 'register') {
        registerForm.addEventListener('submit', async (e) => {
            e.preventDefault();
            const title = registerForm.querySelector('input[name="title"]')?.value.trim();
            const content = registerForm.querySelector('textarea[name="content"]')?.value.trim();
            if (!title || !content) return alert('제목과 내용을 입력하세요.');

            try {
                const headers = { 'Content-Type': 'application/json' };
                if (csrfHeader && csrfToken) headers[csrfHeader] = csrfToken;

                const res = await fetch('/tip/register', {
                    method: 'POST',
                    headers,
                    body: JSON.stringify({ title, content })
                });
                const data = await res.json();
                if (data?.bno) { alert('등록 완료'); location.href = `/tip/read/${data.bno}`; }
                else alert('등록 실패');
            } catch (e2) {
                console.error(e2); alert('등록 중 오류가 발생했습니다.');
            }
        });
    }

    if (registerForm && registerForm.dataset.mode === 'modify') {
        const bno = registerForm.dataset.bno;
        registerForm.addEventListener('submit', async (e) => {
            e.preventDefault();
            const title = registerForm.querySelector('input[name="title"]')?.value.trim();
            const content = registerForm.querySelector('textarea[name="content"]')?.value.trim();
            if (!title || !content) return alert('제목과 내용을 입력하세요.');

            try {
                const headers = { 'Content-Type': 'application/json' };
                if (csrfHeader && csrfToken) headers[csrfHeader] = csrfToken;

                const res = await fetch(`/tip/modify/${bno}`, {
                    method: 'PUT',
                    headers,
                    body: JSON.stringify({ title, content })
                });
                const data = await res.json();
                if (data) { alert('수정 완료'); location.href = `/tip/read/${bno}`; }
                else alert('수정 실패');
            } catch (e2) {
                console.error(e2); alert('수정 중 오류가 발생했습니다.');
            }
        });
    }
})();
