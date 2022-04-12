(function () {
  window.addEventListener('load', function () {
    var contentBox = document.getElementById("mainContent").offsetHeight;
    var section = document.querySelectorAll("section");
    var currentSection = {};
    var screenHeight = window.screen.height;
    var onScreenSections = {};

    // Extracts all onscreen sections
    Array.prototype.forEach.call(section, function(e) {
      if(e.offsetTop > 0 ) {
        onScreenSections[e.id] = e.offsetTop;
      }
    });

    // Resets nav height for sticky nav
    if (screenHeight >= 769) {
      if (contentBox > 990) {
        document.getElementById("navContent").style.height = contentBox + "px";
      }
    }
  
    // Gets the parent of the current link and give's active class
    function hasSomeParentTheClass(element, classname) {
      if (element.parentNode.className.split(' ').indexOf(classname)>= null) {
        element.parentNode.setAttribute('class', 'side-nav__list side-nav__list--selected');
      }
    }
  
    // Gets users position and applies active to the correct section
    function getPosition() {
      // Gets users position on the screen
      var scrollPosition = document.documentElement.scrollTop || document.body.scrollTop;
      var lastItem = null;

      for (i in onScreenSections) {
        lastItem = i;
      }
  
      // Finds which seaction the users in
      Array.prototype.forEach.call(section, function(e) {
        if(e.offsetTop > 0 && scrollPosition >= e.offsetTop) {
          currentSection = {};
          currentSection[e.id] = e.offsetTop;
        }
      });
  
      // Matches the section to the nav link
      for (i in currentSection) {
        var index = i.toLowerCase();
        var link = document.querySelector("a[href*='" + CSS.escape(index) + "'");
        var difference = document.documentElement.scrollHeight - window.innerHeight;
        var scrollposition = document.documentElement.scrollTop;
  
        // Removes any active states
        var elems = document.querySelectorAll(".side-nav__list--selected");
        [].forEach.call(elems, function(el) {
          el.classList.remove("side-nav__list--selected");
        });
  
        // Calls the function to give current nav section active state
        if(link != null){
          if (difference - scrollposition <= 2) {
            var lastLink = document.querySelector("a[href*='" + CSS.escape(lastItem) + "'");
            hasSomeParentTheClass(lastLink, 'side-nav__list');
          } else {
            hasSomeParentTheClass(link, 'side-nav__list');
          }
          
        }
      }
    }

    // Action to be taken when user scrolls
    window.onscroll = function () {
      "use strict";
      getPosition();
    };
  })
})();
