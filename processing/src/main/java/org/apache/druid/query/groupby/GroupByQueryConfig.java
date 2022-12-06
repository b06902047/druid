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

package org.apache.druid.query.groupby;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.druid.java.util.common.HumanReadableBytes;
import org.apache.druid.java.util.common.logger.Logger;
import org.apache.druid.query.DruidProcessingConfig;
import org.apache.druid.query.QueryContexts;
import org.apache.druid.query.groupby.strategy.GroupByStrategySelector;
import org.apache.druid.utils.JvmUtils;

import java.util.Properties;
import java.io.FileInputStream;
import java.io.IOException;
/**
 *
 */
public class GroupByQueryConfig
{
  private static final Logger logger = new Logger(GroupByQueryConfig.class);

  public static final long AUTOMATIC = 0;

  public static final String CTX_KEY_STRATEGY = "groupByStrategy";
  public static final String CTX_KEY_FORCE_LIMIT_PUSH_DOWN = "forceLimitPushDown";
  public static final String CTX_KEY_APPLY_LIMIT_PUSH_DOWN = "applyLimitPushDown";
  public static final String CTX_KEY_APPLY_LIMIT_PUSH_DOWN_TO_SEGMENT = "applyLimitPushDownToSegment";
  public static final String CTX_KEY_FORCE_PUSH_DOWN_NESTED_QUERY = "forcePushDownNestedQuery";
  public static final String CTX_KEY_EXECUTING_NESTED_QUERY = "executingNestedQuery";
  public static final String CTX_KEY_ARRAY_RESULT_ROWS = "resultAsArray";
  public static final String CTX_KEY_ENABLE_MULTI_VALUE_UNNESTING = "groupByEnableMultiValueUnnesting";
  public static final String CTX_KEY_BUFFER_GROUPER_MAX_SIZE = "bufferGrouperMaxSize";
  private static final String CTX_KEY_IS_SINGLE_THREADED = "groupByIsSingleThreaded";
  private static final String CTX_KEY_MAX_INTERMEDIATE_ROWS = "maxIntermediateRows";
  private static final String CTX_KEY_MAX_RESULTS = "maxResults";
  private static final String CTX_KEY_BUFFER_GROUPER_INITIAL_BUCKETS = "bufferGrouperInitialBuckets";
  private static final String CTX_KEY_BUFFER_GROUPER_MAX_LOAD_FACTOR = "bufferGrouperMaxLoadFactor";
  private static final String CTX_KEY_MAX_ON_DISK_STORAGE = "maxOnDiskStorage";
  private static final String CTX_KEY_FORCE_HASH_AGGREGATION = "forceHashAggregation";
  private static final String CTX_KEY_INTERMEDIATE_COMBINE_DEGREE = "intermediateCombineDegree";
  private static final String CTX_KEY_NUM_PARALLEL_COMBINE_THREADS = "numParallelCombineThreads";
  private static final String CTX_KEY_MERGE_THREAD_LOCAL = "mergeThreadLocal";

  public static String CTESTFILEPATH = System.getProperty("user.dir").split("/druid/processing/")[0] + "/core-ctest.xml";
  public static Properties configProps = new Properties();

  // Constants for sizing merging and selector dictionaries. Rationale for these constants:
  //  1) In no case do we want total aggregate dictionary size to exceed 40% of max memory.
  //  2) In no case do we want any dictionary to exceed 1GB of memory: if heaps are giant, better to spill at
  //     "reasonable" sizes rather than get giant dictionaries. (There is probably some other reason the user
  //     wanted a giant heap, so we shouldn't monopolize it with dictionaries.)
  //  3) Use somewhat more memory for merging dictionary vs. selector dictionaries, because if a merging
  //     dictionary is full we must spill to disk, whereas if a selector dictionary is full we simply emit
  //     early to the merge buffer. So, a merging dictionary filling up has a more severe impact on
  //     query performance.
  private static final double MERGING_DICTIONARY_HEAP_FRACTION = 0.3;
  private static final double SELECTOR_DICTIONARY_HEAP_FRACTION = 0.1;
  private static final long MIN_AUTOMATIC_DICTIONARY_SIZE = 1;
  private static final long MAX_AUTOMATIC_DICTIONARY_SIZE = 1_000_000_000;

  @JsonProperty
  private String defaultStrategy = GroupByStrategySelector.STRATEGY_V2;

  @JsonProperty
  private boolean singleThreaded = false;

  @JsonProperty
  private int maxIntermediateRows = 50000;

  @JsonProperty
  private int maxResults = 500000;

  @JsonProperty
  // Not documented, only used for tests to force spilling
  private int bufferGrouperMaxSize = Integer.MAX_VALUE;

