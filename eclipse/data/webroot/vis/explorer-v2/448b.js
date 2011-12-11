var globalRevertDuration=100;

var literalTypes = ["entity","lemma","page","publication","sentiment"];


var rangeSlider= $("#daterange").rangeSlider({
  /*defaultValues:{min:2000, max:2010},*/
  bounds:{min:2000, max:2010},
  wheelMode: null,
  wheelSpeed: 8,
  arrows: false,
  valueLabels: "show",
  formatter: function(value){return Math.round(value)},
  durationIn: 0,
  durationOut: 400,
  delayOut: 200,
  range: {min: false, max: false}
});
rangeSlider.bind("valuesChanged", function(event, ui){
	queryChanged();
});




var viewModel = {
	_horizontalAxis: "month",
	_graphStack: true,
	_graphMode: "bars",
	_startYear: 2000,
	_endYear: 2010
};


viewModel.horizontalAxis = function(data) {
	if(data) {
		if(data == "date") data = "month";
		$("#graphAxis .graphOption").removeClass("selected");
		$("#"+data).addClass("selected");
		queryChanged();
	}
	else {
		return $("#graphAxis .selected").first().attr("id");
	}
};


$("#stacked").click(function() { viewModel.graphStack(!$(this).hasClass("selected")); });
$("#bars,#lines,#areas").click(function() { viewModel.graphMode($(this).attr("id")); });
$("#month,#year,#page").click(function() { viewModel.horizontalAxis($(this).attr("id")); });



viewModel.graphStack = function(data) {
	if(typeof data != "undefined") {
		if(data) {
			$("#stacked").addClass("selected");
		} else {
			$("#stacked").removeClass("selected");
		}
		queryChanged();
	}
	else {
		return $("#stacked").hasClass("selected");
	}
};

viewModel.graphMode = function(data) {
	if(data) {
		$("#graphType .graphOption").removeClass("selected");
		$("#"+data).addClass("selected");
		queryChanged();
	}
	else {
		return $("#graphType .selected").first().attr("id");
	}
};
viewModel.startYear = function(data) {
	if(data) {
		rangeSlider.rangeSlider("min",data);
	}
	else {
		return 	Math.round(rangeSlider.rangeSlider("min"));
	}
};
viewModel.endYear = function(data) {
	if(data) {
		rangeSlider.rangeSlider("max",data);
	}
	else {
		return 	Math.round(rangeSlider.rangeSlider("max"));
	}
};
viewModel._graphData = function(data) {
	if(data) {
		viewModel.__graphData = data;
		updatePlot();
	}
	else {
		return viewModel.__graphData;
	}
};

viewModelContents = [];
for(i in viewModel) {
	if(i[0]!='_')
		viewModelContents.push(i);
}

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
	$.each(literalTypes, function(i, lt) {
		literal.removeClass(lt);
	});
	literal.addClass(type);
	
}

function GetLiteralType(literal) {
	var type;
	$.each(literalTypes, function(i, lt) {
		if(literal.hasClass(lt))
			type=lt;
	});
	return type;
}

function GetLiteralText(literal) {
	return literal.find("span").text();
}

function SetLiteralText(literal, text) {
	literal.find("span").text(text);
}


function AddDisjunctionToSeries(disjunction, series) {
	disjunction.appendTo(series.find(".contents")[0]);
	queryChanged();
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
	queryChanged();
}

function AddLiteralToDisjunction(literal, disjunction) {
	literal
		.addClass("dropped")
		.appendTo(disjunction.find(".contents")[0]);
	queryChanged();
}

function AddLiteralCopyToSeries(literal, series) {
	var d = Disjunction();
	AddLiteralCopyToDisjunction(literal, d);
	AddDisjunctionToSeries(d, series);
}

function AddLiteralToSeries(literal, series) {
	var d = Disjunction();
	AddLiteralToDisjunction(literal, d);
	AddDisjunctionToSeries(d, series);
}

