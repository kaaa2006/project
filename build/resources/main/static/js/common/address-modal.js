// address-modal.js — 공통(프로필/체크아웃) 모달 동작
(() => {
    const $  = (s, r=document) => r.querySelector(s);
    const $$ = (s, r=document) => Array.from(r.querySelectorAll(s));

    if (window.__ADDR_MODAL_BOUND__) return;
    window.__ADDR_MODAL_BOUND__ = true;

    function openModal(){
        const modal = $('#addrModal'); if(!modal) return;
        document.documentElement.classList.add('pf-modal-open');
        document.body.classList.add('pf-modal-open');
        modal.setAttribute('aria-hidden','false');
        setTimeout(()=>$('#addrForm input[name="alias"]')?.focus(),0);
        document.addEventListener('keydown', onEsc);
    }
    function closeModal(){
        const modal = $('#addrModal'); if(!modal) return;
        modal.setAttribute('aria-hidden','true');
        document.removeEventListener('keydown', onEsc);
        document.documentElement.classList.remove('pf-modal-open');
        document.body.classList.remove('pf-modal-open');
        $('#addrForm')?.reset();
        const title = $('#addrModalTitle'); if (title) title.textContent = '배송지 추가';
        const btn   = $('#addrSubmitBtn'); if (btn)   btn.textContent   = '추가하기';
    }
    function onEsc(e){ if(e.key==='Escape') closeModal(); }

    function openDaumPostcode(){
        const onReady = () => new daum.Postcode({ oncomplete: fillFromDaum }).open();
        if (!window.daum || !daum.Postcode){
            if (window.__DAUM_LOADING__) return;
            window.__DAUM_LOADING__ = true;
            const s = document.createElement('script');
            s.src   = 'https://t1.daumcdn.net/mapjsapi/bundle/postcode/prod/postcode.v2.js';
            s.onload= onReady;
            document.head.appendChild(s);
            return;
        }
        onReady();
    }
    function fillFromDaum(data){
        const f = $('#addrForm'); if(!f) return;
        f.zipCode.value = data.zonecode || '';
        f.addr1.value   = (data.roadAddress || data.jibunAddress || '');
        f.addr2.focus();
    }

    // 위임 클릭: 열기/닫기/우편번호검색
    document.addEventListener('click', (e)=>{
        const openBtn = e.target.closest('[data-action="open-address-modal"]');
        const closeBtn= e.target.closest('[data-action="close-address-modal"]');
        const findZip = e.target.closest('#btnFindZip');
        if (openBtn){ e.preventDefault(); $('#addrForm')?.reset(); openModal(); }
        if (closeBtn){ e.preventDefault(); closeModal(); }
        if (findZip){ e.preventDefault(); openDaumPostcode(); }
    });

    // 초기 바인딩
    document.addEventListener('DOMContentLoaded', ()=>{
        const f = $('#addrForm'); if (!f) return;

        // 우편번호 숫자만
        const zip = f.querySelector('input[name="zipCode"]');
        if (zip) zip.addEventListener('input', ()=>{
            zip.value = zip.value.replace(/\D/g,'').slice(0,5);
        });

        const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.content || 'X-CSRF-TOKEN';
        const csrfToken  = document.querySelector('meta[name="_csrf"]')?.content || '';

        f.addEventListener('submit', async (e)=>{
            e.preventDefault();
            try{
                const res = await fetch('/orders/address',{
                    method:'POST',
                    headers:{ 'Content-Type':'application/x-www-form-urlencoded;charset=UTF-8', ...(csrfToken?{[csrfHeader]:csrfToken}:{}) },
                    body: new URLSearchParams(new FormData(f)),
                    redirect:'follow'
                });
                if (!res.ok){ alert('주소 저장에 실패했습니다.'); return; }

                // checkout 페이지면: 현재 쿼리(cartItemIds 등) 유지 + 서버가 제공한 selected 우선
                const isCheckout = !!document.querySelector('.checkout-page');
                if (isCheckout){
                    const target = (res.redirected && res.url) ? new URL(res.url) : new URL('/orders/checkout', location.origin);
                    const cur    = new URL(location.href);
                    cur.searchParams.forEach((v,k)=>{ if(k!=='selected') target.searchParams.append(k,v); });
                    location.href = target.toString();
                    return;
                }

                // 그 외 페이지(프로필 등)는 페이지 자체 스크립트가 갱신 처리
                closeModal();
                document.dispatchEvent(new CustomEvent('address:created', { bubbles:true }));
            }catch(err){
                console.error(err);
                alert('서버와 통신 중 오류가 발생했습니다.');
            }
        });
    });
})();
