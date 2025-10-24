document.addEventListener("DOMContentLoaded", () => {

    // ========== 📌 페이지네이션 처리 ==========
    const paginationContainer = document.getElementById("event-pagination");

    if (paginationContainer) {
        const currentPage = Number(paginationContainer.dataset.currentPage);
        const totalPages = Number(paginationContainer.dataset.totalPages);

        function goToPage(pageNum) {
            const url = new URL(window.location.href);
            url.searchParams.set("page", pageNum);
            window.location.href = url.toString();
        }

        renderPagination({
            container: paginationContainer,
            totalPages,
            currentPage,
            onPageClick: goToPage
        });
    }

    // ========== 📌 폼 관련 로직 (등록 시) ==========
    const today = new Date().toISOString().split("T")[0];
    const registerForm = document.getElementById("registerForm");
    const boardTypeSelect = document.getElementById("boardTypeSelect");
    const fileUploadDiv = document.getElementById("fileUploadDiv");
    const dateDiv = document.getElementById("dateDiv");
    const startDateInput = document.getElementById("startDate");
    const endDateInput = document.getElementById("endDate");

    function toggleBoardForm() {
        const boardType = boardTypeSelect?.value?.toUpperCase() || 'NOTICE';

        // ── 파일 업로드 div 표시
        if (fileUploadDiv) {
            if (boardType === 'FAQ') {
                fileUploadDiv.style.display = 'none';
            } else {
                // FAQ 외에는 항상 보이도록
                fileUploadDiv.style.display = 'block';
            }
        }

        // ── EVENT 전용 날짜 div
        if (dateDiv) {
            dateDiv.style.display = (boardType === 'EVENT') ? 'block' : 'none';
        }

        // ── FAQ일 때 placeholder 변경
        if (boardType === 'FAQ') {
            document.getElementById("titleLabel").innerText = "질문: ";
            document.getElementById("titleInput").placeholder = "질문을 입력하세요";
            document.getElementById("contentLabel").innerText = "답변: ";
            document.getElementById("contentInput").placeholder = "답변을 입력하세요";
        } else {
            document.getElementById("titleLabel").innerText = "제목: ";
            document.getElementById("titleInput").placeholder = "";
            document.getElementById("contentLabel").innerText = "내용: ";
            document.getElementById("contentInput").placeholder = "";
        }
    }

    if (boardTypeSelect) {
        toggleBoardForm();
        boardTypeSelect.addEventListener('change', toggleBoardForm);
    }

    // 날짜 제한
    if (startDateInput && endDateInput) {
        const todayStr = new Date().toISOString().split("T")[0];

        // ── 오늘 이후만 선택 가능하도록 설정
        startDateInput.setAttribute("min", todayStr);
        endDateInput.setAttribute("min", todayStr);

        const updateDisplay = () => {
            const start = new Date(startDateInput.value);
            const end = new Date(endDateInput.value);
            const options = { year:'numeric', month:'2-digit', day:'2-digit', hour:'numeric', minute:'2-digit', hour12:true };

            document.getElementById("startDateDisplay").innerText = startDateInput.value ? start.toLocaleString('ko-KR', options) : "";
            document.getElementById("endDateDisplay").innerText = endDateInput.value ? end.toLocaleString('ko-KR', options) : "";
        }

        startDateInput.addEventListener("change", updateDisplay);
        endDateInput.addEventListener("change", updateDisplay);

        updateDisplay(); // 초기 표시
    }

    // 등록 폼 제출 시 유효성 검사
    if (registerForm) {
        registerForm.addEventListener("submit", (event) => {
            const titleInput = document.getElementById("titleInput");
            const boardType = boardTypeSelect?.value?.toUpperCase() || 'NOTICE';

            if (titleInput && titleInput.value.length < 5) {
                alert("제목은 최소 5글자 이상이어야 합니다!");
                event.preventDefault();
                return;
            }

            if (boardType === 'EVENT' && dateDiv && dateDiv.style.display !== 'none') {
                if (!startDateInput.value || !endDateInput.value) {
                    alert("시작일과 종료일을 모두 입력하세요!");
                    event.preventDefault();
                    return;
                }

                if (new Date(endDateInput.value) < new Date(startDateInput.value)) {
                    alert("종료일은 시작일 이후로 설정해야 합니다!");
                    event.preventDefault();
                    return;
                }
            }

            // submit 통과 시 콘솔 로그 (옵션)
            console.log("submit fired");
            console.log("boardType:", boardTypeSelect.value);
            console.log("startDate:", startDateInput.value);
            console.log("endDate:", endDateInput.value);
        });
    }

});