  @JsonProperty
  private float bufferGrouperMaxLoadFactor = 0;

  @JsonProperty
  private int bufferGrouperInitialBuckets = 0;

  @JsonProperty
  // Size of on-heap string dictionary for merging, per-processing-thread; when exceeded, partial results will be
  // emitted to the merge buffer early.
  private HumanReadableBytes maxSelectorDictionarySize = HumanReadableBytes.valueOf(AUTOMATIC);

  @JsonProperty
  // Size of on-heap string dictionary for merging, per-query; when exceeded, partial results will be spilled to disk
  private HumanReadableBytes maxMergingDictionarySize = HumanReadableBytes.valueOf(AUTOMATIC);

  @JsonProperty
  // Max on-disk temporary storage, per-query; when exceeded, the query fails
  private HumanReadableBytes maxOnDiskStorage = HumanReadableBytes.valueOf(0);

  @JsonProperty
  private HumanReadableBytes defaultOnDiskStorage = HumanReadableBytes.valueOf(-1);

  @JsonProperty
  private boolean forcePushDownLimit = false;

  @JsonProperty
  private boolean applyLimitPushDownToSegment = false;

  @JsonProperty
  private boolean forcePushDownNestedQuery = false;

  @JsonProperty
  private boolean forceHashAggregation = false;

  @JsonProperty
  private int intermediateCombineDegree = 8;

  @JsonProperty
  private int numParallelCombineThreads = 1;

  @JsonProperty
  private boolean mergeThreadLocal = false;

  @JsonProperty
  private boolean vectorize = true;

  @JsonProperty
  private boolean intermediateResultAsMapCompat = false;

  @JsonProperty
  private boolean enableMultiValueUnnesting = true;

  public String getDefaultStrategy()
  {
    logger.info("[CTEST][GET-PARAM] " + "druid.query.groupBy.defaultStrategy");
    try{
      configProps.load(new FileInputStream(CTESTFILEPATH));
      if(configProps.getProperty("druid.query.groupBy.defaultStrategy") != null){
        return configProps.getProperty("druid.query.groupBy.defaultStrategy");
      }
    }
    catch(IOException e){
        logger.info(CTESTFILEPATH);
    }
    return defaultStrategy;
  }

  public boolean isSingleThreaded()
  {
    logger.info("[CTEST][GET-PARAM] " + "druid.query.groupBy.singleThreaded");
    try{
      configProps.load(new FileInputStream(CTESTFILEPATH));
      if(configProps.getProperty("druid.query.groupBy.singleThreaded") != null){
        return Boolean.parseBoolean(configProps.getProperty("druid.query.groupBy.singleThreaded"));
      }
    }
    catch(IOException e){
        logger.info(CTESTFILEPATH);
    }
    return singleThreaded;
  }

  public void setSingleThreaded(boolean singleThreaded)
  {
    logger.info("[CTEST][SET-PARAM] " + "druid.query.groupBy.singleThreaded " + "NoTestTrace");
    this.singleThreaded = singleThreaded;
  }

  public int getMaxIntermediateRows()
  {
    logger.info("[CTEST][GET-PARAM] " + "druid.query.groupBy.maxIntermediateRows");
    try{
      configProps.load(new FileInputStream(CTESTFILEPATH));
      if(configProps.getProperty("druid.query.groupBy.maxIntermediateRows") != null){
        return Integer.parseInt(configProps.getProperty("druid.query.groupBy.maxIntermediateRows"));
      }
    }
    catch(IOException e){
        logger.info(CTESTFILEPATH);
    }
    return maxIntermediateRows;
  }

  public void setMaxIntermediateRows(int maxIntermediateRows)
  {
    logger.info("[CTEST][SET-PARAM] " + "druid.query.groupBy.maxIntermediateRows " + "NoTestTrace");
    this.maxIntermediateRows = maxIntermediateRows;
  }

  public int getMaxResults()
  {
    logger.info("[CTEST][GET-PARAM] " + "druid.query.groupBy.maxResults");
    try{
      configProps.load(new FileInputStream(CTESTFILEPATH));
      if(configProps.getProperty("druid.query.groupBy.maxResults") != null){
        return Integer.parseInt(configProps.getProperty("druid.query.groupBy.maxResults"));
      }
    }
    catch(IOException e){
        logger.info(CTESTFILEPATH);
    }
    return maxResults;
  }

  public void setMaxResults(int maxResults)
  {
    logger.info("[CTEST][SET-PARAM] " + "druid.query.groupBy.maxResults "+ "NoTestTrace");
    this.maxResults = maxResults;
  }

