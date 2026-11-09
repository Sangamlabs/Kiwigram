/*
 * Kiwigram - Enhanced Custom Telegram Client
 */

package org.telegram.messenger;

import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

public class KiwigramDeleteBenchmarkTest {

    private static final long MY_USER_ID = 123456789L;
    private static final long OTHER_USER_ID = 987654321L;
    private static final int BATCH_SIZE = 100;

    public static class MockMessage {
        public int id;
        public long senderId;
        public boolean out;

        public MockMessage(int id, long senderId) {
            this.id = id;
            this.senderId = senderId;
            this.out = (senderId == MY_USER_ID);
        }
    }

    public static class BenchmarkResult {
        public int totalHistorySize;
        public int userMessagesCount;
        public int sequentialApiRequests;
        public int serverSearchApiRequests;
        public long sequentialMemoryAllocatedBytes;
        public long serverSearchMemoryAllocatedBytes;
        public long sequentialScanTimeMs;
        public long serverSearchTimeMs;
        public int foundUserMessages;
        public int foundForeignMessages;
        public double deletionSuccessRate;
    }

    private BenchmarkResult runBenchmark(int historySize, double userRatio) {
        BenchmarkResult result = new BenchmarkResult();
        result.totalHistorySize = historySize;

        // 1. Generate simulated chat history
        List<MockMessage> simulatedHistory = new ArrayList<>(historySize);
        int expectedUserMessages = 0;
        for (int i = 1; i <= historySize; i++) {
            boolean isUser = (i % (int)(1.0 / userRatio) == 0);
            long sender = isUser ? MY_USER_ID : OTHER_USER_ID;
            if (isUser) {
                expectedUserMessages++;
            }
            simulatedHistory.add(new MockMessage(i, sender));
        }
        result.userMessagesCount = expectedUserMessages;

        // 2. Measure Legacy Sequential History Scan
        System.gc();
        long memBeforeSeq = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        long startSeq = System.currentTimeMillis();

        int seqRequests = 0;
        LinkedHashSet<Integer> seqCollectedIds = new LinkedHashSet<>();
        int seqForeignCount = 0;

        for (int offset = 0; offset < historySize; offset += BATCH_SIZE) {
            seqRequests++;
            int end = Math.min(offset + BATCH_SIZE, historySize);
            List<MockMessage> batch = simulatedHistory.subList(offset, end);
            for (MockMessage msg : batch) {
                if (msg.out && msg.senderId == MY_USER_ID) {
                    seqCollectedIds.add(msg.id);
                } else if (msg.senderId != MY_USER_ID) {
                    // Safety check: must never collect foreign messages
                    seqForeignCount++;
                }
            }
        }
        long seqTime = System.currentTimeMillis() - startSeq;
        long memAfterSeq = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();

        result.sequentialApiRequests = seqRequests;
        result.sequentialScanTimeMs = Math.max(1, seqTime);
        result.sequentialMemoryAllocatedBytes = Math.max(0, memAfterSeq - memBeforeSeq);

        // 3. Measure Optimized Server-Side Filtered Search (messages.search with from_id)
        System.gc();
        long memBeforeSearch = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        long startSearch = System.currentTimeMillis();

        int searchRequests = 0;
        LinkedHashSet<Integer> searchCollectedIds = new LinkedHashSet<>();
        int searchForeignCount = 0;

        // In server-side search, Telegram server returns only messages matching from_id
        List<MockMessage> serverFilteredMessages = new ArrayList<>();
        for (MockMessage msg : simulatedHistory) {
            if (msg.senderId == MY_USER_ID) {
                serverFilteredMessages.add(msg);
            }
        }

        for (int offset = 0; offset < serverFilteredMessages.size(); offset += BATCH_SIZE) {
            searchRequests++;
            int end = Math.min(offset + BATCH_SIZE, serverFilteredMessages.size());
            List<MockMessage> batch = serverFilteredMessages.subList(offset, end);
            for (MockMessage msg : batch) {
                // Client-side authorship verification guard
                if (msg.senderId == MY_USER_ID && msg.out) {
                    searchCollectedIds.add(msg.id);
                } else {
                    searchForeignCount++;
                }
            }
        }
        if (serverFilteredMessages.isEmpty()) {
            searchRequests = 1;
        }

        long searchTime = System.currentTimeMillis() - startSearch;
        long memAfterSearch = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();

        result.serverSearchApiRequests = searchRequests;
        result.serverSearchTimeMs = Math.max(1, searchTime);
        result.serverSearchMemoryAllocatedBytes = Math.max(0, memAfterSearch - memBeforeSearch);
        result.foundUserMessages = searchCollectedIds.size();
        result.foundForeignMessages = searchForeignCount;
        result.deletionSuccessRate = expectedUserMessages > 0
                ? (double) result.foundUserMessages / expectedUserMessages * 100.0 : 100.0;

        return result;
    }

    @Test
    public void testBenchmark1000Messages() {
        BenchmarkResult r = runBenchmark(1000, 0.05); // 50 user messages in 1,000
        Assert.assertEquals("Zero foreign messages should be found", 0, r.foundForeignMessages);
        Assert.assertEquals("All user messages found", r.userMessagesCount, r.foundUserMessages);
        Assert.assertEquals(100.0, r.deletionSuccessRate, 0.01);
        Assert.assertTrue("Filtered search should require fewer API calls than sequential scan",
                r.serverSearchApiRequests < r.sequentialApiRequests);
        Assert.assertEquals(10, r.sequentialApiRequests); // 1000 / 100
        Assert.assertEquals(1, r.serverSearchApiRequests);  // 50 / 100 = 1 batch
    }

    @Test
    public void testBenchmark10000Messages() {
        BenchmarkResult r = runBenchmark(10000, 0.02); // 200 user messages in 10,000
        Assert.assertEquals("Zero foreign messages should be found", 0, r.foundForeignMessages);
        Assert.assertEquals("All user messages found", r.userMessagesCount, r.foundUserMessages);
        Assert.assertEquals(100.0, r.deletionSuccessRate, 0.01);
        Assert.assertEquals(100, r.sequentialApiRequests); // 10000 / 100
        Assert.assertEquals(2, r.serverSearchApiRequests);  // 200 / 100 = 2 batches
    }

    @Test
    public void testBenchmark50000Messages() {
        BenchmarkResult r = runBenchmark(50000, 0.01); // 500 user messages in 50,000
        Assert.assertEquals("Zero foreign messages should be found", 0, r.foundForeignMessages);
        Assert.assertEquals("All user messages found", r.userMessagesCount, r.foundUserMessages);
        Assert.assertEquals(100.0, r.deletionSuccessRate, 0.01);
        Assert.assertEquals(500, r.sequentialApiRequests); // 50,000 / 100 = 500 API calls!
        Assert.assertEquals(5, r.serverSearchApiRequests);  // 500 / 100 = 5 API calls! (100x reduction)
    }

    @Test
    public void testBenchmark100000Messages() {
        BenchmarkResult r = runBenchmark(100000, 0.005); // 500 user messages in 100,000
        Assert.assertEquals("Zero foreign messages should be found", 0, r.foundForeignMessages);
        Assert.assertEquals("All user messages found", r.userMessagesCount, r.foundUserMessages);
        Assert.assertEquals(100.0, r.deletionSuccessRate, 0.01);
        Assert.assertEquals(1000, r.sequentialApiRequests); // 100,000 / 100 = 1,000 sequential requests!
        Assert.assertEquals(5, r.serverSearchApiRequests);   // 500 / 100 = 5 requests! (200x reduction)
    }
}
