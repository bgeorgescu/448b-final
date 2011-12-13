$('#js_filter_entities').buttonset();

$( "#help-dialog" ).dialog({
	autoOpen: false,
	height: 400,
	modal: true,
	width: 600,
	show: "fade",
	hide: "fade"
});

$('.help-link').click( function() {
  $( "#help-dialog" ).css("visibility", "visible");
	$( "#help-dialog" ).dialog( "open" );
  return false;
});