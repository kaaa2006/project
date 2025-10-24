// =========================
// /static/js/shop/order-detail.js
// 주문 상세 페이지(리뷰 모달 + 주문취소는 서버 폼으로 처리)
// =========================
(() => {
    'use strict';
    const $ = (s, r=document) => r.querySelector(s);

    // 페이지 가드
    const root = $('.order-detail-page');
    if (!root) return;

    // --- CSRF & 로그인 메타 ---
    const csrfToken  = $('meta[name="_csrf"]')?.content || '';
    const csrfHeader = $('meta[name="_csrf_header"]')?.content || 'X-CSRF-TOKEN';
    const memberId   = $('meta[name="memberId"]')?.content || '';

    // --- 리뷰 모달 엘리먼트 ---
    const modal      = $('#reviewModal');
    const openBtn    = $('#openReviewModal');
    const closeBtn1  = $('#closeReviewModal');
    const closeBtn2  = $('#closeReviewModal2');

    const form       = $('#reviewForm');
    const writerEl   = $('#rv-writer-mno');
    const itemIdEl   = $('#rv-item-id');
    const itemNameEl = $('#rv-item-name');
    const ratingInp  = $('#rv-rating');
    const ratingLbl  = $('#rv-rating-label');
    const starsWrap  = $('#rv-stars');
    const fileEl     = $('#rv-images');
    const previews   = $('#rv-previews');
    const errorBox   = $('#rv-error');
    const submitBtn  = $('#rv-submit');

    const MAX_FILES = 3;
    const MAX_SIZE  = 3 * 1024 * 1024; // 3MB
    const ALLOWED   = ['image/jpeg', 'image/png', 'image/gif'];
    const EXT_OK    = /\.(jpe?g|png|gif)$/i;

    // ============ 모달 열고/닫기 ============
    function openModal() {
        modal.classList.add('show');
        modal.setAttribute('aria-hidden', 'false');
        document.body.style.overflow = 'hidden';
        $('#rv-stars')?.focus();
    }
    function closeModal() {
        modal.classList.remove('show');
        modal.setAttribute('aria-hidden', 'true');
        document.body.style.overflow = '';
        form?.reset();
        previews.innerHTML = '';
        errorBox.textContent = '';
        starCtrl?.set(5);
    }

    openBtn?.addEventListener('click', (e) => {
        e.preventDefault();

        const status = openBtn.dataset.status;
        if (!(status === 'DELIVERED' || status === 'COMPLETED')) {
            alert('배송완료 후부터 리뷰 작성 가능합니다.');
            return;
        }


        const iid  = openBtn.dataset.itemId || '';
        const name = openBtn.dataset.itemName || '상품';
        writerEl.value = memberId || '';
        itemIdEl.value = iid;
        itemNameEl.textContent = name;
        starCtrl?.set(5);
        openModal();
    });
    closeBtn1?.addEventListener('click', closeModal);
    closeBtn2?.addEventListener('click', closeModal);
    modal?.addEventListener('click', (e) => { if (e.target === modal) closeModal(); });
    document.addEventListener('keydown', (e) => {
        if (e.key === 'Escape' && modal?.classList.contains('show')) closeModal();
    });

    // ============ 별점 위젯 ============
    function createStarWidget(container, inputEl, labelEl) {
        if (!container || !inputEl) return null;
        const stars = Array.from(container.querySelectorAll('span[data-val]'));

        const set = (val) => {
            const score = Math.min(5, Math.max(1, parseInt(val, 10) || 1));
            inputEl.value = String(score);
            stars.forEach(s => s.classList.toggle('active', (+s.dataset.val) <= score));
            if (labelEl) labelEl.textContent = `${score}점`;
        };

        let isDragging = false;
        stars.forEach(star => {
            star.addEventListener('click',     () => set(star.dataset.val));
            star.addEventListener('mousedown', () => { isDragging = true; set(star.dataset.val); });
            star.addEventListener('mouseover', () => { if (isDragging) set(star.dataset.val); });

            // 터치
            star.addEventListener('touchstart', (ev) => { ev.preventDefault(); isDragging = true; set(star.dataset.val); }, {passive:false});
            star.addEventListener('touchmove',  (ev) => {
                ev.preventDefault();
                const t = ev.touches[0];
                const el = document.elementFromPoint(t.clientX, t.clientY);
                const target = el && el.closest && el.closest('span[data-val]');
                if (target && container.contains(target)) set(target.dataset.val);
            }, {passive:false});
            star.addEventListener('touchend',   () => { isDragging = false; }, {passive:true});
            star.addEventListener('touchcancel',() => { isDragging = false; }, {passive:true});
        });
        document.addEventListener('mouseup', () => { isDragging = false; });

        // 키보드 접근성
        container.setAttribute('tabindex', '0');
        container.addEventListener('keydown', (e) => {
            let cur = parseInt(inputEl.value || '1', 10) || 1;
            if (e.key === 'ArrowRight') { set(cur + 1); e.preventDefault(); }
            if (e.key === 'ArrowLeft')  { set(cur - 1); e.preventDefault(); }
            if (/^[1-5]$/.test(e.key)) { set(e.key);   e.preventDefault(); }
        });

        return { set };
    }
    const starCtrl = createStarWidget(starsWrap, ratingInp, ratingLbl);
    starCtrl?.set(5);

    // ============ 이미지 미리보기 ============
    function renderPreviews() {
        previews.innerHTML = '';
        errorBox.textContent = '';

        const files = Array.from(fileEl.files || []);
        if (files.length > MAX_FILES) { errorBox.textContent = `최대 ${MAX_FILES}장까지 업로드 가능합니다.`; return; }
        for (const f of files) {
            const okMime = ALLOWED.includes(f.type);
            const okExt  = EXT_OK.test(f.name || '');
            if (!(okMime || okExt)) { errorBox.textContent = '이미지 파일만 업로드 가능합니다. (jpg, jpeg, png, gif)'; return; }
            if (f.size > MAX_SIZE)  { errorBox.textContent = '이미지는 장당 3MB 이하여야 합니다.'; return; }
            const url = URL.createObjectURL(f);
            const img = new Image();
            img.src = url;
            img.onload = () => URL.revokeObjectURL(url);
            img.width = 90; img.height = 90;
            img.style.objectFit = 'cover';
            img.style.borderRadius = '8px';
            img.style.border = '1px solid #eee';
            previews.appendChild(img);
        }
    }
    fileEl?.addEventListener('change', renderPreviews);

    // ============ 제출 ============
    form?.addEventListener('submit', async (e) => {
        e.preventDefault();
        errorBox.textContent = '';

        // 파일 유효성 재검사
        const files = Array.from(fileEl?.files || []);
        if (files.length > MAX_FILES) { errorBox.textContent = `최대 ${MAX_FILES}장까지 업로드 가능합니다.`; return; }
        for (const f of files) {
            const okMime = ALLOWED.includes(f.type);
            const okExt  = EXT_OK.test(f.name || '');
            if (!(okMime || okExt)) { errorBox.textContent = '이미지 파일만 업로드 가능합니다. (jpg, jpeg, png, gif)'; return; }
            if (f.size > MAX_SIZE)  { errorBox.textContent = '이미지는 장당 3MB 이하여야 합니다.'; return; }
        }

        const fd = new FormData(form);
        const headers = {};
        if (csrfHeader && csrfToken) headers[csrfHeader] = csrfToken;

        submitBtn.disabled = true;
        const prev = submitBtn.textContent;
        submitBtn.textContent = '등록 중...';

        try {
            const resp = await fetch('/reviews', { method: 'POST', headers, body: fd });
            if (!resp.ok) {
                const text = await resp.text().catch(() => '');
                throw new Error(text || `등록 실패 (${resp.status})`);
            }
            alert('리뷰가 등록되었습니다.');
            closeModal();
            // 필요 시: location.reload();
        } catch (err) {
            errorBox.textContent = err?.message || '등록 중 오류 발생';
        } finally {
            submitBtn.disabled = false;
            submitBtn.textContent = prev;
        }
    });
})();
