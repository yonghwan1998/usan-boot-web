function __toggleClearBtn(inputEl) {
    const wrapper = inputEl.closest(".target__clear-wrapper");
    const btn = wrapper.querySelector(".target--clear-btn");

    btn.style.display = inputEl.value.trim().length > 0 ? "block" : "none";
}

function __clearInput(btnEl) {
    const wrapper = btnEl.closest(".target__clear-wrapper");
    const input = wrapper.querySelector(".target--clear-input");

    input.value = "";
    btnEl.style.display = "none";
    input.focus();
}

function __toggleViewBtn(btn) {
    const wrapper = btn.closest('.target__view-wrapper');
    if (!wrapper) return;

    const input = wrapper.querySelector('.target--view-input');
    if (!input) return;

    const isHidden = input.type === 'password';

    input.type = isHidden ? 'text' : 'password';

    if (isHidden) {
        btn.classList.remove('common-input__eye-slash-btn');
        btn.classList.add('common-input__eye-btn');
    } else {
        btn.classList.remove('common-input__eye-btn');
        btn.classList.add('common-input__eye-slash-btn');
    }
}