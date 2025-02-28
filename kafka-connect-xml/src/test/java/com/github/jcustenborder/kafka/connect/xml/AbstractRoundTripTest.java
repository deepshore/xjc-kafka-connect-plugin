/**
 * Copyright © 2017 Jeremy Custenborder (jcustenborder@gmail.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.jcustenborder.kafka.connect.xml;

import com.google.common.io.BaseEncoding;
import org.apache.kafka.connect.data.Struct;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;
import javax.xml.datatype.XMLGregorianCalendar;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;

public abstract class AbstractRoundTripTest<T extends Connectable> {
  private static final Logger log = LoggerFactory.getLogger(AbstractRoundTripTest.class);
  JAXBContext context;
  Unmarshaller unmarshaller;

  protected abstract Class<?> objectFactoryClass();

  protected abstract Class<T> dataClass();

  protected abstract String dataFileName();

  @BeforeEach
  public void setupJAXBContext() throws JAXBException {
    Class<?> objectFactoryClass = objectFactoryClass();
    log.info("Configuring JAXBContext for '{}'", objectFactoryClass.getName());
    this.context = JAXBContext.newInstance(objectFactoryClass);
    this.unmarshaller = context.createUnmarshaller();
  }

  @TestFactory
  public Stream<DynamicTest> roundtrip() throws IOException, JAXBException, IllegalAccessException, InstantiationException {
    final T expected;
    final String dataFileName = dataFileName();
    log.info("Unmarshalling XML. dataFile = '{}'", dataFileName);
    try (InputStream inputStream = this.getClass().getResourceAsStream(dataFileName)) {
      Object value = unmarshaller.unmarshal(inputStream);

      if (dataClass().isInstance(value)) {
        expected = dataClass().cast(value);
      } else if (value instanceof JAXBElement) {
        JAXBElement o = (JAXBElement) value;
        expected = dataClass().cast(o.getValue());
      } else {
        throw new UnsupportedOperationException("" +
            String.format("%s is not supported", value.getClass())
        );
      }
    }
    Struct rootStruct = expected.toStruct();
    final T actual = dataClass().newInstance();
    actual.fromStruct(rootStruct);

    return Arrays.stream(dataClass().getMethods())
        .filter(method -> method.getName().startsWith("get") || method.getName().startsWith("is"))
        .filter(method -> !method.getName().equals("getClass"))
        .map(method -> dynamicTest(method.getName(), () -> {
          Object exp = method.invoke(expected);
          Object act = method.invoke(actual);



          if (byte[].class.equals(method.getReturnType())) {
            byte[] ebytes = (byte[]) exp;
            byte[] abytes = (byte[]) act;
            assertEquals(
                BaseEncoding.base32Hex().encode(ebytes),
                BaseEncoding.base32Hex().encode(abytes),
                String.format("%s should match.", method.getName())
            );
          } else if (XMLGregorianCalendar.class.equals(method.getReturnType())) {
            XMLGregorianCalendar eValue = (XMLGregorianCalendar) exp;
            XMLGregorianCalendar aValue = (XMLGregorianCalendar) act;

            assertEquals(eValue.getYear(), aValue.getYear(), "year");
            assertEquals(eValue.getMonth(), aValue.getMonth(), "month");
            assertEquals(eValue.getDay(), aValue.getDay(), "day");
            assertEquals(eValue.getHour(), aValue.getHour(), "hour");
            assertEquals(eValue.getMinute(), aValue.getMinute(), "minute");
            assertEquals(eValue.getSecond(), aValue.getSecond(), "second");
          } else {
            assertEquals(exp, act, String.format("%s should match.", method.getName()));
          }
        }));
  }

}
