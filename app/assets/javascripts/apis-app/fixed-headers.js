$(function() {
    function UpdateTableHeaders() {
        $(".persist-area").each(function() {
            var el, offset, scrollTop, floatingHeader;

            el = $(this);
            offset = el.offset();
            scrollTop = $(window).scrollTop();
            floatingHeader = $(".api-accordion__row--fixed", this);

            var visible = ((scrollTop > offset.top) && (scrollTop < offset.top + (el.height() - floatingHeader.innerHeight())))
                ? 'visible'
                : 'hidden';

            floatingHeader.css({"visibility": visible});
        });
    }

    $(".persist-area:not(.expanded)").each(function() {
        var clonedHeaderRow = $("div.persist-header", this).not(".api-accordion__row--fixed");
        clonedHeaderRow.before(clonedHeaderRow.clone(true)).css("width", clonedHeaderRow.parent().width()).addClass("api-accordion__row--fixed");
    });

    $(window).scroll(UpdateTableHeaders).trigger("scroll");
    $(window).resize(function () {
        var $floatingHeader = $("div.api-accordion__row--fixed");
        $floatingHeader.css("width", $floatingHeader.parent().width());
    });
});
