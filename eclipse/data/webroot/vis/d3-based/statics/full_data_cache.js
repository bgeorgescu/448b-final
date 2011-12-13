var FullDataCache = {
	startDate: new Date(2000,0),
	endDate: new Date(2010, 11),
	timeSpans: [0.5, 1, 2, 4, 6, 10],
	types: ['LOCATION', 'PERSON', 'ORGANIZATION', 'all'],
	
	init: function(){
		this.data = {};
		this.start = this.dateToNumber(this.startDate);
		this.end = this.dateToNumber(this.endDate);
		this.fullRange = this.end - this.start;
		this.timeSpans.push(this.fullRange)
	},
	
	getData: function(timeSpan){
		var totalEntries = Math.ceil(this.fullRange / timeSpan);
		var stepSize = (totalEntries + 1) / this.fullRange;
		var maxStartPoint = this.end - timeSpan;
		this.data[timeSpan] = {LOCATION: {}, ORGANIZATION: {}, PERSON: {}, all: {}};
		for (var i = 0; i < totalEntries; i++){
			var centerPoint = stepSize * i + this.start;
			var startPoint = centerPoint - (timeSpan / 2);
			if (startPoint < 0) startPoint = 0;
			if (startPoint > maxStartPoint) startPoint = maxStartPoint;
			var endPoint = startPoint + timeSpan;
			var currentIndex = i;
			for (var j = 0; j < 4; j++){
				var type = this.types[j];
				var queryType = type;
				if (type == 'all')  queryType = null;
				DatabaseInterface.similarEntitiesOverPeriod(null, this.numberToDate(startPoint), this.numberToDate(endPoint), $.proxy(function(code, results, duration){
					this.data[timeSpan][type][currentIndex] = {terms: results[0]['entity_'], counts: results[0]['count_']}
				}, this) , 15, {type: type})
      }
		}
		
	},
	
	dateToNumber: function(dateObj){
    return dateObj.getFullYear() + (dateObj.getMonth() / 12);
  },
  numberToDate: function(number){
    var month = Math.round((number % 1) * 12);
    var year = Math.floor(number);
    return new Date(year, month);
  }
}
