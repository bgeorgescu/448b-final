//Use this to query the backend and get back results
var DatabaseInterface = {
	
	similarEntities: function(options){
		var series = this._getSeries(options.terms);
		this._querySimilar(Query.QUERY_ENTITIES, series, options.callback, Query.DocEntityTerm, options);
	},
	
	//callbacks of the form: function(responseCode, results, duration)
	similarAdjectives: function(options){
		var series = this._getSeries(options.terms);
		options = $.extend({posPrefix: "JJ"}, options);
		this._querySimilar(Query.QUERY_LEMMAS, series, options.callback, Query.DocLemmaTerm, options);
	},
	
	//callbacks of the form: function(responseCode, results, duration)
	//this method is deprecated. use similarEntities(options) instead
	similarEntitiesOverPeriod: function(terms, startDate, endDate, callback, maxResults, options){
		this.similarEntities($.extend({
			term: terms,
			startDate: startDate,
			endDate: endDate,
			callback: callback,
			maxResults: maxResults,
		}, options || {}))
	},
	
	query: function(options){
		var buckets = this._getMonthBuckets(options.startDate, options.endDate);
		var queryParams = { buckets_: buckets }
		var series = this._getSeries(options.terms)
		if (options.useAnd && series.length > 1) {
			series = [Query.AndTerm.apply(Query, series)];
		}
		queryParams['series_'] = series
		if (options.pubid) queryParams['filter_'] = Query.PublicationTerm(options.pubid);
		Query.arbitraryQuery(Query.BUCKET_DOCS, queryParams, options.callback);
	},
	
	//callbacks of the form: function(responseCode, results, duration)
	//this method is deprecated. use query(options) instead
	queryOverPeriod: function(terms, startDate, endDate, callback, useAnd){
		this.query({
			terms: terms,
			startDate: startDate,
			endDate: endDate,
			callback: callback,
			useAnd: useAnd
		});
	},
		
	//private
	
	_querySimilar: function(url, series, callback, termFunction, options){
		options = options || {}
		var maxResults = options.maxResults || 10
		var dateSpanTerm = this._getDateSpanTerm(options.startDate, options.endDate)
		var stuffToAnd = [dateSpanTerm];
		if (options.pubid) stuffToAnd.push(Query.PublicationTerm(options.pubid))
		if (options.useAnd){
			series = [termFunction(Query.AndTerm.apply(Query, stuffToAnd.concat(series)))];
		} else {
			series = $.map(series, function(term){
				return termFunction(Query.AndTerm.apply(Query, stuffToAnd.concat([term])));
		 });
		}
		var queryParams = {
			series_: series,
			includeText_:true,
      threshold_:undefined,
      maxResults_:maxResults,
		}
		if (options.posPrefix) queryParams['posPrefix_'] = options.posPrefix;
		if (options.type) queryParams['type_'] = options.type;
		Query.arbitraryQuery(url, queryParams, callback);
	},
	
	_getDateSpanTerm: function(startDate, endDate){
		startDate = startDate || new Date(2000,0);
		endDate = endDate || new Date(2010,11);
		return Query.DateSpanTerm(startDate.getFullYear(), startDate.getMonth(), endDate.getFullYear(), endDate.getMonth());
	},
	
	_getSeries: function(terms){
		if (terms && terms.length > 0){
			return $.map(this._toArray(terms), $.proxy(function(term){ 
				return Query.OrTerm(Query.EntityTerm(term), Query.LemmaTerm(term));
			}, this));
		} else {
			return [Query.AllDocsTerm()];
		}
	},
		
	_toArray: function(obj){
		arrayedObj = obj instanceof Array ? obj : [obj]
		return arrayedObj;
	},
	
	_getMonthBuckets: function(startDate, endDate){
		startDate = startDate || new Date(2000,0);
		endDate = endDate || new Date(2010,11);
		var buckets = [];
		var dateIterator = new Date(startDate);
		while (dateIterator < endDate) {
			var year = dateIterator.getFullYear();
			var month = dateIterator.getMonth();
			buckets.push(Query.MonthTerm(year, month));
			month = month + 1;
			if (month > 11){
				month = 0;
				year += 1;
			}
			dateIterator.setFullYear(year);
			dateIterator.setMonth(month);
			dateIterator.month += 1
		}
		return buckets
	}
	
}

