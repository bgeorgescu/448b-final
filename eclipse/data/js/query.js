success = function(code){
    return code >= 200 && code <= 300;
}

FILTER_DOCS = "/api/filter/docs";
TALLY_HITS = "/api/tally/hits";
TALLY_DOCS = "/api/tally/docs";
TALLY_LEMMAS = "/api/tally/lemmas";
TALLY_ENTITIES = "/api/tally/entities";

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
buildXHR = function(service, callback) {
    var xhr = new XMLHttpRequest();
    xhr.startTime = new Date();
    xhr.open("POST", SERVER + service, true);
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
    return {date_:{before_:(year+1)*10000+(month+2)*100, after_:(year*10000 - 1)+(month+1)*100}};
}


arbitraryQuery = function(endpoint, query, onResult) {
    var xhr = buildXHR(endpoint, onResult);
    xhr.send(JSON.stringify(query));
}
