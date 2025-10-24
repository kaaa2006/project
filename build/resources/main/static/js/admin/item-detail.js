/* =========================================================
 * Admin Item Detail (lightweight modal, reply label/text split)
 * - Delete item (STOP → hard delete 안내)
 * - Inject admin buttons on reviews
 * - Reply modal (no Bootstrap): open/edit/save/delete
 * - Trim any "[관리자]" prefix from content before saving
 * ========================================================= */
(() => {
    /* ---------- DOM utils ---------- */
    const $ = (s, r = document) => r.querySelector(s);

    const page = $('.item-detail-page');
    if (!page) return;

    const itemId = page.dataset.itemId || (() => {
        const last = decodeURIComponent(location.pathname.split('/').filter(Boolean).pop() || '');
        return /^\d+$/.test(last) ? last : '';
    })();

    const ADMIN_ITEMS   = $('meta[name="admin-items-api-base"]')?.content || '/api/admin/items';
    const ADMIN_REVIEWS = $('meta[name="admin-reviews-api-base"]')?.content || '/admin/reviews';
    const meta          = (n) => document.querySelector(`meta[name="${n}"]`)?.getAttribute('content');
    const CSRF_TOKEN    = meta('_csrf');
    const CSRF_HEADER   = meta('_csrf_header') || 'X-CSRF-TOKEN';

    /* ---------- fetch wrapper (JSON/TEXT auto + CSRF) ---------- */
    async function jfetch(url, opt = {}) {
        const headers = { 'Accept': 'application/json', ...(opt.headers || {}) };
        if (CSRF_TOKEN) headers[CSRF_HEADER] = CSRF_TOKEN;

        const res = await fetch(url, { credentials: 'same-origin', ...opt, headers });
        if (!res.ok) {
            const text = await res.text().catch(() => '');
            throw new Error(`HTTP ${res.status}: ${text?.slice(0, 300)}`);
        }
        const ct = res.headers.get('content-type') || '';
        return ct.includes('application/json') ? res.json() : res.text();
    }

    /* ---------- helpers ---------- */
    function refreshReviewsOrReload() {
        const active = $('#reviews-pagination .page-item.active a[data-page]');
        if (active) active.click(); else location.reload();
    }
    const trimAdminPrefix = (s = '') => s.replace(/^\s*\[?관리자\]?\s*:?\s*/,'').trim();

    /* =========================================================
     * Sell status (for delete button label)
     * ========================================================= */
    async function getSellStatus() {
        const fromAttr = (page.dataset.sellStatus || '').toUpperCase();
        if (fromAttr) return fromAttr;
        try {
            const detailApi = page.dataset.apiDetail || `/api/items/${itemId}`;
            const dto = await jfetch(detailApi, { method: 'GET' });
            return (dto?.itemSellStatus || dto?.sellStatus || '').toUpperCase() || '';
        } catch {
            return '';
        }
    }

    /* =========================================================
     * Delete item (STOP ⇒ hard delete 안내)
     * ========================================================= */
    (async function initDelete() {
        const btn = $('#btn-admin-delete');
        if (!btn) return;

        const isStop = (await getSellStatus()) === 'STOP';
        btn.textContent = isStop ? '영구 삭제' : '삭제';
        btn.title = isStop
            ? 'STOP 상태이므로 영구 삭제를 수행합니다.'
            : '아이템을 삭제합니다(정책에 따라 소프트 → 조건 시 하드).';

        btn.addEventListener('click', async () => {
            let hasOrders = false;
            try {
                const r = await fetch(`${ADMIN_ITEMS}/${encodeURIComponent(itemId)}/order-exists`, { credentials: 'same-origin' });
                hasOrders = r.ok ? await r.json() : false;
            } catch {}

            const msg = hasOrders
                ? '주문이력이 있는 상품입니다. 관련 주문내역의 해당 상품 라인도 함께 삭제됩니다.\n그래도 영구 삭제하시겠습니까?'
                : (isStop
                    ? '해당 아이템을 영구 삭제하시겠어요? (이미지/리뷰/좋아요 포함)'
                    : '해당 아이템과 관련된 모든 정보(이미지 포함)를 삭제하시겠어요?');
            if (!confirm(msg)) return;

            btn.disabled = true;
            const old = btn.textContent;
            btn.textContent = isStop ? '영구 삭제 중...' : '삭제 중...';

            try {
                const url = isStop
                    ? `${ADMIN_ITEMS}/${encodeURIComponent(itemId)}?forceStopOnly=true`
                    : `${ADMIN_ITEMS}/${encodeURIComponent(itemId)}`;

                const res = await fetch(url, {
                    method: 'DELETE',
                    credentials: 'same-origin',
                    headers: { 'Accept':'application/json', ...(CSRF_TOKEN ? { [CSRF_HEADER]: CSRF_TOKEN } : {}) }
                });

                if (!res.ok) {
                    let detail = '';
                    try { const j = await res.clone().json(); detail = j?.message || ''; } catch {}
                    throw new Error(detail || `삭제 실패 (${res.status})`);
                }

                const msgHdr = res.headers.get('X-Delete-Message');
                const result = res.headers.get('X-Delete-Result'); // soft | hard | hard-forced
                if (msgHdr) alert(msgHdr);
                else if (result === 'soft') alert('판매중지(STOP)로 전환되었습니다. 보존기간 경과 후 삭제 가능합니다.');
                else alert('영구 삭제되었습니다.');

                location.href = '/admin/items';
            } catch (e) {
                console.error(e);
                alert(e.message || '삭제 중 오류가 발생했습니다.');
            } finally {
                btn.disabled = false;
                btn.textContent = old;
            }
        });
    })();

    /* =========================================================
     * Inject admin buttons to each review card
     * ========================================================= */
    const reviewsBox = $('#reviews-box');
    if (reviewsBox) {
        const makeBtn = (cls, label, title) => {
            const b = document.createElement('button');
            b.type = 'button';
            b.className = `btn btn-sm ${cls}`;
            b.textContent = label;
            if (title) b.title = title;
            return b;
        };

        function decorate(card) {
            const reviewId = card.getAttribute('data-review-id') || card.dataset.reviewId || '';
            if (!reviewId || card.dataset.adminDecorated === '1') return;
            card.dataset.adminDecorated = '1';

            const row = document.createElement('div');
            row.className = 'review-admin-btns d-flex mt-2';

            const hasReply = !!card.querySelector('.review-reply');
            const btnReply    = makeBtn('btn-outline-success', hasReply ? '답변수정' : '답변');
            const btnDelReply = makeBtn('btn-outline-secondary', '답변삭제');
            const btnForceDel = makeBtn('btn-outline-danger', '리뷰삭제');

            btnReply.addEventListener('click', () => openReplyModal(reviewId, card));
            btnDelReply.addEventListener('click', () => deleteReply(reviewId));
            btnForceDel.addEventListener('click', () => forceDeleteReview(reviewId));

            row.append(btnReply, btnDelReply, btnForceDel);
            card.appendChild(row);
        }

        const apply = () => reviewsBox.querySelectorAll('.review-card').forEach(decorate);
        const mo = new MutationObserver(apply);
        mo.observe(reviewsBox, { childList:true, subtree:true });
        apply();
    }

    /* =========================================================
     * Lightweight modal (open/edit/save)
     * ========================================================= */
    let currentReviewId = null;
    let replyModalEl = null, replyTextarea = null;

    function ensureReplyModal() {
        replyModalEl  = replyModalEl  || $('#replyModal');
        replyTextarea = replyTextarea || $('#replyContent');
        return !!(replyModalEl && replyTextarea);
    }
    function openModal() {
        replyModalEl.classList.add('show');
        replyModalEl.setAttribute('aria-hidden','false');
        document.body.classList.add('modal-open');
        setTimeout(() => replyTextarea?.focus(), 0);
    }
    function closeModal() {
        replyModalEl.classList.remove('show');
        replyModalEl.setAttribute('aria-hidden','true');
        document.body.classList.remove('modal-open');
    }

    // backdrop/close buttons
    document.addEventListener('click', (e) => {
        if (!replyModalEl?.classList.contains('show')) return;
        if (e.target === replyModalEl || e.target.hasAttribute('data-close')) closeModal();
    });
    // ESC + simple focus loop
    document.addEventListener('keydown', (e) => {
        if (!replyModalEl?.classList.contains('show')) return;
        if (e.key === 'Escape') { e.preventDefault(); closeModal(); }
        if (e.key === 'Tab') {
            const focusables = replyModalEl.querySelectorAll('button,[href],input,textarea,select,[tabindex]:not([tabindex="-1"])');
            const f = Array.from(focusables).filter(el => !el.disabled && !el.getAttribute('aria-hidden'));
            if (!f.length) return;
            const first = f[0], last = f[f.length - 1];
            if (e.shiftKey && document.activeElement === first) { last.focus(); e.preventDefault(); }
            else if (!e.shiftKey && document.activeElement === last) { first.focus(); e.preventDefault(); }
        }
    });

    // open with existing reply prefill (prefers .reply-text)
    async function openReplyModal(reviewId, card) {
        if (!reviewId) return alert('리뷰 ID를 찾을 수 없습니다.');
        if (!ensureReplyModal()) return alert('답변 모달 요소를 찾을 수 없습니다.');
        currentReviewId = reviewId;

        let initial = '';
        const txt = card?.querySelector('.review-reply .reply-text');
        if (txt) initial = txt.textContent.trim();
        else {
            // legacy: .review-reply 전체 텍스트에서 접두사 제거
            const old = card?.querySelector('.review-reply');
            if (old) initial = trimAdminPrefix(old.textContent.trim());
        }
        replyTextarea.value = initial;
        openModal();
    }

    // save (POST/PATCH) or delete when empty
    $('#btn-save-reply')?.addEventListener('click', async () => {
        if (!ensureReplyModal()) return;
        if (!currentReviewId) return;

        const content = (replyTextarea.value || '').trim();
        if (!content) {
            if (!confirm('내용이 비어 있습니다. 기존 답변을 삭제하시겠습니까?')) return;
            return deleteReply(currentReviewId);
        }
        try {
            await upsertReply(currentReviewId, content);
            closeModal();
            refreshReviewsOrReload();
        } catch (e) {
            console.error(e);
            alert('답변 저장 중 오류가 발생했습니다.');
        }
    });

    /* =========================================================
     * Review reply / delete APIs
     * ========================================================= */
    async function upsertReply(reviewId, content) {
        const clean = trimAdminPrefix(content);
        const existed = !!document.querySelector(`.review-card[data-review-id="${reviewId}"] .review-reply`);
        const method  = existed ? 'PATCH' : 'POST';
        return jfetch(`${ADMIN_REVIEWS}/${encodeURIComponent(reviewId)}/reply`, {
            method,
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ content: clean })
        });
    }
    async function deleteReply(reviewId) {
        if (!confirm('이 리뷰의 관리자 답변을 삭제하시겠습니까?')) return;
        await jfetch(`${ADMIN_REVIEWS}/${encodeURIComponent(reviewId)}/reply`, { method: 'DELETE' });
        refreshReviewsOrReload();
    }
    async function forceDeleteReview(reviewId) {
        if (!confirm('정말로 이 리뷰를 삭제하시겠습니까? (구매자 리뷰 영구삭제)')) return;
        await jfetch(`${ADMIN_REVIEWS}/${encodeURIComponent(reviewId)}`, { method: 'DELETE' });
        refreshReviewsOrReload();
    }
})();