  public int getBufferGrouperMaxSize()
  {
    return bufferGrouperMaxSize;
  }

  public float getBufferGrouperMaxLoadFactor()
  {
    logger.info("[CTEST][GET-PARAM] " + "druid.query.groupBy.bufferGrouperMaxLoadFactor");
    try{
      configProps.load(new FileInputStream(CTESTFILEPATH));
      if(configProps.getProperty("druid.query.groupBy.bufferGrouperMaxLoadFactor") != null){
        return Float.parseFloat(configProps.getProperty("druid.query.groupBy.bufferGrouperMaxLoadFactor"));
      }
    }
    catch(IOException e){
        logger.info(CTESTFILEPATH);
    }
    return bufferGrouperMaxLoadFactor;
  }

  public int getBufferGrouperInitialBuckets()
  {
    logger.info("[CTEST][GET-PARAM] " + "druid.query.groupBy.bufferGrouperInitialBuckets");
    try{
      configProps.load(new FileInputStream(CTESTFILEPATH));
      if(configProps.getProperty("druid.query.groupBy.bufferGrouperInitialBuckets") != null){
        return Integer.parseInt(configProps.getProperty("druid.query.groupBy.bufferGrouperInitialBuckets"));
      }
    }
    catch(IOException e){
        logger.info(CTESTFILEPATH);
    }
    return bufferGrouperInitialBuckets;
  }

  /**
   * For unit tests. Production code should use {@link #getActualMaxSelectorDictionarySize}.
   */
  long getConfiguredMaxSelectorDictionarySize()
  {
    logger.info("[CTEST][GET-PARAM] " + "druid.query.groupBy.maxSelectorDictionarySize");
    try{
      configProps.load(new FileInputStream(CTESTFILEPATH));
      if(configProps.getProperty("druid.query.groupBy.maxSelectorDictionarySize") != null){
        return Long.parseLong(configProps.getProperty("druid.query.groupBy.maxSelectorDictionarySize"));
      }
    }
    catch(IOException e){
        logger.info(CTESTFILEPATH);
    }
    return maxSelectorDictionarySize.getBytes();
  }

  /**
   * For unit tests. Production code should use {@link #getActualMaxSelectorDictionarySize}.
   */
  long getActualMaxSelectorDictionarySize(final long maxHeapSize, final int numConcurrentQueries)
  {
    if (maxSelectorDictionarySize.getBytes() == AUTOMATIC) {
      final long heapForDictionaries = (long) (maxHeapSize * SELECTOR_DICTIONARY_HEAP_FRACTION);

      return Math.max(
          MIN_AUTOMATIC_DICTIONARY_SIZE,
          Math.min(
              MAX_AUTOMATIC_DICTIONARY_SIZE,
              heapForDictionaries / numConcurrentQueries
          )
      );
    } else {
      return maxSelectorDictionarySize.getBytes();
    }
  }

  public long getActualMaxSelectorDictionarySize(final DruidProcessingConfig processingConfig)
  {
    return getActualMaxSelectorDictionarySize(
        JvmUtils.getRuntimeInfo().getMaxHeapSizeBytes(),

        // numMergeBuffers is the number of groupBy queries that can run simultaneously
        processingConfig.getNumMergeBuffers()
    );
  }

  /**
   * For unit tests. Production code should use {@link #getActualMaxMergingDictionarySize}.
   */
  long getConfiguredMaxMergingDictionarySize()
  {
    logger.info("[CTEST][GET-PARAM] " + "druid.query.groupBy.maxMergingDictionarySize");
    try{
      configProps.load(new FileInputStream(CTESTFILEPATH));
      if(configProps.getProperty("druid.query.groupBy.maxMergingDictionarySize") != null){
        return Long.parseLong(configProps.getProperty("druid.query.groupBy.maxMergingDictionarySize"));
      }
    }
    catch(IOException e){
        logger.info(CTESTFILEPATH);
    }
    return maxMergingDictionarySize.getBytes();
  }

  /**
   * For unit tests. Production code should use {@link #getActualMaxMergingDictionarySize}.
   */
  public long getActualMaxMergingDictionarySize(final long maxHeapSize, final int numConcurrentQueries)
  {
    if (maxMergingDictionarySize.getBytes() == AUTOMATIC) {
      final long heapForDictionaries = (long) (maxHeapSize * MERGING_DICTIONARY_HEAP_FRACTION);

      return Math.max(
          MIN_AUTOMATIC_DICTIONARY_SIZE,
          Math.min(
              MAX_AUTOMATIC_DICTIONARY_SIZE,
              heapForDictionaries / numConcurrentQueries
          )
      );
    } else {
      return maxMergingDictionarySize.getBytes();
    }
  }

