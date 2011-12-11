//modifies in place
function categorize(achit) {
    if(achit.type_ == 'ENTITY') {
        var i = achit.resolved_.indexOf('/');
        //its definitely more usable with only two categories (lemma/entity)
        //looking at the two for your result requires a different
        //search strategy so splitting entities is confusing because the tagging
        //is not so good
        if(false && i >= 0) {
            achit.category_ = achit.resolved_.substring(i + 1);
        } else {
            achit.category_ = achit.type_;
        }
    } else {
        achit.category_ = achit.type_;
    }
    return achit;
}
function scoreSort(a,b) {
    if(a.score_ < b.score_) {
        return 1; 
    } else if(a.score_ > b.score_) {
        return -1;
    } else {
        return 0;
    }
}
function alphabeticalSort(a,b) {
    if(a.resolved_ < b.resolved_) {
        return -1; 
    } else if(a.resolved_ > b.resolved_) {
        return 1;
    } else {
        return 0;
    }
}
function categorizedSort(a,b) {
    if(a.category_ < b.category_) {
        return -1; 
    } else if(a.category_ > b.category_) {
        return 1;
    } else {
        return scoreSort(a,b);
    }
}
function typeDependentSort(a,b) {
    if(a.category_ < b.category_) {
        return -1; 
    } else if(a.category_ > b.category_) {
        return 1;
    } else {
        if(a.category_ != 'ENTITY')
            return alphabeticalSort(a,b);
        else
            return scoreSort(a,b);
    }
}
function atBeginningOfTerm(s) {
    return /(?:^|[,+\(\)\s])\s*$/.test(s);
}
function getOneTerm(before,after) {
    var a = getTokens(before);
    var b = getTokens(after);
    if(a.length > 0 && b.length > 0) {
        var a_last = a[a.length - 1];
        var b_first = b[0];
        if(word_expr.test(a_last) && word_expr.test(b_first)) {
            a.pop();
            b.shift();
            a.push(a_last + b_first);
        }
    }
    for(var i = a.length - 1; i >= 0; --i) {
        if(ok_word_expr.test(a[i]))
            return a[i];
    }
    return undefined;
}
function getTokens(text) {
    var tokenize = /(?:,|\+|\(|\)|\s+|(?:[a-zA-Z0-9\.]+(?:\s+[a-zA-Z0-9\.]+)*))/g;
    var toks = text.match(tokenize);
    if(toks == null)
        return [];
    return toks;
}
var word_expr = /[^,+\(\)\s]+/;
function getAllTokens(before,after) {
    var a = getTokens(before);
    var b = getTokens(after);
    if(a.length > 0 && b.length > 0) {
        var a_last = a[a.length - 1];
        var b_first = b[0];
        if(word_expr.test(a_last) && word_expr.test(b_first)) {
            a.pop();
            b.shift();
            a.push(a_last + b_first);
        }
    }
    a.push.apply(a, b);
    return a;
}
var ok_or_expr = /[^+\(\)]+/;
var ok_and_expr = /[^,\(\)]+/;
var ok_word_expr = /[^,+\(\)]+/;
var ok_word_expr = /[^,+\(\)]+/;
function extractOrList(before,after) {
    var a = getTokens(before);
    var b = getTokens(after);
    if(a.length > 0 && b.length > 0) {
        var a_last = a[a.length - 1];
        var b_first = b[0];
        if(word_expr.test(a_last) && word_expr.test(b_first)) {
            a.pop();
            b.shift();
            a.push(a_last + b_first);
        }
    }

    var start_or = 0;
    var start_word = undefined;
    for(var i = a.length - 1; i >= 0; --i) {
        if(undefined === start_word && !ok_word_expr.test(a[i])) {
            start_word = i + 1;
        }
        if(!ok_or_expr.test(a[i])) {
            if(a[i] == '(') {
                start_or = i + 1;
            } else {
                start_or = undefined;
            }
            break;
        }
    }
    if(undefined === start_word) {
        start_word = 0;
    }
    var end_word = undefined;
    var end_or = b.length;
    for(var i = 0; i < b.length; ++i) {
        if(undefined === end_word && !ok_word_expr.test(b[i])) {
            end_word = i;
        }
        if(!ok_or_expr.test(b[i])) {
            if(b[i] == ')') {
                end_or = i;
            } else {
                end_or = undefined;
            }
            break;
        }
    }
    if(undefined === end_word) {
        end_word = 0;
    }
    var txt = "";
    if(undefined === start_or || undefined === end_or) {
        var front = "".concat.apply("", a.slice(start_word));
        txt = front.concat.apply(front, b.slice(0, end_word));
    } else {
        var front = "".concat.apply("", a.slice(start_or));
        txt = front.concat.apply(front, b.slice(0, end_or));
    }
    var terms = txt.replace(/\s+/g, " ").split(',');
    terms = terms.filter(function(x) { return x.length > 0; });
    for(var i = 0; i < terms.length; ++i) {
        terms[i] = terms[i].replace(/(?:^\s*)|(?:\s*$)/g, "");
    }
    return terms;
}
function replaceOrList(before,after,replacement_ors, pos_token) {
    var a = getTokens(before);
    var b = getTokens(after);
    if(a.length > 0 && b.length > 0) {
        var a_last = a[a.length - 1];
        var b_first = b[0];
        if(word_expr.test(a_last) && word_expr.test(b_first)) {
            a.pop();
            b.shift();
            a.push(a_last + b_first);
        }
    }
    var start_or = 0;
    var start_word = undefined;
    for(var i = a.length - 1; i >= 0; --i) {
        if(undefined === start_word && !ok_word_expr.test(a[i])) {
            start_word = i + 1;
        }
        if(!ok_or_expr.test(a[i])) {
            if(a[i] == '(') {
                start_or = i + 1;
            } else {
                start_or = undefined;
            }
            break;
        }
    }
    if(undefined === start_word) {
        start_word = 0;
    }
    var end_word = undefined;
    var end_or = b.length;
    for(var i = 0; i < b.length; ++i) {
        if(undefined === end_word && !ok_word_expr.test(b[i])) {
            end_word = i;
        }
        if(!ok_or_expr.test(b[i])) {
            if(b[i] == ')') {
                end_or = i;
            } else {
                end_or = undefined;
            }
            break;
        }
    }
    if(undefined === end_word) {
        end_word = 0;
    }
    var replacement = "";
    for(var i = 0; i < replacement_ors.length; ++i) {
        if(i != 0) replacement += ",";
        replacement += replacement_ors[i];
    }
    var txt = "";
    if(undefined === start_or || undefined === end_or) {
        var front = "".concat.apply("", a.slice(0, start_word));
        //wrap an expanded word on completion
        if(replacement.indexOf(',') != -1) {
            front = front.concat("(");
            replacement = replacement.concat(")");
        }
        front = front.concat(replacement);
        front = front.concat(pos_token);
        txt = front.concat.apply(front, b.slice(end_word));
    } else {
        var front = "".concat.apply("", a.slice(0, start_or));
        //wrap the top level thing in parens on completion
        if(replacement.indexOf(',') != -1 && start_or == 0) {
            front = front.concat("(");
            replacement = replacement.concat(")");
        }
        front = front.concat(replacement);
        front = front.concat(pos_token);
        txt = front.concat.apply(front, b.slice(end_or));
    }
    var terms = txt.replace(/\s+/g, " ").split(',');
    for(var i = 0; i < terms.length; ++i) {
        terms[i] = terms[i].replace(/(?:^\s*)|(?:\s*$)/g, "");
    }
    return terms;
}
function handleTerminal(x) {
    //its possible we already converted it
    if(typeof x != "string")
        return x;
    return OrTerm(EntityTerm(x), LemmaTerm(x));
}
function processTerms(terms) {
    if(terms.length == 0)
        throw "shouldn't end up with no terms left";
    
    var type = undefined;
    var parts = [];
    while(terms.length > 0) {
        var term = terms.shift();
        if(typeof term != "string") {
            //we may be passing a parenthesized expression into another operator
            parts.push(term);
        } else if(term == '(') {
            //recurse for parens
            parts.push(processTerms(terms));
        } else if(term == ')') {
            //pop out of parens
            break;
        } else if(ok_word_expr.test(term)) {
            //plain word, just add it
            parts.push(term);
        } else if(term == ',' || term == '+') {
            if(undefined === type) {
                type = term;
            } else if(type != term) {
                if(type == ',') {
                    //must be an and, recurse
                    terms.unshift(term);
                    if(parts.length == 0)
                        throw "and without preceding term";
                    terms.unshift(parts.pop());
                    parts.push(processTerms(terms));
                } else {
                    //must be an or, scruntch what we have
                    terms.unshift(term);
                    parts = [AndTerm.apply(undefined, parts.map(handleTerminal))];
                    type = undefined;
                }
            }
        } else {
            throw "unknown term '" + term + "'";
        }
    }
    console.log(parts);
    if(undefined === type) {
        if(parts.length == 1) {
            return handleTerminal(parts[0]);
        }
        throw "must have an or/and to combine terms";
    }
    if(type == ',') {
        return OrTerm.apply(undefined, parts.map(handleTerminal));
    } else {
        return AndTerm.apply(undefined, parts.map(handleTerminal));
    }
}

