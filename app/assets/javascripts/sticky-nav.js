$(function () {
  // value to add a margin of padding when scrolling through selections
  var SCROLL_PADDING = 100;

  var requireStickySideNav = $('*[data-sticky-left-nav]').length;

  function isElementInViewport (el) {
      if (typeof jQuery !== 'undefined' && el instanceof jQuery) {
        el = el[0];
      }

      var rect = el.getBoundingClientRect();

      return !(rect.bottom < 0
            || rect.right < 0
            || rect.left > window.innerWidth
            || rect.top > window.innerHeight);
  }

  function scrolledToBottom(bottomPadding) {
    bottomPadding = parseInt(bottomPadding) || 0;
    return ((window.innerHeight + window.scrollY + bottomPadding) >= document.body.offsetHeight);
  }

  function removeActiveState (links) {
    $.each(links, function (index, el) {
      var $parent = $(el).parent();
      if ($parent) {
        if ($parent.hasClass('side-nav__list--selected')) {
          $parent.removeClass('side-nav__list--selected');
        }
      }
    })
  }

  function resizeMenu () {
    $('ul.side-nav__component').css('width', $('.column-one-third').width());
  }

  function checkMenuActiveState (menuItems, scrollTop, atBottom) {
    var selectedIndex = -1;

    menuItems.each(function(index, item) {
      var target = $(item.hash);
      var previousTarget = index > 0 ? $(menuItems[index - 1])
                                     : null;
      var targetOffset = target.offset();

      if (   (targetOffset && scrollTop > targetOffset.top - SCROLL_PADDING)
          || (   previousTarget && atBottom
              && isElementInViewport(target)
              && !isElementInViewport($(previousTarget[0].hash)) )) {
        selectedIndex = index;
      }
    });

    if (selectedIndex > -1) {
      var selectedItem = menuItems[selectedIndex];
      var listItem = $(selectedItem).parent();
      listItem.addClass('side-nav__list--selected');
      removeActiveState([menuItems[selectedIndex + 1], menuItems[selectedIndex - 1]]);

      var subMenuItems = $(selectedItem).parent().find('.fixed-navigation__sub-list a');
      if (subMenuItems.length) {
        checkMenuActiveState(subMenuItems, scrollTop, atBottom);
      }
    }
  }

  function setActiveStateOnScroll () {
    if (!requireStickySideNav) return;

    var scrollTop = $(this).scrollTop();
    var bottomPadding = $('#footer').height();
    var atBottom = scrolledToBottom(bottomPadding);
    var navHeight = $('.side-nav__component').height();
    var bounds = $('*[data-sticky-left-nav]')[0].getBoundingClientRect();

    $('.grid-row').each(function () {
      var topDistance = $(this).offset().top - SCROLL_PADDING;

      if (scrollTop === 0 && requireStickySideNav) {
        $('.side-nav__component li').removeClass('side-nav__list--selected');
      }

      if (bounds.bottom - navHeight < 0) {
        if ($('.side-nav__component').hasClass('affix')) {
          $('.side-nav__component.affix')
            .removeClass('affix')
            .addClass('affix-bottom');
        }
      } else if (topDistance < scrollTop) {
        if ($('.side-nav__component').hasClass('affix-top')) {
          $('.side-nav__component.affix-top')
            .removeClass('affix-top')
            .addClass('affix');
        }
        if ($('.side-nav__component').hasClass('affix-bottom')) {
          $('.side-nav__component.affix-bottom')
            .removeClass('affix-bottom')
            .addClass('affix');
        }
      } else {
        if ($('.side-nav__component').hasClass('affix')) {
          $('.side-nav__component.affix')
            .removeClass('affix')
            .addClass('affix-top');
        }
      }
    });

    var topLevelMenuItems = $('.side-nav__component.affix a').not('.fixed-navigation__sub-list a');

    checkMenuActiveState(topLevelMenuItems, scrollTop, atBottom);

    resizeMenu();
  }

  if (requireStickySideNav) {
    $('ul.side-nav__component').addClass('affix-top');
    $('.side-nav__link').addClass('side-nav__link--in-page');
  }

  $(window)
    .on('scroll', setActiveStateOnScroll)
    .on('resize', resizeMenu);

  $('nav.side-nav').removeClass('js-hidden');
});