function Series() {
	var t = TemplateInstance("series");
	t.droppable({
		accept: function(x) {
			return x.hasClass("literal") 
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
				if(ui.draggable.hasClass("palette"))
					AddLiteralCopyToSeries(ui.draggable, $(this));
				else
					AddLiteralToSeries(ui.draggable, $(this));
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
		accept: function(x) { return x.hasClass("literal")},
		activeClass: "droppable",
		hoverClass: "hover",
		drop: function(event, ui) {
			if(ui.draggable.hasClass("palette"))
				AddLiteralCopyToDisjunction(ui.draggable, $(this));
			else
				AddLiteralToDisjunction(ui.draggable, $(this));
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
		queryChanged();
	},
	activeClass:"droppable",
	hoverClass: "hover",
	tolerance: "pointer",
	greedy:true,
})

function seriesCount() {
    var count = $(".series:not(#series_template) > .contents").length;
    //console.log(count);
    if(count <= 0)
        return 1;
    return count;
}
function domStateToObject() {
	var retval = {
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
	};
	
	$.each(viewModelContents, function(i,c) {
		retval[c] = viewModel[c]();
	});	
	
	return retval;
}

function ClearSeries() {
	var series_container = $("#series");
	series_container.empty();
	//series_container.html("&nbsp;");
	queryChanged();
}

function objectToDomState(obj) {
	ignoreQueryChange = true;
	var series_container = $("#series");
	ClearSeries();
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
	
	$.each(viewModelContents, function(i,c) {
		if(typeof obj[c] != "undefined") {
			viewModel[c](obj[c]);
		}
	});	
	ignoreQueryChange = false;
	queryChanged();
}

function array_range(x,y,step) {
	var retval = [];
	if(!step) step = 1;
	for(var i = x; i <= y; i+=step)
		retval.push(i);
	return retval;
}

function GetSeriesLabels() {
	return $(".series").filter(function(x) { return $(this).find(".literal").length; }).map(function() { return GetLiteralText($(this).find('.literal').first())});
}

var pubMapping = {};
for(i in PUBLICATIONS)
	pubMapping[PUBLICATIONS[i]] = parseFloat(i);
	
function literalObjectToQueryTerm(obj) {
	WordToTerm = function(a) { return OrTerm(LemmaTerm(a), EntityTerm(a)); };
	if(obj.type == "entity")
		return EntityTerm(obj.text);
	else if(obj.type == "lemma")
		return LemmaTerm(obj.text);
	else if(obj.type == "page")
		return PageTerm(parseInt(obj.text), parseInt(obj.text)+1);
	else if(obj.type == "publication")
		return PublicationTerm(pubMapping[obj.text]);
	else if(obj.type == "sentiment")
		return SentimentTerm(obj.text);	
	else
		return WordToTerm(obj.text);
}

function queryForObject(state) {
	var query = {};
	var AndHelper = function(arr) { return arr.length > 1 ? {and_:{terms_: arr }} : arr[0]; };
	var OrHelper = function(arr) { return arr.length > 1 ? {or_:{terms_: arr }} : arr[0]; };

	
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
					return OrHelper(y.map(literalObjectToQueryTerm));
				}));
		}).filter(function(x) {
			return x;
		});
	
	if (state.horizontalAxis == "page") {
		query.buckets_ = array_range(1, 30).map(function(a) {
			return PageTerm(a,a+1);
		});
	} else if(state.horizontalAxis == "year") {
		query.buckets_ = array_range(state.startYear, state.endYear).map(function(a) {
			return YearTerm(a);
		});
	} else if (state.horizontalAxis == "month") {
		query.buckets_ = array_range(state.startYear, state.endYear).map(function(a) {
			return array_range(0,11).map(function(b) {
				return MonthTerm(a,b);
			});
		}).reduce(function(m,n) {
			return m.concat(n);
		});
	} 
	
	if(query.series_.length == 0) {
		query.series_.push(AllDocsTerm());
	}
	
	return query;
}



ignoreQueryChange = false;
var current_generation = 0;
function queryChanged() {
	if(ignoreQueryChange)
		return;

	hashIgnore = true;
	window.location.hash = encodeURIComponent(JSON.stringify(domStateToObject()));
	hashIgnore = false;

	// will get called anytime the query gets changed in any way
	// we probably want to do some rate-limiting to avoid DoSing the server with queries
    var query = queryForObject(domStateToObject());
    if(query.series_.length == 0)
    	return;
    
    arbitraryQuery("/api/query/docs/bucketed",    
        query,
        function(gen,query,c,r,d){
            if(!success(c)) {
                //alert("query failed to run: " + r);
                //alert(JSON.stringify(query));
                return;
            }
            if(gen != current_generation)
                return;
            var labels=GetSeriesLabels();
            //TODO: maybe a better way to do this
            if(viewModel.horizontalAxis() == "page") {
                viewModel._graphData(
                    r
                    .map(function(x, x_i) {
                        
                        return {data: x.map(function(y,y_i) {
                            return [y_i + 1,y];
                        }), label: labels[x_i] };
                    }));
            } 
            else if(viewModel.horizontalAxis() == "year") {
				viewModel._graphData(
					r
					.map(function(x, x_i) {
						
						return {data: x.map(function(y,y_i) {
							return [new Date(viewModel.startYear()+y_i,0,0).getTime(),y];
						}), label: labels[x_i]  };
					}));
            } else if(viewModel.horizontalAxis() == "month") {
				viewModel._graphData(
					r
					.map(function(x, x_i) {
						
						return {data: x.map(function(y,y_i) {
							return [new Date(viewModel.startYear(),y_i,0).getTime(),y];
						}), label: labels[x_i]  };
					}));
            }
        }.bind(this, ++current_generation, query));
}

