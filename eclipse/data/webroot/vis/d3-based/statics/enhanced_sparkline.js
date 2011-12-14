(function() {
  var EnhancedSparkline;
  var __hasProp = Object.prototype.hasOwnProperty, __extends = function(child, parent) {
    for (var key in parent) { if (__hasProp.call(parent, key)) child[key] = parent[key]; }
    function ctor() { this.constructor = child; }
    ctor.prototype = parent.prototype;
    child.prototype = new ctor;
    child.__super__ = parent.prototype;
    return child;
  }, __bind = function(fn, me){ return function(){ return fn.apply(me, arguments); }; };
  EnhancedSparkline = (function() {
    __extends(EnhancedSparkline, SparklinePlot);
    EnhancedSparkline.prototype.extraOptions = {
      onDragend: function() {
        return null;
      },
      onRescale: function() {
        return null;
      }
    };
    function EnhancedSparkline(container, data, options) {
      if (options == null) {
        options = {};
      }
      EnhancedSparkline.__super__.constructor.call(this, container, data, options);
      $.extend(this.options, this.extraOptions, options);
      this.container.css('position', 'relative');
    }
    EnhancedSparkline.prototype.draw = function() {
      var handleCss;
      EnhancedSparkline.__super__.draw.call(this);
      if (!this.rangeSelector) {
        this.selectorOffsetLeft = 20;
        this.selectorWidth = this.chartWidth() - 40;
        this.rangeSelector = $('<div />', {
          "class": 'range_selector'
        }).appendTo(this.container);
        this.rangeSelector.css({
          width: this.selectorWidth + 'px',
          height: (this.options.height + 10) + 'px',
          position: 'absolute',
          left: this.chartOffsetLeft() + this.selectorOffsetLeft + 'px',
          top: '-5px'
        });
        handleCss = {
          position: 'absolute',
          top: (this.options.height / 2) + 5,
          height: '10px',
          width: '6px'
        };
        this.leftHandle = $('<div />', {
          "class": 'handle'
        }).appendTo(this.rangeSelector).css($.extend({
          left: '-5px'
        }, handleCss));
        this.rightHandle = $('<div />', {
          "class": 'handle'
        }).appendTo(this.rangeSelector).css($.extend({
          right: '-5px'
        }, handleCss));
        this.leftHandle.mousedown(__bind(function(evt) {
          return this.startDrag(evt, this.leftHandle);
        }, this));
        this.rightHandle.mousedown(__bind(function(evt) {
          return this.startDrag(evt, this.rightHandle);
        }, this));
        this.rangeSelector.mousedown(__bind(function(evt) {
          return this.startSlide(evt);
        }, this));
        $('body').mouseup(__bind(function() {
          if (this.draggingHandle || this.sliding) {
            this.draggingHandle = false;
            this.sliding = false;
            return this.options.onDragend(this.getDateRange);
          }
        }, this));
        return $('body').mousemove(__bind(function(evt) {
          if (this.draggingHandle) {
            this.updateDragging(evt.pageX);
            return evt.preventDefault();
          } else if (this.sliding) {
            this.updateSliding(evt.pageX);
            return evt.preventDefault();
          }
        }, this));
      }
    };
    EnhancedSparkline.prototype.startDrag = function(evt, handle) {
      evt.preventDefault();
      this.draggingHandle = handle;
      PopupBox.hide();
      this.dragStartProperties = {
        offsetLeft: this.selectorOffsetLeft,
        width: this.selectorWidth
      };
      return this.dragStartPos = evt.pageX;
    };
    EnhancedSparkline.prototype.startSlide = function(evt) {
      evt.preventDefault();
      PopupBox.hide();
      if (this.chartWidth() > this.selectorWidth) {
        this.sliding = true;
        this.slideStartPos = evt.pageX;
        return this.slideStartOffset = this.selectorOffsetLeft;
      }
    };
    EnhancedSparkline.prototype.updateSliding = function(x) {
      var delta, maxLeftOffset, newOffset;
      maxLeftOffset = this.chartWidth() - this.selectorWidth;
      if (maxLeftOffset > 0) {
        delta = x - this.slideStartPos;
        newOffset = this.slideStartOffset + delta;
        if (newOffset < 0) {
          newOffset = 0;
        }
        if (newOffset > maxLeftOffset) {
          newOffset = maxLeftOffset;
        }
        this.rangeSelector.css({
          left: this.chartOffsetLeft() + newOffset + 'px'
        });
        this.selectorOffsetLeft = newOffset;
        return this.options.onRescale(this.getDateRange());
      }
    };
    EnhancedSparkline.prototype.handleMouseover = function(evt) {
      if (!(this.draggingHandle || this.sliding)) {
        return EnhancedSparkline.__super__.handleMouseover.call(this, evt);
      }
    };
    EnhancedSparkline.prototype.updateDragging = function(x) {
      var delta, maxWidth, newOffset, newWidth, offsetLeft, offsetRight;
      offsetLeft = this.dragStartProperties.offsetLeft;
      offsetRight = this.chartWidth() - (this.dragStartProperties.width + offsetLeft);
      maxWidth = this.draggingHandle === this.leftHandle ? this.chartWidth() - offsetRight : this.chartWidth() - offsetLeft;
      delta = x - this.dragStartPos;
      if (this.draggingHandle === this.leftHandle) {
        delta = -1 * delta;
      }
      newWidth = this.dragStartProperties.width + delta;
      if (newWidth < 10) {
        newWidth = 10;
      }
      if (newWidth > maxWidth) {
        newWidth = maxWidth;
      }
      newOffset = this.draggingHandle === this.leftHandle ? this.dragStartProperties.offsetLeft - delta : this.dragStartProperties.offsetLeft;
      if (newOffset > this.chartWidth() - 10) {
        newOffset = this.chartWidth() - 10;
      }
      if (newOffset < 0) {
        newOffset = 0;
      }
      this.selectorOffsetLeft = newOffset;
      this.selectorWidth = newWidth;
      this.rangeSelector.css({
        left: this.chartOffsetLeft() + newOffset + 'px',
        width: newWidth + 'px'
      });
      if (this.selectorWidth < this.chartWidth()) {
        this.rangeSelector.addClass('slidable');
      } else {
        this.rangeSelector.removeClass('slidable');
      }
      return this.options.onRescale(this.getDateRange());
    };
    EnhancedSparkline.prototype.getDateRange = function() {
      var dateRangeEnd, dateRangeSpanNumber, dateRangeStart, dateRangeStartNumber, maxSpan, spanPercent, spanStart, startDateNumber;
      startDateNumber = this.dateToNumber(this.options.startDate);
      maxSpan = this.dateToNumber(this.options.endDate) - startDateNumber;
      spanPercent = this.selectorWidth / this.chartWidth();
      spanStart = this.selectorOffsetLeft / this.chartWidth();
      dateRangeSpanNumber = spanPercent * maxSpan;
      dateRangeStartNumber = startDateNumber + spanStart * maxSpan;
      dateRangeStart = this.numberToDate(dateRangeStartNumber);
      dateRangeEnd = this.numberToDate(dateRangeStartNumber + dateRangeSpanNumber);
      return {
        start: dateRangeStart,
        end: dateRangeEnd
      };
    };
    return EnhancedSparkline;
  })();
  window.EnhancedSparkline = EnhancedSparkline;
}).call(this);
