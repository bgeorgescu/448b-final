
class SimilarityResult
  defaultOptions:
    startDate: null
    endDate: null
    onDragend: () -> null
    graphWidth: 190
    graphHeight: 20
    pubid: false
  
  constructor: (parent, @term, @count, @otherTerms, options = {}) ->
    @parent = $(parent)
    @otherTerms.push(term)
    @options = $.extend({}, @defaultOptions, options)
    @scaffold()
    @loadSparkline()
    @elm.click =>
      @options.onClick(this)
    
  scaffold: ->
    @elm = $('<li />').appendTo @parent
    textContainer = $("<div />").appendTo @elm
    textContainer.append $('<span/>', {class: 'header', text: @term})
    textContainer.append $('<span/>', {class: 'count', text: @count})
    id = "graph#{Math.random().toString().slice(2)}"
    @graphContainer = $('<div />', {class: 'minigraph', id: id}).appendTo @elm
      
  loadSparkline: ->
    DatabaseInterface.query(
      terms: @otherTerms.concat([@term])
      callback: $.proxy(@drawGraph, this)
      useAnd: true
      pubid: @options.pubid
    )
    
  drawGraph: (code, results, duration) ->
    @sparkline = new HighlightableSparkline(@graphContainer[0], results[0] ,
      height: @options.graphHeight
      width: @options.graphWidth
      drawTicks: false
      drawYLabels: false
    )
    @sparkline.highlight(@hlSpan) if @hlSpan

  highlight: (@hlSpan) -> 
    @sparkline.highlight(@hlSpan) if @sparkline 
  
class ExtraTerm
  constructor: (@term, @onRemoveCallback) ->
    @draw()
    
  remove: ->
    @elm.remove()
    @onRemoveCallback(this)
    
  draw: ->
    @elm = $('<span />', {class: 'extra_term', text: @term}).appendTo $('#js_extra_terms')
    closeLink = $('<a />', {text: 'x'}).appendTo @elm
    closeLink.click (evt) =>
      evt.preventDefault()
      @remove()

######################### Main Object for Viz2 #######################################

Viz2 =
  init: ->
    @terms = []
    @search = $('#js_search_box')
    @search.keyup (event) =>
      @loadData() if event.keyCode == 13
    @loadData()
    $('#js_filter_entities').change => @loadRelatedData() if @mainSparkline
    $('#js_filter_newspapers').change => @loadData()
    
  loadData: ->
    $('.js_roller').show()
    term = $.trim(@search.val())
    @search.val('')
    term = null if @term == ""
    @addTerm(term) if term
    @mainSparkline.clear() if @mainSparkline
    DatabaseInterface.query(
      terms: @getSearchTerms()
      callback: $.proxy(@drawGraph, this)
      pubid: @getPubid()
      useAnd: true
    )
    @loadRelatedData()
    
  loadRelatedData: ->
    $('.js_attr_list').empty()
    $('#js_related_nouns_roller').show()
    @loadTimeSpan()
    DatabaseInterface.similarEntities(
      terms: @getSearchTerms()
      useAnd: true
      startDate: @timeSpan.start
      endDate: @timeSpan.end
      callback: $.proxy(@setRelatedNouns, this)
      maxResults: 20
      pubid: @getPubid()
      type: @getEntityType()
    )
    
  getSearchTerms: ->
    return $.map(@terms, (termObj) -> termObj.term)
  
  drawGraph: (code, results, duration) ->
    $('#js_sparkline_roller').hide()
    if @mainSparkline
      @mainSparkline.setData(results[0]) 
    else
      @mainSparkline = new EnhancedSparkline('#js_main_viz', results[0],
        width: 700
        xOffset: 20
        marginX: 30
        marginY: 10
        onRescale: (dateSpan) =>
          $('#js_date_start').html(DateFormatter.format(dateSpan.start))
          $('#js_date_end').html(DateFormatter.format(dateSpan.end))
        onDragend: (dateSpan) => @loadRelatedData()
      )
      
  loadTimeSpan: ->
    @timeSpan = if @mainSparkline then @mainSparkline.getDateRange() else {}
      
  getEntityType: ->
    @filteringEntity = $('input[name=js_filter_entities]:checked').val()
    return if @filteringEntity == 'all' then null else @filteringEntity
    
  getPubid: ->
    @pubid = $('input[name=js_filter_newspapers]:checked').val()
    return if @pubid == 'all' then null else @pubid

  setRelatedNouns: (code, results, duration) ->
    $('#js_related_nouns_roller').hide()
    $('.js_nouns_list').empty()
    @setListValues('#js_related_nouns', results[0]['entity_'], results[0]['count_'])
    
  addTerm: (term) ->
    if @getSearchTerms().indexOf(term) == -1
      termObj = new ExtraTerm term, (thisTermObj) =>
        @terms.remove(thisTermObj)
        @loadData()
      @terms.push(termObj)
        
  setTerm: (term) ->
    @search.val(term)
    @loadData()
    
  setListValues: (ul, values, counts) ->
    currentUl = $(ul)
    for value, i in values
      count = counts[i]
      currentUl = $(ul + '2') if i > 4
      currentUl = $(ul + '3') if i > 9
      currentUl = $(ul + '4') if i > 14
      sparkline = new SimilarityResult(currentUl, value, count, @getSearchTerms(),
        onClick: (result) =>
          #@setTerm(result.term)
          @addTerm(result.term)
          @loadData()
        pubid: @getPubid()
      )
      sparkline.highlight(@timeSpan)

DateFormatter =
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

window.Viz2 = Viz2
    
$ ->
  Viz2.init()
