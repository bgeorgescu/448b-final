var SERVER = "http://localhost:8080";

function success(code) {
    return code >= 200 && code <= 300;
}

//error callbacks get the raw text
//success ones are preprocessed from JSON
function wrappedXHRCallback(xhr,callback) {
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
function buildXHR(service, callback) {
    var xhr = new XMLHttpRequest();
    xhr.startTime = new Date();
    xhr.open("POST", SERVER + service, true);
    xhr.setRequestHeader("Content-type", "application/json");
    xhr.onreadystatechange = function() {wrappedXHRCallback(xhr, callback)};
    return xhr;
}


//exported
getDocumentsForLemmas = function(lemma, onResult) {
    var xhr = buildXHR("/api/filter/docs", onResult);
    var query = {
        terms_:[
            [ //one CNF clause
                {lemma_:{lemma_:lemma}},
            ],
        ],
    };
    xhr.send(JSON.stringify(query));
}

getDocumentsForMultipleLemmas = function(lemmas, onResult) {
    var xhr = buildXHR("/api/filter/docs", onResult);
    var query = {
        terms_:[
            //add CNF clauses
        ],
    };
    for(var lemma in lemmas) {
        query.terms_.push(
            [ //one CNF clause
                {lemma_:{lemma_:lemma}},
            ]);
    }
    xhr.send(JSON.stringify(query));
}