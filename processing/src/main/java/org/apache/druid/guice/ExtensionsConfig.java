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

package org.apache.druid.guice;

import com.fasterxml.jackson.annotation.JsonProperty;

import javax.validation.constraints.NotNull;
import java.util.LinkedHashSet;
import org.apache.druid.java.util.common.logger.Logger;

/**
 */
public class ExtensionsConfig
{
  @JsonProperty
  @NotNull
  private boolean searchCurrentClassloader = true;

  @JsonProperty
  private String directory = "extensions";

  @JsonProperty
  private boolean useExtensionClassloaderFirst = false;

  @JsonProperty
  private String hadoopDependenciesDir = "hadoop-dependencies";

  @JsonProperty
  private String hadoopContainerDruidClasspath = null;

  //Only applicable when hadoopContainerDruidClasspath is explicitly specified.
  @JsonProperty
  private boolean addExtensionsToHadoopContainer = false;

  @JsonProperty
  private LinkedHashSet<String> loadList;

  @JsonProperty
  private static final Logger logger 
            = new Logger(ExtensionsConfig.class.getName());

  public boolean searchCurrentClassloader()
  {
    logger.info("[CTEST][GET-PARAM] " + "druid.extensions.searchCurrentClassloader");
    return searchCurrentClassloader;
  }

  public String getDirectory()
  {
    return directory;
  }

  public boolean isUseExtensionClassloaderFirst()
  {
    logger.info("[CTEST][GET-PARAM] " + "druid.extensions.useExtensionClassloaderFirst");
    return useExtensionClassloaderFirst;
  }

  public String getHadoopDependenciesDir()
  {
    logger.info("[CTEST][GET-PARAM] " + "druid.extensions.hadoopDependenciesDir");
    return hadoopDependenciesDir;
  }

  public String getHadoopContainerDruidClasspath()
  {
   logger.info("[CTEST][GET-PARAM] " + "druid.extensions.hadoopContainerDruidClasspath");
   return hadoopContainerDruidClasspath;
  }

  public boolean getAddExtensionsToHadoopContainer()
  {
    logger.info("[CTEST][GET-PARAM] " + "druid.extensions.addExtensionsToHadoopContainer");
    return addExtensionsToHadoopContainer;
  }

  public LinkedHashSet<String> getLoadList()
  {
    logger.info("[CTEST][GET-PARAM] " + "druid.extensions.loadList");
    return loadList;
  }

  @Override
  public String toString()
  {
    return "ExtensionsConfig{" +
           "searchCurrentClassloader=" + searchCurrentClassloader +
           ", directory='" + directory + '\'' +
           ", useExtensionClassloaderFirst=" + useExtensionClassloaderFirst +
           ", hadoopDependenciesDir='" + hadoopDependenciesDir + '\'' +
           ", hadoopContainerDruidClasspath='" + hadoopContainerDruidClasspath + '\'' +
           ", addExtensionsToHadoopContainer=" + addExtensionsToHadoopContainer +
           ", loadList=" + loadList +
           '}';
  }
}