  public long getActualMaxMergingDictionarySize(final DruidProcessingConfig processingConfig)
  {
    return getActualMaxMergingDictionarySize(
        JvmUtils.getRuntimeInfo().getMaxHeapSizeBytes(),

        // numMergeBuffers is the number of groupBy queries that can run simultaneously
        processingConfig.getNumMergeBuffers()
    );
  }

  public HumanReadableBytes getMaxOnDiskStorage()
  {
    logger.info("[CTEST][GET-PARAM] " + "druid.query.groupBy.maxOnDiskStorage");
    return maxOnDiskStorage;
  }

  /**
   * Mirror maxOnDiskStorage if defaultOnDiskStorage's default is not overridden by cluster operator.
   *
   * This mirroring is done to maintain continuity in behavior between Druid versions. If an operator wants to use
   * defaultOnDiskStorage, they have to explicitly override it.
   *
   * @return The working value for defaultOnDiskStorage
   */
  public HumanReadableBytes getDefaultOnDiskStorage()
  {
    logger.info("[CTEST][GET-PARAM] " + "druid.query.groupBy.defaultOnDiskStorage");
    return defaultOnDiskStorage.getBytes() < 0L ? getMaxOnDiskStorage() : defaultOnDiskStorage;
  }

  public boolean isForcePushDownLimit()
  {
    return forcePushDownLimit;
  }

  public boolean isApplyLimitPushDownToSegment()
  {
    return applyLimitPushDownToSegment;
  }

  public boolean isForceHashAggregation()
  {
    logger.info("[CTEST][GET-PARAM] " + "druid.query.groupBy.forceHashAggregation");
    try{
      configProps.load(new FileInputStream(CTESTFILEPATH));
      if(configProps.getProperty("druid.query.groupBy.forceHashAggregation") != null){
        return Boolean.parseBoolean(configProps.getProperty("druid.query.groupBy.forceHashAggregation"));
      }
    }
    catch(IOException e){
        logger.info(CTESTFILEPATH);
    }
    return forceHashAggregation;
  }

  public int getIntermediateCombineDegree()
  {
    logger.info("[CTEST][GET-PARAM] " + "druid.query.groupBy.intermediateCombineDegree");
    try{
      configProps.load(new FileInputStream(CTESTFILEPATH));
      if(configProps.getProperty("druid.query.groupBy.intermediateCombineDegree") != null){
        return Integer.parseInt(configProps.getProperty("druid.query.groupBy.intermediateCombineDegree"));
      }
    }
    catch(IOException e){
        logger.info(CTESTFILEPATH);
    }
    return intermediateCombineDegree;
  }

  public int getNumParallelCombineThreads()
  {
    logger.info("[CTEST][GET-PARAM] " + "druid.query.groupBy.numParallelCombineThreads");
    try{
      configProps.load(new FileInputStream(CTESTFILEPATH));
      if(configProps.getProperty("druid.query.groupBy.numParallelCombineThreads") != null){
        return Integer.parseInt(configProps.getProperty("druid.query.groupBy.numParallelCombineThreads"));
      }
    }
    catch(IOException e){
        logger.info(CTESTFILEPATH);
    }
    return numParallelCombineThreads;
  }

  public boolean isMergeThreadLocal()
  {
    return mergeThreadLocal;
  }

  public boolean isVectorize()
  {
    return vectorize;
  }

  public boolean isIntermediateResultAsMapCompat()
  {
    return intermediateResultAsMapCompat;
  }

  public boolean isForcePushDownNestedQuery()
  {
    return forcePushDownNestedQuery;
  }

  public boolean isMultiValueUnnestingEnabled()
  {
    return enableMultiValueUnnesting;
  }

