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
                if(self.menu.active) {
                    switch( event.keyCode ) {
                    case keyCode.ESCAPE:
                        self.options.revert();
                        self.close();
                        return;
                    case keyCode.ENTER:
                    case keyCode.PLUS:
                    case keyCode.OPEN_BRACKET:
                    case keyCode.CLOSE_BRACKET:
                        self.options.commit(self.term);
                        self.close();
                        return;
                    case keyCode.SPACE:
                        var checkbox = $(".autocomplete-check", self.menu.active);
                        var old = checkbox.attr("checked");
                        if(old)
                            checbox.removeAttr("checked");
                        else
                            checkbox.attr("checked", true);
                        checkbox.button("refresh");
                        suppress = true;
                        return;
                    default:
                    }
                } else {
                    switch( event.keyCode ) {
                    case keyCode.ENTER:
                    case keyCode.PLUS:
                    case keyCode.OPEN_BRACKET:
                    case keyCode.CLOSE_BRACKET:
                    case keyCode.SPACE:
                        self.options.commit(self.term);
                        self.close();
                    default:
                    }
                }
            }
            old_down.handler(event, ui);
        });
        var old_press = events.keydown[0];
        this.element.unbind('keypress.autocomplete', old_down);
        this.element.bind("keypress.autocomplete", function(event, ui){
            if(suppress) {
                suppress = false;
                event.preventDefault();
            }
        });
    },
    _renderMenu: function( ul, items ) {
        var self = this,
            currentCategory = "";
        $.each( items, function( index, item ) {
            console.log(index);
            if ( item.category != currentCategory ) {
               //need float or categories can be reordered
               ul.append( "<li style='float:left' class='ui-autocomplete-category'>" + item.category + "</li>" );
                currentCategory = item.category;
            }
            self._renderItem( ul, item );
        });
        $(".autocomplete-category", ul).button({disabled:true});
    },
});

})( jQuery );
