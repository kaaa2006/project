/* items-detail.js — item detail (desc / reviews) */
(() => {
    // ---------- DOM helpers ----------
    const $ = (s) => document.querySelector(s);

    const page = document.querySelector('.item-detail-page');
    if (!page) return;

    // ---------- Config ----------
    const API_DETAIL  = page.dataset.apiDetail  || '/api/items/{id}';
    const API_REVIEWS = page.dataset.reviews    || '/api/items/{id}/reviews';
    const MOCK        = (page.dataset.mock === 'on') || new URLSearchParams(location.search).has('mock');
    const PLACEHOLDER_IMG = '/img/No_Image.jpg';

    // ---------- Utils ----------
    const toInt = (v) => Number.isFinite(+v) ? Math.max(0, Math.floor(+v)) : 0;
    const safeStr = (v) => (v == null ? '' : String(v));
    const escapeHTML = (s) =>
        String(s).replace(/&/g,'&amp;').replace(/</g,'&lt;')
            .replace(/>/g,'&gt;').replace(/"/g,'&quot;').replace(/'/g,'&#39;');

    const fmtDate = (iso) => {
        try {
            const d = new Date(iso);
            if (Number.isNaN(+d)) return '';
            return d.toLocaleString('ko-KR');
        } catch { return ''; }
    };

    const calcSalePrice = (op, dr) => {
        const o = toInt(op), d = toInt(dr);
        return !o ? 0 : (!d ? o : Math.floor(o * (100 - d) / 100));
    };

    async function getJSON(url) {
        const r = await fetch(url, { credentials: 'same-origin', headers: { Accept: 'application/json' } });
        if (!r.ok) {
            const body = await r.text().catch(() => '');
            console.error('[API ERROR]', r.status, url, body?.slice(0, 500));
            throw new Error(`HTTP ${r.status}`);
        }
        return r.json();
    }

    // [추가] 관리자 접두사 제거 유틸
    function stripAdminPrefix(s = '') {
        return String(s).replace(/^\s*\[?관리자\]?\s*:?\s*/i, '').trim();
    }

    // itemId: URL 마지막 세그먼트 or data-item-id
    const pathId = (() => {
        const last = decodeURIComponent(location.pathname.split('/').filter(Boolean).pop() || '');
        if (/^\d+$/.test(last)) return last;
        if (page.dataset.itemId && /^\d+$/.test(page.dataset.itemId)) return page.dataset.itemId;
        throw new Error('Missing item id');
    })();

    // 이미지 URL 보정(/images/** 강제)
    function normalizeImageUrl(u) {
        if (!u) return '';
        const raw = String(u).trim();
        if (raw.startsWith('data:')) return raw;
        const withoutHash = raw.split('#')[0];
        if (/^https?:\/\//i.test(withoutHash) || withoutHash.startsWith('/images/')) return withoutHash;
        const idx = withoutHash.indexOf('images/');
        if (idx >= 0) return '/' + withoutHash.slice(idx);
        return withoutHash;
    }

    function pickRepImage(dto) {
        if (dto.repImgUrl) return normalizeImageUrl(dto.repImgUrl);
        const list = Array.isArray(dto.itemImages) ? dto.itemImages : [];
        const rep = list.find((x) => x?.repimgYn === true || String(x?.repimgYn).toUpperCase() === 'Y');
        return normalizeImageUrl(rep?.imgUrl || list[0]?.imgUrl || '');
    }

    function mapItem(raw) {
        const src = raw?.item ? { ...raw.item } : (raw || {});
        if (raw?.avgRating != null)   src.avgRating   = raw.avgRating;
        if (raw?.reviewCount != null) src.reviewCount = raw.reviewCount;
        if (raw?.viewCount != null)   src.itemViewCnt = raw.viewCount;

        const originalPrice = toInt(src.originalPrice);
        const discountRate  = toInt(src.discountRate);
        const salePrice     = toInt(src.price) || calcSalePrice(originalPrice, discountRate);

        const hasDiscount   = originalPrice > 0 && salePrice > 0 && salePrice < originalPrice;
        const effectiveRate = hasDiscount
            ? (discountRate || Math.min(95, Math.round((1 - (salePrice / originalPrice)) * 100)))
            : 0;

        return {
            id: src.id,
            name: src.itemNm ?? '-',
            desc: src.itemDetail ?? '',
            status: String(src.itemSellStatus || '').toUpperCase(),
            category: src.category ?? '',
            foodItem: src.foodItem ?? '',
            stock: toInt(src.stockNumber),
            avgRating: Number(src.avgRating || 0),
            reviewCount: Number(src.reviewCount || 0),
            viewCnt: Number(src.itemViewCnt || 0),

            originalPrice,
            discountRate: effectiveRate,
            salePrice,
            hasDiscount,

            hero: pickRepImage(src),
            thumbs: (Array.isArray(src.itemImages) ? src.itemImages : [])
                .map((x) => normalizeImageUrl(x?.imgUrl)).filter(Boolean),
            longImages: Array.isArray(src.longImages) && src.longImages.length
                ? src.longImages.map(normalizeImageUrl) : []
        };
    }

    const badge = (s) => {
        if (s === 'SOLD_OUT') return { cls: 'text-bg-secondary', txt: '품절' };
        if (s === 'SELL')     return { cls: 'text-bg-success',   txt: '판매중' };
        return { cls: 'text-bg-warning',  txt: s || '-' };
    };

    // ---------- 구매 수량 ----------
    const QTY_MIN_DEFAULT = 1;
    const QTY_MAX_DEFAULT = 99;

    const elQty     = () => $('#qty');
    const elHint    = () => $('#qty-hint');
    const elDec     = () => $('#qty-dec');
    const elInc     = () => $('#qty-inc');
    const elBtnCart = () => $('#btn-cart');
    const elBtnBuy  = () => $('#btn-buy');

    let stockLimit = 0;
    let qtyBound = false;

    function clamp(n, min, max) { return Math.max(min, Math.min(max, n)); }
    function getQty() {
        const v = parseInt(elQty()?.value ?? '1', 10);
        const max = stockLimit > 0 ? stockLimit : QTY_MAX_DEFAULT;
        return clamp(Number.isFinite(v) ? v : 1, QTY_MIN_DEFAULT, max);
    }
    function setQty(n, {silent=false} = {}) {
        const max = stockLimit > 0 ? stockLimit : QTY_MAX_DEFAULT;
        const q = clamp(n, QTY_MIN_DEFAULT, max);
        if (elQty()) elQty().value = String(q);
        if (!silent) updateQtyUI();
        return q;
    }
    function updateQtyUI() {
        const q   = getQty();
        const max = stockLimit > 0 ? stockLimit : QTY_MAX_DEFAULT;

        if (elHint()) elHint().textContent = stockLimit <= 0 ? '품절 상태입니다.' : `최대 ${max.toLocaleString()}개까지 구매 가능`;
        if (elDec()) elDec().disabled = (q <= QTY_MIN_DEFAULT || stockLimit <= 0);
        if (elInc()) elInc().disabled = (q >= max || stockLimit <= 0);

        const disabled = stockLimit <= 0;
        if (elBtnCart()) elBtnCart().disabled = disabled;
        if (elBtnBuy())  elBtnBuy().disabled  = disabled;
    }
    function bindQtyEvents() {
        if (qtyBound) { updateQtyUI(); return; }
        qtyBound = true;

        setQty(1, {silent:true});
        updateQtyUI();

        elDec()?.addEventListener('click', () => { setQty(getQty() - 1); });
        elInc()?.addEventListener('click', () => { setQty(getQty() + 1); });

        elQty()?.addEventListener('input', () => {
            const raw = elQty().value.replace(/[^\d]/g,'');
            elQty().value = raw;
        });
        elQty()?.addEventListener('change', () => { setQty(parseInt(elQty().value || '1', 10)); });
        elQty()?.addEventListener('wheel', (e) => e.preventDefault(), {passive:false});
        elQty()?.addEventListener('keydown', (e) => {
            if (e.key === 'ArrowUp')   { e.preventDefault(); setQty(getQty() + 1); }
            if (e.key === 'ArrowDown') { e.preventDefault(); setQty(getQty() - 1); }
        });
    }

    // ---------- Render: Item ----------
    function updateAvgRatingUI(avg) {
        const v = Number(avg || 0);
        const el = $('#avg-rating');
        if (el) el.textContent = v.toFixed(1);
    }
    function updateReviewCountUI(n) {
        const v = toInt(n || 0);
        const el1 = $('#review-count');
        const el2 = $('#review-count-meta');
        if (el1) el1.textContent = v;
        if (el2) el2.textContent = v;
    }

    async function refreshItemMeta() {
        try {
            if (!API_DETAIL) return;
            const url = (API_DETAIL.includes('{id}'))
                ? API_DETAIL.replace('{id}', encodeURIComponent(pathId))
                : API_DETAIL;
            const raw = await getJSON(url);
            const vm  = mapItem(raw || {});
            updateAvgRatingUI(vm.avgRating);
            updateReviewCountUI(vm.reviewCount);
        } catch (e) {
            console.warn('[avg refresh skipped]', e?.message || e);
        }
    }

    function renderItem(vm) {
        $('#name').textContent = vm.name;
        $('#category').textContent = vm.category || '';
        updateAvgRatingUI(vm.avgRating);
        updateReviewCountUI(vm.reviewCount);
        $('#view-cnt').textContent = vm.viewCnt.toLocaleString();
        $('#stock').textContent = vm.stock;

        const b = badge(vm.status);
        const bd = $('#badge');
        bd.className = `badge ${b.cls}`;
        bd.textContent = b.txt;

        // 가격
        $('#sale-price').textContent = vm.salePrice > 0 ? `${vm.salePrice.toLocaleString()}원` : '가격 문의';
        if (vm.hasDiscount && vm.discountRate > 0) {
            $('#original-price-wrap').classList.remove('d-none');
            $('#original-price').textContent = `${vm.originalPrice.toLocaleString()}원`;
            $('#discount-badge').classList.remove('d-none');
            $('#discount-badge').textContent = `-${vm.discountRate}%`;
            const saving = vm.originalPrice - vm.salePrice;
            if (saving > 0) {
                $('#saving-wrap').classList.remove('d-none');
                $('#saving-amount').textContent = saving.toLocaleString();
            }
        } else {
            $('#original-price-wrap').classList.add('d-none');
            $('#discount-badge').classList.add('d-none');
            $('#saving-wrap').classList.add('d-none');
        }

        // 갤러리
        const hero = $('#hero');
        hero.src = vm.hero || PLACEHOLDER_IMG || '';
        hero.alt = vm.name || '상품 이미지';
        hero.addEventListener('error', () => { hero.src = PLACEHOLDER_IMG || ''; });

        const thumbs = $('#thumbs');
        thumbs.innerHTML = vm.thumbs.map((u, i) =>
            `<img src="${escapeHTML(u)}" ${i === 0 ? 'class="active"' : ''} alt="">`
        ).join('');
        thumbs.querySelectorAll('img').forEach((img) => {
            img.addEventListener('error', () => { img.src = PLACEHOLDER_IMG || ''; });
            img.addEventListener('click', () => {
                thumbs.querySelectorAll('img').forEach((i) => i.classList.remove('active'));
                img.classList.add('active');
                hero.src = img.src;
            });
        });

        // 상세설명 이미지
        const long = $('#long-images');
        try {
            const holder = document.createElement('div');
            holder.innerHTML = vm.desc || '';
            const picks = [];
            holder.querySelectorAll('img').forEach(i => {
                let src = i.getAttribute('src') || i.getAttribute('data-src') || '';
                if (src) picks.push(normalizeImageUrl(src));
            });

            const merged = [];
            const seen = new Set();
            const push = (u) => {
                const k = normalizeImageUrl(u);
                if (!k || seen.has(k)) return;
                seen.add(k); merged.push(k);
            };
            picks.forEach(push);
            vm.longImages.forEach(push);

            long.innerHTML = merged.map(u => `<img src="${escapeHTML(u)}" loading="lazy" alt="">`).join('');
        } catch {}

        // 수량/재고
        stockLimit = Number.isFinite(+vm.stock) ? Math.max(0, Math.floor(+vm.stock)) : 0;
        bindQtyEvents();
        updateQtyUI();
    }

    // ---------- Reviews ----------
    const pager = { page: 0, size: 10, totalPages: 0, busy: false };
    let reviewsLoadedOnce = false;

    const renderSingleReview = (r) => {
        const name   = escapeHTML(safeStr(r.writerName || r.writer || '구매자'));
        const rating = toInt(r.rating || 0);
        const time   = escapeHTML(fmtDate(r.regTime || r.createdAt || r.time));
        const text   = escapeHTML(safeStr(r.content || r.text || '')).replace(/\n/g, '<br>');
        const imgs   = Array.isArray(r.reviewImages) && r.reviewImages.length
            ? `<div class="mt-2 d-flex flex-wrap gap-2">${r.reviewImages.map(it =>
                `<img src="${escapeHTML(normalizeImageUrl(it.imgUrl))}" alt="" style="width:84px;height:84px;object-fit:cover;border-radius:.5rem;border:1px solid #e5e7eb;">`
            ).join('')}</div>`
            : '';

        // [교체] 관리자 답변 렌더: 배지 + 접두사 제거 + 시간 메타(가능 시)
        const replyObj    = r.reply || r.adminReply || r.reviewReply || null;
        const replyText0  = (r.replyContent || replyObj?.content || replyObj?.text || '');
        const replyText   = stripAdminPrefix(replyText0);
        const replyAt     = replyObj?.updatedAt || replyObj?.modifiedAt || replyObj?.createdAt || r.replyUpdatedAt || r.replyCreatedAt || '';
        const replyAtStr  = escapeHTML(fmtDate(replyAt));
        const reply       = replyText
            ?  `<div class="review-reply mt-2 p-2 rounded" style="background:#f8fafc;border:1px solid #e2e8f0;">
         <div class="d-flex align-items-center gap-2 mb-1">
           <span class="badge-admin"><i class="bi bi-shield-check"></i> 관리자</span>
           ${replyAtStr ? `<span class="text-muted small">${replyAtStr}</span>` : ''}
         </div>
         <div class="reply-text">${escapeHTML(replyText)}</div>
       </div>`
            : '';

        const rid = String(r.id ?? r.reviewId ?? '');
        return `
      <div class="review-card" data-review-id="${rid}"
           style="border:1px solid #eef0f2;border-radius:.75rem;padding:12px 14px;margin-bottom:12px;background:#fff;">
        <div class="review-head d-flex align-items-center gap-2">
          <span class="review-name fw-bold">${name}</span>
          <span class="review-stars text-warning">${'★'.repeat(rating)}${'☆'.repeat(Math.max(0, 5 - rating))}</span>
          <span class="ms-auto text-muted small">${time}</span>
        </div>
        <div class="review-body mt-2">${text}</div>
        ${imgs}
        ${reply}
      </div>
    `;
    };

    function renderReviewsPage(data) {
        const box = $('#reviews-box');
        const list = Array.isArray(data?.content) ? data.content : [];
        box.innerHTML = list.map(renderSingleReview).join('') || `<div class="muted">등록된 구매후기가 없습니다.</div>`;

        pager.totalPages = toInt(data?.totalPages || 0);
        pager.page = toInt(data?.number || 0);
        renderPagination();
    }

    function renderPagination() {
        const el  = $('#reviews-pagination');
        const tp  = pager.totalPages;
        const cur = pager.page;
        if (!el) return;
        if (tp <= 1) { el.innerHTML = ''; return; }

        const make = (p, label, disabled, active) =>
            `<li class="page-item ${disabled ? 'disabled' : ''} ${active ? 'active' : ''}">
         <a class="page-link" href="#" data-page="${p}">${label}</a>
       </li>`;

        const parts = [];
        parts.push(make(0, '&laquo;', cur === 0, false));
        const start = Math.max(0, cur - 2);
        const end   = Math.min(tp - 1, cur + 2);
        for (let i = start; i <= end; i++) parts.push(make(i, (i + 1), false, i === cur));
        parts.push(make(tp - 1, '&raquo;', cur >= tp - 1, false));

        el.innerHTML = parts.join('');
        el.querySelectorAll('a[data-page]').forEach((a) => {
            a.addEventListener('click', (ev) => {
                ev.preventDefault();
                const p = toInt(a.dataset.page);
                if (!pager.busy && p !== cur) loadReviews(p);
            });
        });
    }

    async function loadReviews(p = 0) {
        if (reviewsLoadedOnce && p === pager.page) return;

        const box = $('#reviews-box');
        const pag = $('#reviews-pagination');
        try {
            pager.busy = true;
            box.setAttribute('aria-busy', 'true');
            const url = API_REVIEWS.replace('{id}', encodeURIComponent(pathId));
            const qs  = new URLSearchParams({
                page: String(p),
                size: String(pager.size),
                sort: 'id,DESC',
                withImages: 'true',
                withReply: 'true'
            });
            const data = await getJSON(`${url}?${qs}`);
            const total = toInt(data?.totalElements || 0);
            updateReviewCountUI(total);
            renderReviewsPage(data);
            reviewsLoadedOnce = true;
        } catch {
            box.innerHTML = `<div class="muted">구매후기를 불러오지 못했습니다.</div>`;
            if (pag) pag.innerHTML = '';
        } finally {
            pager.busy = false;
            box.removeAttribute('aria-busy');
        }
    }

    // ---------- Bootstrap ----------
    (async () => {
        try {
            const url = (API_DETAIL.includes('{id}'))
                ? API_DETAIL.replace('{id}', encodeURIComponent(pathId))
                : API_DETAIL;

            const raw = MOCK ? null : await getJSON(url);
            renderItem(mapItem(raw || {}));

            // 리뷰는 리뷰 섹션 가시화 시 로드(폴백: 즉시 로드)
            const target = $('#section-reviews');
            if ('IntersectionObserver' in window && target) {
                let done = false;
                new IntersectionObserver(([e], io) => {
                    if (!done && e.isIntersecting && !reviewsLoadedOnce) {
                        done = true; io.disconnect(); loadReviews(0);
                    }
                }, { rootMargin: '0px 0px -50% 0px' }).observe(target);
            } else {
                loadReviews(0);
            }
        } catch (e) {
            console.error(e);
            $('#name').textContent = '상품 정보를 불러오지 못했습니다.';
            document.querySelector('.summary .btns')?.classList.add('d-none');
        }
    })();

    // ---------- Cart ----------
    $('#btn-cart')?.addEventListener('click', async (ev) => {
        const btn = ev.currentTarget;
        if (btn.dataset.busy === '1') return;
        btn.dataset.busy = '1';

        const qty = getQty();
        if (stockLimit <= 0) { alert('품절 상품입니다.'); btn.dataset.busy = '0'; return; }
        if (qty > stockLimit) { alert('재고 수량을 초과했습니다.'); setQty(stockLimit); btn.dataset.busy = '0'; return; }

        const meta = (n) => document.querySelector(`meta[name="${n}"]`)?.getAttribute('content');
        const csrfToken  = meta('_csrf');
        the_csrf_header = meta('_csrf_header') || 'X-CSRF-TOKEN'; // NOTE: keep var name different than const

        try {
            const res = await fetch('/cart/add', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'Accept': 'application/json',
                    ...(csrfToken ? { [the_csrf_header]: csrfToken } : {})
                },
                credentials: 'same-origin',
                body: JSON.stringify({ itemId: pathId, quantity: qty, count: qty })
            });
            if (!res.ok) {
                if (res.status === 401) { alert('로그인이 필요합니다.'); location.href = '/login'; return; }
                throw new Error(await res.text());
            }
            alert('장바구니에 담았습니다!');
        } catch (err) {
            console.error('장바구니 추가 실패:', err);
            alert('장바구니 담기 중 오류가 발생했습니다.');
        } finally {
            btn.dataset.busy = '0';
        }
    });

    // ---------- Buy Now ----------
    $('#btn-buy')?.addEventListener('click', async (ev) => {
        const btn = ev.currentTarget;
        if (btn.dataset.busy === '1') return;
        btn.dataset.busy = '1';

        const qty = getQty();
        if (stockLimit <= 0) { alert('품절 상품입니다.'); btn.dataset.busy = '0'; return; }
        if (qty > stockLimit) { alert('재고 수량을 초과했습니다.'); setQty(stockLimit); btn.dataset.busy = '0'; return; }

        const meta = (n) => document.querySelector(`meta[name="${n}"]`)?.getAttribute('content');
        const csrfToken  = meta('_csrf');
        const csrfHeader = meta('_csrf_header') || 'X-CSRF-TOKEN';

        try {
            const res = await fetch('/cart/add', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'Accept': 'application/json',
                    ...(csrfToken ? { [csrfHeader]: csrfToken } : {})
                },
                credentials: 'same-origin',
                body: JSON.stringify({ itemId: pathId, quantity: qty, count: qty })
            });

            if (res.status === 401) { alert('로그인이 필요합니다.'); location.href = '/login'; return; }
            if (!res.ok) throw new Error(await res.text());

            const cartItemId = await res.json();
            if (!Number.isFinite(+cartItemId)) throw new Error('cartItemId 누락');
            location.href = `/orders/checkout?cartItemIds=${encodeURIComponent(cartItemId)}`;
        } catch (err) {
            console.error('바로구매 실패:', err);
            alert('바로구매 처리 중 오류가 발생했습니다.');
        } finally {
            btn.dataset.busy = '0';
        }
    });

    // ---------- Tabs (스크롤 자동전환 제거: 클릭으로만 전환) ----------
    (() => {
        const tabs = Array.from(document.querySelectorAll('#detail-tabs .tab'));
        if (!tabs.length) return;

        const sections = {
            '#section-desc': document.querySelector('#section-desc'),
            '#section-reviews': document.querySelector('#section-reviews'),
        };
        const pageEl = document.querySelector('.item-detail-page');
        const stickyOffset = parseInt(pageEl?.dataset.stickyOffset || '0', 10) || 0;

        let active = '#section-desc';
        let userClicking = false;
        let clickTimer = null;

        const setActive = (sel, { fromScroll = false, scrollIntoView = false } = {}) => {
            if (!sections[sel]) return;
            if (!fromScroll) {
                userClicking = true;
                clearTimeout(clickTimer);
                clickTimer = setTimeout(() => (userClicking = false), 350);
            }
            tabs.forEach((b) => b.classList.remove('active'));
            const btn = tabs.find((b) => b.getAttribute('data-target') === sel);
            btn?.classList.add('active');

            Object.values(sections).forEach((sec) => sec?.classList.add('d-none'));
            sections[sel]?.classList.remove('d-none');
            active = sel;

            if (scrollIntoView) {
                const rect = sections[sel].getBoundingClientRect();
                const top = rect.top + window.scrollY - stickyOffset - 8;
                window.scrollTo({ top, behavior: 'smooth' });
            }

            if (sel === '#section-reviews' && !reviewsLoadedOnce) loadReviews(0);
        };

        // 클릭으로만 전환
        tabs.forEach((btn) => {
            btn.addEventListener('click', () => {
                const target = btn.getAttribute('data-target');
                setActive(target, { fromScroll: false, scrollIntoView: true });
            });
        });

        document.querySelectorAll('a.link-reviews').forEach((a) => {
            a.addEventListener('click', (ev) => {
                ev.preventDefault();
                setActive('#section-reviews', { fromScroll: false, scrollIntoView: true });
            });
        });

        // 초기 탭
        setActive('#section-desc', { fromScroll: true });
    })();

    // ---------- Live prepend + Avg refresh ----------
    window.addEventListener('review:created', (ev) => {
        const review = ev.detail;
        if (!review) return;

        // 카운트 +1 (즉시 반영 후, 서버 값으로 refreshItemMeta에서 보정)
        const cur = toInt($('#review-count')?.textContent || '0') + 1;
        updateReviewCountUI(cur);

        // 첫 페이지이고 이미 로드된 상태면 prepend
        const box = $('#reviews-box');
        if (box && reviewsLoadedOnce && pager.page === 0) {
            const html = renderSingleReview(review);
            const dom = document.createElement('div');
            dom.innerHTML = html.trim();
            const card = dom.firstElementChild;
            const firstCard = box.querySelector('.review-card');
            if (firstCard) box.insertBefore(card, firstCard);
            else box.innerHTML = html;
        }

        // 평균 평점 및 서버 기준 리뷰수 재조회로 동기화
        refreshItemMeta();
    });
})();
