/*
Copyright 2010 Agus Santoso

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package com.neusou.bioroid.restful;

import java.io.Reader;
import java.io.StringReader;

import android.os.Parcel;
import android.os.Parcelable;

import com.neusou.bioroid.restful.RestfulClient.IRestfulResponse;

/**
 * Default implementation of IRestfulResponse
 * @author asantoso	 
 */
public class StringRestfulResponse implements IRestfulResponse<String>{
		
		private String data;
		
		public StringRestfulResponse(String response) {
			data = response;
		}
		
		@Override
		public int describeContents() {			
			return 0;
		}

		@Override
		public void writeToParcel(Parcel dest, int flags) {
			dest.writeString(data);
		}
		
		public static final Parcelable.Creator<StringRestfulResponse> CREATOR = new Creator<StringRestfulResponse>() {
			
			@Override
			public StringRestfulResponse[] newArray(int size) {
				return null;
			}
			
			@Override
			public StringRestfulResponse createFromParcel(Parcel source) {
				return new StringRestfulResponse(source.readString());
			}
		};
	
		@Override
		public Reader get() {
			return new StringReader(data);
		}

		@Override
		public void set(Reader d) {			
		}

		@Override
		public String getData() {
			return data;
		}

		@Override
		public void setData(String data) {
			this.data = data;
		}
		
}
		