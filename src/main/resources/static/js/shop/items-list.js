(() => {
    const $ = s => document.querySelector(s);

    // ---------- 루트 설정 ----------
    const root = document.querySelector('.shop-items-page');
    const API = root?.dataset.apiList || '/api/items';
    const DETAIL_BASE = root?.dataset.detailBase || '/items';
    const MOCK = root?.dataset.mock === 'on' || new URLSearchParams(location.search).has('mock');

    // ---------- DOM 요소 ----------
    const grid = $('#grid');
    const pager = $('#pagination');
    const resultCount = $('#result-count');
    const $q = $('#q');
    const $btnSearch = $('#btn-search');
    const $food = $('#filter-food');     // ← FoodItem 필터
    const $sort = $('#sort');
    const $status = $('#filter-status');
    const $minPrice = $('#min-price');
    const $maxPrice = $('#max-price');

    // ---------- 초기 상태 ----------
    const initialCategory = root?.dataset.category || '';
    const initialFoodItem = root?.dataset.foodItem || '';
    const initialSpecialDeal = root?.dataset.specialDeal === 'true';
    const initialNewItem = root?.dataset.newItem === 'true';
    const state = {
        page: 0,
        size: 12,
        q: '',
        category: initialCategory,
        foodItem: initialFoodItem,       // ← FoodItem 값
        sort: 'regTime,DESC',
        itemSellStatus: '',
        minPrice: '',
        maxPrice: '',
        specialDeal: initialSpecialDeal ? 'true' : '',
        newItem: initialNewItem ? 'true' : ''
    };

    // ---------- MOCK 데이터 ----------
    const SEED = Array.from({ length: 24 }).map((_, i) => ({
        id: i + 1,
        itemNm: `밀키트 ${i + 1}`,
        price: 7900 + (i % 7) * 600,
        stockNumber: 10 + (i % 9),
        itemSellStatus: (i % 9 === 0) ? 'SOLD_OUT' : 'SELL',
        regTime: '2025-08-18T12:00:00',
        foodItem: (i % 2 ? 'SALAD' : 'POKE'), // ← FoodItem 기반
        avgRating: 4.5,
        reviewCount: 123,
        itemViewCnt: 2345,
        repImgUrl: svg(800, 800, 'IMG')
    }));

    // ---------- 유틸 ----------
    function svg(w, h, t = 'IMG') {
        const s = `<svg xmlns='http://www.w3.org/2000/svg' width='${w}' height='${h}'><rect width='100%' height='100%' fill='#f2f2f2'/><text x='50%' y='50%' dominant-baseline='middle' text-anchor='middle' font-family='sans-serif' font-size='28' fill='#999'>${t}</text></svg>`;
        return `data:image/svg+xml;utf8,${encodeURIComponent(s)}`;
    }
    const fmtWon = n => (n ?? 0).toLocaleString('ko-KR') + '원';
    const fmtCount = n => (n ?? 0).toLocaleString('ko-KR');
    const esc = s => (s ?? '').replace(/[&<>"']/g, m => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[m]));
    const oneDecimal = n => Number.isFinite(+n) ? (+n).toFixed(1) : '0.0';
    const clamp = (v, a, b) => Math.min(Math.max(v, a), b);
    const debounce = (fn, ms = 300) => { let t; return (...args) => { clearTimeout(t); t = setTimeout(() => fn(...args), ms); }; };

    // ---------- 가격 계산 ----------
    const resolvePriceModel = (item) => {
        const sale = +item.price || 0;
        let orig = +item.originalPrice || sale;
        if (orig < sale) orig = sale;
        let rate = item.discountRate ?? Math.max(0, Math.round((orig - sale) * 100 / orig));
        return { sale, orig, rate };
    };

    // ---------- 카드 렌더 ----------
    const makeCard = (item) => {
        const { sale, orig, rate } = resolvePriceModel(item);
        const img = (item.repImgUrl && String(item.repImgUrl).trim() !== '')
            ? item.repImgUrl
            : '/img/No_Image.jpg';

        const statusHtml = item.itemSellStatus === 'SOLD_OUT'
            ? '<span class="status-pill status-soldout">품절</span>'
            : '<span class="status-pill status-on">판매중</span>';

        return `
      <div class="col-6 col-md-4 col-lg-3">
        <a class="card-item d-block text-decoration-none" href="${DETAIL_BASE}/${item.id}">
          <div class="img-wrap">
            <img class="thumb" loading="lazy" src="${img}" alt="${esc(item.itemNm)}"
                 data-fallback="/img/No_Image.jpg"
                 onerror="this.onerror=null;this.src=this.dataset.fallback">
            ${statusHtml}
          </div>
          <div class="body">
            <h3 class="item-title">${esc(item.itemNm)}</h3>
            <div class="price-line">
              <span class="discount">${rate}%</span>
              <span class="sale-price">${fmtWon(sale)}</span>
              <span class="old-price">${fmtWon(orig)}</span>
            </div>
            <div class="item-stats">
              <span class="stat"><i class="bi bi-star-fill"></i>${oneDecimal(item.avgRating)}</span>
              <span class="stat"><i class="bi bi-chat-dots"></i>${fmtCount(item.reviewCount)}</span>
              <span class="stat"><i class="bi bi-eye"></i>${fmtCount(item.itemViewCnt)}</span>
            </div>
          </div>
        </a>
      </div>`;
    };

    // ---------- 렌더링 ----------
    const render = (items, total) => {
        grid.innerHTML = items.map(makeCard).join('');
        if (resultCount) resultCount.textContent = `${fmtCount(total)}개`;
    };

    const renderPager = (totalPages, page) => {
        if (!pager) return;
        const mk = (p, label, active = false, disabled = false) =>
            `<li class="page-item ${active ? 'active' : ''} ${disabled ? 'disabled' : ''}">
                <a class="page-link" href="#" data-page="${p}">${label}</a>
             </li>`;
        const first = 0, last = Math.max(0, totalPages - 1);
        let html = mk(Math.max(page - 1, first), '‹', false, page === first);
        for (let i = 0; i < totalPages; i++) {
            if (i === 0 || i === last || Math.abs(i - page) <= 2) html += mk(i, i + 1, i === page);
            else if (!html.endsWith('…</li>')) html += `<li class="page-item disabled"><span class="page-link">…</span></li>`;
        }
        html += mk(Math.min(page + 1, last), '›', false, page === last);
        pager.innerHTML = html;
        pager.querySelectorAll('a.page-link').forEach(a => {
            a.addEventListener('click', e => {
                e.preventDefault();
                const p = +a.dataset.page;
                if (!isNaN(p)) { state.page = p; fetchList(); }
            });
        });
    };

    // ---------- Fetch ----------
    const addIf = (params, key, val) => {
        const v = (val === "''") ? '' : val;
        if (v !== '' && v != null) params.append(key, v);
    };

    const fetchList = async () => {
        if (MOCK) {
            const filtered = SEED.filter(x =>
                (!state.q || x.itemNm.includes(state.q)) &&
                (!state.foodItem || x.foodItem === state.foodItem) &&
                (!state.itemSellStatus || x.itemSellStatus === state.itemSellStatus) &&
                (!state.minPrice || x.price >= +state.minPrice) &&
                (!state.maxPrice || x.price <= +state.maxPrice)
            );
            const start = state.page * state.size;
            const end = start + state.size;
            const pageData = filtered.slice(start, end);
            render(pageData, filtered.length);
            renderPager(Math.ceil(filtered.length / state.size), state.page);
            return;
        }

        const params = new URLSearchParams();
        params.append('page', state.page);
        params.append('size', state.size);
        addIf(params, 'keyword', state.q);
        addIf(params, 'category', state.category);
        addIf(params, 'foodItem', state.foodItem);
        addIf(params, 'specialDeal', state.specialDeal);
        addIf(params, 'itemSellStatus', state.itemSellStatus);
        addIf(params, 'minPrice', state.minPrice);
        addIf(params, 'maxPrice', state.maxPrice);
        addIf(params, 'sort', state.sort);
        addIf(params, 'newItem', state.newItem);

        const res = await fetch(`${API}?${params.toString()}`, { headers: { Accept: 'application/json' } });
        const json = await res.json();
        const page = json.page ?? json;
        const items = page.content || [];
        const total = page.totalElements ?? items.length;

        render(items, total);
        renderPager(Math.ceil(total / state.size), page.number ?? state.page);
    };

    // ---------- 이벤트 ----------
    $btnSearch?.addEventListener('click', () => {
        state.q = $q.value.trim();
        state.minPrice = $minPrice?.value.trim() ?? '';
        state.maxPrice = $maxPrice?.value.trim() ?? '';
        state.page = 0;
        fetchList();
    });
    $q?.addEventListener('keydown', (e) => { if (e.key === 'Enter') $btnSearch.click(); });
    $food?.addEventListener('change', () => { state.foodItem = $food.value; state.page = 0; fetchList(); });
    $sort?.addEventListener('change', () => { state.sort = $sort.value; state.page = 0; fetchList(); });
    $status?.addEventListener('change', () => { state.itemSellStatus = $status.value; state.page = 0; fetchList(); });

    // ---------- 가격 슬라이더 초기화 ----------
    function initPriceSlider() {
        const pr = document.querySelector('.price-range');
        if (!pr) return;

        const dataMin = +pr.dataset.min || 0;
        const dataMax = +pr.dataset.max || 200000;
        const dataStep = +pr.dataset.step || 500;

        const rangeMin = document.getElementById('range-min');
        const rangeMax = document.getElementById('range-max');
        const label    = document.getElementById('price-range-label');

        [rangeMin, rangeMax].forEach(r => { r.min = dataMin; r.max = dataMax; r.step = dataStep; });

        const startMin = state.minPrice !== '' ? +state.minPrice : dataMin;
        const startMax = state.maxPrice !== '' ? +state.maxPrice : dataMax;
        rangeMin.value = clamp(startMin, dataMin, dataMax);
        rangeMax.value = clamp(startMax, dataMin, dataMax);
        $minPrice.value = rangeMin.value;
        $maxPrice.value = rangeMax.value;

        function updateFill() {
            const fill = pr.querySelector('.range-fill');
            const min = +rangeMin.value, max = +rangeMax.value;
            const pctA = ((min - dataMin) / (dataMax - dataMin)) * 100;
            const pctB = ((max - dataMin) / (dataMax - dataMin)) * 100;
            if (fill) {
                fill.style.left = pctA + '%';
                fill.style.right = (100 - pctB) + '%';
            }
            label.textContent = `${fmtWon(min)} ~ ${fmtWon(max)}`;
        }

        const applyStateAndFetch = debounce(() => {
            const min = +rangeMin.value, max = +rangeMax.value;
            state.minPrice = (min > dataMin) ? String(min) : '';
            state.maxPrice = (max < dataMax) ? String(max) : '';
            state.page = 0;
            fetchList();
        }, 250);

        function normalizeRanges() {
            if (+rangeMin.value > +rangeMax.value) {
                const mid = Math.round((+rangeMin.value + +rangeMax.value) / 2 / dataStep) * dataStep;
                rangeMin.value = mid;
                rangeMax.value = mid;
            }
            $minPrice.value = rangeMin.value;
            $maxPrice.value = rangeMax.value;
            updateFill();
        }

        rangeMin.addEventListener('input', () => { normalizeRanges(); applyStateAndFetch(); });
        rangeMax.addEventListener('input', () => { normalizeRanges(); applyStateAndFetch(); });
        $minPrice.addEventListener('change', () => {
            const v = clamp(Math.round((+$minPrice.value || dataMin) / dataStep) * dataStep, dataMin, +rangeMax.value);
            rangeMin.value = v; normalizeRanges(); applyStateAndFetch();
        });
        $maxPrice.addEventListener('change', () => {
            const v = clamp(Math.round((+$maxPrice.value || dataMax) / dataStep) * dataStep, +rangeMin.value, dataMax);
            rangeMax.value = v; normalizeRanges(); applyStateAndFetch();
        });

        updateFill();
    }

    // ---------- 가격 드롭다운 라벨 ----------
    const ddPriceLabel = document.getElementById('ddPriceLabel');
    function refreshPriceLabel() {
        const allMin = document.querySelector('.price-range')?.dataset.min ?? '0';
        const allMax = document.querySelector('.price-range')?.dataset.max ?? '200000';
        const min = state.minPrice || allMin;
        const max = state.maxPrice || allMax;
        ddPriceLabel.textContent = (state.minPrice === '' && state.maxPrice === '')
            ? '전체'
            : `${(+min).toLocaleString()}원~${(+max).toLocaleString()}원`;
    }
    document.getElementById('priceApplyBtn')?.addEventListener('click', () => {
        refreshPriceLabel();
        bootstrap.Dropdown.getOrCreateInstance(document.getElementById('ddPriceBtn')).hide();
        fetchList();
    });
    document.getElementById('priceClearBtn')?.addEventListener('click', () => {
        const pr = document.querySelector('.price-range');
        const min = +pr.dataset.min || 0;
        const max = +pr.dataset.max || 200000;
        document.getElementById('range-min').value = min;
        document.getElementById('range-max').value = max;
        $minPrice.value = '';
        $maxPrice.value = '';
        state.minPrice = '';
        state.maxPrice = '';
        state.page = 0;
        refreshPriceLabel();
        fetchList();
    });

    // ---------- 초기 실행 ----------
    refreshPriceLabel();
    initPriceSlider();
    fetchList();
})();
