$(document).ready(function() {
  $('#version-select').submit(function(e) {
    e.preventDefault();

    var href = $(this).find('option:selected').val();

    if (href) {
      window.location = href;
    }
  });
});
