// ========================================================
// TIP Board — Read (Detail only)
// - 좋아요/싫어요 토글, 댓글 등록/수정/삭제
// - CSRF/로그인/데이터셋 방어 코드 포함
// - tipBoard.js와 공존 가능(중복 바인딩 방지)
// ========================================================
(function () {
    // 이 페이지가 아닌 경우 종료
    const isDetail = document.body.classList.contains('tip-read') || document.querySelector('.tip-content');
    if (!isDetail) return;

    // ----- 공통: CSRF / 로그인 / 글 번호 -----
    const csrfToken  = document.querySelector('meta[name="_csrf"]')?.content || null;
    const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.content || null;

    const isLoggedStr = (document.body.dataset.isLoggedIn ?? '').toString().toLowerCase();
    const IS_LOGGED_IN = ['true', '1', 'yes'].includes(isLoggedStr);

    const commentsContainer = document.getElementById('comments-container');
    const BNO = document.querySelector('meta[name="bno"]')?.content || commentsContainer?.dataset.bno;
    const MEMBER_ID = document.querySelector('meta[name="member-id"]')?.content || commentsContainer?.dataset.mno;

    // 유틸: 헤더 병합
    function withCsrf(headers) {
        if (csrfHeader && csrfToken) return Object.assign({}, headers, { [csrfHeader]: csrfToken });
        return headers || {};
    }

    // 유틸: 날짜 포맷 (YYYY-MM-DD HH:mm)
    function fmtNow() {
        const d = new Date();
        return d.toLocaleString('ko-KR', { year:'numeric', month:'2-digit', day:'2-digit', hour:'2-digit', minute:'2-digit' });
    }

    // ======================================================
    // 1) 반응(좋아요/싫어요)
    // ======================================================
    const reactionWrap = document.querySelector('.reaction');
    const btnBox       = document.querySelector('.tip-action-buttons');
    const btnLike      = document.querySelector('.btn-helpful');
    const btnDislike   = document.querySelector('.btn-not-helpful');

    // 초기 상태(서버에서 내려준 userReaction 적용)
    (function initReactionState() {
        if (!reactionWrap || !btnBox) return;
        const current = (reactionWrap.dataset.userReaction || '').toLowerCase();
        btnBox.classList.remove('left', 'right');
        btnLike?.classList.remove('active');
        btnDislike?.classList.remove('active');
        if (current === 'like')  { btnBox.classList.add('left');  btnLike?.classList.add('active'); }
        if (current === 'dislike'){ btnBox.classList.add('right'); btnDislike?.classList.add('active'); }
    })();

    function sendReaction(type) {
        if (!IS_LOGGED_IN) {
            if (confirm('로그인이 필요합니다. 로그인 페이지로 이동하시겠습니까?')) location.href = '/login';
            return;
        }
        const params = new URLSearchParams();
        params.append('tipBoardId', BNO);
        params.append('type', type);

        fetch('/board/reaction/tip/toggle', {
            method: 'POST',
            headers: withCsrf({ 'Content-Type': 'application/x-www-form-urlencoded' }),
            body: params.toString()
        })
            .then(res => res.json())
            .then(data => {
                const likeCntEl = btnLike?.querySelector('.count');
                const dislikeCntEl = btnDislike?.querySelector('.count');

                if (likeCntEl && data.likeCount != null) likeCntEl.textContent = data.likeCount;
                if (dislikeCntEl && data.dislikeCount != null) dislikeCntEl.textContent = data.dislikeCount;

                // 활성 토글
                btnLike?.classList.toggle('active', data.userReaction === 'like');
                btnDislike?.classList.toggle('active', data.userReaction === 'dislike');
                btnBox?.classList.remove('left', 'right');
                if (data.userReaction === 'like') btnBox?.classList.add('left');
                if (data.userReaction === 'dislike') btnBox?.classList.add('right');

                reactionWrap.dataset.userReaction = data.userReaction || '';
            })
            .catch(err => console.error('[TIP][reaction] error:', err));
    }

    // 중복 바인딩 방지
    if (btnLike && !btnLike.dataset.bound) {
        btnLike.dataset.bound = '1';
        btnLike.addEventListener('click', () => sendReaction('like'));
    }
    if (btnDislike && !btnDislike.dataset.bound) {
        btnDislike.dataset.bound = '1';
        btnDislike.addEventListener('click', () => sendReaction('dislike'));
    }

    // ======================================================
    // 2) 댓글 등록
    // ======================================================
    const btnAdd = document.getElementById('btn-add-comment');
    if (btnAdd && !btnAdd.dataset.bound) {
        btnAdd.dataset.bound = '1';
        btnAdd.addEventListener('click', () => {
            const textarea = document.getElementById('reply-text');
            const content = (textarea?.value || '').trim();
            if (!IS_LOGGED_IN) { alert('로그인이 필요합니다.'); location.href = '/login'; return; }
            if (!content) { alert('댓글 내용을 입력해주세요.'); return; }

            fetch('/tip/replies', {
                method: 'POST',
                headers: withCsrf({ 'Content-Type': 'application/json' }),
                body: JSON.stringify({ tipBoardId: BNO, replyText: content, writerId: MEMBER_ID })
            })
                .then(res => res.json())
                .then(data => {
                    if (!data || !data.rno) { alert('댓글 등록 실패'); return; }

                    // UL 확보
                    let ul = commentsContainer.querySelector('ul');
                    if (!ul) {
                        ul = document.createElement('ul');
                        commentsContainer.appendChild(ul);
                    }
                    // 새로운 LI 생성
                    const li = document.createElement('li');
                    li.dataset.rno = data.rno;
                    li.innerHTML = `
            <div class="comment-header">
              <strong>${data.writerName ?? '익명'}</strong>
              <span class="comment-date">${fmtNow()}</span>
              <div class="comment-buttons">
                <button class="btn-edit">수정</button>
                <button class="btn-delete">삭제</button>
              </div>
            </div>
            <div class="reply-text"></div>
          `;
                    li.querySelector('.reply-text').textContent = data.replyText ?? content;
                    ul.prepend(li);
                    if (textarea) textarea.value = '';

                    bindCommentRow(li); // 수정/삭제 바인딩
                })
                .catch(err => { console.error('[TIP][reply add] error:', err); alert('댓글 등록 중 오류가 발생했습니다.'); });
        });
    }

    // ======================================================
    // 3) 댓글 수정/삭제 (기존 항목 포함)
    // ======================================================
    function bindCommentRow(li) {
        const btnEdit = li.querySelector('.btn-edit');
        const btnDel  = li.querySelector('.btn-delete');
        const rno     = li.dataset.rno;

        if (btnEdit && !btnEdit.dataset.bound) {
            btnEdit.dataset.bound = '1';
            btnEdit.addEventListener('click', () => {
                const textEl = li.querySelector('.reply-text');
                if (!textEl) return;

                // 인라인 편집 입력으로 교체
                const prev = textEl.textContent;
                const input = document.createElement('input');
                input.type = 'text';
                input.value = prev;
                textEl.replaceWith(input);
                btnEdit.textContent = '저장';

                const save = () => {
                    const newText = input.value.trim();
                    if (!newText) { alert('댓글 내용을 입력해주세요.'); return; }

                    fetch(`/tip/replies/${rno}`, {
                        method: 'PUT',
                        headers: withCsrf({ 'Content-Type': 'application/json' }),
                        body: JSON.stringify({ tipBoardId: Number(BNO), replyText: newText })
                    })
                        .then(res => res.json())
                        .then(data => {
                            if (!data || !data.rno) { alert('댓글 수정 실패'); return; }
                            const span = document.createElement('div');
                            span.className = 'reply-text';
                            span.textContent = newText;
                            input.replaceWith(span);
                            btnEdit.textContent = '수정';
                            // 다시 바인딩 초기화
                            btnEdit.dataset.bound = '';
                            btnDel?.dataset && (btnDel.dataset.bound = '');
                            bindCommentRow(li);
                        })
                        .catch(err => console.error('[TIP][reply edit] error:', err));
                };

                // 저장 이벤트 재바인딩
                btnEdit.addEventListener('click', save, { once: true });
            });
        }

        if (btnDel && !btnDel.dataset.bound) {
            btnDel.dataset.bound = '1';
            btnDel.addEventListener('click', () => {
                if (!confirm('댓글을 삭제하시겠습니까?')) return;
                fetch(`/tip/replies/${rno}`, {
                    method: 'DELETE',
                    headers: withCsrf()
                })
                    .then(res => {
                        if (res.ok) li.remove();
                        else return res.json().then(err => { throw new Error(err.message || '삭제 실패'); });
                    })
                    .catch(err => alert(err.message));
            });
        }
    }

    // 기존 댓글들 바인딩
    commentsContainer?.querySelectorAll('li[data-rno]').forEach(bindCommentRow);
})();
