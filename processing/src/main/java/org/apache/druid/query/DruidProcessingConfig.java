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

package org.apache.druid.query;

import org.apache.druid.java.util.common.HumanReadableBytes;
import org.apache.druid.java.util.common.IAE;
import org.apache.druid.java.util.common.concurrent.ExecutorServiceConfig;
import org.apache.druid.java.util.common.guava.ParallelMergeCombiningSequence;
import org.apache.druid.java.util.common.logger.Logger;
import org.apache.druid.segment.column.ColumnConfig;
import org.apache.druid.utils.JvmUtils;
import org.skife.config.Config;


import java.util.concurrent.atomic.AtomicReference;
import java.util.Properties;
import java.io.FileInputStream;
import java.io.IOException;

public abstract class DruidProcessingConfig extends ExecutorServiceConfig implements ColumnConfig
{
  private static final Logger log = new Logger(DruidProcessingConfig.class);

  public static final int DEFAULT_NUM_MERGE_BUFFERS = -1;
  public static final HumanReadableBytes DEFAULT_PROCESSING_BUFFER_SIZE_BYTES = HumanReadableBytes.valueOf(-1);
  public static final int MAX_DEFAULT_PROCESSING_BUFFER_SIZE_BYTES = 1024 * 1024 * 1024;
  public static final int DEFAULT_MERGE_POOL_AWAIT_SHUTDOWN_MILLIS = 60_000;
  public static final int DEFAULT_INITIAL_BUFFERS_FOR_INTERMEDIATE_POOL = 0;

  public static String CTESTFILEPATH = System.getProperty("user.dir").split("/druid/processing/")[0] + "/core-ctest.xml";
  public static Properties configProps = new Properties();
    
  private AtomicReference<Integer> computedBufferSizeBytes = new AtomicReference<>();

  @Config({"druid.computation.buffer.size", "${base_path}.buffer.sizeBytes"})
  public HumanReadableBytes intermediateComputeSizeBytesConfigured()
  { 
    return DEFAULT_PROCESSING_BUFFER_SIZE_BYTES;
  }

  public int intermediateComputeSizeBytes()
  {
    HumanReadableBytes sizeBytesConfigured = intermediateComputeSizeBytesConfigured();
    if (!DEFAULT_PROCESSING_BUFFER_SIZE_BYTES.equals(sizeBytesConfigured)) {
      log.info("[CTEST][SET-PARAM] " + "druid.processing.buffer.sizeBytes "+"TestNoTrace");
      log.info("[CTEST][GET-PARAM] " + "druid.processing.buffer.sizeBytes");
      if (sizeBytesConfigured.getBytes() > Integer.MAX_VALUE) {
        throw new IAE("druid.processing.buffer.sizeBytes must be less than 2GiB");
      }
      return sizeBytesConfigured.getBytesInInt();
    } else if (computedBufferSizeBytes.get() != null) {
      log.info("[CTEST][GET-PARAM] " + "druid.processing.buffer.sizeBytes");
      return computedBufferSizeBytes.get();
    }

    long directSizeBytes;
    try {
      directSizeBytes = JvmUtils.getRuntimeInfo().getDirectMemorySizeBytes();
      log.info(
          "Detected max direct memory size of [%,d] bytes",
          directSizeBytes
      );
    }
    catch (UnsupportedOperationException e) {
      // max direct memory defaults to max heap size on recent JDK version, unless set explicitly
      directSizeBytes = computeMaxMemoryFromMaxHeapSize();
      log.info("Using up to [%,d] bytes of direct memory for computation buffers.", directSizeBytes);
    }

    int numProcessingThreads = getNumThreads();
    int numMergeBuffers = getNumMergeBuffers();
    int totalNumBuffers = numMergeBuffers + numProcessingThreads;
    int sizePerBuffer = (int) ((double) directSizeBytes / (double) (totalNumBuffers + 1));

    final int computedSizePerBuffer = Math.min(sizePerBuffer, MAX_DEFAULT_PROCESSING_BUFFER_SIZE_BYTES);
    if (computedBufferSizeBytes.compareAndSet(null, computedSizePerBuffer)) {
      log.info(
          "Auto sizing buffers to [%,d] bytes each for [%,d] processing and [%,d] merge buffers. "
          + "If you run out of direct memory, you may need to set these parameters explicitly using the guidelines at "
          + "https://druid.apache.org/docs/latest/operations/basic-cluster-tuning.html#processing-threads-buffers.",
          computedSizePerBuffer,
          numProcessingThreads,
          numMergeBuffers
      );
    }

    log.info("[CTEST][GET-PARAM] " + "druid.processing.buffer.sizeBytes");
    return computedSizePerBuffer;
  }

