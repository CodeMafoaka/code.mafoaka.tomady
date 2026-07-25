package androidx.work;

import android.content.Context;

public abstract class Worker {
    public Worker(Context context, WorkerParameters workerParams) {
    }

    public abstract Result doWork();

    public static abstract class Result {
        public static Result success() {
            return new Result() {};
        }
        public static Result failure() {
            return new Result() {};
        }
        public static Result retry() {
            return new Result() {};
        }
    }
}
