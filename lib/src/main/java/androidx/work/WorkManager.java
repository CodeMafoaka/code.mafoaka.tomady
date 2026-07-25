package androidx.work;

import android.content.Context;

public abstract class WorkManager {
    public static WorkManager getInstance(Context context) {
        return new WorkManager() {
            @Override
            public void enqueue(PeriodicWorkRequest request) {
                // Mock enqueue
            }
        };
    }

    public abstract void enqueue(PeriodicWorkRequest request);
}
