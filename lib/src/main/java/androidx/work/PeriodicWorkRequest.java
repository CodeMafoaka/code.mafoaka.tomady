package androidx.work;

import java.util.concurrent.TimeUnit;

public class PeriodicWorkRequest {
    public static class Builder {
        public Builder(Class<? extends Worker> workerClass, long repeatInterval, TimeUnit repeatIntervalTimeUnit) {
        }

        public Builder setInitialDelay(long duration, TimeUnit timeUnit) {
            return this;
        }

        public PeriodicWorkRequest build() {
            return new PeriodicWorkRequest();
        }
    }
}
