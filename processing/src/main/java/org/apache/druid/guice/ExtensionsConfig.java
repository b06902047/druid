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

import java.util.Properties;
import java.io.FileInputStream;
import java.io.IOException;

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

  public static String CTESTFILEPATH = System.getProperty("user.dir").split("/druid/processing/")[0] + "/core-ctest.xml";
  public static Properties configProps = new Properties();


  public boolean searchCurrentClassloader()
  {
    logger.info("[CTEST][GET-PARAM] " + "druid.extensions.searchCurrentClassloader");
    try{
      configProps.load(new FileInputStream(CTESTFILEPATH));
      if(configProps.getProperty("druid.extensions.searchCurrentClassloader") != null){
        return Boolean.parseBoolean(configProps.getProperty("druid.extensions.searchCurrentClassloader"));
      }
    }
    catch(IOException e){
        logger.info(CTESTFILEPATH);
    }
    return searchCurrentClassloader;
  }

  public String getDirectory()
  {
    return directory;
  }

  public boolean isUseExtensionClassloaderFirst()
  {
    logger.info("[CTEST][GET-PARAM] " + "druid.extensions.useExtensionClassloaderFirst");
    try{
      configProps.load(new FileInputStream(CTESTFILEPATH));
      if(configProps.getProperty("druid.extensions.useExtensionClassloaderFirst") != null){
        return Boolean.parseBoolean(configProps.getProperty("druid.extensions.useExtensionClassloaderFirst"));
      }
    }
    catch(IOException e){
        logger.info(CTESTFILEPATH);
    }
    return useExtensionClassloaderFirst;
  }

  public String getHadoopDependenciesDir()
  {
    logger.info("[CTEST][GET-PARAM] " + "druid.extensions.hadoopDependenciesDir");
    try{
      configProps.load(new FileInputStream(CTESTFILEPATH));
      if(configProps.getProperty("druid.extensions.hadoopDependenciesDir") != null){
        return configProps.getProperty("druid.extensions.hadoopDependenciesDir");
      }
    }
    catch(IOException e){
        logger.info(CTESTFILEPATH);
    }
    return hadoopDependenciesDir;
  }

  public String getHadoopContainerDruidClasspath()
  {
   logger.info("[CTEST][GET-PARAM] " + "druid.extensions.hadoopContainerDruidClasspath");
   try{
      configProps.load(new FileInputStream(CTESTFILEPATH));
      if(configProps.getProperty("druid.extensions.hadoopContainerDruidClasspath") != null){
        return configProps.getProperty("druid.extensions.hadoopContainerDruidClasspath");
      }
    }
    catch(IOException e){
        logger.info(CTESTFILEPATH);
    }
   return hadoopContainerDruidClasspath;
  }

  public boolean getAddExtensionsToHadoopContainer()
  {
    logger.info("[CTEST][GET-PARAM] " + "druid.extensions.addExtensionsToHadoopContainer");
    try{
      configProps.load(new FileInputStream(CTESTFILEPATH));
      if(configProps.getProperty("druid.extensions.addExtensionsToHadoopContainer") != null){
        return Boolean.parseBoolean(configProps.getProperty("druid.extensions.addExtensionsToHadoopContainer"));
      }
    }
    catch(IOException e){
        logger.info(CTESTFILEPATH);
    }
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
