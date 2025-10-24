document.addEventListener('DOMContentLoaded', () => {
    // 삭제 확인
    const delForm = document.querySelector('.inline-delete');
    if (delForm) {
        delForm.addEventListener('submit', (e) => {
            const ok = confirm('정말 삭제하시겠습니까?');
            if (!ok) e.preventDefault();
        });
    }
});
