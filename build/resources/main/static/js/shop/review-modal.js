// review-modal.js — 별점 위젯 + 리뷰 등록/수정
(() => {
    const $ = (s, r=document) => r.querySelector(s);

    const modal      = $('#review-modal');
    if (!modal) return;

    const form       = $('#review-form');
    const submit     = $('#rv-submit');
    const itemIdEl   = $('#rv-item-id');
    const writerEl   = $('#rv-writer-mno');
    const itemNameEl = $('#rv-item-name');
    const itemImgEl  = $('#rv-item-img');
    const writerName = $('#rv-writer-name');

    const ratingInp  = $('#rv-rating');
    const ratingLbl  = $('#rv-rating-label');
    const starsWrap  = $('#rv-stars');

    const contentEl  = $('#rv-content');
    const fileEl     = $('#rv-images');
    const previews   = $('#rv-previews');
    const errorBox   = $('#rv-error');
    const replaceChk = $('#replaceImages');

    const csrfToken  = $('meta[name="_csrf"]')?.content || '';
    const csrfHeader = $('meta[name="_csrf_header"]')?.content || '';
    const memberName = $('meta[name="memberName"]')?.content || '';
    const memberId   = $('meta[name="memberId"]')?.content || '';

    const MAX_FILES  = 3;
    const MAX_SIZE   = 3 * 1024 * 1024;
    const ALLOWED    = ['image/jpeg', 'image/png', 'image/gif'];
    const EXT_OK     = /\.(jpe?g|png|gif)$/i;

    let starCtrl = null;
    let lastBtn  = null;

    // ✅ 목록 갱신 이벤트 발행 유틸
    function emitReviewSaved({ id=null, mode='update' } = {}) {
        try {
            document.dispatchEvent(new CustomEvent('review:saved', { detail: { id, mode } }));
        } catch { /* no-op */ }
    }

    // ========= 모달 열기 =========
    document.addEventListener('click', async (e) => {
        const btn = e.target.closest('.js-open-review');
        if (!btn) return;
        e.preventDefault();

        lastBtn = btn;
        form?.reset();
        clearError();
        clearPreviews();

        // 기본 값 세팅
        itemIdEl.value = btn.dataset.itemId || '';
        writerEl.value = memberId || '';
        itemNameEl.textContent = btn.dataset.itemName || '상품';
        writerName.textContent = memberName || '회원';
        if (itemImgEl) itemImgEl.src = btn.dataset.itemImg || '/img/No_Image.jpg';

        // 등록 모드
        if (!btn.dataset.reviewId) {
            const status = btn.dataset.status;
            if (!(status === 'DELIVERED' || status === 'COMPLETED')) {
                alert('배송완료 후부터 리뷰 작성 가능합니다.');
                return;
            }
            form.dataset.reviewId = '';
            ratingInp.value = '5';
            submit.textContent = '등록';
            if (replaceChk) replaceChk.checked = false;
        }
        // 수정 모드
        else {
            const reviewId = btn.dataset.reviewId;
            form.dataset.reviewId = reviewId;
            submit.textContent = '수정하기';

            try {
                const res = await fetch(`/reviews/${reviewId}`);
                if (res.ok) {
                    const r = await res.json();
                    contentEl.value = r.content || '';
                    ratingInp.value = String(r.rating || 5);

                    if (Array.isArray(r.reviewImages) && r.reviewImages.length) {
                        clearPreviews();
                        r.reviewImages.forEach(img => {
                            const image = new Image();
                            image.src = img.imgUrl || '/img/No_Image.jpg';
                            image.width = 90; image.height = 90;
                            image.style.objectFit = 'cover';
                            image.style.borderRadius = '8px';
                            image.style.border = '1px solid #eee';
                            previews.appendChild(image);
                        });
                    }
                    if (replaceChk) replaceChk.checked = false;
                }
            } catch (err) {
                console.warn('리뷰 불러오기 실패', err);
            }
        }

        openModal();
        requestAnimationFrame(() => ensureStarReady());
    });

    // ========= 모달 컨트롤 =========
    function openModal() {
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
        form.dataset.reviewId = '';
        lastBtn?.focus();
    }
    modal.addEventListener('click', (e) => { if (e.target === modal) closeModal(); });
    modal.querySelectorAll('[data-close]')?.forEach(b => b.addEventListener('click', closeModal));
    document.addEventListener('keydown', (e) => {
        if (e.key === 'Escape' && modal.classList.contains('show')) closeModal();
    });

    // ========= 별점 위젯 =========
    function createStarWidget(container, inputEl, labelEl) {
        if (!container || !inputEl) return null;
        if (container.__starAPI) return container.__starAPI;

        const fill  = container.querySelector('.fill');
        const track = container.querySelector('.bg');
        if (!fill || !track) return null;

        const measure = () => track.getBoundingClientRect();

        const set = (val) => {
            const score = Math.min(5, Math.max(1, parseInt(val, 10) || 1));
            const rect  = measure();
            fill.style.right = 'auto';
            fill.style.width = `${(rect.width * score / 5)}px`;
            inputEl.value = String(score);
            if (labelEl) labelEl.textContent = `${score}점`;
        };

        const computeScore = (clientX) => {
            const rect = measure();
            const x = Math.min(Math.max(clientX - rect.left, 0), rect.width - 0.001);
            return Math.floor(x / (rect.width / 5)) + 1;
        };

        let dragging = false;
        const onDown = (e) => {
            dragging = true;
            const cx = e.clientX ?? e.touches?.[0]?.clientX ?? 0;
            set(computeScore(cx));
        };
        const onMove = (e) => {
            if (!dragging) return;
            const cx = e.clientX ?? e.touches?.[0]?.clientX ?? 0;
            set(computeScore(cx));
        };
        const onUp = () => { dragging = false; };

        container.addEventListener('click', (e) => set(computeScore(e.clientX)));
        container.addEventListener('pointerdown', onDown);
        window.addEventListener('pointermove',  onMove);
        window.addEventListener('pointerup',    onUp);

        container.addEventListener('mousedown', onDown);
        window.addEventListener('mousemove',    onMove);
        window.addEventListener('mouseup',      onUp);

        container.addEventListener('touchstart',onDown, {passive:true});
        window.addEventListener('touchmove',    onMove, {passive:true});
        window.addEventListener('touchend',     onUp,   {passive:true});

        container.__starAPI = { set };
        return container.__starAPI;
    }

    function ensureStarReady() {
        if (!starsWrap) return;
        if (!starCtrl) {
            starCtrl = createStarWidget(starsWrap, ratingInp, ratingLbl);
        }
        const initial = parseInt(ratingInp.value, 10) || 5;
        starCtrl.set(initial);
    }

    window.addEventListener('resize', () => {
        if (!modal.classList.contains('show') || !starCtrl) return;
        const current = parseInt(ratingInp.value, 10) || 5;
        starCtrl.set(current);
    });
    modal.querySelector('.modal-card')?.addEventListener('transitionend', () => {
        if (!starCtrl) return;
        const current = parseInt(ratingInp.value, 10) || 5;
        starCtrl.set(current);
    });

    // ========= 파일 미리보기 =========
    function clearPreviews() { previews.innerHTML = ''; }
    function clearError() { errorBox.textContent = ''; }
    fileEl?.addEventListener('change', () => {
        clearError(); clearPreviews();
        const files = Array.from(fileEl.files || []);
        if (files.length > MAX_FILES) return setError(`이미지는 최대 ${MAX_FILES}장까지 업로드 가능합니다.`);
        for (const f of files) {
            const okByMime = ALLOWED.includes(f.type);
            const okByExt  = EXT_OK.test(f.name || '');
            if (!(okByMime || okByExt)) return setError('이미지 파일만 업로드 가능합니다.');
            if (f.size > MAX_SIZE) return setError('이미지는 장당 3MB 이하만 가능합니다.');
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
    });
    function setError(msg) { if (errorBox) errorBox.textContent = msg; }

    // ========= 제출 =========
    form?.addEventListener('submit', async (e) => {
        e.preventDefault();
        clearError();

        const reviewId = form.dataset.reviewId || '';
        if (!itemIdEl.value) return setError('상품 정보가 없습니다.');
        if (!writerEl.value) return setError('작성자 정보가 없습니다.');
        if (!contentEl.value.trim()) return setError('내용을 입력하세요.');

        const files = Array.from(fileEl.files || []);
        if (files.length > MAX_FILES) return setError(`이미지는 최대 ${MAX_FILES}장까지 업로드 가능합니다.`);

        const fd = new FormData(form);
        if (replaceChk) fd.set('replaceImages', replaceChk.checked ? 'true' : 'false');

        const headers = {};
        if (csrfHeader && csrfToken) headers[csrfHeader] = csrfToken;

        submit.disabled = true;
        const prev = submit.textContent;
        submit.textContent = reviewId ? '수정 중...' : '등록 중...';

        try {
            const url = reviewId ? `/reviews/${reviewId}` : '/reviews';
            const method = reviewId ? 'PATCH' : 'POST';
            const res = await fetch(url, { method, headers, body: fd });
            if (!res.ok) throw new Error(await res.text());

            // 서버가 JSON을 반환하면 id 추출 시도 (없어도 무방)
            let saved = null;
            try { saved = await res.json(); } catch {}

            alert(reviewId ? '리뷰가 수정되었습니다.' : '리뷰가 등록되었습니다.');

            // ✅ 목록 갱신 이벤트 발행 (리뷰 ID 전달 시 부분 갱신에도 활용 가능)
            emitReviewSaved({
                id: saved?.id ?? reviewId ?? null,
                mode: reviewId ? 'update' : 'create'
            });

            closeModal(); // 전체 리로드 제거
        } catch (err) {
            setError(err.message || '등록/수정 실패');
        } finally {
            submit.disabled = false;
            submit.textContent = prev;
            form.dataset.reviewId = '';
        }
    });
})();
