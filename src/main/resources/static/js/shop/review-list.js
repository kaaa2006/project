// =========================
// /static/js/shop/review-list.js
// 내 상품후기 페이지 (목록 + 삭제만 담당)
// 등록/수정은 review-modal.js에서 처리
// =========================
(() => {
    const $ = (s, r=document) => r.querySelector(s);
    const meta = (n) => document.querySelector(`meta[name="${n}"]`)?.content || '';
    const csrfToken  = meta('_csrf');
    const csrfHeader = meta('_csrf_header') || 'X-CSRF-TOKEN';
    const pageSize   = +(meta('page-size') || 10);
    const myMno      = meta('my-mno') || '';

    let page = +(new URLSearchParams(location.search).get('page') || 0);

    const box     = $('#reviews-container');
    const pager   = $('#pager');
    const loading = $('#loading');

    const setLoading = (on) => { if (loading) loading.style.display = on ? '' : 'none'; };
    const escapeHtml = (s='') => s.replaceAll('&','&amp;').replaceAll('<','&lt;')
        .replaceAll('>','&gt;').replaceAll('"','&quot;')
        .replaceAll("'",'&#39;');
    const safeText = async (res) => { try { return await res.text(); } catch { return ''; } };
    const fmtDate = (iso) => {
        if (!iso) return '';
        const d = new Date(iso);
        if (Number.isNaN(+d)) return '';
        return `${d.getFullYear()}.${String(d.getMonth()+1).padStart(2,'0')}.${String(d.getDate()).padStart(2,'0')}`;
    };

    document.addEventListener('DOMContentLoaded', init);
    async function init() {
        if (!box) return;
        setLoading(true);
        await loadPage(page);
        setLoading(false);
        bindEvents();

        // ✅ 모달 저장(등록/수정) 성공 시 현재 페이지 목록만 다시 로드
        document.addEventListener('review:saved', async () => {
            setLoading(true);
            await loadPage(page);
            setLoading(false);
        });
    }

    async function loadPage(n) {
        const url = `/reviews/my?withImages=true&withReply=true&page=${n}&size=${pageSize}&sort=id,DESC`;
        let res;
        try {
            res = await fetch(url, { headers: { 'Accept':'application/json' } });
        } catch {
            box.innerHTML = `<div class="text-muted text-center py-4">네트워크 오류</div>`;
            return;
        }
        if (!res.ok) {
            const msg = await safeText(res);
            box.innerHTML = `<div class="text-muted text-center py-4">불러오기 실패 (${res.status})<br>${escapeHtml(msg)}</div>`;
            return;
        }
        const data = await res.json();
        renderList(data);
        renderPager(data);
    }

    function renderList(data) {
        if (!data?.content?.length) {
            box.innerHTML = `
        <div class="text-center text-muted py-5">
          아직 작성한 후기가 없습니다.<br>
          <a href="/items" class="btn btn-sm btn-brand mt-3">상품 보러가기</a>
        </div>`;
            return;
        }
        box.innerHTML = data.content.map(renderCard).join('');
    }

    function renderCard(r) {
        const writerMno = r?.writerMno ?? r?.writer?.mno ?? r?.memberId ?? '';
        const isMine    = myMno && String(writerMno) === String(myMno);
        const thumb     = r.itemThumbUrl ? r.itemThumbUrl : '/img/No_Image.jpg';

        const imgsHtml = (r.reviewImages || [])
            .map(img => `<img src="${img.imgUrl}" alt="" loading="lazy" onerror="this.src='/img/No_Image.jpg'">`)
            .join('');

        const replyHtml = r.reply ? `
      <div class="rc-reply">
        <span class="reply-label">관리자</span>
        <div class="reply-body">
          <div class="reply-text">${escapeHtml(r.reply.content || '')}</div>
          <div class="reply-meta">
            <time datetime="${r.reply.modifiedAt || r.reply.regTime}">
              ${fmtDate(r.reply.modifiedAt || r.reply.regTime)}
            </time>
          </div>
        </div>
      </div>` : '';

        return `
      <article class="review-card" data-id="${r.id}" data-rating="${r.rating}" ${isMine?'data-mine="1"':''}>
        <header class="rc-head">
          <img class="rc-thumb" src="${thumb}" alt="" onerror="this.src='/img/No_Image.jpg'">
          <div>
            <div class="rc-title">${escapeHtml(r.itemName || '')}</div>
            <div class="rc-meta">
              <time datetime="${r.modifiedAt || r.regTime || r.createdAt}">
                ${fmtDate(r.modifiedAt || r.regTime || r.createdAt)}
              </time>
              <span class="rc-rating">${renderStars(r.rating)} ${r.rating}/5</span>
            </div>
          </div>
        </header>
        <div class="rc-body">
          ${imgsHtml ? `<div class="rc-media">${imgsHtml}</div>` : ''}
          <div class="rc-text">${escapeHtml(r.content || '')}</div>
        </div>
        ${replyHtml}
        <div class="rc-actions">
          <button type="button" class="btn btn-outline-primary btn-sm js-open-review"
                  data-review-id="${r.id}"
                  data-item-id="${r.itemId}"
                  data-item-name="${escapeHtml(r.itemName || '')}"
                  data-item-img="${thumb}">수정</button>
          <button type="button" class="btn btn-outline-danger btn-sm" data-action="delete" data-id="${r.id}">삭제</button>
        </div>
      </article>`;
    }

    function renderPager(p) {
        const hasPrev = p.number > 0;
        const hasNext = p.number + 1 < p.totalPages;
        pager.innerHTML = `
      ${hasPrev ? `<a href="#" class="page-link" data-page="${p.number-1}">이전</a>` : ''}
      <span class="mx-2">${p.number+1} / ${Math.max(p.totalPages,1)}</span>
      ${hasNext ? `<a href="#" class="page-link" data-page="${p.number+1}">다음</a>` : ''}`;
    }

    function bindEvents() {
        box?.addEventListener('click', (e) => {
            const btn = e.target.closest('[data-action="delete"]');
            if (btn) onDelete(btn.dataset.id);
        });
        pager?.addEventListener('click', (e) => {
            const a = e.target.closest('a.page-link[data-page]');
            if (!a) return;
            e.preventDefault();
            page = +a.dataset.page;
            setLoading(true);
            loadPage(page).finally(() => setLoading(false));
            const usp = new URLSearchParams(location.search);
            usp.set('page', String(page));
            history.replaceState(null, '', `${location.pathname}?${usp.toString()}`);
        });
    }

    async function onDelete(id) {
        if (!confirm('정말 삭제하시겠습니까?')) return;
        try {
            const res = await fetch(`/reviews/${id}`, { method:'DELETE', headers: { [csrfHeader]: csrfToken } });
            if (res.ok || res.status === 204) {
                setLoading(true);
                await loadPage(page);
                setLoading(false);
            } else {
                alert('삭제 실패: ' + res.status + '\n' + await safeText(res));
            }
        } catch {
            alert('삭제 실패: 네트워크 오류');
        }
    }

    function renderStars(n) {
        const v = Math.max(0, Math.min(5, +n || 0));
        return '★'.repeat(v) + '☆'.repeat(5-v);
    }
})();
