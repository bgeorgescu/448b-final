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
				ui.draggable //.css({'top':'','left':'','position':''})
					.appendTo($(this).find(".contents")[0])
			}
			else if(ui.draggable.hasClass("literal")) {
				var d = Disjunction();
				Literal()
				.text(ui.draggable.text())
				.addClass("dropped")
				.appendTo(d.find(".contents")[0]);
				d.appendTo($(this).find(".contents")[0]);
			}
		},
		greedy:true
	});
	t.draggable({
		revert:"invalid",
		handle: "sGrip",
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
			Literal()
				.text(ui.draggable.text())
				.addClass("dropped")
				.appendTo($(this).find(".contents")[0]);
		},
		greedy:true
	});
	t.draggable({
		revert:"invalid",
		handle: "dGrip",
		//z-index: 1000,
		helper: function() { return $(this).css("z-index",10000) },
		revertDuration: globalRevertDuration,
	});
	return t;
}


s1 = Series();
s2 = Series();
s3 = Series();


$("#series").append(s1);
$("#series").append(s2);
$("#series").append(s3);


d1 = Disjunction();
s1.find(".contents").append(d1);

l1 = Literal().text("cool").draggable("option","helper","clone");
s3.append(l1);