  public static long computeMaxMemoryFromMaxHeapSize()
  {
    return Runtime.getRuntime().maxMemory() / 4;
  }

  @Config({"druid.computation.buffer.poolCacheMaxCount", "${base_path}.buffer.poolCacheMaxCount"})
  public int poolCacheMaxCount()
  {
    log.info("[CTEST][GET-PARAM] " + "druid.processing.buffer.poolCacheMaxCount");
    try{
      configProps.load(new FileInputStream(CTESTFILEPATH));
      if(configProps.getProperty("druid.processing.buffer.poolCacheMaxCount") != null){
        return Integer.parseInt(configProps.getProperty("druid.processing.buffer.poolCacheMaxCount"));
      }
    }
    catch(IOException e){
        log.info(CTESTFILEPATH);
    }
    return Integer.MAX_VALUE;
  }

  @Config({
      "druid.computation.buffer.poolCacheInitialCount",
      "${base_path}.buffer.poolCacheInitialCount"
  })
  public int getNumInitalBuffersForIntermediatePool()
  {
    log.info("[CTEST][GET-PARAM] " + "druid.processing.buffer.poolCacheInitialCount" + DEFAULT_INITIAL_BUFFERS_FOR_INTERMEDIATE_POOL);
    try{
      configProps.load(new FileInputStream(CTESTFILEPATH));
      if(configProps.getProperty("druid.processing.buffer.poolCacheInitialCount") != null){
        return Integer.parseInt(configProps.getProperty("druid.processing.buffer.poolCacheInitialCount"));
      }
    }
    catch(IOException e){
        log.info(CTESTFILEPATH);
    }
    return DEFAULT_INITIAL_BUFFERS_FOR_INTERMEDIATE_POOL;
  }

  @Override
  @Config(value = "${base_path}.numThreads")
  public int getNumThreadsConfigured()
  {
    log.info("[CTEST][GET-PARAM] " + "druid.processing.numThreads");
    try{
      configProps.load(new FileInputStream(CTESTFILEPATH));
      if(configProps.getProperty("druid.processing.numThreads") != null){
        return Integer.parseInt(configProps.getProperty("druid.processing.numThreads"));
      }
    }
    catch(IOException e){
        log.info(CTESTFILEPATH);
    }
    return DEFAULT_NUM_THREADS;
  }

  public int getNumMergeBuffers()
  {
    int numMergeBuffersConfigured = getNumMergeBuffersConfigured();
    
    if (numMergeBuffersConfigured != DEFAULT_NUM_MERGE_BUFFERS) {
      log.info("[CTEST][SET-PARAM] " + "druid.processing.numMergeBuffers "+"TestNoTrace");
      log.info("[CTEST][GET-PARAM] " + "druid.processing.numMergeBuffers");
      return numMergeBuffersConfigured;
    } else {
      log.info("[CTEST][GET-PARAM] " + "druid.processing.numMergeBuffers");
      return Math.max(2, getNumThreads() / 4);
    }
  }

  /**
   * Returns the number of merge buffers _explicitly_ configured, or -1 if it is not explicitly configured, that is not
   * a valid number of buffers. To get the configured value or the default (valid) number, use {@link
   * #getNumMergeBuffers()}. This method exists for ability to distinguish between the default value set when there is
   * no explicit config, and an explicitly configured value.
   */
  @Config("${base_path}.numMergeBuffers")
  public int getNumMergeBuffersConfigured()
  { 
    try{
      configProps.load(new FileInputStream(CTESTFILEPATH));
      if(configProps.getProperty("druid.processing.numMergeBuffers") != null){
        return Integer.parseInt(configProps.getProperty("druid.processing.numMergeBuffers"));
      }
    }
    catch(IOException e){
        log.info(CTESTFILEPATH);
    }
    return DEFAULT_NUM_MERGE_BUFFERS;
  }

