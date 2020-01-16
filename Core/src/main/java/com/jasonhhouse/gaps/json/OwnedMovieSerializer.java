/*
 * Copyright 2020 Jason H House
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.jasonhhouse.gaps.json;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.jasonhhouse.gaps.OwnedMovie;
import java.io.IOException;

public class OwnedMovieSerializer extends StdSerializer<OwnedMovie> {
    public OwnedMovieSerializer() {
        this(null);
    }

    protected OwnedMovieSerializer(Class<OwnedMovie> t) {
        super(t);
    }

    @Override
    public void serialize(OwnedMovie movie, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        jsonGenerator.writeStartObject();
        jsonGenerator.writeNumberField(OwnedMovie.TVDB_ID, movie.getTvdbId());
        jsonGenerator.writeStringField(OwnedMovie.IMDB_ID, movie.getImdbId());
        jsonGenerator.writeStringField(OwnedMovie.NAME, movie.getName());
        jsonGenerator.writeNumberField(OwnedMovie.YEAR, movie.getYear());
        jsonGenerator.writeStringField(OwnedMovie.THUMBNAIL, movie.getThumbnail());
        jsonGenerator.writeStringField(OwnedMovie.LANGUAGE, movie.getLanguage());
        jsonGenerator.writeEndObject();
    }
}
