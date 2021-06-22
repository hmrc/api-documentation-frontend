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
  for (var i = 0; i < links.length; i++) {
    links[i].addEventListener("click", function (e) {
      var clickedEndpoint = e.target.id
      var parentEndpoint = document.getElementById(clickedEndpoint + "-details");
      if (parentEndpoint.hasAttribute("open")) {
        var parentEndpoint = document.getElementById(clickedEndpoint + "-details");
        parentEndpoint.removeAttribute("open");
      } else {
        var parentEndpoint = document.getElementById(clickedEndpoint + "-details");
        var att = document.createAttribute("open");
        att.value = "";
        parentEndpoint.setAttributeNode(att);
      }
    });
  }

    // Action to be taken when details link is clicked
    var backBtns = document.getElementsByClassName("api-back-btn");
    for (var i = 0; i < backBtns.length; i++) {
      backBtns[i].addEventListener("click", function (e) {
        var clickedBackBnt = e.target.id.split('-')[0]
        var backBntParent = document.getElementById(clickedBackBnt + "-details");
        backBntParent.removeAttribute("open");
      });
    }

  window.addEventListener('load', function () {
    expandEnpoint()
  });
})();
