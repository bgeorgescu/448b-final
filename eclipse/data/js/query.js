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

function LemmaTerm(word) {
    return {lemma_:{lemma_:word}};
}

//lemmas = 'a'
getDocumentsForLemmas = function(lemma, onResult) {
    var xhr = buildXHR("/api/filter/docs", onResult);
    var query = {
        terms_:[
            [ //one CNF clause
                LemmaTerm(lemma),
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
        query.terms_.push(
            [ //one CNF clause
                LemmaTerm(lemmas[i]),
            ]
        );
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
        query.terms_[0].push(LemmaTerm(lemmas[i]));
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
                LemmaTerm(lemmas[i]),
            ]
        );
    }
    for(var i in buckets) {
        query.buckets_.push({
            terms_:[
                [ //one CNF clause
                    LemmaTerm(buckets[i]),
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
                LemmaTerm(lemmas[i]),
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
            agg.terms_[0].push(LemmaTerm(buckets[i][j]));
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
                LemmaTerm(lemmas[i]),
            ]
        );
    }
    for(var i in buckets) {
        query.buckets_.push(
            {
                terms_:[
                    [ //one CNF clause
                        LemmaTerm(buckets[i]),
                    ]
                ],
            }
        );
    }
    xhr.send(JSON.stringify(query));
}
function YearTerm(year) {
    return {date_:{before_:(year+1)*10000, after_:(year*10000 - 1)}};
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
                LemmaTerm(lemmas[i]),
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
            agg.terms_[0].push(LemmaTerm(buckets[i][j]));
        }               
        query.buckets_.push(agg);
    }
    xhr.send(JSON.stringify(query));
}
//lemmas = ['a','b']
//buckets = [['a', 'c'], ['a','d'}, {'b','c'}, {'b', 'd'}]
getYearlyComboDocHitsForAnyLemmas = function(lemmas, buckets, onResult) {
    var xhr = buildXHR("/api/tally/docs", 
        function(code, body, duration) {
            var res = body;
            //transform the data
            if(success(code)) {
                res = {};
                for(var y = 2000; y <= 2010; ++y) {
                    res[y+""] = body.splice(0, buckets.length)
                }
            }
            onResult(code, res, duration);
        }
    );
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
                LemmaTerm(lemmas[i]),
            ]
        );
    }
    for(var y = 2000; y <= 2010; ++y) {
        for(var i in buckets) {
            var agg = {
                terms_:[
                    [ //one CNF clause
                        YearTerm(y),
                    ],
                ],
            };
            for(var j in buckets[i]) {
                agg.terms_[0].push(LemmaTerm(buckets[i][j]));
            }               
            query.buckets_.push(agg);
        }
    }
    xhr.send(JSON.stringify(query));
}
