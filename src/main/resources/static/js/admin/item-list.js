// src/main/resources/static/js/admin/item-list.js
(() => {
    // 중복 초기화 방지 (스크립트가 두 번 로드돼도 한 번만 바인딩)
    if (window.__ADMIN_ITEM_LIST_INIT__) return;
    window.__ADMIN_ITEM_LIST_INIT__ = true;

    console.log('INIT item-list', Date.now());

    const $  = (sel) => document.querySelector(sel);
    const $$ = (sel) => Array.from(document.querySelectorAll(sel));
    const meta = (n) => document.querySelector(`meta[name="${n}"]`)?.content;

    const API_BASE = '/api/admin/items'; // 관리자 JSON API
    window.__ADMIN_ITEM_DELETE_LOCK__ = window.__ADMIN_ITEM_DELETE_LOCK__ || new Set();

    function showToast(msg){
        const t = $('#toast');
        if (!t) return;
        t.textContent = msg;
        t.classList.add('show');
        setTimeout(() => t.classList.remove('show'), 1800);
    }

    function getCsrf() {
        const token  = meta('_csrf');
        const header = meta('_csrf_header');
        return token && header ? { header, token } : null;
    }

    async function deleteOne(id, isStop){
        // 이미 해당 id 삭제 진행 중이면 무시
        if (window.__ADMIN_ITEM_DELETE_LOCK__.has(id)) return false;
        window.__ADMIN_ITEM_DELETE_LOCK__.add(id);

        // 주문 이력 존재 여부 사전 확인
        let hasOrders = false;
        try {
            const res = await fetch(`${API_BASE}/${id}/order-exists`, { credentials:'include' });
            hasOrders = res.ok ? await res.json() : false;
        } catch {}

        const ask = hasOrders
            ? '주문이력이 있는 상품입니다. 관련 주문내역의 해당 상품 라인도 함께 삭제됩니다.\n그래도 영구 삭제하시겠습니까?'
            : (isStop
                ? '정말 영구 삭제하시겠습니까? (이미지/리뷰/좋아요 포함, 되돌릴 수 없음)'
                : '정말 삭제하시겠습니까? 이 작업은 되돌릴 수 없습니다.');

        if (!confirm(ask)) {
            window.__ADMIN_ITEM_DELETE_LOCK__.delete(id);
            return false;
        }

        const csrf = getCsrf();
        const headers = csrf ? { [csrf.header]: csrf.token } : {};

        // 해당 카드의 버튼만 잠금
        const btns = $$(`.btn-del-one[data-id="${id}"]`);
        btns.forEach(b => b.disabled = true);

        try {
            const url = `${API_BASE}/${id}${isStop ? '?forceStopOnly=true' : ''}`;
            const res = await fetch(url, { method: 'DELETE', headers, credentials:'include' });

            if (!res.ok) {
                const hdrMsg = res.headers.get('X-Delete-Message');
                let msg = hdrMsg || '삭제 실패';
                try {
                    const j = await res.clone().json();
                    if (j?.message) msg = j.message;
                } catch {}
                // FK 차단 패턴 대응(백엔드 미수정 시)
                if (res.status === 409) {
                    msg = msg || '주문 이력이 있어 영구삭제할 수 없습니다.';
                }
                showToast(msg);
                btns.forEach(b => b.disabled = false);
                window.__ADMIN_ITEM_DELETE_LOCK__.delete(id);
                return false;
            }

            // 서버 안내 헤더 처리(soft/hard/hard-forced)
            const result = res.headers.get('X-Delete-Result'); // soft | hard | hard-forced
            const msgHdr = res.headers.get('X-Delete-Message');
            const msg = msgHdr
                || (result === 'hard-forced' ? '영구 삭제되었습니다.'
                    : result === 'hard' ? '영구 삭제되었습니다.'
                        : result === 'soft' ? '판매중지(STOP)로 전환되었습니다. 보존기간 경과 후 삭제 가능합니다.'
                            : '삭제 완료');

            // 화면 업데이트
            const card = document.querySelector(`.row-check[data-id="${id}"]`)?.closest('.card');

            if (result === 'hard' || result === 'hard-forced') {
                // 영구삭제: 카드 제거
                card?.remove();
            } else if (result === 'soft' && card) {
                // soft(판매중지) 처리: 버튼/상태 갱신 후 재클릭으로 영구삭제 가능하게
                // 1) 버튼 복구 + 레이블 변경 + 상태 변경
                btns.forEach(b => {
                    b.disabled = false;
                    b.setAttribute('data-status', 'STOP');
                    const span = b.querySelector('span');
                    if (span) span.textContent = '영구삭제';
                });
                // 2) 썸네일 톤다운
                const thumb = card.querySelector('.thumb');
                thumb?.classList.add('dim');
                // 3) '판매중지' 배지 추가 (중복 방지)
                if (!card.querySelector('.status-badge-stop')) {
                    const badge = document.createElement('span');
                    badge.className = 'status-badge status-badge-stop';
                    badge.textContent = '판매중지';
                    card.querySelector('.thumb-wrap')?.appendChild(badge);
                }
            } else {
                // 기타: 버튼만 복구
                btns.forEach(b => b.disabled = false);
            }

            showToast(msg);
            window.__ADMIN_ITEM_DELETE_LOCK__.delete(id);
            return true;

        } catch (e) {
            console.error(e);
            showToast('삭제 중 오류 발생');
            btns.forEach(b => b.disabled = false);
            window.__ADMIN_ITEM_DELETE_LOCK__.delete(id);
            return false;
        }
    }

    // 개별 삭제 이벤트 위임
    document.addEventListener('click', (e) => {
        const btn = e.target.closest('.btn-del-one');
        if (!btn) return;
        const id = btn.getAttribute('data-id');
        if (!id) return;
        const st = (btn.getAttribute('data-status') || '').toUpperCase();
        const isStop = st === 'STOP';
        deleteOne(id, isStop);
    });

    // 선택 삭제/전체선택
    const checkAll = $('#check-all');
    const bulkBtn  = $('#btn-bulk-delete');

    function updateBulkState(){
        const anyChecked = $$('.row-check:checked').length > 0;
        if (bulkBtn) {
            bulkBtn.disabled = !anyChecked;
            bulkBtn.textContent = anyChecked ? `선택 삭제 (${$$('.row-check:checked').length})` : '선택 삭제';
        }
    }

    document.addEventListener('change', (e) => {
        if (e.target && e.target.id === 'check-all') {
            const on = e.target.checked;
            $$('.row-check').forEach(c => { c.checked = on; });
            updateBulkState();
        }
        if (e.target && e.target.classList.contains('row-check')) {
            if (!e.target.checked && checkAll?.checked) {
                checkAll.checked = false;
            } else if ($$('.row-check:not(:checked)').length === 0) {
                if (checkAll) checkAll.checked = true;
            }
            updateBulkState();
        }
    });

    bulkBtn?.addEventListener('click', async () => {
        const rows = $$('.row-check:checked');
        const targets = rows.map(c => ({
            id: c.getAttribute('data-id'),
            isStop: (c.getAttribute('data-status') || '').toUpperCase() === 'STOP'
        })).filter(t => t.id);

        if (targets.length === 0) return;
        if (!confirm(`선택한 ${targets.length}개 항목을 삭제할까요? (STOP은 영구삭제)`)) return;

        bulkBtn.disabled = true;
        let ok = 0;
        for (const {id, isStop} of targets) {
            const success = await deleteOne(id, isStop);
            if (success) ok++;
        }
        bulkBtn.disabled = false;
        showToast(`삭제 완료: ${ok}/${targets.length}`);
        updateBulkState();
    });

    // 날짜 표시 포맷
    (function formatDates(){
        const els = document.querySelectorAll('.js-date[data-iso]');
        if (!els.length) return;
        const fmt = new Intl.DateTimeFormat('ko-KR', { year:'numeric', month:'2-digit', day:'2-digit' });
        els.forEach(el => {
            const iso = el.getAttribute('data-iso');
            if (!iso) return;
            const d = new Date(iso);
            if (isNaN(d.getTime())) return;
            const parts = fmt.formatToParts(d).reduce((acc,p) => { acc[p.type]=p.value; return acc; }, {});
            el.textContent = `${parts.year}.${parts.month}.${parts.day}`;
        });
    })();
})();
