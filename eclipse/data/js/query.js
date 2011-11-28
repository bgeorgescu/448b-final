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


//lemmas = 'a'
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

//lemmas = ['a','b']
getDocumentsForAnyLemmas = function(lemmas, onResult) {
    var xhr = buildXHR("/api/filter/docs", onResult);
    var query = {
        terms_:[
            //add CNF clauses
        ],
    };
    for(var i in lemmas) {
        query.terms_.push([ //one CNF clause
            {lemma_:{lemma_:lemmas[i]}},
        ]);
    }
    xhr.send(JSON.stringify(query));
}
//lemmas = ['a','b']
getDocumentsForAllLemmas = function(lemmas, onResult) {
    var xhr = buildXHR("/api/filter/docs", onResult);
    var query = {
        terms_:[
            [ //one CNF clause
            ],
        ],
    };
    for(var i in lemmas) {
        query.terms_[0].push({ //one CNF term
            lemma_:{
                lemma_:lemmas[i]
            },
        });
    }
    xhr.send(JSON.stringify(query));
}
//lemmas = ['a','b']
//buckets = ['c', 'd']
getHitsForAnyLemmas = function(lemmas, buckets, onResult) {
    var xhr = buildXHR("/api/tally/hits", onResult);
    var query = {
        filter_:{
            terms_:[
                //add CNF clauses
            ],
        },
        buckets_:[
            //add bucket expresions
        ],
    };
    for(var i in lemmas) {
        query.filter_.terms_.push(
            [ //one CNF clause
                {lemma_:{lemma_:lemmas[i]}},
            ]
        );
    }
    for(var i in buckets) {
        query.buckets_.push({
            terms_:[
                [ //one CNF clause
                    {lemma_:{lemma_:buckets[i]}},
                ]
            ],
        });
    }
    xhr.send(JSON.stringify(query));
}
//lemmas = ['a','b']
//buckets = [['a', 'c'], ['a','d'}, {'b','c'}, {'b', 'd'}]
getComboHitsForAnyLemmas = function(lemmas, buckets, onResult) {
    var xhr = buildXHR("/api/tally/hits", onResult);
    var query = {
        filter_:{
            terms_:[
                //add CNF clauses
            ],
        },
        buckets_:[
            //add bucket expresions
        ],
    };
    for(var i in lemmas) {
        query.filter_.terms_.push(
            [ //one CNF clause
                {lemma_:{lemma_:lemmas[i]}},
            ]
        );
    }
    for(var i in buckets) {
        var agg = {
            terms_:[
                [ //one CNF clause
                ],
            ],
        };
        for(var j in buckets[i]) {
            agg.terms_[0].push({ //one CNF term
                lemma_:{
                    lemma_:buckets[i][j]
                },
            });
        }               
        query.buckets_.push(agg);
    }
    xhr.send(JSON.stringify(query));
}
//lemmas = ['a','b']
//buckets = ['c', 'd']
getDocHitsForAnyLemmas = function(lemmas, buckets, onResult) {
    var xhr = buildXHR("/api/tally/docs", onResult);
    var query = {
        filter_:{
            terms_:[
                //add CNF clauses
            ],
        },
        buckets_:[
            //add bucket expresions
        ],
    };
    for(var i in lemmas) {
        query.filter_.terms_.push(
            [ //one CNF clause
                {lemma_:{lemma_:lemmas[i]}},
            ]
        );
    }
    for(var i in buckets) {
        query.buckets_.push({
            terms_:[
                [ //one CNF clause
                    {lemma_:{lemma_:buckets[i]}},
                ]
            ],
        });
    }
    xhr.send(JSON.stringify(query));
}
//lemmas = ['a','b']
//buckets = [['a', 'c'], ['a','d'}, {'b','c'}, {'b', 'd'}]
getComboDocHitsForAnyLemmas = function(lemmas, buckets, onResult) {
    var xhr = buildXHR("/api/tally/docs", onResult);
    var query = {
        filter_:{
            terms_:[
                //add CNF clauses
            ],
        },
        buckets_:[
            //add bucket expresions
        ],
    };
    for(var i in lemmas) {
        query.filter_.terms_.push(
            [ //one CNF clause
                {lemma_:{lemma_:lemmas[i]}},
            ]
        );
    }
    for(var i in buckets) {
        var agg = {
            terms_:[
                [ //one CNF clause
                ],
            ],
        };
        for(var j in buckets[i]) {
            agg.terms_[0].push({ //one CNF term
                lemma_:{
                    lemma_:buckets[i][j]
                },
            });
        }               
        query.buckets_.push(agg);
    }
    xhr.send(JSON.stringify(query));
}