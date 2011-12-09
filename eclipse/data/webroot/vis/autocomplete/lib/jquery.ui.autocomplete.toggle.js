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
    }
});

})( jQuery );
