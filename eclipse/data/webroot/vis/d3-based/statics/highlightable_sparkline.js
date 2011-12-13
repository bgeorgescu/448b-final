(function() {
  var HighlightableSparkline;
  var __hasProp = Object.prototype.hasOwnProperty, __extends = function(child, parent) {
    for (var key in parent) { if (__hasProp.call(parent, key)) child[key] = parent[key]; }
    function ctor() { this.constructor = child; }
    ctor.prototype = parent.prototype;
    child.prototype = new ctor;
    child.__super__ = parent.prototype;
    return child;
  };
  HighlightableSparkline = (function() {
    __extends(HighlightableSparkline, SparklinePlot);
    function HighlightableSparkline() {
      HighlightableSparkline.__super__.constructor.apply(this, arguments);
    }
    HighlightableSparkline.prototype.highlight = function(dateRange) {
      var end, fullSpan, hlSpan, offsetLeft, spanOffsetLeft, spanWidth, start;
      start = dateRange.start || this.options.startDate;
      end = dateRange.end || this.options.endDate;
      if (this.hlBox) {
        this.hlBox.remove();
      }
      hlSpan = this.dateToNumber(end) - this.dateToNumber(start);
      fullSpan = this.dateToNumber(this.options.endDate) - this.dateToNumber(this.options.startDate);
      offsetLeft = this.dateToNumber(start) - this.dateToNumber(this.options.startDate);
      spanWidth = (hlSpan / fullSpan) * this.options.width;
      spanOffsetLeft = (offsetLeft / fullSpan) * this.options.width;
      return this.hlBox = this.vis.append('svg:rect').attr('style', "fill: #FFFF00; fill-opacity: 0.3").attr('height', this.options.height).attr('width', spanWidth).attr('class', 'hlBox').attr('x', spanOffsetLeft).attr('y', 0);
    };
    return HighlightableSparkline;
  })();
  window.HighlightableSparkline = HighlightableSparkline;
}).call(this);
