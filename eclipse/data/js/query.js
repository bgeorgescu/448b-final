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
//lemmas = 'a'
getDocumentsForEntity = function(entity, onResult) {
    var xhr = buildXHR("/api/filter/docs", onResult);
    var query = {
        terms_:[
            [ //one CNF clause
                EntityTerm(entity),
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
//buckets = [['a', 'c'], ['a','d'], ['b','c'], ['b', 'd']]
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

var MONTHS = ['01', '02', '03', '04', '05', '06', '07', '08', '09', '10', '11', '12'];

//buckets = [['a', 'c'], ['a','d'], ['b','c'], ['b', 'd']]
getMonthlyComboDocHits = function(buckets, onResult) {
    var xhr = buildXHR("/api/tally/docs", 
        function(code, body, duration) {
            var res = body;
            //transform the data
            if(success(code)) {
                res = {};
                for(var y = 2000; y <= 2010; ++y) {
	                for(var m = 0; m < 12; ++m) {
                        res[y + MONTHS[m]] = body.splice(0, buckets.length);
	                }
                }
            }
            onResult(code, res, duration);
        }
    );
    var query = {
        buckets_:[
            //add bucket expresions
        ],
    };
    for(var y = 2000; y <= 2010; ++y) {
        for(var m = 0; m < 12; ++m) {
            for(var i in buckets) {
                var agg = {
                    terms_:[
                        [ //one CNF clause
                            MonthTerm(y, m),
                        ],
                    ],
                };
                for(var j in buckets[i]) {
                    agg.terms_[0].push(LemmaTerm(buckets[i][j]));
                }               
                query.buckets_.push(agg);
            }
        }
    }

    xhr.send(JSON.stringify(query));
}


//buckets = [   [[Term(), Term()], [Term()], ...]  , ... ]
getMonthlyDocHits = function(buckets, onResult) {
    var xhr = buildXHR("/api/tally/docs", 
        function(code, body, duration) {
            var res = body;
            //transform the data
            if(success(code)) {
                res = {};
                for(var y = 2000; y <= 2010; ++y) {
   	                for(var m = 0; m < 12; ++m) {
                            res[y + MONTHS[m]] = body.splice(0, buckets.length);
	                }
                }
            }
            onResult(code, res, duration);
        }
    );
    var query = {
        buckets_:[
            //add bucket expresions
        ],
    };
    for(var y = 2000; y <= 2010; ++y) {
        for(var m = 0; m < 12; ++m) {
            for(var i in buckets) {
                var term_copy = JSON.parse(JSON.stringify(buckets[i]));
                var agg = {
                    terms_:term_copy,
                };
                for(var j in agg.terms_) {
                    agg.terms_[j].push(MonthTerm(y,m));
                }
                query.buckets_.push(agg);
            }
        }
    }
    xhr.send(JSON.stringify(query));
}


//lemmas = ['a','b']
//buckets = ['c', 'd']
bencmarkHitsForAnyLemmas = function(lemmas, buckets, onResult) {
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
    for(var j = 0; j < 100; ++j)
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
