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
	this.mutateParent();
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

/*
	{
   filter_:AndTerm(
       OrTerm(
           EntityTerm('obama'),
           EntityTerm('clinton')
       )
   ),
   series_:[
       OrTerm(
           LemmaTerm('healthcare'),
           LemmaTermm('medicare'),
       )
   ];
   buckets_:[
       YearTerm(2001),
       YearTerm(2002),
       YearTerm(2003),
       YearTerm(2004),
       YearTerm(2005),
       YearTerm(2006),
       YearTerm(2007),
       YearTerm(2008),
       YearTerm(2009),
   ]
} */

function LemmaOrEntityTerm(a) {
	return OrTerm(DocLemmaTerm(a),DocEntityTerm(a));
}

function queryForModelState(state) {
	var query = { filter_: false, series_: [], buckets_:[] };
	
	query.filter_ = state.filters
		.filter(function(x) { return x.filterType == "text" })
		.map(function(x) { return x.disjunction.map(LemmaOrEntityTerm).reduce(OrTerm)})
		.reduce(AndTerm);
	
	query.series_ = state.buckets
		.filter(function(x) { return x.filterType == "text" })
		.map(function(x) { return x.disjunction.map(LemmaOrEntityTerm).reduce(OrTerm)});
	
	if(state.horizontalAxis == "date") {
		if(state.dateGranularity == "year") {
			for(var i = state.startYear; i <= state.endYear; i++) {
				query.buckets_.push(YearTerm(i));
			}
		} else if (state.dateGranularity == "month") {
			for(var i = state.startYear; i <= state.endYear; i++) {
				for(var j = 1; j <= 12; j++) {
					query.buckets_.push(MonthTerm(i,j));
				}
			}
		} else if (state.dateGranularity == "fixed #") {
			// TODO
		}
	} else if (state.horizontalAxis == "page number") {
		// TODO: make clever buckets rather than 1 by 1
		for(var i = 1; i <= 30; i++) {
			query.buckets_.push(PageTerm(i,i+1));
		}
	}
	
	return query;
}

function addMockData() {
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

addMockData();
ko.applyBindings(viewModel);


$("#slider-range").slider({
	range: true,
	min: 2000,
	max: 2010,
	values: [viewModel.startYear(), viewModel.endYear()],
	slide: function(event, ui) {
		viewModel.startYear(ui.values[0]);
		viewModel.endYear(ui.values[1])
	}
});


// Fix for two-way binding when the start/end years are changed. This might be brittle
function updateSlider() {
	$("#slider-range").slider({values: [viewModel.startYear(), viewModel.endYear()]});
}
viewModel.startYear.subscribe(updateSlider);
viewModel.endYear.subscribe(updateSlider);



function newFilterWithEmptyLiteralTo(f) {
	var n = new SearchFilter();
	n.addLiteral('');
	f(n);
}

$("#filterList").droppable({
	accept: '.suggestion',
	activeClass: "filterListHover",
	drop: function(event, ui) {
			var n = new SearchFilter();
			n.addLiteral($(ui.draggable).text());
			viewModel.addFilter(n);
	}
});
$("#bucketList").droppable({
	accept: '.suggestion',
	activeClass: "filterListHover",
	drop: function(event, ui) {
			var n = new SearchFilter();
			n.addLiteral($(ui.draggable).text());
			viewModel.addBucket(n);
	}
});

function newInputsCallback() {
	$("input.justAdded").blur(queryChanged);
}

viewModel.filters.subscribe(newInputsCallback);
viewModel.buckets.subscribe(newInputsCallback);
newInputsCallback();

function suggestionsAdded() {
	$(".suggestion.justAdded").removeClass(".justAdded").draggable({helper: 'clone'});
}

viewModel.suggestions.subscribe(suggestionsAdded);
suggestionsAdded();

function queryChanged() {
	// will get called anytime the query gets changed in any way
	// we probably want to do some rate-limiting to avoid DoSing the server with queries
	viewModel.save();
}

viewModel.filters.subscribe(queryChanged);
viewModel.buckets.subscribe(queryChanged);
viewModel.startYear.subscribe(queryChanged);
viewModel.endYear.subscribe(queryChanged);
viewModel.horizontalAxis.subscribe(queryChanged);
viewModel.dateGranularity.subscribe(queryChanged);
viewModel.dateGranularityFixed.subscribe(queryChanged);






