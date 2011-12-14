class EnhancedSparkline extends SparklinePlot
  extraOptions:
    onDragend: -> null
    onRescale: -> null
    
  constructor: (container, data, options = {}) ->
    super(container, data, options)
    $.extend(@options, @extraOptions, options)
    @container.css('position', 'relative')
    
  draw: ->
    super()
    unless @rangeSelector
      @selectorOffsetLeft = 20
      @selectorWidth = @chartWidth() - 40
      @rangeSelector = $('<div />', {class: 'range_selector'}).appendTo(@container)
      @rangeSelector.css(
        width: @selectorWidth + 'px' 
        height: (@options.height + 10) + 'px'
        position: 'absolute'
        left: @chartOffsetLeft() + @selectorOffsetLeft + 'px'
        top: '-5px'
      )
      handleCss = 
        position: 'absolute'
        top: (@options.height / 2) + 5
        height: '10px'
        width: '6px'
      @leftHandle = $('<div />', {class: 'handle'}).appendTo(@rangeSelector).css($.extend({left: '-5px'}, handleCss))
      @rightHandle = $('<div />', {class: 'handle'}).appendTo(@rangeSelector).css($.extend({right: '-5px'}, handleCss))
      @leftHandle.mousedown (evt) => @startDrag(evt, @leftHandle)
      @rightHandle.mousedown (evt) => @startDrag(evt, @rightHandle)
      @rangeSelector.mousedown (evt) => @startSlide(evt)
          
      $('body').mouseup =>
        if @draggingHandle or @sliding
          @draggingHandle = false
          @sliding = false
          @options.onDragend(@getDateRange)
      $('body').mousemove (evt) => 
        if @draggingHandle
          @updateDragging(evt.pageX)
          evt.preventDefault()
        else if @sliding
          @updateSliding(evt.pageX) 
          evt.preventDefault()
  
  startDrag: (evt, handle) ->
    evt.preventDefault()
    @draggingHandle = handle
    PopupBox.hide()
    @dragStartProperties = {offsetLeft: @selectorOffsetLeft, width: @selectorWidth}
    @dragStartPos = evt.pageX
    
  startSlide: (evt) ->
    evt.preventDefault()
    PopupBox.hide()
    if @chartWidth() > @selectorWidth
      @sliding = true
      @slideStartPos = evt.pageX
      @slideStartOffset = @selectorOffsetLeft
    
  updateSliding: (x) ->
    maxLeftOffset = @chartWidth() - @selectorWidth
    if maxLeftOffset > 0
      delta = x - @slideStartPos
      newOffset = @slideStartOffset + delta
      newOffset = 0 if newOffset < 0
      newOffset = maxLeftOffset if newOffset > maxLeftOffset
      @rangeSelector.css(left: @chartOffsetLeft() + newOffset + 'px')
      @selectorOffsetLeft = newOffset
      @options.onRescale(@getDateRange())
  
  handleMouseover: (evt) ->
    super(evt) unless @draggingHandle || @sliding
  
  updateDragging: (x) ->
    offsetLeft = @dragStartProperties.offsetLeft
    offsetRight = @chartWidth() - (@dragStartProperties.width + offsetLeft)
    maxWidth = if @draggingHandle == @leftHandle then @chartWidth() - offsetRight else @chartWidth() - offsetLeft
    delta = x - @dragStartPos
    delta = -1 * delta if @draggingHandle == @leftHandle
    
    newWidth = @dragStartProperties.width + delta
    newWidth = 10 if newWidth < 10
    newWidth = maxWidth if newWidth > maxWidth
    newOffset = if @draggingHandle == @leftHandle then @dragStartProperties.offsetLeft - delta else @dragStartProperties.offsetLeft
    newOffset = @chartWidth() - 10 if newOffset > @chartWidth() - 10
    newOffset = 0 if newOffset < 0
    @selectorOffsetLeft = newOffset
    @selectorWidth = newWidth
    @rangeSelector.css({left: @chartOffsetLeft() + newOffset + 'px', width: newWidth + 'px'})
    if @selectorWidth < @chartWidth()
      @rangeSelector.addClass('slidable')
    else
      @rangeSelector.removeClass('slidable')
    @options.onRescale(@getDateRange())
        
  getDateRange: ->
    startDateNumber = @dateToNumber(@options.startDate)
    maxSpan = @dateToNumber(@options.endDate) - startDateNumber
    spanPercent = @selectorWidth / @chartWidth()
    spanStart = @selectorOffsetLeft / @chartWidth()
    dateRangeSpanNumber = spanPercent * maxSpan
    dateRangeStartNumber = startDateNumber + spanStart * maxSpan
    dateRangeStart = @numberToDate(dateRangeStartNumber)
    dateRangeEnd = @numberToDate(dateRangeStartNumber + dateRangeSpanNumber)
    return {start: dateRangeStart, end: dateRangeEnd}
  
window.EnhancedSparkline = EnhancedSparkline