  public GroupByQueryConfig withOverrides(final GroupByQuery query)
  {
    final GroupByQueryConfig newConfig = new GroupByQueryConfig();
    newConfig.defaultStrategy = query.getContextValue(CTX_KEY_STRATEGY, getDefaultStrategy());
    newConfig.singleThreaded = query.getContextBoolean(CTX_KEY_IS_SINGLE_THREADED, isSingleThreaded());
    newConfig.maxIntermediateRows = Math.min(
        query.getContextValue(CTX_KEY_MAX_INTERMEDIATE_ROWS, getMaxIntermediateRows()),
        getMaxIntermediateRows()
    );
    newConfig.maxResults = Math.min(
        query.getContextValue(CTX_KEY_MAX_RESULTS, getMaxResults()),
        getMaxResults()
    );
    newConfig.bufferGrouperMaxSize = Math.min(
        query.getContextValue(CTX_KEY_BUFFER_GROUPER_MAX_SIZE, getBufferGrouperMaxSize()),
        getBufferGrouperMaxSize()
    );
    newConfig.bufferGrouperMaxLoadFactor = query.getContextValue(
        CTX_KEY_BUFFER_GROUPER_MAX_LOAD_FACTOR,
        getBufferGrouperMaxLoadFactor()
    );
    newConfig.bufferGrouperInitialBuckets = query.getContextValue(
        CTX_KEY_BUFFER_GROUPER_INITIAL_BUCKETS,
        getBufferGrouperInitialBuckets()
    );
    // If the client overrides do not provide "maxOnDiskStorage" context key, the server side "defaultOnDiskStorage"
    // value is used in the calculation of the newConfig value of maxOnDiskStorage. This allows the operator to
    // choose a default value lower than the max allowed when the context key is missing in the client query.
    newConfig.maxOnDiskStorage = HumanReadableBytes.valueOf(
        Math.min(
            query.getContextHumanReadableBytes(CTX_KEY_MAX_ON_DISK_STORAGE, getDefaultOnDiskStorage()).getBytes(),
            getMaxOnDiskStorage().getBytes()
        )
    );
    newConfig.maxSelectorDictionarySize = maxSelectorDictionarySize; // No overrides
    newConfig.maxMergingDictionarySize = maxMergingDictionarySize; // No overrides
    newConfig.forcePushDownLimit = query.getContextBoolean(CTX_KEY_FORCE_LIMIT_PUSH_DOWN, isForcePushDownLimit());
    newConfig.applyLimitPushDownToSegment = query.getContextBoolean(
        CTX_KEY_APPLY_LIMIT_PUSH_DOWN_TO_SEGMENT,
        isApplyLimitPushDownToSegment()
    );
    newConfig.forceHashAggregation = query.getContextBoolean(CTX_KEY_FORCE_HASH_AGGREGATION, isForceHashAggregation());
    newConfig.forcePushDownNestedQuery = query.getContextBoolean(
        CTX_KEY_FORCE_PUSH_DOWN_NESTED_QUERY,
        isForcePushDownNestedQuery()
    );
    newConfig.intermediateCombineDegree = query.getContextValue(
        CTX_KEY_INTERMEDIATE_COMBINE_DEGREE,
        getIntermediateCombineDegree()
    );
    newConfig.numParallelCombineThreads = query.getContextValue(
        CTX_KEY_NUM_PARALLEL_COMBINE_THREADS,
        getNumParallelCombineThreads()
    );
    newConfig.mergeThreadLocal = query.getContextBoolean(CTX_KEY_MERGE_THREAD_LOCAL, isMergeThreadLocal());
    newConfig.vectorize = query.getContextBoolean(QueryContexts.VECTORIZE_KEY, isVectorize());
    newConfig.enableMultiValueUnnesting = query.getContextBoolean(
        CTX_KEY_ENABLE_MULTI_VALUE_UNNESTING,
        isMultiValueUnnestingEnabled()
    );

    logger.debug("Override config for GroupBy query %s - %s", query.getId(), newConfig.toString());
    return newConfig;
  }

  @Override
  public String toString()
  {
    return "GroupByQueryConfig{" +
           "defaultStrategy='" + defaultStrategy + '\'' +
           ", singleThreaded=" + singleThreaded +
           ", maxIntermediateRows=" + maxIntermediateRows +
           ", maxResults=" + maxResults +
           ", bufferGrouperMaxSize=" + bufferGrouperMaxSize +
           ", bufferGrouperMaxLoadFactor=" + bufferGrouperMaxLoadFactor +
           ", bufferGrouperInitialBuckets=" + bufferGrouperInitialBuckets +
           ", maxMergingDictionarySize=" + maxMergingDictionarySize +
           ", maxOnDiskStorage=" + maxOnDiskStorage.getBytes() +
           ", defaultOnDiskStorage=" + getDefaultOnDiskStorage().getBytes() + // use the getter because of special behavior for mirroring maxOnDiskStorage if defaultOnDiskStorage not explicitly set.
           ", forcePushDownLimit=" + forcePushDownLimit +
           ", forceHashAggregation=" + forceHashAggregation +
           ", intermediateCombineDegree=" + intermediateCombineDegree +
           ", numParallelCombineThreads=" + numParallelCombineThreads +
           ", vectorize=" + vectorize +
           ", forcePushDownNestedQuery=" + forcePushDownNestedQuery +
           ", enableMultiValueUnnesting=" + enableMultiValueUnnesting +
           '}';
  }
}
