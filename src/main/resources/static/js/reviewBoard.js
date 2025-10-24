document.addEventListener("DOMContentLoaded", () => {
    /* ---------------------------------
     * 페이지 구분 가드
     * --------------------------------- */
    const page = document.body.dataset.page || "";
    const isRegister = page === "review-register";

    /* ---------------------------------
     * 공통
     * --------------------------------- */
    const csrfHeader = document.querySelector("meta[name='_csrf_header']")?.content;
    const csrfToken  = document.querySelector("meta[name='_csrf']")?.content;
    const isLoggedIn = document.body.getAttribute("data-is-logged-in") === "true";

    const commentsList =
        document.getElementById("review-comments-container") ||
        document.querySelector("ul.review-comments");

    // 빈상태 엘리먼트 보장 + 토글 (등록 페이지/댓글 리스트 없으면 생성/동기화 안 함)
    function ensureEmptyStateEl() {
        if (!commentsList) return null; // 등록/댓글 없는 페이지는 스킵
        let el = document.getElementById("no-comments") || document.querySelector(".no-comments");
        if (!el) {
            el = document.createElement("div");
            el.id = "no-comments";
            el.className = "text-muted no-comments";
            el.textContent = "댓글이 없습니다.";
            const header = document.querySelector("h5.mt-4");
            if (header?.nextElementSibling === commentsList) {
                header.insertAdjacentElement("afterend", el);
            } else {
                commentsList.insertAdjacentElement("beforebegin", el);
            }
        }
        return el;
    }

    function syncEmptyState() {
        if (!commentsList) return; // 등록/댓글 없는 페이지는 스킵
        const count = commentsList.querySelectorAll("li.review-comment").length;
        const el = ensureEmptyStateEl();
        if (el) el.style.display = count > 0 ? "none" : "";
    }

    // 최초 동기화 (등록 페이지에선 실행하지 않음)
    if (!isRegister) {
        syncEmptyState();
    }

    // wasSecretBoard: meta 혹은 hidden div(data-was-secret)에서 읽기
    const wasSecretMeta   = document.querySelector('meta[name="was-secret-board"]')?.content;
    const theWasSecretDiv = document.getElementById("wasSecretState")?.dataset?.wasSecret;
    const wasSecretBoard  = ((wasSecretMeta ?? theWasSecretDiv) ?? "false").toString() === "true";

    const toNum = (s) => Number(String(s).replace(/[^\d-]/g, "")) || 0;

    /* ---------------------------------
     * A) 등록/수정 폼: 비밀글 토글 처리
     * --------------------------------- */
    (function wireSecretControls() {
        // REGISTER
        const regCb = document.getElementById("registerSecretCheck");
        const regPw = document.getElementById("registerCurrentPassword");

        if (regCb && regPw) {
            const sync = () => {
                const on = regCb.checked;
                regPw.disabled = !on;
                regPw.required = on;
                if (!on) regPw.value = "";
            };
            regCb.addEventListener("change", sync);
            sync();

            const regForm = document.getElementById("registerReviewForm");
            if (regForm) {
                regForm.addEventListener("submit", (e) => {
                    if (regCb.checked && regPw.value.trim().length < 4) {
                        e.preventDefault();
                        alert("비밀글 비밀번호는 최소 4자 이상이어야 합니다.");
                        regPw.focus();
                    }
                });
            }
        }

        // MODIFY
        const modCb  = document.getElementById("modifySecretCheck");
        const curGrp = document.getElementById("modifyCurrentPasswordGroup");
        const newGrp = document.getElementById("modifyNewPasswordGroup");
        const curPw  = document.getElementById("modifyCurrentPassword");
        const newPw  = document.getElementById("modifyNewPassword");
        const modHidden = document.getElementById("modifySecretBoardHidden");

        if (modCb && curGrp && newGrp && curPw && newPw) {
            const sync = () => {
                const on = modCb.checked;
                if (modHidden) modHidden.value = on ? "true" : "false";

                if (!on) {
                    curGrp.style.display = "none";
                    newGrp.style.display = "none";
                    curPw.disabled = true; curPw.required = false; curPw.value = "";
                    newPw.disabled = true; newPw.required = false; newPw.value = "";
                    return;
                }

                if (wasSecretBoard) {
                    curGrp.style.display = "";
                    newGrp.style.display = "";
                    curPw.disabled = false; curPw.required = true;
                    newPw.disabled = false; newPw.required = false;
                } else {
                    curGrp.style.display = "none";
                    newGrp.style.display = "";
                    curPw.disabled = true; curPw.required = false; curPw.value = "";
                    newPw.disabled = false; newPw.required = true;
                }
            };

            modCb.addEventListener("change", sync);
            sync();
        }
    })();

    /* ---------------------------------
     * (NEW) 수정 폼 제출 가드
     * --------------------------------- */
    const modForm = document.querySelector('form[action$="/board/review/modify"]');
    if (modForm) {
        modForm.addEventListener("submit", (e) => {
            const modCb  = document.getElementById("modifySecretCheck");
            const curPw  = document.getElementById("modifyCurrentPassword");
            const newPw  = document.getElementById("modifyNewPassword");

            const secretOn = !!modCb?.checked;
            if (!secretOn) return;

            if (wasSecretBoard) {
                if (!curPw.value.trim()) {
                    e.preventDefault();
                    alert("현재 비밀번호를 입력하세요.");
                    curPw.focus();
                    return;
                }
                if (newPw.value && newPw.value.trim().length < 4) {
                    e.preventDefault();
                    alert("새 비밀번호는 4자 이상이어야 합니다.");
                    newPw.focus();
                    return;
                }
            } else {
                if (!newPw.value || newPw.value.trim().length < 4) {
                    e.preventDefault();
                    alert("비밀글 전환 시 새 비밀번호는 4자 이상이어야 합니다.");
                    newPw.focus();
                    return;
                }
            }
        });
    }

    /* ---------------------------------
     * (NEW) 서버 검증 실패로 리렌더된 경우 alert(선택)
     * --------------------------------- */
    const errBox = document.getElementById("fieldErrors");
    if (errBox) {
        const cpErr = errBox.dataset.currentPasswordErr;
        const npErr = errBox.dataset.newPasswordErr;
        const msg = cpErr || npErr;
        if (msg) alert(msg);
    }

    /* ---------------------------------
     * 이하 댓글/반응/삭제 가드는 등록 페이지에선 비활성
     * --------------------------------- */
    if (!isRegister) {
        // 0) 상세페이지: 초기 반응 버튼 상태
        document.querySelectorAll(".review-action-buttons").forEach(container => {
            const reaction = container.dataset.userReaction;
            container.classList.remove("left", "right");
            if (reaction === "helpful") {
                container.classList.add("left");
                container.querySelector(".btn-helpful")?.classList.add("active");
            } else if (reaction === "notHelpful") {
                container.classList.add("right");
                container.querySelector(".btn-not-helpful")?.classList.add("active");
            }
        });

        // 1) 도움/비도움 토글
        let isProcessingReaction = false;

        async function toggleReaction(btn, type, reviewBoardId) {
            if (!isLoggedIn) {
                if (confirm("로그인이 필요합니다. 로그인 페이지로 이동하시겠습니까?")) {
                    window.location.href = "/login";
                }
                return;
            }
            if (isProcessingReaction) return;
            isProcessingReaction = true;

            const container     = btn.closest(".review-action-buttons");
            const btnHelpful    = container.querySelector(".btn-helpful");
            const btnNotHelpful = container.querySelector(".btn-not-helpful");
            const helpfulSpan   = btnHelpful?.querySelector(".count");
            const notHelpfulSpan= btnNotHelpful?.querySelector(".count");

            if (!helpfulSpan || !notHelpfulSpan) {
                isProcessingReaction = false;
                return;
            }

            let helpfulCount    = toNum(helpfulSpan.textContent);
            let notHelpfulCount = toNum(notHelpfulSpan.textContent);

            const prevState = {
                helpfulCount,
                notHelpfulCount,
                userReaction: container.dataset.userReaction || ""
            };

            // 낙관적 UI
            if (type === "helpful") {
                if (!btnHelpful.classList.contains("active")) {
                    helpfulCount++;
                    if (btnNotHelpful.classList.contains("active")) {
                        notHelpfulCount--;
                        btnNotHelpful.classList.remove("active");
                    }
                    btnHelpful.classList.add("active");
                } else {
                    helpfulCount--;
                    btnHelpful.classList.remove("active");
                }
                container.classList.remove("right");
                container.classList.add("left");
            } else {
                if (!btnNotHelpful.classList.contains("active")) {
                    notHelpfulCount++;
                    if (btnHelpful.classList.contains("active")) {
                        helpfulCount--;
                        btnHelpful.classList.remove("active");
                    }
                    btnNotHelpful.classList.add("active");
                } else {
                    notHelpfulCount--;
                    btnNotHelpful.classList.remove("active");
                }
                container.classList.remove("left");
                container.classList.add("right");
            }

            // 즉시 카운트 반영
            helpfulSpan.textContent    = helpfulCount;
            notHelpfulSpan.textContent = notHelpfulCount;

            // 실제 활성 상태 기록
            let postReaction = type;
            if (type === "helpful") {
                postReaction = btnHelpful.classList.contains("active") ? "helpful" : "";
            } else {
                postReaction = btnNotHelpful.classList.contains("active") ? "notHelpful" : "";
            }
            container.dataset.userReaction = postReaction;

            try {
                const params = new URLSearchParams();
                params.append("reviewBoardId", reviewBoardId);
                params.append("type", type);
                if (csrfToken) params.append("_csrf", csrfToken);

                const res = await fetch("/board/reaction/review/toggle", {
                    method: "POST",
                    headers: {
                        "Content-Type": "application/x-www-form-urlencoded;charset=UTF-8",
                        "Accept": "application/json",
                        ...(csrfHeader && csrfToken ? { [csrfHeader]: csrfToken } : {})
                    },
                    credentials: "same-origin",
                    cache: "no-store",
                    body: params.toString()
                });

                if (!res.ok) throw new Error(`HTTP ${res.status}`);

                let data = {};
                const ct = res.headers.get("content-type") || "";
                if (ct.includes("application/json")) {
                    data = await res.json().catch(() => ({}));
                }

                // 서버값 동기화
                btnHelpful.classList.remove("active");
                btnNotHelpful.classList.remove("active");
                container.classList.remove("left", "right");

                const hasUserReaction = Object.prototype.hasOwnProperty.call(data, "userReaction");
                const finalReaction = hasUserReaction ? (data.userReaction ?? "") : (container.dataset.userReaction || "");
                container.dataset.userReaction = finalReaction;

                if (finalReaction === "helpful") {
                    btnHelpful.classList.add("active");
                    container.classList.add("left");
                } else if (finalReaction === "notHelpful") {
                    btnNotHelpful.classList.add("active");
                    container.classList.add("right");
                }

                if (Object.prototype.hasOwnProperty.call(data, "helpfulCount")) {
                    helpfulSpan.textContent = Number(data.helpfulCount);
                }
                if (Object.prototype.hasOwnProperty.call(data, "notHelpfulCount")) {
                    notHelpfulSpan.textContent = Number(data.notHelpfulCount);
                }

            } catch (err) {
                console.error("[reaction toggle] failed:", err);
                // 롤백
                helpfulSpan.textContent    = prevState.helpfulCount;
                notHelpfulSpan.textContent = prevState.notHelpfulCount;
                btnHelpful.classList.remove("active");
                btnNotHelpful.classList.remove("active");
                container.classList.remove("left", "right");

                if (prevState.userReaction === "helpful") {
                    btnHelpful.classList.add("active");
                    container.classList.add("left");
                } else if (prevState.userReaction === "notHelpful") {
                    btnNotHelpful.classList.add("active");
                    container.classList.add("right");
                }
                container.dataset.userReaction = prevState.userReaction;
            } finally {
                isProcessingReaction = false;
            }
        }

        document.querySelectorAll(".btn-helpful").forEach(btn => {
            btn.addEventListener("click", (e) => {
                e.preventDefault();
                e.stopPropagation();
                toggleReaction(btn, "helpful", btn.dataset.bno);
            });
        });
        document.querySelectorAll(".btn-not-helpful").forEach(btn => {
            btn.addEventListener("click", (e) => {
                e.preventDefault();
                e.stopPropagation();
                toggleReaction(btn, "notHelpful", btn.dataset.bno);
            });
        });

        // 2) 댓글 등록 (즉시 반영)
        const reviewForm = document.getElementById("review-reply-form");
        if (reviewForm && commentsList) {
            reviewForm.addEventListener("submit", async (e) => {
                e.preventDefault();

                const textarea = reviewForm.querySelector("textarea[name='replyText']");
                const reviewBoardId = reviewForm.querySelector("input[name='reviewBoardId']")?.value;
                const content = textarea.value.trim();
                if (!content) {
                    alert("댓글 내용을 입력해주세요.");
                    return;
                }

                const params = new URLSearchParams();
                params.append("bno", reviewBoardId);
                params.append("replyText", content);
                if (csrfToken) params.append("_csrf", csrfToken);

                try {
                    const res = await fetch("/board/review/reply/register", {
                        method: "POST",
                        headers: {
                            "Content-Type": "application/x-www-form-urlencoded;charset=UTF-8",
                            ...(csrfHeader && csrfToken ? { [csrfHeader]: csrfToken } : {})
                        },
                        body: params.toString(),
                        credentials: "same-origin",
                        cache: "no-store"
                    });

                    const data = await res.json();
                    if (!data || !data.rno) {
                        alert("댓글 등록 실패");
                        return;
                    }

                    const displayTime = new Date(data.regDate ?? Date.now()).toLocaleString("ko-KR", {
                        year:"numeric", month:"2-digit", day:"2-digit",
                        hour:"numeric", minute:"2-digit", hour12:true
                    });

                    const li = document.createElement("li");
                    li.className = "review-comment";
                    li.dataset.rno = data.rno;

                    const adminBadge = data.admin ? '<span class="badge bg-danger ms-1">관리자</span>' : '';
                    li.innerHTML = `
            <p>
              <strong>${data.replyer ?? "관리자"} ${adminBadge}</strong>
              <span class="reply-text ms-1">${data.replyText}</span>
              <em class="ms-2">${displayTime}</em>
              <span class="ms-2">
                <button type="button" class="btn btn-sm btn-outline-secondary btn-edit">수정</button>
                <button type="button" class="btn btn-sm btn-outline-danger btn-delete">삭제</button>
              </span>
            </p>
          `;

                    commentsList.prepend(li);
                    textarea.value = "";
                    syncEmptyState();

                } catch (err) {
                    console.error(err);
                    alert("댓글 등록 중 오류가 발생했습니다.");
                }
            });
        }

        // 3) 댓글 수정/삭제 (이벤트 위임)
        if (commentsList) {
            commentsList.addEventListener("click", async (e) => {
                const li = e.target.closest("li.review-comment");
                if (!li) return;

                const editBtn = e.target.closest(".btn-edit");
                const delBtn  = e.target.closest(".btn-delete");
                const rno     = li.dataset.rno;

                // 삭제
                if (delBtn) {
                    if (!confirm("댓글을 삭제하시겠습니까?")) return;
                    try {
                        const res = await fetch(`/board/review/reply/${rno}`, {
                            method: "DELETE",
                            headers: { ...(csrfHeader && csrfToken ? { [csrfHeader]: csrfToken } : {}) },
                            credentials: "same-origin",
                            cache: "no-store"
                        });
                        await res.json().catch(() => ({}));
                        li.remove();
                        syncEmptyState();
                    } catch (err) {
                        console.error(err);
                        alert("댓글 삭제 실패");
                    }
                    return;
                }

                // 수정
                if (editBtn) {
                    const editing = editBtn.dataset.mode === "edit";

                    if (!editing) {
                        const span = li.querySelector(".reply-text");
                        const input = document.createElement("input");
                        input.type = "text";
                        input.className = "form-control d-inline-block";
                        input.value = (span?.textContent ?? "").trim();
                        span.replaceWith(input);
                        editBtn.textContent = "저장";
                        editBtn.dataset.mode = "edit";
                    } else {
                        const input = li.querySelector("input[type='text']");
                        const newText = input?.value.trim() ?? "";
                        if (!newText) { alert("댓글 내용을 입력해주세요."); return; }

                        try {
                            const res = await fetch(`/board/review/reply/${rno}`, {
                                method: "PUT",
                                headers: {
                                    "Content-Type": "application/json",
                                    ...(csrfHeader && csrfToken ? { [csrfHeader]: csrfToken } : {})
                                },
                                body: JSON.stringify({ rno, replyText: newText }),
                                credentials: "same-origin",
                                cache: "no-store"
                            });
                            await res.json().catch(() => ({}));

                            const spanNew = document.createElement("span");
                            spanNew.className = "reply-text ms-1";
                            spanNew.textContent = newText;
                            input.replaceWith(spanNew);
                            editBtn.textContent = "수정";
                            editBtn.dataset.mode = "";
                        } catch (err) {
                            console.error(err);
                            alert("댓글 수정 중 오류가 발생했습니다.");
                        }
                    }
                }
            });
        }

        // 4) 댓글 있는 글 삭제 가드
        document.body.addEventListener("click", (e) => {
            const btn = e.target.closest(".btn-delete-review");
            if (!btn) return;

            const hasComments = !!commentsList && commentsList.querySelectorAll("li.review-comment").length > 0;
            if (hasComments) {
                e.preventDefault();
                e.stopImmediatePropagation();
                alert("댓글이 달린 리뷰는 삭제할 수 없습니다.");
                return;
            }

            const ok = confirm("정말 이 리뷰를 삭제하시겠습니까?");
            if (!ok) {
                e.preventDefault();
                e.stopImmediatePropagation();
            }
        });
    } // !isRegister

    /* ---------------------------------
     * 5) 비밀글 모달 취소 (공통)
     * --------------------------------- */
    const passwordModal  = document.querySelector(".secret-overlay");
    const passwordCancel = document.getElementById("passwordCancelBtn");
    if (passwordCancel) {
        passwordCancel.addEventListener("click", (e) => {
            e.preventDefault();
            if (passwordModal) passwordModal.style.display = "none";
            window.location.href = "/board/review/list";
        });
    }
});
