(function () {

    const elCategoryFilterCount = document.getElementById('categoryFilterCount');
    console.log("*** elCategoryFilterCount: "+ elCategoryFilterCount.textContent);

    function updateCategoryFilterCount() {
        console.log("*** In updateCategoryFilterCount");
        var selectionCount = document.querySelectorAll('#categoryFilters input:checked').length;
        console.log("*** In selectionCount: " + selectionCount);
        elCategoryFilterCount.textContent = '' + selectionCount;
        console.log("*** elCategoryFilterCount: "+ elCategoryFilterCount.textContent);
    }

    var allCheckboxes = document.querySelectorAll("#categoryFilterCheckBox")
    for (var i = 0; i < allCheckboxes.length; i++) {
        allCheckboxes[i].addEventListener("click", updateCategoryFilterCount);
    }
})();