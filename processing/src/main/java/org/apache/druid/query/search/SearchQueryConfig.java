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

package org.apache.druid.query.search;

import com.fasterxml.jackson.annotation.JsonProperty;

import javax.validation.constraints.Min;

import org.apache.druid.java.util.common.logger.Logger;

import java.util.Properties;
import java.io.FileInputStream;
import java.io.IOException;

/**
 */
public class SearchQueryConfig
{
  private static final Logger log = new Logger(SearchQueryConfig.class);
  public static final String CTX_KEY_STRATEGY = "searchStrategy";

  public static String CTESTFILEPATH = System.getProperty("user.dir").split("/druid/processing/")[0] + "/core-ctest.xml";
  public static Properties configProps = new Properties();

  @JsonProperty
  @Min(1)
  private int maxSearchLimit = 1000;

  @JsonProperty
  private String searchStrategy = UseIndexesStrategy.NAME;

  public int getMaxSearchLimit()
  {
    log.info("[CTEST][GET-PARAM] " + "druid.query.search.maxSearchLimit");
    try{
      configProps.load(new FileInputStream(CTESTFILEPATH));
      if(configProps.getProperty("druid.query.search.maxSearchLimit") != null){
        return Integer.parseInt(configProps.getProperty("druid.query.search.maxSearchLimit"));
      }
    }
    catch(IOException e){
        log.info(CTESTFILEPATH);
    }
    return maxSearchLimit;
  }

  public String getSearchStrategy()
  {
    log.info("[CTEST][GET-PARAM] " + "druid.query.search.searchStrategy");
    try{
      configProps.load(new FileInputStream(CTESTFILEPATH));
      if(configProps.getProperty("druid.query.search.searchStrategy") != null){
        return configProps.getProperty("druid.query.search.searchStrategy");
      }
    }
    catch(IOException e){
        log.info(CTESTFILEPATH);
    }
    return searchStrategy;
  }

  public void setSearchStrategy(final String strategy)
  {
    log.info("[CTEST][SET-PARAM] " + "druid.query.search.searchStrategy " + "NoTestTrace");
    this.searchStrategy = strategy;
  }

  public SearchQueryConfig withOverrides(final SearchQuery query)
  {
    final SearchQueryConfig newConfig = new SearchQueryConfig();
    newConfig.maxSearchLimit = query.getLimit();
    newConfig.searchStrategy = query.getQueryContext().getAsString(CTX_KEY_STRATEGY, searchStrategy);
    return newConfig;
  }
}
