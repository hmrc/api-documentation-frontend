(function () {

    const elDocTypeFilterCount = document.getElementById('docTypeFilterCount');
    const elCategoryFilterCount = document.getElementById('categoryFilterCount');

    function updateDocTypeFilterCount() {
        let selectionCount = document.querySelectorAll('#docTypeFilters input:checked').length;
        elDocTypeFilterCount.textContent = '' + selectionCount;
    }

    function updateCategoryFilterCount() {
        let selectionCount = document.querySelectorAll('#categoryFilters input:checked').length;
        elCategoryFilterCount.textContent = '' + selectionCount;
    }

    let allDocTypeCheckboxes = document.querySelectorAll("#docTypeFilterCheckBox")
    for (let i = 0; i < allDocTypeCheckboxes.length; i++) {
        allDocTypeCheckboxes[i].addEventListener("click", updateDocTypeFilterCount);
    }

    let allCategoryCheckboxes = document.querySelectorAll("#categoryFilterCheckBox")
    for (let i = 0; i < allCategoryCheckboxes.length; i++) {
        allCategoryCheckboxes[i].addEventListener("click", updateCategoryFilterCount);
    }
})();
