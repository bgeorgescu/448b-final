// http://phrogz.net/js/classes/OOPinJS2.html
Function.prototype.inheritsFrom = function( parentClassOrObject ){ 
	if ( parentClassOrObject.constructor == Function ) 
	{ 
		//Normal Inheritance 
		this.prototype = new parentClassOrObject;
		this.prototype.constructor = this;
		this.prototype.parent = parentClassOrObject.prototype;
	} 
	else 
	{ 
		//Pure Virtual Inheritance 
		this.prototype = parentClassOrObject;
		this.prototype.constructor = this;
		this.prototype.parent = parentClassOrObject;
	} 
	return this;
} 


function AbstractFilter() {
	this.filterType = "abstract";
	this.container = undefined;
}
AbstractFilter.prototype.remove = function() {
	if(this.container) {
		ko.utils.arrayRemoveItem(this.container, this);
		this.mutateParent();
		this.container = undefined;
	}
}
AbstractFilter.prototype.mutateParent = function() {
	if(this.container) {
		this.container.valueHasMutated();
	}
}
AbstractFilter.prototype.toPlainObject = function() {
	return { filterType: this.filterType }
}




SearchFilter.inheritsFrom(AbstractFilter);
function SearchFilter() {
	this.parent.constructor.call(this);
	this.filterType = "text";
	this.disjunction = new ko.observableArray([]);
}
SearchFilter.prototype.addLiteral = function(literal) {
	this.disjunction.push(new ko.observable(literal));
	this.disjunction.valueHasMutated();
}
SearchFilter.prototype.removeLiteral = function(literal) {
	ko.utils.arrayRemoveItem(this.disjunction, literal);
	this.mutateParent();
}
SearchFilter.prototype.toPlainObject = function() {
	var retval = this.parent.toPlainObject.call(this);
	var d = ko.utils.unwrapObservable(this.disjunction());
	retval.disjunction = [];
	for(var i = 0; i < d.length; i++) {
		retval.disjunction[i] = ko.utils.unwrapObservable(d[i]);
	}
	return retval;
}



DateFilter.inheritsFrom(AbstractFilter);
function DateFilter() {
	this.parent.constructor.call(this);
	this.filterType = "date";
}

 
var viewModel = {
	debug: true,
	filters: ko.observableArray([]),
	buckets: ko.observableArray([]),
	startYear: ko.observable(2000),
	endYear: ko.observable(2010),
	horizontalAxis: ko.observable("date"),
	dateGranularity: ko.observable("year"),
	dateGranularityOptions: ["year", "month", "fixed #"],
	dateGranularityFixed: ko.observable(100),
	suggestions: ko.observableArray([]), // note this doesn't need to be persisted to JSON

	
	addFilter: function (filter) {
		filter.container = viewModel.filters;
        viewModel.filters.push(filter);
        //viewModel.contacts.valueHasMutated();
    },
    
    addBucket: function (filter) {
		filter.container = viewModel.buckets;
        viewModel.buckets.push(filter);
    },
    
    toPlainObject: function () {
    	var retval = {
    		filters: [],
    		buckets: [],
    		startYear: this.startYear(),
    		endYear: this.endYear(),
    		horizontalAxis: this.horizontalAxis()};
    	for(i = 0; i < this.filters().length; i++) {
			retval.filters.push(this.filters()[i].toPlainObject());
		}
		for(i = 0; i < this.buckets().length; i++) {
			retval.buckets.push(this.buckets()[i].toPlainObject());
		}
		if(retval.horizontalAxis == "date") {
			retval.dateGranularity = this.dateGranularity();
			if(retval.dateGranularity == "fixed #")
				retval.dateGranularityFixed = this.dateGranularityFixed();
		}
		return retval;
    },
	
	save: function () {
        viewModel.lastSavedJson(ko.utils.stringifyJson(this.toPlainObject(), null, 2));
    },
    lastSavedJson: new ko.observable("")
}

function addMockSettings() {
	var f = new SearchFilter();
	f.addLiteral("election");
	viewModel.addFilter(f);
	
	f = new SearchFilter();
	f.addLiteral("clinton");
	f.addLiteral("hillary");
	viewModel.addBucket(f);
	
	f = new SearchFilter();
	f.addLiteral("obama");
	f.addLiteral("hope");
	f.addLiteral("barack");
	viewModel.addBucket(f);
	
	f = new SearchFilter();
	f.addLiteral("mccain");
	viewModel.addBucket(f);
	
	viewModel.startYear(2006);
	viewModel.endYear(2009);
	
	viewModel.suggestions().push("mitt romney");
	viewModel.suggestions().push("palin");
	viewModel.suggestions().push("kucinich");
}

addMockSettings();
ko.applyBindings(viewModel);

// TODO: figure out a way to subscribe slider to changes in model (i.e. 2 way updates)


$( "#slider-range" ).slider({
	range: true,
	min: 2000,
	max: 2010,
	values: [viewModel.startYear(), viewModel.endYear()],
	slide: function(event, ui) {
		viewModel.startYear(ui.values[0]);
		viewModel.endYear(ui.values[1])
	}
});

function newFilterWithEmptyLiteralTo(f) {
	var n = new SearchFilter();
	n.addLiteral('');
	f(n);
}