  @Override
  @Config(value = "${base_path}.columnCache.sizeBytes")
  public int columnCacheSizeBytes()
  {
    log.info("[CTEST][GET-PARAM] " + "druid.processing.columnCache.sizeBytes");
    try{
      configProps.load(new FileInputStream(CTESTFILEPATH));
      if(configProps.getProperty("druid.processing.columnCache.sizeBytes") != null){
        return Integer.parseInt(configProps.getProperty("druid.processing.columnCache.sizeBytes"));
      }
    }
    catch(IOException e){
        log.info(CTESTFILEPATH);
    }
    return 0;
  }

  @Config(value = "${base_path}.fifo")
  public boolean isFifo()
  {
    log.info("[CTEST][GET-PARAM] " + "druid.processing.fifo");
    try{
      configProps.load(new FileInputStream(CTESTFILEPATH));
      if(configProps.getProperty("druid.processing.fifo") != null){
        return Boolean.parseBoolean(configProps.getProperty("druid.processing.fifo"));
      }
    }
    catch(IOException e){
        log.info(CTESTFILEPATH);
    }
    return true;
  }

  @Config(value = "${base_path}.tmpDir")
  public String getTmpDir()
  {
    log.info("[CTEST][GET-PARAM] " + "druid.processing.tmpDir");
    try{
      configProps.load(new FileInputStream(CTESTFILEPATH));
      if(configProps.getProperty("druid.processing.tmpDir") != null){
        return configProps.getProperty("druid.processing.tmpDir");
      }
    }
    catch(IOException e){
        log.info(CTESTFILEPATH);
    }
    return System.getProperty("java.io.tmpdir");
  }

  @Config(value = "${base_path}.merge.useParallelMergePool")
  public boolean useParallelMergePoolConfigured()
  {
    log.info("[CTEST][GET-PARAM] " + "druid.processing.merge.useParallelMergePool");
    try{
      configProps.load(new FileInputStream(CTESTFILEPATH));
      if(configProps.getProperty("druid.processing.merge.useParallelMergePool") != null){
        return Boolean.parseBoolean(configProps.getProperty("druid.processing.merge.useParallelMergePool"));
      }
    }
    catch(IOException e){
        log.info(CTESTFILEPATH);
    }
    return true;
  }

  public boolean useParallelMergePool()
  {
    final boolean useParallelMergePoolConfigured = useParallelMergePoolConfigured();
    final int parallelism = getMergePoolParallelism();
    // need at least 3 to do 2 layer merge
    if (parallelism > 2) {
      return useParallelMergePoolConfigured;
    }
    if (useParallelMergePoolConfigured) {
      log.debug(
          "Parallel merge pool is enabled, but there are not enough cores to enable parallel merges: %s",
          parallelism
      );
    }
    return false;
  }

  @Config(value = "${base_path}.merge.pool.parallelism")
  public int getMergePoolParallelismConfigured()
  { 
    log.info("[CTEST][GET-PARAM] " + "druid.processing.merge.pool.parallelism");
    try{
      configProps.load(new FileInputStream(CTESTFILEPATH));
      if(configProps.getProperty("druid.processing.merge.pool.parallelism") != null){
        return Integer.parseInt(configProps.getProperty("druid.processing.merge.pool.parallelism"));
      }
    }
    catch(IOException e){
        log.info(CTESTFILEPATH);
    }
    return DEFAULT_NUM_THREADS;
  }

  public int getMergePoolParallelism()
  {
    int poolParallelismConfigured = getMergePoolParallelismConfigured();
    if (poolParallelismConfigured != DEFAULT_NUM_THREADS) {
      return poolParallelismConfigured;
    } else {
      // assume 2 hyper-threads per core, so that this value is probably by default the number of physical cores * 1.5
      return (int) Math.ceil(JvmUtils.getRuntimeInfo().getAvailableProcessors() * 0.75);
    }
  }

