DROP TABLE IF EXISTS  bioroid_restful_response_cache;
CREATE TABLE bioroid_restful_response_cache ( 
	_id INTEGER PRIMARY KEY AUTOINCREMENT,
	request_url TEXT,
	http_method TEXT,	
	invocation_date REAL UNIQUE ON CONFLICT REPLACE,
	response TEXT,
	call_id INTEGER	
); 
	