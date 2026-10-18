package dev.puitheme;

/** Read scheduling, async completion and lifecycle race coverage; no Android or Root execution. */
public final class HomeStatusRefreshPolicyCheck {
    private static int checks;
    private static void require(boolean value, String why) { checks++; if (!value) throw new AssertionError(why); }
    private static final HomeStatusRefreshPolicy.Kind PERF = HomeStatusRefreshPolicy.Kind.PERFORMANCE;
    private static final HomeStatusRefreshPolicy.Kind RUNTIME = HomeStatusRefreshPolicy.Kind.RUNTIME;

    public static void main(String[] args) {
        publication();
        HomeStatusRefreshPolicy policy = new HomeStatusRefreshPolicy();
        require(policy.begin(PERF, 0, false) == 0L && policy.nextDelay(0) == -1L, "hidden page started a read");
        require(policy.setVisible(true), "entry did not activate");
        long first = policy.begin(PERF, 0, false), runtime = policy.begin(RUNTIME, 0, false);
        require(first > 0L && runtime > first, "home entry did not read both independent channels");
        require(policy.begin(PERF, 1, false) == 0L, "duplicate read while in flight");
        require(!policy.complete(PERF, runtime, 2, true, false, false), "wrong callback ticket accepted");
        require(policy.complete(PERF, first, 10, true, false, false), "read result rejected");
        require(!policy.complete(PERF, first, 11, true, false, false), "duplicate completion accepted");
        require(policy.begin(PERF, 30009, false) == 0L, "stable status queried too frequently");
        require(policy.begin(PERF, 30010, true) == 0L, "status read overlapped an authorized operation");
        long second = policy.begin(PERF, 30010, false);
        require(second > first, "stable periodic read missing");
        policy.setVisible(false);
        require(policy.nextDelay(30011) == -1L, "background scheduled polling");
        require(!policy.complete(PERF, second, 30012, true, false, false), "background result accepted");
        policy.setVisible(true);
        long reentry = policy.begin(PERF, 30013, false);
        require(reentry > second, "reentry did not request fresh status");
        require(!policy.complete(PERF, second, 30014, true, false, false), "old page result overwrote fresh read");
        require(policy.complete(PERF, reentry, 30015, true, true, false), "changing result rejected");
        require(policy.begin(PERF, 35014, false) == 0L, "changing status polled before 5 seconds");
        long fast = policy.begin(PERF, 35015, false);
        require(fast > reentry, "async optimization not followed up");
        policy.complete(PERF, fast, 35016, false, false, false);
        require(policy.begin(PERF, 50015, false) == 0L, "first transient retry too soon");
        long retry = policy.begin(PERF, 50016, false);
        require(retry > fast, "transient unknown status never retried");
        policy.complete(PERF, retry, 50017, false, false, false);
        require(policy.begin(PERF, 80016, false) == 0L, "second retry did not back off");
        retry = policy.begin(PERF, 80017, false);
        policy.complete(PERF, retry, 80018, false, false, false);
        require(policy.begin(PERF, 140017, false) == 0L, "third retry did not reach 60 seconds");
        retry = policy.begin(PERF, 140018, false);
        policy.complete(PERF, retry, 140019, false, false, true);
        require(policy.begin(PERF, 1000000, false) == 0L, "denied Root status repeatedly requested authorization");
        policy.retry(PERF, 1000001);
        retry = policy.begin(PERF, 1000001, false);
        require(retry > 0L, "explicit retry remained blocked");
        policy.complete(PERF, retry, 1000002, true, false, false);
        policy.afterAction(PERF, 1000003);
        require(policy.begin(PERF, 1001002, false) == 0L, "post-action status read before state could settle");
        retry = policy.begin(PERF, 1001003, false);
        require(retry > 0L, "post-action status not automatically refreshed");
        policy.afterAction(PERF, 1001004);
        require(policy.begin(PERF, 1002004, false) == 0L, "post-action refresh duplicated in-flight work");
        require(policy.complete(PERF, retry, 1002005, true, false, false), "old in-flight read did not finish");
        retry = policy.begin(PERF, 1002005, false);
        require(retry > 0L, "old read swallowed the confirmed action's follow-up");
        policy.afterAction(PERF, 1002006);
        require(policy.complete(PERF, retry, 1002007, false, false, true), "old failed read did not finish");
        require(policy.begin(PERF, 1003005, false) == 0L, "follow-up ignored post-action settling delay");
        retry = policy.begin(PERF, 1003006, false);
        require(retry > 0L, "old denial blocked a newly authorized action's follow-up");
        policy.setVisible(false); policy.setVisible(true);
        require(!policy.complete(PERF, retry, 1002005, true, false, false), "pause/resume accepted cancelled read");
        require(policy.begin(PERF, 1002005, false) > 0L, "pause/resume failed to resume fresh reads");
        policy = new HomeStatusRefreshPolicy(); policy.setVisible(true);
        long beforeAction = policy.begin(RUNTIME, 0, false);
        policy.afterAction(RUNTIME, 100);
        require(policy.complete(RUNTIME, beforeAction, 200, true, false, false), "resident read completion rejected");
        require(policy.begin(RUNTIME, 1099, false) == 0L, "resident follow-up read too early");
        long afterAction = policy.begin(RUNTIME, 1100, false);
        require(afterAction > beforeAction, "resident operation followed by stale probe deferred new read for 30 seconds");
        policy.retry(RUNTIME, 1200);
        require(policy.begin(RUNTIME, 1200, false) == 0L, "explicit retry duplicated pending probe");
        require(policy.complete(RUNTIME, afterAction, 1300, false, false, false), "retry pending completion rejected");
        require(policy.begin(RUNTIME, 1300, false) > afterAction, "pending completion swallowed explicit retry");
        policy = new HomeStatusRefreshPolicy(); policy.setVisible(true);
        long deniedPerf = policy.begin(PERF, 0, false), deniedRuntime = policy.begin(RUNTIME, 0, false);
        policy.complete(PERF, deniedPerf, 1, false, false, true);
        policy.complete(RUNTIME, deniedRuntime, 1, false, false, true);
        require(policy.nextDelay(2) == -1L, "blocked channels kept polling");
        policy.setVisible(false); policy.setVisible(true);
        require(policy.begin(PERF, 3, false) > deniedRuntime, "page reentry failed to recheck denied status");
        System.out.println("HomeStatusRefreshPolicyCheck: " + checks + " checks passed");
    }

