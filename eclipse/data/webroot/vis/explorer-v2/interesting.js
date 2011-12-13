var interesting = [];

interesting.push(["effect of 9/11 on coverage of selected countries","#%7B%22series%22%3A%5B%7B%22contents%22%3A%5B%5B%7B%22text%22%3A%22pakistan%22%2C%22type%22%3A%22default%22%7D%5D%5D%7D%2C%7B%22contents%22%3A%5B%5B%7B%22text%22%3A%22afghanistan%22%2C%22type%22%3A%22default%22%7D%5D%5D%7D%2C%7B%22contents%22%3A%5B%5B%7B%22text%22%3A%22iraq%22%2C%22type%22%3A%22default%22%7D%5D%5D%7D%2C%7B%22contents%22%3A%5B%5B%7B%22text%22%3A%22north%20korea%22%2C%22type%22%3A%22default%22%7D%5D%5D%7D%2C%7B%22contents%22%3A%5B%5B%7B%22text%22%3A%22iran%22%2C%22type%22%3A%22default%22%7D%5D%5D%7D%5D%2C%22startYear%22%3A2000%2C%22endYear%22%3A2010%2C%22horizontalAxis%22%3A%22date%22%2C%22dateGranularity%22%3A%22month%22%7D"]);

interesting.push(["sports seasons","#%7B%22series%22%3A%5B%7B%22contents%22%3A%5B%5B%7B%22text%22%3A%22soccer%22%2C%22type%22%3A%22default%22%7D%5D%5D%7D%2C%7B%22contents%22%3A%5B%5B%7B%22text%22%3A%22basketball%22%2C%22type%22%3A%22default%22%7D%5D%5D%7D%2C%7B%22contents%22%3A%5B%5B%7B%22text%22%3A%22football%22%2C%22type%22%3A%22default%22%7D%5D%5D%7D%2C%7B%22contents%22%3A%5B%5B%7B%22text%22%3A%22baseball%22%2C%22type%22%3A%22default%22%7D%5D%5D%7D%2C%7B%22contents%22%3A%5B%5B%7B%22text%22%3A%22hockey%22%2C%22type%22%3A%22default%22%7D%5D%5D%7D%5D%2C%22startYear%22%3A2000%2C%22endYear%22%3A2010%2C%22horizontalAxis%22%3A%22date%22%2C%22dateGranularity%22%3A%22month%22%7D"]);


interesting.push(["super heroes","#%7B%22series%22%3A%5B%7B%22contents%22%3A%5B%5B%7B%22text%22%3A%22batman%22%7D%5D%5D%7D%2C%7B%22contents%22%3A%5B%5B%7B%22text%22%3A%22spider%20man%22%7D%2C%7B%22text%22%3A%22spiderman%22%7D%5D%5D%7D%2C%7B%22contents%22%3A%5B%5B%7B%22text%22%3A%22frodo%22%2C%22type%22%3A%22entity%22%7D%5D%5D%7D%5D%2C%22horizontalAxis%22%3A%22month%22%2C%22graphStack%22%3Afalse%2C%22graphCountMode%22%3Atrue%2C%22graphMode%22%3A%22areas%22%2C%22startYear%22%3A2000%2C%22endYear%22%3A2010%7D"]);



$.each(interesting, function(t, i) {
	$("#interesting").append("<a href='"+i[1]+"'>"+i[0]+"</a><br />");
});