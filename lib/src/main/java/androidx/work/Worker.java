package androidx.work;

import android.content.Context;

public abstract class Worker {
    public Worker(Context context, WorkerParameters workerParams) {
    }

    public abstract Result doWork();

    public static abstract class Result {
        private static final Result SUCCESS_INSTANCE = new Result() {
            @Override
            public String toString() { return "SUCCESS"; }
        };
        private static final Result FAILURE_INSTANCE = new Result() {
            @Override
            public String toString() { return "FAILURE"; }
        };
        private static final Result RETRY_INSTANCE = new Result() {
            @Override
            public String toString() { return "RETRY"; }
        };

        public static Result success() {
            return SUCCESS_INSTANCE;
        }
        public static Result failure() {
            return FAILURE_INSTANCE;
        }
        public static Result retry() {
            return RETRY_INSTANCE;
        }
    }
}
