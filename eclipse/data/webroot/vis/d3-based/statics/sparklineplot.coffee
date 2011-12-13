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
  
  setData: (@data) ->
    @xmax = d3.max(@data)
    @ymax = data.length
    yScaleBounds = [0 + @options.marginY + @options.yOffset, @options.height - @options.marginY + @options.yOffset]
    xScaleBounds = [0 + @options.marginX + @options.xOffset, @options.width - @options.marginX + @options.xOffset]
    @yScale = d3.scale.linear().domain([0, @xmax]).range(yScaleBounds)
    @xScale = d3.scale.linear().domain([0, @ymax]).range(xScaleBounds)
    @draw()
    
  draw: ->
    @clearCanvas()
    lineFn = @getLine(@options.interpolation, @options.tension, @xScale, @yScale)
    @path = @g.append("svg:path")
        .attr("d", lineFn(@data))
        .attr("style", "stroke: #{@options.color}; stroke-width: #{@options.strokeWidth}px;");

    #Draw the X axis
    @drawGraphAxis(@xScale, @yScale, @ymax, @xmax)
    
    #Draw the Y axis if necessary
    @drawGraphLabels(@xScale, @yScale, @options.yOffset)   
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
        
  drawGraphLabels: (xfn, yfn, yOffset) ->
    if @options.drawXLabels
      @g.selectAll(".xLabel")
          .data(xfn.ticks(5))
          .enter().append("svg:text")
          .attr("class", "xLabel")
          .text(String)
          .attr("x", (d) -> xfn(d))
          .attr("y", -1*yOffset)
          .attr("text-anchor", "middle")
    
    if @options.drawYLabels
      @g.selectAll(".yLabel")
          .data(yfn.ticks(4))
          .enter().append("svg:text")
          .attr("class", "yLabel")
          .text(String)
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
        
  cssWidth: ->
    return @options.width - @options.marginX
      
  dateToNumber: (dateObj) ->
    return dateObj.getFullYear() + dateObj.getMonth() / 12
    
  numberToDate: (number) ->
    month = Math.round((number % 1) * 12)
    year = Math.floor(number)
    return new Date(year, month)
        
window.SparklinePlot = SparklinePlot