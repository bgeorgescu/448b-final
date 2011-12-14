(function() {
  var ExtraTerm, SimilarityResult, Viz2;
  var __bind = function(fn, me){ return function(){ return fn.apply(me, arguments); }; };
  SimilarityResult = (function() {
    SimilarityResult.prototype.defaultOptions = {
      startDate: null,
      endDate: null,
      onDragend: function() {
        return null;
      },
      graphWidth: 190,
      graphHeight: 20,
      pubid: false
    };
    function SimilarityResult(parent, term, count, otherTerms, options) {
      this.term = term;
      this.count = count;
      this.otherTerms = otherTerms;
      if (options == null) {
        options = {};
      }
      this.parent = $(parent);
      this.otherTerms.push(term);
      this.options = $.extend({}, this.defaultOptions, options);
      this.scaffold();
      this.loadSparkline();
      this.elm.click(__bind(function() {
        return this.options.onClick(this);
      }, this));
    }
    SimilarityResult.prototype.scaffold = function() {
      var id, textContainer;
      this.elm = $('<li />').appendTo(this.parent);
      textContainer = $("<div />").appendTo(this.elm);
      textContainer.append($('<span/>', {
        "class": 'header',
        text: this.term
      }));
      textContainer.append($('<span/>', {
        "class": 'count',
        text: this.count
      }));
      id = "graph" + (Math.random().toString().slice(2));
      return this.graphContainer = $('<div />', {
        "class": 'minigraph',
        id: id
      }).appendTo(this.elm);
    };
    SimilarityResult.prototype.loadSparkline = function() {
      return DatabaseInterface.query({
        terms: this.otherTerms.concat([this.term]),
        callback: $.proxy(this.drawGraph, this),
        useAnd: true,
        pubid: this.options.pubid
      });
    };
    SimilarityResult.prototype.drawGraph = function(code, results, duration) {
      this.sparkline = new HighlightableSparkline(this.graphContainer[0], results[0], {
        height: this.options.graphHeight,
        width: this.options.graphWidth,
        drawTicks: false,
        drawYLabels: false
      });
      if (this.hlSpan) {
        return this.sparkline.highlight(this.hlSpan);
      }
    };
    SimilarityResult.prototype.highlight = function(hlSpan) {
      this.hlSpan = hlSpan;
      if (this.sparkline) {
        return this.sparkline.highlight(this.hlSpan);
      }
    };
    return SimilarityResult;
  })();
  ExtraTerm = (function() {
    function ExtraTerm(term, onRemoveCallback) {
      this.term = term;
      this.onRemoveCallback = onRemoveCallback;
      this.draw();
    }
    ExtraTerm.prototype.remove = function() {
      this.elm.remove();
      return this.onRemoveCallback(this);
    };
    ExtraTerm.prototype.draw = function() {
      var closeLink;
      this.elm = $('<span />', {
        "class": 'extra_term',
        text: this.term
      }).appendTo($('#js_extra_terms'));
      closeLink = $('<a />', {
        text: 'x'
      }).appendTo(this.elm);
      return closeLink.click(__bind(function(evt) {
        evt.preventDefault();
        return this.remove();
      }, this));
    };
    return ExtraTerm;
  })();
  Viz2 = {
    init: function() {
      this.terms = [];
      this.search = $('#js_search_box');
      this.search.keyup(__bind(function(event) {
        if (event.keyCode === 13) {
          return this.loadData();
        }
      }, this));
      this.loadStateFromHash();
      this.loadData();
      $('#js_filter_entities').change(__bind(function() {
        if (this.mainSparkline) {
          return this.loadRelatedData();
        }
      }, this));
      $('#js_filter_newspapers').change(__bind(function() {
        return this.loadData();
      }, this));
      $('#js_filter_entities').buttonset();
      $("#help-dialog").dialog({
        autoOpen: false,
        height: 400,
        modal: true,
        width: 600,
        show: "fade",
        hide: "fade"
      });
      return $('.help-link').click(function() {
        $("#help-dialog").css("visibility", "visible");
        $("#help-dialog").dialog("open");
        return false;
      });
    },
    loadData: function() {
      var term;
      $('.js_roller').show();
      term = $.trim(this.search.val());
      if (term === "") {
        term = null;
      }
      this.setTerm(term);
      if (this.mainSparkline) {
        this.mainSparkline.clear();
      }
      DatabaseInterface.query({
        terms: term,
        callback: $.proxy(this.drawGraph, this),
        pubid: this.getPubid(),
        useAnd: true
      });
      return this.loadRelatedData();
    },
    loadRelatedData: function() {
      $('.js_attr_list').empty();
      $('#js_related_nouns_roller').show();
      this.loadTimeSpan();
      this.pushStateToHash();
      return DatabaseInterface.similarEntities({
        terms: this.getSearchTerms(),
        useAnd: true,
        startDate: this.timeSpan.start,
        endDate: this.timeSpan.end,
        callback: $.proxy(this.setRelatedNouns, this),
        maxResults: 20,
        pubid: this.getPubid(),
        type: this.getEntityType()
      });
    },
    getSearchTerms: function() {
      return $.map(this.terms, function(termObj) {
        return termObj.term;
      });
    },
    drawGraph: function(code, results, duration) {
      $('#js_sparkline_roller').hide();
      if (this.mainSparkline) {
        return this.mainSparkline.setData(results[0]);
      } else {
        return this.mainSparkline = new EnhancedSparkline('#js_main_viz', results[0], {
          width: 700,
          height: 150,
          xOffset: 20,
          marginX: 30,
          marginY: 15,
          drawXLabels: true,
          popup: true,
          onRescale: __bind(function(dateSpan) {
            $('#js_date_start').html(DateFormatter.format(dateSpan.start));
            return $('#js_date_end').html(DateFormatter.format(dateSpan.end));
          }, this),
          onDragend: __bind(function(dateSpan) {
            return this.loadRelatedData();
          }, this)
        });
      }
    },
    loadTimeSpan: function() {
      return this.timeSpan = this.mainSparkline ? this.mainSparkline.getDateRange() : {};
    },
    getEntityType: function() {
      this.filteringEntity = $('input[name=js_filter_entities]:checked').val();
      if (this.filteringEntity === 'all') {
        return null;
      } else {
        return this.filteringEntity;
      }
    },
    setEntityType: function(type) {
      type || (type = 'all');
      return $("input[name=js_filter_entities][value=" + type + "]")[0].checked = true;
    },
    getPubid: function() {
      this.pubid = $('input[name=js_filter_newspapers]:checked').val();
      if (this.pubid === 'all') {
        return null;
      } else {
        return this.pubid;
      }
    },
    setPubid: function(value) {
      value || (value = 'all');
      return $("input[name=js_filter_newspapers][value=" + value + "]")[0].checked = true;
    },
    setRelatedNouns: function(code, results, duration) {
      $('#js_related_nouns_roller').hide();
      $('.js_nouns_list').empty();
      return this.setListValues('#js_related_nouns', results[0]['entity_'], results[0]['count_']);
    },
    addTerm: function(term) {
      var termObj;
      if (this.getSearchTerms().indexOf(term) === -1) {
        termObj = new ExtraTerm(term, __bind(function(thisTermObj) {
          this.terms.remove(thisTermObj);
          return this.loadData();
        }, this));
        return this.terms.push(termObj);
      }
    },
    setTerm: function(term) {
      this.term = term;
      this.search.val(this.term);
      return $('#js_current_term').text(this.term || 'All Articles');
    },
    setListValues: function(ul, values, counts) {
      var count, currentUl, i, sparkline, value, _len, _results;
      currentUl = $(ul);
      _results = [];
      for (i = 0, _len = values.length; i < _len; i++) {
        value = values[i];
        count = counts[i];
        if (i > 4) {
          currentUl = $(ul + '2');
        }
        if (i > 9) {
          currentUl = $(ul + '3');
        }
        if (i > 14) {
          currentUl = $(ul + '4');
        }
        sparkline = new SimilarityResult(currentUl, value, count, [], {
          onClick: __bind(function(result) {
            this.setTerm(result.term);
            return this.loadData();
          }, this),
          pubid: this.getPubid()
        });
        _results.push(sparkline.highlight(this.timeSpan));
      }
      return _results;
    },
    getState: function() {
      var state;
      state = {};
      if (this.term) {
        state.term = this.term;
      }
      if (this.getPubid()) {
        state.pubid = this.getPubid();
      }
      if (this.getEntityType()) {
        state.entityType = this.getEntityType();
      }
      if (this.timeSpan.start) {
        state.start = this.timeSpan.start.toDateString();
      }
      if (this.timeSpan.end) {
        state.end = this.timeSpan.end.toDateString();
      }
      return state;
    },
    pushStateToHash: function() {
      return window.location.hash = encodeURIComponent(JSON.stringify(this.getState()));
    },
    loadStateFromHash: function() {
      var state;
      if (window.location.hash !== "") {
        state = JSON.parse(decodeURIComponent(window.location.hash.slice(1)));
        this.setTerm(state.term);
        this.setPubid(state.pubid);
        this.setEntityType(state.entityType);
        this.timeSpan = {};
        if (state.start) {
          this.timeSpan.start = new Date(state.start);
        }
        if (state.end) {
          return this.timeSpan.end = new Date(state.end);
        }
      }
    }
  };
  window.Viz2 = Viz2;
  $(function() {
    return Viz2.init();
  });
}).call(this);
