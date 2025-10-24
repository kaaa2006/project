document.addEventListener("DOMContentLoaded", () => {

    // =========================
    // CSRF 토큰 설정 (AJAX 요청용)
    // =========================
    const csrfToken = document.querySelector('meta[name="_csrf"]').content;
    const csrfHeader = document.querySelector('meta[name="_csrf_header"]').content;

    // =========================
    // 1️⃣ 목록 페이지(list.html) 관련 JS
    // =========================
    const formContainer = document.getElementById("inquiryFormContainer");
    const form = document.getElementById("inquiryForm");
    const openBtn = document.getElementById("openInquiryFormBtn");
    const cancelBtn = document.getElementById("cancelInquiryBtn");
    const inquiryListTableBody = document.getElementById("inquiryListTableBody");

    if (form && openBtn && cancelBtn && inquiryListTableBody) {

        // ✅ 문의 리스트 로드
        function loadInquiries() {
            fetch("/inquiry/my", {
                method: "GET",
                headers: { "Content-Type": "application/json", [csrfHeader]: csrfToken }
            })
                .then(res => res.json())
                .then(data => {
                    inquiryListTableBody.innerHTML = "";

                    if (data.length === 0) {
                        inquiryListTableBody.innerHTML = `
                        <tr>
                            <td colspan="5" class="text-center">작성된 문의가 없습니다.</td>
                        </tr>`;
                        return;
                    }

                    data.forEach((inquiryBoard, index) => {
                        // ✅ 상태 한글로 변환
                        let statusText = "";
                        switch(inquiryBoard.status) {
                            case "PENDING": statusText = "답변 대기"; break;
                            case "COMPLETED": statusText = "답변 완료"; break;
                            default: statusText = inquiryBoard.status; // 혹시 다른 상태가 있으면 그대로 표시
                        }

                        // 날짜 안전 처리
                        let dateText = "-";
                        if (inquiryBoard.regDate) {
                            const date = new Date(inquiryBoard.regDate);
                            dateText = date.toLocaleString('ko-KR', {
                                year: 'numeric',
                                month: '2-digit',
                                day: '2-digit',
                                hour: 'numeric',     // 앞자리 0 제거
                                minute: '2-digit',
                                hour12: true         // 12시간제
                            });
                        }

                        const tr = document.createElement("tr");
                        tr.innerHTML = `
        <td>${index + 1}</td>
        <td><a href="/inquiry/my/${inquiryBoard.id}" class="fw-bold">${inquiryBoard.title}</a></td>
        <td>${inquiryBoard.userName}</td>
        <td>${dateText}</td>  <!-- ← 여기 -->
        <td>${statusText}</td>
    `;
                        inquiryListTableBody.appendChild(tr);
                    });
                });
        }

        loadInquiries();

        // ✅ 문의 작성 폼 열기
        openBtn.addEventListener("click", () => {
            formContainer.style.display = "block";
            openBtn.style.display = "none";
        });

        // ✅ 문의 작성 폼 닫기
        cancelBtn.addEventListener("click", () => {
            formContainer.style.display = "none";
            openBtn.style.display = "block";
            form.reset();
        });

        // ✅ 문의 작성
        form.addEventListener("submit", e => {
            e.preventDefault();
            const title = document.getElementById("title").value.trim();
            const content = document.getElementById("content").value.trim();

            if (!title || !content) {
                alert("제목과 내용을 입력해주세요.");
                return;
            }

            fetch("/inquiry/my", {
                method: "POST",
                headers: { "Content-Type": "application/json", [csrfHeader]: csrfToken },
                body: JSON.stringify({ title, content })
            })
                .then(res => res.json())
                .then(() => {
                    form.reset();
                    formContainer.style.display = "none";
                    openBtn.style.display = "block";
                    loadInquiries();
                });
        });
    }

    // =========================
    // 2️⃣ 상세 페이지(read.html) 관련 JS
    // =========================
    const titleDisplay = document.getElementById("titleDisplay");
    const contentDisplay = document.getElementById("contentDisplay");
    const answerDisplay = document.getElementById("answerDisplay"); // 답변 표시용
    const titleEdit = document.getElementById("titleEdit");
    const contentEdit = document.getElementById("contentEdit");
    const editBtn = document.getElementById("editInquiryBtn");
    const saveEditBtn = document.getElementById("saveEditBtn");
    const cancelEditBtn = document.getElementById("cancelEditBtn");
    const deleteBtn = document.getElementById("deleteInquiryBtn");

    if (titleDisplay && editBtn && deleteBtn) {
        const inquiryId = titleDisplay.dataset.id;

        let editing = false; // 편집 모드 상태
        let saving = false;

        fetch(`/inquiry/my/${inquiryId}/json`, { headers: { [csrfHeader]: csrfToken } })
            .then(res => res.json())
            .then(dto => {
                if(dto.answerContent){
                    // 답변이 있으면 버튼 숨기기
                    editBtn.style.display = "none";
                    deleteBtn.style.display = "none";
                } else {
                    // 답변이 없으면 버튼 보이게 + 클릭 이벤트 연결
                    editBtn.style.display = "inline-block";
                    deleteBtn.style.display = "inline-block";

                    editBtn.addEventListener("click", () => {
                        if (!editing) {
                            // ========================
                            // 편집 모드로 전환
                            // ========================
                            editing = true;

                            // 기존 텍스트 저장
                            const originalTitle = titleDisplay.textContent;
                            const originalContent = contentDisplay.textContent;

                            // input / textarea 생성
                            const titleInput = document.createElement("input");
                            titleInput.type = "text";
                            titleInput.id = "titleInput";
                            titleInput.className = "form-control mb-2";
                            titleInput.value = originalTitle;

                            const contentInput = document.createElement("textarea");
                            contentInput.id = "contentInput";
                            contentInput.className = "form-control mb-2";
                            contentInput.rows = 5;
                            contentInput.value = originalContent;

                            titleDisplay.replaceWith(titleInput);
                            contentDisplay.replaceWith(contentInput);

                            // 버튼 텍스트 변경
                            editBtn.textContent = "저장";

                            // 취소 버튼 생성
                            const cancelBtn = document.createElement("button");
                            cancelBtn.type = "button";
                            cancelBtn.textContent = "취소";
                            cancelBtn.className = "btn btn-secondary btn-sm ms-2";
                            editBtn.parentNode.appendChild(cancelBtn);

                            // 취소 버튼 클릭
                            cancelBtn.addEventListener("click", () => {
                                titleInput.replaceWith(titleDisplay);
                                contentInput.replaceWith(contentDisplay);
                                editBtn.textContent = "수정";
                                cancelBtn.remove();
                                editing = false;
                            });

                        } else {
                            if(saving) return; // 중복 클릭 방지
                            const titleInput = document.querySelector("#titleInput");
                            const contentInput = document.querySelector("#contentInput");
                            if (!titleInput || !contentInput) return;

                            saving = true;
                            fetch(`/inquiry/my/${inquiryId}`, {
                                method: "PUT",
                                headers: {
                                    "Content-Type": "application/json",
                                    [csrfHeader]: csrfToken
                                },
                                body: JSON.stringify({
                                    title: titleInput.value,
                                    content: contentInput.value
                                })
                            })
                                .then(res => res.text()) // 문자열 반환일 경우
                                .then(msg => {
                                    alert(msg);
                                    if(msg.includes("수정되었습니다")) {
                                        titleDisplay.textContent = titleInput.value;
                                        contentDisplay.textContent = contentInput.value;
                                        titleInput.replaceWith(titleDisplay);
                                        contentInput.replaceWith(contentDisplay);
                                        editBtn.textContent = "수정";
                                        const cancelBtn = editBtn.parentNode.querySelector("button.btn-secondary");
                                        if(cancelBtn) cancelBtn.remove();
                                        editing = false;
                                    }
                                })
                                .finally(() => saving = false);
                        }
                    });

                    deleteBtn.addEventListener("click", () => {
                        if(confirm("정말 삭제하시겠습니까?")){
                            fetch(`/inquiry/my/${inquiryId}`, {
                                method:"DELETE",
                                headers:{ [csrfHeader]: csrfToken }
                            })
                                .then(res => res.text().then(msg => ({ ok: res.ok, msg })))
                                .then(result => {
                                    alert(result.msg);
                                    if(result.ok) window.location.href = "/inquiry/my/list";
                                })
                                .catch(() => alert("삭제 중 오류가 발생했습니다."));
                        }
                    });
                }
            })
            .catch(() => console.error("문의 데이터 로드 실패"));
    }
});
