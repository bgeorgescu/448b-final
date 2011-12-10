(function( $ ) {

var proto = $.ui.autocomplete.prototype;
var renderItem = proto._renderItem;
var create = proto._create;

$.extend( proto, {
	_renderItem: function( ul, item) {
        var res = renderItem.call(this, ul, item);
        $(".autocomplete-check").button();
        return res;
	},
    _create: function() {
        create.call(this);
        var self = this;
        var doc = this.element[ 0 ].ownerDocument;
        this.menu.options.selected = function(event, ui) {
			var item = ui.item.data( "item.autocomplete" ),
            previous = self.previous;

            // only trigger when focus was lost (click on menu)
            if ( self.element[0] !== doc.activeElement ) {
                self.element.focus();
                self.previous = previous;
                // #6109 - IE triggers two focus events and the second
                // is asynchronous, so we need to reset the previous
                // term synchronously and asynchronously :-(
                setTimeout(function() {
                    self.previous = previous;
                    self.selectedItem = item;
                }, 1);
            }

            if ( false !== self._trigger( "select", event, { item: item } ) ) {
                self.element.val( item.value );
                self.close( event ); // only close if select says we do it
            }
            // reset the term after the select event
            // this allows custom select handling to work properly
            self.term = self.element.val();
            self.selectedItem = item;
        }
        var suppress = false;
        //todo: should lookup using namespace
        var events = this.element.data("events");
        var old_down = events.keydown[0];
        this.element.unbind('keydown.autocomplete', old_down);
        this.element.bind("keydown.autocomplete", function(event, ui){
            var keyCode = $.ui.keyCode;
            if(self.menu.element.is(":visible")) {
                switch( event.keyCode ) {
                case keyCode.ENTER:
                    var checkbox = $(".autocomplete-check");
                    var old = checkbox.attr("checked");
                    checkbox.attr("checked", !old);
                    checkbox.button("refresh");
                    return;
                default:
                }
            }
            old_down.handler(event, ui);
        });
        var old_press = events.keydown[0];
        this.element.unbind('keypress.autocomplete', old_down);
        this.element.bind("keypress.autocomplete", function(event, ui){
            if(suppress) {
                suppress = false;
                event.preventdefault();
            }
        });
    },
    _renderMenu: function( ul, items ) {
        var self = this,
            currentCategory = "";
        $.each( items, function( index, item ) {
            if ( item.category != currentCategory ) {
                ul.append( "<li class='ui-autocomplete-category'>" + item.category + "</li>" );
                currentCategory = item.category;
            }
            self._renderItem( ul, item );
        });
    },
});

})( jQuery );
