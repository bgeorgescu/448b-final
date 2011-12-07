var globalRevertDuration=100;

function TemplateInstance(t) {
	return $("#"+t+"_template").clone(true, true).removeAttr("id");
}

function Literal() {
	return TemplateInstance("literal").draggable({
		revert:"invalid",
		revertDuration: globalRevertDuration,
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
		helper: function() { return $(this).css("z-index",10000) },
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
			})
	}
}

function objectToDomState(obj) {
	var series_container = $("#series");
	series_container.empty();
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






s1 = Series();
s2 = Series();
s3 = Series();


$("#series").append(s1);
$("#series").append(s2);
$("#series").append(s3);


d1 = Disjunction();
s1.find(".contents").append(d1);

l1 = Literal().text("cool")//.draggable("option","helper","clone");
s3.append(l1);