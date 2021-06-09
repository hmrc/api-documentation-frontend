(function () {
  var section = document.querySelectorAll(".section");
  var currentSection = {};

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

      // Removes any active states
      var elems = document.querySelectorAll(".side-nav__list--selected");
      [].forEach.call(elems, function(el) {
        el.classList.remove("side-nav__list--selected");
      });

      // Calls the function to give current nav section active state
      if(link != null){
        hasSomeParentTheClass(link, 'side-nav__list');
      }
    }
  }

  // Action to be taken when user scrolls
  window.onscroll = function () {
    "use strict";
    getPosition();
  };
})();
