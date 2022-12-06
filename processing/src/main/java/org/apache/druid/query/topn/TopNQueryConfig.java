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

package org.apache.druid.query.topn;

import com.fasterxml.jackson.annotation.JsonProperty;

import javax.validation.constraints.Min;
import org.apache.druid.java.util.common.logger.Logger;

import java.util.Properties;
import java.io.FileInputStream;
import java.io.IOException;

/**
 */
public class TopNQueryConfig
{
  private static final Logger log = new Logger(TopNQueryConfig.class);
  public static final int DEFAULT_MIN_TOPN_THRESHOLD = 1000;

  public static String CTESTFILEPATH = System.getProperty("user.dir").split("/druid/processing/")[0] + "/core-ctest.xml";
  public static Properties configProps = new Properties();


  @JsonProperty
  @Min(1)
  private int minTopNThreshold = DEFAULT_MIN_TOPN_THRESHOLD;

  public int getMinTopNThreshold()
  {
    log.info("[CTEST][GET-PARAM] " + "druid.query.topN.minTopNThreshold");
    try{
      configProps.load(new FileInputStream(CTESTFILEPATH));
      if(configProps.getProperty("druid.query.topN.minTopNThreshold") != null){
        return Integer.parseInt(configProps.getProperty("druid.query.topN.minTopNThreshold"));
      }
    }
    catch(IOException e){
        log.info(CTESTFILEPATH);
    }
    return minTopNThreshold;
  }
}
