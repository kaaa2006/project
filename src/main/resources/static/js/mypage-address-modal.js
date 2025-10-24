(function(){
    const $  = (s, r=document)=>r.querySelector(s);

    // ❗ 중복 로딩 방지
    if (window.__ADDR_SCRIPT_BOUND__) return;
    window.__ADDR_SCRIPT_BOUND__ = true;

    /* ---------- 상태 ---------- */
    let editingId = null; // null이면 추가, 숫자면 수정

    /* ---------- 모달 ---------- */
    function openModal(){
        const modal = $('#addrModal'); if(!modal) return;
        const sbw = window.innerWidth - document.documentElement.clientWidth;
        document.documentElement.style.setProperty('--sbw', sbw + 'px');
        document.documentElement.classList.add('pf-modal-open');
        document.body.classList.add('pf-modal-open');
        modal.setAttribute('aria-hidden', 'false');
        setTimeout(()=> $('#addrForm input[name="alias"]')?.focus(), 0);
        document.addEventListener('keydown', onEsc);
    }
    function closeModal(){
        const modal = $('#addrModal'); if(!modal) return;
        modal.setAttribute('aria-hidden', 'true');
        document.removeEventListener('keydown', onEsc);
        document.documentElement.classList.remove('pf-modal-open');
        document.body.classList.remove('pf-modal-open');
        document.documentElement.style.removeProperty('--sbw');
        $('#addrForm')?.reset();
        editingId = null;
        $('#addrModalTitle') && ($('#addrModalTitle').textContent = '배송지 추가');
        $('#addrSubmitBtn') && ($('#addrSubmitBtn').textContent = '추가하기');
    }
    function onEsc(e){ if(e.key==='Escape') closeModal(); }

    /* ---------- 폼 채우기/비우기 ---------- */
    function fillForm(data){
        const f = $('#addrForm'); if(!f) return;
        f.alias.value   = data.alias   ?? '';
        f.zipCode.value = data.zipCode ?? '';
        f.addr1.value   = data.addr1   ?? '';
        f.addr2.value   = data.addr2   ?? '';
        f.isDefault.checked = !!data.isDefault;
    }
    function resetForm(){
        const f = $('#addrForm'); if(!f) return;
        f.reset();
    }

    /* ---------- 테이블 유틸 ---------- */
    function sanitize(s){ return (s ?? '').toString(); }

    function rowHtml(addr){
        return `
      <td>${sanitize(addr.alias) || '-'}</td>
      <td>${sanitize(addr.zipCode)}</td>
      <td>${sanitize(addr.addr1)}</td>
      <td>${sanitize(addr.addr2) || '-'}</td>
      <td>${addr.isDefault ? '<span class="pf-badge pf-badge-primary">기본</span>' : '<span class="text-muted">-</span>'}</td>
      <td class="text-end">
        <button type="button" class="pf-btn pf-btn-outline pf-btn-xs" data-action="edit-address">수정</button>
        <button type="button" class="pf-btn pf-btn-danger pf-btn-xs" data-action="delete-address">삭제</button>
      </td>
    `;
    }

    function applyDefaultBadgeRule(tbody){
        // 기본 배송지 한 개만 표시되도록 보정
        const rows = Array.from(tbody.querySelectorAll('tr'));
        const hasDefault = rows.some(tr => tr.querySelector('.pf-badge-primary'));
        if (!hasDefault && rows.length){
            // 아무것도 기본이 아니라면 첫 행을 기본으로 표기 X (서버 기준이므로 프론트 강제는 생략)
        }
    }

    function prependRow(addr){
        const tbody = $('#addrTbody'); const table = $('#addrTable'); const empty = $('#addrEmpty');
        if (empty) empty.classList.add('d-none');
        if (table) table.classList.remove('d-none');

        // 새 기본이면 기존 기본 뱃지 정리
        if (addr.isDefault && tbody){
            tbody.querySelectorAll('td:nth-last-child(2) .pf-badge-primary').forEach(b=>{
                const cell = b.parentElement;
                b.remove();
                const dash = document.createElement('span'); dash.className='text-muted'; dash.textContent='-';
                cell.appendChild(dash);
            });
        }

        const tr = document.createElement('tr');
        tr.setAttribute('data-address-id', addr.id);
        tr.setAttribute('data-alias',  addr.alias ?? '');
        tr.setAttribute('data-zip',    addr.zipCode ?? '');
        tr.setAttribute('data-addr1',  addr.addr1 ?? '');
        tr.setAttribute('data-addr2',  addr.addr2 ?? '');
        tr.setAttribute('data-default', !!addr.isDefault);
        tr.innerHTML = rowHtml(addr);
        tbody?.prepend(tr);
        applyDefaultBadgeRule(tbody);
    }

    function updateRow(addr){
        const tr = document.querySelector(`#addrTbody tr[data-address-id="${addr.id}"]`);
        if (!tr) { prependRow(addr); return; }

        // 새 기본이면 다른 기본 뱃지 삭제
        if (addr.isDefault) {
            document.querySelectorAll('#addrTbody tr').forEach(row=>{
                if (row === tr) return;
                const badge = row.querySelector('.pf-badge-primary');
                if (badge) {
                    const cell = badge.parentElement; badge.remove();
                    const dash = document.createElement('span'); dash.className='text-muted'; dash.textContent='-';
                    cell.appendChild(dash);
                }
            });
        }

        tr.setAttribute('data-alias',  addr.alias ?? '');
        tr.setAttribute('data-zip',    addr.zipCode ?? '');
        tr.setAttribute('data-addr1',  addr.addr1 ?? '');
        tr.setAttribute('data-addr2',  addr.addr2 ?? '');
        tr.setAttribute('data-default', !!addr.isDefault);
        tr.innerHTML = rowHtml(addr);
    }

    function removeRow(id){
        const tr = document.querySelector(`#addrTbody tr[data-address-id="${id}"]`);
        if (tr) tr.remove();
        const tbody = $('#addrTbody');
        if (tbody && tbody.children.length === 0){
            $('#addrTable')?.classList.add('d-none');
            $('#addrEmpty')?.classList.remove('d-none');
        }
    }

    /* ---------- 다음 주소 API ---------- */
    function openDaumPostcode(){
        if (!window.daum || !daum.Postcode) {
            // 스크립트가 아직 로드 안된 경우 한번만 동적 로드
            if (window.__DAUM_LOADING__) return;
            window.__DAUM_LOADING__ = true;
            const s = document.createElement('script');
            s.src = 'https://t1.daumcdn.net/mapjsapi/bundle/postcode/prod/postcode.v2.js';
            s.onload = ()=> new daum.Postcode({
                oncomplete: fillFromDaum
            }).open();
            document.head.appendChild(s);
            return;
        }
        new daum.Postcode({ oncomplete: fillFromDaum }).open();
    }
    function fillFromDaum(data){
        const addr = data.roadAddress || data.jibunAddress;
        const f = $('#addrForm'); if(!f) return;
        f.zipCode.value = data.zonecode || '';
        f.addr1.value   = addr || '';
        f.addr2.focus();
    }

    /* ---------- 이벤트 바인딩 ---------- */
    document.addEventListener('click', (e)=>{
        const openBtn  = e.target.closest('[data-action="open-address-modal"]');
        const closeBtn = e.target.closest('[data-action="close-address-modal"]');
        const editBtn  = e.target.closest('[data-action="edit-address"]');
        const delBtn   = e.target.closest('[data-action="delete-address"]');
        const findZip  = e.target.closest('#btnFindZip');

        if (openBtn){
            e.preventDefault(); e.stopPropagation();
            editingId = null;
            resetForm();
            $('#addrModalTitle').textContent = '배송지 추가';
            $('#addrSubmitBtn').textContent = '추가하기';
            openModal();
            return;
        }
        if (closeBtn){
            e.preventDefault(); e.stopPropagation();
            closeModal();
            return;
        }
        if (findZip){
            e.preventDefault(); e.stopPropagation();
            openDaumPostcode();
            return;
        }
        if (editBtn){
            e.preventDefault(); e.stopPropagation();
            const tr = editBtn.closest('tr');
            if (!tr) return;
            editingId = tr.dataset.addressId;
            fillForm({
                alias: tr.dataset.alias,
                zipCode: tr.dataset.zip,
                addr1: tr.dataset.addr1,
                addr2: tr.dataset.addr2,
                isDefault: tr.dataset.default === 'true'
            });
            $('#addrModalTitle').textContent = '배송지 수정';
            $('#addrSubmitBtn').textContent = '저장하기';
            openModal();
            return;
        }
        if (delBtn){
            e.preventDefault(); e.stopPropagation();
            const tr = delBtn.closest('tr'); if(!tr) return;
            const id = tr.dataset.addressId;
            if (!window.confirm('이 배송지를 삭제할까요?')) return;

            const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.content || 'X-CSRF-TOKEN';
            const csrfToken  = document.querySelector('meta[name="_csrf"]')?.content || '';

            fetch(`/mypage/address/${id}`, {
                method: 'DELETE',
                headers: csrfToken ? { [csrfHeader]: csrfToken } : {}
            }).then(res=>{
                if (res.ok) removeRow(id);
            });
            return;
        }
    });

    /* ---------- 제출(추가/수정 통합) ---------- */
    document.addEventListener('DOMContentLoaded', ()=>{
        const f = $('#addrForm'); if(!f) return;

        // 숫자만
        const zip = f.querySelector('input[name="zipCode"]');
        zip && zip.addEventListener('input', ()=>{ zip.value = zip.value.replace(/\D/g,'').slice(0,5); });

        const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.content || 'X-CSRF-TOKEN';
        const csrfToken  = document.querySelector('meta[name="_csrf"]')?.content || '';

        f.addEventListener('submit', (e)=>{
            e.preventDefault();

            const payload = {
                alias:   f.alias?.value.trim(),
                zipCode: f.zipCode?.value.trim(),
                addr1:   f.addr1?.value.trim(),
                addr2:   f.addr2?.value.trim(),
                isDefault: !!f.isDefault?.checked
            };
            if (!payload.zipCode || !payload.addr1) return;

            const isEdit = !!editingId;
            const url    = isEdit ? `/mypage/address/${editingId}` : '/mypage/address';
            const method = isEdit ? 'PUT' : 'POST';

            fetch(url, {
                method,
                headers: { 'Content-Type': 'application/json', ...(csrfToken?{[csrfHeader]:csrfToken}:{}) },
                body: JSON.stringify(payload)
            }).then(async (res)=>{
                if(!res.ok) return;
                const data = await res.json(); // AddressResponseDTO
                if (isEdit) updateRow(data); else prependRow(data);
                closeModal();
            });
        });
    });

})();