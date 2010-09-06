DROP TABLE IF EXISTS  bioroid_restful_response_cache;
CREATE TABLE bioroid_restful_response_cache ( 
	_id INTEGER PRIMARY KEY AUTOINCREMENT,
	request_url TEXT UNIQUE ON CONFLICT REPLACE,
	http_method TEXT,
	invocation_time REAL,
	response TEXT,
	call_id INTEGER,
	response_size REAL
); 
	