(function () {
  // Expand endpoint function
  function expandEnpoint() {
    var url = decodeURI(window.location.href);
    var lastPart = url.split("#").pop();
    var endpoint = document.getElementById(lastPart + "-details");
    if (endpoint != null) {
      if (endpoint.hasAttribute("open")){
        // NEEDS WORK
        var endpoint = document.getElementById(lastPart + "-details");
        endpoint.removeAttribute("open");
      } else {
        var endpoint = document.getElementById(lastPart + "-details");
        var att = document.createAttribute("open");
        att.value = "";
        endpoint.setAttributeNode(att);
      }
    }
  }

  // Function to reset the height of the nav when expanding/closing endpoint
  function getHeight() {
    setTimeout(
      function() {
        var mainContent = document.getElementById("mainContent").offsetHeight;
        document.getElementById("navContent").style.height = mainContent + "px";
      }, 100);
  }

  // Action to be taken when details box is clicked
  var item = document.getElementsByClassName("govuk-details");
  function clickHandler(){
    getHeight()
  }
  for (var i = 0; i < item.length; i++) {
    item[i].addEventListener('click', clickHandler, false);
  }

  // Action to be taken when details link is clicked
  var links = document.getElementsByClassName("api-links");
  function linksHandler(){
    expandEnpoint()
    console.log("Clicked");
  }
  for (var i = 0; i < links.length; i++) {
    links[i].addEventListener('click', linksHandler, false);
  }

  window.addEventListener('popstate', function () {
    expandEnpoint()
  });

  window.addEventListener('load', function () {
    expandEnpoint()
  });
})();
