var globalRevertDuration=100;

function TemplateInstance(t) {
	return $("#"+t+"_template").clone(true, true).removeAttr("id");
}

function Literal() {
	return TemplateInstance("literal").draggable({
		revert:"invalid",
		revertDuration: globalRevertDuration,
		zIndex: 2700
	});
}


function SetLiteralType(literal, type) {
	// TODO
}

function GetLiteralType(literal) {
	// TODO
	return "default";
}

function GetLiteralText(literal) {
	return literal.text();
}

function SetLiteralText(literal, text) {
	literal.text(text);
}


function AddDisjunctionToSeries(disjunction, series) {
	disjunction.appendTo(series.find(".contents")[0]);
}

function CopyLiteral(literal) {
	var l = Literal();
	SetLiteralType(l, GetLiteralType(literal));
	SetLiteralText(l, GetLiteralText(literal));
	return l;
}

function AddLiteralCopyToDisjunction(literal, disjunction) {
	CopyLiteral(literal)
		.addClass("dropped")
		.appendTo(disjunction.find(".contents")[0]);
}

function AddLiteralToDisjunction(literal, disjunction) {
	literal
		.addClass("dropped")
		.appendTo(disjunction.find(".contents")[0]);
}

function AddLiteralCopyToSeries(literal, series) {
	var d = Disjunction();
	AddLiteralCopyToDisjunction(literal, d);
	AddDisjunctionToSeries(d, series);
}

function Series() {
	var t = TemplateInstance("series");
	t.droppable({
		accept: function(x) {
			return x.hasClass("literal") && !x.hasClass("dropped")
				|| x.hasClass("disjunction")
		},
		activeClass: "droppable",
		hoverClass: "hover",
		drop: function(event, ui) {
			if(ui.draggable.hasClass("disjunction")) {
				AddDisjunctionToSeries(ui.draggable, $(this));
				ui.draggable.attr('style','');
			}
			else if(ui.draggable.hasClass("literal")) {
				AddLiteralCopyToSeries(ui.draggable, $(this));
			}
		},
		greedy:true
	});
	t.draggable({
		revert:"invalid",
		handle: ".sGrip",
		zIndex: 2700,
		revertDuration: globalRevertDuration,
	});
	return t;
}


function Disjunction() {
	var t = TemplateInstance("disjunction");
	t.droppable({
		accept: function(x) { return x.hasClass("literal") && !x.hasClass("dropped")},
		activeClass: "droppable",
		hoverClass: "hover",
		drop: function(event, ui) {
			AddLiteralCopyToDisjunction(ui.draggable, $(this));
		},
		greedy:true
	});
	t.draggable({
		revert:"invalid",
		handle: ".dGrip",
		zIndex: 2700,
		//helper: function() { return $(this).css("z-index",10000) },
		revertDuration: globalRevertDuration,
	});
	return t;
}


$("#trash").droppable({
	accept: function(x) {
		return x.hasClass("literal") && x.hasClass("dropped")
			|| x.hasClass("disjunction")
			|| x.hasClass("series")
	},
	drop: function(event, ui) {
		$(ui.draggable).remove();
	},
	activeClass:"droppable",
	hoverClass: "hover",
	tolerance: "pointer",
	greedy:true,
})


function domStateToObject() {
	return {
		series:
			$(".series:not(#series_template) > .contents").get().map(function(x) {
				return {
					contents:		
						$(x).find(".disjunction > .contents").get().map(function(y) {
							return $(y).find(".literal").get().map(function(z) {
								return {text: GetLiteralText($(z)), type: GetLiteralType($(z))};
							});
						})
				}
			}),
		startYear: 2000,
		endYear: 2010,
		horizontalAxis: "date",
		dateGranularity: "month"
	}
}

