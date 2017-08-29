/**
 * Written by Gil Tene of Azul Systems, and released to the public domain,
 * as explained at http://creativecommons.org/publicdomain/zero/1.0/
 *
 * @author Gil Tene
 */

package org.HdrHistogram;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.nio.ByteBuffer;
import java.nio.LongBuffer;
import java.util.concurrent.atomic.AtomicLongArray;
import java.util.concurrent.atomic.AtomicLongFieldUpdater;
import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.concurrent.atomic.LongAccumulator;
import java.util.concurrent.atomic.LongAdder;
import java.util.zip.DataFormatException;

/**
 * <h3>A High Dynamic Range (HDR) Histogram using atomic <b><code>long</code></b> count type </h3>
 * An AtomicHistogram guarantees lossless recording of values into the histogram even when the
 * histogram is updated by multiple threads. It is important to note though that this lossless
 * recording capability is the only thread-safe behavior provided by AtomicHistogram, and that it
 * is not otherwise synchronized. Specifically, AtomicHistogram does not support auto-resizing,
 * does not support value shift operations, and provides no implicit synchronization
 * that would prevent the contents of the histogram from changing during iterations, copies, or
 * addition operations on the histogram. Callers wishing to make potentially concurrent,
 * multi-threaded updates that would safely work in the presence of queries, copies, or additions
 * of histogram objects should either take care to externally synchronize and/or order their access,
 * use the {@link org.HdrHistogram.SynchronizedHistogram} variant, or (recommended) use the
 * {@link Recorder} class, which is intended for this purpose.
 * <p>
 * See package description for {@link org.HdrHistogram} for details.
 */

public class ActionHistogram extends Histogram {

//    volatile long totalCount;
//    volatile AtomicLongArray counts;

    volatile LongAdder totalCount = new LongAdder();
    volatile AtomicReferenceArray<LongAdder> counts;

    @Override
    long getCountAtIndex(final int index) {
        return counts.get(index).sum();
    }

    @Override
    long getCountAtNormalizedIndex(final int index) {
        return counts.get(index).sum();
    }

    @Override
    void incrementCountAtIndex(final int index) {
        counts.get(index).increment();
    }

    @Override
    void addToCountAtIndex(final int index, final long value) {
        counts.get(index).add(value);
    }

    @Override
    void setCountAtIndex(int index, long value) {
        final LongAdder l = new LongAdder();
        l.add(value);
        counts.set(index, l);
    }

    @Override
    void setCountAtNormalizedIndex(int index, long value) {
        final LongAdder l = new LongAdder();
        l.add(value);
        counts.set(index, l);
    }

    @Override
    int getNormalizingIndexOffset() {
        return 0;
    }

    @Override
    void setNormalizingIndexOffset(int normalizingIndexOffset) {
        if (normalizingIndexOffset != 0) {
            throw new IllegalStateException(
                    "AtomicHistogram does not support non-zero normalizing index settings." +
                            " Use ConcurrentHistogram Instead.");
        }
    }

    @Override
    void shiftNormalizingIndexByOffset(int offsetToAdd, boolean lowestHalfBucketPopulated) {
        throw new IllegalStateException(
                "AtomicHistogram does not support Shifting operations." +
                        " Use ConcurrentHistogram Instead.");
    }

    @Override
    void resize(long newHighestTrackableValue) {
        throw new IllegalStateException(
                "AtomicHistogram does not support resizing operations." +
                        " Use ConcurrentHistogram Instead.");
    }

    @Override
    public void setAutoResize(boolean autoResize) {
        throw new IllegalStateException(
                "AtomicHistogram does not support AutoResize operation." +
                        " Use ConcurrentHistogram Instead.");
    }

    @Override
    void clearCounts() {
        for (int i = 0; i < counts.length(); i++) {
            counts.get(i).reset();
//            counts.lazySet(i, new LongAdder());
        }
        totalCount.reset();
    }

    @Override
    public ActionHistogram copy() {
        ActionHistogram copy = new ActionHistogram(this);
        copy.add(this);
        return copy;
    }

    @Override
    public ActionHistogram copyCorrectedForCoordinatedOmission(final long expectedIntervalBetweenValueSamples) {
        ActionHistogram toHistogram = new ActionHistogram(this);
        toHistogram.addWhileCorrectingForCoordinatedOmission(this, expectedIntervalBetweenValueSamples);
        return toHistogram;
    }

    @Override
    public long getTotalCount() {
        return totalCount == null ? 0 : totalCount.sum();
    }

    @Override
    void setTotalCount(final long totalCount) {
        this.totalCount = new LongAdder();
        this.totalCount.add(totalCount);
    }

    @Override
    void incrementTotalCount() {
        totalCount.increment();
    }

