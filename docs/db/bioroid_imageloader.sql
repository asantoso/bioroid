DROP TABLE IF EXISTS bioroid_imageloader_cache;
CREATE TABLE bioroid_imageloader_cache ( 
	_id INTEGER PRIMARY KEY AUTOINCREMENT, 
	last_accessed_date REAL UNIQUE ON CONFLICT REPLACE,
	filesize INTEGER,
	imagepath TEXT
); 