function objectToDomState(obj) {
	var series_container = $("#series");
	series_container.empty();
	series_container.html("&nbsp;");
	$.each(obj.series, function(i,series) {
		var s = Series();
		series_container.append(s);
		$.each(series.contents, function(j, disjunction) {
			var d = Disjunction();
			AddDisjunctionToSeries(d,s);
			$.each(disjunction, function(k, literal) {
				var l = Literal();
				SetLiteralText(l, literal.text);
				SetLiteralType(l, literal.type);
				AddLiteralToDisjunction(l,d);
			});
		});
	});
}

function array_range(x,y,step) {
	var retval = [];
	if(!step) step = 1;
	for(var i = x; i <= y; i+=step)
		retval.push(i);
	return retval;
}


/*

snippet to find first literal of plottable series (to be used for legend):
$(".series").filter(function(x) { return $(this).find(".literal").length; }).find(".literal").first();

*/

function queryForObject(state) {
	var query = {};
	var AndHelper = function(arr) { return arr.length > 1 ? {and_:{terms_: arr }} : arr[0]; };
	var OrHelper = function(arr) { return arr.length > 1 ? {or_:{terms_: arr }} : arr[0]; };
	var WordToTerm = LemmaTerm;
	
	WordToTerm = function(a) { return OrTerm(LemmaTerm(a), EntityTerm(a)); };

	
	query.filter_ = {date_:{before_:(state.endYear+1)*10000, after_:(state.startYear*10000 - 1)}};
	
	query.series_ = state.series
		.filter(function(x) {
			return x.contents.length
		})
		.map(function(x) {
			return AndHelper(x.contents
				.filter(function(y) {
					return y.length;
				})
				.map(function(y) {
					return OrHelper(y.map(function(z) {
							return WordToTerm(z.text);
						}));
				}));
		}).filter(function(x) {
			return x;
		});
	
	if (state.horizontalAxis == "page") {
		query.buckets_ = array_range(1, 30).map(function(a) {
			return PageTerm(a,a+1);
		});
	} else if(state.dateGranularity == "year") {
		query.buckets_ = array_range(state.startYear, state.endYear).map(function(a) {
			return YearTerm(a);
		});
	} else if (state.dateGranularity == "month") {
		query.buckets_ = array_range(state.startYear, state.endYear).map(function(a) {
			return array_range(0,11).map(function(b) {
				return MonthTerm(a,b);
			});
		}).reduce(function(m,n) {
			return m.concat(n);
		});
	} 
	
	return query;
}


var viewModel = {};

viewModel.horizontalAxis = function() { return "date" };
viewModel.dateGranularity = function() { return "month" };
viewModel.graphStack = function() { return true; };
viewModel.graphFill = function() { return true; };
viewModel.graphMode = function() { return "bars"; };
viewModel.startYear = function() { return 2000; };
viewModel.endYear = function() { return 2010; };


viewModel.graphData = function(data) {
	if(data) {
		viewModel.graphData_ = data;
		updatePlot();
	}
	else {
		return viewModel.graphData_;
	}
};



var current_generation = 0;
function queryChanged() {

	hashIgnore = true;
	window.location.hash = encodeURIComponent(JSON.stringify(domStateToObject()));
	hashIgnore = false;

	// will get called anytime the query gets changed in any way
	// we probably want to do some rate-limiting to avoid DoSing the server with queries
    var query = queryForObject(domStateToObject());
    arbitraryQuery("/api/query/docs/bucketed",    
        query,
        function(gen,query,c,r,d){
            if(!success(c)) {
                alert("query failed to run: " + r);
                alert(JSON.stringify(query));
                return;
            }
            if(gen != current_generation)
                return;
            //TODO: maybe a better way to do this
            if(viewModel.horizontalAxis() == "page") {
                viewModel.graphData(
                    r
                    .map(function(x, x_i) {
                        
                        return {data: x.map(function(y,y_i) {
                            return [y_i + 1,y];
                        }), label: "" };
                    }));
            } 
            else if(viewModel.dateGranularity() == "year") {
				viewModel.graphData(
					r
					.map(function(x, x_i) {
						
						return {data: x.map(function(y,y_i) {
							return [new Date(viewModel.startYear()+y_i,0,0).getTime(),y];
						}), label: "" };
					}));
            } else if(viewModel.dateGranularity() == "month") {
				viewModel.graphData(
					r
					.map(function(x, x_i) {
						
						return {data: x.map(function(y,y_i) {
							return [new Date(viewModel.startYear(),y_i,0).getTime(),y];
						}), label: "" };
					}));
            }
        }.bind(this, ++current_generation, query));
}

