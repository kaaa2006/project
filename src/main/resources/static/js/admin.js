document.addEventListener("DOMContentLoaded", () => {
    // ============================
    // 0. CSRF 토큰 가져오기
    // ============================
    const csrfToken = document.querySelector('meta[name="_csrf"]')?.content;
    const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.content;

    // ============================
    // 1. DOM 요소 가져오기
    // ============================
    const tbody = document.querySelector("#inquiryList tbody"); // 문의 목록
    const answerContainer = document.getElementById("answerContainer"); // 답변 전체 영역
    const answerDisplay = document.getElementById("answerDisplay");     // 답변 표시 영역
    const answerText = document.getElementById("answerText");           // 답변 내용
    const noAnswerText = document.getElementById("noAnswerText");       // 답변 미등록 안내
    const inlineAnswerForm = document.getElementById("inlineAnswerForm"); // 입력 폼 영역
    const answerContent = document.getElementById("answerContent");       // textarea
    const createAnswerBtn = document.getElementById("createAnswerBtn");
    const editAnswerBtn = document.getElementById("editAnswerBtn");
    const saveAnswerBtn = document.getElementById("saveAnswerBtn");
    const cancelAnswerBtn = document.getElementById("cancelAnswerBtn");
    const deleteAnswerBtn = document.getElementById("deleteAnswerBtn");

    // ============================
    // 2. URL에서 inquiryId 추출
    // ============================
    const pathParts = window.location.pathname.split("/").filter(Boolean);
    const lastPart = pathParts[pathParts.length - 1];
    const inquiryId = !isNaN(lastPart) ? lastPart : null;
    const isListPage = window.location.pathname.endsWith("/list");
    const isReadPage = !isListPage && inquiryId !== null;

    // ============================
    // 3. 문의 목록 로딩
    // ============================
    function loadInquiries() {
        if (!tbody) return; // tbody 없으면 리스트 페이지 아님

        fetch("/inquiry/admin", {
            method: "GET",
            headers: { "Content-Type": "application/json", [csrfHeader]: csrfToken },
            credentials: "same-origin"
        })
            .then(res => {
                if (!res.ok) throw new Error(`서버 오류: ${res.status}`);
                return res.json();
            })
            .then(data => {
                tbody.innerHTML = "";

                if (!data || data.length === 0) {
                    tbody.innerHTML = `<tr><td colspan="5">등록된 문의가 없습니다.</td></tr>`;
                    return;
                }

                data.forEach(inq => {
                    const tr = document.createElement("tr");
                    tr.innerHTML = `
        <td>${inq.id}</td>
        <td><a href="/inquiry/admin/${inq.id}" class="inquiry-link">${inq.title}</a></td>
        <td>${inq.userName}</td>
        <td>${inq.userEmail}</td>
        <td>${inq.regDate ? new Date(inq.regDate).toLocaleString('ko-KR', {
                        year: 'numeric',
                        month: '2-digit',
                        day: '2-digit',
                        hour: 'numeric',   // 앞자리 0 제거
                        minute: '2-digit',
                        hour12: true       // 12시간제
                    }) : ''}</td>
        <td>${inq.status}</td>
    `;
                    tbody.appendChild(tr);
                });
            })
            .catch(err => {
                console.error(err);
                tbody.innerHTML = `<tr><td colspan="5">문의 로딩 실패: ${err.message}</td></tr>`;
            });
    }

    // ============================
    // 4. 문의 상세 로딩
    // ============================
    function loadInquiryDetail() {
        if (!inquiryId) return;

        fetch(`/inquiry/admin/${inquiryId}/json`, {
            method: "GET",
            headers: { "Content-Type": "application/json", [csrfHeader]: csrfToken },
            credentials: "same-origin"
        })
            .then(res => res.json())
            .then(inq => {
                // null, undefined, 빈 문자열 모두 false 처리
                const hasAnswer = !!(inq.answerContent && inq.answerContent.trim() !== "");

                // answerContainer 항상 보여주고, 폼/텍스트만 토글
                if (answerContainer) answerContainer.style.display = "block";

                if (hasAnswer) {
                    // 답변 있음 → 표시
                    answerText.textContent = inq.answerContent;   // ← 여기 추가
                    answerContainer.style.display = "none"; // 회색 박스 숨김
                    answerDisplay.style.display = "block";  // 답변 텍스트 표시
                    createAnswerBtn.style.display = "none";
                    editAnswerBtn.style.display = "inline-block";
                    deleteAnswerBtn.style.display = "inline-block";
                } else {
                    // 답변 없음 → 작성 버튼만 보이게
                    answerContainer.style.display = "block"; // 회색 박스 표시
                    noAnswerText.style.display = "block";     // 안내문 표시
                    inlineAnswerForm.style.display = "none";  // 폼 숨김
                    answerDisplay.style.display = "none";     // 답변 텍스트 숨김

                    createAnswerBtn.style.display = "inline-block"; // 작성 버튼 보이기
                    editAnswerBtn.style.display = "none";
                    deleteAnswerBtn.style.display = "none";
                }
            })
            .catch(err => console.error(err));
    }

    // ============================
    // 5. 답변 작성 버튼 클릭
    // ============================
    createAnswerBtn?.addEventListener("click", () => {
        if (!answerContainer) return;
        answerContainer.style.display = "block";
        inlineAnswerForm.style.display = "block";
        noAnswerText.style.display = "none";
        answerDisplay.style.display = "none";

        createAnswerBtn.style.display = "none";
        editAnswerBtn.style.display = "none";
        deleteAnswerBtn.style.display = "none";

        answerContent.value = "";
    });

    // ============================
    // 6. 답변 수정 버튼 클릭
    // ============================
    editAnswerBtn?.addEventListener("click", () => {
        if (!answerContainer) return;
        answerContainer.style.display = "block";
        inlineAnswerForm.style.display = "block";
        noAnswerText.style.display = "none";
        answerDisplay.style.display = "none";

        createAnswerBtn.style.display = "none";
        editAnswerBtn.style.display = "none";
        deleteAnswerBtn.style.display = "none";

        answerContent.value = answerText.textContent;
    });

    // ============================
    // 7. 답변 저장
    // ============================
    saveAnswerBtn?.addEventListener("click", () => {
        if (!inquiryId) return alert("문의 선택 오류");
        const content = answerContent.value.trim();
        if (!content) return alert("답변 내용을 입력해주세요.");

        fetch(`/inquiry/admin/answer`, {
            method: "POST",
            headers: {
                "Content-Type": "application/x-www-form-urlencoded",
                [csrfHeader]: csrfToken
            },
            body: `inquiryId=${inquiryId}&content=${encodeURIComponent(content)}`,
            credentials: "same-origin"
        })
            .then(res => {
                if (!res.ok) throw new Error(`서버 오류: ${res.status}`);
                return res.text();
            })
            .then(msg => {
                alert(msg);
                answerContent.value = "";
                loadInquiryDetail();
            })
            .catch(err => {
                console.error(err);
                alert("답변 등록 오류: " + err.message);
            });
    });

    // ============================
    // 8. 답변 취소
    // ============================
    cancelAnswerBtn?.addEventListener("click", () => {
        inlineAnswerForm.style.display = "none";
        const hasAnswer = answerText.textContent?.trim() !== "";
        answerDisplay.style.display = hasAnswer ? "block" : "none";
        noAnswerText.style.display = hasAnswer ? "none" : "block";

        createAnswerBtn.style.display = hasAnswer ? "none" : "inline-block";
        editAnswerBtn.style.display = hasAnswer ? "inline-block" : "none";
        deleteAnswerBtn.style.display = hasAnswer ? "inline-block" : "none";
    });

    // ============================
// 9. 답변 삭제
// ============================
    deleteAnswerBtn?.addEventListener("click", () => {
        if (!inquiryId) return alert("문의 선택 오류");

        if (!confirm("정말 답변을 삭제하시겠습니까?")) return;

        fetch(`/inquiry/admin/answer/${inquiryId}`, {
            method: "DELETE",
            headers: {
                [csrfHeader]: csrfToken,
                "Content-Type": "application/json"
            },
            credentials: "same-origin"
        })
            .then(res => {
                if (!res.ok) throw new Error(`서버 오류: ${res.status}`);
                return res.text();
            })
            .then(msg => {
                alert(msg);

                // 답변 삭제 후 UI 초기화
                answerText.textContent = "";
                answerDisplay.style.display = "none";
                inlineAnswerForm.style.display = "none";
                noAnswerText.style.display = "block";
                answerContainer.style.display = "block";

                createAnswerBtn.style.display = "inline-block";
                editAnswerBtn.style.display = "none";
                deleteAnswerBtn.style.display = "none";
            })
            .catch(err => {
                console.error(err);
                alert("답변 삭제 오류: " + err.message);
            });
    });

    // ============================
    // 10. 초기 로딩
    // ============================
    loadInquiries();       // 리스트 페이지 로딩
    loadInquiryDetail();   // 상세 페이지 로딩
});
