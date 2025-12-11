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