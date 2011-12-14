class SparklinePlot
  defaultOptions:
    interpolation: 'linear'
    tension: 1
    drawXLabels: false
    drawYLabels: true
    drawTicks: true
    marginX: 0
    marginY: 0
    color: 'blue'
    strokeWidth: 2 
    width: 958
    height: 100
    xOffset: 0
    yOffset: 0
    popup: false
    startDate: new Date(2000,0)
    endDate: new Date(2010, 11)
  
  constructor: (container, data, options = {}) ->
    @container = $(container)
    @options = $.extend({}, @defaultOptions, options)
    @vis = d3.select(container)
        .append("svg:svg")
        .attr("width", @options.width)
        .attr("height", @options.height)
    @g = @vis.append("svg:g").attr("transform", "translate(0, #{@options.height})")
    @setData(data)
    @container.mousemove (evt) => 
      @handleMouseover(evt) if @options.popup   
    $('body').mouseover (evt) =>
      @container.removeClass('hl_path')
      PopupBox.hide()
  
  setData: (@data) ->
    @xmax = d3.max(@data)
    @ymax = data.length
    yScaleBounds = [0 + @options.marginY + @options.yOffset, @options.height - @options.marginY + @options.yOffset]
    xScaleBounds = [0 + @options.marginX + @options.xOffset, @options.width - @options.marginX + @options.xOffset]
    @yScale = d3.scale.linear().domain([0, @xmax]).range(yScaleBounds)
    @xScale = d3.scale.linear().domain([0, @ymax]).range(xScaleBounds)
    @xinv = d3.scale.linear().domain(xScaleBounds).range([0, @ymax]);
    @draw()
    
  draw: ->
    @clearCanvas()
    lineFn = @getLine(@options.interpolation, @options.tension, @xScale, @yScale)
    @path = @g.append("svg:path")
        .attr("d", lineFn(@data))
        .attr("style", "stroke: #{@options.color}; stroke-width: #{@options.strokeWidth}px;")

    #Draw the X axis
    @drawGraphAxis(@xScale, @yScale, @ymax, @xmax)
    
    #Draw the Y axis if necessary
    @drawGraphLabels( @yScale, @options.yOffset)   
    @drawGraphTicks(@xScale, @yScale) if @options.drawTicks
    
  getLine: (interpolation, tension, xfn, yfn) ->
    d3.svg.line()
      .x((d,i) -> xfn(i))
      .y((d) -> -1 * yfn(d))
      .interpolate(interpolation)
      .tension(tension)
  
  clear: ->
    @clearCanvas()
  
  clearCanvas: ->
    @g.selectAll("line").remove()
    @g.selectAll("text").remove()
    @g.selectAll("path").remove()
    @g.selectAll('.xTicks').remove()
    @g.selectAll('.yTicks').remove()
    
  drawGraphTicks: (xfn, yfn) ->
    @g.selectAll(".xTicks")
        .data(xfn.ticks(5))
        .enter().append("svg:line")
        .attr("class", "xTicks")
        .attr("x1", (d) -> xfn(d))
        .attr("y1", -1 * yfn(0))
        .attr("x2", (d) -> xfn(d))
        .attr("y2", -1 * yfn(-0.2))
    
    @g.selectAll(".yTicks")
        .data(yfn.ticks(4))
        .enter().append("svg:line")
        .attr("class", "yTicks")
        .attr("y1", (d) -> -1 * yfn(d))
        .attr("x1", xfn(-0.2))
        .attr("y2", (d) -> -1 * yfn(d))
        .attr("x2", xfn(0))
        
  drawGraphLabels: (yfn, yOffset) ->
    if @options.drawXLabels
      xLabels = [
        {date: @options.startDate, pos: @chartOffsetLeft() + 10},
        {date: @options.endDate, pos: @chartOffsetLeft() + @chartWidth() - 14}
      ]
      @g.selectAll(".xLabel")
          .data(xLabels)
          .enter().append("svg:text")
          .attr("class", "xLabel")
          .text((d) -> d.date.getFullYear())
          .attr("x", (d) -> d.pos)
          .attr("y", -1 * yOffset)
          .attr("text-anchor", "middle")
    
    if @options.drawYLabels
      @g.selectAll(".yLabel")
          .data(yfn.ticks(4))
          .enter().append("svg:text")
          .attr("class", "yLabel")
          .text((d) -> NumberFormatter.format(d))
          .attr("y", (d) -> -1 * yfn(d))
          .attr("text-anchor", "right")
          .attr("dy", 4)
  
  drawGraphAxis: (xfn, yfn, maxX, maxY) ->
    #Draw the X axis
    @g.append("svg:line")
        .attr("x1", xfn(0))
        .attr("y1", -1 * yfn(0))
        .attr("x2", xfn(maxX))
        .attr("y2", -1 * yfn(0))
  
    #Draw the Y axis
    @g.append("svg:line")
        .attr("x1", xfn(0))
        .attr("y1", -1 * yfn(0))
        .attr("x2", xfn(0))
        .attr("y2", -1 * yfn(maxY))
        .attr("y2", -1 * yfn(maxY))
        
  handleMouseover: (evt) ->
    evt.preventDefault()
    evt.stopPropagation()
    offset = @container.offset()
    relX = evt.pageX - offset.left
    relY = evt.pageY - offset.top
    if relX > @chartOffsetLeft() && relX < @chartOffsetLeft() + @chartWidth() && relY > @chartOffsetTop() && relY < @chartOffsetTop() + @chartHeight()
      date = @getDateFromChartPos(relX - @chartOffsetLeft())
      val = @getValueForDate(date)
      realYPos = @options.height - @yScale(val)
      if Math.abs(relY - realYPos) < 20
        PopupBox.draw(evt.pageX, evt.pageY, DateFormatter.format(date), val)
        @container.addClass('hl_path')
      else
        @container.removeClass('hl_path')
        PopupBox.hide()
        
  chartWidth: ->
    return @options.width - 2 * @options.marginX
    
  chartHeight: ->
    return @options.height - 2 * @options.marginY
      
  chartOffsetTop: ->
    return @options.marginY + @options.yOffset    
      
  chartOffsetLeft: ->
    return @options.marginX + @options.xOffset    
      
  getDateFromChartPos: (chartPos) ->
    spanPercent = chartPos / @chartWidth()
    return @numberToDate(@fullDateNumberSpan() * spanPercent + @startDateNumber())
    
  getValueForDate: (date) ->
    spanPercent = (@dateToNumber(date) - @startDateNumber()) / @fullDateNumberSpan()
    dataX = Math.round((@data.length - 1) * spanPercent)
    return @data[dataX]
    
  startDateNumber: ->
    return @dateToNumber(@options.startDate)
      
  fullDateNumberSpan: ->
    return @dateToNumber(@options.endDate) - @startDateNumber()
      
  dateToNumber: (dateObj) ->
    return dateObj.getFullYear() + dateObj.getMonth() / 12
    
  numberToDate: (number) ->
    month = Math.round((number % 1) * 12)
    year = Math.floor(number)
    return new Date(year, month)
    
window.PopupBox = 
  draw: (x, y, header, text)->
    elm = $('#js_viz_popup')
    elm.show()
    elm.css('left', x + 5 + 'px').css('top', y + 5 + 'px')
    elm.children('div').text(header)
    elm.children('p').text(NumberFormatter.format(text) + " articles")
  hide: ->
    $('#js_viz_popup').hide()
    
window.NumberFormatter = 
  format: (number) ->
   return number.toString().replace(/(\d)(?=(\d\d\d)+(?!\d))/g, "$1,")    
    
window.DateFormatter =
  months:
    0: 'January'
    1: 'February'
    2: 'March'
    3: 'April'
    4: 'May'
    5: 'June'
    6: 'July'
    7: 'August'
    8: 'September'
    9: 'October'
    10: 'November'
    11: 'December'
  format: (date) ->
    month = @months[date.getMonth()]
    return month + ' ' + date.getFullYear()  
    
        
window.SparklinePlot = SparklinePlot