function buildExpression(text) {
    if(/^\s*$/.test(text)) {
        return AllDocsTerm();
    }
    text = text.replace(/\s+/g, " ");
    var terms = getTokens(text);
    terms = terms.filter(function(x) { return !/^\s*$/.test(x); });
    var q = processTerms(terms);
    //var q = EntityTerm('obama');
    
    console.log(q);
    return q;
}
function autocompleteUpdate(ors, callback, code, response, duration) {
    $("#duration").text(duration + "ms");
    if(!success(code)) {
        alert('query failed\n' + response);
        callback([]);
        return;
    }
    var top = response.slice(0,50);
    top.map(categorize);
    //top.sort(categorizedSort);
    top.sort(typeDependentSort);
    //TODO, remove duplicate words/ones already added
    //TODO, remove base term if it isn't somewhere in the autocomplete
    var hits = top.map(function(x) { 
        var id = Math.uuid();
        var checked = "";
        var word = x.resolved_.substring(0, x.resolved_.indexOf('/'));
        return {
            label:"<input class='autocomplete-check'  id='" + id + "' alt='" + word + "' type='checkbox' " + checked + ">" +
                "<label for='" + id + "'>" + 
                word + " (" + x.score_ + ")" +
                "</label>",
            value:x.resolved_.substring(0, x.resolved_.indexOf('/')),
            category:x.category_,
        };
    });
    for(var i = ors.length - 1; i >= 0; --i) {
        var id = Math.uuid();
        var checked = "checked";
        hits.unshift({
            label:"<input class='autocomplete-check' id='" + id + "' alt='" + ors[i] + "' type='checkbox' " + checked + ">" +
                "<label for='" + id + "'>" + 
                ors[i] +
                "</label>",
            value:ors[i],
            category:"Already Added",
        });
    }                    
    callback(hits);
}

