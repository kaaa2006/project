/* ================================
   order-list.js — 주문 내역 + 리뷰 모달
================================== */
(() => {
    const $  = (s, r=document) => r.querySelector(s);
    const $$ = (s, r=document) => Array.from(r.querySelectorAll(s));

    const csrfToken  = $('meta[name="_csrf"]')?.content || '';
    const csrfHeader = $('meta[name="_csrf_header"]')?.content || 'X-CSRF-TOKEN';
    const memberName = $('meta[name="memberName"]')?.content || '';
    const memberId   = $('meta[name="memberId"]')?.content || '';

    // ========= 주문 행 클릭 → 상세 이동 =========
    function bindRowClick() {
        const rows = $$('.order-list-page table tbody tr');
        rows.forEach((row) => {
            const detailBtn = row.querySelector('.btn-outline-primary');
            if (!detailBtn) return;

            row.style.cursor = 'pointer';
            row.addEventListener('click', (e) => {
                if (e.target.closest('a')) return; // 상세/리뷰 버튼 클릭 시 중복 이동 방지
                window.location.href = detailBtn.href;
            });
        });
    }

    // ========= 리뷰 모달 구현 =========
    function initReviewModal() {
        const modal = $('#review-modal');
        if (!modal) return;

        const openBtns = $$('.js-open-review');
        const closes = modal.querySelectorAll('[data-close]');
        const form = $('#review-form');
        const submit = $('#rv-submit');

        const itemIdEl = $('#rv-item-id');
        const writerEl = $('#rv-writer-mno');
        const itemNameEl = $('#rv-item-name');
        const writerName = $('#rv-writer-name');

        const ratingInp = $('#rv-rating');        // hidden (서버 전송용)
        const ratingLbl = $('#rv-rating-label');
        const starsWrap = $('#rv-stars');

        const contentEl = $('#rv-content');
        const fileEl = $('#rv-images');
        const previews = $('#rv-previews');
        const errorBox = $('#rv-error');

        const MAX_FILES = 3;
        const MAX_SIZE = 3 * 1024 * 1024; // 3MB
        const ALLOWED = ['image/jpeg', 'image/png', 'image/gif'];
        const EXT_OK = /\.(jpe?g|png|gif)$/i;

        let starCtrl = null;
        let lastFocusedBtn = null;

        // 모달 열기
        openBtns.forEach(btn => btn.addEventListener('click', (e) => {
            e.preventDefault();

            const status = btn.dataset.status;
            // 배송완료, COMPLETED만 허용
            if (!(status === 'DELIVERED' || status === 'COMPLETED')) {
                alert('배송완료 후부터 리뷰 작성 가능합니다.');
                return;
            }

            lastFocusedBtn = e.currentTarget;
            const itemId = btn.dataset.itemId || '';
            const itemName = btn.dataset.itemName || '상품';

            form?.reset();
            clearError();
            clearPreviews();

            itemIdEl.value = itemId;
            writerEl.value = memberId || '';
            itemNameEl.textContent = itemName;
            writerName.textContent = memberName || '회원';

            starCtrl?.set(5); // 별점 기본 5점

            openModal();
        }));

        // 닫기
        closes.forEach(b => b.addEventListener('click', closeModal));
        modal.addEventListener('click', (e) => {
            if (e.target === modal) closeModal();
        });
        document.addEventListener('keydown', (e) => {
            if (e.key === 'Escape' && isOpen()) closeModal();
        });

        // 파일 미리보기
        fileEl?.addEventListener('change', renderPreviews);

        // 제출
        submit?.addEventListener('click', onSubmit);

        // === 별점 위젯 초기화 (기본 1점) ===
        starCtrl = createStarWidget(starsWrap, ratingInp, ratingLbl);
        starCtrl?.set(5);

        // ---------- 내부 함수들 ----------
        function openModal() {
            starCtrl?.set(5);
            modal.classList.add('show');
            modal.classList.add('show');
            modal.setAttribute('aria-hidden', 'false');
            document.body.style.overflow = 'hidden';
            $('#rv-content')?.focus();
        }

        function closeModal() {
            modal.classList.remove('show');
            modal.setAttribute('aria-hidden', 'true');
            document.body.style.overflow = '';
            form?.reset();
            clearPreviews();
            clearError();
            lastFocusedBtn?.focus();
        }

        function isOpen() {
            return modal.classList.contains('show');
        }

        function setSubmitting(on) {
            submit.disabled = !!on;
            submit.textContent = on ? '등록 중...' : '등록';
        }

        function setError(msg) {
            if (errorBox) errorBox.textContent = msg || '';
        }

        function clearError() { setError(''); }

        async function safeText(res) {
            try { return await res.text(); } catch { return ''; }
        }

        function clearPreviews() { if (previews) previews.innerHTML = ''; }

        function renderPreviews() {
            clearError();
            clearPreviews();
            const files = Array.from(fileEl.files || []);
            if (files.length > MAX_FILES) {
                return setError(`이미지는 최대 ${MAX_FILES}장까지 업로드 가능합니다.`);
            }
            for (const f of files) {
                const okByMime = ALLOWED.includes(f.type);
                const okByExt  = EXT_OK.test(f.name || '');
                if (!(okByMime || okByExt)) return setError('이미지 파일만 업로드 가능합니다. (jpg, jpeg, png, gif)');
                if (f.size > MAX_SIZE) return setError('이미지 용량은 장당 3MB 이하여야 합니다.');
            }
            files.forEach(f => {
                const url = URL.createObjectURL(f);
                const img = new Image();
                img.src = url;
                img.onload = () => URL.revokeObjectURL(url);
                previews.appendChild(img);
            });
        }

        async function onSubmit() {
            clearError();

            if (!itemIdEl.value) return setError('상품 정보가 없습니다.');
            if (!writerEl.value) return setError('작성자 정보가 없습니다.');
            if (!contentEl.value.trim()) return setError('내용을 입력하세요.');

            const files = Array.from(fileEl.files || []);
            if (files.length > MAX_FILES) return setError(`이미지는 최대 ${MAX_FILES}장까지 업로드 가능합니다.`);
            for (const f of files) {
                const okByMime = ALLOWED.includes(f.type);
                const okByExt  = EXT_OK.test(f.name || '');
                if (!(okByMime || okByExt)) return setError('이미지 파일만 업로드 가능합니다. (jpg, jpeg, png, gif)');
                if (f.size > MAX_SIZE) return setError('이미지 용량은 장당 3MB 이하여야 합니다.');
            }

            const fd = new FormData();
            fd.append('itemId', itemIdEl.value);
            fd.append('writerMno', writerEl.value);
            fd.append('rating', ratingInp.value || '1');
            fd.append('content', contentEl.value.trim());
            files.forEach(f => fd.append('images', f));

            const headers = {};
            if (csrfHeader && csrfToken) headers[csrfHeader] = csrfToken;

            setSubmitting(true);
            try {
                const res = await fetch('/reviews', { method: 'POST', headers, body: fd });
                if (!res.ok) {
                    const msg = await safeText(res);
                    throw new Error(`등록 실패 (${res.status})\n${msg}`);
                }
            } catch (err) {
                setSubmitting(false);
                return setError(err.message || '등록 중 오류가 발생했습니다.');
            }

            setSubmitting(false);
            closeModal();
            alert('리뷰가 등록되었습니다.');
            // 필요 시: location.reload();
        }

        // ★ 별점 위젯 (1~5 정수 확정)
        function createStarWidget(container, inputEl, labelEl) {
            if (!container || !inputEl) return null;
            const stars = Array.from(container.querySelectorAll('span[data-val]'));
            if (stars.length === 0) return null;

            const set = (val) => {
                const score = Math.min(5, Math.max(1, parseInt(val, 10) || 1));
                inputEl.value = String(score);
                stars.forEach(s => s.classList.toggle('active', (+s.dataset.val) <= score));
                if (labelEl) labelEl.textContent = `${score}점`;
            };

            let isDragging = false;

            // 마우스
            stars.forEach(star => {
                star.addEventListener('click', () => set(star.dataset.val));
                star.addEventListener('mousedown', (e) => {
                    isDragging = true;
                    set(star.dataset.val);
                    e.preventDefault();
                });
                star.addEventListener('mouseover', () => {
                    if (isDragging) set(star.dataset.val);
                });
            });
            document.addEventListener('mouseup', () => { isDragging = false; });

            // 터치
            stars.forEach(star => {
                star.addEventListener('touchstart', (ev) => {
                    ev.preventDefault(); isDragging = true; set(star.dataset.val);
                }, {passive: false});
                star.addEventListener('touchmove', (ev) => {
                    ev.preventDefault();
                    const t = ev.touches[0];
                    const el = document.elementFromPoint(t.clientX, t.clientY);
                    const target = el && el.closest('span[data-val]');
                    if (target && container.contains(target)) set(target.dataset.val);
                }, {passive: false});
                star.addEventListener('touchend', () => { isDragging = false; }, {passive: true});
                star.addEventListener('touchcancel', () => { isDragging = false; }, {passive: true});
            });

            // 빈 영역 클릭 무시
            container.addEventListener('click', (e) => {
                if (!e.target.closest('span[data-val]')) e.stopPropagation();
            });

            // 키보드 접근성
            container.addEventListener('keydown', (e) => {
                let cur = parseInt(inputEl.value || '1', 10) || 1;
                if (e.key === 'ArrowRight') { set(cur + 1); e.preventDefault(); }
                if (e.key === 'ArrowLeft')  { set(cur - 1); e.preventDefault(); }
                if (/^[1-5]$/.test(e.key)) { set(e.key);   e.preventDefault(); }
            });

            return { set };
        }
    }

    // ========= 초기 실행 =========
    document.addEventListener('DOMContentLoaded', () => {
        bindRowClick();
        initReviewModal();
    });
})();
