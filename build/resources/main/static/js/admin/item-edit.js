/* src/main/resources/static/js/admin/item-edit.js — 최종본 (등록/수정 공용) */
(() => {
    // ===== 중복 로드 방지 =====
    if (window.__ITEM_EDIT_SCRIPT_LOADED__) return;
    window.__ITEM_EDIT_SCRIPT_LOADED__ = true;

    // ===== 공통 유틸 =====
    const qs = (s, r = document) => r.querySelector(s);
    const qa = (s, r = document) => Array.from(r.querySelectorAll(s));
    const on = (el, ev, fn, opt) => el && el.addEventListener(ev, fn, opt);
    const digits = s => String(s || '').replace(/[^\d]/g, '');
    const toNum = s => Number(digits(s));
    const clamp = (v, min, max) => Math.min(Math.max(+v || 0, min), max);
    const esc = s => String(s || '')
        .replaceAll('&', '&amp;').replaceAll('<', '&lt;')
        .replaceAll('>', '&gt;').replaceAll('"', '&quot;')
        .replaceAll("'", '&#039;');

    const apiBase = () =>
        qs('meta[name="items-api-base"]')?.content ||
        qs('body')?.dataset.api ||
        '/api/admin/items';

    const isNewPage  = () => !!qs('[data-page="new"]');
    const isEditPage = () => !!qs('[data-page="edit"]');

    // CSRF + fetch 래퍼
    const csrf = () => {
        const metaTok = qs('meta[name="_csrf"]')?.content || '';
        const metaHdr = qs('meta[name="_csrf_header"]')?.content || '';
        const cookieTok = decodeURIComponent(
            (document.cookie.split(';').map(s => s.trim()).find(s => s.startsWith('XSRF-TOKEN=')) || '').split('=')[1] || ''
        );
        const token = metaTok || cookieTok || '';
        return { token, metaHdr };
    };
    const jfetch = (url, opt = {}) => {
        const { token, metaHdr } = csrf();
        const headers = {
            'Accept': 'application/json',
            'X-Requested-With': 'XMLHttpRequest',
            ...(token ? { [metaHdr || 'X-CSRF-TOKEN']: token, 'X-XSRF-TOKEN': token } : {}),
            ...(opt.headers || {}),
        };
        return fetch(url, { credentials: 'include', headers, ...opt });
    };

    // 텍스트 영역 커서 위치에 문자열 삽입
    const insertAtCursor = (ta, text) => {
        const start = ta.selectionStart ?? ta.value.length;
        const end   = ta.selectionEnd ?? ta.value.length;
        ta.value = ta.value.slice(0, start) + text + ta.value.slice(end);
        ta.selectionStart = ta.selectionEnd = start + text.length;
        ta.dispatchEvent(new Event('change', { bubbles: true }));
    };

    // 상세 HTML 내 <img src> 수집(중복 삽입 방지용)
    const collectDetailSrcSet = () => {
        const ta = qs('#itemDetail');
        if (!ta) return new Set();
        const holder = document.createElement('div');
        holder.innerHTML = ta.value || '';
        return new Set([...holder.querySelectorAll('img[src]')].map(i => (i.getAttribute('src') || '').trim()));
    };

    // 미리보기용 ObjectURL 해제
    const revokeAllThumbURLs = () => {
        try {
            qa('#thumbs img, #detailFilesPreview img').forEach(img => {
                const u = img.getAttribute('src');
                if (u && u.startsWith('blob:')) URL.revokeObjectURL(u);
            });
        } catch {}
    };

    // ===== 가격 입력 & 미리보기 =====
    const computeSale = (op, dr) => Math.max(0, Math.round(toNum(op) * (1 - clamp(toNum(dr), 0, 95) / 100)));
    function bindPrice(root) {
        const f   = qs('#itemForm', root) || root;
        const $op = qs('input[name="originalPrice"]', f);
        const $dr = qs('input[name="discountRate"]', f);
        const outEnsure = () => {
            let out = qs('#salePriceValue');
            if (!out) {
                const wrap = document.createElement('div');
                wrap.className = 'help'; wrap.id = 'salePricePreview'; wrap.style.marginTop = '-6px';
                wrap.innerHTML = '실판매가: <b id="salePriceValue">-</b> 원';
                (qs('input[name="discountRate"]', f)?.closest('.row')?.parentElement || f).appendChild(wrap);
                out = qs('#salePriceValue', wrap);
            }
            return out;
        };
        const $out = outEnsure();
        const upd = () => ($out.textContent = computeSale($op?.value, $dr?.value).toLocaleString());
        const allowNum = e => {
            const k = e.key, c = e.ctrlKey || e.metaKey;
            const ok = ['Backspace','Delete','Tab','Enter','ArrowLeft','ArrowRight','Home','End'].includes(k)
                || (c && 'acvzxACVZX'.includes(k)) || /^\d$/.test(k);
            if (!ok) e.preventDefault();
        };
        if ($op) {
            on($op, 'keydown', allowNum);
            on($op, 'input', () => {
                const n = toNum($op.value);
                $op.value = n ? n.toLocaleString() : '';
                $op.dispatchEvent(new Event('change', { bubbles: true }));
                upd();
            });
            on($op, 'blur', () => $op.dispatchEvent(new Event('input')));
        }
        if ($dr) {
            on($dr, 'keydown', allowNum);
            const clampDR = () => {
                const n = clamp(toNum($dr.value), 0, 95);
                if (digits($dr.value) !== String(n)) $dr.value = String(n);
                $dr.dispatchEvent(new Event('change', { bubbles: true }));
                upd();
            };
            on($dr, 'input', clampDR);
            on($dr, 'blur', clampDR);
        }
        upd();
    }

    // ===== 상세 HTML 저장 전 유효성 검사 (data:/blob:/file: 금지) =====
    function validateDetailHtml() {
        const detail = qs('#itemDetail');
        if (!detail) return true;
        const holder = document.createElement('div');
        holder.innerHTML = detail.value || '';
        const bad = [...holder.querySelectorAll('img')].filter(img => {
            const src = (img.getAttribute('src') || '').trim().toLowerCase();
            const ok = !!src && (src.startsWith('/images/') || src.startsWith('http://') || src.startsWith('https://') || src.startsWith('/'));
            const badProto = src.startsWith('data:') || src.startsWith('blob:') || src.startsWith('file:');
            return !ok || badProto;
        });
        if (bad.length) {
            alert('본문 내 이미지 중 업로드되지 않은 항목이 있습니다. 파일 선택으로 서버 업로드 후 저장해 주세요.');
            return false;
        }
        return true;
    }

    // ===== 신규 업로드(상품 이미지) 상태/미리보기 =====
    const editState = { productFiles: [], keys: new Set() };
    const fileKey = f => `${f.name}|${f.size}|${f.lastModified}`;

    function renderThumbs() {
        const $c = qs('#thumbs');
        const $rep = qs('#repIndex');
        if (!$c || !$rep) return;

        const prevSel = $rep.value;
        $rep.innerHTML = '<option value="">(자동 지정)</option>';
        for (let i = 0; i < editState.productFiles.length; i++) {
            const opt = document.createElement('option');
            opt.value = String(i);
            opt.textContent = `${i}번(추가 ${i + 1})`;
            $rep.appendChild(opt);
        }
        if (prevSel !== '' && +prevSel < editState.productFiles.length) $rep.value = prevSel;
        else if (editState.productFiles.length) $rep.value = '0';

        $c.innerHTML = '';
        editState.productFiles.forEach((f, i) => {
            const u = URL.createObjectURL(f);
            const name = esc(f.name.length > 28 ? f.name.slice(0, 27) + '…' : f.name);
            const card = document.createElement('div');
            card.className = 'thumb';
            card.draggable = true;
            card.innerHTML = `
        <button type="button" class="btn-icon btn-del" aria-label="삭제">×</button>
        <img alt="미리보기 ${i + 1}" src="${u}">
        <div class="meta-row">
          <small class="muted truncate" title="${name}">${name}</small>
          <button type="button" class="btn sm" data-action="rep-new" data-index="${i}">대표</button>
        </div>`;

            on(qs('.btn-del', card), 'click', () => {
                const key = fileKey(editState.productFiles[i]);
                editState.keys.delete(key);
                editState.productFiles.splice(i, 1);
                renderThumbs();
                try { if (u.startsWith('blob:')) URL.revokeObjectURL(u); } catch {}
            });
            on(qs('[data-action="rep-new"]', card), 'click', () => {
                $rep.value = String(i);
                renderThumbs();
            });

            $c.appendChild(card);
        });

        qa('#thumbs .thumb').forEach((el, idx) => {
            if ($rep.value !== '' && +$rep.value === idx) el.classList.add('rep');
            else el.classList.remove('rep');
        });
    }

    function enableThumbReorder() {
        const wrap = qs('#thumbs');
        if (!wrap) return;
        let dragIdx = -1;

        on(wrap, 'dragstart', e => {
            const t = e.target.closest('.thumb'); if (!t) return;
            dragIdx = [...wrap.children].indexOf(t);
            t.classList.add('dragging');
            e.dataTransfer.effectAllowed = 'move';
            try { e.dataTransfer.setData('text/plain', String(dragIdx)); } catch {}
        });
        on(wrap, 'dragend', e => e.target.closest('.thumb')?.classList.remove('dragging'));
        on(wrap, 'dragover', e => {
            e.preventDefault();
            const dragging = qs('.thumb.dragging'); if (!dragging) return;
            const siblings = [...wrap.querySelectorAll('.thumb:not(.dragging)')];
            const after = siblings.find(el => e.clientY <= el.getBoundingClientRect().top + el.offsetHeight / 2);
            wrap.insertBefore(dragging, after || null);
        });
        on(wrap, 'drop', e => {
            e.preventDefault();
            const newIdx = [...wrap.children].indexOf(qs('.thumb.dragging'));
            if (dragIdx >= 0 && newIdx >= 0 && dragIdx !== newIdx) {
                const moved = editState.productFiles.splice(dragIdx, 1)[0];
                editState.productFiles.splice(newIdx, 0, moved);
                const $rep = qs('#repIndex');
                if ($rep?.value !== '') {
                    let cur = +$rep.value;
                    if (cur === dragIdx) cur = newIdx;
                    else if (dragIdx < cur && cur <= newIdx) cur -= 1;
                    else if (newIdx <= cur && cur < dragIdx) cur += 1;
                    $rep.value = String(cur);
                }
                renderThumbs();
            }
            dragIdx = -1;
        });

        on(qs('#repIndex'), 'change', renderThumbs);
    }

    function bindNewImages(root) {
        const $file = qs('#files', root);
        const $drop = qs('#productDropZone');
        const $rep  = qs('#repIndex');
        const $btn  = qs('#btnUpload', root); // 수정 페이지에서만 있을 수 있음

        const add = files => {
            let added = 0;
            Array.from(files || []).forEach(f => {
                if (!f.type.startsWith('image/')) return;
                const k = fileKey(f);
                if (editState.keys.has(k)) return;
                editState.keys.add(k);
                editState.productFiles.push(f);
                added++;
            });
            if (added) renderThumbs();
        };

        on($file, 'change', e => {
            add(e.target.files);
            $file.value = '';
        });

        ['dragover', 'dragenter'].forEach(t =>
            on($drop, t, e => { e.preventDefault(); $drop?.classList.add('border-primary'); })
        );
        ['dragleave', 'dragend'].forEach(t =>
            on($drop, t, e => { e.preventDefault(); $drop?.classList.remove('border-primary'); })
        );
        on($drop, 'drop', e => {
            e.preventDefault();
            $drop?.classList.remove('border-primary');
            add(e.dataTransfer.files);
        });

        on(document, 'paste', e => {
            const fs = [];
            for (const it of e.clipboardData?.items || []) {
                if (it.kind === 'file') {
                    const f = it.getAsFile();
                    if (f?.type?.startsWith('image/')) fs.push(f);
                }
            }
            add(fs);
        });

        // 수정 페이지에서만 "새 이미지 업로드" 버튼 존재
        on($btn, 'click', async () => {
            if (!editState.productFiles.length) return alert('업로드할 이미지를 선택하세요.');
            const fd = new FormData();
            editState.productFiles.forEach(f => fd.append('files', f));
            if ($rep?.value !== '') fd.append('repIndex', $rep.value);

            $btn.disabled = true;
            const old = $btn.textContent;
            $btn.textContent = '업로드 중...';
            try {
                const itemId = qs('[data-page="edit"]')?.dataset.itemId;
                const r = await jfetch(`${apiBase()}/${itemId}/images`, { method: 'POST', body: fd });
                if (!r.ok) throw new Error(`업로드 실패 (${r.status})`);
                // 새로고침 전 폼 값 보존
                const f = qs('#itemForm'); const key = `mealkit:item:edit:${itemId}`;
                if (f) {
                    const o = {}; new FormData(f).forEach((v, k) => (o[k] = v));
                    try { localStorage.setItem(key, JSON.stringify(o)); } catch {}
                }
                location.reload();
            } catch (e) {
                alert(e.message || '업로드 중 오류');
            } finally {
                $btn.disabled = false;
                $btn.textContent = old;
                editState.productFiles = [];
                editState.keys.clear();
                renderThumbs();
            }
        });
    }

    // ===== 신규 페이지: 상세 이미지 로컬 미리보기 전용 =====
    function renderDetailPreviewsLocal(files) {
        const wrap = qs('#detailFilesPreview');
        if (!wrap) return;
        wrap.innerHTML = '';
        const list = Array.from(files || []).filter(f => f.type.startsWith('image/'));
        list.forEach((f, i) => {
            const u = URL.createObjectURL(f);
            const card = document.createElement('div');
            card.className = 'thumb thumb-sm';
            card.innerHTML = `
        <button type="button" class="btn-icon btn-del" aria-label="삭제">×</button>
        <img alt="상세 미리보기 ${i + 1}" src="${u}">
        <div style="margin-top:4px"><small class="muted">본문 삽입은 등록 후 편집에서</small></div>
      `;
            on(qs('.btn-del', card), 'click', () => {
                const $in = qs('#itemDetailFiles');
                if (!$in?.files?.length) return;
                const dt = new DataTransfer();
                Array.from($in.files).forEach((file, idx) => { if (idx !== i) dt.items.add(file); });
                $in.files = dt.files;
                URL.revokeObjectURL(u);
                renderDetailPreviewsLocal($in.files);
            });
            wrap.appendChild(card);
        });
    }

    function bindDetailPreviewForNew() {
        const root = qs('[data-page="new"]');
        if (!root) return;
        const $in = qs('#itemDetailFiles');
        if (!$in) return;

        on($in, 'change', () => renderDetailPreviewsLocal($in.files));

        const drop = qs('#detailFilesPreview');
        ['dragover','dragenter'].forEach(t =>
            drop?.addEventListener(t, e => { e.preventDefault(); drop.classList.add('border-primary'); })
        );
        ['dragleave','dragend','drop'].forEach(t =>
            drop?.addEventListener(t, e => { e.preventDefault(); drop.classList.remove('border-primary'); })
        );
        drop?.addEventListener('drop', e => {
            const fs = Array.from(e.dataTransfer?.files || []).filter(f => f.type.startsWith('image/'));
            if (!fs.length) return;
            const dt = new DataTransfer();
            Array.from($in.files || []).forEach(f => dt.items.add(f));
            fs.forEach(f => dt.items.add(f));
            $in.files = dt.files;
            renderDetailPreviewsLocal($in.files);
        });
    }

    // ===== 수정 페이지: 상세 이미지 업로드 → 본문 자동 삽입 =====
    async function uploadDetail(files) {
        if (!files?.length) throw new Error('상세 이미지가 없습니다.');
        const editRoot = qs('[data-page="edit"]');
        if (!editRoot) throw new Error('신규 등록에서는 상세 이미지 업로드를 사용할 수 없습니다. 먼저 저장 후 편집 화면에서 이용하세요.');
        const itemId = editRoot.dataset.itemId;

        const fd = new FormData();
        files.forEach(f => f?.type?.startsWith('image/') && fd.append('files', f));
        if (![...fd.keys()].length) throw new Error('이미지 파일만 업로드 가능합니다.');

        const r = await jfetch(`${apiBase()}/${encodeURIComponent(itemId)}/detail-images`, { method: 'POST', body: fd });
        if (!r.ok) throw new Error(`상세 이미지 업로드 실패 (${r.status})`);
        const ct = r.headers.get('content-type') || '';
        if (!ct.includes('application/json')) throw new Error('업로드 응답이 JSON이 아닙니다.');

        const j = await r.json();
        if (Array.isArray(j?.urls)) return j.urls;
        if (Array.isArray(j)) {
            if (j.every(s => typeof s === 'string')) return j;
            const urls = j.map(o => o?.imgUrl || o?.url || o?.path).filter(Boolean);
            if (urls.length) return urls;
        }
        throw new Error('응답 형식 해석 실패');
    }

    function bindDetailForEdit() {
        const root = qs('[data-page="edit"]');
        if (!root) return;
        const $in = qs('#itemDetailFiles', root);

        on($in, 'change', async () => {
            const files = Array.from($in.files || []).filter(f => f.type.startsWith('image/'));
            if (!files.length) return;
            $in.disabled = true;
            try {
                const urls = await uploadDetail(files);
                const existing = collectDetailSrcSet();
                const uniq = urls.filter(u => !existing.has(u));
                const $detail = qs('#itemDetail');
                if ($detail && uniq.length) {
                    const block = '\n' + uniq.map(u => `<p><img src="${u}" alt=""></p>`).join('\n');
                    insertAtCursor($detail, block);
                }
            } catch (e) {
                alert(e.message || '상세 이미지 업로드 오류');
            } finally {
                const $prev = qs('#detailFilesPreview');
                if ($prev) $prev.innerHTML = '';
                $in.value = '';
                $in.disabled = false;
            }
        });

        // 붙여넣기/드롭 가드
        const detail = qs('#itemDetail');
        if (!detail) return;

        on(detail, 'paste', async (e) => {
            const items = [...(e.clipboardData?.items || [])].filter(it => it.kind === 'file');
            if (!items.length) return;
            e.preventDefault();
            const files = items.map(it => it.getAsFile()).filter(Boolean).filter(f => f.type.startsWith('image/'));
            if (!files.length) return;
            try {
                const urls = await uploadDetail(files);
                const existing = collectDetailSrcSet();
                const uniq = urls.filter(u => !existing.has(u));
                if (uniq.length) {
                    const block = '\n' + uniq.map(u => `<p><img src="${u}" alt=""></p>`).join('\n');
                    insertAtCursor(detail, block);
                }
            } catch (err) {
                alert(err?.message || '붙여넣은 이미지를 업로드하는 중 오류가 발생했습니다.');
            }
        });

        on(detail, 'dragover', (e) => {
            if (e.dataTransfer?.types?.includes('Files')) e.preventDefault();
        });
        on(detail, 'drop', async (e) => {
            if (!(e.dataTransfer?.files?.length)) return;
            e.preventDefault();
            const files = [...e.dataTransfer.files].filter(f => f.type.startsWith('image/'));
            if (!files.length) return;
            try {
                const urls = await uploadDetail(files);
                const existing = collectDetailSrcSet();
                const uniq = urls.filter(u => !existing.has(u));
                if (uniq.length) {
                    const block = '\n' + uniq.map(u => `<p><img src="${u}" alt=""></p>`).join('\n');
                    insertAtCursor(detail, block);
                }
            } catch (err) {
                alert(err?.message || '드롭한 이미지를 업로드하는 중 오류가 발생했습니다.');
            }
        });
    }

    // ===== 기존 이미지 액션(수정 페이지) =====
    function bindExistingActions(itemId) {
        document.addEventListener('click', async e => {
            const t = e.target.closest('[data-action]');
            if (!t) return;
            const act = t.dataset.action;
            const imgId = t.dataset.imageId;
            const imgUrl = t.dataset.imgUrl;
            const detail = qs('#itemDetail');

            // 기존 "상품" 이미지 대표 지정
            if (act === 'rep-existing' && imgId) {
                t.disabled = true;
                try {
                    const r = await jfetch(`${apiBase()}/${itemId}/images/${imgId}/rep`, { method: 'POST' });
                    if (!r.ok) throw new Error('대표 지정 실패');
                    // 폼 값 보존 후 새로고침
                    const f = qs('#itemForm'); const key = `mealkit:item:edit:${itemId}`;
                    if (f) {
                        const o = {}; new FormData(f).forEach((v, k) => (o[k] = v));
                        try { localStorage.setItem(key, JSON.stringify(o)); } catch {}
                    }
                    location.reload();
                } catch (err) {
                    alert(err.message || '대표 지정 오류');
                } finally {
                    t.disabled = false;
                }
                return;
            }

            // 기존 "상품" 이미지 삭제
            if (act === 'del-existing' && imgId) {
                if (!confirm('이 이미지를 삭제하시겠어요?')) return;
                t.disabled = true;
                try {
                    const r = await jfetch(`${apiBase()}/${itemId}/images/${imgId}`, { method: 'DELETE' });
                    if (!r.ok) throw new Error('이미지 삭제 실패');
                    const f = qs('#itemForm'); const key = `mealkit:item:edit:${itemId}`;
                    if (f) {
                        const o = {}; new FormData(f).forEach((v, k) => (o[k] = v));
                        try { localStorage.setItem(key, JSON.stringify(o)); } catch {}
                    }
                    location.reload();
                } catch (err) {
                    alert(err.message || '삭제 오류');
                } finally {
                    t.disabled = false;
                }
                return;
            }

            // 기존 "상세" 이미지 → 본문에 삽입
            if (act === 'insert-detail-existing' && imgUrl && detail) {
                const block = `\n<p><img src="${imgUrl}" alt=""></p>\n`;
                insertAtCursor(detail, block);
                detail.focus();
                return;
            }

            // 기존 "상세" 이미지 삭제 (서버 + 본문 태그 제거)
            if (act === 'del-detail-existing' && imgId) {
                if (!confirm('이 상세 이미지를 서버에서 삭제하시겠어요? (본문 태그도 제거됩니다)')) return;
                t.disabled = true;
                try {
                    const r = await jfetch(`${apiBase()}/${itemId}/images/${imgId}`, { method: 'DELETE' });
                    if (!r.ok) throw new Error('상세 이미지 삭제 실패');

                    if (imgUrl && detail) {
                        const holder = document.createElement('div');
                        holder.innerHTML = detail.value || '';
                        const imgs = holder.querySelectorAll('img[src]');
                        let removed = 0;
                        imgs.forEach(img => {
                            const src = (img.getAttribute('src') || '').trim();
                            if (src === imgUrl) {
                                const p = img.closest('p');
                                (p || img).remove();
                                removed++;
                            }
                        });
                        if (removed > 0) {
                            detail.value = holder.innerHTML;
                            detail.dispatchEvent(new Event('change', { bubbles: true }));
                        }
                    }

                    const f = qs('#itemForm'); const key = `mealkit:item:edit:${itemId}`;
                    if (f) {
                        const o = {}; new FormData(f).forEach((v, k) => (o[k] = v));
                        try { localStorage.setItem(key, JSON.stringify(o)); } catch {}
                    }
                    location.reload();
                } catch (err) {
                    alert(err.message || '삭제 오류');
                } finally {
                    t.disabled = false;
                }
                return;
            }
        });
    }

    // ===== 아이템 수정(PUT) / 삭제(DELETE) =====
    function bindUpdate() {
        const f = qs('#itemForm');
        const btn = qs('#btnUpdate');
        const root = qs('[data-page="edit"]');
        if (!f || !btn || !root) return;

        const id = Number(root.dataset.itemId);
        let busy = false;

        const toObj = () => {
            const o = Object.fromEntries(new FormData(f).entries());
            o.originalPrice = String(toNum(o.originalPrice));
            o.discountRate  = String(clamp(toNum(o.discountRate), 0, 95));
            o.stockNumber   = String(toNum(o.stockNumber));
            return o;
        };

        const go = async e => {
            e.preventDefault();
            if (busy) return;
            busy = true;

            if (!f.reportValidity()) { busy = false; return; }
            if (!validateDetailHtml()) { busy = false; return; }

            btn.disabled = true;
            const old = btn.textContent;
            btn.textContent = '저장 중...';
            try {
                const data = toObj();
                const fd = new FormData();
                Object.entries(data).forEach(([k, v]) => fd.append(k, v ?? ''));
                const r = await jfetch(`${apiBase()}/${id}`, { method: 'PUT', body: fd });
                if (!r.ok) throw new Error(`수정 실패 (${r.status})`);

                try { localStorage.removeItem(`mealkit:item:edit:${id}`); } catch {}
                const redirect =
                    qs('meta[name="after-update-url"]')?.content ||
                    root.dataset.afterUpdateUrl || `/admin/items/${id}`;
                location.assign(redirect);
            } catch (err) {
                alert(err.message || '수정 오류');
                busy = false;
            } finally {
                btn.disabled = false;
                btn.textContent = old;
            }
        };

        on(f, 'submit', go);
    }

    function bindDelete() {
        const btn = qs('#btnDelete');
        const root = qs('[data-page="edit"]');
        if (!btn || !root) return;

        const id = Number(root.dataset.itemId);
        const after = qs('meta[name="after-delete-url"]')?.content || '/admin/items';

        const sellStatus = (root.dataset.sellStatus || '').toUpperCase();
        const isStop = sellStatus === 'STOP';
        if (isStop) {
            btn.textContent = '영구 삭제';
            btn.title = 'STOP 상태이므로 영구 삭제를 수행합니다.';
            btn.classList.add('danger');
        }

        on(btn, 'click', async () => {
            const msg = isStop
                ? '해당 아이템을 영구 삭제하시겠어요? (이미지/리뷰/좋아요 포함)'
                : '해당 아이템과 관련된 모든 정보(이미지 포함)를 삭제하시겠어요?';
            if (!confirm(msg)) return;

            btn.disabled = true;
            const old = btn.textContent;
            btn.textContent = isStop ? '영구 삭제 중...' : '삭제 중...';

            try {
                const url = isStop
                    ? `${apiBase()}/${id}?forceStopOnly=true`
                    : `${apiBase()}/${id}`;

                const r = await jfetch(url, { method: 'DELETE' });
                if (!r.ok) {
                    let detail = '';
                    try { const j = await r.clone().json(); detail = j?.message || ''; } catch {}
                    throw new Error(detail || `삭제 실패 (${r.status})`);
                }

                const msgHdr = r.headers.get('X-Delete-Message');
                const result = r.headers.get('X-Delete-Result'); // soft|hard|hard-forced
                if (msgHdr) alert(msgHdr);
                else if (result === 'hard-forced' || result === 'hard') alert('영구 삭제되었습니다.');
                else if (result === 'soft') alert('판매중지(STOP)로 전환되었습니다. 보존기간 경과 후 삭제 가능합니다.');

                location.assign(after);
            } catch (e) {
                alert(e.message || '삭제 오류');
            } finally {
                btn.disabled = false;
                btn.textContent = old;
            }
        });
    }

    // ===== 선택 파일 비우기 =====
    function clearAllProductFiles() {
        revokeAllThumbURLs();
        editState.productFiles = [];
        editState.keys.clear();
        const $rep = qs('#repIndex'); if ($rep) $rep.value = '';
        renderThumbs();
        const $files = qs('#files'); if ($files) $files.value = '';
    }
    function clearNonRepProductFiles() {
        const $rep = qs('#repIndex');
        if (!$rep || $rep.value === '') { clearAllProductFiles(); return; }
        const keepIdx = +$rep.value;
        const keep = editState.productFiles[keepIdx];
        revokeAllThumbURLs();
        editState.productFiles = keep ? [keep] : [];
        editState.keys.clear();
        if (keep) editState.keys.add(`${keep.name}|${keep.size}|${keep.lastModified}`);
        if ($rep) $rep.value = editState.productFiles.length ? '0' : '';
        renderThumbs();
    }

    // ===== 신규 생성(POST) =====
    function bindCreate() {
        const f = qs('#itemForm');
        const btn = qs('#btnCreate');
        if (!f || !btn) return;

        let busy = false;
        const go = async e => {
            e.preventDefault();
            if (busy) return; busy = true;

            if (!f.reportValidity()) { busy = false; return; }
            if (!validateDetailHtml()) { busy = false; return; }

            const fd = new FormData(f);
            fd.set('originalPrice', String(toNum(fd.get('originalPrice'))));
            fd.set('discountRate', String(clamp(toNum(fd.get('discountRate')), 0, 95)));

            const rep = qs('#repIndex')?.value ?? '';
            if (rep !== '') fd.set('repIndex', rep);

            // 상품 이미지 파일들
            editState.productFiles.forEach(f => fd.append('files', f));

            btn.disabled = true;
            const old = btn.textContent;
            btn.textContent = '등록 중...';
            try {
                const r = await jfetch(apiBase(), { method: 'POST', body: fd });
                if (!r.ok) throw new Error(`등록 실패 (${r.status})`);
                let red = r.headers.get('Location')
                    || qs('meta[name="after-create-url"]')?.content
                    || qs('[data-page="new"]')?.dataset.afterCreateUrl
                    || '/admin/items';
                try { const j = await r.clone().json(); red = j?.location || red; } catch {}
                location.assign(red);
            } catch (e) {
                alert(e.message || '등록 오류');
                busy = false;
            } finally {
                btn.disabled = false;
                btn.textContent = old;
            }
        };

        on(f, 'submit', go);
    }

    // ===== 스타일(대표 배지 하이라이트) 주입 =====
    (() => {
        const css = `
      .thumb.rep::after{
        content:"대표";
        position:absolute; top:6px; left:6px;
        background:#2563eb; color:#fff; font-size:11px;
        padding:2px 6px; border-radius:999px;
      }
      .thumb.rep{ box-shadow:0 0 0 2px rgba(37,99,235,.45); position:relative; }
      .thumb.dragging{ opacity:.7 }
    `.trim();
        const style = document.createElement('style');
        style.type = 'text/css';
        style.appendChild(document.createTextNode(css));
        document.head.appendChild(style);
    })();

    // ===== 페이지 초기화 =====
    // (수정폼 값 보존 로컬스토리지 드래프트)
    const draftKey = () => `mealkit:item:edit:${qs('[data-page="edit"]')?.dataset.itemId || '0'}`;
    const loadDraft = () => {
        try {
            const raw = localStorage.getItem(draftKey());
            if (!raw) return;
            const o = JSON.parse(raw); const f = qs('#itemForm'); if (!f) return;
            for (const [k, v] of Object.entries(o)) {
                const el = f.querySelector(`[name="${CSS.escape(k)}"]`);
                if (el) {
                    el.value = v;
                    el.dispatchEvent(new Event('input', { bubbles: true }));
                    el.dispatchEvent(new Event('change', { bubbles: true }));
                }
            }
        } finally { try { localStorage.removeItem(draftKey()); } catch {} }
    };

    function initNew() {
        const root = qs('[data-page="new"]'); if (!root) return;
        bindPrice(root);
        // 상세 이미지 "로컬 미리보기"만 지원 (업로드/본문삽입 없음)
        bindDetailPreviewForNew();
        bindNewImages(root);
        enableThumbReorder();
        bindCreate();
        on(qs('#btnClearAll', root), 'click', () => clearAllProductFiles());
        on(qs('#btnClearNonRep', root), 'click', () => clearNonRepProductFiles());
    }

    function initEdit() {
        const root = qs('[data-page="edit"]'); if (!root) return;
        loadDraft();
        bindPrice(root);
        // 파일 선택/붙여넣기/드롭 → 즉시 업로드 & 본문 삽입
        bindDetailForEdit();
        bindNewImages(root);
        enableThumbReorder();
        bindExistingActions(Number(root.dataset.itemId));
        bindUpdate();
        bindDelete();
        on(qs('#btnClearAll', root), 'click', () => clearAllProductFiles());
        on(qs('#btnClearNonRep', root), 'click', () => clearNonRepProductFiles());
    }

    document.addEventListener('DOMContentLoaded', () => {
        (isNewPage() ? initNew : initEdit)();
    });
})();
