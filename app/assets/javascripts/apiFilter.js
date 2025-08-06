(function () {

    const elDocTypeFilterCount = document.getElementById('docTypeFilterCount');
    const elCategoryFilterCount = document.getElementById('categoryFilterCount');

    function updateDocTypeFilterCount() {
        let selectionCount = document.querySelectorAll('#docTypeFilterDetails input:checked').length;
        elDocTypeFilterCount.textContent = '' + selectionCount;
    }

    function updateCategoryFilterCount() {
        let selectionCount = document.querySelectorAll('#categoryFilterDetails input:checked').length;
        elCategoryFilterCount.textContent = '' + selectionCount;
    }

    let docTypeFilterDetails = document.querySelector("#docTypeFilterDetails")
    let allDocTypeCheckboxes = docTypeFilterDetails.querySelectorAll(".govuk-checkboxes")

    for (let i = 0; i < allDocTypeCheckboxes.length; i++) {
        allDocTypeCheckboxes[i].addEventListener("click", updateDocTypeFilterCount);
    }

    let categoryFilterDetails = document.querySelector("#categoryFilterDetails")
    let allCategoryCheckboxes = categoryFilterDetails.querySelectorAll(".govuk-checkboxes")
    for (let i = 0; i < allCategoryCheckboxes.length; i++) {
        allCategoryCheckboxes[i].addEventListener("click", updateCategoryFilterCount);
    }
})();
