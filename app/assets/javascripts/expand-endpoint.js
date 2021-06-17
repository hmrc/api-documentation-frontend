(function () {
  // Expand endpoint function
  function expandEnpoint() {
    var url = decodeURI(window.location.href);
    var lastPart = url.split("#").pop();
    var endpoint = document.getElementById(lastPart + "-details");
    if (endpoint != null) {
      var endpoint = document.getElementById(lastPart + "-details");
      var att = document.createAttribute("open");
      att.value = "";
      endpoint.setAttributeNode(att);
    }
  }

  function getHeight() {
    setTimeout(
      function() {
        var mainContent = document.getElementById("mainContent").offsetHeight;
        document.getElementById("navContent").style.height = mainContent + "px";
      }, 100);
  }

  var item = document.getElementsByClassName("govuk-details");

  function clickHandler(){
    console.log("Clicked");
    getHeight()
  }

  for (var i = 0; i < item.length; i++) {
    item[i].addEventListener('click', clickHandler, false);
  }

  window.addEventListener('popstate', function () {
    expandEnpoint()
  });
  window.addEventListener('load', function () {
    expandEnpoint()
  });
})();