viewModel.graphOptions = function() {
    var retval = {
		series: {
			stack: viewModel.graphStack() ? 1 : null,
			lines: { show: (viewModel.graphMode() == "lines" || viewModel.graphMode() == "steps"), fill: viewModel.graphFill(), steps: viewModel.graphMode() == "steps" },
			bars: { show: viewModel.graphMode() == "bars",
					barWidth: 0.8 }
		},
		xaxis: {},
		yaxis: {min: 0 },
    };
    
    if(viewModel.horizontalAxis() == "page") {
    	retval.xaxis.mode = null;
		retval.xaxis.min = 0;
		retval.xaxis.max = 32;
    }
    else if(viewModel.horizontalAxis() == "date") {
    	retval.xaxis.mode = "time";
		retval.xaxis.min = new Date(viewModel.startYear(), 0, 1).getTime();
		if(viewModel.graphMode() == "lines" || viewModel.graphMode() == "steps") {
			if(viewModel.dateGranularity() == "year") {
				retval.xaxis.max = new Date(viewModel.endYear(), 0, 1).getTime();
			} else if(viewModel.dateGranularity() == "month") {
				retval.xaxis.max = new Date(viewModel.endYear(), 11, 0).getTime();
			}
		}
		else {
			retval.xaxis.max = new Date(viewModel.endYear(), 11, 30).getTime();
			if(viewModel.dateGranularity() == "year") {
				retval.series.bars.barWidth = 0.8*365*86400000;	
			} else if(viewModel.dateGranularity() == "month") {
				retval.series.bars.barWidth = 0.8*28*86400000;
			}
		}
		if(viewModel.dateGranularity() == "year") {
			retval.xaxis.minTickWidth = [1, "year"];

		} else if(viewModel.dateGranularity() == "month") {
			retval.xaxis.minTickWidth = [1, "month"];
		}
    }
    return retval;
}

function updatePlot() {
	$.plot($("#plot"), viewModel.graphData(), viewModel.graphOptions());
}

function AddSeries(s) {
	$("#series").append(s);
}



$("#newseries").click(function(x) { AddSeries(Series()); return false; })
	.droppable({
		accept: function(x) {
			return x.hasClass("literal") && !x.hasClass("dropped")
				|| x.hasClass("disjunction")
		},
		activeClass: "droppable",
		hoverClass: "hover",
		drop: function(event, ui) {
			if(ui.draggable.hasClass("disjunction")) {
				var s = Series();
				AddDisjunctionToSeries(ui.draggable, s);
				AddSeries(s);
			}
			else if(ui.draggable.hasClass("literal")) {
				var s = Series();
				AddLiteralCopyToSeries(ui.draggable, s);
				AddSeries(s);
			}
		},
		greedy:true
	});

$("#palette input").keyup(function() {
	var pval = $(this).val();
	$("#palette .contents .literal").each(function() {
		SetLiteralText($(this), pval);
	});
});

l1 = Literal().text("").draggable("option","helper","clone");
$("#palette .contents").append(l1);

var hashIgnore = false;
function hashChange() {
	if(!hashIgnore && window.location.hash != "") {
		objectToDomState(JSON.parse(decodeURIComponent(window.location.hash.slice(1))));
		queryChanged();
	}
}

window.onhashchange = hashChange;
hashChange();