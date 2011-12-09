success = function(code){
    return code >= 200 && code <= 300;
}

AUTOCOMPLETE_BASE = "/api/autocomplete/term/";
AUTOCOMPLETE_TYPE_LIST = "/api/autocomplete/types";

//error callbacks get the raw text
//success ones are preprocessed from JSON
wrappedXHRCallback = function(xhr,callback) {
    if(xhr.readyState == 4){
        xhr.stopTime = new Date();
        var ms = xhr.stopTime.getTime() - xhr.startTime.getTime();
        if(success(xhr.status)) {
            callback(xhr.status, JSON.parse(xhr.responseText), ms);
        } else {
            callback(xhr.status, xhr.responseText, ms);
        }
    }
}
buildXHR = function(method, service, callback) {
    var xhr = new XMLHttpRequest();
    xhr.startTime = new Date();
    xhr.open(method, SERVER + service, true);
    xhr.setRequestHeader("Content-type", "application/json");
    xhr.onreadystatechange = function() {wrappedXHRCallback(xhr, callback)};
    return xhr;
}

LemmaTerm = function(word) {
    return {lemma_:{lemma_:word}};
}
EntityTerm = function(entity) {
    return {entity_:{entity_:entity}};
}
YearTerm = function(year) {
    return {date_:{before_:(year+1)*10000, after_:(year*10000 - 1)}};
}
MonthTerm = function(year, month) {
    return {date_:{before_:(year)*10000+(month+2)*100, after_:(year*10000)+(month+1)*100}};
}
OrTerm = function() {
    var terms = [];
    for(var i = 0; i < arguments.length; ++i) {
        terms.push(arguments[i]);
    }
    return {or_:{terms_:terms}};
}
AndTerm = function() {
    var terms = [];
    for(var i = 0; i < arguments.length; ++i) {
        terms.push(arguments[i]);
    }
    return {and_:{terms_:terms}};
}
NotTerm = function(a) {
    return {not_:{term_:a}};
}
PageTerm = function(begin_inclusive, end_exclusive) {
    return {page_:{begin_:begin_inclusive, end_:end_exclusive}};
}
SectionTerm = function(name) {
    return {section_:{name_:name}};
}
PublicationTerm = function(id) {
    return {publication_:{publication_:id}};
}
DocLemmaTerm = function(expr) {
    return {docLemma_:{term_:expr}};
}
DocEntityTerm = function(expr) {
    return {docEntity_:{term_:expr}};
}
ThresholdTerm = function(minimum_hits) {
    return {threshold_:{min_:minimum_hits}};
}
AllDocsTerm = function() {
    return {allDocs_:{}};
}
AllLemmasTerm = function() {
    return {allLemmas_:{}};
}
AllEntitiesTerm = function() {
    return {allEntities_:{}};
}

//from /api/autocomplete/publications
var PUBLICATIONS = {"7556":"Baltimore Sun","7683":"Los Angeles Times","7684":"Chicago Tribune"};
//from /api/autocomplete/types
var AUTOCOMPLETE_TYPES = {"0":"ENTITY","1":"LEMMA","2":"SISTER","3":"PARENT","4":"CHILD","5":"SENTIMENT","6":"PAGE","7":"SECTION","8":"PUBLICATION"};

arbitraryQuery = function(endpoint, query, onResult) {
    var xhr = buildXHR("POST", endpoint, onResult);
    xhr.send(JSON.stringify(query));
}

//for now it is dynamic, but we will pull these out as constants.  this is just a nice
//method to be able to check if the server is speaking our language at that point.
listAutoCompleteTypes = function(onResult) {
    var xhr = buildXHR("GET", AUTOCOMPLETE_TYPE_LIST, onResult);
    xhr.send(null);
}
//pass in something to autocomplete, a term is required, its probably best to set a limit
autoCompleteTerm = function(term, type, limit, onResult) {
    var url = AUTOCOMPLETE_BASE + term;
    if(undefined != type) {
        url += "/type/" + type;
    }
    if(undefined != limit) {
        url += "/limit/" + limit;
    }
    var xhr = buildXHR("GET", url, onResult);
    xhr.send(null);
}
