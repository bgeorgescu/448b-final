(function() {
  var SparklinePlot;
  SparklinePlot = (function() {
    SparklinePlot.prototype.defaultOptions = {
      interpolation: 'linear',
      tension: 1,
      drawXLabels: false,
      drawYLabels: true,
      drawTicks: true,
      marginX: 0,
      marginY: 0,
      color: 'blue',
      strokeWidth: 2,
      width: 958,
      height: 100,
      xOffset: 0,
      yOffset: 0,
      startDate: new Date(2000, 0),
      endDate: new Date(2010, 11)
    };
    function SparklinePlot(container, data, options) {
      if (options == null) {
        options = {};
      }
      this.container = $(container);
      this.options = $.extend({}, this.defaultOptions, options);
      this.vis = d3.select(container).append("svg:svg").attr("width", this.options.width).attr("height", this.options.height);
      this.g = this.vis.append("svg:g").attr("transform", "translate(0, " + this.options.height + ")");
      this.setData(data);
    }
    SparklinePlot.prototype.setData = function(data) {
      var xScaleBounds, yScaleBounds;
      this.data = data;
      this.xmax = d3.max(this.data);
      this.ymax = data.length;
      yScaleBounds = [0 + this.options.marginY + this.options.yOffset, this.options.height - this.options.marginY + this.options.yOffset];
      xScaleBounds = [0 + this.options.marginX + this.options.xOffset, this.options.width - this.options.marginX + this.options.xOffset];
      this.yScale = d3.scale.linear().domain([0, this.xmax]).range(yScaleBounds);
      this.xScale = d3.scale.linear().domain([0, this.ymax]).range(xScaleBounds);
      return this.draw();
    };
    SparklinePlot.prototype.draw = function() {
      var lineFn;
      this.clearCanvas();
      lineFn = this.getLine(this.options.interpolation, this.options.tension, this.xScale, this.yScale);
      this.path = this.g.append("svg:path").attr("d", lineFn(this.data)).attr("style", "stroke: " + this.options.color + "; stroke-width: " + this.options.strokeWidth + "px;");
      this.drawGraphAxis(this.xScale, this.yScale, this.ymax, this.xmax);
      this.drawGraphLabels(this.xScale, this.yScale, this.options.yOffset);
      if (this.options.drawTicks) {
        return this.drawGraphTicks(this.xScale, this.yScale);
      }
    };
    SparklinePlot.prototype.getLine = function(interpolation, tension, xfn, yfn) {
      return d3.svg.line().x(function(d, i) {
        return xfn(i);
      }).y(function(d) {
        return -1 * yfn(d);
      }).interpolate(interpolation).tension(tension);
    };
    SparklinePlot.prototype.clear = function() {
      return this.clearCanvas();
    };
    SparklinePlot.prototype.clearCanvas = function() {
      this.g.selectAll("line").remove();
      this.g.selectAll("text").remove();
      this.g.selectAll("path").remove();
      this.g.selectAll('.xTicks').remove();
      return this.g.selectAll('.yTicks').remove();
    };
    SparklinePlot.prototype.drawGraphTicks = function(xfn, yfn) {
      this.g.selectAll(".xTicks").data(xfn.ticks(5)).enter().append("svg:line").attr("class", "xTicks").attr("x1", function(d) {
        return xfn(d);
      }).attr("y1", -1 * yfn(0)).attr("x2", function(d) {
        return xfn(d);
      }).attr("y2", -1 * yfn(-0.2));
      return this.g.selectAll(".yTicks").data(yfn.ticks(4)).enter().append("svg:line").attr("class", "yTicks").attr("y1", function(d) {
        return -1 * yfn(d);
      }).attr("x1", xfn(-0.2)).attr("y2", function(d) {
        return -1 * yfn(d);
      }).attr("x2", xfn(0));
    };
    SparklinePlot.prototype.drawGraphLabels = function(xfn, yfn, yOffset) {
      if (this.options.drawXLabels) {
        this.g.selectAll(".xLabel").data(xfn.ticks(5)).enter().append("svg:text").attr("class", "xLabel").text(String).attr("x", function(d) {
          return xfn(d);
        }).attr("y", -1 * yOffset).attr("text-anchor", "middle");
      }
      if (this.options.drawYLabels) {
        return this.g.selectAll(".yLabel").data(yfn.ticks(4)).enter().append("svg:text").attr("class", "yLabel").text(String).attr("y", function(d) {
          return -1 * yfn(d);
        }).attr("text-anchor", "right").attr("dy", 4);
      }
    };
    SparklinePlot.prototype.drawGraphAxis = function(xfn, yfn, maxX, maxY) {
      this.g.append("svg:line").attr("x1", xfn(0)).attr("y1", -1 * yfn(0)).attr("x2", xfn(maxX)).attr("y2", -1 * yfn(0));
      return this.g.append("svg:line").attr("x1", xfn(0)).attr("y1", -1 * yfn(0)).attr("x2", xfn(0)).attr("y2", -1 * yfn(maxY)).attr("y2", -1 * yfn(maxY));
    };
    SparklinePlot.prototype.dateToNumber = function(dateObj) {
      return dateObj.getFullYear() + dateObj.getMonth() / 12;
    };
    SparklinePlot.prototype.numberToDate = function(number) {
      var month, year;
      month = Math.round((number % 1) * 12);
      year = Math.floor(number);
      return new Date(year, month);
    };
    return SparklinePlot;
  })();
  window.SparklinePlot = SparklinePlot;
}).call(this);