  @Config(value = "${base_path}.merge.pool.awaitShutdownMillis")
  public long getMergePoolAwaitShutdownMillis()
  {
    log.info("[CTEST][GET-PARAM] " + "druid.processing.merge.pool.awaitShutdownMillis");
    try{
      configProps.load(new FileInputStream(CTESTFILEPATH));
      if(configProps.getProperty("druid.processing.merge.pool.awaitShutdownMillis") != null){
        return Long.parseLong(configProps.getProperty("druid.processing.merge.pool.awaitShutdownMillis"));
      }
    }
    catch(IOException e){
        log.info(CTESTFILEPATH);
    }
    return DEFAULT_MERGE_POOL_AWAIT_SHUTDOWN_MILLIS;
  }

  @Config(value = "${base_path}.merge.pool.defaultMaxQueryParallelism")
  public int getMergePoolDefaultMaxQueryParallelism()
  {
    // assume 2 hyper-threads per core, so that this value is probably by default the number of physical cores
    log.info("[CTEST][GET-PARAM] " + "druid.processing.merge.pool.defaultMaxQueryParallelism");
    try{
      configProps.load(new FileInputStream(CTESTFILEPATH));
      if(configProps.getProperty("druid.processing.merge.pool.defaultMaxQueryParallelism") != null){
        return Integer.parseInt(configProps.getProperty("druid.processing.merge.pool.defaultMaxQueryParallelism"));
      }
    }
    catch(IOException e){
        log.info(CTESTFILEPATH);
    }
    return (int) Math.max(JvmUtils.getRuntimeInfo().getAvailableProcessors() * 0.5, 1);
  }

  @Config(value = "${base_path}.merge.task.targetRunTimeMillis")
  public int getMergePoolTargetTaskRunTimeMillis()
  {
    log.info("[CTEST][GET-PARAM] " + "druid.processing.merge.task.targetRunTimeMillis");
    try{
      configProps.load(new FileInputStream(CTESTFILEPATH));
      if(configProps.getProperty("druid.processing.merge.task.targetRunTimeMillis") != null){
        return Integer.parseInt(configProps.getProperty("druid.processing.merge.task.targetRunTimeMillis"));
      }
    }
    catch(IOException e){
        log.info(CTESTFILEPATH);
    }
    return ParallelMergeCombiningSequence.DEFAULT_TASK_TARGET_RUN_TIME_MILLIS;
  }

  @Config(value = "${base_path}.merge.task.initialYieldNumRows")
  public int getMergePoolTaskInitialYieldRows()
  {
    log.info("[CTEST][GET-PARAM] " + "druid.processing.merge.task.initialYieldNumRows");
    try{
      configProps.load(new FileInputStream(CTESTFILEPATH));
      if(configProps.getProperty("druid.processing.merge.task.initialYieldNumRows") != null){
        return Integer.parseInt(configProps.getProperty("druid.processing.merge.task.initialYieldNumRows"));
      }
    }
    catch(IOException e){
        log.info(CTESTFILEPATH);
    }
    return ParallelMergeCombiningSequence.DEFAULT_TASK_INITIAL_YIELD_NUM_ROWS;
  }

  @Config(value = "${base_path}.merge.task.smallBatchNumRows")
  public int getMergePoolSmallBatchRows()
  {
    log.info("[CTEST][GET-PARAM] " + "druid.processing.merge.task.smallBatchNumRows");
    try{
      configProps.load(new FileInputStream(CTESTFILEPATH));
      if(configProps.getProperty("druid.processing.merge.task.targetRunTimeMillis") != null){
        return Integer.parseInt(configProps.getProperty("druid.processing.merge.task.smallBatchNumRows"));
      }
    }
    catch(IOException e){
        log.info(CTESTFILEPATH);
    }
    return ParallelMergeCombiningSequence.DEFAULT_TASK_SMALL_BATCH_NUM_ROWS;
  }
}

