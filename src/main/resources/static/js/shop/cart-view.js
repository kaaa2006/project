/* ================================
   cart-view.js — Cart page script
==================================*/
(() => {
    /* --------------------------------
       0) DOM/유틸
    ----------------------------------*/
    const $  = (s) => document.querySelector(s);
    const $$ = (s) => document.querySelectorAll(s);

    const cartBox    = $('#cart-box');
    const summaryBox = $('#summary-box');
    const clearBtn   = $('#clear-btn');

    const deleteCheckedBtn = $('#delete-checked-btn');
    const orderCheckedBtn  = $('#order-checked-btn');
    const orderAllBtn      = $('#order-all-btn');

    const csrfToken  = document.querySelector('meta[name="_csrf"]')?.content || '';
    const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.content || 'X-CSRF-TOKEN';

    let currentCart = null; // 현재 카트 상태

    async function safeFetch(url, options = {}) {
        const res = await fetch(url, options);
        if (res.status === 401) {
            location.href = '/login';
            throw new Error('Unauthorized');
        }
        return res;
    }

    function clampByStock(inputEl, nextQty) {
        const max = parseInt(inputEl.getAttribute('data-stock'), 10);
        if (Number.isNaN(max)) return nextQty;
        if (nextQty > max) {
            alert(`재고가 부족합니다. 최대 ${max}개까지 주문 가능합니다.`);
            return max;
        }
        return nextQty;
    }

    /* --------------------------------
      정적 버튼 이벤트 (한 번만)
    ----------------------------------*/
    let baseBound = false;
    function bindBaseActions() {
        if (baseBound) return;

        // 선택 주문
        orderCheckedBtn?.addEventListener('click', () => {
            if (!currentCart) return alert('장바구니를 불러오는 중입니다.');
            if (!currentCart.items?.length) return alert('장바구니가 비어 있습니다.');

            const checked = [...$$('.select-item:checked')].map(cb => cb.dataset.id);
            if (!checked.length) return alert('선택된 항목이 없습니다.');

            const form = document.createElement('form');
            form.method = 'GET';
            form.action = '/orders/checkout';
            checked.forEach(id => {
                const input = document.createElement('input');
                input.type = 'hidden';
                input.name = 'cartItemIds';
                input.value = id;
                form.appendChild(input);
            });
            document.body.appendChild(form);
            form.submit();
        });

        // 전체 주문
        orderAllBtn?.addEventListener('click', (e) => {
            if (!currentCart || !currentCart.items?.length) {
                e.preventDefault();
                alert('장바구니가 비어 있습니다.');
            }
        });

        // 선택 삭제
        deleteCheckedBtn?.addEventListener('click', async () => {
            if (!currentCart) return;
            if (!confirm('선택된 항목을 삭제하시겠습니까?')) return;
            await safeFetch('/cart/checked', {
                method: 'DELETE',
                headers: { [csrfHeader]: csrfToken },
            });
            loadCart();
        });

        // 비우기
        clearBtn?.addEventListener('click', async () => {
            if (!confirm('장바구니를 비우시겠습니까?')) return;
            await safeFetch('/cart/clear', {
                method: 'DELETE',
                headers: { [csrfHeader]: csrfToken },
            });
            loadCart();
        });

        baseBound = true;
    }

    /* --------------------------------
        렌더링
    ----------------------------------*/
    async function loadCart() {
        try {
            const res  = await safeFetch('/cart', { credentials: 'same-origin' });
            if (!res.ok) throw new Error(`HTTP ${res.status}`);
            const cart = await res.json();
            currentCart = cart;

            bindBaseActions(); // 정적 버튼 보장

            if (!cart.items?.length) {
                cartBox.innerHTML    = `<p class="text-muted">장바구니에 담긴 상품이 없습니다.</p>`;
                summaryBox.innerHTML = '';
                if (clearBtn) clearBtn.style.display = 'none';
                return;
            }

            const allChecked = cart.items.every((item) => item.checked);
            cartBox.innerHTML = `
              <table class="cart-table">
                <thead>
                  <tr>
                    <th><input type="checkbox" id="select-all" ${allChecked ? 'checked' : ''}></th>
                    <th>이미지</th><th>상품명</th><th>정가</th><th>세일가</th>
                    <th>수량</th><th>합계</th><th></th>
                  </tr>
                </thead>
                <tbody>
                  ${cart.items.map((item) => `
                    <tr>
                      <td><input type="checkbox" class="select-item" data-id="${item.cartItemId}" ${item.checked ? 'checked' : ''}></td>
                      <td><img src="${item.thumbnailUrl || '/img/No_Image.jpg'}" class="cart-thumbnail"
                               alt="상품 이미지" onerror="this.src='/img/No_Image.jpg'"></td>
                      <td>${item.itemName}</td>
                      <td>${item.originalPrice.toLocaleString()}원</td>
                      <td>${item.salePrice.toLocaleString()}원</td>
                      <td>
                        <div class="quantity-box">
                          <button class="quantity-btn" data-id="${item.cartItemId}" data-type="minus">-</button>
                          <input type="number" min="1" value="${item.quantity}" 
                                 data-id="${item.cartItemId}" class="quantity-input"
                                 ${item.stock ? `max="${item.stock}" data-stock="${item.stock}"` : ''}>
                          <button class="quantity-btn" data-id="${item.cartItemId}" data-type="plus">+</button>
                        </div>
                      </td>
                      <td>${item.linePayable.toLocaleString()}원</td>
                      <td><button class="btn btn-outline-danger remove-btn" data-id="${item.cartItemId}">삭제</button></td>
                    </tr>`).join('')}
                </tbody>
              </table>`;

            renderSummary(cart.checkedSummary);
            if (clearBtn) clearBtn.style.display = 'inline-block';

            bindDynamicEvents();
        } catch (err) {
            console.error(err);
            cartBox.innerHTML = `<p class="text-danger">장바구니 불러오기 실패</p>`;
        }
    }

    function renderSummary(summary) {
        if (!summary) return summaryBox.innerHTML = '';
        summaryBox.innerHTML = `
          <div class="summary">
            <div class="summary-row"><span>총 상품금액</span><span>${summary.productsTotal.toLocaleString()}원</span></div>
            <div class="summary-row"><span>등급 할인</span><span>-${summary.couponDiscount.toLocaleString()}원</span></div>
            <div class="summary-row"><span>총 배송비</span><span>${summary.shippingFee.toLocaleString()}원</span></div>
            <div class="summary-row total"><span>결제금액</span><span>${summary.payableAmount.toLocaleString()}원</span></div>
          </div>`;
    }

    /* --------------------------------
       3) 동적 엘리먼트 이벤트
    ----------------------------------*/
    function bindDynamicEvents() {
        // 수량 버튼
        $$('.quantity-btn').forEach((btn) => {
            btn.addEventListener('click', async () => {
                const id    = btn.dataset.id;
                const input = $(`input.quantity-input[data-id="${id}"]`);
                let qty     = parseInt(input.value, 10);
                qty += btn.dataset.type === 'plus' ? 1 : -1;
                if (qty < 1) return;
                qty = clampByStock(input, qty);
                await updateQuantity(id, qty);
            });
        });

        // 수량 직접 입력
        $$('.quantity-input').forEach((input) => {
            input.addEventListener('change', async () => {
                const id  = input.dataset.id;
                let qty   = Number(input.value);
                if (qty < 1 || Number.isNaN(qty)) return;
                qty = clampByStock(input, qty);
                input.value = String(qty);
                await updateQuantity(id, qty);
            });
        });

        // 개별 삭제
        $$('.remove-btn').forEach((btn) => {
            btn.addEventListener('click', async () => {
                await updateQuantity(btn.dataset.id, 0);
            });
        });

        // 개별 체크
        $$('.select-item').forEach((checkbox) => {
            checkbox.addEventListener('change', async () => {
                await safeFetch('/cart/update', {
                    method: 'PUT',
                    headers: { 'Content-Type': 'application/json', [csrfHeader]: csrfToken },
                    body: JSON.stringify({ cartItemId: checkbox.dataset.id, checked: checkbox.checked }),
                });
                loadCart();
            });
        });

        // 전체 선택
        $('#select-all')?.addEventListener('change', async (e) => {
            const checkboxes = $$('.select-item');
            checkboxes.forEach((cb) => (cb.checked = e.target.checked));
            await Promise.all(Array.from(checkboxes).map((cb) =>
                safeFetch('/cart/update', {
                    method: 'PUT',
                    headers: { 'Content-Type': 'application/json', [csrfHeader]: csrfToken },
                    body: JSON.stringify({ cartItemId: cb.dataset.id, checked: cb.checked }),
                })
            ));
            loadCart();
        });
    }

    /* --------------------------------
        API 헬퍼
    ----------------------------------*/
    async function updateQuantity(id, qty) {
        await safeFetch('/cart/update', {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json', [csrfHeader]: csrfToken },
            body: JSON.stringify({ cartItemId: id, quantity: qty }),
        });
        loadCart();
    }

    /* --------------------------------
        초기화
    ----------------------------------*/
    document.addEventListener('DOMContentLoaded', () => {
        bindBaseActions();
        loadCart();
    });
})();