    private static void publication() {
        HomeStatusRefreshPolicy policy = new HomeStatusRefreshPolicy();
        java.util.List<Object> unknown = java.util.Arrays.<Object>asList(null, true, "Root unavailable");
        require(policy.publish(PERF, unknown), "initial unknown result was suppressed");
        require(!policy.publish(PERF, new java.util.ArrayList<>(unknown)), "unknown result published again just because the report instance changed");
        java.util.List<Object> installed = java.util.Arrays.<Object>asList(true, false, "running", 0L, 4L, false);
        require(policy.publish(PERF, installed), "first real rooted status not published");
        policy.setVisible(true);
        for (int i = 0; i < 64; i++) {
            long now = i * (HomeStatusRefreshPolicy.NORMAL_MS + 1L);
            long ticket = policy.begin(PERF, now, false);
            require(ticket != 0L && policy.complete(PERF, ticket, now, true, false, false), "stable background read did not finish");
            require(!policy.publish(PERF, new java.util.ArrayList<>(installed)), "stable periodic read republished UI state");
        }
        policy.setVisible(false); policy.setVisible(true);
        require(!policy.publish(PERF, new java.util.ArrayList<>(installed)), "page reentry republished unchanged real status");
        java.util.List<Object> finished = java.util.Arrays.<Object>asList(true, false, "ok", 4L, 4L, false);
        require(policy.publish(PERF, finished), "async optimization completion was hidden");
        java.util.List<Object> failure = java.util.Arrays.<Object>asList(true, false, "ok", 4L, 4L, true);
        require(policy.publish(PERF, failure), "new read failure did not publish unknown state alongside the last real report");
        require(!policy.publish(PERF, new java.util.ArrayList<>(failure)), "unchanged read failure caused repeated UI publication");
        require(policy.publish(PERF, finished), "successful recovery after a failure was suppressed");
        require(policy.publish(RUNTIME, finished) && !policy.publish(RUNTIME, new java.util.ArrayList<>(finished)), "independent status channels shared a publication cache");
    }
}
