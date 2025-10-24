// ===================== Checkout — 배송지 선택 시 서버 요약 반영 =====================
(() => {
    const $  = (s, r=document) => r.querySelector(s);
    const $$ = (s, r=document) => Array.from(r.querySelectorAll(s));

    async function updateSelectedSummary() {
        const selectedRadio  = document.querySelector('input[name="addressId"]:checked');
        const addressWrapper = selectedRadio?.closest('.address-option');
        const span           = addressWrapper?.querySelector('span');
        const zipText        = span?.textContent?.trim();
        const zipcode        = zipText?.slice(0, 5); // 5자리

        const hiddenInputs = document.querySelectorAll('input[name="cartItemIds"]');
        const cartItemIds  = Array.from(hiddenInputs).map(input => input.value);

        const totalEl    = document.getElementById('selectedTotal');
        const shippingEl = document.getElementById('selectedShipping');
        if (!totalEl || !shippingEl) return;

        if (!zipcode || cartItemIds.length === 0) {
            totalEl.textContent    = '0원';
            shippingEl.textContent = '0원';
            window.dispatchEvent(new Event('shipping:change'));
            return;
        }

        try {
            const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.content || 'X-CSRF-TOKEN';
            const csrfToken  = document.querySelector('meta[name="_csrf"]')?.content  || '';

            const res = await fetch('/cart/checked-summary', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json', [csrfHeader]: csrfToken },
                body: JSON.stringify({ zipcode, cartItemIds })
            });
            if (!res.ok) throw new Error('요약 정보 조회 실패');

            const data  = await res.json();
            const s     = data?.summary || {};
            const toNum = (v) => Number.isFinite(+v) ? +v : 0;

            // 서버 요약(등급/배송비 정책 포함) — 카트 페이지와 동일 키 대응
            const productsTotal  = toNum(s.productsTotal);
            const couponDiscount = toNum(s.couponDiscount ?? s.gradeDiscount);
            const shippingFee    = toNum(s.shippingFee);
            const payableAmount  = toNum(s.payableAmount ?? (productsTotal - couponDiscount + shippingFee));

            // 전역 저장(합계 렌더러가 우선 사용)
            window.__CHECKOUT_SUMMARY__ = { productsTotal, couponDiscount, shippingFee, payableAmount };

            // 호환용 스팬은 읽어갈 소스 역할 → 값만 갱신
            const shipText = shippingFee.toLocaleString('ko-KR') + '원';
            const payText  = payableAmount.toLocaleString('ko-KR') + '원';
            if (shippingEl.textContent !== shipText) shippingEl.textContent = shipText;
            if (totalEl.textContent    !== payText ) totalEl.textContent    = payText;

            window.dispatchEvent(new Event('shipping:change'));
        } catch (e) {
            console.error(e);
            totalEl.textContent    = '오류';
            shippingEl.textContent = '오류';
            window.dispatchEvent(new Event('shipping:change'));
        }
    }

    // 배송지 변경 시
    document.addEventListener('DOMContentLoaded', () => {
        $$('input[name="addressId"]').forEach(radio => {
            radio.addEventListener('change', updateSelectedSummary);
        });

        // 초기 및 뒤로가기 복귀 시 재계산
        updateSelectedSummary();
        window.addEventListener('pageshow', updateSelectedSummary);
    });
})();

// ===================== Checkout — 합계(서버 요약 우선, 소스 불변) =====================
(function(){
    const $  = (s, r=document) => r.querySelector(s);
    const onlyDigits = (t) => parseInt(String(t||'').replace(/[^\d\-]/g,''), 10) || 0;
    const fmtKRW     = (n) => (n<0?'-':'') + Math.abs(n).toLocaleString('ko-KR') + '원';

    function renderTotals(){
        const server = window.__CHECKOUT_SUMMARY__; // {productsTotal, couponDiscount, shippingFee, payableAmount}

        let products, gradeDiscount, ship, total;

        if (server && Number.isFinite(server.productsTotal)) {
            products      = server.productsTotal;
            gradeDiscount = -Math.abs(server.couponDiscount || 0);
            ship          = server.shippingFee || 0;
            total         = server.payableAmount ?? (products + gradeDiscount + ship);
        } else {
            // 서버 요약이 아직 없을 때만 DOM에서 파생
            products      = [...document.querySelectorAll('.order-summary .order-item .price')]
                .map(el => onlyDigits(el.textContent)).reduce((a,b)=>a+b,0);
            const hostData = document.querySelector('.checkout-page')?.dataset?.gradeDiscount;
            const injected = (hostData != null) ? onlyDigits(hostData)
                : (window.__GRADE_DISCOUNT__ != null ? onlyDigits(window.__GRADE_DISCOUNT__) : 0);
            gradeDiscount = -Math.abs(injected);
            ship          = onlyDigits(document.getElementById('selectedShipping')?.textContent);
            total         = products + gradeDiscount + ship;
        }

        const sumProducts   = document.getElementById('sumProducts');
        const gradeNode     = document.getElementById('gradeDiscount');
        const sumShipping   = document.getElementById('sumShipping');
        const grandTotal    = document.getElementById('grandTotal');

        if (sumProducts) sumProducts.textContent = fmtKRW(products);
        if (gradeNode)   gradeNode.textContent   = (gradeDiscount===0? '-0원' : fmtKRW(gradeDiscount));
        if (sumShipping) sumShipping.textContent = fmtKRW(ship);
        if (grandTotal)  grandTotal.textContent  = fmtKRW(total);

        // ❌ selectedShipping/selectedTotal 은 소스 — 여기서 갱신하지 않음
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', renderTotals);
    } else {
        renderTotals();
    }

    window.addEventListener('shipping:change', renderTotals);
    window.addEventListener('cart:recalc',     renderTotals);
    setTimeout(renderTotals, 0);
})();
