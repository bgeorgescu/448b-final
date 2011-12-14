(function() {
  var SparklinePlot;
  var __bind = function(fn, me){ return function(){ return fn.apply(me, arguments); }; };
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
      popup: false,
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
      this.container.mousemove(__bind(function(evt) {
        if (this.options.popup) {
          return this.handleMouseover(evt);
        }
      }, this));
      $('body').mouseover(__bind(function(evt) {
        this.container.removeClass('hl_path');
        return PopupBox.hide();
      }, this));
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
      this.xinv = d3.scale.linear().domain(xScaleBounds).range([0, this.ymax]);
      return this.draw();
    };
    SparklinePlot.prototype.draw = function() {
      var lineFn;
      this.clearCanvas();
      lineFn = this.getLine(this.options.interpolation, this.options.tension, this.xScale, this.yScale);
      this.path = this.g.append("svg:path").attr("d", lineFn(this.data)).attr("style", "stroke: " + this.options.color + "; stroke-width: " + this.options.strokeWidth + "px;");
      this.drawGraphAxis(this.xScale, this.yScale, this.ymax, this.xmax);
      this.drawGraphLabels(this.yScale, this.options.yOffset);
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
    SparklinePlot.prototype.drawGraphLabels = function(yfn, yOffset) {
      var xLabels;
      if (this.options.drawXLabels) {
        xLabels = [
          {
            date: this.options.startDate,
            pos: this.chartOffsetLeft() + 10
          }, {
            date: this.options.endDate,
            pos: this.chartOffsetLeft() + this.chartWidth() - 14
          }
        ];
        this.g.selectAll(".xLabel").data(xLabels).enter().append("svg:text").attr("class", "xLabel").text(function(d) {
          return d.date.getFullYear();
        }).attr("x", function(d) {
          return d.pos;
        }).attr("y", -1 * yOffset).attr("text-anchor", "middle");
      }
      if (this.options.drawYLabels) {
        return this.g.selectAll(".yLabel").data(yfn.ticks(4)).enter().append("svg:text").attr("class", "yLabel").text(function(d) {
          return NumberFormatter.format(d);
        }).attr("y", function(d) {
          return -1 * yfn(d);
        }).attr("text-anchor", "right").attr("dy", 4);
      }
    };
    SparklinePlot.prototype.drawGraphAxis = function(xfn, yfn, maxX, maxY) {
      this.g.append("svg:line").attr("x1", xfn(0)).attr("y1", -1 * yfn(0)).attr("x2", xfn(maxX)).attr("y2", -1 * yfn(0));
      return this.g.append("svg:line").attr("x1", xfn(0)).attr("y1", -1 * yfn(0)).attr("x2", xfn(0)).attr("y2", -1 * yfn(maxY)).attr("y2", -1 * yfn(maxY));
    };
    SparklinePlot.prototype.handleMouseover = function(evt) {
      var date, offset, realYPos, relX, relY, val;
      evt.preventDefault();
      evt.stopPropagation();
      offset = this.container.offset();
      relX = evt.pageX - offset.left;
      relY = evt.pageY - offset.top;
      if (relX > this.chartOffsetLeft() && relX < this.chartOffsetLeft() + this.chartWidth() && relY > this.chartOffsetTop() && relY < this.chartOffsetTop() + this.chartHeight()) {
        date = this.getDateFromChartPos(relX - this.chartOffsetLeft());
        val = this.getValueForDate(date);
        realYPos = this.options.height - this.yScale(val);
        if (Math.abs(relY - realYPos) < 20) {
          PopupBox.draw(evt.pageX, evt.pageY, DateFormatter.format(date), val);
          return this.container.addClass('hl_path');
        } else {
          this.container.removeClass('hl_path');
          return PopupBox.hide();
        }
      }
    };
    SparklinePlot.prototype.chartWidth = function() {
      return this.options.width - 2 * this.options.marginX;
    };
    SparklinePlot.prototype.chartHeight = function() {
      return this.options.height - 2 * this.options.marginY;
    };
    SparklinePlot.prototype.chartOffsetTop = function() {
      return this.options.marginY + this.options.yOffset;
    };
    SparklinePlot.prototype.chartOffsetLeft = function() {
      return this.options.marginX + this.options.xOffset;
    };
    SparklinePlot.prototype.getDateFromChartPos = function(chartPos) {
      var spanPercent;
      spanPercent = chartPos / this.chartWidth();
      return this.numberToDate(this.fullDateNumberSpan() * spanPercent + this.startDateNumber());
    };
    SparklinePlot.prototype.getValueForDate = function(date) {
      var dataX, spanPercent;
      spanPercent = (this.dateToNumber(date) - this.startDateNumber()) / this.fullDateNumberSpan();
      dataX = Math.round((this.data.length - 1) * spanPercent);
      return this.data[dataX];
    };
    SparklinePlot.prototype.startDateNumber = function() {
      return this.dateToNumber(this.options.startDate);
    };
    SparklinePlot.prototype.fullDateNumberSpan = function() {
      return this.dateToNumber(this.options.endDate) - this.startDateNumber();
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
  window.PopupBox = {
    draw: function(x, y, header, text) {
      var elm;
      elm = $('#js_viz_popup');
      elm.show();
      elm.css('left', x + 5 + 'px').css('top', y + 5 + 'px');
      elm.children('div').text(header);
      return elm.children('p').text(NumberFormatter.format(text) + " articles");
    },
    hide: function() {
      return $('#js_viz_popup').hide();
    }
  };
  window.NumberFormatter = {
    format: function(number) {
      return number.toString().replace(/(\d)(?=(\d\d\d)+(?!\d))/g, "$1,");
    }
  };
  window.DateFormatter = {
    months: {
      0: 'January',
      1: 'February',
      2: 'March',
      3: 'April',
      4: 'May',
      5: 'June',
      6: 'July',
      7: 'August',
      8: 'September',
      9: 'October',
      10: 'November',
      11: 'December'
    },
    format: function(date) {
      var month;
      month = this.months[date.getMonth()];
      return month + ' ' + date.getFullYear();
    }
  };
  window.SparklinePlot = SparklinePlot;
}).call(this);
