package com.dream11.logcentralorchestrator.tests.unit.common;

import com.dream11.logcentralorchestrator.common.util.CompletableUtils;
import com.dream11.logcentralorchestrator.common.util.MaybeUtils;
import com.dream11.logcentralorchestrator.common.util.SingleUtils;
import io.reactivex.Completable;
import io.reactivex.Maybe;
import io.reactivex.Single;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.Test;

/** Unit tests for MaybeUtils, SingleUtils, and CompletableUtils. */
public class ReactiveUtilsTest {

  private static final Logger log = LoggerFactory.getLogger(ReactiveUtilsTest.class);

  // ========== MaybeUtils Tests ==========

  @Test
  public void testMaybeUtils_ReadThroughCache_WithCacheHit_ReturnsCachedValue() {
    // Arrange
    Maybe<String> cacheHit = Maybe.just("cached-value");
    Maybe<String> source = Maybe.just("source-value");

    // Act
    Maybe<String> result =
        MaybeUtils.readThroughCache(cacheHit, source, data -> Completable.complete());

    // Assert
    String value = result.blockingGet();
    Assert.assertEquals(value, "cached-value");
  }

  @Test
  public void testMaybeUtils_ReadThroughCache_WithCacheMiss_ReturnsSourceValue() {
    // Arrange
    Maybe<String> cacheMiss = Maybe.empty();
    Maybe<String> source = Maybe.just("source-value");

    // Act
    Maybe<String> result =
        MaybeUtils.readThroughCache(cacheMiss, source, data -> Completable.complete());

    // Assert
    String value = result.blockingGet();
    Assert.assertEquals(value, "source-value");
  }

  @Test
  public void testMaybeUtils_ApplyDebugLogs_WithLogPrefix_ReturnsTransformer() {
    // Arrange
    Maybe<String> maybe = Maybe.just("test-value");

    // Act
    Maybe<String> result = maybe.compose(MaybeUtils.applyDebugLogs(log, "TestPrefix"));

    // Assert
    String value = result.blockingGet();
    Assert.assertEquals(value, "test-value");
  }

  @Test
  public void testMaybeUtils_ApplyDebugLogs_WithoutLogPrefix_ReturnsTransformer() {
    // Arrange
    Maybe<String> maybe = Maybe.just("test-value");

    // Act
    Maybe<String> result = maybe.compose(MaybeUtils.applyDebugLogs(log));

    // Assert
    String value = result.blockingGet();
    Assert.assertEquals(value, "test-value");
  }

  // ========== SingleUtils Tests ==========

  @Test
  public void testSingleUtils_ReadThroughCache_WithCacheHit_ReturnsCachedValue() {
    // Arrange
    Maybe<String> cacheHit = Maybe.just("cached-value");
    Single<String> source = Single.just("source-value");

    // Act
    Single<String> result =
        SingleUtils.readThroughCache(cacheHit, source, data -> Completable.complete());

    // Assert
    String value = result.blockingGet();
    Assert.assertEquals(value, "cached-value");
  }

  @Test
  public void testSingleUtils_ReadThroughCache_WithCacheMiss_ReturnsSourceValue() {
    // Arrange
    Maybe<String> cacheMiss = Maybe.empty();
    Single<String> source = Single.just("source-value");

    // Act
    Single<String> result =
        SingleUtils.readThroughCache(cacheMiss, source, data -> Completable.complete());

    // Assert
    String value = result.blockingGet();
    Assert.assertEquals(value, "source-value");
  }

  @Test
  public void testSingleUtils_ApplyDebugLogs_WithLogPrefix_ReturnsTransformer() {
    // Arrange
    Single<String> single = Single.just("test-value");

    // Act
    Single<String> result = single.compose(SingleUtils.applyDebugLogs(log, "TestPrefix"));

    // Assert
    String value = result.blockingGet();
    Assert.assertEquals(value, "test-value");
  }

  @Test
  public void testSingleUtils_ApplyDebugLogs_WithoutLogPrefix_ReturnsTransformer() {
    // Arrange
    Single<String> single = Single.just("test-value");

    // Act
    Single<String> result = single.compose(SingleUtils.applyDebugLogs(log));

    // Assert
    String value = result.blockingGet();
    Assert.assertEquals(value, "test-value");
  }

  // ========== CompletableUtils Tests ==========

  @Test
  public void testCompletableUtils_ApplyDebugLogs_WithLogPrefix_ReturnsTransformer() {
    // Arrange
    Completable completable = Completable.complete();

    // Act
    Completable result = completable.compose(CompletableUtils.applyDebugLogs(log, "TestPrefix"));

    // Assert
    result.blockingAwait(); // Should complete without error
  }

  @Test
  public void testCompletableUtils_ApplyDebugLogs_WithoutLogPrefix_ReturnsTransformer() {
    // Arrange
    Completable completable = Completable.complete();

    // Act
    Completable result = completable.compose(CompletableUtils.applyDebugLogs(log));

    // Assert
    result.blockingAwait(); // Should complete without error
  }

  @Test
  public void testCompletableUtils_ApplyDebugLogs_WithError_LogsError() {
    // Arrange
    RuntimeException error = new RuntimeException("Test error");
    Completable completable = Completable.error(error);

    // Act
    Completable result = completable.compose(CompletableUtils.applyDebugLogs(log, "TestPrefix"));

    // Assert
    try {
      result.blockingAwait();
      Assert.fail("Should have thrown exception");
    } catch (RuntimeException e) {
      Assert.assertEquals(e.getMessage(), "Test error");
    }
  }
}
