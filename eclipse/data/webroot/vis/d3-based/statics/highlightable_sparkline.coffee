class HighlightableSparkline extends SparklinePlot
  
  highlight: (dateRange) ->
    start = dateRange.start || @options.startDate
    end = dateRange.end || @options.endDate
    @hlBox.remove() if @hlBox
    hlSpan = @dateToNumber(end) - @dateToNumber(start)
    fullSpan = @dateToNumber(@options.endDate) - @dateToNumber(@options.startDate)
    offsetLeft = @dateToNumber(start) - @dateToNumber(@options.startDate)
    spanWidth = (hlSpan / fullSpan) * @options.width
    spanOffsetLeft = (offsetLeft / fullSpan) * @options.width
    @hlBox = @vis.append('svg:rect')
      .attr('style', "fill: #FFFF00; fill-opacity: 0.3")
      .attr('height', @options.height)
      .attr('width', spanWidth)
      .attr('class', 'hlBox')
      .attr('x', spanOffsetLeft)
      .attr('y', 0)
    
window.HighlightableSparkline = HighlightableSparkline
    