    @Override
    void addToTotalCount(final long value) {
        totalCount.add(value);
    }

    @Override
    int _getEstimatedFootprintInBytes() {
        return (512 + (8 * counts.length()));
    }

    /**
     * Construct a AtomicHistogram given the Highest value to be tracked and a number of significant decimal digits. The
     * histogram will be constructed to implicitly track (distinguish from 0) values as low as 1.
     *
     * @param highestTrackableValue The highest value to be tracked by the histogram. Must be a positive
     *                              integer that is {@literal >=} 2.
     * @param numberOfSignificantValueDigits Specifies the precision to use. This is the number of significant
     *                                       decimal digits to which the histogram will maintain value resolution
     *                                       and separation. Must be a non-negative integer between 0 and 5.
     */
    public ActionHistogram(final long highestTrackableValue, final int numberOfSignificantValueDigits) {
        this(1, highestTrackableValue, numberOfSignificantValueDigits);
    }

    /**
     * Construct a AtomicHistogram given the Lowest and Highest values to be tracked and a number of significant
     * decimal digits. Providing a lowestDiscernibleValue is useful is situations where the units used
     * for the histogram's values are much smaller that the minimal accuracy required. E.g. when tracking
     * time values stated in nanosecond units, where the minimal accuracy required is a microsecond, the
     * proper value for lowestDiscernibleValue would be 1000.
     *
     * @param lowestDiscernibleValue The lowest value that can be tracked (distinguished from 0) by the histogram.
     *                               Must be a positive integer that is {@literal >=} 1. May be internally rounded
     *                               down to nearest power of 2.
     * @param highestTrackableValue The highest value to be tracked by the histogram. Must be a positive
     *                              integer that is {@literal >=} (2 * lowestDiscernibleValue).
     * @param numberOfSignificantValueDigits Specifies the precision to use. This is the number of significant
     *                                       decimal digits to which the histogram will maintain value resolution
     *                                       and separation. Must be a non-negative integer between 0 and 5.
     */
    public ActionHistogram(final long lowestDiscernibleValue, final long highestTrackableValue, final int numberOfSignificantValueDigits) {
        super(lowestDiscernibleValue, highestTrackableValue, numberOfSignificantValueDigits, false);
//        counts = new AtomicLongArray(countsArrayLength);
        counts = new AtomicReferenceArray<>(countsArrayLength);
        initCounts();
        wordSizeInBytes = 8;
    }

    /**
     * Construct a histogram with the same range settings as a given source histogram,
     * duplicating the source's start/end timestamps (but NOT it's contents)
     * @param source The source histogram to duplicate
     */
    public ActionHistogram(final AbstractHistogram source) {
        super(source, false);
//        counts = new AtomicLongArray(countsArrayLength);
        counts = new AtomicReferenceArray<>(countsArrayLength);
        initCounts();
        wordSizeInBytes = 8;
    }

    private void initCounts() {
        for (int i = 0; i < counts.length(); i++) {
            counts.set(i, new LongAdder());
        }
    }

    /**
     * Construct a new histogram by decoding it from a ByteBuffer.
     * @param buffer The buffer to decode from
     * @param minBarForHighestTrackableValue Force highestTrackableValue to be set at least this high
     * @return The newly constructed histogram
     */
    public static ActionHistogram decodeFromByteBuffer(final ByteBuffer buffer,
                                                       final long minBarForHighestTrackableValue) {
        return (ActionHistogram) decodeFromByteBuffer(buffer, ActionHistogram.class,
                minBarForHighestTrackableValue);
    }

    /**
     * Construct a new histogram by decoding it from a compressed form in a ByteBuffer.
     * @param buffer The buffer to decode from
     * @param minBarForHighestTrackableValue Force highestTrackableValue to be set at least this high
     * @return The newly constructed histogram
     * @throws DataFormatException on error parsing/decompressing the buffer
     */
    public static ActionHistogram decodeFromCompressedByteBuffer(final ByteBuffer buffer,
                                                                 final long minBarForHighestTrackableValue) throws DataFormatException {
        return decodeFromCompressedByteBuffer(buffer, ActionHistogram.class, minBarForHighestTrackableValue);
    }

    private void readObject(final ObjectInputStream o)
            throws IOException, ClassNotFoundException {
        o.defaultReadObject();
    }

    @Override
    synchronized void fillCountsArrayFromBuffer(final ByteBuffer buffer, final int length) {
        LongBuffer logbuffer = buffer.asLongBuffer();
        for (int i = 0; i < length; i++) {
            final LongAdder l = new LongAdder();
            l.add(logbuffer.get());
            counts.lazySet(i, l);
        }
    }
}
