$('.toggle').click(function(e) {
  	e.preventDefault();

    var $this = $(this);

    console.log($this.attr("id"));

    var query = 'tr.' + $this.attr("id")

    var object = $this.parent().parent().parent().find(query);

    if (object.is(':hidden')) {
       object.css("display", "table-row");
    } else {
       object.css("display", "none");
    }

});