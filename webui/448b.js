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
	filters: ko.observableArray([]),
	buckets: ko.observableArray([]),
	startYear: ko.observable(2000),
	endYear: ko.observable(2010),
	horizontalAxis: ko.observable("date"),
	dateGranularity: ko.observable("year"),
	dateGranularityOptions: ["year", "month" /*, "fixed #"*/],
	dateGranularityFixed: ko.observable(100),
	
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
        viewModel.lastSavedJson(ko.utils.stringifyJson(queryForModelState(this.toPlainObject()), null, 2));
    },
    lastSavedJson: new ko.observable(""),
    
    // State below this does not reflect query stuff and doesn't need to be saved to JSON
    
    debug: true,
	suggestions: ko.observableArray([]), 
	graphModeOptions: ["bars","lines","steps"],
	graphMode: ko.observable("bars"),
	graphStack: ko.observable(true),
	graphData: ko.observable([]),
}

viewModel.graphOptions = ko.dependentObservable(function() {
    var retval = {
		series: {
			stack: this.graphStack() ? 1 : null,
			lines: { show: (this.graphMode() == "lines" || this.graphMode() == "steps"), fill: true, steps: this.graphMode() == "steps" },
			bars: { show: this.graphMode() == "bars",
					barWidth: 1 }
		},
		xaxis: {}
    };
    
    if(this.horizontalAxis() == "page") {
		// noop
    }
    else if(this.dateGranularity() == "year") {
    	retval.xaxis.mode = "time";
    	retval.xaxis.minTickWidth = [1, "year"];
    	retval.series.bars.barWidth = 0.8*365*86400000;
    	retval.xaxis.min = new Date(this.startYear(), 1, 1).getTime();
    	retval.xaxis.max = new Date(this.endYear(), 12, 31).getTime();
    } else if(this.dateGranularity() == "month") {
		retval.xaxis.mode = "time";
    	retval.series.bars.barWidth = 0.8*28*86400000;
    	retval.xaxis.minTickWidth = [1, "month"];
        retval.xaxis.min = new Date(this.startYear(), 0, 1).getTime();
    	retval.xaxis.max = new Date(this.endYear(), 11, 31).getTime();	
	}
    
    return retval;
}, viewModel);


function updatePlot() {
	$.plot($("#graph_container"), viewModel.graphData(), viewModel.graphOptions());
}


viewModel.graphOptions.subscribe(updatePlot);
viewModel.graphData.subscribe(updatePlot);

function array_range(x,y,step) {
	var retval = [];
	if(!step) step = 1;
	for(var i = x; i <= y; i+=step)
		retval.push(i);
	return retval;
}

function fakeData(state) {
	return state.buckets.map(function(x) {
		if (state.horizontalAxis == "page")
			return array_range(1, 30).map(function(a) {
				return [a, parseInt(Math.random() * 30)];
			});
		else if(state.dateGranularity == "year")
			return array_range(state.startYear, state.endYear).map(function(a) {
				return [new Date(a,1,1).getTime(), parseInt(Math.random() * 30)];
			});
		else if (state.dateGranularity == "month")
			return array_range(state.startYear, state.endYear).map(function(a) {
				return array_range(0,11).map(function(b) {
					return [new Date(a,b,1).getTime(),parseInt(Math.random() * 30)]
				});
			}).reduce(function(m,n) {
				return m.concat(n);
			});
		else if (state.dateGranularity == "fixed #")
			return array_range(1, state.dateGranularityFixed).map(function(a) {
				return [a, parseInt(Math.random() * 30)];
			});
	}).map(function(x,x_i) { 
		return { data: x, label: state.buckets[x_i].disjunction[0] };
	});
}


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
	
	
	if (state.horizontalAxis == "page")
		query.buckets = array_range(1, 30).map(function(a) {
			return PageTerm(a,a+1);
		});
	else if(state.dateGranularity == "year")
		query.buckets = array_range(state.startYear, state.endYear).map(function(a) {
			return YearTerm(a);
		});
	else if (state.dateGranularity == "month")
		query.buckets = array_range(state.startYear, state.endYear).map(function(a) {
			return array_range(0,11).map(function(b) {
				return MonthTerm(a,b);
			});
		}).reduce(function(m,n) {
			return m.concat(n);
		});
	else if (state.dateGranularity == "fixed #") {
		// ?
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
	viewModel.graphData(fakeData(viewModel.toPlainObject()));
}



viewModel.filters.subscribe(queryChanged);
viewModel.buckets.subscribe(queryChanged);
viewModel.startYear.subscribe(queryChanged);
viewModel.endYear.subscribe(queryChanged);
viewModel.horizontalAxis.subscribe(queryChanged);
viewModel.dateGranularity.subscribe(queryChanged);
viewModel.dateGranularityFixed.subscribe(queryChanged);

viewModel.graphData(fakeData(viewModel.toPlainObject()));
updatePlot();