function createAutoComplete(jqs, onQueryChanged, start_at) {
    jqs.autocomplete(
        {
            source:function(req, resp) {
                var caret = jqs.caret();
                var before = req.term.substring(0, caret.start);
                var after = req.term.substring(caret.start);
                var ors = extractOrList(before, after);
                var term = getOneTerm(before, after);
                if(undefined === term) {
                    resp([]);
                    return;
                }
                var xhr = buildXHR("GET", "/api/autocomplete/term/" + encodeURIComponent(term), autocompleteUpdate.bind(undefined, ors, resp));
                xhr.send(null);                
            }.bind(jqs),
            appendTo:$("#term-completes"),
            html:true,
            select:function (event, ui) {
                return false;
            },
            focus:function (event, ui) {
                return false;
            },
            commit:function(term) {
                var caret = jqs.caret();
                console.log("commit");
                var before = term.substring(0, caret.start);
                var after = term.substring(caret.start);
                //var old_terms = extractOrList(before, after);
                //old_terms.push("%&%");
                //var new_terms = replaceOrList(before, after, old_terms);
                console.log({before:before, after:after});
                console.log(getAllTokens(before, after));
                console.log(extractOrList(before, after));
                var selected = [];
                var check = $("#term-completes .autocomplete-check");
                for(var i = 0; i < check.length; ++i) {
                    var a_check = $(check[i]);
                    if(a_check.next().hasClass("ui-state-active")) {
                        selected.push(a_check.attr("alt"));
                    }
                }
                var new_terms = replaceOrList(before, after, selected, "%&%");
                var replacement = "";   
                for(var i = 0; i < new_terms.length; ++i) {
                    if(i != 0) replacement += ",";
                    replacement += new_terms[i];
                }
                var cursor = replacement.indexOf("%&%");
                replacement = replacement.replace("%&%", "");
                jqs.attr("value", replacement);
                jqs.caret({start:cursor, end:cursor});
                onQueryChanged && onQueryChanged(replacement);
            },
            revert:function() {
                console.log("revert");
            },
        }
    );
    // jqs.bind( "autocompleteclose", function(event, ui) {
        // try {
            // history.replaceState(undefined, 'NewsWiz', (window.location +"").split('#')[0] + '#' + encodeURIComponent(jqs.attr("value")));
        // } catch(err) {
        // }
    // });
    if(start_at)
        jqs.attr("value", start_at);
};