//Wrapping TJ's JS Query stuff in a JS object to make things cleaner
var Query = {
	
	SERVER: "http://wrldsuksgo2mars.doesntexist.org:9876",
	BUCKET_DOCS: "/api/query/docs/bucketed",
	QUERY_LEMMAS: "/api/query/lemmas",
	QUERY_ENTITIES: "/api/query/entities",
	AUTOCOMPLETE_BASE: "/api/autocomplete/term/",
	AUTOCOMPLETE_TYPE_LIST: "/api/autocomplete/types",
	
	//error callbacks get the raw text
	//success ones are preprocessed from JSON
	wrappedXHRCallback: function(xhr,callback) {
	    if(xhr.readyState == 4){
	        xhr.stopTime = new Date();
	        var ms = xhr.stopTime.getTime() - xhr.startTime.getTime();
	        if(this.success(xhr.status)) {
	            callback(xhr.status, JSON.parse(xhr.responseText), ms);
	        } else {
	            callback(xhr.status, xhr.responseText, ms);
	        }
	    }
	},
	buildXHR: function(method, service, callback) {
	    var xhr = new XMLHttpRequest();
	    xhr.startTime = new Date();
	    xhr.open(method, this.SERVER + service, true);
	    xhr.setRequestHeader("Content-type", "application/json");
	    xhr.onreadystatechange = $.proxy(function(){ this.wrappedXHRCallback(xhr, callback) }, this);
	    return xhr;
	},
	success: function(code){
	    return code >= 200 && code <= 300;
	},
	AllDocsTerm: function() {
    return {allDocs_:{}};
	},
	LemmaTerm: function(word) {
	    return {lemma_:{lemma_:word}};
	},
	EntityTerm: function(entity) {
	    return {entity_:{entity_:entity}};
	},
	YearTerm: function(year) {
	    return {date_:{before_:(year+1)*10000, after_:(year*10000 - 1)}};
	},
	MonthTerm: function(year, month) {
	    return {date_:{before_:(year)*10000+(month+2)*100, after_:(year*10000)+(month+1)*100}};
	},
	DateSpanTerm: function(year1, month1, year2, month2){
		return {date_:{before_:(year2)*10000+(month2+2)*100, after_:(year1*10000)+(month1+1)*100}};
	},
	//new not yet implemented
	OrTerm: function() {
	    var terms = [];
	    for(var i = 0; i < arguments.length; ++i) {
	        terms.push(arguments[i]);
	    }
	    return {or_:{terms_:terms}};
	},
	AndTerm: function() {
	    var terms = [];
	    for(var i = 0; i < arguments.length; ++i) {
	        terms.push(arguments[i]);
	    }
	    return {and_:{terms_:terms}};
	},
	NotTerm: function(a) {
	    return {not_:{term_:a}};
	},
	PageTerm: function(begin_inclusive, end_exclusive) {
	    return {page_:{begin_:begin_inclusive, end_:end_exclusive}};
	},
	SectionTerm: function(name) {
	    return {section_:{name_:name}};
	},
	PublicationTerm: function(id) {
	    return {publication_:{publication_:id}};
	},
	DocLemmaTerm: function(expr) {
	    return {docLemma_:{term_:expr}};
	},
	DocEntityTerm: function(expr) {
	    return {docEntity_:{term_:expr}};
	},
	ThresholdTerm: function(minimum_hits) {
	    return {threshold_:{min_:minimum_hits}};
	},
	
	arbitraryQuery: function(endpoint, query, onResult) {
	    var xhr = this.buildXHR("POST", endpoint, onResult);
	    xhr.send(JSON.stringify(query));
	},
	//for now it is dynamic, but we will pull these out as constants. this is just a nice
	//method to be able to check if the server is speaking our language at that point.
	listAutoCompleteTypes: function(onResult) {
	    var xhr = this.buildXHR("GET", AUTOCOMPLETE_TYPE_LIST, onResult);
	    xhr.send(null);
	},
	//pass in something to autocomplete, a term is required, its probably best to set a limit
	autoCompleteTerm: function(term, type, limit, onResult) {
	    var url = this.AUTOCOMPLETE_BASE + term;
	    if(undefined != type) {
	        url += "/type/" + type;
	    }
	    if(undefined != limit) {
	        url += "/limit/" + limit;
	    }
	    var xhr = this.buildXHR("GET", url, onResult);
	    xhr.send(null);
	}
}