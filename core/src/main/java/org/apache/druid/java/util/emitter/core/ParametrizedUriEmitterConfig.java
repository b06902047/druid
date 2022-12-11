/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.druid.java.util.emitter.core;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.druid.java.util.common.logger.Logger;

import javax.validation.constraints.NotNull;

import java.util.Properties;
import java.io.FileInputStream;
import java.io.IOException;

public class ParametrizedUriEmitterConfig
{
  private static final Logger log = new Logger(ParametrizedUriEmitterConfig.class);
  public static String CTESTFILEPATH = System.getProperty("user.dir").split("/druid/core/")[0] + "/core-ctest.xml";
  public static Properties configProps = new Properties();
  private static final BaseHttpEmittingConfig DEFAULT_HTTP_EMITTING_CONFIG = new BaseHttpEmittingConfig();

  @NotNull
  @JsonProperty
  private String recipientBaseUrlPattern;

  @JsonProperty("httpEmitting")
  private BaseHttpEmittingConfig httpEmittingConfig = DEFAULT_HTTP_EMITTING_CONFIG;

  public String getRecipientBaseUrlPattern()
  {
    log.info("[CTEST][GET-PARAM] " + "druid.emitter.parametrized.recipientBaseUrlPattern");

    try{
      configProps.load(new FileInputStream(CTESTFILEPATH));
      if(configProps.getProperty("druid.emitter.parametrized.recipientBaseUrlPattern") != null){
        return configProps.getProperty("druid.emitter.parametrized.recipientBaseUrlPattern");
      }
    }
    catch(IOException e){
        log.info(CTESTFILEPATH);
    }
    return recipientBaseUrlPattern;
  }

  public HttpEmitterConfig buildHttpEmitterConfig(String baseUri)
  {
    log.info("[CTEST][SET-PARAM] " + "druid.emitter.http.recipientBaseUrl " + "NoTestTrace");
    return new HttpEmitterConfig(httpEmittingConfig, baseUri);
  }

  @Override
  public String toString()
  {
    return "ParametrizedUriEmitterConfig{" +
           "recipientBaseUrlPattern='" + recipientBaseUrlPattern + '\'' +
           ", httpEmittingConfig=" + httpEmittingConfig +
           '}';
  }
}
