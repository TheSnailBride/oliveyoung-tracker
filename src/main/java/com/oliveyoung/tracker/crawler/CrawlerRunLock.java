package com.oliveyoung.tracker.crawler;

import java.util.Optional;

public interface CrawlerRunLock {

    Optional<Lease> tryAcquire();

    interface Lease extends AutoCloseable {
        @Override
        void close();
    }
}
