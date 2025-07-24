(function () {

    const elDocTypeFilterCount = document.getElementById('docTypeFilterCount');
    console.log("*** elDocTypeFilterCount: "+ elDocTypeFilterCount.textContent);

    function updateDocTypeFilterCount() {
        console.log("*** In updateDocTypeFilterCount");
        var selectionCount = document.querySelectorAll('#docTypeFilters input:checked').length;
        console.log("*** In selectionCount: " + selectionCount);
        elDocTypeFilterCount.textContent = '' + selectionCount;
        console.log("*** elDocTypeFilterCount: "+ elDocTypeFilterCount.textContent);
    }

    var allCheckboxes = document.querySelectorAll("#docTypeFilterCheckBox")
    for (var i = 0; i < allCheckboxes.length; i++) {
        allCheckboxes[i].addEventListener("click", updateDocTypeFilterCount);
    }
})();