viewModel._graphOptions = function() {
    var retval = {
		series: {
			stack: viewModel.graphStack() ? 1 : null,
			lines: { show: (viewModel.graphMode() == "lines" || viewModel.graphMode() == "areas"), fill: viewModel.graphMode() == "areas", steps: viewModel.graphMode() == "steps" },
			bars: { show: viewModel.graphMode() == "bars",
					barWidth: 0.8}
		},
		xaxis: {},
		yaxis: {min: 0 },
    };
    
    if(viewModel.horizontalAxis() == "page") {
    	retval.xaxis.mode = null;
		retval.xaxis.min = 0;
		retval.xaxis.max = 32;
        retval.series.bars.barWidth = 0.5;
    }
    else if(viewModel.horizontalAxis() == "year" || viewModel.horizontalAxis() == "month") {
    	retval.xaxis.mode = "time";
		if(viewModel.graphMode() == "lines" || viewModel.graphMode() == "steps" || viewModel.graphMode() == "areas" ) {
            retval.xaxis.min = new Date(viewModel.startYear(), 0, 1).getTime();
			if(viewModel.horizontalAxis() == "year") {
				retval.xaxis.max = new Date(viewModel.endYear(), 0, 1).getTime();
			} else if(viewModel.horizontalAxis() == "month") {
				retval.xaxis.max = new Date(viewModel.endYear(), 11, 0).getTime();
			}
		}
		else {
            retval.xaxis.min = new Date(viewModel.startYear(), 0, 1).getTime();
			retval.xaxis.max = new Date(viewModel.endYear(), 11, 30).getTime();
			if(viewModel.horizontalAxis() == "year") {
                if(!viewModel.graphStack()) {
                    retval.xaxis.min -= 365*86400000 / 2;
                    retval.xaxis.max -= 365*86400000 / 2;
                }
				retval.series.bars.barWidth = 0.8*365*86400000;	
			} else if(viewModel.horizontalAxis() == "month") {
                if(!viewModel.graphStack()) {
                    retval.xaxis.min -= 28*86400000 / 2;
                    retval.xaxis.max -= 28*86400000 / 2;
                }
				retval.series.bars.barWidth = 0.8*28*86400000;
			}
		}
		if(viewModel.horizontalAxis() == "year") {
			retval.xaxis.minTickWidth = [1, "year"];

		} else if(viewModel.horizontalAxis() == "month") {
			retval.xaxis.minTickWidth = [1, "month"];
		}
    }
    if(!viewModel.graphStack()) {
        retval.series.bars.barWidth /= seriesCount();
        retval.series.bars.order = 1;
    }
    return retval;
}

function updatePlot() {
	if(!ignoreQueryChange) {
		$.plot($("#plot"), viewModel._graphData(), viewModel._graphOptions());
	}
}

function AddSeries(s) {
	$("#series").append(s);
	queryChanged();
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
	
$("#clearseries").click(function(x) { ClearSeries(); return false; })


var autocomplete_gen = 0;
$("#palette input").keyup(function() {
	var pval = $(this).val();
	if(pval=="") {
		$("#palette .contents .literal").show();
		$("#suggestions").empty();
		++autocomplete_gen;
	}
	else if(isNaN(pval)) {
		$("#palette .contents .literal").show();
		$("#palette .contents .literal.page").hide();
		if(pval.length>1)
			autoCompleteTerm(pval, undefined,  8, populateAutocomplete.bind(undefined, ++autocomplete_gen));
	} else {
		$("#palette .contents .literal").hide();
		$("#palette .contents .literal.page").show();
		$("#suggestions").empty();
		++autocomplete_gen;
	}
	$("#palette .contents .literal").each(function() {
		SetLiteralText($(this), pval);
	});
});

var hashIgnore = false;
function hashChange() {
	if(hashIgnore)
		return;
	if(window.location.hash != "") {
		objectToDomState(JSON.parse(decodeURIComponent(window.location.hash.slice(1))));
	}
	queryChanged();
}

function PaletteLiteral(type, text) {
	var l1 = Literal().draggable("option","helper","clone");
	if(text) SetLiteralText(l1, text);
	if(type) SetLiteralType(l1, type);
	l1.addClass("palette");
	return l1;
}


//$("#palette .contents").append(PaletteLiteral("entity"));
//$("#palette .contents").append(PaletteLiteral("lemma"));
$("#palette .contents").append(PaletteLiteral());
$("#palette .contents").append(PaletteLiteral("page"));


for(i in pubMapping) {
	$("#pubs").append(PaletteLiteral("publication",i));
}

function populateAutocomplete(gen, c, data) {
	if(gen == autocomplete_gen && success(c)) {
		$("#suggestions").empty();
		var lemma_dedup = {};
		$.each(data, function(i,suggestion) {
			var resolved = suggestion.resolved_.split("/");
			if(suggestion.type_ == "LEMMA" && lemma_dedup[resolved[0]])
				return;
			lemma_dedup[resolved[0]]=true;
			var literal = PaletteLiteral(suggestion.type_.toLowerCase(), resolved[0]);
			if(resolved.length > 1) {
				literal.addClass(resolved[1]);
			}
			$("#suggestions").append(literal);
		});
	}
}

window.onhashchange = hashChange;
hashChange();

