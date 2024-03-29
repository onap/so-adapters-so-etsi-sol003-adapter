/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2021 Nordix Foundation.
 * ================================================================================
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 * ============LICENSE_END=========================================================
 */
package org.onap.so.adapters.etsi.sol003.adapter.common.utils;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * @author Waqas Ikram (waqas.ikram@est.tech)
 *
 */
public class LocalDateTimeTypeAdapter extends TypeAdapter<LocalDateTime> {

    private static final DateTimeFormatter DEFAULT_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private DateTimeFormatter formatter;

    public LocalDateTimeTypeAdapter() {
        this(DEFAULT_FORMATTER);
    }

    public LocalDateTimeTypeAdapter(final DateTimeFormatter formatter) {
        this.formatter = formatter;
    }

    @Override
    public void write(final JsonWriter out, final LocalDateTime localDateTime) throws IOException {
        if (localDateTime == null) {
            out.nullValue();
        } else {
            out.value(formatter.format(localDateTime));
        }
    }

    @Override
    public LocalDateTime read(final JsonReader in) throws IOException {
        if (JsonToken.NULL == in.peek()) {
            in.nextNull();
            return null;

        }
        final String dateTime = in.nextString();
        return LocalDateTime.parse(dateTime, formatter);

    }

}
